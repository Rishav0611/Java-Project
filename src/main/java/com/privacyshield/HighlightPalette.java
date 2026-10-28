package com.privacyshield;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

/**
 * Background colours used to highlight each kind of personal data.
 *
 * Aadhaar is red, PAN is green, phone numbers are yellow and email addresses
 * are blue. The foreground is dark so the text stays readable on every colour.
 */
public final class HighlightPalette {
    private static final Color FOREGROUND = new Color(0x20, 0x20, 0x20);
    private static final Color UNKNOWN = new Color(0xE0, 0xE0, 0xE0);

    private static final Map<DataType, Color> BACKGROUNDS = new EnumMap<DataType, Color>(DataType.class);

    static {
        BACKGROUNDS.put(DataType.AADHAAR, new Color(0xFF, 0xC7, 0xC2));
        BACKGROUNDS.put(DataType.PAN, new Color(0xC8, 0xF0, 0xC8));
        BACKGROUNDS.put(DataType.PHONE, new Color(0xFF, 0xF3, 0xB0));
        BACKGROUNDS.put(DataType.EMAIL, new Color(0xBF, 0xD7, 0xFF));
    }

    private HighlightPalette() {
    }

    public static Color background(DataType type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

        Color color = BACKGROUNDS.get(type);
        return color == null ? UNKNOWN : color;
    }

    public static Color foreground(DataType type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

        return FOREGROUND;
    }

    public static String label(DataType type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

        switch (type) {
            case AADHAAR:
                return "Aadhaar";
            case PAN:
                return "PAN";
            case PHONE:
                return "Phone";
            case EMAIL:
                return "Email";
            default:
                return type.name();
        }
    }
}
