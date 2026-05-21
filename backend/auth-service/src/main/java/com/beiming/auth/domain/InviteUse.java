package com.beiming.auth.domain;

import java.time.Instant;

public record InviteUse(
        String id,
        String inviteId,
        String userId,
        String username,
        String sourceIp,
        Instant usedAt) {
}
