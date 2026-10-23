package com.privacyshield.detect;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneDetector implements Detector {

    private static final Pattern PATTERN = Pattern.compile(
            "(?<![A-Za-z0-9])(?:\\+91[\\s-]?|0)?[6-9]\\d{4}[\\s-]?\\d{5}(?![A-Za-z0-9])");

    @Override
    public DataType type() {
        return DataType.PHONE;
    }

    @Override
    public List<Detection> detect(String text) {
        List<Detection> found = new ArrayList<>();
        Matcher m = PATTERN.matcher(text);
        while (m.find()) {
            found.add(new Detection(m.start(), m.end(), DataType.PHONE, m.group()));
        }
        return found;
    }
}