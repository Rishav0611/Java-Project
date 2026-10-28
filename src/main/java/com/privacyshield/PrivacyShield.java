package com.privacyshield;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class PrivacyShield {
    private static final String REDACTION_MARKER = "[REDACTED]";

    private final List<Detector> detectors;

    public PrivacyShield() {
        this(Arrays.asList(
                new AadhaarDetector(),
                new PanDetector(),
                new PhoneDetector(),
                new EmailDetector()
        ));
    }

    public PrivacyShield(List<Detector> detectors) {
        if (detectors == null) {
            throw new IllegalArgumentException("detectors cannot be null");
        }

        this.detectors = new ArrayList<>();

        for (Detector detector : detectors) {
            if (detector == null) {
                throw new IllegalArgumentException("detectors cannot contain null");
            }
            this.detectors.add(detector);
        }
    }

    public List<Detection> detect(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        List<Detection> matches = new ArrayList<>();

        for (Detector detector : detectors) {
            List<Detection> found = detector.detect(text);

            if (found != null) {
                for (Detection detection : found) {
                    if (detection != null) {
                        matches.add(detection);
                    }
                }
            }
        }

        // Put matches in text order. For the same start, keep the longer match first.
        Collections.sort(matches, new Comparator<Detection>() {
            @Override
            public int compare(Detection left, Detection right) {
                if (left.getStart() != right.getStart()) {
                    return Integer.compare(left.getStart(), right.getStart());
                }
                return Integer.compare(right.getEnd(), left.getEnd());
            }
        });

        // Keep the first match when two detector results overlap.
        List<Detection> nonOverlapping = new ArrayList<>();
        int lastEnd = -1;

        for (Detection detection : matches) {
            int start = detection.getStart();
            int end = detection.getEnd();

            if (start < 0 || end <= start || end > text.length()) {
                continue;
            }

            if (start >= lastEnd) {
                nonOverlapping.add(detection);
                lastEnd = end;
            }
        }

        return Collections.unmodifiableList(nonOverlapping);
    }

    public String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        List<Detection> detections = detect(text);
        StringBuilder redacted = new StringBuilder(text);

        // Replace from right to left so earlier character positions stay the same.
        for (int i = detections.size() - 1; i >= 0; i--) {
            Detection detection = detections.get(i);
            redacted.replace(detection.getStart(), detection.getEnd(), REDACTION_MARKER);
        }

        return redacted.toString();
    }

    public String sanitize(String text) {
        return redact(text);
    }

    /**
     * Replaces every detected value with a fake value of the same length and
     * format, and returns the masked text together with the fake-to-real map.
     */
    public MaskResult mask(String text) {
        return new Masker(this).mask(text);
    }

    /**
     * Puts the real values back into a text that contains fakes from mask.
     */
    public String unmask(String text, Map<String, String> fakeToReal) {
        return new Unmasker().unmask(text, fakeToReal);
    }
}

