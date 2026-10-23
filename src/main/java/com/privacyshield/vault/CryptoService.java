package com.privacyshield.vault;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

public class CryptoService {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final int ITERATIONS = 600_000;
    private static final int KEY_BITS = 256;

    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;

    public CryptoService(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            byte[] keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
            this.key = new SecretKeySpec(keyBytes, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Key derivation failed", e);
        } finally {
            spec.clearPassword();
        }
    }

    public static byte[] newSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    /** Output = IV (12 bytes) + ciphertext + GCM tag */
    public byte[] encrypt(byte[] plain) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain);

            byte[] out = new byte[IV_LENGTH + ct.length];
            System.arraycopy(iv, 0, out, 0, IV_LENGTH);
            System.arraycopy(ct, 0, out, IV_LENGTH, ct.length);
            return out;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] blob) throws GeneralSecurityException {
        if (blob.length < IV_LENGTH + TAG_BITS / 8) {
            throw new GeneralSecurityException("Blob too short");
        }
        byte[] iv = Arrays.copyOfRange(blob, 0, IV_LENGTH);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        return cipher.doFinal(blob, IV_LENGTH, blob.length - IV_LENGTH);
    }
}