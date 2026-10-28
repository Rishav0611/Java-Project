package com.privacyshield;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for scanning, stripping and redacting photo metadata.
 *
 * The JPEG fixture is a tiny made-up image with fake EXIF camera, date and GPS
 * fields. It contains no real person's data.
 */
class PhotoServiceTest {
    private static final String EXIF_JPEG_BASE64 =
            "/9j/4QE/RXhpZgAASUkqAAgAAAAGAA8BAgALAAAAqgAAABABAgANAAAAtQAAADEBAgASAAAAwgAAADIBAgAUAAAA1AAAAGmHBAAB" +
            "AAAAVgAAACWIBAABAAAAaAAAAAAAAAABAAOQAgAUAAAA6AAAAAAAAAAFAAEAAgACAAAATgAAAAIABQADAAAA/AAAAAMAAgACAAAA" +
            "RQAAAAQABQADAAAAFAEAAB0AAgALAAAALAEAAAAAAABQcml2YWN5Q2FtAFRlc3RQaG9uZSAwMQBQcml2YWN5U2hpZWxkVGVzdAAy" +
            "MDI2OjEwOjI4IDEyOjAwOjAwADIwMjY6MTA6MjggMTI6MDA6MDAADAAAAAEAAAAiAAAAAQAAADgAAAABAAAATQAAAAEAAAAjAAAA" +
            "AQAAAAAAAAABAAAAMjAyNjoxMDoyOAD/4AAQSkZJRgABAgAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRof" +
            "Hh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIy" +
            "MjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAAQABADASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQF" +
            "BgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJico" +
            "KSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5" +
            "usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QA" +
            "tREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5" +
            "OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbH" +
            "yMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwCtoGgf259o/wBJ8jydv/LPdnOfcelGv6B/Yf2f/SfP" +
            "87d/yz24xj3PrRoGv/2H9o/0bz/O2/8ALTbjGfY+tGv6/wD259n/ANG8jyd3/LTdnOPYelT/AMK39rf9Q3/bv8v/AIF8X9WOT/ZP" +
            "qn/Tz59/u2P/2Q==";

    private static final String GROUP_EXIF = "Exif IFD0";
    private static final String GROUP_EXIF_SUB = "Exif SubIFD";
    private static final String GROUP_GPS = "GPS";

    private final PhotoService service = new PhotoService();

    @Test
    void scanFindsGpsCameraAndTime(@TempDir Path temp) throws Exception {
        List<PhotoMetadata> metadata = service.scan(writeExifJpeg(temp));

        assertEquals("PrivacyCam", valueOf(metadata, GROUP_EXIF, "Make"));
        assertEquals("TestPhone 01", valueOf(metadata, GROUP_EXIF, "Model"));
        assertEquals("PrivacyShieldTest", valueOf(metadata, GROUP_EXIF, "Software"));
        assertEquals("2026:10:28 12:00:00", valueOf(metadata, GROUP_EXIF, "Date/Time"));
        assertEquals("2026:10:28 12:00:00", valueOf(metadata, GROUP_EXIF_SUB, "Date/Time Original"));
        assertNotNull(valueOf(metadata, GROUP_GPS, "GPS Latitude"));
        assertNotNull(valueOf(metadata, GROUP_GPS, "GPS Longitude"));
        assertEquals("2026:10:28", valueOf(metadata, GROUP_GPS, "GPS Date Stamp"));
        assertTrue(metadata.stream().anyMatch(PhotoMetadata::isLocation));
        assertThrows(UnsupportedOperationException.class,
                () -> metadata.add(new PhotoMetadata("XMP", "Creator", "Nobody")));
    }

    @Test
    void plainJpegHasNoPrivateMetadata(@TempDir Path temp) throws Exception {
        assertTrue(service.scan(writePlainJpeg(temp)).isEmpty());
    }

    @Test
    void stripRemovesMetadataAndKeepsPixels(@TempDir Path temp) throws Exception {
        File input = writeExifJpeg(temp);
        File output = temp.resolve("clean.jpg").toFile();

        service.strip(input, output);

        assertTrue(output.isFile());
        assertTrue(service.scan(output).isEmpty());
        assertFalse(rawText(output).contains("PrivacyCam"));

        BufferedImage before = ImageIO.read(input);
        BufferedImage after = ImageIO.read(output);

        assertEquals(before.getWidth(), after.getWidth());
        assertEquals(before.getHeight(), after.getHeight());

        for (int y = 0; y < before.getHeight(); y++) {
            for (int x = 0; x < before.getWidth(); x++) {
                assertEquals(before.getRGB(x, y), after.getRGB(x, y));
            }
        }
    }

    @Test
    void scanFindsPngTextMetadata(@TempDir Path temp) throws Exception {
        List<PhotoMetadata> metadata = service.scan(writePngWithText(temp));

        assertTrue(metadata.stream()
                .anyMatch(item -> item.getValue().contains("PrivacyShieldTest")));
        assertFalse(metadata.stream().anyMatch(PhotoMetadata::isLocation));
    }

    @Test
    void stripRemovesPngTextChunks(@TempDir Path temp) throws Exception {
        File input = writePngWithText(temp);
        File output = temp.resolve("clean.png").toFile();

        assertTrue(rawText(input).contains("PrivacyShieldTest"));

        service.strip(input, output);

        assertFalse(rawText(output).contains("PrivacyShieldTest"));
        assertFalse(rawText(output).contains("tEXt"));
        assertTrue(service.scan(output).isEmpty());

        BufferedImage image = ImageIO.read(output);
        assertNotNull(image);
        assertEquals(4, image.getWidth());
        assertEquals(4, image.getHeight());
    }

    @Test
    void redactFillsBoxesAndRemovesMetadata(@TempDir Path temp) throws Exception {
        File input = writeExifJpeg(temp);
        File output = temp.resolve("redacted.png").toFile();

        service.redact(input, output, Arrays.asList(new Rectangle(2, 2, 4, 4)));

        assertTrue(output.isFile());
        assertTrue(service.scan(output).isEmpty());

        BufferedImage source = ImageIO.read(input);
        BufferedImage image = ImageIO.read(output);
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        assertEquals(Color.BLACK.getRGB(), image.getRGB(3, 3));
        assertEquals(source.getRGB(10, 10), image.getRGB(10, 10));
    }

    @Test
    void redactClipsBoxesOutsideTheImage(@TempDir Path temp) throws Exception {
        File input = writePlainJpeg(temp);
        File output = temp.resolve("corner.png").toFile();

        service.redact(input, output, Arrays.asList(new Rectangle(6, 6, 50, 50)));

        BufferedImage image = ImageIO.read(output);
        assertEquals(Color.BLACK.getRGB(), image.getRGB(7, 7));
    }

    @Test
    void redactWithoutBoxesSavesACleanCopy(@TempDir Path temp) throws Exception {
        File input = writeExifJpeg(temp);
        File output = temp.resolve("copy.jpg").toFile();

        service.redact(input, output, null);

        assertTrue(output.isFile());
        assertTrue(service.scan(output).isEmpty());
        assertEquals(16, ImageIO.read(output).getWidth());
    }

    @Test
    void sameFileIsRejected(@TempDir Path temp) throws Exception {
        File photo = writeExifJpeg(temp);

        assertThrows(IllegalArgumentException.class, () -> service.strip(photo, photo));
        assertThrows(IllegalArgumentException.class,
                () -> service.redact(photo, photo, Arrays.asList(new Rectangle(1, 1, 2, 2))));
    }

    @Test
    void unsupportedFormatsAreRejected(@TempDir Path temp) throws Exception {
        File text = temp.resolve("notes.txt").toFile();
        Files.write(text.toPath(), "not a photo".getBytes(StandardCharsets.UTF_8));

        assertThrows(IOException.class,
                () -> service.strip(text, temp.resolve("out.jpg").toFile()));
        assertThrows(IOException.class,
                () -> service.redact(text, temp.resolve("out.jpg").toFile(), null));
    }

    @Test
    void redactNeedsAUsefulOutputExtension(@TempDir Path temp) throws Exception {
        File input = writePlainJpeg(temp);

        assertThrows(IOException.class,
                () -> service.redact(input, temp.resolve("out.gif").toFile(), null));
    }

    @Test
    void missingFilesAndNullsAreRejected(@TempDir Path temp) {
        File missing = temp.resolve("missing.jpg").toFile();

        assertThrows(IOException.class, () -> service.scan(missing));
        assertThrows(IOException.class,
                () -> service.strip(missing, temp.resolve("out.jpg").toFile()));
        assertThrows(IllegalArgumentException.class, () -> service.scan(null));
        assertThrows(IllegalArgumentException.class,
                () -> service.strip(null, temp.resolve("out.jpg").toFile()));
        assertThrows(IllegalArgumentException.class,
                () -> service.redact(null, temp.resolve("out.jpg").toFile(), null));
        assertThrows(IllegalArgumentException.class,
                () -> service.strip(writeExifJpeg(temp), null));
    }

    @Test
    void defaultFileNamesKeepTheExtension(@TempDir Path temp) {
        assertEquals("photo-stripped.jpg",
                PhotoService.defaultStrippedFile(new File("photo.jpg")).getName());
        assertEquals("photo-redacted.png",
                PhotoService.defaultRedactedFile(new File("photo.png")).getName());
        assertEquals("noext-stripped.jpg",
                PhotoService.defaultStrippedFile(new File("noext")).getName());
        assertThrows(IllegalArgumentException.class,
                () -> PhotoService.defaultStrippedFile(null));
    }

    private static String valueOf(List<PhotoMetadata> metadata, String group, String tag) {
        for (PhotoMetadata item : metadata) {
            if (group.equals(item.getGroup()) && tag.equals(item.getTag())) {
                return item.getValue();
            }
        }

        return null;
    }

    private static File writeExifJpeg(Path directory) throws IOException {
        File file = directory.resolve("exif.jpg").toFile();
        Files.write(file.toPath(), Base64.getDecoder().decode(EXIF_JPEG_BASE64));
        return file;
    }

    private static File writePlainJpeg(Path directory) throws IOException {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, 8, 8);
        graphics.dispose();

        File file = directory.resolve("plain.jpg").toFile();

        if (!ImageIO.write(image, "jpg", file)) {
            throw new IOException("Could not create the test JPEG.");
        }

        return file;
    }

    private static File writePngWithText(Path directory) throws IOException {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream pngOut = new ByteArrayOutputStream();

        if (!ImageIO.write(image, "png", pngOut)) {
            throw new IOException("Could not create the test PNG.");
        }

        byte[] png = pngOut.toByteArray();
        int insertAt = png.length - 12;
        byte[] type = "tEXt".getBytes(StandardCharsets.US_ASCII);
        byte[] data = "Author\0PrivacyShieldTest".getBytes(StandardCharsets.ISO_8859_1);
        CRC32 crc = new CRC32();
        crc.update(type);
        crc.update(data);

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        result.write(png, 0, insertAt);
        writeInt(result, data.length);
        result.write(type);
        result.write(data);
        writeInt(result, (int) crc.getValue());
        result.write(png, insertAt, png.length - insertAt);

        File file = directory.resolve("text.png").toFile();
        Files.write(file.toPath(), result.toByteArray());
        return file;
    }

    private static void writeInt(ByteArrayOutputStream out, int value) {
        out.write((value >> 24) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    private static String rawText(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.ISO_8859_1);
    }
}
