package com.privacyshield.vault;

import java.time.Duration;

public record RetentionPolicy(Mode mode, Duration ttl) {

    public enum Mode { SESSION, TTL, PERMANENT }

    public static RetentionPolicy session() {
        return new RetentionPolicy(Mode.SESSION, null);
    }

    public static RetentionPolicy ttl(Duration duration) {
        return new RetentionPolicy(Mode.TTL, duration);
    }

    public static RetentionPolicy permanent() {
        return new RetentionPolicy(Mode.PERMANENT, null);
    }
}