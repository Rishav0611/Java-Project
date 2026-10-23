package com.privacyshield.mask;

import com.privacyshield.detect.DetectionEngine;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnmaskerTest {

    private final Unmasker unmasker = new Unmasker();
    private final Map<String, String> map = Map.of(
            "[PHONE_1]", "9876543210",
            "[EMAIL_1]", "a@x.com");

    @Test
    void restoresExactTokens() {
        assertEquals("Call 9876543210 now",
                unmasker.unmask("Call [PHONE_1] now", map));
    }

    @Test
    void restoresTokenWithoutBrackets() {
        assertEquals("Call 9876543210 now",
                unmasker.unmask("Call PHONE_1 now", map));
    }

    @Test
    void restoresLowercaseAndSpaceOrHyphen() {
        assertEquals("9876543210 and 9876543210",
                unmasker.unmask("[phone 1] and phone-1", map));
    }

    @Test
    void restoresMultipleDifferentTokens() {
        assertEquals("a@x.com / 9876543210",
                unmasker.unmask("[EMAIL_1] / [PHONE_1]", map));
    }

    @Test
    void unknownTokenIsLeftAsIs() {
        assertEquals("Send to [EMAIL_9]",
                unmasker.unmask("Send to [EMAIL_9]", map));
    }

    @Test
    void ordinaryWordsAreUntouched() {
        assertEquals("See page 2 and item 5",
                unmasker.unmask("See page 2 and item 5", map));
    }

    @Test
    void roundTripWithMasker() {
        String original = "Mail a@x.com ph 9876543210";
        Masker masker = new Masker();
        MaskResult r = masker.mask(original,
                DetectionEngine.withDefaults().detect(original));

        assertEquals(original, unmasker.unmask(r.maskedText(), r.tokenToValue()));
    }
}