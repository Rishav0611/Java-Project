package com.privacyshield;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AadhaarDetector implements Detector {
    // Allows a 12-digit number with optional spaces or hyphens between digits.
    private static final Pattern CANDIDATE_PATTERN =
            Pattern.compile("(?<!\\d)(?:\\d[ -]?){11}\\d(?!\\d)");

    @Override
    public List<Detection> detect(String text) {
        List<Detection> detections = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return detections;
        }

        Matcher matcher = CANDIDATE_PATTERN.matcher(text);

        while (matcher.find()) {
            String value = matcher.group();
            String digits = value.replace(" ", "").replace("-", "");

            // Aadhaar numbers start with 2-9 and include a Verhoeff check digit.
            if (digits.charAt(0) >= '2' && Verhoeff.isValid(digits)) {
                detections.add(new Detection(
                        DataType.AADHAAR,
                        value,
                        matcher.start(),
                        matcher.end()
                ));
            }
        }

        return detections;
    }
}

