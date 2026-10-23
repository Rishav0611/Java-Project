package com.privacyshield.mask;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Unmasker {

    // [PHONE_1], PHONE_1, [phone 1], phone-1 ...
    private static final Pattern TOKEN_LIKE = Pattern.compile(
            "(?<![A-Za-z0-9])(\\[\\s*)?([A-Za-z]+)[ _\\-]?(\\d+)(\\s*\\])?");

    public String unmask(String text, Map<String, String> tokenToValue) {
        Matcher m = TOKEN_LIKE.matcher(text);
        StringBuilder out = new StringBuilder();

        while (m.find()) {
            String normalized = "[" + m.group(2).toUpperCase() + "_" + m.group(3) + "]";
            String value = tokenToValue.get(normalized);
            String replacement = (value != null) ? value : m.group();
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);

        return out.toString();
    }
}