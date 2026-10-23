package com.privacyshield.detect;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AadhaarDetector implements Detector {

    private static final Pattern AADHAAR = Pattern.compile(
            "(?<!\\d)[2-9]\\d{3}([ -]?)\\d{4}\\1\\d{4}(?!\\d)");

    @Override
    public DataType type() {
        return DataType.AADHAAR;
    }

    @Override
    public List<Detection> detect(String text) {
        List<Detection> results = new ArrayList<>();
        Matcher m = AADHAAR.matcher(text);
        while (m.find()) {
            String digitsOnly = m.group().replaceAll("[ -]", "");
            if (Verhoeff.isValid(digitsOnly)) {
                results.add(new Detection(m.start(), m.end(), type(), m.group()));
            }
        }
        return results;
    }
}