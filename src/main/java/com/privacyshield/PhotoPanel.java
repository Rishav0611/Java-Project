package com.privacyshield;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The Photos tab.
 *
 * The user chooses a photo, sees its GPS, camera and time metadata, and can
 * save a copy with that metadata removed. Private areas that are visible in
 * the picture can be covered with black boxes before saving.
 *
 * Reading, stripping and redacting run in a SwingWorker so the window stays
 * responsive. The original photo is never changed.
 */
public class PhotoPanel extends JPanel {
    private static final FileNameExtensionFilter PHOTO_FILTER =
            new FileNameExtensionFilter("Photos (JPEG, PNG)", "jpg", "jpeg", "png");

    private final PhotoService photoService;
    private final PhotoCanvas canvas = new PhotoCanvas();
    private final MetadataTableModel tableModel = new MetadataTableModel();
    private final JTable metadataTable = new JTable(tableModel);

    private final JButton chooseButton = new JButton("Choose photo...");
    private final JButton stripButton = new JButton("Strip metadata");
    private final JButton saveButton = new JButton("Save redacted copy");
    private final JButton undoButton = new JButton("Undo box");
    private final JButton clearButton = new JButton("Clear boxes");

    private final JLabel photoLabel = new JLabel("No photo chosen.");
    private final JLabel statusLabel = new JLabel(" ");

    private File currentPhoto;
    private BufferedImage currentImage;
    private boolean busy;

    public PhotoPanel() {
        this(new PhotoService());
    }

    public PhotoPanel(PhotoService photoService) {
        if (photoService == null) {
            throw new IllegalArgumentException("photoService cannot be null");
        }

        this.photoService = photoService;

        buildUi();
        updateButtons();
    }

    /**
     * Loads one photo in the background and shows its metadata and preview.
     */
    public void openPhoto(final File photo) {
        if (photo == null) {
            throw new IllegalArgumentException("photo cannot be null");
        }

        if (busy) {
            return;
        }

        setStatus("Reading the photo and its metadata...");
        setBusy(true);

        new SwingWorker<LoadedPhoto, Void>() {
            @Override
            protected LoadedPhoto doInBackground() throws Exception {
                List<PhotoMetadata> metadata = photoService.scan(photo);
                BufferedImage picture = ImageIO.read(photo);

                if (picture == null) {
                    throw new IOException("This file is not a photo that can be previewed: "
                            + photo.getName());
                }

                return new LoadedPhoto(photo, metadata, picture);
            }

            @Override
            protected void done() {
                try {
                    LoadedPhoto loaded = get();

                    currentPhoto = loaded.photo;
                    currentImage = loaded.image;
                    canvas.setImage(loaded.image);
                    tableModel.setItems(loaded.metadata);
                    photoLabel.setText(loaded.photo.getName() + " - "
                            + loaded.image.getWidth() + " x " + loaded.image.getHeight());
                    setStatus(describe(loaded.metadata));
                } catch (Exception exception) {
                    showError("Could not open the photo.", exception);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void buildUi() {
        setLayout(new BorderLayout());

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        chooseButton.setToolTipText("Choose a JPEG or PNG photo from this computer");
        stripButton.setToolTipText("Save a copy with GPS, camera, time and comment metadata removed");
        saveButton.setToolTipText("Save a copy with the black boxes painted on it");
        undoButton.setToolTipText("Remove the last black box");
        clearButton.setToolTipText("Remove every black box");

        toolbar.add(chooseButton);
        toolbar.add(stripButton);
        toolbar.add(saveButton);
        toolbar.addSeparator();
        toolbar.add(undoButton);
        toolbar.add(clearButton);

        chooseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                choosePhoto();
            }
        });

        stripButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                stripMetadata();
            }
        });

        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                saveRedactedCopy();
            }
        });

        undoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                canvas.removeLastBox();
                updateBoxStatus();
            }
        });

        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                canvas.clearBoxes();
                updateBoxStatus();
            }
        });

        canvas.setBoxChangeListener(new Runnable() {
            @Override
            public void run() {
                updateButtons();
                updateBoxStatus();
            }
        });

        add(toolbar, BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createEmptyBorder(0, 8, 6, 8));
        south.add(statusLabel, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        setStatus("Choose a photo to check its metadata.");
    }

    private JSplitPane buildCenter() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildPreviewPanel(), buildDetailsPanel());
        split.setResizeWeight(0.58);
        split.setOneTouchExpandable(true);
        return split;
    }

    private JPanel buildPreviewPanel() {
        JLabel title = new JLabel("Photo preview");
        title.setFont(title.getFont().deriveFont(Font.BOLD));

        JLabel hint = new JLabel("Drag on the photo to draw a black box over a private area.");
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN));

        JPanel header = new JPanel(new BorderLayout(0, 2));
        header.add(title, BorderLayout.NORTH);
        header.add(hint, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(header, BorderLayout.NORTH);
        panel.add(canvas, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildDetailsPanel() {
        JLabel title = new JLabel("Photo metadata");
        title.setFont(title.getFont().deriveFont(Font.BOLD));

        metadataTable.setFillsViewportHeight(true);
        metadataTable.setRowHeight(22);
        metadataTable.setAutoCreateRowSorter(true);
        metadataTable.getTableHeader().setReorderingAllowed(false);

        metadataTable.getColumnModel().getColumn(0).setPreferredWidth(85);
        metadataTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        metadataTable.getColumnModel().getColumn(2).setPreferredWidth(240);

        JPanel header = new JPanel(new BorderLayout(0, 2));
        header.add(title, BorderLayout.NORTH);
        header.add(photoLabel, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(metadataTable), BorderLayout.CENTER);

        JLabel note = new JLabel("<html>GPS and date fields tell others where and when a photo was taken.</html>");
        panel.add(note, BorderLayout.SOUTH);
        return panel;
    }

    private void choosePhoto() {
        if (busy) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a photo");
        chooser.setFileFilter(PHOTO_FILTER);

        if (currentPhoto != null) {
            chooser.setSelectedFile(currentPhoto);
        }

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            openPhoto(chooser.getSelectedFile());
        }
    }

    private void stripMetadata() {
        if (busy || currentPhoto == null) {
            return;
        }

        final File input = currentPhoto;
        final File output = chooseSaveFile("Save the stripped copy",
                PhotoService.defaultStrippedFile(input), input);

        if (output == null) {
            return;
        }

        setStatus("Removing metadata...");
        setBusy(true);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                photoService.strip(input, output);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("Saved " + output.getName()
                            + ". Metadata removed; the original photo is unchanged.");
                } catch (Exception exception) {
                    showError("Could not strip the metadata.", exception);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void saveRedactedCopy() {
        if (busy || currentPhoto == null) {
            return;
        }

        final List<Rectangle> boxes = new ArrayList<Rectangle>(canvas.getBoxes());

        if (boxes.isEmpty()) {
            setStatus("Drag at least one black box on the photo first.");
            return;
        }

        final File input = currentPhoto;
        final File output = chooseSaveFile("Save the redacted copy",
                PhotoService.defaultRedactedFile(input), input);

        if (output == null) {
            return;
        }

        setStatus("Painting the boxes and saving...");
        setBusy(true);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                photoService.redact(input, output, boxes);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("Saved " + output.getName()
                            + ". The boxes are black and the metadata is removed.");
                } catch (Exception exception) {
                    showError("Could not save the redacted copy.", exception);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    /**
     * Asks for an output file and makes sure it is not the original photo.
     * Returns null when the user cancels.
     */
    private File chooseSaveFile(String title, File suggested, File input) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileFilter(PHOTO_FILTER);
        chooser.setSelectedFile(suggested);

        while (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();

            if (selected == null) {
                return null;
            }

            File target = withPhotoExtension(selected, suggested);

            if (isSameFile(target, input)) {
                JOptionPane.showMessageDialog(this,
                        "Choose a different file name. The original photo is not overwritten.",
                        "Choose another name", JOptionPane.WARNING_MESSAGE);
                chooser.setSelectedFile(target);
                continue;
            }

            if (target.exists()) {
                int answer = JOptionPane.showConfirmDialog(this,
                        target.getName() + " already exists. Replace it?",
                        "Replace file?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                if (answer != JOptionPane.YES_OPTION) {
                    chooser.setSelectedFile(target);
                    continue;
                }
            }

            return target;
        }

        return null;
    }

    private static File withPhotoExtension(File selected, File suggested) {
        String name = selected.getName().toLowerCase(Locale.ROOT);

        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")) {
            return selected;
        }

        File parent = selected.getAbsoluteFile().getParentFile();
        return new File(parent, selected.getName() + extensionOf(suggested));
    }

    private static String extensionOf(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot).toLowerCase(Locale.ROOT) : ".jpg";
    }

    private static boolean isSameFile(File first, File second) {
        try {
            return first.getCanonicalFile().equals(second.getCanonicalFile());
        } catch (IOException exception) {
            return first.getAbsoluteFile().equals(second.getAbsoluteFile());
        }
    }

    private void showError(String message, Exception exception) {
        Throwable cause = exception;

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        String detail = cause.getMessage();

        if (detail == null || detail.trim().isEmpty()) {
            detail = cause.toString();
        }

        if (exception instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }

        setStatus(message + " " + detail);
        JOptionPane.showMessageDialog(this, message + "\n\n" + detail,
                "Photo problem", JOptionPane.ERROR_MESSAGE);
    }

    private void setBusy(boolean busy) {
        this.busy = busy;

        chooseButton.setEnabled(!busy);
        setCursor(busy
                ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                : Cursor.getDefaultCursor());
        updateButtons();
    }

    private void updateButtons() {
        boolean hasPhoto = currentPhoto != null;

        stripButton.setEnabled(!busy && hasPhoto);
        saveButton.setEnabled(!busy && hasPhoto && canvas.hasBoxes());
        undoButton.setEnabled(!busy && canvas.hasBoxes());
        clearButton.setEnabled(!busy && canvas.hasBoxes());
    }

    private void updateBoxStatus() {
        if (currentPhoto == null || busy) {
            return;
        }

        int count = canvas.getBoxes().size();

        if (count == 0) {
            setStatus("Drag on the photo to hide a private area.");
        } else if (count == 1) {
            setStatus("1 black box ready. Save the redacted copy when done.");
        } else {
            setStatus(count + " black boxes ready. Save the redacted copy when done.");
        }
    }

    private static String describe(List<PhotoMetadata> metadata) {
        if (metadata.isEmpty()) {
            return "No GPS, camera, time or comment metadata found in this photo.";
        }

        int locationFields = 0;

        for (PhotoMetadata item : metadata) {
            if (item.isLocation()) {
                locationFields++;
            }
        }

        String message = metadata.size() + " metadata item(s) found.";

        if (locationFields > 0) {
            message += " " + locationFields + " of them are location related.";
        }

        return message + " Strip them or draw boxes over private areas.";
    }

    private void setStatus(String message) {
        statusLabel.setText(message == null || message.isEmpty() ? " " : message);
    }

    public File getCurrentPhoto() {
        return currentPhoto;
    }

    public BufferedImage getCurrentImage() {
        return currentImage;
    }

    /**
     * What the background load returns to the window.
     */
    private static final class LoadedPhoto {
        private final File photo;
        private final List<PhotoMetadata> metadata;
        private final BufferedImage image;

        private LoadedPhoto(File photo, List<PhotoMetadata> metadata, BufferedImage image) {
            this.photo = photo;
            this.metadata = metadata;
            this.image = image;
        }
    }

    /**
     * Three read-only columns: group, tag and value.
     */
    private static final class MetadataTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Group", "Tag", "Value"};

        private List<PhotoMetadata> items = Collections.emptyList();

        public void setItems(List<PhotoMetadata> items) {
            this.items = items == null
                    ? Collections.<PhotoMetadata>emptyList()
                    : items;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return items.size();
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
            return String.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Object getValueAt(int row, int column) {
            PhotoMetadata item = items.get(row);

            switch (column) {
                case 0:
                    return item.getGroup();
                case 1:
                    return item.getTag();
                case 2:
                    return item.getValue();
                default:
                    return "";
            }
        }
    }
}
