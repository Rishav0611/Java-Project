package com.privacyshield;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Finds QR codes in a photo with the ZXing library.
 *
 * Several codes can be found in one picture. Each result has a box in image
 * coordinates, padded a little so a black redaction box covers the whole
 * code, including its quiet zone.
 */
public class QrCodeService {
    private static final float BOUNDS_PADDING_RATIO = 0.20f;
    private static final int MIN_BOUNDS_PADDING = 8;

    /**
     * Returns every readable QR code in the photo, or an empty list.
     */
    public List<QrCodeResult> scan(File photo) throws IOException {
        requireExistingFile(photo);

        BufferedImage image = ImageIO.read(photo);

        if (image == null) {
            throw new IOException("Could not read the image: " + photo.getName());
        }

        return scan(image);
    }

    /**
     * Scans an image that is already loaded, which the GUI uses for previews.
     */
    public List<QrCodeResult> scan(BufferedImage image) throws IOException {
        if (image == null) {
            throw new IllegalArgumentException("image cannot be null");
        }

        Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS,
                Collections.singletonList(BarcodeFormat.QR_CODE));
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.ALSO_INVERTED, Boolean.TRUE);

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(
                new BufferedImageLuminanceSource(image)));

        Result[] decoded;

        try {
            decoded = new QRCodeMultiReader().decodeMultiple(bitmap, hints);
        } catch (NotFoundException exception) {
            return Collections.emptyList();
        }

        List<QrCodeResult> found = new ArrayList<QrCodeResult>();

        if (decoded != null) {
            for (Result result : decoded) {
                if (result == null) {
                    continue;
                }

                found.add(new QrCodeResult(
                        result.getText(),
                        boundsOf(result, image.getWidth(), image.getHeight())));
            }
        }

        Collections.sort(found, new Comparator<QrCodeResult>() {
            @Override
            public int compare(QrCodeResult left, QrCodeResult right) {
                if (left.getBounds().y != right.getBounds().y) {
                    return Integer.compare(left.getBounds().y, right.getBounds().y);
                }
                return Integer.compare(left.getBounds().x, right.getBounds().x);
            }
        });

        return Collections.unmodifiableList(found);
    }

    private static Rectangle boundsOf(Result result, int imageWidth, int imageHeight) {
        ResultPoint[] points = result.getResultPoints();
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        boolean foundPoint = false;

        if (points != null) {
            for (ResultPoint point : points) {
                if (point == null) {
                    continue;
                }

                minX = Math.min(minX, point.getX());
                minY = Math.min(minY, point.getY());
                maxX = Math.max(maxX, point.getX());
                maxY = Math.max(maxY, point.getY());
                foundPoint = true;
            }
        }

        if (!foundPoint) {
            // A decoded code with no corner points is rare. Covering the whole
            // photo is the safe choice because the code cannot be located.
            return new Rectangle(0, 0, imageWidth, imageHeight);
        }

        float width = maxX - minX;
        float height = maxY - minY;
        float padding = Math.max(MIN_BOUNDS_PADDING,
                Math.max(width, height) * BOUNDS_PADDING_RATIO);

        int left = Math.max(0, (int) Math.floor(minX - padding));
        int top = Math.max(0, (int) Math.floor(minY - padding));
        int right = Math.min(imageWidth, (int) Math.ceil(maxX + padding));
        int bottom = Math.min(imageHeight, (int) Math.ceil(maxY + padding));

        if (right <= left || bottom <= top) {
            return new Rectangle(0, 0, imageWidth, imageHeight);
        }

        return new Rectangle(left, top, right - left, bottom - top);
    }

    private static void requireExistingFile(File photo) throws IOException {
        if (photo == null) {
            throw new IllegalArgumentException("photo cannot be null");
        }

        if (!photo.isFile()) {
            throw new IOException("Photo not found: " + photo);
        }
    }
}
