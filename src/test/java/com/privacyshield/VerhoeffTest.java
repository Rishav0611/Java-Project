package com.privacyshield;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the Verhoeff checksum check.
 *
 * All numbers in this file are made-up test values.
 */
class VerhoeffTest {

    @Test
    void acceptsValidNumbers() {
        // Values from the published Verhoeff examples plus a made-up 12-digit number.
        assertTrue(Verhoeff.isValid("2363"));
        assertTrue(Verhoeff.isValid("123451"));
        assertTrue(Verhoeff.isValid("1428570"));
        assertTrue(Verhoeff.isValid("1234567890120"));
        assertTrue(Verhoeff.isValid("234567890124"));
    }

    @Test
    void rejectsWrongCheckDigit() {
        assertFalse(Verhoeff.isValid("2364"));
        assertFalse(Verhoeff.isValid("123452"));
        assertFalse(Verhoeff.isValid("234567890125"));
        assertFalse(Verhoeff.isValid("987654321013"));
    }

    @Test
    void rejectsNullEmptyAndBlankInput() {
        assertFalse(Verhoeff.isValid(null));
        assertFalse(Verhoeff.isValid(""));
        assertFalse(Verhoeff.isValid(" "));
    }

    @Test
    void rejectsNonDigitCharacters() {
        assertFalse(Verhoeff.isValid("236a"));
        assertFalse(Verhoeff.isValid("23 63"));
        assertFalse(Verhoeff.isValid("236-3"));
        assertFalse(Verhoeff.isValid("+2363"));
        assertFalse(Verhoeff.isValid("2363.0"));
    }

    @Test
    void checkDigitCompletesAValidNumber() {
        assertEquals(3, Verhoeff.checkDigit("236"));
        assertEquals(1, Verhoeff.checkDigit("12345"));
        assertEquals(4, Verhoeff.checkDigit("23456789012"));
        assertEquals(0, Verhoeff.checkDigit("123456789012"));
    }

    @Test
    void checkDigitRejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> Verhoeff.checkDigit(null));
        assertThrows(IllegalArgumentException.class, () -> Verhoeff.checkDigit(""));
        assertThrows(IllegalArgumentException.class, () -> Verhoeff.checkDigit("12a"));
        assertThrows(IllegalArgumentException.class, () -> Verhoeff.checkDigit("12 3"));
    }
}
