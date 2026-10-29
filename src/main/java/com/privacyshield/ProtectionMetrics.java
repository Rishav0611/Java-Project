package com.privacyshield;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Counts what the current window has protected.
 *
 * The numbers stay in memory and disappear when the window closes. They are
 * used by the Dashboard tab and never include the actual personal values.
 */
public class ProtectionMetrics {
    private final Map<DataType, Integer> maskedByType = emptyCounts();
    private final Map<DataType, Integer> ocrByType = emptyCounts();
    private final Map<DataType, Integer> clipboardByType = emptyCounts();
    private final List<Runnable> listeners = new ArrayList<Runnable>();

    private int maskedValues;
    private int ocrValues;
    private int clipboardValues;
    private int qrCodes;
    private int photosStripped;
    private int photosRedacted;
    private int redactedAreas;

    public synchronized void recordMasked(List<Detection> detections) {
        if (detections == null) {
            throw new IllegalArgumentException("detections cannot be null");
        }

        for (Detection detection : detections) {
            if (detection != null && detection.getType() != null) {
                maskedValues++;
                increment(maskedByType, detection.getType());
            }
        }

        notifyListeners();
    }

    public synchronized void recordOcrMatches(List<OcrMatch> matches) {
        if (matches == null) {
            throw new IllegalArgumentException("matches cannot be null");
        }

        for (OcrMatch match : matches) {
            if (match != null && match.getType() != null) {
                ocrValues++;
                increment(ocrByType, match.getType());
            }
        }

        notifyListeners();
    }

    public synchronized void recordClipboard(List<Detection> detections) {
        if (detections == null) {
            throw new IllegalArgumentException("detections cannot be null");
        }

        for (Detection detection : detections) {
            if (detection != null && detection.getType() != null) {
                clipboardValues++;
                increment(clipboardByType, detection.getType());
            }
        }

        notifyListeners();
    }

    public synchronized void recordQrCodes(int count) {        checkCount(count, "count");
        qrCodes += count;
        notifyListeners();
    }

    public synchronized void recordPhotoStripped() {
        photosStripped++;
        notifyListeners();
    }

    public synchronized void recordPhotoRedacted(int areas) {
        checkCount(areas, "areas");
        photosRedacted++;
        redactedAreas += areas;
        notifyListeners();
    }

    public synchronized int getMaskedValues() {
        return maskedValues;
    }

    public synchronized int getOcrValues() {
        return ocrValues;
    }

    public synchronized int getClipboardValues() {
        return clipboardValues;
    }

    public synchronized int getQrCodes() {
        return qrCodes;
    }

    public synchronized int getPhotosStripped() {
        return photosStripped;
    }

    public synchronized int getPhotosRedacted() {
        return photosRedacted;
    }

    public synchronized int getRedactedAreas() {
        return redactedAreas;
    }

    public synchronized int getPhotosCleaned() {
        return photosStripped + photosRedacted;
    }

    public synchronized int getMaskedCount(DataType type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

        return maskedByType.get(type);
    }

    public synchronized int getOcrCount(DataType type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

        return ocrByType.get(type);
    }

    public synchronized int getClipboardCount(DataType type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

        return clipboardByType.get(type);
    }

    public synchronized int getCoveredCount(DataType type) {
        return getMaskedCount(type) + getOcrCount(type) + getClipboardCount(type);
    }

    public synchronized void reset() {
        maskedValues = 0;
        ocrValues = 0;
        clipboardValues = 0;
        qrCodes = 0;
        photosStripped = 0;
        photosRedacted = 0;
        redactedAreas = 0;

        for (DataType type : DataType.values()) {
            maskedByType.put(type, 0);
            ocrByType.put(type, 0);
            clipboardByType.put(type, 0);
        }

        notifyListeners();
    }

    /**
     * Dashboard listens for changes so its cards stay current.
     */
    public synchronized void addChangeListener(Runnable listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }

        listeners.add(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : new ArrayList<Runnable>(listeners)) {
            listener.run();
        }
    }

    private static void increment(Map<DataType, Integer> counts, DataType type) {
        counts.put(type, counts.get(type) + 1);
    }

    private static void checkCount(int count, String name) {
        if (count < 0) {
            throw new IllegalArgumentException(name + " cannot be negative");
        }
    }

    private static Map<DataType, Integer> emptyCounts() {
        Map<DataType, Integer> counts = new EnumMap<DataType, Integer>(DataType.class);

        for (DataType type : DataType.values()) {
            counts.put(type, 0);
        }

        return counts;
    }
}
