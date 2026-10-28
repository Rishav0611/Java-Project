# PrivacyShield

PrivacyShield is a small Java tool for finding and masking personal data in text, and for removing hidden personal data from photos, before they are shared with an AI agent.

## Project phases

1. Core models and Verhoeff checksum validation.
2. Detectors for Aadhaar-like numbers, PAN, Indian mobile numbers, and email addresses.
3. A combined scanner that orders detections and avoids overlapping replacements.
4. A redaction API, command-line demo, and build instructions.
5. Fake-value masking with a fake-to-real map, so a reply can be restored.
6. Master-password encryption of saved values with PBKDF2 and AES-GCM.
7. A desktop GUI with live colour highlights, reply restoration, a Keep/Delete choice, and light and dark themes.
8. Photo tools: GPS, camera and time metadata scanning, lossless metadata stripping, and manual black-box redaction.

## What it detects

- Aadhaar-like values: 12 digits, optional spaces or hyphens, first digit 2-9, and a valid Verhoeff checksum.
- PAN values: the standard 10-character pattern, including the holder-type character. The fourth character must be a real holder type (P, C, H, F, A, T, B, L, J, G), so `ABCDE1234F` is not detected while `ABCPD1234E` is.
- Indian mobile numbers: 10 digits beginning with 6-9, with an optional +91 prefix and spaces or hyphens.
- Email addresses: a practical pattern for common email formats. A full stop or other punctuation right after the address is not included in the match, so emails at the end of a sentence are found.

The checks are format-based. They do not confirm a person's identity or contact an official service. Review detections before relying on them for important decisions.

## Build

Requires Java 8 or newer and Maven. From the repository root, run:

    mvn package

The build compiles the code, runs the tests, and creates two jars in `target/`:

- `privacyshield-1.0.0.jar` is the normal library jar.
- `privacyshield-1.0.0-gui.jar` is a runnable jar that includes the FlatLaf theme library and the metadata-extractor library.

## Run the tests

    mvn clean test

The tests use JUnit 5 and cover the Verhoeff checksum, every detector (positive and negative cases), the scanner ordering and overlap rules, redaction, fake values, masking and unmasking, the crypto service, the GUI session vault and highlight colours, photo metadata scanning, lossless JPEG and PNG stripping, manual redaction, the drag-box photo canvas, and the command-line demo. They only use made-up values and a made-up JPEG with synthetic EXIF fields. The window smoke test is skipped automatically when no display is available.

## Run the demo

    java -cp target/classes com.privacyshield.PrivacyShieldCli "Contact person@example.com or 9876543210"

The program prints data types and character positions, then prints a redacted version. Each detected value is replaced with `[REDACTED]`. It does not print the matched values separately.

## Desktop GUI

Build the runnable jar and start the window:

    mvn package
    java -jar target/privacyshield-1.0.0-gui.jar

The window has two tabs, **Text** and **Photos**, and one **Dark theme** button for both.

### Text tab

The tab has four panes:

- **Original text** (top left, editable). Detected values are highlighted while you type: Aadhaar red, PAN green, phone yellow and email blue.
- **Masked text** (top right, read-only). Copy this into the AI chat.
- **AI reply** (bottom left, editable). Paste the reply here.
- **Restored text** (bottom right). Press **Restore reply** to put the real values back.

Press **Mask** to fill the masked pane. Editing the original text afterwards clears the masked pane, so an old masked text cannot be shared by mistake. The **Mask mode** list offers two choices:

- **Fake values** keeps each value's length and format and can be restored later.
- **[REDACTED]** replaces each value with a fixed marker and cannot be restored.

Restore works while the fake-to-real map is still in memory. **Keep/Delete...** asks whether to keep that map. **Delete** is the default, so closing the dialog without choosing also deletes. The map never touches the disk and is cleared when the window closes.

### Photos tab

**Choose photo...** opens a JPEG or PNG. The preview shows the picture, and the table lists only the privacy-relevant fields: camera make and model, software, date and time, GPS location, author, comment and XMP data. Technical fields such as width and compression are left out.

- **Strip metadata** saves a copy with those fields removed. The original photo is never changed.
- **Drag on the preview** to draw a black box over something visible, such as a house number or a face. **Undo box** removes the last box and **Clear boxes** removes all of them.
- **Save redacted copy** paints the boxes black and saves the copy with no metadata.

For JPEG, stripping copies the compressed image data unchanged and removes only the metadata segments, so the picture is not re-compressed. PNG text, time and EXIF chunks are removed the same way. Every new file is scanned again before the tool reports success. The redacted copy is re-encoded as JPEG or PNG, so JPEG pixels outside the boxes may change slightly because of normal JPEG compression; the metadata is removed either way.

Both actions run in a background `SwingWorker`, so the window stays responsive on large photos. Suggested names end in `-stripped` or `-redacted`, and the output must be a different file from the original.

The GUI uses the same `PrivacyShield` engine as the command-line tools. It does not use a network connection or a database.

## Use in code

    PrivacyShield privacyShield = new PrivacyShield();
    String safeText = privacyShield.sanitize(prompt);

Pass safeText to the AI service instead of the original prompt.

## Photo tools in code

    PhotoService photos = new PhotoService();
    List<PhotoMetadata> metadata = photos.scan(photo);
    photos.strip(photo, new File("clean.jpg"));
    photos.redact(photo, new File("private.png"), boxes);

`scan()` returns an unmodifiable list of group, tag and value items. `isLocation()` is true for GPS coordinates. `strip()` accepts JPEG and PNG files and writes a new file; it throws `IOException` when the format is not supported or metadata is still found after stripping. `redact()` accepts a list of `java.awt.Rectangle` boxes in image pixels, fills them black and saves a new JPEG or PNG. Both methods refuse to overwrite the input file.

Metadata reading uses the metadata-extractor library (with xmpcore for XMP). Stripping and redaction are implemented in this project: the JPEG segment walker and PNG chunk walker copy image data without re-compressing it.

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

