package com.privacyshield;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores the fake-to-real map in a small SQLite file.
 *
 * The fake value is the key and is stored as plain text so the file can be
 * searched later. Only the real value is encrypted, with AES-GCM and the
 * master password from {@link CryptoService}. The password itself is never
 * saved. A wrong password is reported as {@link VaultLockedException}.
 */
public class VaultStore {
    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS vault_entries ("
                    + "masked TEXT PRIMARY KEY, "
                    + "encrypted TEXT NOT NULL, "
                    + "created_at TEXT NOT NULL)";

    private final File file;
    private final CryptoService crypto;

    public VaultStore(File file) {
        this(file, new CryptoService());
    }

    public VaultStore(File file, CryptoService crypto) {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null");
        }
        if (crypto == null) {
            throw new IllegalArgumentException("crypto cannot be null");
        }

        this.file = file;
        this.crypto = crypto;
    }

    public File getFile() {
        return file;
    }

    public boolean exists() {
        return file.isFile();
    }

    /**
     * Saves the map, replacing everything that was in the vault before.
     * An empty map clears the saved entries.
     */
    public void save(char[] password, Map<String, String> fakeToReal) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("password cannot be null or empty");
        }
        if (fakeToReal == null) {
            throw new IllegalArgumentException("fakeToReal cannot be null");
        }

        Map<String, String> encrypted = new LinkedHashMap<String, String>();

        for (Map.Entry<String, String> entry : fakeToReal.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new IllegalArgumentException("fake and real values cannot be null");
            }

            encrypted.put(entry.getKey(), crypto.encrypt(entry.getValue(), password));
        }

        try (Connection connection = connect()) {
            createTable(connection);
            connection.setAutoCommit(false);

            try {
                deleteRows(connection);

                String sql = "INSERT INTO vault_entries (masked, encrypted, created_at) "
                        + "VALUES (?, ?, ?)";
                String now = Instant.now().toString();

                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    for (Map.Entry<String, String> entry : encrypted.entrySet()) {
                        statement.setString(1, entry.getKey());
                        statement.setString(2, entry.getValue());
                        statement.setString(3, now);
                        statement.addBatch();
                    }

                    statement.executeBatch();
                }

                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save the vault.", exception);
        }
    }

    /**
     * Reads every entry and decrypts the real values with the password.
     * Throws VaultLockedException when the password is wrong.
     */
    public Map<String, String> load(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("password cannot be null or empty");
        }
        if (!file.isFile()) {
            throw new IllegalStateException("No vault file at " + file.getPath());
        }

        Map<String, String> fakeToReal = new LinkedHashMap<String, String>();

        try (Connection connection = connect()) {
            createTable(connection);

            String sql = "SELECT masked, encrypted FROM vault_entries ORDER BY masked";

            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(sql)) {
                while (rows.next()) {
                    String masked = rows.getString("masked");
                    String real = crypto.decrypt(rows.getString("encrypted"), password);
                    fakeToReal.put(masked, real);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load the vault.", exception);
        }

        return fakeToReal;
    }

    /**
     * Number of saved entries. This works without the master password,
     * because only fake keys and encrypted values are in the file.
     */
    public int count() {
        if (!file.isFile()) {
            return 0;
        }

        try (Connection connection = connect()) {
            createTable(connection);

            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM vault_entries")) {
                return rows.next() ? rows.getInt(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read the vault.", exception);
        }
    }

    /**
     * Deletes the vault file, so nothing is left on the disk.
     * Returns false when there was no file.
     */
    public boolean delete() {
        if (!file.exists()) {
            return false;
        }

        boolean deleted = file.delete();

        if (!deleted) {
            throw new IllegalStateException("Could not delete " + file.getPath());
        }

        // SQLite can leave helper files next to a database.
        deleteQuietly(new File(file.getPath() + "-journal"));
        deleteQuietly(new File(file.getPath() + "-wal"));
        deleteQuietly(new File(file.getPath() + "-shm"));
        return true;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
    }

    private static void createTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_TABLE);
        }
    }

    private static void deleteRows(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM vault_entries");
        }
    }

    private static void deleteQuietly(File helper) {
        if (helper.exists()) {
            helper.delete();
        }
    }
}
