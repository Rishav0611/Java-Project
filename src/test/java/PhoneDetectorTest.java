package com.privacyshield.detect;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneDetectorTest {

    private final PhoneDetector detector = new PhoneDetector();

    @Test
    void findsPlainTenDigitNumber() {
        List<Detection> result = detector.detect("Call 9876543210 now");

        assertEquals(1, result.size());
        assertEquals("9876543210", result.get(0).value());
        assertEquals(5, result.get(0).start());
        assertEquals(15, result.get(0).end());
    }
}