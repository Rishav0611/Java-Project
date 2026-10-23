package com.privacyshield.mask;

import com.privacyshield.detect.Detection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.privacyshield.detect.DataType;

public class Masker {

    // "PHONE:9876543210" -> "[PHONE_1]"
    private final Map<String, String> valueToToken = new HashMap<>();
    private final Map<DataType, Integer> counters = new EnumMap<>(DataType.class);

    public MaskResult mask(String text, List<Detection> detections) {
        List<Detection> sorted = new ArrayList<>(detections);
        sorted.sort(Comparator.comparingInt(Detection::start));

        StringBuilder out = new StringBuilder();
        Map<String, String> used = new LinkedHashMap<>();
        int cursor = 0;

        for (Detection d : sorted) {
            if (d.start() < cursor) {
                continue; // overlap bacha ho to skip (engine pehle hi hata deta hai)
            }
            out.append(text, cursor, d.start());
            String token = tokenFor(d);
            out.append(token);
            used.put(token, d.value());
            cursor = d.end();
        }
        out.append(text, cursor, text.length());

        return new MaskResult(out.toString(), used);
    }

    private String tokenFor(Detection d) {
        String key = d.type() + ":" + d.value();
        return valueToToken.computeIfAbsent(key, k -> {
            int next = counters.merge(d.type(), 1, Integer::sum);
            return "[" + d.type() + "_" + next + "]";
        });
    }
}