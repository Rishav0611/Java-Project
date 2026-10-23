package com.privacyshield.detect;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailDetector implements Detector {

    private static final Pattern EMAIL = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}\\b");

    @Override
    public DataType type() {
        return DataType.EMAIL;
    }

    @Override
    public List<Detection> detect(String text) {
        List<Detection> results = new ArrayList<>();
        Matcher m = EMAIL.matcher(text);
        while (m.find()) {
            results.add(new Detection(m.start(), m.end(), type(), m.group()));
        }
        return results;
    }
}