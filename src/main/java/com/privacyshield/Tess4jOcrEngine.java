package com.privacyshield;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reads words from a photo with the Tess4J library.
 *
 * The Tess4J jar includes the Tesseract 5 native library for Windows plus
 * English language data. On first use, the English data file is copied from
 * the jar into a temporary folder and Tesseract is pointed at it, so OCR works
 * on Windows without installing anything else.
 *
 * If a different language is needed, install Tesseract 5 and set
 * -Dprivacyshield.ocr.language=... plus
 * -Dprivacyshield.ocr.datapath=...\tessdata. A clear IOException is returned
 * when OCR cannot start, so the window shows a message instead of crashing.
 *
 * Tests use an OcrEngine fake, so they never need the native library.
 */
public class Tess4jOcrEngine implements OcrEngine {
    private static final String OCR_HELP_MESSAGE =
            "OCR needs Tesseract 5. PrivacyShield includes English data, but "
                    + "Tesseract could not start. Install Tesseract 5 or point "
                    + "-Dprivacyshield.ocr.datapath at a tessdata folder, then restart.";

    private static final String TEMP_DATA_FOLDER = "privacyshield-tessdata";

    private final String language;
    private final String dataPath;

    // Tesseract is slow to start and its native memory is freed when the
    // program ends, so one configured instance is reused for every scan.
    private Tesseract tesseract;

    public Tess4jOcrEngine() {
        this(System.getProperty("privacyshield.ocr.language", "eng"),
                System.getProperty("privacyshield.ocr.datapath", ""));
    }

    public Tess4jOcrEngine(String language, String dataPath) {
        this.language = language == null || language.trim().isEmpty()
                ? "eng"
                : language.trim();
        this.dataPath = dataPath == null ? "" : dataPath.trim();
    }

    @Override
    public synchronized List<OcrWord> readWords(BufferedImage image) throws IOException {
        if (image == null) {
            throw new IllegalArgumentException("image cannot be null");
        }

        try {
            List<Word> words = engine().getWords(
                    image, ITessAPI.TessPageIteratorLevel.RIL_WORD);

            if (words == null || words.isEmpty()) {
                return Collections.emptyList();
            }

            List<OcrWord> result = new ArrayList<OcrWord>();

            for (Word word : words) {
                if (word == null || word.getText() == null
                        || word.getText().trim().isEmpty()) {
                    continue;
                }

                Rectangle box = word.getBoundingBox();

                if (box == null || box.width <= 0 || box.height <= 0) {
                    continue;
                }

                result.add(new OcrWord(word.getText(), box, word.getConfidence()));
            }

            return Collections.unmodifiableList(result);
        } catch (VirtualMachineError error) {
            // Out of memory or a broken JVM: not something OCR can report.
            throw error;
        } catch (LinkageError error) {
            throw new IOException(OCR_HELP_MESSAGE, error);
        } catch (Exception exception) {
            String detail = exception.getMessage();
            throw new IOException("Could not read text with Tesseract"
                    + (detail == null ? "." : ": " + detail), exception);
        } catch (Error error) {
            // The native library can throw a plain Error when its data files
            // are missing. Show the friendly message instead of crashing.
            throw new IOException(OCR_HELP_MESSAGE, error);
        }
    }

    /**
     * Creates the Tesseract object once and points it at English data that
     * ships inside the jar, unless the user gave a data path.
     */
    private Tesseract engine() {
        if (tesseract == null) {
            Tesseract created = new Tesseract();

            if (!dataPath.isEmpty()) {
                created.setDatapath(dataPath);
            } else {
                File bundled = ensureBundledData(language);

                if (bundled != null) {
                    created.setDatapath(bundled.getAbsolutePath());
                }
            }

            created.setLanguage(language);
            tesseract = created;
        }

        return tesseract;
    }

    /**
     * Copies the language data that ships inside the Tess4J jar into a temp
     * folder, because Tesseract reads data files from a normal folder.
     * Returns null when this language is not bundled.
     */
    private static synchronized File ensureBundledData(String language) {
        String resource = "tessdata/" + language + ".traineddata";
        InputStream input = Tess4jOcrEngine.class.getClassLoader()
                .getResourceAsStream(resource);

        if (input == null) {
            return null;
        }

        File folder = new File(System.getProperty("java.io.tmpdir"), TEMP_DATA_FOLDER);
        File target = new File(folder, language + ".traineddata");

        try {
            if (!folder.isDirectory() && !folder.mkdirs()) {
                return null;
            }

            if (target.isFile() && target.length() > 0) {
                return folder;
            }

            OutputStream output = new FileOutputStream(target);

            try {
                byte[] buffer = new byte[8192];
                int read;

                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            } finally {
                output.close();
            }

            return folder;
        } catch (IOException exception) {
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ignored) {
                // Nothing useful to do while closing a resource from the jar.
            }
        }
    }
}
