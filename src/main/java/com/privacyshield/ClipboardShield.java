package com.privacyshield;

/**
 * Checks text on the clipboard and can replace personal values with
 * [REDACTED].
 *
 * Redaction is used instead of fake values on purpose: the redacted text has
 * no detectable values, so watching the clipboard cannot keep cleaning its own
 * output. Nothing is stored on disk. Use the Text tab when a reply has to be
 * restored with fake values.
 */
public class ClipboardShield {
    private final PrivacyShield privacyShield;
    private final ClipboardText clipboard;

    public ClipboardShield() {
        this(new PrivacyShield(), new AwtClipboardText());
    }

    public ClipboardShield(PrivacyShield privacyShield, ClipboardText clipboard) {
        if (privacyShield == null) {
            throw new IllegalArgumentException("privacyShield cannot be null");
        }
        if (clipboard == null) {
            throw new IllegalArgumentException("clipboard cannot be null");
        }

        this.privacyShield = privacyShield;
        this.clipboard = clipboard;
    }

    /**
     * Reads the clipboard and reports what it found, without changing it.
     */
    public ClipboardCheck inspect() {
        String text = clipboard.read();
        return new ClipboardCheck(text, privacyShield.detect(text), false);
    }

    /**
     * Reads the clipboard, and when personal data is found, writes a version
     * with [REDACTED] back to it. Returns the new clipboard state.
     */
    public ClipboardCheck clean() {
        ClipboardCheck found = inspect();

        if (!found.hasSensitiveData()) {
            return found;
        }

        String redacted = privacyShield.redact(found.getText());
        clipboard.write(redacted);
        return new ClipboardCheck(redacted, found.getDetections(), true);
    }
}
