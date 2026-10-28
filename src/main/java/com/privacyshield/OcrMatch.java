package com.privacyshield;

import java.awt.Rectangle;

/**
 * A sensitive value found by OCR, together with the photo area it occupies so
 * the window can cover it with a black box.
 */
public final class OcrMatch {
    private final DataType type;
    private final String text;
    private final Rectangle bounds;

    public OcrMatch(DataType type, String text, Rectangle bounds) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (bounds == null) {
            throw new IllegalArgumentException("bounds cannot be null");
        }

        this.type = type;
        this.text = text == null ? "" : text;
        this.bounds = new Rectangle(bounds);
    }

    public DataType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public Rectangle getBounds() {
        return new Rectangle(bounds);
    }
}
