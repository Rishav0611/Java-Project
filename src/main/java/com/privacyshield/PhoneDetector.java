package com.privacyshield;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneDetector implements Detector {
    // Detects Indian mobile numbers, optionally written with the +91 country code.
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?<![A-Z0-9])(?:\\+91[ -]?)?[6-9](?:[ -]?\\d){9}(?![A-Z0-9])",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public List<Detection> detect(String text) {
        List<Detection> detections = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return detections;
        }

        Matcher matcher = PHONE_PATTERN.matcher(text);

        while (matcher.find()) {
            detections.add(new Detection(
                    DataType.PHONE,
                    matcher.group(),
                    matcher.start(),
                    matcher.end()
            ));
        }

        return detections;
    }
}

