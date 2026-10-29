package com.privacyshield;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProtectionMetricsTest {

    @Test
    void recordsTextAndOcrCountsByType() {
        ProtectionMetrics metrics = new ProtectionMetrics();

        metrics.recordMasked(Arrays.asList(
                new Detection(DataType.EMAIL, "a@example.com", 0, 13),
                new Detection(DataType.PHONE, "9876543210", 20, 30),
                new Detection(DataType.EMAIL, "b@example.com", 40, 53)));
        metrics.recordOcrMatches(Arrays.asList(
                new OcrMatch(DataType.AADHAAR, "2345 6789 0124", new Rectangle(1, 2, 3, 4)),
                new OcrMatch(DataType.EMAIL, "c@example.com", new Rectangle(5, 6, 7, 8))));

        assertEquals(3, metrics.getMaskedValues());
        assertEquals(2, metrics.getOcrValues());
        assertEquals(2, metrics.getMaskedCount(DataType.EMAIL));
        assertEquals(1, metrics.getMaskedCount(DataType.PHONE));
        assertEquals(0, metrics.getMaskedCount(DataType.PAN));
        assertEquals(1, metrics.getOcrCount(DataType.AADHAAR));
        assertEquals(1, metrics.getOcrCount(DataType.EMAIL));
        assertEquals(3, metrics.getCoveredCount(DataType.EMAIL));
    }

    @Test
    void recordsClipboardCountsByType() {
        ProtectionMetrics metrics = new ProtectionMetrics();

        metrics.recordClipboard(Arrays.asList(
                new Detection(DataType.EMAIL, "a@example.com", 0, 13),
                new Detection(DataType.EMAIL, "b@example.com", 20, 33)));
        metrics.recordClipboard(Collections.singletonList(
                new Detection(DataType.PAN, "ABCPD1234E", 0, 10)));

        assertEquals(3, metrics.getClipboardValues());
        assertEquals(2, metrics.getClipboardCount(DataType.EMAIL));
        assertEquals(1, metrics.getClipboardCount(DataType.PAN));
        assertEquals(0, metrics.getClipboardCount(DataType.PHONE));
        assertEquals(2, metrics.getCoveredCount(DataType.EMAIL));
    }

    @Test
    void recordsQrAndPhotoCounts() {
        ProtectionMetrics metrics = new ProtectionMetrics();

        metrics.recordQrCodes(2);
        metrics.recordPhotoStripped();
        metrics.recordPhotoStripped();
        metrics.recordPhotoRedacted(3);

        assertEquals(2, metrics.getQrCodes());
        assertEquals(2, metrics.getPhotosStripped());
        assertEquals(1, metrics.getPhotosRedacted());
        assertEquals(3, metrics.getRedactedAreas());
        assertEquals(3, metrics.getPhotosCleaned());
    }

    @Test
    void listenersAreCalledAndResetClearsEverything() {
        ProtectionMetrics metrics = new ProtectionMetrics();
        final int[] changes = {0};

        metrics.addChangeListener(new Runnable() {
            @Override
            public void run() {
                changes[0]++;
            }
        });

        metrics.recordMasked(Collections.singletonList(
                new Detection(DataType.PAN, "ABCPD1234E", 0, 10)));
        metrics.recordQrCodes(1);
        assertEquals(2, changes[0]);

        metrics.reset();
        assertEquals(3, changes[0]);
        assertEquals(0, metrics.getMaskedValues());
        assertEquals(0, metrics.getOcrValues());
        assertEquals(0, metrics.getClipboardValues());
        assertEquals(0, metrics.getQrCodes());
        assertEquals(0, metrics.getPhotosCleaned());
        assertEquals(0, metrics.getMaskedCount(DataType.PAN));
        assertEquals(0, metrics.getClipboardCount(DataType.PAN));
    }

    @Test
    void badArgumentsAreRejected() {
        ProtectionMetrics metrics = new ProtectionMetrics();

        assertThrows(IllegalArgumentException.class, () -> metrics.recordMasked(null));
        assertThrows(IllegalArgumentException.class, () -> metrics.recordOcrMatches(null));
        assertThrows(IllegalArgumentException.class, () -> metrics.recordClipboard(null));
        assertThrows(IllegalArgumentException.class, () -> metrics.recordQrCodes(-1));
        assertThrows(IllegalArgumentException.class, () -> metrics.recordPhotoRedacted(-1));
        assertThrows(IllegalArgumentException.class, () -> metrics.getMaskedCount(null));
        assertThrows(IllegalArgumentException.class, () -> metrics.getClipboardCount(null));
        assertThrows(IllegalArgumentException.class, () -> metrics.addChangeListener(null));
    }
}
