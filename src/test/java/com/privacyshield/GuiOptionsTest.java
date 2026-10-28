package com.privacyshield;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Tests for the small option enums used by the GUI.
 */
class GuiOptionsTest {

    @Test
    void fakeValuesAreTheDefaultMaskMode() {
        assertEquals(MaskMode.FAKE, MaskMode.getDefault());
        assertNotEquals(MaskMode.REDACT, MaskMode.getDefault());
    }

    @Test
    void deleteIsTheDefaultKeepPolicy() {
        assertEquals(KeepPolicy.DELETE, KeepPolicy.getDefault());
        assertNotEquals(KeepPolicy.KEEP, KeepPolicy.getDefault());
    }

    @Test
    void maskModeLabelsAreReadable() {
        assertEquals("Fake values", MaskMode.FAKE.toString());
        assertEquals("[REDACTED]", MaskMode.REDACT.toString());
        assertEquals("Fake values", MaskMode.FAKE.getLabel());
    }

    @Test
    void keepPolicyLabelsAreReadable() {
        assertEquals("Keep", KeepPolicy.KEEP.toString());
        assertEquals("Delete", KeepPolicy.DELETE.toString());
        assertEquals("Delete", KeepPolicy.DELETE.getLabel());
    }
}
