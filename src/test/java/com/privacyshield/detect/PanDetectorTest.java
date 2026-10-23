package com.privacyshield.detect;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanDetectorTest {

    private final PanDetector detector = new PanDetector();

    @Test
    void findsValidPan() {
        List<Detection> result = detector.detect("PAN ABCPD1234E hai");

        assertEquals(1, result.size());
        assertEquals("ABCPD1234E", result.get(0).value());
        assertEquals(4, result.get(0).start());
        assertEquals(14, result.get(0).end());
    }

    @Test
    void ignoresInvalidFourthLetter() {
        assertTrue(detector.detect("ABCXD1234E").isEmpty());
    }

    @Test
    void ignoresPanInsideLongerWord() {
        assertTrue(detector.detect("XABCPD1234EY").isEmpty());
    }

    @Test
    void ignoresLowercase() {
        assertTrue(detector.detect("abcpd1234e").isEmpty());
    }

    @Test
    void findsTwoPans() {
        List<Detection> result = detector.detect("ABCPD1234E aur FGHCI5678K");

        assertEquals(2, result.size());
    }
}