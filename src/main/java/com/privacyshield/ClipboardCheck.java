package com.privacyshield;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One look at the clipboard.
 *
 * Holds the text that was seen, any personal values detected in it, and
 * whether the text was replaced with a redacted version.
 */
public final class ClipboardCheck {
    private final String text;
    private final List<Detection> detections;
    private final boolean cleaned;

    public ClipboardCheck(String text, List<Detection> detections, boolean cleaned) {
        if (detections == null) {
            throw new IllegalArgumentException("detections cannot be null");
        }

        this.text = text == null ? "" : text;
        this.detections = Collections.unmodifiableList(new ArrayList<Detection>(detections));
        this.cleaned = cleaned;
    }

    public String getText() {
        return text;
    }

    public List<Detection> getDetections() {
        return detections;
    }

    public boolean isCleaned() {
        return cleaned;
    }

    public boolean hasSensitiveData() {
        return !detections.isEmpty();
    }

    public int getValueCount() {
        return detections.size();
    }
}
