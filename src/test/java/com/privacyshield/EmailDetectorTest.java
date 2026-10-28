package com.privacyshield;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link EmailDetector}.
 *
 * All addresses in this file use the reserved example domains.
 */
class EmailDetectorTest {

    private final EmailDetector detector = new EmailDetector();

    @Test
    void findsSimpleEmail() {
        List<Detection> found = detector.detect("mail a@b.com now");

        assertEquals(1, found.size());
        assertEquals(DataType.EMAIL, found.get(0).getType());
        assertEquals("a@b.com", found.get(0).getValue());
        assertEquals(5, found.get(0).getStart());
        assertEquals(12, found.get(0).getEnd());
    }

    @Test
    void findsEmailAtEndOfSentence() {
        List<Detection> found = detector.detect("Mail me at a@b.com.");

        assertEquals(1, found.size());
        assertEquals("a@b.com", found.get(0).getValue());
        assertEquals(11, found.get(0).getStart());
        assertEquals(18, found.get(0).getEnd());
    }

    @Test
    void doesNotIncludeTrailingPunctuationInValue() {
        String[] texts = {
                "a@b.com,",
                "a@b.com!",
                "a@b.com?",
                "a@b.com)",
                "a@b.com."
        };

        for (String text : texts) {
            List<Detection> found = detector.detect(text);

            assertEquals(1, found.size(), "expected to detect an email in " + text);
            assertEquals("a@b.com", found.get(0).getValue(), "unexpected value for " + text);
        }
    }

    @Test
    void findsEmailInsideParentheses() {
        List<Detection> found = detector.detect("(a@b.com)");

        assertEquals(1, found.size());
        assertEquals(1, found.get(0).getStart());
        assertEquals(8, found.get(0).getEnd());
    }

    @Test
    void acceptsUppercaseSubdomainsAndTags() {
        String text = "User.Name+tag@Sub.Example.CO.UK";
        List<Detection> found = detector.detect(text);

        assertEquals(1, found.size());
        assertEquals(text, found.get(0).getValue());
    }

    @Test
    void findsTwoEmailsInOneText() {
        List<Detection> found = detector.detect("first.last@example.org, then a@b.co.in");

        assertEquals(2, found.size());
        assertEquals("first.last@example.org", found.get(0).getValue());
        assertEquals("a@b.co.in", found.get(1).getValue());
    }

    @Test
    void rejectsMissingAtSignOrDomain() {
        assertTrue(detector.detect("a.b.com").isEmpty());
        assertTrue(detector.detect("a@@b.com").isEmpty());
    }

    @Test
    void rejectsDomainWithoutValidEnding() {
        assertTrue(detector.detect("a@b").isEmpty());
        assertTrue(detector.detect("a@b.c").isEmpty());
        assertTrue(detector.detect("a@b_c.com").isEmpty());
    }

    @Test
    void handlesNullAndEmptyText() {
        assertTrue(detector.detect(null).isEmpty());
        assertTrue(detector.detect("").isEmpty());
    }
}
