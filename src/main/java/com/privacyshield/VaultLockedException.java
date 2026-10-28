package com.privacyshield;

/**
 * Thrown when encrypted data cannot be opened, for example because the
 * master password is wrong, the data was changed, or the vault is locked.
 */
public class VaultLockedException extends RuntimeException {

    public VaultLockedException(String message) {
        super(message);
    }

    public VaultLockedException(String message, Throwable cause) {
        super(message, cause);
    }
}
