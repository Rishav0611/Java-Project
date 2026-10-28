package com.privacyshield;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Reads text from a photo and finds Aadhaar, PAN, phone and email values.
 *
 * OCR returns separate words with boxes. The service joins words on the same
 * line, runs the normal PrivacyShield detectors, and then joins the boxes of
 * the words that make up each detected value. The result can be painted as
 * black boxes and saved with PhotoService.redact().
 */
public class OcrTextService {
    private static final float MIN_CONFIDENCE = 20f;
    private static final int BOX_PADDING = 3;

    private final OcrEngine engine;
    private final PrivacyShield privacyShield;

    public OcrTextService() {
        this(new Tess4jOcrEngine());
    }

    public OcrTextService(OcrEngine engine) {
        this(engine, new PrivacyShield());
    }

    public OcrTextService(OcrEngine engine, PrivacyShield privacyShield) {
        if (engine == null) {
            throw new IllegalArgumentException("engine cannot be null");
        }
        if (privacyShield == null) {
            throw new IllegalArgumentException("privacyShield cannot be null");
        }

        this.engine = engine;
        this.privacyShield = privacyShield;
    }

    /**
     * Returns the sensitive values found in the photo and their image boxes.
     * The decoded text stays in memory and is not written to disk.
     */
    public List<OcrMatch> findSensitive(File photo) throws IOException {
        requireExistingFile(photo);

        BufferedImage image = ImageIO.read(photo);

        if (image == null) {
            throw new IOException("Could not read the image: " + photo.getName());
        }

        return findSensitive(image);
    }

    public List<OcrMatch> findSensitive(BufferedImage image) throws IOException {
        if (image == null) {
            throw new IllegalArgumentException("image cannot be null");
        }

        List<OcrWord> words = engine.readWords(image);

        if (words == null || words.isEmpty()) {
            return Collections.emptyList();
        }

        List<OcrWord> usable = new ArrayList<OcrWord>();

        for (OcrWord word : words) {
            if (word == null || word.getText().isEmpty()) {
                continue;
            }

            // A negative confidence means "not reported" and is kept. A low
            // positive confidence is usually noise from the photo.
            if (word.getConfidence() >= 0f && word.getConfidence() < MIN_CONFIDENCE) {
                continue;
            }

            Rectangle box = word.getBounds();

            if (box.width > 0 && box.height > 0) {
                usable.add(word);
            }
        }

        if (usable.isEmpty()) {
            return Collections.emptyList();
        }

        Collections.sort(usable, new Comparator<OcrWord>() {
            @Override
            public int compare(OcrWord left, OcrWord right) {
                Rectangle leftBox = left.getBounds();
                Rectangle rightBox = right.getBounds();

                if (leftBox.y != rightBox.y) {
                    return Integer.compare(leftBox.y, rightBox.y);
                }
                return Integer.compare(leftBox.x, rightBox.x);
            }
        });

        List<List<OcrWord>> lines = groupLines(usable);
        List<OcrMatch> matches = new ArrayList<OcrMatch>();

        for (List<OcrWord> line : lines) {
            addLineMatches(matches, line, image.getWidth(), image.getHeight());
        }

        return Collections.unmodifiableList(matches);
    }

    private void addLineMatches(List<OcrMatch> matches, List<OcrWord> line,
                                int imageWidth, int imageHeight) {
        Collections.sort(line, new Comparator<OcrWord>() {
            @Override
            public int compare(OcrWord left, OcrWord right) {
                return Integer.compare(left.getBounds().x, right.getBounds().x);
            }
        });

        StringBuilder text = new StringBuilder();
        List<WordSpan> spans = new ArrayList<WordSpan>();

        for (OcrWord word : line) {
            if (text.length() > 0) {
                text.append(' ');
            }

            int start = text.length();
            text.append(word.getText());
            spans.add(new WordSpan(word, start, text.length()));
        }

        if (text.length() == 0) {
            return;
        }

        for (Detection detection : privacyShield.detect(text.toString())) {
            Rectangle union = null;

            for (WordSpan span : spans) {
                if (span.end > detection.getStart() && span.start < detection.getEnd()) {
                    Rectangle box = span.word.getBounds();
                    union = union == null ? new Rectangle(box) : union.union(box);
                }
            }

            if (union == null) {
                continue;
            }

            Rectangle box = padAndClip(union, imageWidth, imageHeight);

            if (!box.isEmpty()) {
                matches.add(new OcrMatch(detection.getType(), detection.getValue(), box));
            }
        }
    }

    private static List<List<OcrWord>> groupLines(List<OcrWord> words) {
        List<List<OcrWord>> lines = new ArrayList<List<OcrWord>>();

        for (OcrWord word : words) {
            List<OcrWord> current = lines.isEmpty()
                    ? null
                    : lines.get(lines.size() - 1);

            if (current == null || !sameLine(current.get(current.size() - 1), word)) {
                current = new ArrayList<OcrWord>();
                lines.add(current);
            }

            current.add(word);
        }

        return lines;
    }

    private static boolean sameLine(OcrWord previous, OcrWord word) {
        Rectangle first = previous.getBounds();
        Rectangle second = word.getBounds();
        int firstCenter = first.y + first.height / 2;
        int secondCenter = second.y + second.height / 2;
        int tolerance = Math.max(6, Math.max(first.height, second.height) / 2);

        return Math.abs(firstCenter - secondCenter) <= tolerance;
    }

    private static Rectangle padAndClip(Rectangle box, int imageWidth, int imageHeight) {
        Rectangle padded = new Rectangle(
                box.x - BOX_PADDING,
                box.y - BOX_PADDING,
                box.width + BOX_PADDING * 2,
                box.height + BOX_PADDING * 2);

        return padded.intersection(new Rectangle(0, 0, imageWidth, imageHeight));
    }

    private static void requireExistingFile(File photo) throws IOException {
        if (photo == null) {
            throw new IllegalArgumentException("photo cannot be null");
        }

        if (!photo.isFile()) {
            throw new IOException("Photo not found: " + photo);
        }
    }

    private static final class WordSpan {
        private final OcrWord word;
        private final int start;
        private final int end;

        private WordSpan(OcrWord word, int start, int end) {
            this.word = word;
            this.start = start;
            this.end = end;
        }
    }
}
