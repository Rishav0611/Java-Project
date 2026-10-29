package com.privacyshield;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * The Vault tab.
 *
 * Saves the current fake-to-real map into a small SQLite file, protected by a
 * master password. Loading the file puts the map back into the session, so the
 * Text tab can restore an AI reply later, even after the window was closed.
 *
 * Only fake values are stored as plain text. Real values are encrypted and the
 * password is never saved.
 */
public class VaultPanel extends JPanel {
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final SessionVault vault;
    private final VaultStore store;

    private final JPasswordField passwordField = new JPasswordField(18);
    private final JButton saveButton = new JButton("Save current values");
    private final JButton loadButton = new JButton("Load into Text tab");
    private final JButton deleteButton = new JButton("Delete vault");
    private final JLabel fileLabel = new JLabel(" ");
    private final JLabel statusLabel = new JLabel(" ");

    public VaultPanel(SessionVault vault) {
        this(vault, new VaultStore(defaultFile()));
    }

    public VaultPanel(SessionVault vault, VaultStore store) {
        if (vault == null) {
            throw new IllegalArgumentException("vault cannot be null");
        }
        if (store == null) {
            throw new IllegalArgumentException("store cannot be null");
        }

        this.vault = vault;
        this.store = store;

        buildUi();
        refreshFileLabel();
        setStatus("Mask a text with fake values, then save them here.");
    }

    /**
     * The default vault file, in the user's home folder.
     */
    public static File defaultFile() {
        return new File(System.getProperty("user.home"), "privacyshield-vault.db");
    }

    private void buildUi() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.add(statusLabel, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("Encrypted vault");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JLabel hint = new JLabel("Keep the fake-to-real map on disk so a reply can be restored later.");
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC));

        JPanel header = new JPanel(new GridLayout(0, 1, 0, 2));
        header.add(title);
        header.add(hint);
        return header;
    }

    private JPanel buildBody() {
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.add(buildPasswordRow());
        form.add(buildFileRow());

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.add(form, BorderLayout.NORTH);
        body.add(buildInfoPanel(), BorderLayout.CENTER);
        return body;
    }

    private JPanel buildPasswordRow() {
        JLabel label = new JLabel("Master password:");

        passwordField.setToolTipText("Used to encrypt the real values. It is never saved.");
        saveButton.setToolTipText("Encrypt the fake-to-real map from the Text tab and write it to the vault file");
        loadButton.setToolTipText("Read the vault and make its values available for Restore reply");
        deleteButton.setToolTipText("Delete the vault file from this computer");

        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                saveVault();
            }
        });

        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                loadVault();
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                deleteVault();
            }
        });

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.add(label);
        row.add(passwordField);
        row.add(saveButton);
        row.add(loadButton);
        row.add(deleteButton);
        return row;
    }

    private JPanel buildFileRow() {
        fileLabel.setFont(fileLabel.getFont().deriveFont(Font.PLAIN, 12f));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.add(fileLabel);
        return row;
    }

    private JPanel buildInfoPanel() {
        JPanel text = new JPanel(new GridLayout(0, 1, 0, 4));
        text.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        text.add(new JLabel("Stored in the file:"));
        text.add(new JLabel("  - fake value, so a saved entry can be found"));
        text.add(new JLabel("  - real value, encrypted with AES-GCM"));
        text.add(new JLabel("Not stored: your password, the original text or the AI reply."));
        text.add(new JLabel("Use at least " + MIN_PASSWORD_LENGTH
                + " characters. Without the password the values cannot be opened."));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("What this file holds"));
        panel.add(text, BorderLayout.NORTH);
        return panel;
    }

    private void saveVault() {
        char[] password = passwordField.getPassword();

        if (password.length < MIN_PASSWORD_LENGTH) {
            Arrays.fill(password, '\0');
            setStatus("Use a master password with at least " + MIN_PASSWORD_LENGTH + " characters.");
            return;
        }

        if (!vault.hasValues()) {
            Arrays.fill(password, '\0');
            setStatus("There are no fake values to save. Mask a text with fake values first.");
            return;
        }

        if (store.exists() && !confirmReplace()) {
            Arrays.fill(password, '\0');
            setStatus("Save cancelled. The old vault was not changed.");
            return;
        }

        final Map<String, String> values = vault.getSavedValues();
        setBusy(true);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                store.save(password, values);
                return null;
            }

            @Override
            protected void done() {
                Arrays.fill(password, '\0');
                setBusy(false);

                try {
                    get();
                    refreshFileLabel();
                    setStatus("Saved " + values.size() + " value(s) to the vault file.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    setStatus("Save was interrupted.");
                } catch (ExecutionException exception) {
                    showError("Could not save the vault.", exception);
                }
            }
        }.execute();
    }

    private void loadVault() {
        char[] password = passwordField.getPassword();

        if (password.length < MIN_PASSWORD_LENGTH) {
            Arrays.fill(password, '\0');
            setStatus("Type the master password used when the vault was saved.");
            return;
        }

        if (!store.exists()) {
            Arrays.fill(password, '\0');
            setStatus("There is no vault file yet. Save values first.");
            return;
        }

        setBusy(true);

        new SwingWorker<Map<String, String>, Void>() {
            @Override
            protected Map<String, String> doInBackground() {
                return store.load(password);
            }

            @Override
            protected void done() {
                Arrays.fill(password, '\0');
                setBusy(false);

                try {
                    Map<String, String> values = get();
                    vault.loadValues(values);
                    refreshFileLabel();

                    if (values.isEmpty()) {
                        setStatus("The vault file is empty.");
                    } else {
                        setStatus("Loaded " + values.size() + " fake value(s). Paste the AI reply "
                                + "in the Text tab and press Restore reply.");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    setStatus("Load was interrupted.");
                } catch (ExecutionException exception) {
                    showError("Could not load the vault.", exception);
                }
            }
        }.execute();
    }

    private void deleteVault() {
        if (!store.exists()) {
            setStatus("There is no vault file to delete.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Delete the vault file?\n" + store.getFile().getPath()
                        + "\n\nWithout the file the saved values cannot be restored.",
                "Delete vault",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            setStatus("Delete cancelled.");
            return;
        }

        try {
            store.delete();
            refreshFileLabel();
            setStatus("Vault file deleted from this computer.");
        } catch (RuntimeException exception) {
            showError("Could not delete the vault.", exception);
        }
    }

    private boolean confirmReplace() {
        int count = 0;

        try {
            count = store.count();
        } catch (RuntimeException exception) {
            // A damaged file can still be replaced by saving over it.
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "The vault file already holds " + count + " value(s).\nReplace them?",
                "Replace vault",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        return choice == JOptionPane.YES_OPTION;
    }

    private void refreshFileLabel() {
        String path = store.getFile().getPath();

        if (!store.exists()) {
            fileLabel.setText("Vault file: " + path + "  (not created yet)");
            fileLabel.setToolTipText(path);
            return;
        }

        try {
            fileLabel.setText("Vault file: " + path + "  (" + store.count() + " saved value(s))");
        } catch (RuntimeException exception) {
            fileLabel.setText("Vault file: " + path + "  (could not be read)");
        }

        fileLabel.setToolTipText(path);
    }

    private void setBusy(boolean busy) {
        saveButton.setEnabled(!busy);
        loadButton.setEnabled(!busy);
        deleteButton.setEnabled(!busy);
        passwordField.setEnabled(!busy);

        if (busy) {
            setStatus("Working...");
        }
    }

    private void showError(String title, Throwable throwable) {
        Throwable cause = throwable;

        if (cause instanceof ExecutionException && cause.getCause() != null) {
            cause = cause.getCause();
        }

        String detail = cause.getMessage();

        if (detail == null || detail.trim().isEmpty()) {
            detail = cause.getClass().getSimpleName();
        }

        setStatus(title + " " + detail);
        JOptionPane.showMessageDialog(this, detail, title, JOptionPane.ERROR_MESSAGE);
    }

    private void setStatus(String message) {
        statusLabel.setText(message == null || message.isEmpty() ? " " : message);
    }
}
