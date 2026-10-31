# PrivacyShield

PrivacyShield is a small Java tool for finding and masking personal data in text before it is shared with an AI agent. It can mask text, put the real values back in a reply, and keep the fake-to-real map in an encrypted SQLite vault.

This branch contains only the text masking/unmasking and vault flow.

## Project phases

1. Core models and Verhoeff checksum validation.
2. Detectors for Aadhaar-like numbers, PAN, Indian mobile numbers, and email addresses.
3. A combined scanner that orders detections and avoids overlapping replacements.
4. A redaction API, command-line demo, and build instructions.
5. Fake-value masking with a fake-to-real map, so a reply can be restored.
6. Master-password encryption of saved values with PBKDF2 and AES-GCM.
7. A desktop GUI with live colour highlights, reply restoration, a Keep/Delete choice, light and dark themes, and an optional SQLite vault.

## What it detects

- Aadhaar-like values: 12 digits, optional spaces or hyphens, first digit 2-9, and a valid Verhoeff checksum.
- PAN values: the standard 10-character pattern, including the holder-type character. The fourth character must be a real holder type (P, C, H, F, A, T, B, L, J, G), so `ABCDE1234F` is not detected while `ABCPD1234E` is.
- Indian mobile numbers: 10 digits beginning with 6-9, with an optional +91 prefix and spaces or hyphens.
- Email addresses: a practical pattern for common email formats. A full stop or other punctuation right after the address is not included in the match, so emails at the end of a sentence are found.

The checks are format-based. They do not confirm a person's identity or contact an official service. Review detections before relying on them for important decisions.

## Build

Requires Java 17 or newer and Maven. The project compiles with Java 17 language level and was tested on a newer JDK as well. From the repository root, run:

    mvn package

The build compiles the code, runs the tests, and creates two jars in `target/`:

- `privacyshield-1.0.0.jar` is the normal library jar.
- `privacyshield-1.0.0-gui.jar` is a runnable jar that includes the FlatLaf theme library and the SQLite JDBC driver. Its main class is `com.privacyshield.GuiMain`.

## Run the tests

    mvn clean test

The tests use JUnit 5 and cover the Verhoeff checksum, every detector (positive and negative cases), the scanner ordering and overlap rules, redaction, fake values, masking and unmasking, the crypto service, the GUI session vault and highlight colours, the SQLite vault and the command-line demo. They only use made-up values and temporary vault files. The window smoke test is skipped automatically when no display is available.

There are 136 tests.

## Run the demo

    java -cp target/classes com.privacyshield.PrivacyShieldCli "Contact person@example.com or 9876543210"

The program prints data types and character positions, then prints a redacted version. Each detected value is replaced with `[REDACTED]`. It does not print the matched values separately.

## Desktop GUI

Build the runnable jar and start the window:

    mvn package
    java -jar target/privacyshield-1.0.0-gui.jar

The window has two tabs, **Text** and **Vault**, and one **Dark theme** button for both of them.

### Text tab

The tab has four panes:

- **Original text** (top left, editable). Detected values are highlighted while you type: Aadhaar red, PAN green, phone yellow and email blue.
- **Masked text** (top right, read-only). Copy this into the AI chat.
- **AI reply** (bottom left, editable). Paste the reply here.
- **Restored text** (bottom right). Press **Restore reply** to put the real values back.

Press **Mask** to fill the masked pane. Editing the original text afterwards clears the masked pane, so an old masked text cannot be shared by mistake. The **Mask mode** list offers two choices:

- **Fake values** keeps each value's length and format and can be restored later.
- **[REDACTED]** replaces each value with a fixed marker and cannot be restored.

Restore works while the fake-to-real map is still in memory. **Keep/Delete...** asks whether to keep that map. **Delete** is the default, so closing the dialog without choosing also deletes. Unless you save the map in the **Vault** tab, it never touches the disk and is cleared when the window closes.

### Vault tab

The vault is optional. It saves the current fake-to-real map so an AI reply can be restored after the window was closed.

1. In the **Text** tab, mask with **Fake values** and press **Keep/Delete...**, then **Keep**.
2. Open the **Vault** tab and type a master password of at least 8 characters.
3. Press **Save current values**. This replaces any old vault contents after a confirmation.

The default file is `privacyshield-vault.db` in your home folder; its path and entry count are shown in the tab. The fake values are stored as plain keys, so the file can be searched later. The real values are encrypted with AES-GCM, using a key derived from the master password with PBKDF2WithHmacSHA256 and a random salt. The password itself is never written anywhere.

To use the vault later, start the app, type the same password and press **Load into Text tab**. After a successful load the Text tab opens by itself. Paste the AI reply in the Text tab and press **Restore reply**; the loaded values work the same way as values masked in this window.

The Vault tab also lists the loaded fake values (only the fakes, never the real values). If there is no AI reply to test with, copy one fake value into the **AI reply** box and press **Restore reply**; the matching real value appears on the right. This proves that the vault load worked.

A wrong password or a damaged file shows an error and nothing is loaded. A short password, an empty session or a missing file is reported in a dialog as well as the status line. **Delete vault** removes the file after a confirmation.

Without the password the real values cannot be opened; there is no recovery question. Saving and loading run in the background so the window stays responsive.

The GUI uses the same `PrivacyShield` engine as the command-line tools. It does not use a network connection. It writes files only when you create the vault.

## Use in code

    PrivacyShield privacyShield = new PrivacyShield();
    String safeText = privacyShield.sanitize(prompt);

Pass safeText to the AI service instead of the original prompt.

## Vault in code

    VaultStore store = new VaultStore(new File("privacy-vault.db"));
    store.save(masterPassword, result.getFakeToReal());

    Map<String, String> loaded = store.load(masterPassword);
    new SessionVault().loadValues(loaded);

    store.delete();

`save()` replaces the old entries in one transaction and encrypts every real value with `CryptoService`. `load()` needs the same master password and throws `VaultLockedException` for a wrong password or damaged data. `count()` works without the password because only fake keys and encrypted values are in the file. `delete()` removes the file.

`SessionVault.loadValues()` places a loaded map into the current window, so **Restore reply** works after loading. Only the fake keys are stored as plain text; the real values stay encrypted.

## Mask and unmask

`mask()` replaces every detected value with a fake value that keeps the same length and format, and keeps a fake-to-real map. `unmask()` uses that map to put the real values back, so `unmask(mask(text))` gives back the original text.

    PrivacyShield privacyShield = new PrivacyShield();
    MaskResult result = privacyShield.mask(prompt);

    String forAi = result.getMaskedText();      // send this to the AI
    String restored = result.unmask(aiReply);   // real values back

The same real value always gets the same fake, and a fake is never a value that already appears in the text, so unmasking cannot damage the rest of the reply. A fake Aadhaar number still has a valid Verhoeff check digit, and a fake phone keeps its +91 prefix and separators.

Run the masking demo:

    java -cp target/classes com.privacyshield.Main

It prints the original text, the masked text, the restored text and the fake-to-real pairs, then checks the round trip.

## Encrypt saved values

    CryptoService crypto = new CryptoService();
    String saved = crypto.encrypt(realValue, masterPassword);
    String realValueAgain = crypto.decrypt(saved, masterPassword);

The master password is turned into a key with PBKDF2WithHmacSHA256 (65,536 iterations, random 16-byte salt). Values are encrypted with AES-GCM using a new random 12-byte IV every time. A wrong password or changed data throws `VaultLockedException`. The **Vault** tab and `VaultStore` use this service for every real value they save.

## Limits

- Detection is format based. It can miss a value written in an unusual way and can flag a lookalike. Check the masked text before sharing.
- The vault protects the real values, not the fact that a vault exists: the fake keys are stored as plain text. Anyone who has both the file and the master password can read the values, so keep the file private. A lost password cannot be recovered.
- Restoring a reply replaces exact fake text. If the AI changes a fake value's spelling or spacing, that value will not be restored.
