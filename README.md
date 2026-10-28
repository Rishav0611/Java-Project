# PrivacyShield

PrivacyShield is a small Java tool for finding and masking personal data before text is shared with an AI agent.

## Project phases

1. Core models and Verhoeff checksum validation.
2. Detectors for Aadhaar-like numbers, PAN, Indian mobile numbers, and email addresses.
3. A combined scanner that orders detections and avoids overlapping replacements.
4. A redaction API, command-line demo, and build instructions.
5. Fake-value masking with a fake-to-real map, so a reply can be restored.
6. Master-password encryption of saved values with PBKDF2 and AES-GCM.

## What it detects

- Aadhaar-like values: 12 digits, optional spaces or hyphens, first digit 2-9, and a valid Verhoeff checksum.
- PAN values: the standard 10-character pattern, including the holder-type character. The fourth character must be a real holder type (P, C, H, F, A, T, B, L, J, G), so `ABCDE1234F` is not detected while `ABCPD1234E` is.
- Indian mobile numbers: 10 digits beginning with 6-9, with an optional +91 prefix and spaces or hyphens.
- Email addresses: a practical pattern for common email formats. A full stop or other punctuation right after the address is not included in the match, so emails at the end of a sentence are found.

The checks are format-based. They do not confirm a person's identity or contact an official service. Review detections before relying on them for important decisions.

## Build

Requires Java 8 or newer and Maven. From the repository root, run:

    mvn package

The build compiles the code and runs the tests.

## Run the tests

    mvn clean test

The tests use JUnit 5 and cover the Verhoeff checksum, every detector (positive and negative cases), the scanner ordering and overlap rules, redaction, fake values, masking and unmasking, the crypto service, and the command-line demo. They only use made-up values.

## Run the demo

    java -cp target/classes com.privacyshield.PrivacyShieldCli "Contact person@example.com or 9876543210"

The program prints data types and character positions, then prints a redacted version. Each detected value is replaced with `[REDACTED]`. It does not print the matched values separately.

## Use in code

    PrivacyShield privacyShield = new PrivacyShield();
    String safeText = privacyShield.sanitize(prompt);

Pass safeText to the AI service instead of the original prompt.

## Mask and unmask

`mask()` replaces every detected value with a fake value that keeps the same length and format, and keeps a fake-to-real map. `unmask()` uses that map to put the real values back, so `unmask(mask(text))` gives back the original text.

    PrivacyShield privacyShield = new PrivacyShield();
    MaskResult result = privacyShield.mask(prompt);

    String forAi = result.getMaskedText();   // send this to the AI
    String restored = result.unmask(aiReply); // real values back

The same real value always gets the same fake, and a fake is never a value that already appears in the text, so unmasking cannot damage the rest of the reply. A fake Aadhaar number still has a valid Verhoeff check digit, and a fake phone keeps its +91 prefix and separators.

Run the masking demo:

    java -cp target/classes com.privacyshield.Main

It prints the original text, the masked text, the restored text and the fake-to-real pairs, then checks the round trip.

## Encrypt saved values

    CryptoService crypto = new CryptoService();
    String saved = crypto.encrypt(realValue, masterPassword);
    String realValueAgain = crypto.decrypt(saved, masterPassword);

The master password is turned into a key with PBKDF2WithHmacSHA256 (65,536 iterations, random 16-byte salt). Values are encrypted with AES-GCM using a new random 12-byte IV every time. A wrong password or changed data throws `VaultLockedException`.

