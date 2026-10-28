package com.privacyshield;

import java.util.Map;

/**
 * Demo for the masking engine.
 *
 * Prints the original text, the masked text and the restored text, then checks
 * that unmask(mask(text)) gives back the original text.
 */
public class Main {
    private static final String SAMPLE_TEXT =
            "My email is person@example.com and my phone is +91 98765 43210. "
                    + "PAN: ABCPD1234E. Aadhaar: 2345 6789 0124.";

    public static void main(String[] args) {
        String text = joinArguments(args);

        PrivacyShield privacyShield = new PrivacyShield();
        MaskResult result = privacyShield.mask(text);
        String restored = result.unmask(result.getMaskedText());

        System.out.println("Original: " + text);
        System.out.println("Masked:   " + result.getMaskedText());
        System.out.println("Restored: " + restored);
        System.out.println();
        System.out.println("Found " + result.getDetections().size() + " value(s). Fake -> real:");

        for (Map.Entry<String, String> entry : result.getFakeToReal().entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
        }

        System.out.println();

        if (text.equals(restored)) {
            System.out.println("Round trip OK: restored text matches the original.");
        } else {
            System.out.println("Round trip FAILED.");
            System.exit(1);
        }
    }

    private static String joinArguments(String[] args) {
        if (args == null || args.length == 0) {
            return SAMPLE_TEXT;
        }

        StringBuilder text = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                text.append(' ');
            }
            text.append(args[i]);
        }

        return text.toString();
    }
}
