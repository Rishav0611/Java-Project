package com.privacyshield;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PanDetector implements Detector {
    // PAN format: three letters, holder type, initial, four digits, and one letter.
    private static final Pattern PAN_PATTERN = Pattern.compile(
            "(?i)(?<![A-Z0-9])[A-Z]{3}[PCHFATBLJG][A-Z][0-9]{4}[A-Z](?![A-Z0-9])"
    );

    @Override
    public List<Detection> detect(String text) {
        List<Detection> detections = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return detections;
        }

        Matcher matcher = PAN_PATTERN.matcher(text);

        while (matcher.find()) {
            detections.add(new Detection(
                    DataType.PAN,
                    matcher.group(),
                    matcher.start(),
                    matcher.end()
            ));
        }

        return detections;
    }
}

