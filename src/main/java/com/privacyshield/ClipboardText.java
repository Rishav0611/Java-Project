package com.privacyshield;

/**
 * Plain-text clipboard operations.
 *
 * The interface keeps the clipboard shield testable: the GUI uses the real
 * system clipboard, while tests use a simple fake.
 */
public interface ClipboardText {

    /**
     * Returns the current clipboard text, or an empty string when there is no
     * text on the clipboard.
     */
    String read();

    /**
     * Puts text on the clipboard, replacing the old contents.
     */
    void write(String text);
}
