package com.privacyshield;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The Clipboard tab.
 *
 * Shows what is on the clipboard, highlights personal values, and can replace
 * them with [REDACTED]. Watching is off by default; when it is on, the panel
 * checks the clipboard about once a second and cleans new text automatically.
 *
 * Only plain text is read. Nothing is saved, and the text is never uploaded.
 */
public class ClipboardPanel extends JPanel {
    private static final int CHECK_DELAY_MS = 1200;

    private final ClipboardShield shield;
    private final ProtectionMetrics metrics;

    private final JCheckBox watchBox = new JCheckBox("Watch clipboard");
    private final JButton checkButton = new JButton("Check now");
    private final JButton cleanButton = new JButton("Clean now");
    private final JTextPane previewPane = createPreviewPane();
    private final JLabel statusLabel = new JLabel(" ");
    private final Timer timer;

    private String lastText;

    public ClipboardPanel() {
        this(new ProtectionMetrics());
    }

    public ClipboardPanel(ProtectionMetrics metrics) {
        this(new ClipboardShield(), metrics);
    }

    public ClipboardPanel(ClipboardShield shield, ProtectionMetrics metrics) {
        if (shield == null) {
            throw new IllegalArgumentException("shield cannot be null");
        }
        if (metrics == null) {
            throw new IllegalArgumentException("metrics cannot be null");
        }

        this.shield = shield;
        this.metrics = metrics;
        this.timer = new Timer(CHECK_DELAY_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                checkClipboard();
            }
        });

        buildUi();
        setStatus("Clipboard watch is off. Copy text, then press Check now.");
    }

    /**
     * Stops the timer, for example when the window closes.
     */
    public void stopWatching() {
        timer.stop();

        if (watchBox.isSelected()) {
            watchBox.setSelected(false);
        }
    }

    private void buildUi() {
        setLayout(new BorderLayout());

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        watchBox.setToolTipText("Check new clipboard text and replace personal values with [REDACTED]");
        checkButton.setToolTipText("Read the clipboard and show what personal data it contains");
        cleanButton.setToolTipText("Replace personal values on the clipboard with [REDACTED]");

        watchBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                watchChanged();
            }
        });

        checkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                checkNow();
            }
        });

        cleanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                cleanNow();
            }
        });

        toolbar.add(watchBox);
        toolbar.addSeparator();
        toolbar.add(checkButton);
        toolbar.add(cleanButton);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(new JLabel("Automatic cleaning uses [REDACTED]. "));

        add(toolbar, BorderLayout.NORTH);
        add(buildPreviewPanel(), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createEmptyBorder(0, 8, 6, 8));
        south.add(statusLabel, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    private JPanel buildPreviewPanel() {
        JLabel title = new JLabel("Clipboard preview");
        title.setFont(title.getFont().deriveFont(Font.BOLD));

        JLabel hint = new JLabel("Plain text only. The clipboard is checked on this computer "
                + "and nothing is saved.");

        JPanel header = new JPanel(new BorderLayout(0, 2));
        header.add(title, BorderLayout.NORTH);
        header.add(hint, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(previewPane), BorderLayout.CENTER);
        return panel;
    }

    private static JTextPane createPreviewPane() {
        JTextPane pane = new JTextPane();
        pane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        pane.setEditable(false);
        pane.setMargin(new Insets(8, 8, 8, 8));
        return pane;
    }

    private void watchChanged() {
        if (watchBox.isSelected()) {
            lastText = null;
            timer.start();
            setStatus("Watching the clipboard. Personal values will be replaced with [REDACTED].");
            checkClipboard();
        } else {
            timer.stop();
            setStatus("Clipboard watch is off.");
        }
    }

    private void checkNow() {
        try {
            ClipboardCheck check = shield.inspect();
            lastText = check.getText();
            showCheck(check, true);

            if (check.getText().isEmpty()) {
                setStatus("The clipboard is empty.");
            } else if (check.hasSensitiveData()) {
                setStatus(describe(check) + " Press Clean now to redact the clipboard.");
            } else {
                setStatus("Clipboard checked. " + describe(check));
            }
        } catch (RuntimeException exception) {
            showClipboardError(exception);
        }
    }

    private void cleanNow() {
        try {
            ClipboardCheck result = shield.clean();
            lastText = result.getText();
            showCheck(result, false);

            if (result.isCleaned()) {
                metrics.recordClipboard(result.getDetections());
                setStatus(describe(result) + " Replaced with [REDACTED] on the clipboard.");
            } else if (result.getText().isEmpty()) {
                setStatus("The clipboard is empty, so there is nothing to clean.");
            } else {
                setStatus("Nothing to clean. " + describe(result));
            }
        } catch (RuntimeException exception) {
            showClipboardError(exception);
        }
    }

    /**
     * Called by the timer and by the watch check box. It only looks at text
     * that changed since the last look, so it does not repeat the same message
     * every second.
     */
    private void checkClipboard() {
        try {
            ClipboardCheck check = shield.inspect();

            if (check.getText().equals(lastText)) {
                return;
            }

            lastText = check.getText();

            if (!check.hasSensitiveData()) {
                showCheck(check, true);
                setStatus("Watch is on. Clipboard checked: no personal data.");
                return;
            }

            showCheck(check, true);
            ClipboardCheck cleaned = shield.clean();
            lastText = cleaned.getText();
            showCheck(cleaned, false);
            metrics.recordClipboard(cleaned.getDetections());
            setStatus(describe(cleaned) + " Replaced with [REDACTED] on the clipboard.");
        } catch (RuntimeException exception) {
            showClipboardError(exception);
        }
    }

    private void showCheck(ClipboardCheck check, boolean highlight) {
        previewPane.setText(check.getText());
        previewPane.setCaretPosition(0);

        if (highlight) {
            TextHighlighter.apply(previewPane, check.getDetections());
        }
    }

    private static String describe(ClipboardCheck check) {
        int count = check.getValueCount();

        if (count == 0) {
            return "No Aadhaar, PAN, phone or email value found.";
        }

        Set<String> labels = new LinkedHashSet<String>();

        for (Detection detection : check.getDetections()) {
            labels.add(HighlightPalette.label(detection.getType()));
        }

        StringBuilder names = new StringBuilder();

        for (String label : labels) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(label);
        }

        return count + " personal value(s) found (" + names + ").";
    }

    private void showClipboardError(RuntimeException exception) {
        String detail = exception.getMessage();

        if (detail == null || detail.trim().isEmpty()) {
            detail = "The clipboard is not available.";
        }

        setStatus("Clipboard problem: " + detail);
    }

    private void setStatus(String message) {
        statusLabel.setText(message == null || message.isEmpty() ? " " : message);
    }
}
