package com.privacyshield;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Turns a master password into a key with PBKDF2, then uses AES-GCM to
 * encrypt and decrypt text. Only the standard Java library is used.
 *
 * Every encryption gets a new random salt and a new random IV, so the same
 * text never produces the same encrypted result twice.
 */
public class CryptoService {
    private static final int ITERATIONS = 65536;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final byte VERSION = 1;

    private final SecureRandom random = new SecureRandom();

    /**
     * Encrypts text and returns one Base64 string that holds the version,
     * the salt, the IV and the encrypted bytes.
     */
    public String encrypt(String plainText, char[] password) {
        if (plainText == null) {
            throw new IllegalArgumentException("plainText cannot be null");
        }
        checkPassword(password);

        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);

        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);

        try {
            byte[] cipherText = crypt(
                    Cipher.ENCRYPT_MODE,
                    plainText.getBytes(StandardCharsets.UTF_8),
                    password,
                    salt,
                    iv
            );

            ByteBuffer buffer = ByteBuffer.allocate(1 + SALT_BYTES + IV_BYTES + cipherText.length);
            buffer.put(VERSION);
            buffer.put(salt);
            buffer.put(iv);
            buffer.put(cipherText);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not encrypt the text.", e);
        }
    }

    /**
     * Decrypts a string made by encrypt. A wrong password or changed data
     * throws VaultLockedException.
     */
    public String decrypt(String encryptedText, char[] password) {
        if (encryptedText == null) {
            throw new IllegalArgumentException("encryptedText cannot be null");
        }
        checkPassword(password);

        byte[] data;

        try {
            data = Base64.getDecoder().decode(encryptedText);
        } catch (IllegalArgumentException e) {
            throw new VaultLockedException("Encrypted text is not valid Base64.", e);
        }

        if (data.length <= 1 + SALT_BYTES + IV_BYTES) {
            throw new VaultLockedException("Encrypted text is too short.");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte version = buffer.get();

        if (version != VERSION) {
            throw new VaultLockedException("Unknown encryption version: " + version);
        }

        byte[] salt = new byte[SALT_BYTES];
        buffer.get(salt);

        byte[] iv = new byte[IV_BYTES];
        buffer.get(iv);

        byte[] cipherText = new byte[buffer.remaining()];
        buffer.get(cipherText);

        try {
            byte[] plainText = crypt(Cipher.DECRYPT_MODE, cipherText, password, salt, iv);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new VaultLockedException("Wrong password or damaged data.", e);
        }
    }

    private byte[] crypt(int mode, byte[] input, char[] password, byte[] salt, byte[] iv)
            throws GeneralSecurityException {
        SecretKeySpec key = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, iv));

        return cipher.doFinal(input);
    }

    private SecretKeySpec deriveKey(char[] password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } finally {
            spec.clearPassword();
        }
    }

    private void checkPassword(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("password cannot be null or empty");
        }
    }
}
