package com.privacyshield;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shows a photo scaled to fit and lets the user drag black boxes over private
 * areas. Boxes are stored in image coordinates, so the preview can be resized
 * without moving them.
 */
public class PhotoCanvas extends JPanel {
    private static final Color BOX_BORDER = new Color(0xE0, 0x30, 0x30);

    private BufferedImage image;
    private final List<Rectangle> boxes = new ArrayList<Rectangle>();
    private Point dragStart;
    private Point dragEnd;
    private Runnable boxChangeListener;

    public PhotoCanvas() {
        setPreferredSize(new Dimension(520, 380));
        setToolTipText("Drag on the photo to draw a black box.");
        setBorder(javax.swing.BorderFactory.createLineBorder(new Color(0x88, 0x88, 0x88)));

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (image != null && event.getButton() == MouseEvent.BUTTON1) {
                    dragStart = imagePoint(event.getPoint());
                    dragEnd = dragStart;
                    repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (dragStart != null) {
                    dragEnd = imagePoint(event.getPoint());
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (dragStart == null) {
                    return;
                }

                dragEnd = imagePoint(event.getPoint());
                Rectangle box = rectangleBetween(dragStart, dragEnd);

                if (box.width >= 2 && box.height >= 2) {
                    boxes.add(box);
                    notifyBoxesChanged();
                }

                dragStart = null;
                dragEnd = null;
                repaint();
            }
        };

        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        boxes.clear();
        dragStart = null;
        dragEnd = null;
        setCursor(image == null
                ? Cursor.getDefaultCursor()
                : Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        repaint();
    }

    public BufferedImage getImage() {
        return image;
    }

    /**
     * A copy of the boxes, in image coordinates.
     */
    public List<Rectangle> getBoxes() {
        return Collections.unmodifiableList(new ArrayList<Rectangle>(boxes));
    }

    public boolean hasBoxes() {
        return !boxes.isEmpty();
    }

    /**
     * Called after a box is added, removed or cleared. The photo panel uses it
     * to enable or disable the save button.
     */
    public void setBoxChangeListener(Runnable listener) {
        this.boxChangeListener = listener;
    }

    public void clearBoxes() {
        boxes.clear();
        dragStart = null;
        dragEnd = null;
        notifyBoxesChanged();
        repaint();
    }

    public void removeLastBox() {
        if (!boxes.isEmpty()) {
            boxes.remove(boxes.size() - 1);
            notifyBoxesChanged();
            repaint();
        }
    }

    private void notifyBoxesChanged() {
        if (boxChangeListener != null) {
            boxChangeListener.run();
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (image == null) {
                paintPlaceholder(g);
                return;
            }

            View view = viewFor(getWidth(), getHeight(), image);

            g.drawImage(image,
                    (int) Math.round(view.x),
                    (int) Math.round(view.y),
                    (int) Math.round(view.width),
                    (int) Math.round(view.height),
                    null);

            for (Rectangle box : boxes) {
                paintBox(g, box, view, true);
            }

            if (dragStart != null && dragEnd != null) {
                paintBox(g, rectangleBetween(dragStart, dragEnd), view, false);
            }
        } finally {
            g.dispose();
        }
    }

    private void paintPlaceholder(Graphics2D g) {
        String message = "Choose a photo to preview it.";
        FontMetrics metrics = g.getFontMetrics();
        int x = Math.max(8, (getWidth() - metrics.stringWidth(message)) / 2);
        int y = Math.max(metrics.getAscent() + 8,
                (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());

        g.setColor(getForeground());
        g.drawString(message, x, y);
    }

    private void paintBox(Graphics2D g, Rectangle box, View view, boolean filled) {
        Rectangle screen = new Rectangle(
                (int) Math.round(view.x + box.x * view.scale),
                (int) Math.round(view.y + box.y * view.scale),
                (int) Math.round(box.width * view.scale),
                (int) Math.round(box.height * view.scale)
        );

        if (filled) {
            g.setColor(new Color(0, 0, 0, 170));
            g.fillRect(screen.x, screen.y, screen.width, screen.height);
        } else {
            g.setColor(new Color(0, 0, 0, 70));
            g.fillRect(screen.x, screen.y, screen.width, screen.height);
            g.setColor(BOX_BORDER);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{6f, 4f}, 0f));
        }

        g.setColor(BOX_BORDER);
        g.drawRect(screen.x, screen.y, screen.width, screen.height);
    }

    private Point imagePoint(Point screenPoint) {
        if (image == null) {
            return null;
        }

        View view = viewFor(getWidth(), getHeight(), image);

        int x = (int) Math.floor((screenPoint.x - view.x) / view.scale);
        int y = (int) Math.floor((screenPoint.y - view.y) / view.scale);

        x = Math.max(0, Math.min(image.getWidth(), x));
        y = Math.max(0, Math.min(image.getHeight(), y));

        return new Point(x, y);
    }

    private static Rectangle rectangleBetween(Point first, Point second) {
        int x = Math.min(first.x, second.x);
        int y = Math.min(first.y, second.y);
        int width = Math.abs(second.x - first.x);
        int height = Math.abs(second.y - first.y);

        return new Rectangle(x, y, width, height);
    }

    private static View viewFor(int panelWidth, int panelHeight, BufferedImage image) {
        double scale = Math.min(
                (double) panelWidth / image.getWidth(),
                (double) panelHeight / image.getHeight()
        );

        if (scale <= 0) {
            scale = 1;
        }

        double width = image.getWidth() * scale;
        double height = image.getHeight() * scale;

        return new View(scale, (panelWidth - width) / 2.0, (panelHeight - height) / 2.0, width, height);
    }

    private static final class View {
        private final double scale;
        private final double x;
        private final double y;
        private final double width;
        private final double height;

        private View(double scale, double x, double y, double width, double height) {
            this.scale = scale;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
