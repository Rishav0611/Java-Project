package com.privacyshield;

import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.util.List;

/**
 * Paints the coloured highlight boxes for detected values in a text pane.
 *
 * The Text tab, the clipboard preview and any other preview can use it, so a
 * detected value always looks the same.
 */
public final class TextHighlighter {
    private TextHighlighter() {
        // Only static helpers are needed.
    }

    /**
     * Clears old colours and highlights each detection in its type colour.
     */
    public static void apply(JTextPane pane, List<Detection> detections) {
        if (pane == null) {
            throw new IllegalArgumentException("pane cannot be null");
        }

        StyledDocument document = pane.getStyledDocument();

        SimpleAttributeSet normal = new SimpleAttributeSet();
        StyleConstants.setBackground(normal, pane.getBackground());
        StyleConstants.setForeground(normal, pane.getForeground());
        document.setCharacterAttributes(0, document.getLength(), normal, true);

        if (detections == null) {
            return;
        }

        for (Detection detection : detections) {
            int start = detection.getStart();
            int end = detection.getEnd();

            if (start < 0 || end <= start || end > document.getLength()) {
                continue;
            }

            SimpleAttributeSet attributes = new SimpleAttributeSet();
            StyleConstants.setBackground(attributes, HighlightPalette.background(detection.getType()));
            StyleConstants.setForeground(attributes, HighlightPalette.foreground(detection.getType()));
            document.setCharacterAttributes(start, end - start, attributes, true);
        }
    }
}
