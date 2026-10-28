package com.privacyshield;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A small detector self-check on made-up samples.
 *
 * The samples are built into the program, contain no real person's data, and
 * cover each detector plus a clean sample. The dashboard shows the resulting
 * recall and precision.
 */
public final class DetectionBenchmark {
    private static final List<Sample> SAMPLES = Collections.unmodifiableList(Arrays.asList(
            new Sample("Email person@example.com", expected(DataType.EMAIL)),
            new Sample("Phone +91 98765 43210", expected(DataType.PHONE)),
            new Sample("Aadhaar 2345 6789 0124", expected(DataType.AADHAAR)),
            new Sample("PAN ABCPD1234E", expected(DataType.PAN)),
            new Sample("Reach person@example.com or 9876543210. "
                            + "Aadhaar 2345 6789 0124, PAN ABCPD1234E.",
                    expected(DataType.EMAIL, DataType.PHONE,
                            DataType.AADHAAR, DataType.PAN)),
            new Sample("Reference 123456789012 and ABCDE1234F.", expected())
    ));

    private DetectionBenchmark() {
    }

    public static Result run() {
        return run(new PrivacyShield());
    }

    public static Result run(PrivacyShield privacyShield) {
        if (privacyShield == null) {
            throw new IllegalArgumentException("privacyShield cannot be null");
        }

        int expectedValues = 0;
        int foundValues = 0;
        int correctValues = 0;
        int missedValues = 0;
        int falsePositives = 0;

        for (Sample sample : SAMPLES) {
            Map<DataType, Integer> found = counts();

            for (Detection detection : privacyShield.detect(sample.text)) {
                if (detection.getType() != null) {
                    found.put(detection.getType(), found.get(detection.getType()) + 1);
                }
            }

            for (DataType type : DataType.values()) {
                int expectedCount = sample.expected.get(type);
                int foundCount = found.get(type);

                expectedValues += expectedCount;
                foundValues += foundCount;
                correctValues += Math.min(expectedCount, foundCount);
                missedValues += Math.max(0, expectedCount - foundCount);
                falsePositives += Math.max(0, foundCount - expectedCount);
            }
        }

        return new Result(SAMPLES.size(), expectedValues, foundValues, correctValues,
                missedValues, falsePositives);
    }

    private static Map<DataType, Integer> expected(DataType... types) {
        Map<DataType, Integer> counts = counts();

        for (DataType type : types) {
            counts.put(type, counts.get(type) + 1);
        }

        return counts;
    }

    private static Map<DataType, Integer> counts() {
        Map<DataType, Integer> counts = new EnumMap<DataType, Integer>(DataType.class);

        for (DataType type : DataType.values()) {
            counts.put(type, 0);
        }

        return counts;
    }

    private static final class Sample {
        private final String text;
        private final Map<DataType, Integer> expected;

        private Sample(String text, Map<DataType, Integer> expected) {
            this.text = text;
            this.expected = expected;
        }
    }

    /**
     * The numbers shown on the dashboard.
     */
    public static final class Result {
        private final int samples;
        private final int expectedValues;
        private final int foundValues;
        private final int correctValues;
        private final int missedValues;
        private final int falsePositives;

        private Result(int samples, int expectedValues, int foundValues,
                       int correctValues, int missedValues, int falsePositives) {
            this.samples = samples;
            this.expectedValues = expectedValues;
            this.foundValues = foundValues;
            this.correctValues = correctValues;
            this.missedValues = missedValues;
            this.falsePositives = falsePositives;
        }

        public int getSamples() {
            return samples;
        }

        public int getExpectedValues() {
            return expectedValues;
        }

        public int getFoundValues() {
            return foundValues;
        }

        public int getCorrectValues() {
            return correctValues;
        }

        public int getMissedValues() {
            return missedValues;
        }

        public int getFalsePositives() {
            return falsePositives;
        }

        public double getPrecision() {
            return foundValues == 0 ? 1.0 : (double) correctValues / foundValues;
        }

        public double getRecall() {
            return expectedValues == 0 ? 1.0 : (double) correctValues / expectedValues;
        }

        public double getAccuracy() {
            int denominator = expectedValues + falsePositives;
            return denominator == 0 ? 1.0 : (double) correctValues / denominator;
        }
    }
}
