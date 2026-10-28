package com.privacyshield;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the highlight colours used in the GUI.
 */
class HighlightPaletteTest {

    @Test
    void everyTypeHasItsOwnColourAndLabel() {
        Set<Color> colours = new HashSet<Color>();

        for (DataType type : DataType.values()) {
            Color background = HighlightPalette.background(type);

            assertNotNull(background);
            assertNotNull(HighlightPalette.foreground(type));
            assertFalse(HighlightPalette.label(type).trim().isEmpty());
            colours.add(background);
        }

        assertEquals(DataType.values().length, colours.size());
    }

    @Test
    void coloursFollowThePlannedOrder() {
        assertEquals(new Color(0xFFC7C2), HighlightPalette.background(DataType.AADHAAR));
        assertEquals(new Color(0xC8F0C8), HighlightPalette.background(DataType.PAN));
        assertEquals(new Color(0xFFF3B0), HighlightPalette.background(DataType.PHONE));
        assertEquals(new Color(0xBFD7FF), HighlightPalette.background(DataType.EMAIL));
    }

    @Test
    void foregroundIsDarkSoTextStaysReadable() {
        for (DataType type : DataType.values()) {
            Color foreground = HighlightPalette.foreground(type);

            assertTrue(foreground.getRed() < 100);
            assertTrue(foreground.getGreen() < 100);
            assertTrue(foreground.getBlue() < 100);
        }
    }

    @Test
    void labelsMatchTheDataType() {
        assertEquals("Aadhaar", HighlightPalette.label(DataType.AADHAAR));
        assertEquals("PAN", HighlightPalette.label(DataType.PAN));
        assertEquals("Phone", HighlightPalette.label(DataType.PHONE));
        assertEquals("Email", HighlightPalette.label(DataType.EMAIL));
    }

    @Test
    void nullTypeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> HighlightPalette.background(null));
        assertThrows(IllegalArgumentException.class, () -> HighlightPalette.foreground(null));
        assertThrows(IllegalArgumentException.class, () -> HighlightPalette.label(null));
    }
}
