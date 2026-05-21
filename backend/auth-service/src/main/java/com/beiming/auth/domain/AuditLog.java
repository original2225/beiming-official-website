package com.beiming.auth.domain;

import java.time.Instant;
import java.util.List;

public record AuditLog(
        String id,
        String requestId,
        String actorUserId,
        Role actorRole,
        List<Permission> actorPermissions,
        String sourceIp,
        String targetType,
        String targetId,
        String action,
        String riskLevel,
        String reason,
        Object beforeState,
        Object afterState,
        String result,
        String failureReason,
        Instant createdAt) {
}
