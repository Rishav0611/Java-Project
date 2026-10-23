package com.privacyshield.mask;

import com.privacyshield.detect.DetectionEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaskerTest {

    private final DetectionEngine engine = DetectionEngine.withDefaults();
    private final Masker masker = new Masker();

    private MaskResult run(String text) {
        return masker.mask(text, engine.detect(text));
    }

    @Test
    void masksSinglePhone() {
        MaskResult r = run("Call 9876543210 now");

        assertEquals("Call [PHONE_1] now", r.maskedText());
        assertEquals("9876543210", r.tokenToValue().get("[PHONE_1]"));
    }

    @Test
    void sameValueGetsSameToken() {
        MaskResult r = run("9876543210 and again 9876543210");

        assertEquals("[PHONE_1] and again [PHONE_1]", r.maskedText());
        assertEquals(1, r.tokenToValue().size());
    }

    @Test
    void differentValuesGetIncrementingTokens() {
        MaskResult r = run("9876543210 or 8123456789");

        assertEquals("[PHONE_1] or [PHONE_2]", r.maskedText());
    }

    @Test
    void countersAreSeparatePerType() {
        MaskResult r = run("Mail a@x.com ph 9876543210");

        assertEquals("Mail [EMAIL_1] ph [PHONE_1]", r.maskedText());
    }

    @Test
    void textWithoutPiiIsUnchanged() {
        MaskResult r = run("Hello, kaise ho?");

        assertEquals("Hello, kaise ho?", r.maskedText());
        assertTrue(r.tokenToValue().isEmpty());
    }

    @Test
    void tokensStayConsistentAcrossCalls() {
        run("Call 9876543210");
        MaskResult second = run("Again 9876543210 and 8123456789");

        assertEquals("Again [PHONE_1] and [PHONE_2]", second.maskedText());
    }
}