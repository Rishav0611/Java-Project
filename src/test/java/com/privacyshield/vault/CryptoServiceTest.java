package com.privacyshield.vault;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CryptoServiceTest {

    private final byte[] salt = CryptoService.newSalt();
    private final CryptoService crypto = new CryptoService("pass123".toCharArray(), salt);
    private final byte[] plain = "9876543210".getBytes(StandardCharsets.UTF_8);

    @Test
    void decryptReturnsOriginal() throws Exception {
        assertArrayEquals(plain, crypto.decrypt(crypto.encrypt(plain)));
    }

    @Test
    void sameInputEncryptsDifferentlyEachTime() {
        assertFalse(java.util.Arrays.equals(crypto.encrypt(plain), crypto.encrypt(plain)));
    }

    @Test
    void wrongPasswordFails() {
        byte[] blob = crypto.encrypt(plain);
        CryptoService other = new CryptoService("wrong".toCharArray(), salt);

        assertThrows(GeneralSecurityException.class, () -> other.decrypt(blob));
    }

    @Test
    void tamperedBlobFails() {
        byte[] blob = crypto.encrypt(plain);
        blob[blob.length - 1] ^= 1;

        assertThrows(GeneralSecurityException.class, () -> crypto.decrypt(blob));
    }
}