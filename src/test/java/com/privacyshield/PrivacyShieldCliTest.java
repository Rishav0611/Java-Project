package com.privacyshield;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the command-line demo.
 */
class PrivacyShieldCliTest {

    private String captureStdOut(String... args) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(buffer));
            PrivacyShieldCli.main(args);
        } finally {
            System.setOut(originalOut);
        }

        return buffer.toString();
    }

    private String captureStdErr(String... args) {
        PrintStream originalErr = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try {
            System.setErr(new PrintStream(buffer));
            PrivacyShieldCli.main(args);
        } finally {
            System.setErr(originalErr);
        }

        return buffer.toString();
    }

    @Test
    void printsDetectionsAndRedactedText() {
        String output = captureStdOut(
                "Contact",
                "person@example.com",
                "or",
                "9876543210"
        );

        assertTrue(output.contains("EMAIL found at characters 8-26"), output);
        assertTrue(output.contains("PHONE found at characters 30-40"), output);
        assertTrue(output.contains("Redacted text: Contact [REDACTED] or [REDACTED]"), output);
    }

    @Test
    void joinsMultipleArgumentsWithSingleSpaces() {
        String output = captureStdOut("mail", "a@b.com");

        assertTrue(output.contains("EMAIL found at characters 5-12"), output);
    }

    @Test
    void printsNoDataMessageForCleanText() {
        String output = captureStdOut("hello", "world");

        assertTrue(output.contains("No supported personal data found."), output);
        assertTrue(output.contains("Redacted text: hello world"), output);
    }

    @Test
    void printsUsageWhenNoArgumentsAreGiven() {
        String output = captureStdErr();

        assertTrue(output.contains("Usage: PrivacyShieldCli"), output);
    }
}
