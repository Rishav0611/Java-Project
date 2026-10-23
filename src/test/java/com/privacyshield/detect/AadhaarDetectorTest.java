package com.privacyshield.detect;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AadhaarDetectorTest {

    private final AadhaarDetector detector = new AadhaarDetector();

    @Test
    void findsValidPlainAadhaar() {
        List<Detection> result = detector.detect("ID 234567890124 hai");

        assertEquals(1, result.size());
        assertEquals("234567890124", result.get(0).value());
        assertEquals(3, result.get(0).start());
        assertEquals(15, result.get(0).end());
    }

    @Test
    void findsAadhaarWithSpaces() {
        List<Detection> result = detector.detect("ID 2345 6789 0124 hai");

        assertEquals(1, result.size());
        assertEquals("2345 6789 0124", result.get(0).value());
    }

    @Test
    void findsAadhaarWithHyphens() {
        assertEquals(1, detector.detect("2345-6789-0124").size());
    }

    @Test
    void ignoresWrongChecksum() {
        assertTrue(detector.detect("ID 234567890125").isEmpty());
    }

    @Test
    void ignoresNumberStartingWithOne() {
        assertTrue(detector.detect("134567890124").isEmpty());
    }

    @Test
    void ignoresLongerDigitRun() {
        assertTrue(detector.detect("2345678901245").isEmpty());
    }
}