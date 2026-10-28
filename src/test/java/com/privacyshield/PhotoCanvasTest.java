package com.privacyshield;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the drag-to-draw behaviour of the photo preview.
 *
 * The panel is never shown, so these tests also run on a build machine
 * without a display.
 */
class PhotoCanvasTest {

    @Test
    void dragCreatesABoxInImageCoordinates() {
        PhotoCanvas canvas = canvasWithImage(100, 100);
        final int[] changes = {0};

        canvas.setBoxChangeListener(new Runnable() {
            @Override
            public void run() {
                changes[0]++;
            }
        });

        drag(canvas, 20, 20, 120, 120);

        assertEquals(1, canvas.getBoxes().size());
        assertEquals(new Rectangle(10, 10, 50, 50), canvas.getBoxes().get(0));
        assertTrue(canvas.hasBoxes());
        assertEquals(1, changes[0]);
    }

    @Test
    void tinyDragIsIgnored() {
        PhotoCanvas canvas = canvasWithImage(100, 100);

        drag(canvas, 30, 30, 30, 30);

        assertTrue(canvas.getBoxes().isEmpty());
        assertFalse(canvas.hasBoxes());
    }

    @Test
    void undoAndClearRemoveBoxes() {
        PhotoCanvas canvas = canvasWithImage(100, 100);
        final int[] changes = {0};

        canvas.setBoxChangeListener(new Runnable() {
            @Override
            public void run() {
                changes[0]++;
            }
        });

        drag(canvas, 10, 10, 40, 40);
        drag(canvas, 60, 60, 90, 90);
        assertEquals(2, canvas.getBoxes().size());

        canvas.removeLastBox();
        assertEquals(1, canvas.getBoxes().size());

        canvas.clearBoxes();
        assertTrue(canvas.getBoxes().isEmpty());
        assertEquals(4, changes[0]);
    }

    @Test
    void aNewImageClearsOldBoxes() {
        PhotoCanvas canvas = canvasWithImage(100, 100);
        drag(canvas, 10, 10, 40, 40);
        assertEquals(1, canvas.getBoxes().size());

        canvas.setImage(new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB));

        assertTrue(canvas.getBoxes().isEmpty());
        assertFalse(canvas.hasBoxes());
    }

    @Test
    void automaticBoxesAreClippedToTheImage() {
        PhotoCanvas canvas = canvasWithImage(100, 100);
        final int[] changes = {0};

        canvas.setBoxChangeListener(new Runnable() {
            @Override
            public void run() {
                changes[0]++;
            }
        });

        Rectangle[] boxes = {
                new Rectangle(10, 10, 20, 20),
                new Rectangle(-15, -10, 25, 20),
                new Rectangle(95, 95, 40, 40),
                new Rectangle(0, 0, 1, 1),
                null
        };

        for (Rectangle box : boxes) {
            canvas.addBox(box);
        }

        assertEquals(3, canvas.getBoxes().size());
        assertEquals(new Rectangle(10, 10, 20, 20), canvas.getBoxes().get(0));
        assertEquals(new Rectangle(0, 0, 10, 10), canvas.getBoxes().get(1));
        assertEquals(new Rectangle(95, 95, 5, 5), canvas.getBoxes().get(2));
        assertEquals(3, changes[0]);
    }

    @Test
    void boxesNeedAnImageFirst() {
        PhotoCanvas canvas = new PhotoCanvas();
        canvas.addBox(new Rectangle(0, 0, 10, 10));

        assertTrue(canvas.getBoxes().isEmpty());
    }

    @Test
    void addBoxesIgnoresNullOrEmptyLists() {
        PhotoCanvas canvas = canvasWithImage(100, 100);
        canvas.addBoxes(null);
        canvas.addBoxes(java.util.Collections.<Rectangle>emptyList());

        assertTrue(canvas.getBoxes().isEmpty());
    }

    private static PhotoCanvas canvasWithImage(int width, int height) {
        PhotoCanvas canvas = new PhotoCanvas();
        canvas.setSize(200, 200);
        canvas.setImage(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB));
        return canvas;
    }

    private static void drag(PhotoCanvas canvas, int startX, int startY, int endX, int endY) {
        long when = System.currentTimeMillis();

        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED,
                when, InputEvent.BUTTON1_DOWN_MASK, startX, startY, 1, false,
                MouseEvent.BUTTON1));
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED,
                when + 10, InputEvent.BUTTON1_DOWN_MASK, endX, endY, 1, false,
                MouseEvent.NOBUTTON));
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED,
                when + 20, InputEvent.BUTTON1_DOWN_MASK, endX, endY, 1, false,
                MouseEvent.BUTTON1));
    }
}
