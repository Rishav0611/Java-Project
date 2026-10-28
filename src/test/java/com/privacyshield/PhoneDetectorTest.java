package com.privacyshield;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PhoneDetector}.
 */
class PhoneDetectorTest {

    private final PhoneDetector detector = new PhoneDetector();

    @Test
    void findsTenDigitNumber() {
        List<Detection> found = detector.detect("call 9876543210 now");

        assertEquals(1, found.size());
        assertEquals(DataType.PHONE, found.get(0).getType());
        assertEquals("9876543210", found.get(0).getValue());
        assertEquals(5, found.get(0).getStart());
        assertEquals(15, found.get(0).getEnd());
    }

    @Test
    void findsNumberWithCountryCode() {
        List<Detection> found = detector.detect("+91 9876543210");

        assertEquals(1, found.size());
        assertEquals("+91 9876543210", found.get(0).getValue());
        assertEquals(0, found.get(0).getStart());
        assertEquals(14, found.get(0).getEnd());
    }

    @Test
    void findsNumberWithCountryCodeAndHyphen() {
        List<Detection> found = detector.detect("+91-9876543210");

        assertEquals(1, found.size());
        assertEquals("+91-9876543210", found.get(0).getValue());
    }

    @Test
    void findsNumberWithCountryCodeAndNoSeparator() {
        List<Detection> found = detector.detect("+919876543210");

        assertEquals(1, found.size());
        assertEquals("+919876543210", found.get(0).getValue());
    }

    @Test
    void findsNumberWithSpacesOrHyphensBetweenDigits() {
        assertEquals(1, detector.detect("98765 43210").size());
        assertEquals(1, detector.detect("9876-543-210").size());
        assertEquals(1, detector.detect("+91 98765 43210").size());
    }

    @Test
    void findsNumberAtEndOfSentence() {
        List<Detection> found = detector.detect("call 9876543210.");

        assertEquals(1, found.size());
        assertEquals("9876543210", found.get(0).getValue());
    }

    @Test
    void findsTwoNumbersInOneText() {
        List<Detection> found = detector.detect("9876543210 and 8123456789");

        assertEquals(2, found.size());
        assertEquals("9876543210", found.get(0).getValue());
        assertEquals(15, found.get(1).getStart());
        assertEquals("8123456789", found.get(1).getValue());
    }

    @Test
    void acceptsAllStartingDigitsSixToNine() {
        assertEquals(1, detector.detect("6123456789").size());
        assertEquals(1, detector.detect("7123456789").size());
        assertEquals(1, detector.detect("8123456789").size());
        assertEquals(1, detector.detect("9123456789").size());
    }

    @Test
    void rejectsNumberStartingBelowSix() {
        assertTrue(detector.detect("5123456789").isEmpty());
        assertTrue(detector.detect("4123456789").isEmpty());
    }

    @Test
    void rejectsNumberWithLeadingZero() {
        assertTrue(detector.detect("09876543210").isEmpty());
    }

    @Test
    void rejectsWrongLength() {
        assertTrue(detector.detect("98765432101").isEmpty());
        assertTrue(detector.detect("987654321").isEmpty());
    }

    @Test
    void rejectsNumberGluedToLettersOrDigits() {
        assertTrue(detector.detect("abc9876543210").isEmpty());
        assertTrue(detector.detect("9876543210x").isEmpty());
        assertTrue(detector.detect("199876543210").isEmpty());
    }

    @Test
    void handlesNullAndEmptyText() {
        assertTrue(detector.detect(null).isEmpty());
        assertTrue(detector.detect("").isEmpty());
    }
}
