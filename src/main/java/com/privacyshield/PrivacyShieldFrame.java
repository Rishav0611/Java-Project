package com.privacyshield;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * The PrivacyShield desktop window.
 *
 * The left side shows the original text with detected values highlighted. The
 * masked text is shown next to it, ready to copy into an AI chat. The bottom
 * row restores the real values in an AI reply.
 */
public class PrivacyShieldFrame extends JFrame {
    private static final String SAMPLE_TEXT =
            "Email: person@example.com\n"
                    + "Phone: +91 98765 43210\n"
                    + "PAN: ABCPD1234E\n"
                    + "Aadhaar: 2345 6789 0124";

    private final PrivacyShield privacyShield = new PrivacyShield();
    private final SessionVault vault;

    private final JTextPane originalPane = createTextPane(true);
    private final JTextPane maskedPane = createTextPane(false);
    private final JTextPane replyPane = createTextPane(true);
    private final JTextPane restoredPane = createTextPane(false);

    private final JComboBox<MaskMode> modeBox = new JComboBox<MaskMode>(MaskMode.values());
    private final JButton themeButton = new JButton("Dark theme");
    private final JLabel statusLabel = new JLabel(" ");

    private boolean darkTheme;

    private final DocumentListener originalListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent event) {
            scheduleOriginalChange();
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
            scheduleOriginalChange();
        }

        @Override
        public void changedUpdate(DocumentEvent event) {
            // Attribute changes do not change the text, so nothing to do here.
        }
    };

    public PrivacyShieldFrame() {
        this(new SessionVault());
    }

    public PrivacyShieldFrame(SessionVault vault) {
        super("PrivacyShield - hide personal data before sharing");

        if (vault == null) {
            throw new IllegalArgumentException("vault cannot be null");
        }

        this.vault = vault;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(920, 640));
        setLocationByPlatform(true);

        buildUi();

        originalPane.getDocument().addDocumentListener(originalListener);
        originalPane.setText(SAMPLE_TEXT);
        refreshHighlights();
        setStatus("Edit the text or paste your own, then press Mask.");

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                // The fake-to-real map lives in memory for this window only.
                PrivacyShieldFrame.this.vault.delete();
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton maskButton = new JButton("Mask");
        maskButton.setToolTipText("Detect personal data and replace it in the masked text");

        JButton restoreButton = new JButton("Restore reply");
        restoreButton.setToolTipText("Put the real values back into the AI reply");

        JButton keepDeleteButton = new JButton("Keep/Delete...");
        keepDeleteButton.setToolTipText("Choose whether to keep the values in memory for restoring");

        toolbar.add(maskButton);
        toolbar.add(restoreButton);
        toolbar.add(keepDeleteButton);
        toolbar.addSeparator();
        toolbar.add(new JLabel(" Mask mode: "));
        modeBox.setSelectedItem(MaskMode.getDefault());
        modeBox.setToolTipText("Fake values can be restored later; [REDACTED] cannot");
        toolbar.add(modeBox);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(themeButton);

        maskButton.addActionListener(event -> maskText());
        restoreButton.addActionListener(event -> restoreReply());
        keepDeleteButton.addActionListener(event -> askKeepOrDelete());
        themeButton.addActionListener(event -> toggleTheme());

        add(toolbar, BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildSouth(), BorderLayout.SOUTH);
    }

    private JComponent buildCenter() {
        JSplitPane top = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                labeledPanel("Original text (edit me)", originalPane),
                labeledPanel("Masked text (safe to share)", maskedPane));
        top.setResizeWeight(0.5);
        top.setOneTouchExpandable(true);

        JSplitPane bottom = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                labeledPanel("AI reply (paste here)", replyPane),
                labeledPanel("Restored text (real values)", restoredPane));
        bottom.setResizeWeight(0.5);
        bottom.setOneTouchExpandable(true);

        JSplitPane center = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        center.setResizeWeight(0.6);
        center.setOneTouchExpandable(true);
        return center;
    }

    private JComponent buildSouth() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        legend.add(new JLabel("Highlight legend:"));

        for (DataType type : DataType.values()) {
            legend.add(createLegendChip(type));
        }

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 6, 8));
        panel.add(legend, BorderLayout.NORTH);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 2));
        panel.add(statusLabel, BorderLayout.SOUTH);
        return panel;
    }

    private static JPanel labeledPanel(String title, JTextPane textPane) {
        JLabel header = new JLabel(title);
        header.setFont(header.getFont().deriveFont(Font.BOLD));

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(textPane), BorderLayout.CENTER);
        return panel;
    }

    private static JTextPane createTextPane(boolean editable) {
        JTextPane pane = new JTextPane();
        pane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        pane.setEditable(editable);
        pane.setMargin(new Insets(8, 8, 8, 8));
        return pane;
    }

    private static JLabel createLegendChip(DataType type) {
        JLabel chip = new JLabel(" " + HighlightPalette.label(type) + " ");
        chip.setOpaque(true);
        chip.setBackground(HighlightPalette.background(type));
        chip.setForeground(HighlightPalette.foreground(type));
        chip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xA0, 0xA0, 0xA0)),
                BorderFactory.createEmptyBorder(1, 5, 1, 5)
        ));
        return chip;
    }

    private void maskText() {
        MaskMode mode = (MaskMode) modeBox.getSelectedItem();

        if (mode == null) {
            mode = MaskMode.getDefault();
        }

        MaskResult result = vault.mask(originalPane.getText(), mode);
        setPaneText(maskedPane, result.getMaskedText());
        applyHighlights(maskedPane, privacyShield.detect(maskedPane.getText()));
        setPaneText(restoredPane, "");

        int count = result.getDetections().size();

        if (count == 0) {
            setStatus("No personal data found. The masked text is the same as the original.");
        } else if (mode == MaskMode.FAKE) {
            setStatus(count + " value(s) replaced with fake values. "
                    + "The real values stay in memory so the reply can be restored.");
        } else {
            setStatus(count + " value(s) replaced with [REDACTED]. "
                    + "This mode cannot restore the values.");
        }
    }

    private void restoreReply() {
        String reply = replyPane.getText();

        if (reply == null || reply.isEmpty()) {
            setStatus("Paste the AI reply in the box first.");
            return;
        }

        if (!vault.hasValues()) {
            setPaneText(restoredPane, "");
            setStatus("No saved fake values. Mask with fake values first, "
                    + "and do not choose Delete.");
            return;
        }

        String restored = vault.restore(reply);
        setPaneText(restoredPane, restored);

        if (restored.equals(reply)) {
            setStatus("The reply did not contain any fake values. Nothing changed.");
        } else {
            setStatus("Real values restored in the preview on the right.");
        }
    }

    private void askKeepOrDelete() {
        if (!vault.hasValues()) {
            setStatus("There are no saved values to keep or delete.");
            return;
        }

        Object[] options = {
                KeepPolicy.KEEP.getLabel(),
                KeepPolicy.DELETE.getLabel()
        };

        int choice = JOptionPane.showOptionDialog(
                this,
                "Keep the fake-to-real map in memory so the AI reply can be restored?\n"
                        + "Delete forgets it now. Delete is the safer default.",
                "Keep or delete saved values",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                KeepPolicy.getDefault().getLabel()
        );

        if (choice == 0) {
            vault.keep();
            setStatus("Saved values kept in memory for this window session.");
        } else {
            vault.delete();
            setPaneText(restoredPane, "");
            setStatus("Saved values deleted. Restoring is no longer possible.");
        }
    }

    private void toggleTheme() {
        darkTheme = !darkTheme;

        if (darkTheme) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }

        SwingUtilities.updateComponentTreeUI(this);
        themeButton.setText(darkTheme ? "Light theme" : "Dark theme");
        revalidate();
        repaint();
        refreshHighlights();
        setStatus(darkTheme ? "Dark theme on." : "Light theme on.");
    }

    private void refreshHighlights() {
        applyHighlights(originalPane, privacyShield.detect(originalPane.getText()));
        applyHighlights(maskedPane, privacyShield.detect(maskedPane.getText()));
    }

    private void scheduleOriginalChange() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                applyHighlights(originalPane, privacyShield.detect(originalPane.getText()));

                // A masked text from older input must not be shared by mistake.
                if (!maskedPane.getText().isEmpty()) {
                    setPaneText(maskedPane, "");
                    setPaneText(restoredPane, "");
                    setStatus("The original text changed. Press Mask to create fresh masked text.");
                }
            }
        });
    }

    private static void applyHighlights(JTextPane pane, List<Detection> detections) {
        StyledDocument document = pane.getStyledDocument();

        SimpleAttributeSet normal = new SimpleAttributeSet();
        StyleConstants.setBackground(normal, pane.getBackground());
        StyleConstants.setForeground(normal, pane.getForeground());
        document.setCharacterAttributes(0, document.getLength(), normal, true);

        for (Detection detection : detections) {
            int start = detection.getStart();
            int end = detection.getEnd();

            if (start < 0 || end <= start || end > document.getLength()) {
                continue;
            }

            SimpleAttributeSet attributes = new SimpleAttributeSet();
            StyleConstants.setBackground(attributes, HighlightPalette.background(detection.getType()));
            StyleConstants.setForeground(attributes, HighlightPalette.foreground(detection.getType()));
            document.setCharacterAttributes(start, end - start, attributes, true);
        }
    }

    private static void setPaneText(JTextPane pane, String text) {
        pane.setText(text == null ? "" : text);
        pane.setCaretPosition(0);
    }

    private void setStatus(String message) {
        if (message == null || message.isEmpty()) {
            statusLabel.setText(" ");
        } else {
            statusLabel.setText(message);
        }
    }
}
