package com.privacyshield;

import java.util.List;

public class PrivacyShieldCli {
    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            System.err.println("Usage: PrivacyShieldCli \"text to scan\"");
            return;
        }

        StringBuilder input = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                input.append(' ');
            }
            input.append(args[i]);
        }

        String text = input.toString();
        PrivacyShield privacyShield = new PrivacyShield();
        List<Detection> detections = privacyShield.detect(text);

        if (detections.isEmpty()) {
            System.out.println("No supported personal data found.");
        } else {
            for (Detection detection : detections) {
                System.out.println(
                        detection.getType()
                                + " found at characters "
                                + detection.getStart()
                                + "-"
                                + detection.getEnd()
                );
            }
        }

        System.out.println("Redacted text: " + privacyShield.redact(text));
    }
}

