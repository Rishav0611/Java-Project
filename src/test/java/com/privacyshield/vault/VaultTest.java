package com.privacyshield.vault;

import com.privacyshield.detect.DetectionEngine;
import com.privacyshield.mask.MaskResult;
import com.privacyshield.mask.Masker;
import com.privacyshield.mask.Unmasker;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultTest {

    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final Vault vault = new Vault(
            new CryptoService("pass123".toCharArray(), CryptoService.newSalt()), clock);
    private final Map<String, String> mapping = Map.of("[PHONE_1]", "9876543210");

    @Test
    void storesAndLoadsMapping() {
        vault.put("s1", mapping, RetentionPolicy.session());

        assertEquals(mapping, vault.get("s1").orElseThrow());
    }

    @Test
    void unknownSessionIsEmpty() {
        assertTrue(vault.get("nope").isEmpty());
    }

    @Test
    void deleteRemovesMapping() {
        vault.put("s1", mapping, RetentionPolicy.session());

        assertTrue(vault.delete("s1"));
        assertTrue(vault.get("s1").isEmpty());
    }

    @Test
    void ttlEntryExpires() {
        vault.put("s1", mapping, RetentionPolicy.ttl(Duration.ofHours(24)));

        clock.advance(Duration.ofHours(23));
        assertTrue(vault.get("s1").isPresent());

        clock.advance(Duration.ofHours(2));
        assertTrue(vault.get("s1").isEmpty());
    }

    @Test
    void permanentEntryDoesNotExpire() {
        vault.put("s1", mapping, RetentionPolicy.permanent());

        clock.advance(Duration.ofDays(365));
        assertTrue(vault.get("s1").isPresent());
    }

    @Test
    void storedBlobDoesNotContainPlaintext() {
        vault.put("s1", mapping, RetentionPolicy.permanent());

        String raw = new String(vault.rawBlob("s1"), StandardCharsets.ISO_8859_1);
        assertFalse(raw.contains("9876543210"));
        assertFalse(raw.contains("PHONE_1"));
    }

    @Test
    void purgeExpiredRemovesOnlyExpired() {
        vault.put("short", mapping, RetentionPolicy.ttl(Duration.ofHours(1)));
        vault.put("long", mapping, RetentionPolicy.permanent());

        clock.advance(Duration.ofHours(2));

        assertEquals(1, vault.purgeExpired());
        assertEquals(1, vault.size());
    }

    @Test
    void fullFlowMaskVaultUnmask() {
        String original = "Mail a@x.com ph 9876543210";
        MaskResult r = new Masker().mask(original,
                DetectionEngine.withDefaults().detect(original));

        vault.put("s1", r.tokenToValue(), RetentionPolicy.session());
        String aiReply = "Contacting " + r.tokenToValue().keySet().iterator().next();

        Map<String, String> restored = vault.get("s1").orElseThrow();
        String finalText = new Unmasker().unmask(r.maskedText(), restored);

        assertEquals(original, finalText);
    }
}