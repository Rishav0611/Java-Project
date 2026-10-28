# PrivacyShield

PrivacyShield is a small Java tool for finding and masking personal data before text is shared with an AI agent.

## Project phases

1. Core models and Verhoeff checksum validation.
2. Detectors for Aadhaar-like numbers, PAN, Indian mobile numbers, and email addresses.
3. A combined scanner that orders detections and avoids overlapping replacements.
4. A redaction API, command-line demo, and build instructions.

## What it detects

- Aadhaar-like values: 12 digits, optional spaces or hyphens, first digit 2-9, and a valid Verhoeff checksum.
- PAN values: the standard 10-character pattern, including the holder-type character.
- Indian mobile numbers: 10 digits beginning with 6-9, with an optional +91 prefix and spaces or hyphens.
- Email addresses: a practical pattern for common email formats.

The checks are format-based. They do not confirm a person's identity or contact an official service. Review detections before relying on them for important decisions.

## Build

Requires Java 8 or newer and Maven. From the repository root, run:

    mvn package

## Run the demo

    java -cp target/classes com.privacyshield.PrivacyShieldCli "Contact person@example.com or 9876543210"

The program prints data types and character positions, then prints a redacted version. It does not print the matched values separately.

## Use in code

    PrivacyShield privacyShield = new PrivacyShield();
    String safeText = privacyShield.sanitize(prompt);

Pass safeText to the AI service instead of the original prompt.

