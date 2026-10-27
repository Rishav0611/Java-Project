package com.privacyshield.detect;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomDetectorTest {

    @Test
    void findsSingleWord() {
        CustomDetector detector = new CustomDetector(List.of("Rahul"));

        List<Detection> result = detector.detect("Rahul studies");

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).start());
        assertEquals(5, result.get(0).end());
        assertEquals("Rahul", result.get(0).value());
    }

    @Test
    void ignoresCase() {
        CustomDetector detector = new CustomDetector(List.of("rahul"));

        List<Detection> result = detector.detect("RAHUL here");

        assertEquals(1, result.size());
        assertEquals("RAHUL", result.get(0).value());
    }

    @Test
    void ignoresWordInsideLongerWord() {
        CustomDetector detector = new CustomDetector(List.of("Rahul"));

        List<Detection> result = detector.detect("Rahulkumar");

        assertTrue(result.isEmpty());
    }

    @Test
    void emptyListFindsNothing() {
        CustomDetector detector = new CustomDetector(List.of());

        List<Detection> result = detector.detect("Rahul studies");

        assertTrue(result.isEmpty());
    }
}