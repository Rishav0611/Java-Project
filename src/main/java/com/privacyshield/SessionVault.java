package com.privacyshield;

import java.util.Collections;
import java.util.Map;

/**
 * Keeps the current masking result for one GUI session.
 *
 * The fake-to-real map exists only in memory. "Keep" leaves it available for
 * restoring an AI reply, "Delete" throws it away. Both choices reset when a new
 * text is masked, and the default policy is Delete.
 */
public class SessionVault {
    private final PrivacyShield privacyShield;

    private MaskResult current;
    private KeepPolicy policy = KeepPolicy.getDefault();

    public SessionVault() {
        this(new PrivacyShield());
    }

    public SessionVault(PrivacyShield privacyShield) {
        if (privacyShield == null) {
            throw new IllegalArgumentException("privacyShield cannot be null");
        }

        this.privacyShield = privacyShield;
    }

    /**
     * Masks a text with the chosen mode and remembers the result.
     */
    public MaskResult mask(String text, MaskMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode cannot be null");
        }

        MaskResult result;

        if (mode == MaskMode.FAKE) {
            result = privacyShield.mask(text);
        } else {
            result = new MaskResult(
                    privacyShield.redact(text),
                    Collections.<String, String>emptyMap(),
                    privacyShield.detect(text)
            );
        }

        current = result;
        policy = KeepPolicy.getDefault();

        return result;
    }

    /**
     * True when the current mask result has at least one fake value.
     */
    public boolean hasValues() {
        return current != null && !current.getFakeToReal().isEmpty();
    }

    public MaskResult getCurrent() {
        return current;
    }

    public KeepPolicy getPolicy() {
        return policy;
    }

    /**
     * Keeps the current fake-to-real map in memory.
     */
    public void keep() {
        if (current != null) {
            policy = KeepPolicy.KEEP;
        }
    }

    /**
     * Forgets the current fake-to-real map and returned masked text.
     */
    public void delete() {
        current = null;
        policy = KeepPolicy.DELETE;
    }

    /**
     * Puts the real values back into a text, such as an AI reply.
     * If nothing is saved, the text is returned unchanged.
     */
    public String restore(String text) {
        if (current == null) {
            return text;
        }

        return current.unmask(text);
    }

    /**
     * The saved fake-to-real map, or an empty map after Delete.
     */
    public Map<String, String> getSavedValues() {
        if (current == null) {
            return Collections.emptyMap();
        }

        return current.getFakeToReal();
    }
}
