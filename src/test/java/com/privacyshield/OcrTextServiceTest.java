package com.privacyshield;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the OCR post-processing with a fake engine, so no native Tesseract
 * installation is needed. All values are made up.
 */
class OcrTextServiceTest {
    private static final int IMAGE_WIDTH = 500;
    private static final int IMAGE_HEIGHT = 220;

    @Test
    void findsSensitiveValuesAndJoinsTheirBoxes() throws Exception {
        List<OcrWord> words = new ArrayList<OcrWord>();
        words.add(word("Email:", 10, 10, 55, 22, 95));
        words.add(word("person@example.com", 75, 10, 170, 22, 96));
        words.add(word("+91", 10, 55, 35, 22, 90));
        words.add(word("98765", 50, 55, 48, 22, 92));
        words.add(word("43210", 105, 55, 48, 22, 93));
        words.add(word("PAN", 10, 100, 38, 22, 88));
        words.add(word("ABCPD1234E", 55, 100, 110, 22, 91));
        words.add(word("Aadhaar", 10, 145, 65, 22, 87));
        words.add(word("2345", 80, 145, 45, 22, 94));
        words.add(word("6789", 130, 145, 45, 22, 94));
        words.add(word("0124", 180, 145, 45, 22, 94));

        List<OcrMatch> matches = service(words).findSensitive(image());

        assertEquals(4, matches.size());
        Set<DataType> types = EnumSet.noneOf(DataType.class);

        for (OcrMatch match : matches) {
            types.add(match.getType());
            assertFalse(match.getBounds().isEmpty());
            assertTrue(match.getBounds().x >= 0);
            assertTrue(match.getBounds().y >= 0);
            assertTrue(match.getBounds().x + match.getBounds().width <= IMAGE_WIDTH);
            assertTrue(match.getBounds().y + match.getBounds().height <= IMAGE_HEIGHT);
        }

        assertEquals(EnumSet.allOf(DataType.class), types);

        OcrMatch email = matchOf(matches, DataType.EMAIL);
        assertTrue(email.getBounds().contains(160, 21));
        assertFalse(email.getBounds().contains(37, 21));

        OcrMatch phone = matchOf(matches, DataType.PHONE);
        assertTrue(phone.getBounds().contains(27, 66));
        assertTrue(phone.getBounds().contains(129, 66));

        OcrMatch aadhaar = matchOf(matches, DataType.AADHAAR);
        assertTrue(aadhaar.getBounds().contains(100, 156));
        assertTrue(aadhaar.getBounds().contains(200, 156));
    }

    @Test
    void lowConfidenceWordsAreIgnored() throws Exception {
        List<OcrWord> words = new ArrayList<OcrWord>();
        words.add(word("person@example.com", 10, 10, 170, 22, 5));

        assertTrue(service(words).findSensitive(image()).isEmpty());
    }

    @Test
    void ordinaryWordsHaveNoMatches() throws Exception {
        List<OcrWord> words = new ArrayList<OcrWord>();
        words.add(word("Meeting", 10, 10, 70, 22, 95));
        words.add(word("at", 90, 10, 20, 22, 95));
        words.add(word("10:30", 120, 10, 50, 22, 95));

        assertTrue(service(words).findSensitive(image()).isEmpty());
    }

    @Test
    void engineFailureIsReported() {
        OcrEngine failing = new OcrEngine() {
            @Override
            public List<OcrWord> readWords(BufferedImage image) throws IOException {
                throw new IOException("Tesseract is not installed");
            }
        };

        IOException error = assertThrows(IOException.class,
                () -> new OcrTextService(failing).findSensitive(image()));

        assertTrue(error.getMessage().contains("Tesseract"));
    }

    @Test
    void missingFilesAndNullsAreRejected(@TempDir Path temp) {
        OcrTextService service = service(new ArrayList<OcrWord>());

        assertThrows(IOException.class,
                () -> service.findSensitive(temp.resolve("missing.png").toFile()));
        assertThrows(IllegalArgumentException.class,
                () -> service.findSensitive((File) null));
        assertThrows(IllegalArgumentException.class,
                () -> service.findSensitive((BufferedImage) null));
        assertThrows(IllegalArgumentException.class, () -> new OcrTextService(null));
    }

    private static OcrTextService service(List<OcrWord> words) {
        final List<OcrWord> copy = new ArrayList<OcrWord>(words);

        return new OcrTextService(new OcrEngine() {
            @Override
            public List<OcrWord> readWords(BufferedImage image) {
                return copy;
            }
        });
    }

    private static BufferedImage image() {
        return new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
    }

    private static OcrWord word(String text, int x, int y, int width, int height,
                                float confidence) {
        return new OcrWord(text, new Rectangle(x, y, width, height), confidence);
    }

    private static OcrMatch matchOf(List<OcrMatch> matches, DataType type) {
        for (OcrMatch match : matches) {
            if (match.getType() == type) {
                return match;
            }
        }

        throw new AssertionError("No match of type " + type);
    }
}
