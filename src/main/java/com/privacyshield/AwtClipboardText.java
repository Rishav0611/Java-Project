package com.privacyshield;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * The real system clipboard, using only the standard java.awt classes.
 *
 * Only plain text is read and written. Pictures and files on the clipboard
 * are ignored.
 */
public class AwtClipboardText implements ClipboardText {
    private final Clipboard clipboard;

    public AwtClipboardText() {
        this(Toolkit.getDefaultToolkit().getSystemClipboard());
    }

    public AwtClipboardText(Clipboard clipboard) {
        if (clipboard == null) {
            throw new IllegalArgumentException("clipboard cannot be null");
        }

        this.clipboard = clipboard;
    }

    @Override
    public String read() {
        try {
            if (!clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                return "";
            }

            Object data = clipboard.getData(DataFlavor.stringFlavor);
            return data == null ? "" : data.toString();
        } catch (IllegalStateException exception) {
            throw new IllegalStateException("Could not read the clipboard. Try again.", exception);
        } catch (UnsupportedFlavorException exception) {
            return "";
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read the clipboard. Try again.", exception);
        }
    }

    @Override
    public void write(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null");
        }

        try {
            StringSelection selection = new StringSelection(text);
            clipboard.setContents(selection, selection);
        } catch (IllegalStateException exception) {
            throw new IllegalStateException("Could not write to the clipboard. Try again.", exception);
        }
    }
}
