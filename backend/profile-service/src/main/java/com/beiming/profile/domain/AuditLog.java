package com.beiming.profile.domain;

import java.time.Instant;

public record AuditLog(
        String id,
        String actorUserId,
        Role actorRole,
        String targetType,
        String targetId,
        String action,
        String riskLevel,
        String reason,
        Object beforeState,
        Object afterState,
        String result,
        Instant createdAt) {
}
