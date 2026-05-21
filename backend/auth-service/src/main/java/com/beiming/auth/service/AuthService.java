package com.beiming.auth.service;

import com.beiming.auth.api.ApiException;
import com.beiming.auth.api.PageResponse;
import com.beiming.auth.domain.AuditLog;
import com.beiming.auth.domain.Invite;
import com.beiming.auth.domain.InviteStatus;
import com.beiming.auth.domain.InviteType;
import com.beiming.auth.domain.InviteUse;
import com.beiming.auth.domain.PasswordResetToken;
import com.beiming.auth.domain.Permission;
import com.beiming.auth.domain.Role;
import com.beiming.auth.domain.SessionToken;
import com.beiming.auth.domain.UserAccount;
import com.beiming.auth.domain.UserStatus;
import com.beiming.auth.service.AuthDtos.AuthResult;
import com.beiming.auth.service.AuthDtos.InviteCreation;
import com.beiming.auth.service.AuthDtos.InviteSummary;
import com.beiming.auth.service.AuthDtos.InviteUseSummary;
import com.beiming.auth.service.AuthDtos.UserSummary;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AtomicInteger userSequence = new AtomicInteger(1);
    private final AtomicInteger inviteSequence = new AtomicInteger(1);
    private final AtomicInteger inviteUseSequence = new AtomicInteger(1);
    private final AtomicInteger auditSequence = new AtomicInteger(1);
    private final AtomicInteger resetSequence = new AtomicInteger(1);
    private final Map<String, UserAccount> usersById = new LinkedHashMap<>();
    private final Map<String, String> userIdByUsername = new LinkedHashMap<>();
    private final Map<String, String> userIdByEmail = new LinkedHashMap<>();
    private final Map<String, String> userIdByMinecraftId = new LinkedHashMap<>();
    private final Map<String, Invite> invitesById = new LinkedHashMap<>();
    private final Map<String, String> inviteIdByCodeHash = new LinkedHashMap<>();
    private final Map<String, SessionToken> sessionsByHash = new LinkedHashMap<>();
    private final Map<String, PasswordResetToken> resetTokensByHash = new LinkedHashMap<>();
    private final List<AuditLog> auditLogs = new ArrayList<>();

    public AuthService() {
        Instant now = Instant.now();
        UserAccount owner = new UserAccount(nextUserId(), "owner", "Owner", "owner@beiming.local",
                passwordEncoder.encode("OwnerPass123!"), Role.OWNER, List.of(Permission.values()), UserStatus.ACTIVE, now);
        saveUserIndexes(owner);
    }

    public synchronized AuthResult register(String inviteCode, String username, String password, String displayName,
            String email, String sourceIp, String requestId) {
        requireNonBlank(inviteCode, "inviteCode");
        requireNonBlank(username, "username");
        requireNonBlank(password, "password");
        requireNonBlank(displayName, "displayName");
        requireNonBlank(email, "email");
        String normalizedUsername = normalize(username);
        String normalizedEmail = normalize(email);
        if (userIdByUsername.containsKey(normalizedUsername)) {
            throw new ApiException(41104, HttpStatus.CONFLICT, "username already exists");
        }
        if (userIdByEmail.containsKey(normalizedEmail)) {
            throw new ApiException(41105, HttpStatus.CONFLICT, "email already exists");
        }
        Invite invite = inviteByRawCode(inviteCode);
        ensureInviteUsable(invite);
        if (invite.role == Role.OWNER) {
            throw new ApiException(42001, HttpStatus.FORBIDDEN, "role not allowed");
        }

        Instant now = Instant.now();
        UserAccount user = new UserAccount(nextUserId(), normalizedUsername, displayName, normalizedEmail,
                passwordEncoder.encode(password), invite.role, invite.permissions, UserStatus.PENDING_PROFILE, now);
        saveUserIndexes(user);
        invite.usedCount++;
        invite.uses.add(new InviteUse(nextInviteUseId(), invite.id, user.id, user.username, sourceIp, now));
        if (invite.usedCount >= invite.maxUses) {
            invite.status = InviteStatus.EXHAUSTED;
        }
        audit(requestId, user.id, user.role, user.permissions, sourceIp, "USER", user.id, "REGISTER", "LOW", null, null,
                summary(user), "SUCCESS", null);
        return createSession(user, now);
    }

    public synchronized AuthResult login(String username, String password, String sourceIp, String requestId) {
        requireNonBlank(username, "username");
        requireNonBlank(password, "password");
        UserAccount user = userByUsername(username).orElseThrow(() -> invalidCredentials(requestId, sourceIp, username));
        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw invalidCredentials(requestId, sourceIp, username);
        }
        if (user.status == UserStatus.DISABLED) {
            throw new ApiException(41102, HttpStatus.FORBIDDEN, "user disabled");
        }
        if (user.status == UserStatus.BANNED) {
            throw new ApiException(41103, HttpStatus.FORBIDDEN, "user banned");
        }
        if (user.status == UserStatus.DELETED) {
            throw new ApiException(41001, HttpStatus.UNAUTHORIZED, "invalid session");
        }
        user.lastLoginAt = Instant.now();
        audit(requestId, user.id, user.role, user.permissions, sourceIp, "USER", user.id, "LOGIN", "LOW", null, null,
                summary(user), "SUCCESS", null);
        return createSession(user, Instant.now());
    }

    public synchronized void logout(String token, String sourceIp, String requestId) {
        UserAccount user = requireUser(token);
        sessionsByHash.get(hash(token)).active = false;
        audit(requestId, user.id, user.role, user.permissions, sourceIp, "SESSION", user.id, "LOGOUT", "LOW", null, null,
                null, "SUCCESS", null);
    }

    public synchronized UserSummary me(String token) {
        return summary(requireUser(token));
    }

    public synchronized Map<String, Object> verifySession(String token) {
        UserAccount user = requireUser(token);
        return Map.of("valid", true, "user", summary(user));
    }

    public synchronized PageResponse<UserSummary> users(String token, int page, int pageSize, String keyword, Role role,
            UserStatus status) {
        UserAccount actor = requireUser(token);
        requireAdmin(actor);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        String normalizedKeyword = keyword == null ? null : normalize(keyword);
        List<UserSummary> all = usersById.values().stream()
                .filter(user -> user.status != UserStatus.DELETED)
                .filter(user -> role == null || user.role == role)
                .filter(user -> status == null || user.status == status)
                .filter(user -> normalizedKeyword == null || user.username.contains(normalizedKeyword)
                        || normalize(user.displayName).contains(normalizedKeyword)
                        || user.email.contains(normalizedKeyword))
                .sorted(Comparator.comparing((UserAccount user) -> user.createdAt).reversed())
                .map(this::summary)
                .toList();
        return page(all, safePage, safePageSize);
    }

    public synchronized UserSummary user(String token, String userId) {
        UserAccount actor = requireUser(token);
        UserAccount user = userById(userId);
        if (!actor.id.equals(userId)) {
            requireAdmin(actor);
        }
        return summary(user);
    }

    public synchronized UserSummary updateUser(String token, String userId, Map<String, Object> updates, String sourceIp,
            String requestId) {
        UserAccount actor = requireUser(token);
        UserAccount user = userById(userId);
        Object before = summary(user);
        boolean self = actor.id.equals(user.id);
        if (!self) {
            requireAdmin(actor);
            if (actor.role == Role.ADMIN && user.role == Role.OWNER) {
                throw new ApiException(42100, HttpStatus.FORBIDDEN, "cannot modify owner user");
            }
        }
        if (updates.containsKey("displayName")) {
            user.displayName = Objects.toString(updates.get("displayName"), user.displayName);
        }
        if (updates.containsKey("email")) {
            String email = normalize(Objects.toString(updates.get("email"), user.email));
            String existingUserId = userIdByEmail.get(email);
            if (existingUserId != null && !existingUserId.equals(user.id)) {
                throw new ApiException(41105, HttpStatus.CONFLICT, "email already exists");
            }
            userIdByEmail.remove(user.email);
            user.email = email;
            userIdByEmail.put(email, user.id);
        }
        if (updates.containsKey("avatarUrl")) {
            user.avatarUrl = Objects.toString(updates.get("avatarUrl"), null);
        }
        if (!self && updates.containsKey("status")) {
            user.status = parseEnum(UserStatus.class, updates.get("status"));
            invalidateSessions(user.id);
        }
        user.updatedAt = Instant.now();
        audit(requestId, actor.id, actor.role, actor.permissions, sourceIp, "USER", user.id, "UPDATE_USER", "MEDIUM", null,
                before, summary(user), "SUCCESS", null);
        return summary(user);
    }

    public synchronized UserSummary updateRoles(String token, String userId, Role role, List<Permission> permissions,
            String reason, String sourceIp, String requestId) {
        UserAccount actor = requireUser(token);
        if (actor.role != Role.OWNER) {
            throw new ApiException(42001, HttpStatus.FORBIDDEN, "role permission denied");
        }
        UserAccount user = userById(userId);
        if (user.role == Role.OWNER && role != Role.OWNER && ownerCount() <= 1) {
            throw new ApiException(42101, HttpStatus.FORBIDDEN, "cannot remove only owner");
        }
        Object before = summary(user);
        Role previousRole = user.role;
        List<Permission> previousPermissions = List.copyOf(user.permissions);
        user.role = role;
        user.permissions = new ArrayList<>(permissions == null ? List.of() : permissions);
        user.updatedAt = Instant.now();
        if (roleRank(role) < roleRank(previousRole) || !user.permissions.containsAll(previousPermissions)) {
            invalidateSessions(user.id);
        }
        audit(requestId, actor.id, actor.role, actor.permissions, sourceIp, "USER", user.id, "UPDATE_ROLE", "HIGH", reason,
                before, summary(user), "SUCCESS", null);
        return summary(user);
    }

    public synchronized InviteCreation createInvite(String token, InviteType type, Role role, List<Permission> permissions,
            int maxUses, Instant expiresAt, String note, String sourceIp, String requestId) {
        UserAccount actor = requireUser(token);
        requireAdmin(actor);
        if (type == InviteType.ADMIN && actor.role != Role.OWNER) {
            throw new ApiException(41205, HttpStatus.FORBIDDEN, "admin invite permission denied");
        }
        if (type == InviteType.PLAYER && role != Role.USER) {
            throw new ApiException(42001, HttpStatus.FORBIDDEN, "player invite can only grant user role");
        }
        if (role == Role.OWNER) {
            throw new ApiException(42001, HttpStatus.FORBIDDEN, "owner role invite is not allowed");
        }
        String rawCode = "BM-" + type.name() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        String id = nextInviteId();
        Invite invite = new Invite(id, hash(rawCode), rawCode.substring(0, Math.min(rawCode.length(), 12)), type, role,
                permissions == null ? List.of() : permissions, Math.max(maxUses, 1), InviteStatus.ACTIVE, expiresAt, note,
                actor.id, Instant.now());
        invitesById.put(id, invite);
        inviteIdByCodeHash.put(invite.codeHash, invite.id);
        audit(requestId, actor.id, actor.role, actor.permissions, sourceIp, "INVITE", invite.id, "CREATE_INVITE", "HIGH", note,
                null, inviteSummary(invite), "SUCCESS", null);
        return new InviteCreation(invite.id, rawCode, invite.type, invite.role, invite.permissions, invite.maxUses,
                invite.usedCount, currentStatus(invite), invite.expiresAt);
    }

    public synchronized PageResponse<InviteSummary> invites(String token, int page, int pageSize) {
        UserAccount actor = requireUser(token);
        requireAdmin(actor);
        return page(invitesById.values().stream()
                .sorted(Comparator.comparing((Invite invite) -> invite.createdAt).reversed())
                .map(this::inviteSummary)
                .toList(), Math.max(page, 1), Math.min(Math.max(pageSize, 1), 100));
    }

    public synchronized InviteSummary updateInvite(String token, String inviteId, InviteStatus status, String sourceIp,
            String requestId) {
        UserAccount actor = requireUser(token);
        requireAdmin(actor);
        Invite invite = inviteById(inviteId);
        if (invite.type == InviteType.ADMIN && actor.role != Role.OWNER) {
            throw new ApiException(41205, HttpStatus.FORBIDDEN, "admin invite permission denied");
        }
        Object before = inviteSummary(invite);
        if (status != null) {
            invite.status = status;
        }
        audit(requestId, actor.id, actor.role, actor.permissions, sourceIp, "INVITE", invite.id, "UPDATE_INVITE", "HIGH",
                null, before, inviteSummary(invite), "SUCCESS", null);
        return inviteSummary(invite);
    }

    public synchronized PageResponse<InviteUseSummary> inviteUses(String token, String inviteId, int page, int pageSize) {
        UserAccount actor = requireUser(token);
        requireAdmin(actor);
        Invite invite = inviteById(inviteId);
        return page(invite.uses.stream()
                .map(use -> new InviteUseSummary(use.id(), use.inviteId(), use.userId(), use.username(), use.sourceIp(),
                        use.usedAt()))
                .toList(), Math.max(page, 1), Math.min(Math.max(pageSize, 1), 100));
    }

    public synchronized UserSummary bindMinecraft(String token, String minecraftId, String minecraftUuid, String sourceIp,
            String requestId) {
        UserAccount user = requireUser(token);
        requireNonBlank(minecraftId, "minecraftId");
        requireNonBlank(minecraftUuid, "minecraftUuid");
        String normalized = normalize(minecraftId);
        String existing = userIdByMinecraftId.get(normalized);
        if (existing != null && !existing.equals(user.id)) {
            throw new ApiException(41300, HttpStatus.CONFLICT, "minecraft identity already bound");
        }
        Object before = summary(user);
        if (user.minecraftId != null) {
            userIdByMinecraftId.remove(normalize(user.minecraftId));
        }
        user.minecraftId = minecraftId;
        user.minecraftUuid = minecraftUuid;
        userIdByMinecraftId.put(normalized, user.id);
        user.updatedAt = Instant.now();
        audit(requestId, user.id, user.role, user.permissions, sourceIp, "MINECRAFT_BINDING", user.id, "BIND_MINECRAFT",
                "MEDIUM", null, before, summary(user), "SUCCESS", null);
        return summary(user);
    }

    public synchronized UserSummary unbindMinecraft(String token, String targetUserId, String sourceIp, String requestId) {
        UserAccount actor = requireUser(token);
        UserAccount user = targetUserId == null ? actor : userById(targetUserId);
        if (!actor.id.equals(user.id)) {
            requireAdmin(actor);
        }
        Object before = summary(user);
        if (user.minecraftId != null) {
            userIdByMinecraftId.remove(normalize(user.minecraftId));
        }
        user.minecraftId = null;
        user.minecraftUuid = null;
        user.updatedAt = Instant.now();
        audit(requestId, actor.id, actor.role, actor.permissions, sourceIp, "MINECRAFT_BINDING", user.id, "UNBIND_MINECRAFT",
                "HIGH", null, before, summary(user), "SUCCESS", null);
        return summary(user);
    }

    public synchronized Map<String, Object> requestPasswordReset(String usernameOrEmail, String sourceIp, String requestId) {
        Optional<UserAccount> user = userByUsername(usernameOrEmail);
        if (user.isEmpty()) {
            user = Optional.ofNullable(userIdByEmail.get(normalize(usernameOrEmail))).map(usersById::get);
        }
        if (user.isEmpty()) {
            return Map.of("accepted", true);
        }
        String token = "rst_" + UUID.randomUUID().toString().replace("-", "");
        PasswordResetToken resetToken = new PasswordResetToken(nextResetId(), hash(token), user.get().id,
                Instant.now().plus(Duration.ofMinutes(30)));
        resetTokensByHash.put(resetToken.tokenHash, resetToken);
        audit(requestId, user.get().id, user.get().role, user.get().permissions, sourceIp, "USER", user.get().id,
                "REQUEST_PASSWORD_RESET", "MEDIUM", null, null, null, "SUCCESS", null);
        return Map.of("accepted", true, "resetToken", token);
    }

    public synchronized Map<String, Object> confirmPasswordReset(String token, String newPassword, String sourceIp,
            String requestId) {
        requireNonBlank(token, "token");
        requireNonBlank(newPassword, "newPassword");
        PasswordResetToken resetToken = resetTokensByHash.get(hash(token));
        if (resetToken == null || resetToken.used) {
            throw new ApiException(41400, HttpStatus.BAD_REQUEST, "invalid password reset token");
        }
        if (resetToken.expiresAt.isBefore(Instant.now())) {
            throw new ApiException(41401, HttpStatus.BAD_REQUEST, "expired password reset token");
        }
        UserAccount user = userById(resetToken.userId);
        user.passwordHash = passwordEncoder.encode(newPassword);
        user.updatedAt = Instant.now();
        resetToken.used = true;
        invalidateSessions(user.id);
        audit(requestId, user.id, user.role, user.permissions, sourceIp, "USER", user.id, "CONFIRM_PASSWORD_RESET",
                "MEDIUM", null, null, null, "SUCCESS", null);
        return Map.of("reset", true);
    }

    private AuthResult createSession(UserAccount user, Instant now) {
        String token = "atk_" + UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = now.plus(Duration.ofHours(12));
        sessionsByHash.put(hash(token), new SessionToken(hash(token), user.id, expiresAt));
        return new AuthResult(token, expiresAt, summary(user));
    }

    private UserAccount requireUser(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(41000, HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        SessionToken session = sessionsByHash.get(hash(token));
        if (session == null || !session.active) {
            throw new ApiException(41001, HttpStatus.UNAUTHORIZED, "invalid session");
        }
        if (session.expiresAt.isBefore(Instant.now())) {
            throw new ApiException(41002, HttpStatus.UNAUTHORIZED, "session expired");
        }
        UserAccount user = userById(session.userId);
        if (user.status == UserStatus.DISABLED || user.status == UserStatus.BANNED || user.status == UserStatus.DELETED) {
            throw new ApiException(41001, HttpStatus.UNAUTHORIZED, "invalid session");
        }
        return user;
    }

    private void requireAdmin(UserAccount user) {
        if (user.role != Role.ADMIN && user.role != Role.OWNER) {
            throw new ApiException(42001, HttpStatus.FORBIDDEN, "role permission denied");
        }
    }

    private Invite inviteByRawCode(String rawCode) {
        String inviteId = inviteIdByCodeHash.get(hash(rawCode));
        if (inviteId == null) {
            throw new ApiException(41201, HttpStatus.NOT_FOUND, "invite not found");
        }
        return invitesById.get(inviteId);
    }

    private Invite inviteById(String inviteId) {
        Invite invite = invitesById.get(inviteId);
        if (invite == null) {
            throw new ApiException(43000, HttpStatus.NOT_FOUND, "invite not found");
        }
        return invite;
    }

    private void ensureInviteUsable(Invite invite) {
        InviteStatus status = currentStatus(invite);
        if (status == InviteStatus.DISABLED) {
            throw new ApiException(41202, HttpStatus.CONFLICT, "invite unavailable");
        }
        if (status == InviteStatus.EXPIRED) {
            throw new ApiException(41203, HttpStatus.CONFLICT, "invite expired");
        }
        if (status == InviteStatus.EXHAUSTED) {
            throw new ApiException(41204, HttpStatus.CONFLICT, "invite exhausted");
        }
    }

    private InviteStatus currentStatus(Invite invite) {
        if (invite.status == InviteStatus.DISABLED) {
            return InviteStatus.DISABLED;
        }
        if (invite.expiresAt != null && invite.expiresAt.isBefore(Instant.now())) {
            return InviteStatus.EXPIRED;
        }
        if (invite.usedCount >= invite.maxUses) {
            return InviteStatus.EXHAUSTED;
        }
        return InviteStatus.ACTIVE;
    }

    private ApiException invalidCredentials(String requestId, String sourceIp, String username) {
        audit(requestId, null, null, List.of(), sourceIp, "USER", username, "LOGIN", "LOW", null, null, null, "FAILED",
                "bad credentials");
        return new ApiException(41101, HttpStatus.UNAUTHORIZED, "username or password is incorrect");
    }

    private void invalidateSessions(String userId) {
        sessionsByHash.values().stream()
                .filter(session -> session.userId.equals(userId))
                .forEach(session -> session.active = false);
    }

    private void saveUserIndexes(UserAccount user) {
        usersById.put(user.id, user);
        userIdByUsername.put(normalize(user.username), user.id);
        userIdByEmail.put(normalize(user.email), user.id);
    }

    private Optional<UserAccount> userByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(userIdByUsername.get(normalize(username))).map(usersById::get);
    }

    private UserAccount userById(String userId) {
        UserAccount user = usersById.get(userId);
        if (user == null) {
            throw new ApiException(43000, HttpStatus.NOT_FOUND, "user not found");
        }
        return user;
    }

    private UserSummary summary(UserAccount user) {
        return new UserSummary(user.id, user.username, user.displayName, user.role, List.copyOf(user.permissions),
                user.status, user.minecraftId, user.minecraftUuid, user.avatarUrl);
    }

    private InviteSummary inviteSummary(Invite invite) {
        return new InviteSummary(invite.id, invite.codePrefix, invite.type, invite.role, List.copyOf(invite.permissions),
                invite.maxUses, invite.usedCount, currentStatus(invite), invite.expiresAt, invite.note);
    }

    private <T> PageResponse<T> page(List<T> items, int page, int pageSize) {
        int from = Math.min((page - 1) * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageResponse<>(items.subList(from, to), page, pageSize, items.size());
    }

    private int ownerCount() {
        return (int) usersById.values().stream()
                .filter(user -> user.role == Role.OWNER && user.status != UserStatus.DELETED)
                .count();
    }

    private int roleRank(Role role) {
        return switch (role) {
            case USER -> 1;
            case HELPER -> 2;
            case ADMIN -> 3;
            case OWNER -> 4;
        };
    }

    private void audit(String requestId, String actorUserId, Role actorRole, List<Permission> actorPermissions, String sourceIp,
            String targetType, String targetId, String action, String riskLevel, String reason, Object beforeState,
            Object afterState, String result, String failureReason) {
        auditLogs.add(new AuditLog("audit_" + auditSequence.getAndIncrement(), requestId, actorUserId, actorRole,
                actorPermissions == null ? List.of() : List.copyOf(actorPermissions), sourceIp, targetType, targetId,
                action, riskLevel, reason, beforeState, afterState, result, failureReason, Instant.now()));
    }

    private void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ApiException(40001, HttpStatus.BAD_REQUEST, "invalid request");
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, Object value) {
        try {
            return Enum.valueOf(enumType, Objects.toString(value));
        } catch (RuntimeException exception) {
            throw new ApiException(40001, HttpStatus.BAD_REQUEST, "invalid request");
        }
    }

    private String nextUserId() {
        return "user_" + "%03d".formatted(userSequence.getAndIncrement());
    }

    private String nextInviteId() {
        return "invite_" + "%03d".formatted(inviteSequence.getAndIncrement());
    }

    private String nextInviteUseId() {
        return "invite_use_" + "%03d".formatted(inviteUseSequence.getAndIncrement());
    }

    private String nextResetId() {
        return "reset_" + "%03d".formatted(resetSequence.getAndIncrement());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
