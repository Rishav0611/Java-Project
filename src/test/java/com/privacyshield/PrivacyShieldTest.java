package com.privacyshield;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the combined scanner and the redaction helpers in {@link PrivacyShield}.
 */
class PrivacyShieldTest {

    private static final String SAMPLE =
            "Mail person@example.com or call 9876543210. PAN ABCPD1234E, Aadhaar 2345 6789 0124.";

    /**
     * A detector that always reports the same position, so overlap rules can be tested directly.
     */
    private static final class FixedDetector implements Detector {
        private final DataType type;
        private final int start;
        private final int end;

        FixedDetector(DataType type, int start, int end) {
            this.type = type;
            this.start = start;
            this.end = end;
        }

        @Override
        public List<Detection> detect(String text) {
            return Collections.singletonList(new Detection(type, "value", start, end));
        }
    }

    @Test
    void detectsEverySupportedTypeInTextOrder() {
        List<Detection> found = new PrivacyShield().detect(SAMPLE);

        assertEquals(4, found.size());
        assertEquals(DataType.EMAIL, found.get(0).getType());
        assertEquals(DataType.PHONE, found.get(1).getType());
        assertEquals(DataType.PAN, found.get(2).getType());
        assertEquals(DataType.AADHAAR, found.get(3).getType());
    }

    @Test
    void reportsOffsetsThatPointAtTheMatchedText() {
        List<Detection> found = new PrivacyShield().detect(SAMPLE);

        assertEquals(5, found.get(0).getStart());
        assertEquals(23, found.get(0).getEnd());
        assertEquals(32, found.get(1).getStart());
        assertEquals(42, found.get(1).getEnd());
        assertEquals(48, found.get(2).getStart());
        assertEquals(58, found.get(2).getEnd());
        assertEquals(68, found.get(3).getStart());
        assertEquals(82, found.get(3).getEnd());

        for (Detection detection : found) {
            assertEquals(
                    detection.getValue(),
                    SAMPLE.substring(detection.getStart(), detection.getEnd())
            );
        }
    }

    @Test
    void keepsTheLongerMatchWhenTwoDetectorsStartAtTheSamePlace() {
        PrivacyShield shield = new PrivacyShield(Arrays.asList(
                new FixedDetector(DataType.PHONE, 0, 5),
                new FixedDetector(DataType.EMAIL, 0, 10)
        ));

        List<Detection> found = shield.detect("abcdefghij");

        assertEquals(1, found.size());
        assertEquals(DataType.EMAIL, found.get(0).getType());
        assertEquals(10, found.get(0).getEnd());
    }

    @Test
    void keepsTheEarlierMatchWhenTwoDetectionsOverlap() {
        PrivacyShield shield = new PrivacyShield(Arrays.asList(
                new FixedDetector(DataType.PHONE, 0, 15),
                new FixedDetector(DataType.EMAIL, 10, 20)
        ));

        List<Detection> found = shield.detect("abcdefghijklmnopqrst");

        assertEquals(1, found.size());
        assertEquals(DataType.PHONE, found.get(0).getType());
        assertEquals(15, found.get(0).getEnd());
    }

    @Test
    void keepsMatchesThatOnlyTouch() {
        PrivacyShield shield = new PrivacyShield(Arrays.asList(
                new FixedDetector(DataType.PHONE, 0, 5),
                new FixedDetector(DataType.EMAIL, 5, 8)
        ));

        assertEquals(2, shield.detect("abcdefgh").size());
    }

    @Test
    void dropsMatchesWithImpossiblePositions() {
        PrivacyShield shield = new PrivacyShield(Arrays.asList(
                new FixedDetector(DataType.PHONE, -1, 4),
                new FixedDetector(DataType.PHONE, 5, 5),
                new FixedDetector(DataType.PHONE, 8, 99)
        ));

        assertTrue(shield.detect("abcdefghij").isEmpty());
    }

    @Test
    void rejectsNullDetectorInput() {
        assertThrows(IllegalArgumentException.class, () -> new PrivacyShield(null));
        assertThrows(IllegalArgumentException.class, () -> new PrivacyShield(Arrays.asList((Detector) null)));
    }

    @Test
    void returnsEmptyListForNullAndEmptyText() {
        PrivacyShield shield = new PrivacyShield();

        assertTrue(shield.detect(null).isEmpty());
        assertTrue(shield.detect("").isEmpty());
    }

    @Test
    void returnsReadOnlyDetectionList() {
        List<Detection> found = new PrivacyShield().detect(SAMPLE);

        assertThrows(UnsupportedOperationException.class, () -> found.add(null));
    }

    @Test
    void redactsEveryDetectedValue() {
        String expected = "Mail [REDACTED] or call [REDACTED]. PAN [REDACTED], Aadhaar [REDACTED].";

        assertEquals(expected, new PrivacyShield().redact(SAMPLE));
    }

    @Test
    void redactsAdjacentMatches() {
        PrivacyShield shield = new PrivacyShield(Arrays.asList(
                new FixedDetector(DataType.EMAIL, 0, 3),
                new FixedDetector(DataType.PHONE, 3, 6)
        ));

        assertEquals("[REDACTED][REDACTED]!", shield.redact("abcdef!"));
    }

    @Test
    void redactsWholeText() {
        PrivacyShield shield = new PrivacyShield(
                Arrays.asList(new FixedDetector(DataType.EMAIL, 0, 3))
        );

        assertEquals("[REDACTED]", shield.redact("abc"));
    }

    @Test
    void leavesCleanTextUnchanged() {
        assertEquals("hello world", new PrivacyShield().redact("hello world"));
    }

    @Test
    void handlesNullAndEmptyTextForRedaction() {
        PrivacyShield shield = new PrivacyShield();

        assertNull(shield.redact(null));
        assertEquals("", shield.redact(""));
        assertNull(shield.sanitize(null));
        assertEquals("", shield.sanitize(""));
    }

    @Test
    void sanitizeBehavesLikeRedact() {
        PrivacyShield shield = new PrivacyShield();

        assertEquals(shield.redact(SAMPLE), shield.sanitize(SAMPLE));
        assertFalse(shield.sanitize("hello").contains("[REDACTED]"));
    }

    @Test
    void keepsTheWholeEmailWhenItsStartLooksLikeAPhoneNumber() {
        List<Detection> found = new PrivacyShield().detect("9876543210@example.com");

        assertEquals(1, found.size());
        assertEquals(DataType.EMAIL, found.get(0).getType());
        assertEquals("9876543210@example.com", found.get(0).getValue());
    }

    @Test
    void usesOnlyTheGivenDetectors() {
        PrivacyShield shield = new PrivacyShield(Arrays.asList(new EmailDetector()));

        List<Detection> found = shield.detect("a@b.com and 9876543210");

        assertEquals(1, found.size());
        assertEquals(DataType.EMAIL, found.get(0).getType());
    }
}
