package com.privacyshield;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the clipboard shield.
 *
 * A small fake clipboard stands in for the real one, so these tests do not
 * touch the system clipboard. All values are made-up test values.
 */
class ClipboardShieldTest {
    private static final String TEXT =
            "Email person@example.com and phone +91 98765 43210. PAN ABCPD1234E.";

    @Test
    void inspectFindsValuesWithoutChangingTheClipboard() {
        FakeClipboard clipboard = new FakeClipboard(TEXT);
        ClipboardShield shield = new ClipboardShield(new PrivacyShield(), clipboard);

        ClipboardCheck check = shield.inspect();

        assertEquals(TEXT, check.getText());
        assertEquals(3, check.getValueCount());
        assertTrue(check.hasSensitiveData());
        assertFalse(check.isCleaned());
        assertEquals(TEXT, clipboard.text);
        assertTrue(clipboard.writes.isEmpty());
    }

    @Test
    void cleanReplacesValuesAndWritesTheRedactedText() {
        FakeClipboard clipboard = new FakeClipboard(TEXT);
        ClipboardShield shield = new ClipboardShield(new PrivacyShield(), clipboard);

        ClipboardCheck result = shield.clean();

        assertTrue(result.isCleaned());
        assertTrue(result.hasSensitiveData());
        assertFalse(result.getText().contains("person@example.com"));
        assertFalse(result.getText().contains("98765 43210"));
        assertFalse(result.getText().contains("ABCPD1234E"));
        assertTrue(result.getText().contains("[REDACTED]"));
        assertEquals(3, result.getValueCount());
        assertEquals(result.getText(), clipboard.text);
        assertEquals(1, clipboard.writes.size());
    }

    @Test
    void cleanedTextIsCleanWhenCheckedAgain() {
        FakeClipboard clipboard = new FakeClipboard(TEXT);
        ClipboardShield shield = new ClipboardShield(new PrivacyShield(), clipboard);

        shield.clean();
        ClipboardCheck again = shield.inspect();

        assertFalse(again.hasSensitiveData());
        assertEquals(0, again.getValueCount());
        assertEquals(1, clipboard.writes.size());
    }

    @Test
    void cleanDoesNothingWhenThereIsNoSensitiveData() {
        FakeClipboard clipboard = new FakeClipboard("A normal sentence.");
        ClipboardShield shield = new ClipboardShield(new PrivacyShield(), clipboard);

        ClipboardCheck result = shield.clean();

        assertFalse(result.isCleaned());
        assertFalse(result.hasSensitiveData());
        assertEquals("A normal sentence.", result.getText());
        assertTrue(clipboard.writes.isEmpty());
    }

    @Test
    void emptyClipboardIsReportedAsEmpty() {
        FakeClipboard clipboard = new FakeClipboard("");
        ClipboardShield shield = new ClipboardShield(new PrivacyShield(), clipboard);

        ClipboardCheck check = shield.inspect();
        ClipboardCheck result = shield.clean();

        assertEquals("", check.getText());
        assertFalse(check.hasSensitiveData());
        assertFalse(result.isCleaned());
        assertTrue(clipboard.writes.isEmpty());
    }

    @Test
    void previewTextCannotBeChangedByCallers() {
        ClipboardCheck check = new ClipboardShield(new PrivacyShield(),
                new FakeClipboard(TEXT)).inspect();

        assertThrows(UnsupportedOperationException.class,
                () -> check.getDetections().add(null));
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ClipboardShield(null, new FakeClipboard(TEXT)));
        assertThrows(IllegalArgumentException.class,
                () -> new ClipboardShield(new PrivacyShield(), null));
        assertEquals("", new ClipboardCheck(null, new ArrayList<Detection>(), false).getText());
        assertThrows(IllegalArgumentException.class,
                () -> new ClipboardCheck("text", null, false));
    }

    /**
     * A clipboard that keeps its text in memory.
     */
    private static final class FakeClipboard implements ClipboardText {
        private String text;
        private final List<String> writes = new ArrayList<String>();

        private FakeClipboard(String text) {
            this.text = text;
        }

        @Override
        public String read() {
            return text;
        }

        @Override
        public void write(String text) {
            this.text = text;
            writes.add(text);
        }
    }
}
