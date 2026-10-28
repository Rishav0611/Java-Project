package com.privacyshield;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link AadhaarDetector}.
 *
 * 234567890124 is a made-up number whose last digit is a valid Verhoeff check digit.
 */
class AadhaarDetectorTest {

    private final AadhaarDetector detector = new AadhaarDetector();

    @Test
    void findsPlainTwelveDigitNumber() {
        List<Detection> found = detector.detect("Aadhaar 234567890124 done");

        assertEquals(1, found.size());
        assertEquals(DataType.AADHAAR, found.get(0).getType());
        assertEquals("234567890124", found.get(0).getValue());
        assertEquals(8, found.get(0).getStart());
        assertEquals(20, found.get(0).getEnd());
    }

    @Test
    void findsNumberWrittenWithSpaces() {
        List<Detection> found = detector.detect("ID: 2345 6789 0124");

        assertEquals(1, found.size());
        assertEquals("2345 6789 0124", found.get(0).getValue());
        assertEquals(4, found.get(0).getStart());
        assertEquals(18, found.get(0).getEnd());
    }

    @Test
    void findsNumberWrittenWithHyphens() {
        List<Detection> found = detector.detect("ID: 2345-6789-0124");

        assertEquals(1, found.size());
        assertEquals("2345-6789-0124", found.get(0).getValue());
    }

    @Test
    void findsNumberWithMixedSeparators() {
        List<Detection> found = detector.detect("2345 6789-0124");

        assertEquals(1, found.size());
        assertEquals("2345 6789-0124", found.get(0).getValue());
    }

    @Test
    void findsNumberAtEndOfSentence() {
        List<Detection> found = detector.detect("id is 234567890124.");

        assertEquals(1, found.size());
        assertEquals("234567890124", found.get(0).getValue());
    }

    @Test
    void findsTwoNumbersInOneText() {
        List<Detection> found = detector.detect("234567890124 and 9876 5432 1012");

        assertEquals(2, found.size());
        assertEquals("234567890124", found.get(0).getValue());
        assertEquals("9876 5432 1012", found.get(1).getValue());
    }

    @Test
    void rejectsWrongChecksum() {
        // Same number with the last digit changed on purpose.
        assertTrue(detector.detect("234567890125").isEmpty());
    }

    @Test
    void rejectsNumberStartingWithZeroOrOne() {
        // 123456789010 has a valid Verhoeff check digit, but Aadhaar numbers start with 2-9.
        assertTrue(Verhoeff.isValid("123456789010"));
        assertTrue(detector.detect("123456789010").isEmpty());
    }

    @Test
    void rejectsWrongLength() {
        assertTrue(detector.detect("23456789012").isEmpty());
        assertTrue(detector.detect("2345678901245").isEmpty());
    }

    @Test
    void rejectsNumberInsideLongerDigitRun() {
        assertTrue(detector.detect("9999999999999").isEmpty());
        assertTrue(detector.detect("02345678901240").isEmpty());
    }

    @Test
    void handlesNullAndEmptyText() {
        assertTrue(detector.detect(null).isEmpty());
        assertTrue(detector.detect("").isEmpty());
    }
}
