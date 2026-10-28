package com.privacyshield;

/**
 * How the GUI hides personal values.
 *
 * FAKE   replaces each value with a fake value that keeps its length and format.
 * REDACT replaces each value with a fixed marker and cannot be undone.
 */
public enum MaskMode {
    FAKE("Fake values"),
    REDACT("[REDACTED]");

    private final String label;

    MaskMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static MaskMode getDefault() {
        return FAKE;
    }

    @Override
    public String toString() {
        return label;
    }
}
