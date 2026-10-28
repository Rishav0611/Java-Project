package com.privacyshield;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailDetector implements Detector {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "(?i)(?<![A-Z0-9._%+-])[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}(?![A-Z0-9._%+-])"
    );

    @Override
    public List<Detection> detect(String text) {
        List<Detection> detections = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return detections;
        }

        Matcher matcher = EMAIL_PATTERN.matcher(text);

        while (matcher.find()) {
            detections.add(new Detection(
                    DataType.EMAIL,
                    matcher.group(),
                    matcher.start(),
                    matcher.end()
            ));
        }

        return detections;
    }
}

