package com.privacyshield.detect;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionEngineTest {

    private static class FakeDetector implements Detector {
        private final DataType type;
        private final List<Detection> output;

        FakeDetector(DataType type, List<Detection> output) {
            this.type = type;
            this.output = output;
        }

        @Override
        public DataType type() {
            return type;
        }

        @Override
        public List<Detection> detect(String text) {
            return output;
        }
    }

    @Test
    void findsAllTypesInOrderOfPosition() {
        DetectionEngine engine = DetectionEngine.withDefaults();

        List<Detection> result = engine.detect(
                "Mail a@x.com, phone 9876543210, PAN ABCPD1234E, Aadhaar 2345 6789 0124");

        assertEquals(4, result.size());
        assertEquals(DataType.EMAIL, result.get(0).type());
        assertEquals(DataType.PHONE, result.get(1).type());
        assertEquals(DataType.PAN, result.get(2).type());
        assertEquals(DataType.AADHAAR, result.get(3).type());
    }

    @Test
    void longerMatchWinsOnOverlap() {
        Detection longer = new Detection(0, 10, DataType.PHONE, "0123456789");
        Detection shorter = new Detection(2, 8, DataType.EMAIL, "234567");
        DetectionEngine engine = new DetectionEngine(List.of(
                new FakeDetector(DataType.PHONE, List.of(longer)),
                new FakeDetector(DataType.EMAIL, List.of(shorter))));

        List<Detection> result = engine.detect("ignored");

        assertEquals(1, result.size());
        assertEquals(DataType.PHONE, result.get(0).type());
    }

    @Test
    void sameLengthOverlapUsesEnumOrder() {
        Detection phone = new Detection(0, 10, DataType.PHONE, "0123456789");
        Detection aadhaar = new Detection(0, 10, DataType.AADHAAR, "0123456789");
        DetectionEngine engine = new DetectionEngine(List.of(
                new FakeDetector(DataType.PHONE, List.of(phone)),
                new FakeDetector(DataType.AADHAAR, List.of(aadhaar))));

        List<Detection> result = engine.detect("ignored");

        assertEquals(1, result.size());
        assertEquals(DataType.AADHAAR, result.get(0).type());
    }

    @Test
    void returnsEmptyForCleanText() {
        assertTrue(DetectionEngine.withDefaults().detect("Hello, kaise ho?").isEmpty());
    }
}