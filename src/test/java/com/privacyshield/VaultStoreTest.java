package com.privacyshield;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the SQLite vault.
 *
 * Every test uses a temporary folder and made-up values, so no file from the
 * real home folder is touched.
 */
class VaultStoreTest {
    private static final char[] PASSWORD = "correct horse battery".toCharArray();

    @TempDir
    Path folder;

    @Test
    void savedValuesCanBeLoadedWithTheSamePassword() {
        VaultStore store = new VaultStore(vaultFile());
        Map<String, String> values = sampleValues();

        store.save(PASSWORD, values);

        assertTrue(store.exists());
        assertEquals(2, store.count());
        assertEquals(values, store.load(PASSWORD));
    }

    @Test
    void wrongPasswordIsReportedAsLocked() {
        VaultStore store = new VaultStore(vaultFile());
        store.save(PASSWORD, sampleValues());

        assertThrows(VaultLockedException.class,
                () -> store.load("wrong password".toCharArray()));
    }

    @Test
    void realValuesAreNotStoredAsPlainText() throws Exception {
        VaultStore store = new VaultStore(vaultFile());
        store.save(PASSWORD, sampleValues());

        String fileText = new String(Files.readAllBytes(vaultFile().toPath()),
                StandardCharsets.UTF_8);

        assertTrue(fileText.contains("priya@fake.example"));
        assertFalse(fileText.contains("priya.sharma@example.com"));
        assertFalse(fileText.contains("98765 43210"));
    }

    @Test
    void savingAgainReplacesTheOldEntries() {
        VaultStore store = new VaultStore(vaultFile());
        store.save(PASSWORD, sampleValues());

        Map<String, String> smaller = new LinkedHashMap<String, String>();
        smaller.put("one@fake.example", "one.person@example.com");

        store.save(PASSWORD, smaller);

        assertEquals(1, store.count());
        assertEquals(smaller, store.load(PASSWORD));
    }

    @Test
    void emptyMapClearsTheFile() {
        VaultStore store = new VaultStore(vaultFile());
        store.save(PASSWORD, sampleValues());

        store.save(PASSWORD, new LinkedHashMap<String, String>());

        assertEquals(0, store.count());
        assertTrue(store.load(PASSWORD).isEmpty());
    }

    @Test
    void deleteRemovesTheFileAndCountStaysSafe() {
        VaultStore store = new VaultStore(vaultFile());
        store.save(PASSWORD, sampleValues());

        assertTrue(store.delete());

        assertFalse(store.exists());
        assertEquals(0, store.count());
        assertFalse(store.delete());
    }

    @Test
    void loadingWithoutAFileFailsClearly() {
        VaultStore store = new VaultStore(vaultFile());

        assertThrows(IllegalStateException.class, () -> store.load(PASSWORD));
        assertEquals(0, store.count());
    }

    @Test
    void nullArgumentsAreRejected() {
        VaultStore store = new VaultStore(vaultFile());

        assertThrows(IllegalArgumentException.class, () -> new VaultStore(null));
        assertThrows(IllegalArgumentException.class, () -> new VaultStore(vaultFile(), null));
        assertThrows(IllegalArgumentException.class, () -> store.save(null, sampleValues()));
        assertThrows(IllegalArgumentException.class,
                () -> store.save("".toCharArray(), sampleValues()));
        assertThrows(IllegalArgumentException.class, () -> store.save(PASSWORD, null));
        assertThrows(IllegalArgumentException.class, () -> store.load(null));
    }

    private File vaultFile() {
        return folder.resolve("test-vault.db").toFile();
    }

    private static Map<String, String> sampleValues() {
        Map<String, String> values = new LinkedHashMap<String, String>();
        values.put("priya@fake.example", "priya.sharma@example.com");
        values.put("+91 90000 11111", "98765 43210");
        return values;
    }
}
