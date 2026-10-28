package com.privacyshield;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The result of masking a text.
 *
 * It keeps the masked text, the detections that were found, and the
 * fake-to-real map that is needed to put the real values back later.
 */
public class MaskResult {
    private final String maskedText;
    private final Map<String, String> fakeToReal;
    private final List<Detection> detections;

    public MaskResult(String maskedText, Map<String, String> fakeToReal, List<Detection> detections) {
        if (fakeToReal == null) {
            throw new IllegalArgumentException("fakeToReal cannot be null");
        }
        if (detections == null) {
            throw new IllegalArgumentException("detections cannot be null");
        }

        this.maskedText = maskedText;
        this.fakeToReal = Collections.unmodifiableMap(new LinkedHashMap<String, String>(fakeToReal));
        this.detections = Collections.unmodifiableList(new ArrayList<Detection>(detections));
    }

    public String getMaskedText() {
        return maskedText;
    }

    /**
     * Returns the fake-to-real map. The old values can be restored with unmask.
     */
    public Map<String, String> getFakeToReal() {
        return fakeToReal;
    }

    public List<Detection> getDetections() {
        return detections;
    }

    /**
     * Puts the real values back into a text that contains the fakes.
     */
    public String unmask(String text) {
        return new Unmasker().unmask(text, fakeToReal);
    }
}
