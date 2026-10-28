package com.privacyshield;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link CryptoService} and {@link VaultLockedException}.
 */
class CryptoServiceTest {

    private final CryptoService crypto = new CryptoService();
    private final char[] password = "correct horse battery staple".toCharArray();

    @Test
    void encryptThenDecryptGivesBackTheText() {
        String text = "person@example.com +91 98765 43210";

        assertEquals(text, crypto.decrypt(crypto.encrypt(text, password), password));
    }

    @Test
    void encryptsUnicodeAndLongText() {
        String unicode = "नमस्ते दुनिया - secure message";
        String longText = repeat("Aadhaar 2345 6789 0124. ", 100);

        assertEquals(unicode, crypto.decrypt(crypto.encrypt(unicode, password), password));
        assertEquals(longText, crypto.decrypt(crypto.encrypt(longText, password), password));
    }

    @Test
    void encryptsEmptyText() {
        assertEquals("", crypto.decrypt(crypto.encrypt("", password), password));
    }

    @Test
    void sameTextEncryptsDifferentlyEveryTime() {
        String first = crypto.encrypt("hello", password);
        String second = crypto.encrypt("hello", password);

        assertNotEquals(first, second);
        assertEquals("hello", crypto.decrypt(first, password));
        assertEquals("hello", crypto.decrypt(second, password));
    }

    @Test
    void encryptedTextDoesNotShowThePlainText() {
        String encrypted = crypto.encrypt("my secret value", password);

        assertFalse(encrypted.contains("secret"));
        assertTrue(encrypted.length() > 40);
    }

    @Test
    void wrongPasswordThrowsVaultLockedException() {
        String encrypted = crypto.encrypt("hello", password);
        char[] wrong = "wrong password".toCharArray();

        assertThrows(VaultLockedException.class, () -> crypto.decrypt(encrypted, wrong));
    }

    @Test
    void changedDataThrowsVaultLockedException() {
        String encrypted = crypto.encrypt("hello", password);
        byte[] data = Base64.getDecoder().decode(encrypted);

        data[data.length - 1] = (byte) (data[data.length - 1] ^ 0x01);

        final String tampered = Base64.getEncoder().encodeToString(data);

        assertThrows(VaultLockedException.class, () -> crypto.decrypt(tampered, password));
    }

    @Test
    void malformedInputThrowsVaultLockedException() {
        assertThrows(VaultLockedException.class, () -> crypto.decrypt("not base64!!", password));
        assertThrows(VaultLockedException.class,
                () -> crypto.decrypt(Base64.getEncoder().encodeToString(new byte[]{1, 2, 3}), password));
        assertThrows(VaultLockedException.class,
                () -> crypto.decrypt(Base64.getEncoder().encodeToString(new byte[40]), password));
    }

    @Test
    void rejectsNullAndEmptyPasswords() {
        assertThrows(IllegalArgumentException.class, () -> crypto.encrypt("hello", null));
        assertThrows(IllegalArgumentException.class, () -> crypto.encrypt("hello", new char[0]));
        assertThrows(IllegalArgumentException.class, () -> crypto.decrypt("abc", null));
        assertThrows(IllegalArgumentException.class, () -> crypto.decrypt("abc", new char[0]));
    }

    @Test
    void rejectsNullText() {
        assertThrows(IllegalArgumentException.class, () -> crypto.encrypt(null, password));
        assertThrows(IllegalArgumentException.class, () -> crypto.decrypt(null, password));
    }

    private String repeat(String text, int times) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < times; i++) {
            builder.append(text);
        }

        return builder.toString();
    }
}
