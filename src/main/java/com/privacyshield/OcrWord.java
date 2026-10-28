package com.privacyshield;

import java.awt.Rectangle;

/**
 * One word read from a photo by an OCR engine.
 */
public final class OcrWord {
    private final String text;
    private final Rectangle bounds;
    private final float confidence;

    public OcrWord(String text, Rectangle bounds, float confidence) {
        if (bounds == null) {
            throw new IllegalArgumentException("bounds cannot be null");
        }

        this.text = text == null ? "" : text.trim();
        this.bounds = new Rectangle(bounds);
        this.confidence = confidence;
    }

    public String getText() {
        return text;
    }

    public Rectangle getBounds() {
        return new Rectangle(bounds);
    }

    public float getConfidence() {
        return confidence;
    }
}
