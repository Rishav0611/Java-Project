package com.privacyshield;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the in-memory session vault that the GUI uses.
 *
 * All values in this file are made-up test values.
 */
class SessionVaultTest {
    private static final String TEXT =
            "Email person@example.com and phone +91 98765 43210. PAN ABCPD1234E.";

    @Test
    void defaultPolicyIsDelete() {
        assertEquals(KeepPolicy.DELETE, new SessionVault().getPolicy());
    }

    @Test
    void fakeModeMasksAndRestoresReply() {
        SessionVault vault = new SessionVault();

        MaskResult result = vault.mask(TEXT, MaskMode.FAKE);

        assertFalse(result.getMaskedText().contains("person@example.com"));
        assertFalse(result.getMaskedText().contains("98765 43210"));
        assertFalse(result.getMaskedText().contains("ABCPD1234E"));
        assertEquals(3, result.getDetections().size());
        assertTrue(vault.hasValues());

        String fakeEmail = fakeFor(result, "person@example.com");
        String reply = "I will contact " + fakeEmail + " about the PAN.";
        String restored = vault.restore(reply);

        assertTrue(restored.contains("person@example.com"));
        assertFalse(restored.contains(fakeEmail));
    }

    @Test
    void redactModeCannotRestore() {
        SessionVault vault = new SessionVault();

        MaskResult result = vault.mask(TEXT, MaskMode.REDACT);

        assertTrue(result.getMaskedText().contains("[REDACTED]"));
        assertFalse(result.getMaskedText().contains("person@example.com"));
        assertEquals(3, result.getDetections().size());
        assertTrue(result.getFakeToReal().isEmpty());
        assertFalse(vault.hasValues());

        assertEquals("A reply with no fakes.", vault.restore("A reply with no fakes."));
    }

    @Test
    void keepMarksPolicyAndKeepsValues() {
        SessionVault vault = new SessionVault();
        MaskResult result = vault.mask(TEXT, MaskMode.FAKE);

        vault.keep();

        assertEquals(KeepPolicy.KEEP, vault.getPolicy());
        assertTrue(vault.hasValues());
        assertEquals(result.getFakeToReal(), vault.getSavedValues());
    }

    @Test
    void deleteForgetsValues() {
        SessionVault vault = new SessionVault();
        MaskResult result = vault.mask(TEXT, MaskMode.FAKE);
        String fakeEmail = fakeFor(result, "person@example.com");

        vault.delete();

        assertEquals(KeepPolicy.DELETE, vault.getPolicy());
        assertFalse(vault.hasValues());
        assertTrue(vault.getSavedValues().isEmpty());
        assertEquals("Send it to " + fakeEmail, vault.restore("Send it to " + fakeEmail));
    }

    @Test
    void maskingAgainResetsPolicyAndPreviousValues() {
        SessionVault vault = new SessionVault();
        vault.mask(TEXT, MaskMode.FAKE);
        vault.keep();

        vault.mask("Just a clean sentence.", MaskMode.FAKE);

        assertEquals(KeepPolicy.DELETE, vault.getPolicy());
        assertFalse(vault.hasValues());
    }

    @Test
    void cleanTextHasNoValues() {
        SessionVault vault = new SessionVault();

        MaskResult result = vault.mask("A normal sentence with no personal data.", MaskMode.FAKE);

        assertEquals("A normal sentence with no personal data.", result.getMaskedText());
        assertTrue(result.getDetections().isEmpty());
        assertTrue(result.getFakeToReal().isEmpty());
        assertFalse(vault.hasValues());
    }

    @Test
    void restoreWithoutMaskReturnsText() {
        assertEquals("Hello", new SessionVault().restore("Hello"));
    }

    @Test
    void nullAndEmptyTextIsSafe() {
        SessionVault vault = new SessionVault();

        MaskResult nullResult = vault.mask(null, MaskMode.FAKE);
        assertNull(nullResult.getMaskedText());
        assertTrue(nullResult.getDetections().isEmpty());
        assertFalse(vault.hasValues());

        MaskResult emptyResult = vault.mask("", MaskMode.REDACT);
        assertEquals("", emptyResult.getMaskedText());
        assertFalse(vault.hasValues());
    }

    @Test
    void savedValuesCannotBeChangedByCallers() {
        SessionVault vault = new SessionVault();
        vault.mask(TEXT, MaskMode.FAKE);

        Map<String, String> saved = vault.getSavedValues();

        assertThrows(UnsupportedOperationException.class, () -> saved.put("fake", "real"));
    }

    @Test
    void keepWithoutMaskDoesNotClaimToKeepValues() {
        SessionVault vault = new SessionVault();

        vault.keep();

        assertEquals(KeepPolicy.DELETE, vault.getPolicy());
        assertFalse(vault.hasValues());
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SessionVault(null));
        assertThrows(IllegalArgumentException.class,
                () -> new SessionVault().mask(TEXT, null));
    }

    private static String fakeFor(MaskResult result, String real) {
        for (Map.Entry<String, String> entry : result.getFakeToReal().entrySet()) {
            if (real.equals(entry.getValue())) {
                return entry.getKey();
            }
        }

        throw new AssertionError("No fake value for " + real);
    }
}
