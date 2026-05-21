package com.beiming.auth.service;

import com.beiming.auth.domain.InviteStatus;
import com.beiming.auth.domain.InviteType;
import com.beiming.auth.domain.Permission;
import com.beiming.auth.domain.Role;
import com.beiming.auth.domain.UserStatus;
import java.time.Instant;
import java.util.List;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record AuthResult(String accessToken, Instant expiresAt, UserSummary user) {
    }

    public record UserSummary(
            String id,
            String username,
            String displayName,
            Role role,
            List<Permission> permissions,
            UserStatus status,
            String minecraftId,
            String minecraftUuid,
            String avatarUrl) {
    }

    public record InviteSummary(
            String id,
            String codePrefix,
            InviteType type,
            Role role,
            List<Permission> permissions,
            int maxUses,
            int usedCount,
            InviteStatus status,
            Instant expiresAt,
            String note) {
    }

    public record InviteCreation(
            String id,
            String code,
            InviteType type,
            Role role,
            List<Permission> permissions,
            int maxUses,
            int usedCount,
            InviteStatus status,
            Instant expiresAt) {
    }

    public record InviteUseSummary(String id, String inviteId, String userId, String username, String sourceIp, Instant usedAt) {
    }
}
