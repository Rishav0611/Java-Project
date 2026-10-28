package com.privacyshield;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Replaces every detected value with a fake value that keeps the same
 * length and format, and builds the fake-to-real map.
 *
 * The scanner from PrivacyShield decides which values are sensitive and
 * removes overlapping matches before anything is replaced.
 */
public class Masker {
    private static final int MAX_FAKE_ATTEMPTS = 50;

    private final PrivacyShield privacyShield;
    private final FakeValueGenerator generator;

    public Masker() {
        this(new PrivacyShield());
    }

    public Masker(PrivacyShield privacyShield) {
        this(privacyShield, new DefaultFakeValueGenerator());
    }

    public Masker(PrivacyShield privacyShield, FakeValueGenerator generator) {
        if (privacyShield == null) {
            throw new IllegalArgumentException("privacyShield cannot be null");
        }
        if (generator == null) {
            throw new IllegalArgumentException("generator cannot be null");
        }

        this.privacyShield = privacyShield;
        this.generator = generator;
    }

    public MaskResult mask(String text) {
        if (text == null) {
            return new MaskResult(null, Collections.<String, String>emptyMap(),
                    Collections.<Detection>emptyList());
        }

        List<Detection> detections = privacyShield.detect(text);

        if (detections.isEmpty()) {
            return new MaskResult(text, Collections.<String, String>emptyMap(),
                    Collections.<Detection>emptyList());
        }

        Set<String> realValues = new HashSet<String>();

        for (Detection detection : detections) {
            realValues.add(detection.getValue());
        }

        Set<String> usedFakes = new HashSet<String>();
        Map<String, String> realToFake = new HashMap<String, String>();
        Map<String, String> fakeToReal = new LinkedHashMap<String, String>();

        // The same real value always gets the same fake.
        for (Detection detection : detections) {
            String real = detection.getValue();

            if (!realToFake.containsKey(real)) {
                String fake = makeFake(detection.getType(), real, realValues, usedFakes, text);

                realToFake.put(real, fake);
                fakeToReal.put(fake, real);
                usedFakes.add(fake);
            }
        }

        StringBuilder masked = new StringBuilder(text);

        // Replace from right to left so earlier character positions stay the same.
        for (int i = detections.size() - 1; i >= 0; i--) {
            Detection detection = detections.get(i);
            masked.replace(
                    detection.getStart(),
                    detection.getEnd(),
                    realToFake.get(detection.getValue())
            );
        }

        return new MaskResult(masked.toString(), fakeToReal, detections);
    }

    private String makeFake(DataType type, String real, Set<String> realValues,
                            Set<String> usedFakes, String text) {
        String fake = generator.generate(type, real);

        int attempts = 0;

        // Avoid fakes that clash with another real or fake value, or with text
        // that is already in the message, because unmask would replace those too.
        while (isClashing(fake, real, realValues, usedFakes, text)
                && attempts < MAX_FAKE_ATTEMPTS) {
            fake = generator.generate(type, real);
            attempts++;
        }

        return fake;
    }

    private boolean isClashing(String fake, String real, Set<String> realValues,
                               Set<String> usedFakes, String text) {
        return fake.equals(real)
                || usedFakes.contains(fake)
                || realValues.contains(fake)
                || text.contains(fake);
    }
}
