package com.beiming.auth.domain;

import java.time.Instant;

public class PasswordResetToken {
    public final String id;
    public final String tokenHash;
    public final String userId;
    public final Instant expiresAt;
    public boolean used;

    public PasswordResetToken(String id, String tokenHash, String userId, Instant expiresAt) {
        this.id = id;
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }
}
