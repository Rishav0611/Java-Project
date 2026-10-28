package com.privacyshield;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.jpeg.JpegCommentDirectory;
import com.drew.metadata.png.PngDirectory;
import com.drew.metadata.xmp.XmpDirectory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reads, removes and redacts the metadata of photos.
 *
 * Metadata is read with the metadata-extractor library. For JPEG files the
 * strip step copies the image data unchanged and removes the APPn and comment
 * segments, so the photo is not re-compressed. PNG text, time and EXIF chunks
 * are removed the same way. The new file is scanned again before the method
 * returns. All work is synchronous; the GUI calls it from a background thread.
 */
public class PhotoService {
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 'P', 'N', 'G', (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A
    };

    private static final Set<String> PNG_PRIVATE_CHUNKS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("tEXt", "zTXt", "iTXt", "eXIf", "tIME"))
    );

    /**
     * Returns the GPS, camera, time, author and comment fields of a photo.
     * Technical fields such as image width or compression are left out.
     */
    public List<PhotoMetadata> scan(File photo) throws IOException {
        requireExistingFile(photo);

        Metadata metadata;

        try {
            metadata = ImageMetadataReader.readMetadata(photo);
        } catch (ImageProcessingException exception) {
            throw new IOException("Could not read photo metadata: " + photo.getName(), exception);
        }

        List<PhotoMetadata> found = new ArrayList<PhotoMetadata>();

        for (Directory directory : metadata.getDirectories()) {
            if (directory instanceof XmpDirectory) {
                addXmpProperties(found, (XmpDirectory) directory);
            } else if (directory instanceof PngDirectory) {
                addPngText(found, (PngDirectory) directory);
            } else if (isPrivacyDirectory(directory)) {
                for (Tag tag : directory.getTags()) {
                    addMetadata(found, directory.getName(), tag.getTagName(), tag.getDescription());
                }
            }
        }

        return Collections.unmodifiableList(found);
    }

    /**
     * Writes a copy of a JPEG or PNG photo with GPS, camera, time, author and
     * comment metadata removed. The new file is scanned again to prove that
     * nothing was missed.
     */
    public void strip(File input, File output) throws IOException {
        validateDifferentOutput(input, output);

        byte[] data = Files.readAllBytes(input.toPath());

        if (isJpeg(data)) {
            stripJpeg(data, output);
        } else if (isPng(data)) {
            stripPng(data, output);
        } else {
            throw new IOException("Only JPEG and PNG photos are supported for stripping.");
        }

        verifyNoMetadata(output);
    }

    /**
     * Saves a copy of a photo with the given image areas filled with black.
     * The saved copy has no metadata, so it is safe to share.
     */
    public void redact(File input, File output, List<Rectangle> boxes) throws IOException {
        validateDifferentOutput(input, output);

        String format = formatFor(output);
        BufferedImage source = ImageIO.read(input);

        if (source == null) {
            throw new IOException("Could not read the image: " + input.getName());
        }

        BufferedImage redacted = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = redacted.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.setColor(Color.BLACK);

        if (boxes != null) {
            Rectangle imageArea = new Rectangle(0, 0, source.getWidth(), source.getHeight());

            for (Rectangle box : boxes) {
                if (box == null) {
                    continue;
                }

                Rectangle clipped = box.intersection(imageArea);

                if (!clipped.isEmpty()) {
                    graphics.fillRect(clipped.x, clipped.y, clipped.width, clipped.height);
                }
            }
        }

        graphics.dispose();

        if (!ImageIO.write(redacted, format, output)) {
            throw new IOException("Could not save a " + format + " image.");
        }

        verifyNoMetadata(output);
    }

    /**
     * A friendly default name for the stripped copy, next to the original.
     */
    public static File defaultStrippedFile(File input) {
        return siblingFile(input, "-stripped");
    }

    /**
     * A friendly default name for the redacted copy, next to the original.
     */
    public static File defaultRedactedFile(File input) {
        return siblingFile(input, "-redacted");
    }

    private static void addXmpProperties(List<PhotoMetadata> found, XmpDirectory directory) {
        for (Map.Entry<String, String> entry : directory.getXmpProperties().entrySet()) {
            addMetadata(found, "XMP", entry.getKey(), entry.getValue());
        }
    }

    /**
     * PNG keeps text and time in its own directory. Only those two tags are
     * private; width, colour type and similar technical tags are skipped.
     */
    private static void addPngText(List<PhotoMetadata> found, PngDirectory directory) {
        addPngTag(found, directory, PngDirectory.TAG_TEXTUAL_DATA);
        addPngTag(found, directory, PngDirectory.TAG_LAST_MODIFICATION_TIME);
    }

    private static void addPngTag(List<PhotoMetadata> found, PngDirectory directory, int tagType) {
        String value = directory.getString(tagType);

        if (value != null) {
            addMetadata(found, directory.getName(), directory.getTagName(tagType), value);
        }
    }

    private static void addMetadata(List<PhotoMetadata> found, String group, String tag, String value) {
        String cleanValue = clean(value);

        if (cleanValue.isEmpty()) {
            return;
        }

        found.add(new PhotoMetadata(group, tag == null ? "Unknown tag" : tag, cleanValue));
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("\\s+", " ").trim();
    }

    private static boolean isPrivacyDirectory(Directory directory) {
        return directory instanceof GpsDirectory
                || directory instanceof ExifIFD0Directory
                || directory instanceof ExifSubIFDDirectory
                || directory instanceof IptcDirectory
                || directory instanceof JpegCommentDirectory;
    }

    private void stripJpeg(byte[] data, File output) throws IOException {
        ByteArrayOutputStream stripped = new ByteArrayOutputStream(data.length);
        stripped.write(0xFF);
        stripped.write(0xD8);

        int position = 2;
        boolean sawImageData = false;

        while (position < data.length) {
            if ((data[position] & 0xFF) != 0xFF) {
                throw new IOException("Not a valid JPEG file.");
            }

            int markerStart = position;

            while (position < data.length && (data[position] & 0xFF) == 0xFF) {
                position++;
            }

            if (position >= data.length) {
                throw new IOException("Not a valid JPEG file.");
            }

            int marker = data[position] & 0xFF;
            position++;

            if (marker == 0xD9) {
                stripped.write(data, markerStart, position - markerStart);
                break;
            }

            if (marker == 0xD8 || marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7)) {
                stripped.write(data, markerStart, position - markerStart);
                continue;
            }

            if (marker == 0xDA) {
                stripped.write(data, markerStart, data.length - markerStart);
                sawImageData = true;
                break;
            }

            int length = readLength(data, position);

            boolean remove = (marker >= 0xE0 && marker <= 0xEF) || marker == 0xFE;

            if (!remove) {
                stripped.write(data, markerStart, (position - markerStart) + length);
            }

            position += length;
        }

        if (!sawImageData) {
            throw new IOException("Not a valid JPEG file.");
        }

        Files.write(output.toPath(), stripped.toByteArray());
    }

    private void stripPng(byte[] data, File output) throws IOException {
        ByteArrayOutputStream stripped = new ByteArrayOutputStream(data.length);
        stripped.write(data, 0, PNG_SIGNATURE.length);

        int position = PNG_SIGNATURE.length;
        boolean sawEnd = false;

        while (position + 12 <= data.length) {
            int length = readInt(data, position);

            if (length < 0 || position + 12 + length > data.length) {
                throw new IOException("Not a valid PNG file.");
            }

            String type = new String(data, position + 4, 4, StandardCharsets.US_ASCII);
            int total = 12 + length;

            if (!PNG_PRIVATE_CHUNKS.contains(type)) {
                stripped.write(data, position, total);
            }

            position += total;

            if ("IEND".equals(type)) {
                sawEnd = true;
                break;
            }
        }

        if (!sawEnd) {
            throw new IOException("Not a valid PNG file.");
        }

        Files.write(output.toPath(), stripped.toByteArray());
    }

    private static int readLength(byte[] data, int position) throws IOException {
        if (position + 2 > data.length) {
            throw new IOException("Not a valid JPEG file.");
        }

        int length = ((data[position] & 0xFF) << 8) | (data[position + 1] & 0xFF);

        if (length < 2 || position + length > data.length) {
            throw new IOException("Not a valid JPEG file.");
        }

        return length;
    }

    private static int readInt(byte[] data, int position) {
        return ((data[position] & 0xFF) << 24)
                | ((data[position + 1] & 0xFF) << 16)
                | ((data[position + 2] & 0xFF) << 8)
                | (data[position + 3] & 0xFF);
    }

    private static boolean isJpeg(byte[] data) {
        return data.length >= 2
                && (data[0] & 0xFF) == 0xFF
                && (data[1] & 0xFF) == 0xD8;
    }

    private static boolean isPng(byte[] data) {
        if (data.length < PNG_SIGNATURE.length) {
            return false;
        }

        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (data[i] != PNG_SIGNATURE[i]) {
                return false;
            }
        }

        return true;
    }

    private void verifyNoMetadata(File output) throws IOException {
        List<PhotoMetadata> remaining = scan(output);

        if (!remaining.isEmpty()) {
            throw new IOException("Metadata is still present in the new file ("
                    + remaining.size() + " item(s) found).");
        }
    }

    private static void validateDifferentOutput(File input, File output) throws IOException {
        requireExistingFile(input);

        if (output == null) {
            throw new IllegalArgumentException("output cannot be null");
        }

        if (input.getCanonicalFile().equals(output.getCanonicalFile())) {
            throw new IllegalArgumentException("Choose a different output file; "
                    + "the original photo is not overwritten.");
        }

        File parent = output.getAbsoluteFile().getParentFile();

        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create the output folder: " + parent);
        }
    }

    private static void requireExistingFile(File photo) throws IOException {
        if (photo == null) {
            throw new IllegalArgumentException("photo cannot be null");
        }

        if (!photo.isFile()) {
            throw new IOException("Photo not found: " + photo);
        }
    }

    private static String formatFor(File output) throws IOException {
        String name = output.getName().toLowerCase(Locale.ROOT);

        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "jpg";
        }
        if (name.endsWith(".png")) {
            return "png";
        }

        throw new IOException("Use a .jpg, .jpeg or .png file name for the redacted photo.");
    }

    private static File siblingFile(File input, String suffix) {
        if (input == null) {
            throw new IllegalArgumentException("photo cannot be null");
        }

        String name = input.getName();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String extension = dot > 0 ? name.substring(dot) : ".jpg";
        File parent = input.getAbsoluteFile().getParentFile();

        return new File(parent, base + suffix + extension);
    }
}
