package com.privacyshield;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * A source of words and boxes for OCR. The window uses Tess4J, while tests can
 * provide a small fake engine and run without the native Tesseract library.
 */
public interface OcrEngine {
    List<OcrWord> readWords(BufferedImage image) throws IOException;
}
