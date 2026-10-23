package com.privacyshield.detect;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PanDetector implements Detector {

    private static final Pattern PAN = Pattern.compile(
            "\\b[A-Z]{3}[ABCFGHJLPT][A-Z]\\d{4}[A-Z]\\b");

    @Override
    public DataType type() {
        return DataType.PAN;
    }

    @Override
    public List<Detection> detect(String text) {
        List<Detection> results = new ArrayList<>();
        Matcher m = PAN.matcher(text);
        while (m.find()) {
            results.add(new Detection(m.start(), m.end(), type(), m.group()));
        }
        return results;
    }
}