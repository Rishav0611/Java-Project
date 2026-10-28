package com.privacyshield;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

/**
 * The Dashboard tab.
 *
 * Shows how many text values, OCR areas and QR codes were covered in this
 * window, how many photos were cleaned and a type-wise table. It also runs a
 * small detector self-check on made-up samples. Nothing is written to disk.
 */
public class DashboardPanel extends JPanel {
    private final ProtectionMetrics metrics;

    private final JLabel maskedValue = new JLabel("0");
    private final JLabel ocrValue = new JLabel("0");
    private final JLabel qrValue = new JLabel("0");
    private final JLabel photosValue = new JLabel("0");
    private final JLabel areasLabel = new JLabel(" ");
    private final JLabel checkLabel = new JLabel();

    private final TypeTableModel tableModel;

    public DashboardPanel(ProtectionMetrics metrics) {
        if (metrics == null) {
            throw new IllegalArgumentException("metrics cannot be null");
        }

        this.metrics = metrics;
        this.tableModel = new TypeTableModel(metrics);

        buildUi();

        metrics.addChangeListener(new Runnable() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                });
            }
        });

        refresh();
        showCheck(DetectionBenchmark.run());
    }

    /**
     * Re-reads the counters. The dashboard also calls this when they change.
     */
    public void refresh() {
        maskedValue.setText(String.valueOf(metrics.getMaskedValues()));
        ocrValue.setText(String.valueOf(metrics.getOcrValues()));
        qrValue.setText(String.valueOf(metrics.getQrCodes()));
        photosValue.setText(String.valueOf(metrics.getPhotosCleaned()));
        areasLabel.setText("Redacted photo areas: " + metrics.getRedactedAreas()
                + "   |   Stripped photos: " + metrics.getPhotosStripped()
                + "   |   Redacted photos: " + metrics.getPhotosRedacted());
        tableModel.fireTableDataChanged();
    }

    private void buildUi() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Protection dashboard");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JLabel intro = new JLabel("Counts for this window only. No personal values "
                + "and no statistics are saved to disk.");

        JPanel header = new JPanel(new BorderLayout(0, 2));
        header.add(title, BorderLayout.NORTH);
        header.add(intro, BorderLayout.SOUTH);

        JPanel cards = new JPanel(new GridLayout(1, 4, 10, 0));
        cards.add(createCard("Text values masked", maskedValue));
        cards.add(createCard("OCR areas covered", ocrValue));
        cards.add(createCard("QR codes found", qrValue));
        cards.add(createCard("Photos cleaned", photosValue));

        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(140);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);

        JLabel tableTitle = new JLabel("Values by type");
        tableTitle.setFont(tableTitle.getFont().deriveFont(Font.BOLD));

        JPanel tablePanel = new JPanel(new BorderLayout(0, 4));
        tablePanel.add(tableTitle, BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
        tablePanel.add(areasLabel, BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.add(cards, BorderLayout.NORTH);
        center.add(tablePanel, BorderLayout.CENTER);

        JButton checkButton = new JButton("Run detector check");
        checkButton.setToolTipText("Run the built-in detectors on synthetic samples");

        JButton resetButton = new JButton("Reset stats");
        resetButton.setToolTipText("Clear the counts for this window");

        checkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showCheck(DetectionBenchmark.run());
            }
        });

        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                askReset();
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(checkButton);
        buttons.add(resetButton);

        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        footer.add(checkLabel, BorderLayout.CENTER);
        footer.add(buttons, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    private static JPanel createCard(String caption, JLabel value) {
        value.setFont(value.getFont().deriveFont(Font.BOLD, 26f));

        JLabel label = new JLabel(caption);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xB0, 0xB0, 0xB0)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        panel.add(label, BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private void askReset() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Reset the dashboard counts for this window?",
                "Reset statistics", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (answer == JOptionPane.YES_OPTION) {
            metrics.reset();
        }
    }

    private void showCheck(DetectionBenchmark.Result result) {
        String extra = result.getMissedValues() == 0 && result.getFalsePositives() == 0
                ? " No misses or false positives."
                : " Missed " + result.getMissedValues() + ", false positives "
                        + result.getFalsePositives() + ".";

        checkLabel.setText(String.format(Locale.ROOT,
                "<html><b>Detector self-check:</b> %.0f%% recall and %.0f%% precision "
                        + "on %d synthetic samples (%d values).%s</html>",
                result.getRecall() * 100.0,
                result.getPrecision() * 100.0,
                result.getSamples(),
                result.getExpectedValues(),
                extra));
    }

    /**
     * Four read-only rows, one for each data type.
     */
    private static final class TypeTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Type", "Text masked", "OCR covered", "Total"};
        private final ProtectionMetrics metrics;

        private TypeTableModel(ProtectionMetrics metrics) {
            this.metrics = metrics;
        }

        @Override
        public int getRowCount() {
            return DataType.values().length;
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 0 ? String.class : Integer.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Object getValueAt(int row, int column) {
            DataType type = DataType.values()[row];

            switch (column) {
                case 0:
                    return HighlightPalette.label(type);
                case 1:
                    return metrics.getMaskedCount(type);
                case 2:
                    return metrics.getOcrCount(type);
                case 3:
                    return metrics.getCoveredCount(type);
                default:
                    return 0;
            }
        }
    }
}
