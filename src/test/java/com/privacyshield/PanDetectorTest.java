package com.privacyshield;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PanDetector}.
 *
 * PAN format: three letters, a holder-type letter, one letter, four digits and one letter.
 */
class PanDetectorTest {

    private final PanDetector detector = new PanDetector();

    @Test
    void findsValidPan() {
        List<Detection> found = detector.detect("PAN: ABCPD1234E");

        assertEquals(1, found.size());
        assertEquals(DataType.PAN, found.get(0).getType());
        assertEquals("ABCPD1234E", found.get(0).getValue());
        assertEquals(5, found.get(0).getStart());
        assertEquals(15, found.get(0).getEnd());
    }

    @Test
    void findsEveryValidHolderType() {
        String holderTypes = "PCHFATBLJG";

        for (int i = 0; i < holderTypes.length(); i++) {
            String pan = "ABC" + holderTypes.charAt(i) + "E1234F";
            List<Detection> found = detector.detect("id " + pan);

            assertEquals(1, found.size(), "expected to detect " + pan);
            assertEquals(pan, found.get(0).getValue());
        }
    }

    @Test
    void acceptsLowercaseAndKeepsTheOriginalText() {
        List<Detection> found = detector.detect("pan abcpd1234e");

        assertEquals(1, found.size());
        assertEquals("abcpd1234e", found.get(0).getValue());
    }

    @Test
    void findsPanSurroundedByPunctuation() {
        assertEquals(1, detector.detect("(ABCPD1234E)").size());
        assertEquals(1, detector.detect("ABCPD1234E.").size());
        assertEquals(1, detector.detect("ABCPD1234E, next").size());
    }

    @Test
    void findsTwoPansInOneText() {
        List<Detection> found = detector.detect("ABCPD1234E and XYZPG5678H");

        assertEquals(2, found.size());
        assertEquals("ABCPD1234E", found.get(0).getValue());
        assertEquals("XYZPG5678H", found.get(1).getValue());
    }

    @Test
    void rejectsUnknownHolderType() {
        // The fourth character must be a real holder type, so E and Z are not accepted.
        assertTrue(detector.detect("ABCDE1234F").isEmpty());
        assertTrue(detector.detect("ABCZD1234E").isEmpty());
    }

    @Test
    void rejectsWrongLength() {
        assertTrue(detector.detect("ABCPD1234").isEmpty());
        assertTrue(detector.detect("ABCPD12345").isEmpty());
        assertTrue(detector.detect("ABCPD123E").isEmpty());
    }

    @Test
    void rejectsPanGluedToLettersOrDigits() {
        assertTrue(detector.detect("XABCPD1234EY").isEmpty());
        assertTrue(detector.detect("1ABCPD1234E").isEmpty());
        assertTrue(detector.detect("ABCPD1234E1").isEmpty());
    }

    @Test
    void handlesNullAndEmptyText() {
        assertTrue(detector.detect(null).isEmpty());
        assertTrue(detector.detect("").isEmpty());
    }
}
