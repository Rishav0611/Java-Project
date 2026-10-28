package com.privacyshield;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for masking, unmasking and the fake-to-real map.
 */
class MaskingTest {

    private static final String SAMPLE =
            "My email is person@example.com and my phone is +91 98765 43210. "
                    + "PAN: ABCPD1234E. Aadhaar: 2345 6789 0124.";

    private static final String[] REAL_VALUES = {
            "person@example.com",
            "+91 98765 43210",
            "ABCPD1234E",
            "2345 6789 0124"
    };

    private final PrivacyShield privacyShield = new PrivacyShield();

    @Test
    void unmaskOfMaskGivesBackTheOriginalText() {
        MaskResult result = privacyShield.mask(SAMPLE);
        String restored = result.unmask(result.getMaskedText());

        assertEquals(SAMPLE, restored);
        assertEquals(SAMPLE, privacyShield.unmask(result.getMaskedText(), result.getFakeToReal()));
    }

    @Test
    void maskedTextHidesEveryRealValue() {
        String masked = privacyShield.mask(SAMPLE).getMaskedText();

        assertNotEquals(SAMPLE, masked);

        for (String real : REAL_VALUES) {
            assertFalse(masked.contains(real), "masked text still contains " + real);
        }
    }

    @Test
    void maskedValuesStillLookLikeTheSameDataTypes() {
        MaskResult result = privacyShield.mask(SAMPLE);
        List<Detection> found = privacyShield.detect(result.getMaskedText());

        assertEquals(4, found.size());
        assertEquals(DataType.EMAIL, found.get(0).getType());
        assertEquals(DataType.PHONE, found.get(1).getType());
        assertEquals(DataType.PAN, found.get(2).getType());
        assertEquals(DataType.AADHAAR, found.get(3).getType());
    }

    @Test
    void buildsAFakeToRealMap() {
        MaskResult result = privacyShield.mask(SAMPLE);
        Map<String, String> map = result.getFakeToReal();

        assertEquals(4, map.size());
        assertEquals(4, result.getDetections().size());

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String fake = entry.getKey();
            String real = entry.getValue();

            assertNotEquals(real, fake);
            assertTrue(result.getMaskedText().contains(fake), fake);
            assertTrue(SAMPLE.contains(real), real);
        }
    }

    @Test
    void theSameRealValueAlwaysGetsTheSameFake() {
        String text = "Mail a@b.com now, and again a@b.com.";
        MaskResult result = privacyShield.mask(text);

        assertEquals(1, result.getFakeToReal().size());

        String fake = result.getFakeToReal().keySet().iterator().next();

        assertEquals(2, countOccurrences(result.getMaskedText(), fake));
        assertEquals(text, result.unmask(result.getMaskedText()));
    }

    @Test
    void differentValuesGetDifferentFakes() {
        String text = "First a@b.com then c@d.com.";
        MaskResult result = privacyShield.mask(text);

        assertEquals(2, result.getFakeToReal().size());
        assertEquals(text, result.unmask(result.getMaskedText()));
    }

    @Test
    void cleanTextIsReturnedUnchanged() {
        MaskResult result = privacyShield.mask("hello world");

        assertEquals("hello world", result.getMaskedText());
        assertTrue(result.getFakeToReal().isEmpty());
        assertTrue(result.getDetections().isEmpty());
        assertEquals("hello world", result.unmask("hello world"));
    }

    @Test
    void handlesNullAndEmptyText() {
        MaskResult nullResult = privacyShield.mask(null);

        assertNull(nullResult.getMaskedText());
        assertTrue(nullResult.getFakeToReal().isEmpty());
        assertTrue(nullResult.getDetections().isEmpty());
        assertNull(nullResult.unmask(null));
        assertNull(privacyShield.unmask(null, nullResult.getFakeToReal()));

        MaskResult emptyResult = privacyShield.mask("");

        assertEquals("", emptyResult.getMaskedText());
        assertEquals("", emptyResult.unmask(""));
    }

    @Test
    void unmaskWithoutMapReturnsTheText() {
        assertEquals("hello", privacyShield.unmask("hello", null));
        assertEquals("hello", privacyShield.unmask("hello",
                Collections.<String, String>emptyMap()));
    }

    @Test
    void mapAndDetectionListAreReadOnly() {
        final MaskResult result = privacyShield.mask(SAMPLE);

        assertThrows(UnsupportedOperationException.class,
                () -> result.getFakeToReal().put("a", "b"));
        assertThrows(UnsupportedOperationException.class,
                () -> result.getDetections().add(null));
    }

    @Test
    void maskerRejectsNullParts() {
        assertThrows(IllegalArgumentException.class, () -> new Masker(null));
        assertThrows(IllegalArgumentException.class,
                () -> new Masker(new PrivacyShield(), null));
    }

    @Test
    void usesTheGivenFakeValueGenerator() {
        FakeValueGenerator fixed = new FakeValueGenerator() {
            @Override
            public String generate(DataType type, String realValue) {
                return "fake_" + type;
            }

            @Override
            public String generateWord(String realValue) {
                return "fake_word";
            }
        };

        MaskResult result = new Masker(new PrivacyShield(), fixed).mask("Mail a@b.com");

        assertEquals("Mail fake_EMAIL", result.getMaskedText());
        assertEquals("a@b.com", result.getFakeToReal().get("fake_EMAIL"));
        assertEquals("Mail a@b.com", result.unmask(result.getMaskedText()));
    }

    @Test
    void roundTripWorksForSpacedAndHyphenatedValues() {
        String text = "a@b.co.in, 9876-543-210, 9876 5432 1012, abcpd1234e";
        MaskResult result = privacyShield.mask(text);

        assertEquals(4, result.getDetections().size());
        assertEquals(text, result.unmask(result.getMaskedText()));
    }

    private int countOccurrences(String text, String part) {
        int count = 0;
        int index = text.indexOf(part);

        while (index >= 0) {
            count++;
            index = text.indexOf(part, index + part.length());
        }

        return count;
    }
}
