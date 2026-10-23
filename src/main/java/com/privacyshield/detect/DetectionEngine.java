package com.privacyshield.detect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DetectionEngine {

    private final List<Detector> detectors;

    public DetectionEngine(List<Detector> detectors) {
        this.detectors = List.copyOf(detectors);
    }

    public static DetectionEngine withDefaults() {
        return new DetectionEngine(List.of(
                new AadhaarDetector(),
                new PanDetector(),
                new PhoneDetector(),
                new EmailDetector()));
    }

    public List<Detection> detect(String text) {
        List<Detection> all = new ArrayList<>();
        for (Detector d : detectors) {
            all.addAll(d.detect(text));
        }

        // Lamba match pehle, barabar length par DataType ke order se
        all.sort(Comparator
                .comparingInt((Detection x) -> x.end() - x.start()).reversed()
                .thenComparingInt(x -> x.type().ordinal())
                .thenComparingInt(Detection::start));

        List<Detection> accepted = new ArrayList<>();
        for (Detection candidate : all) {
            boolean overlaps = accepted.stream().anyMatch(a ->
                    candidate.start() < a.end() && a.start() < candidate.end());
            if (!overlaps) {
                accepted.add(candidate);
            }
        }

        accepted.sort(Comparator.comparingInt(Detection::start));
        return accepted;
    }
}