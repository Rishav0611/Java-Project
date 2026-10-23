package com.privacyshield.vault;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Vault {

    private record Entry(byte[] blob, RetentionPolicy policy, Instant expiresAt) {
    }

    private final CryptoService crypto;
    private final Clock clock;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public Vault(CryptoService crypto) {
        this(crypto, Clock.systemUTC());
    }

    public Vault(CryptoService crypto, Clock clock) {
        this.crypto = crypto;
        this.clock = clock;
    }

    public void put(String sessionId, Map<String, String> mapping, RetentionPolicy policy) {
        byte[] blob = crypto.encrypt(serialize(mapping));
        Instant expiresAt = policy.mode() == RetentionPolicy.Mode.TTL
                ? clock.instant().plus(policy.ttl())
                : null;
        entries.put(sessionId, new Entry(blob, policy, expiresAt));
    }

    public Optional<Map<String, String>> get(String sessionId) {
        Entry e = entries.get(sessionId);
        if (e == null) {
            return Optional.empty();
        }
        if (isExpired(e)) {
            entries.remove(sessionId);
            return Optional.empty();
        }
        try {
            return Optional.of(deserialize(crypto.decrypt(e.blob())));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Vault entry could not be decrypted", ex);
        }
    }

    public boolean delete(String sessionId) {
        return entries.remove(sessionId) != null;
    }

    public int purgeExpired() {
        int before = entries.size();
        entries.entrySet().removeIf(en -> isExpired(en.getValue()));
        return before - entries.size();
    }

    public int size() {
        return entries.size();
    }

    // sirf tests ke liye (package-private)
    byte[] rawBlob(String sessionId) {
        Entry e = entries.get(sessionId);
        return e == null ? null : e.blob();
    }

    private boolean isExpired(Entry e) {
        return e.expiresAt() != null && !clock.instant().isBefore(e.expiresAt());
    }

    // Har line: base64(token):base64(value)  (base64 me ':' ya newline nahi hota)
    private static byte[] serialize(Map<String, String> mapping) {
        Base64.Encoder enc = Base64.getEncoder();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> en : mapping.entrySet()) {
            sb.append(enc.encodeToString(en.getKey().getBytes(StandardCharsets.UTF_8)))
                    .append(':')
                    .append(enc.encodeToString(en.getValue().getBytes(StandardCharsets.UTF_8)))
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static Map<String, String> deserialize(byte[] data) {
        Base64.Decoder dec = Base64.getDecoder();
        Map<String, String> mapping = new LinkedHashMap<>();
        for (String line : new String(data, StandardCharsets.UTF_8).split("\n")) {
            if (line.isEmpty()) {
                continue;
            }
            int i = line.indexOf(':');
            String token = new String(dec.decode(line.substring(0, i)), StandardCharsets.UTF_8);
            String value = new String(dec.decode(line.substring(i + 1)), StandardCharsets.UTF_8);
            mapping.put(token, value);
        }
        return mapping;
    }
}