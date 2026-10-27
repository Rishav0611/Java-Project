package com.privacyshield.detect;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomDetector implements Detector {

    private final List<String> words;

    public CustomDetector(List<String> words) {
        this.words = List.copyOf(words);
    }

    @Override
    public DataType type() {
        return DataType.CUSTOM;
    }

    @Override
    public List<Detection> detect(String text) {
        List<Detection> results = new ArrayList<>();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            Pattern pattern = Pattern.compile(
                    "(?<![A-Za-z0-9])" + Pattern.quote(word.trim()) + "(?![A-Za-z0-9])",
                    Pattern.CASE_INSENSITIVE);

            Matcher m = pattern.matcher(text);
            while (m.find()) {
                results.add(new Detection(m.start(), m.end(), type(), m.group()));
            }
        }
        return results;
    }
}