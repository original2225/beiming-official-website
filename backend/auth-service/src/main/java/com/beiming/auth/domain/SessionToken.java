package com.beiming.auth.domain;

import java.time.Instant;

public class SessionToken {
    public final String tokenHash;
    public final String userId;
    public final Instant expiresAt;
    public boolean active = true;

    public SessionToken(String tokenHash, String userId, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }
}
