# PrivacyShield

PrivacyShield is a small Java tool for finding and masking personal data in text and photos before they are shared with an AI agent. It can mask text, strip photo metadata, cover QR codes and sensitive text found by OCR, and show a small dashboard of what it protected in the current window.

## Project phases

1. Core models and Verhoeff checksum validation.
2. Detectors for Aadhaar-like numbers, PAN, Indian mobile numbers, and email addresses.
3. A combined scanner that orders detections and avoids overlapping replacements.
4. A redaction API, command-line demo, and build instructions.
5. Fake-value masking with a fake-to-real map, so a reply can be restored.
6. Master-password encryption of saved values with PBKDF2 and AES-GCM.
7. A desktop GUI with live colour highlights, reply restoration, a Keep/Delete choice, and light and dark themes.
8. Photo tools: GPS, camera and time metadata scanning, lossless metadata stripping, and manual black-box redaction.
9. QR code detection, OCR text detection, automatic black boxes and a protection dashboard.

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
- `privacyshield-1.0.0-gui.jar` is a runnable jar that includes the FlatLaf theme library, the metadata-extractor library, ZXing and Tess4J with the Windows Tesseract library and English data. This jar is large (about 23 MB) because of the OCR and QR libraries.

## Run the tests

    mvn clean test

The tests use JUnit 5 and cover the Verhoeff checksum, every detector (positive and negative cases), the scanner ordering and overlap rules, redaction, fake values, masking and unmasking, the crypto service, the GUI session vault and highlight colours, photo metadata scanning, lossless JPEG and PNG stripping, manual redaction, the drag-box photo canvas, QR decoding and bounds, OCR word grouping and sensitive-text boxes, the protection counters, the detector self-check and the command-line demo. They only use made-up values, a made-up JPEG with synthetic EXIF fields and generated QR codes. OCR tests use a fake engine, so they do not need the native Tesseract library. The window smoke test is skipped automatically when no display is available.

There are 161 tests. The small synthetic detector check in the dashboard reports 100% recall and precision on its 8 made-up values; this checks the format rules, not real-world accuracy.

## Run the demo

    java -cp target/classes com.privacyshield.PrivacyShieldCli "Contact person@example.com or 9876543210"

The program prints data types and character positions, then prints a redacted version. Each detected value is replaced with `[REDACTED]`. It does not print the matched values separately.

## Desktop GUI

Build the runnable jar and start the window:

    mvn package
    java -jar target/privacyshield-1.0.0-gui.jar

The window has three tabs, **Text**, **Photos** and **Dashboard**, and one **Dark theme** button for all of them.

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
- **Find QR codes** looks for QR codes and adds a box over each one, so the code cannot be scanned from the shared photo.
- **Find text (OCR)** reads the text in the photo and adds a box over each Aadhaar-like number, PAN, phone or email value it finds.

For JPEG, stripping copies the compressed image data unchanged and removes only the metadata segments, so the picture is not re-compressed. PNG text, time and EXIF chunks are removed the same way. Every new file is scanned again before the tool reports success. The redacted copy is re-encoded as JPEG or PNG, so JPEG pixels outside the boxes may change slightly because of normal JPEG compression; the metadata is removed either way.

Photo actions run in a background `SwingWorker`, so the window stays responsive on large photos. Suggested names end in `-stripped` or `-redacted`, and the output must be a different file from the original.

### QR codes

The **Find QR codes** button uses the ZXing library to decode QR codes and gives the position of each one. A padded box is added over every code. The boxes are only a preview; press **Save redacted copy** to get a file where the codes are painted black and the metadata is gone. Photos that contain no QR code simply report that none were found.

### OCR text

The **Find text (OCR)** button uses Tess4J, which runs the Tesseract 5 reader, to read words and their positions. Words on the same line are joined into a line of text, the normal PrivacyShield detectors run on that text, and the matching word positions are joined into a padded box. Very low-confidence words are skipped. The four detectors are the same format checks used in the Text tab, so OCR boxes have the same strengths and limits.

The Tess4J jar includes the Tesseract library for Windows and English language data. On the first OCR scan the English data file is copied from the jar to a temporary folder, so OCR works on Windows without installing Tesseract. The temporary folder is `privacyshield-tessdata` inside the system temp directory and contains no personal data.

For another language, or on macOS and Linux, install Tesseract 5 and start the jar with system properties:

    java -Dprivacyshield.ocr.language=eng -Dprivacyshield.ocr.datapath="C:\Program Files\Tesseract-OCR\tessdata" -jar target/privacyshield-1.0.0-gui.jar

If OCR cannot start, the Photos tab shows a message with these options instead of crashing.

### Dashboard

The **Dashboard** tab counts what the current window protected: text values masked, OCR areas covered, QR codes found and photos cleaned. The table splits the counts by type (Aadhaar, PAN, phone, email) and shows a total. **Reset stats** clears the counters.

The counts exist only in memory and contain no personal values, so nothing about the data is written to disk. **Run detector check** runs the format detectors over six small synthetic samples (eight expected values) and shows recall, precision and any misses or false positives. This is a quick self-check of the rules, not a measure of accuracy on real photos.

The dashboard also refreshes by itself after a mask, a QR scan, an OCR scan, a strip or a redaction.

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

## QR and OCR in code

    QrCodeService qrCodes = new QrCodeService();
    List<QrCodeResult> codes = qrCodes.scan(photo);

    OcrTextService ocr = new OcrTextService();
    List<OcrMatch> matches = ocr.findSensitive(photo);

    List<Rectangle> boxes = new ArrayList<Rectangle>();

    for (QrCodeResult code : codes) {
        boxes.add(code.getBounds());
    }

    for (OcrMatch match : matches) {
        boxes.add(match.getBounds());
    }

    photos.redact(photo, new File("clean.png"), boxes);

`scan()` returns the decoded text and a padded `Rectangle` for every QR code, sorted top to bottom. `findSensitive()` returns the data type, the matched text and a padded `Rectangle` for every sensitive word area found by OCR. Both return empty lists when nothing is found and throw `IOException` for an unreadable file.

QR decoding uses ZXing. OCR uses Tess4J, and the engine can be replaced:

    OcrTextService ocr = new OcrTextService(new MyOcrEngine(), new PrivacyShield());

`OcrEngine` is a one-method interface that returns `OcrWord` items (text, box and confidence), so tests and other tools can run without the native library.

`ProtectionMetrics` holds the dashboard counters:

    ProtectionMetrics metrics = new ProtectionMetrics();
    metrics.recordMasked(result.getDetections());
    metrics.recordOcrMatches(matches);
    metrics.recordQrCodes(codes.size());
    metrics.recordPhotoStripped();
    metrics.recordPhotoRedacted(boxes.size());

The counters live in memory only. `addChangeListener()` lets a panel refresh when they change.

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

## Limits

- Detection is format based. It can miss a value written in an unusual way and can flag a lookalike. Check the boxes and the masked text before sharing.
- OCR depends on picture quality, lighting and font. Words that Tesseract cannot read cannot be boxed, so review the preview.
- Only QR codes are decoded, not other barcode types.
- The redaction boxes are applied when the copy is saved. The preview alone does not change the photo.
- The dashboard counts cover one window session. They are not saved or uploaded, and the built-in detector check uses only synthetic samples.
- Strip and redact remove metadata, but the saved picture itself is still a picture. Nothing else is hidden by these tools.

