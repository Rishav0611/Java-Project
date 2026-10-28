package com.privacyshield;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The dashboard's detector self-check must stay perfect on its synthetic
 * samples; a change in detector behaviour should make this test fail.
 */
class DetectionBenchmarkTest {

    @Test
    void builtInSamplesProducePerfectRecallAndPrecision() {
        DetectionBenchmark.Result result = DetectionBenchmark.run();

        assertEquals(6, result.getSamples());
        assertEquals(8, result.getExpectedValues());
        assertEquals(8, result.getFoundValues());
        assertEquals(8, result.getCorrectValues());
        assertEquals(0, result.getMissedValues());
        assertEquals(0, result.getFalsePositives());
        assertEquals(1.0, result.getRecall(), 0.0001);
        assertEquals(1.0, result.getPrecision(), 0.0001);
        assertEquals(1.0, result.getAccuracy(), 0.0001);
    }

    @Test
    void nullScannerIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> DetectionBenchmark.run(null));
    }
}
