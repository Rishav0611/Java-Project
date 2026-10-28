package com.privacyshield;

import java.awt.Rectangle;

/**
 * One QR code found in a photo.
 *
 * The decoded text is kept in memory only; the window does not show it. The
 * bounds use image pixels and are used to paint a black box over the code.
 */
public final class QrCodeResult {
    private final String text;
    private final Rectangle bounds;

    public QrCodeResult(String text, Rectangle bounds) {
        if (bounds == null) {
            throw new IllegalArgumentException("bounds cannot be null");
        }

        this.text = text == null ? "" : text;
        this.bounds = new Rectangle(bounds);
    }

    public String getText() {
        return text;
    }

    public Rectangle getBounds() {
        return new Rectangle(bounds);
    }

    @Override
    public String toString() {
        return "QR code at " + bounds.x + "," + bounds.y
                + " (" + bounds.width + " x " + bounds.height + ")";
    }
}
