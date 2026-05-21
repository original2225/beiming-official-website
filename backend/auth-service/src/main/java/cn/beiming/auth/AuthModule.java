package cn.beiming.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {
    private final AuthStore store;

    AuthController(AuthStore store) {
        this.store = store;
    }

    @PostMapping("/register")
    ResponseEntity<Map<String, Object>> register(@RequestBody(required = false) Map<String, Object> body) {
        body = bodyOrEmpty(body);
        String invitationCode = requiredString(body, "invitationCode");
        String username = requiredString(body, "username");
        String password = requiredString(body, "password");
        String displayName = requiredString(body, "displayName");
        validateUsername(username);
        validatePassword(password);
        validateDisplayName(displayName);
        return created(store.register(invitationCode, username, password, displayName, optionalString(body, "idempotencyKey")));
    }

    @PostMapping("/login")
    ResponseEntity<Map<String, Object>> login(@RequestBody(required = false) Map<String, Object> body) {
        body = bodyOrEmpty(body);
        return ok(store.login(requiredString(body, "username"), requiredString(body, "password"), optionalString(body, "idempotencyKey")));
    }

    @PostMapping("/logout")
    ResponseEntity<Map<String, Object>> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        CurrentSession session = store.requireSessionForLogout(authorization);
        store.logout(session);
        return ok(null);
    }

    @GetMapping("/me")
    ResponseEntity<Map<String, Object>> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        CurrentSession session = store.requireSession(authorization);
        return ok(store.userSummary(session.user));
    }

    @GetMapping("/session/verify")
    ResponseEntity<Map<String, Object>> verify(@RequestHeader(value = "Authorization", required = false) String authorization) {
        CurrentSession session = store.requireSession(authorization);
        return ok(mapOf("valid", true, "expiresAt", session.expiresAt.toString(), "user", store.userSummary(session.user)));
    }

    @GetMapping("/me/sessions")
    ResponseEntity<Map<String, Object>> sessions(@RequestHeader(value = "Authorization", required = false) String authorization) {
        CurrentSession session = store.requireSession(authorization);
        return ok(store.currentUserSessions(session));
    }

    @DeleteMapping("/me/sessions/{sessionId}")
    ResponseEntity<Map<String, Object>> revokeSession(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                       @PathVariable String sessionId,
                                                       @RequestBody(required = false) Map<String, Object> body) {
        CurrentSession session = store.requireSession(authorization);
        body = bodyOrEmpty(body);
        store.revokeUserSession(session, sessionId, requiredString(body, "reason"));
        return ok(null);
    }

    @PostMapping("/me/password")
    ResponseEntity<Map<String, Object>> changePassword(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                       @RequestBody(required = false) Map<String, Object> body) {
        CurrentSession session = store.requireSession(authorization);
        body = bodyOrEmpty(body);
        String currentPassword = requiredString(body, "currentPassword");
        String newPassword = requiredString(body, "newPassword");
        validatePassword(newPassword);
        store.changePassword(session, currentPassword, newPassword, requiredString(body, "reason"));
        return ok(null);
    }

    @PostMapping("/password-reset/request")
    ResponseEntity<Map<String, Object>> requestPasswordReset(@RequestBody(required = false) Map<String, Object> body) {
        body = bodyOrEmpty(body);
        store.requestPasswordReset(requiredString(body, "username"));
        return ok(null);
    }

    @PostMapping("/password-reset/confirm")
    ResponseEntity<Map<String, Object>> confirmPasswordReset(@RequestBody(required = false) Map<String, Object> body) {
        body = bodyOrEmpty(body);
        String resetToken = requiredString(body, "resetToken");
        String newPassword = requiredString(body, "newPassword");
        validatePassword(newPassword);
        store.confirmPasswordReset(resetToken, newPassword);
        return ok(null);
    }

    @PutMapping("/me/minecraft-binding")
    ResponseEntity<Map<String, Object>> bindMinecraft(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                       @RequestBody(required = false) Map<String, Object> body) {
        CurrentSession session = store.requireSession(authorization);
        body = bodyOrEmpty(body);
        String minecraftId = requiredString(body, "minecraftId");
        String minecraftUuid = requiredString(body, "minecraftUuid");
        requiredString(body, "verificationCode");
        if (!Pattern.matches("[A-Za-z0-9_]{3,16}", minecraftId) || !Pattern.matches("[a-f0-9]{32}", minecraftUuid)) {
            throw ApiException.badRequest("minecraftBinding");
        }
        return ok(store.bindMinecraft(session.user.username, minecraftId, minecraftUuid));
    }

    @DeleteMapping("/me/minecraft-binding")
    ResponseEntity<Map<String, Object>> unbindMinecraft(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                         @RequestBody(required = false) Map<String, Object> body) {
        CurrentSession session = store.requireSession(authorization);
        body = bodyOrEmpty(body);
        store.unbindMinecraft(session.user.username, requiredString(body, "reason"));
        return ok(null);
    }

    @GetMapping("/admin/users")
    ResponseEntity<Map<String, Object>> users(@RequestHeader(value = "Authorization", required = false) String authorization,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int pageSize,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String role,
                                               @RequestParam(required = false) String sort) {
        CurrentSession session = store.requireSession(authorization);
        store.requireAdmin(session);
        return ok(store.listUsers(page, pageSize, keyword, status, role, sort));
    }

    @GetMapping("/admin/users/{userId}")
    ResponseEntity<Map<String, Object>> userDetail(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                    @PathVariable String userId) {
        CurrentSession session = store.requireSession(authorization);
        store.requireAdmin(session);
        return ok(store.adminUserDetail(userId));
    }

    @PatchMapping("/admin/users/{userId}")
    ResponseEntity<Map<String, Object>> updateUser(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                    @PathVariable String userId,
                                                    @RequestBody(required = false) Map<String, Object> body) {
        CurrentSession session = store.requireSession(authorization);
        store.requireAdmin(session);
        body = bodyOrEmpty(body);
        String reason = requiredString(body, "reason");
        String displayName = optionalString(body, "displayName");
        String status = optionalString(body, "status");
        if (displayName != null) {
            validateDisplayName(displayName);
        }
        return ok(store.updateUser(session.user, userId, displayName, status, reason));
    }

    @PutMapping("/admin/users/{userId}/roles")
    ResponseEntity<Map<String, Object>> updateRoles(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                     @PathVariable String userId,
                                                     @RequestBody(required = false) Map<String, Object> body) {
        CurrentSession session = store.requireSession(authorization);
        store.requireOwner(session);
        body = bodyOrEmpty(body);
        Set<String> roles = stringSet(body.get("roles"));
        Set<String> permissions = stringSet(body.getOrDefault("permissions", List.of()));
        String reason = requiredString(body, "reason");
        if (roles.isEmpty()) {
            throw ApiException.badRequest("roles");
        }
        store.validateRolesAndPermissions(roles, permissions);
        return ok(store.updateRoles(session.user, userId, roles, permissions, reason));
    }

    @GetMapping("/admin/invitations")
    ResponseEntity<Map<String, Object>> invitations(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int pageSize,
                                                    @RequestParam(required = false) String type,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String createdBy) {
        CurrentSession session = store.requireSession(authorization);
        store.requireAdmin(session);
        return ok(store.listInvitations(session.user, page, pageSize, type, status, createdBy));
    }

    @PostMapping("/admin/invitations")
    ResponseEntity<Map<String, Object>> createInvitation(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                          @RequestBody(required = false) Map<String, Object> body) {
        CurrentSession session = store.requireSession(authorization);
        store.requireAdmin(session);
        body = bodyOrEmpty(body);
        String type = requiredString(body, "type");
        Set<String> roles = stringSet(body.get("boundRoles"));
        Set<String> permissions = stringSet(body.getOrDefault("boundPermissions", List.of()));
        int maxUses = intValue(body, "maxUses");
        String reason = requiredString(body, "reason");
        String expiresAtValue = optionalString(body, "expiresAt");
        Instant expiresAt = expiresAtValue == null ? null : Instant.parse(expiresAtValue);
        if (maxUses < 1 || maxUses > 1000 || (expiresAt != null && !expiresAt.isAfter(Instant.now()))) {
            throw ApiException.badRequest("invitation");
        }
        store.validateRolesAndPermissions(roles, permissions);
        return created(store.createInvitation(session.user, type, roles, permissions, maxUses, expiresAt, reason, optionalString(body, "idempotencyKey")));
    }

    @PatchMapping("/admin/invitations/{invitationId}/disable")
    ResponseEntity<Map<String, Object>> disableInvitation(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                           @PathVariable String invitationId,
                                                           @RequestBody(required = false) Map<String, Object> body) {
        CurrentSession session = store.requireSession(authorization);
        store.requireAdmin(session);
        body = bodyOrEmpty(body);
        return ok(store.disableInvitation(session.user, invitationId, requiredString(body, "reason")));
    }

    @GetMapping("/admin/invitations/{invitationId}/usage-records")
    ResponseEntity<Map<String, Object>> usageRecords(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                      @PathVariable String invitationId,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int pageSize) {
        CurrentSession session = store.requireSession(authorization);
        store.requireAdmin(session);
        return ok(store.invitationUsageRecords(session.user, invitationId, page, pageSize));
    }

    private ResponseEntity<Map<String, Object>> ok(Object data) {
        return ResponseEntity.ok(envelope(0, "success", data));
    }

    private ResponseEntity<Map<String, Object>> created(Object data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope(0, "success", data));
    }

    static Map<String, Object> envelope(int code, String message, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", data);
        return body;
    }

    static Map<String, Object> bodyOrEmpty(Map<String, Object> body) {
        return body == null ? Map.of() : body;
    }

    static String requiredString(Map<String, Object> body, String field) {
        String value = optionalString(body, field);
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(field);
        }
        return value;
    }

    static String optionalString(Map<String, Object> body, String field) {
        Object value = body.get(field);
        return value == null ? null : String.valueOf(value);
    }

    static int intValue(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw ApiException.badRequest(field);
    }

    static void validateUsername(String username) {
        if (!Pattern.matches("[A-Za-z0-9_]{3,32}", username)) {
            throw ApiException.badRequest("username");
        }
    }

    static void validateDisplayName(String displayName) {
        if (displayName.length() < 2 || displayName.length() > 24) {
            throw ApiException.badRequest("displayName");
        }
    }

    static void validatePassword(String password) {
        Set<String> commonWeakPasswords = Set.of("password123", "password1234", "qwerty12345", "admin123456", "welcome123", "letmein123");
        if (password.length() < 10 || password.length() > 128 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*") || commonWeakPasswords.contains(password.toLowerCase(Locale.ROOT))) {
            throw ApiException.badRequest("password");
        }
    }

    @SuppressWarnings("unchecked")
    static Set<String> stringSet(Object raw) {
        if (!(raw instanceof List<?> list)) {
            throw ApiException.badRequest("array");
        }
        Set<String> values = new LinkedHashSet<>();
        for (Object item : list) {
            values.add(String.valueOf(item));
        }
        return values;
    }

    static Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}

@Configuration
class AuthLocalWebConfig {
    @Bean
    WebMvcConfigurer authCorsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/v1/auth/**")
                        .allowedOrigins(
                                "http://localhost:5173",
                                "http://127.0.0.1:5173",
                                "http://localhost:5174",
                                "http://127.0.0.1:5174",
                                "http://localhost:5182",
                                "http://127.0.0.1:5182"
                        )
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("X-Request-Id");
            }
        };
    }

    @Bean
    ApplicationRunner authLocalSeedData(AuthStore store) {
        return args -> store.seedLocalDevDataIfEmpty();
    }
}

@Service
class AuthStore {
    private static final Set<String> VALID_ROLES = Set.of("OWNER", "ADMIN", "HELPER", "USER");
    private static final Set<String> VALID_PERMISSIONS = Set.of("NODE_READ", "NODE_WRITE", "CONTAINER_OPERATE", "VM_OPERATE", "FILE_MANAGE", "TERMINAL_ACCESS", "HIGH_RISK_APPROVE");
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, UserAccount> usersById = new ConcurrentHashMap<>();
    private final Map<String, String> userIdByUsername = new ConcurrentHashMap<>();
    private final Map<String, SessionRecord> sessions = new ConcurrentHashMap<>();
    private final Map<String, InvitationRecord> invitationsById = new ConcurrentHashMap<>();
    private final Map<String, String> invitationIdByCode = new ConcurrentHashMap<>();
    private final Map<String, PasswordResetRecord> passwordResets = new ConcurrentHashMap<>();
    private final List<AuditRecord> audits = java.util.Collections.synchronizedList(new ArrayList<>());
    private final Map<String, IdempotencyRecord> registerIdempotency = new ConcurrentHashMap<>();
    private final Map<String, IdempotencyRecord> invitationIdempotency = new ConcurrentHashMap<>();
    private final Map<String, IdempotencyRecord> loginIdempotency = new ConcurrentHashMap<>();
    private final Map<String, Integer> loginFailures = new ConcurrentHashMap<>();
    private final Map<String, Integer> registerAttempts = new ConcurrentHashMap<>();
    private final Map<String, Integer> passwordResetAttempts = new ConcurrentHashMap<>();
    private boolean failNextSessionCreation;
    private boolean failNextAudit;

    synchronized void reset() {
        usersById.clear();
        userIdByUsername.clear();
        sessions.clear();
        invitationsById.clear();
        invitationIdByCode.clear();
        passwordResets.clear();
        audits.clear();
        registerIdempotency.clear();
        invitationIdempotency.clear();
        loginIdempotency.clear();
        loginFailures.clear();
        registerAttempts.clear();
        passwordResetAttempts.clear();
        failNextSessionCreation = false;
        failNextAudit = false;
    }

    synchronized void seedOwner(String username, String password) {
        seedUser(username, password, Set.of("OWNER"), VALID_PERMISSIONS, "ACTIVE");
    }

    synchronized void seedUser(String username, String password, Set<String> roles, Set<String> permissions, String status) {
        String id = "usr_" + UUID.randomUUID();
        UserAccount user = new UserAccount(id, username, username, encoder.encode(password), roles, permissions, status, Instant.now(), Instant.now());
        usersById.put(id, user);
        userIdByUsername.put(username.toLowerCase(Locale.ROOT), id);
    }

    synchronized void seedInvitation(String rawCode, String type, Set<String> roles, Set<String> permissions, int maxUses, Instant expiresAt, String createdByUsername) {
        UserAccount creator = findByUsername(createdByUsername);
        InvitationRecord invitation = new InvitationRecord("inv_" + UUID.randomUUID(), rawCode.substring(0, Math.min(rawCode.length(), 8)), encoder.encode(rawCode), type, roles, permissions, maxUses, 0, expiresAt, creator.id, Instant.now());
        invitationsById.put(invitation.id, invitation);
        invitationIdByCode.put(rawCode, invitation.id);
    }

    synchronized void disableInvitationByCode(String rawCode, String reason) {
        InvitationRecord invitation = invitationByRawCode(rawCode);
        invitation.disabledAt = Instant.now();
    }

    synchronized void exhaustInvitationByCode(String rawCode) {
        InvitationRecord invitation = invitationByRawCode(rawCode);
        invitation.usedCount = invitation.maxUses;
    }

    synchronized void seedMinecraftBinding(String username, String minecraftId, String minecraftUuid) {
        findByUsername(username).minecraftBinding = new MinecraftBinding(minecraftId, minecraftUuid, Instant.now(), "MANUAL_VERIFICATION");
    }

    synchronized void seedLocalDevDataIfEmpty() {
        if (!usersById.isEmpty()) {
            return;
        }
        seedOwner("owner", "Password12345");
        seedUser("admin", "Password12345", Set.of("ADMIN"), Set.of(), "ACTIVE");
        seedUser("helper", "Password12345", Set.of("HELPER"), Set.of(), "ACTIVE");
        seedUser("user", "Password12345", Set.of("USER"), Set.of(), "ACTIVE");
        seedUser("disabled", "Password12345", Set.of("USER"), Set.of(), "DISABLED");
        seedUser("banned", "Password12345", Set.of("USER"), Set.of(), "BANNED");
        seedUser("deleted", "Password12345", Set.of("USER"), Set.of(), "DELETED");
        seedInvitation("PLAYER-CODE-1", "PLAYER", Set.of("USER"), Set.of(), 10, null, "owner");
        seedInvitation("ADMIN-CODE-1", "ADMIN", Set.of("ADMIN"), Set.of(), 3, null, "owner");
        seedInvitation("LAST-CODE-1", "PLAYER", Set.of("USER"), Set.of(), 1, null, "owner");
        seedMinecraftBinding("user", "UsedName", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }

    synchronized int invitationUsedCount(String rawCode) {
        return invitationByRawCode(rawCode).usedCount;
    }

    synchronized String invitationId(String rawCode) {
        return invitationByRawCode(rawCode).id;
    }

    synchronized String userId(String username) {
        return findByUsername(username).id;
    }

    synchronized String passwordHash(String username) {
        return findByUsername(username).passwordHash;
    }

    synchronized String invitationStoredSecret(String rawCode) {
        return invitationByRawCode(rawCode).codeHash;
    }

    synchronized List<String> auditActions() {
        return audits.stream().map(audit -> audit.action).toList();
    }

    synchronized long auditCount(String action) {
        return audits.stream().filter(audit -> audit.action.equals(action)).count();
    }

    synchronized String latestPasswordResetToken(String username) {
        UserAccount user = findByUsername(username);
        return passwordResets.values().stream()
                .filter(record -> record.userId.equals(user.id))
                .max(Comparator.comparing(record -> record.createdAt))
                .orElseThrow()
                .token;
    }

    synchronized String createExpiredPasswordResetToken(String username) {
        UserAccount user = findByUsername(username);
        String token = "rst_" + UUID.randomUUID();
        passwordResets.put(token, new PasswordResetRecord(token, user.id, Instant.now().minus(2, ChronoUnit.HOURS), Instant.now().minus(1, ChronoUnit.HOURS)));
        return token;
    }

    synchronized String latestAuditRequestId(String action) {
        return audits.stream().filter(audit -> audit.action.equals(action)).reduce((first, second) -> second).orElseThrow().requestId;
    }

    synchronized String latestAuditAction() {
        return audits.get(audits.size() - 1).action;
    }

    synchronized void expireSession(String token) {
        SessionRecord session = sessions.get(token);
        if (session != null) {
            session.expiresAt = Instant.now().minusSeconds(1);
        }
    }

    synchronized void setUserStatus(String username, String status) {
        findByUsername(username).status = status;
    }

    synchronized String displayName(String username) {
        return findByUsername(username).displayName;
    }

    synchronized String invitationStatus(String rawCode) {
        return invitationStatus(invitationByRawCode(rawCode));
    }

    synchronized void resetRegisterAttempts() {
        registerAttempts.clear();
    }

    synchronized boolean userExists(String username) {
        return userIdByUsername.containsKey(username.toLowerCase(Locale.ROOT));
    }

    synchronized void failNextSessionCreation() {
        failNextSessionCreation = true;
    }

    synchronized void failNextAudit() {
        failNextAudit = true;
    }

    synchronized Map<String, Object> register(String rawCode, String username, String password, String displayName, String idempotencyKey) {
        int attempts = registerAttempts.merge("global", 1, Integer::sum);
        if (attempts > 5) {
            throw new ApiException(44101, HttpStatus.TOO_MANY_REQUESTS, "too many register attempts");
        }
        String signature = signature(rawCode, username, password, displayName);
        if (idempotencyKey != null) {
            IdempotencyRecord old = registerIdempotency.get(idempotencyKey);
            if (old != null) {
                if (!old.signature.equals(signature)) {
                    throw new ApiException(43002, HttpStatus.CONFLICT, "idempotency key conflict");
                }
                return old.payload;
            }
        }
        if (userIdByUsername.containsKey(username.toLowerCase(Locale.ROOT))) {
            throw new ApiException(43110, HttpStatus.CONFLICT, "username exists");
        }
        if (usersById.values().stream().anyMatch(user -> user.displayName.equalsIgnoreCase(displayName))) {
            throw new ApiException(43111, HttpStatus.CONFLICT, "display name exists");
        }
        InvitationRecord invitation = invitationByRawCodeForUse(rawCode);
        if (invitation.boundRoles.contains("OWNER")) {
            throw new ApiException(42101, HttpStatus.FORBIDDEN, "owner cannot be registered");
        }
        if (invitation.usedCount >= invitation.maxUses) {
            throw new ApiException(43114, HttpStatus.CONFLICT, "invitation exhausted");
        }
        String id = "usr_" + UUID.randomUUID();
        int beforeUsedCount = invitation.usedCount;
        int beforeUsageSize = invitation.usageRecords.size();
        UserAccount user = new UserAccount(id, username, displayName, encoder.encode(password), invitation.boundRoles, invitation.boundPermissions, "PENDING_PROFILE", Instant.now(), Instant.now());
        try {
            usersById.put(id, user);
            userIdByUsername.put(username.toLowerCase(Locale.ROOT), id);
            invitation.usedCount++;
            invitation.usageRecords.add(Map.of("id", "use_" + UUID.randomUUID(), "invitationId", invitation.id, "usedByUserId", id, "usedByUsername", username, "usedAt", Instant.now().toString(), "sourceIp", "127.0.0.1", "requestId", RequestIdFilter.currentRequestId()));
            try {
                audit("AUTH_REGISTER_SUCCESS", null, user, "SUCCESS", null, null, null);
                if ("ADMIN".equals(invitation.type)) {
                    audit("AUTH_ADMIN_INVITATION_USED", null, user, "SUCCESS", null, null, null);
                }
            } catch (AuditWriteException exception) {
                audits.add(new AuditRecord("aud_" + UUID.randomUUID(), RequestIdFilter.currentRequestId(), null, null, user.id, "AUTH_AUDIT_COMPENSATION_RECORDED", null, null, null, "SUCCESS", exception.getMessage(), Instant.now()));
            }
            Map<String, Object> payload = sessionPayload(user);
            if (idempotencyKey != null) {
                registerIdempotency.put(idempotencyKey, new IdempotencyRecord(signature, payload, null));
            }
            return payload;
        } catch (RuntimeException exception) {
            usersById.remove(id);
            userIdByUsername.remove(username.toLowerCase(Locale.ROOT));
            invitation.usedCount = beforeUsedCount;
            while (invitation.usageRecords.size() > beforeUsageSize) {
                invitation.usageRecords.remove(invitation.usageRecords.size() - 1);
            }
            throw exception;
        }
    }

    synchronized Map<String, Object> login(String username, String password, String idempotencyKey) {
        String signature = signature(username, password);
        if (idempotencyKey != null) {
            IdempotencyRecord old = loginIdempotency.get(idempotencyKey);
            if (old != null) {
                if (!old.signature.equals(signature)) {
                    throw new ApiException(43002, HttpStatus.CONFLICT, "idempotency key conflict");
                }
                return old.payload;
            }
        }
        String key = username.toLowerCase(Locale.ROOT);
        if (loginFailures.getOrDefault(key, 0) >= 5) {
            audit("AUTH_LOGIN_RISK_BLOCKED", null, null, "FAILED", null, null, "too many attempts");
            throw new ApiException(44100, HttpStatus.TOO_MANY_REQUESTS, "too many login attempts");
        }
        UserAccount user = userIdByUsername.containsKey(key) ? usersById.get(userIdByUsername.get(key)) : null;
        if (user == null || !encoder.matches(password, user.passwordHash)) {
            loginFailures.merge(key, 1, Integer::sum);
            throw new ApiException(41100, HttpStatus.UNAUTHORIZED, "invalid username or password");
        }
        if ("DISABLED".equals(user.status)) {
            throw new ApiException(41101, HttpStatus.UNAUTHORIZED, "user disabled");
        }
        if ("BANNED".equals(user.status)) {
            throw new ApiException(41102, HttpStatus.UNAUTHORIZED, "user banned");
        }
        if ("DELETED".equals(user.status)) {
            throw new ApiException(43116, HttpStatus.CONFLICT, "user status conflict");
        }
        loginFailures.remove(key);
        user.lastLoginAt = Instant.now();
        audit("AUTH_LOGIN_SUCCESS", user, user, "SUCCESS", null, null, null);
        Map<String, Object> payload = sessionPayload(user);
        if (idempotencyKey != null) {
            loginIdempotency.put(idempotencyKey, new IdempotencyRecord(signature, payload, null));
        }
        return payload;
    }

    synchronized void logout(CurrentSession current) {
        if (!current.session.revoked) {
            current.session.revoked = true;
            audit("AUTH_LOGOUT_SUCCESS", current.user, current.user, "SUCCESS", null, null, null);
        }
    }

    synchronized Map<String, Object> currentUserSessions(CurrentSession current) {
        List<Map<String, Object>> rows = sessions.values().stream()
                .filter(session -> session.userId.equals(current.user.id))
                .sorted(Comparator.comparing((SessionRecord session) -> session.createdAt).reversed())
                .map(session -> sessionSummary(session, current.session.id))
                .toList();
        return AuthController.mapOf("items", rows);
    }

    synchronized void revokeUserSession(CurrentSession current, String sessionId, String reason) {
        SessionRecord target = sessions.values().stream()
                .filter(session -> session.id.equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new ApiException(41106, HttpStatus.UNAUTHORIZED, "session not operable"));
        if (!target.userId.equals(current.user.id) || target.expiresAt.isBefore(Instant.now())) {
            throw new ApiException(41106, HttpStatus.UNAUTHORIZED, "session not operable");
        }
        if (!target.revoked) {
            target.revoked = true;
            audit("AUTH_SESSION_REVOKED", current.user, current.user, "SUCCESS", reason, null, sessionId);
        }
    }

    synchronized void changePassword(CurrentSession current, String currentPassword, String newPassword, String reason) {
        if (!encoder.matches(currentPassword, current.user.passwordHash)) {
            throw new ApiException(41105, HttpStatus.UNAUTHORIZED, "current password incorrect");
        }
        if (encoder.matches(newPassword, current.user.passwordHash)) {
            throw new ApiException(43001, HttpStatus.CONFLICT, "same password");
        }
        current.user.passwordHash = encoder.encode(newPassword);
        current.user.updatedAt = Instant.now();
        sessions.values().stream()
                .filter(session -> session.userId.equals(current.user.id))
                .filter(session -> !session.id.equals(current.session.id))
                .forEach(session -> session.revoked = true);
        audit("AUTH_PASSWORD_CHANGED", current.user, current.user, "SUCCESS", reason, null, null);
    }

    synchronized void requestPasswordReset(String username) {
        int attempts = passwordResetAttempts.merge(username.toLowerCase(Locale.ROOT), 1, Integer::sum);
        if (attempts > 5) {
            throw new ApiException(44102, HttpStatus.TOO_MANY_REQUESTS, "too many password reset attempts");
        }
        UserAccount user = userIdByUsername.containsKey(username.toLowerCase(Locale.ROOT)) ? usersById.get(userIdByUsername.get(username.toLowerCase(Locale.ROOT))) : null;
        if (user != null && !"DELETED".equals(user.status)) {
            String token = "rst_" + UUID.randomUUID();
            passwordResets.put(token, new PasswordResetRecord(token, user.id, Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS)));
            audit("AUTH_PASSWORD_RESET_REQUESTED", null, user, "SUCCESS", null, null, null);
        }
    }

    synchronized void confirmPasswordReset(String token, String newPassword) {
        PasswordResetRecord reset = passwordResets.get(token);
        if (reset == null || reset.used || reset.expiresAt.isBefore(Instant.now())) {
            audit("AUTH_PASSWORD_RESET_FAILED", null, null, "FAILED", null, null, "invalid token");
            throw new ApiException(41104, HttpStatus.UNAUTHORIZED, "invalid reset token");
        }
        UserAccount user = usersById.get(reset.userId);
        if (encoder.matches(newPassword, user.passwordHash)) {
            throw new ApiException(43001, HttpStatus.CONFLICT, "same password");
        }
        user.passwordHash = encoder.encode(newPassword);
        reset.used = true;
        revokeSessions(user.id);
        audit("AUTH_PASSWORD_RESET_CONFIRMED", null, user, "SUCCESS", null, null, null);
    }

    synchronized Map<String, Object> bindMinecraft(String username, String minecraftId, String minecraftUuid) {
        UserAccount user = findByUsername(username);
        if (user.minecraftBinding != null) {
            if (user.minecraftBinding.minecraftId.equals(minecraftId) && user.minecraftBinding.minecraftUuid.equals(minecraftUuid)) {
                return minecraftBindingMap(user.minecraftBinding);
            }
            throw new ApiException(43116, HttpStatus.CONFLICT, "already bound");
        }
        boolean occupied = usersById.values().stream()
                .filter(other -> other.minecraftBinding != null)
                .anyMatch(other -> other.minecraftBinding.minecraftId.equalsIgnoreCase(minecraftId) || other.minecraftBinding.minecraftUuid.equals(minecraftUuid));
        if (occupied) {
            throw new ApiException(43115, HttpStatus.CONFLICT, "minecraft identity bound");
        }
        user.minecraftBinding = new MinecraftBinding(minecraftId, minecraftUuid, Instant.now(), "MANUAL_VERIFICATION");
        audit("AUTH_MINECRAFT_BOUND", user, user, "SUCCESS", null, null, null);
        return minecraftBindingMap(user.minecraftBinding);
    }

    synchronized void unbindMinecraft(String username, String reason) {
        UserAccount user = findByUsername(username);
        if (user.minecraftBinding == null) {
            throw new ApiException(43102, HttpStatus.NOT_FOUND, "minecraft binding not found");
        }
        user.minecraftBinding = null;
        audit("AUTH_MINECRAFT_UNBOUND", user, user, "SUCCESS", reason, null, null);
    }

    synchronized Map<String, Object> listUsers(int page, int pageSize, String keyword, String status, String role, String sort) {
        validatePage(page, pageSize);
        if (status != null && !Set.of("PENDING_PROFILE", "ACTIVE", "DISABLED", "BANNED", "DELETED").contains(status)) {
            throw ApiException.badRequest("status");
        }
        if (role != null && !VALID_ROLES.contains(role)) {
            throw ApiException.badRequest("role");
        }
        if (sort != null && !Set.of("createdAt_desc", "createdAt_asc", "lastLoginAt_desc", "updatedAt_desc").contains(sort)) {
            throw new ApiException(40003, HttpStatus.BAD_REQUEST, "invalid sort");
        }
        List<Map<String, Object>> rows = usersById.values().stream()
                .filter(user -> keyword == null || user.username.contains(keyword) || user.displayName.contains(keyword) || (user.minecraftBinding != null && user.minecraftBinding.minecraftId.contains(keyword)))
                .filter(user -> status == null || user.status.equals(status))
                .filter(user -> role == null || user.roles.contains(role))
                .sorted(Comparator.comparing((UserAccount user) -> user.createdAt).reversed())
                .map(this::userSummary)
                .toList();
        return page(rows, page, pageSize);
    }

    synchronized Map<String, Object> adminUserDetail(String userId) {
        UserAccount user = usersById.get(userId);
        if (user == null) {
            throw new ApiException(43100, HttpStatus.NOT_FOUND, "user not found");
        }
        return userSummary(user);
    }

    synchronized Map<String, Object> updateUser(UserAccount actor, String userId, String displayName, String status, String reason) {
        UserAccount target = usersById.get(userId);
        if (target == null) {
            throw new ApiException(43100, HttpStatus.NOT_FOUND, "user not found");
        }
        if (target.roles.contains("OWNER") && !actor.roles.contains("OWNER")) {
            throw new ApiException(42100, HttpStatus.FORBIDDEN, "cannot modify owner");
        }
        if (status != null && !Set.of("PENDING_PROFILE", "ACTIVE", "DISABLED", "BANNED", "DELETED").contains(status)) {
            throw ApiException.badRequest("status");
        }
        if (target.roles.contains("OWNER") && Set.of("DISABLED", "BANNED", "DELETED").contains(status) && ownerCount() == 1) {
            throw new ApiException(42101, HttpStatus.FORBIDDEN, "cannot disable only owner");
        }
        if ("DELETED".equals(target.status) && !"DELETED".equals(status)) {
            throw new ApiException(43116, HttpStatus.CONFLICT, "deleted user cannot recover");
        }
        if (displayName != null && usersById.values().stream().anyMatch(user -> !user.id.equals(target.id) && user.displayName.equalsIgnoreCase(displayName))) {
            throw new ApiException(43111, HttpStatus.CONFLICT, "display name exists");
        }
        String before = state(target);
        String oldDisplayName = target.displayName;
        String oldStatus = target.status;
        if (displayName != null) {
            target.displayName = displayName;
        }
        if (status != null) {
            target.status = status;
            if (Set.of("DISABLED", "BANNED", "DELETED").contains(status)) {
                revokeSessions(target.id);
            }
        }
        target.updatedAt = Instant.now();
        try {
            audit("AUTH_USER_UPDATED", actor, target, "SUCCESS", reason, before, state(target));
        } catch (RuntimeException exception) {
            target.displayName = oldDisplayName;
            target.status = oldStatus;
            throw exception;
        }
        return userSummary(target);
    }

    synchronized Map<String, Object> updateRoles(UserAccount actor, String userId, Set<String> roles, Set<String> permissions, String reason) {
        UserAccount target = usersById.get(userId);
        if (target == null) {
            throw new ApiException(43100, HttpStatus.NOT_FOUND, "user not found");
        }
        if (target.roles.contains("OWNER") && !roles.contains("OWNER") && ownerCount() == 1) {
            throw new ApiException(42101, HttpStatus.FORBIDDEN, "cannot remove only owner");
        }
        String before = state(target);
        target.roles = new LinkedHashSet<>(roles);
        target.permissions = new LinkedHashSet<>(permissions);
        target.updatedAt = Instant.now();
        revokeSessions(target.id);
        audit("AUTH_ROLE_PERMISSION_UPDATED", actor, target, "SUCCESS", reason, before, state(target));
        return userSummary(target);
    }

    synchronized Map<String, Object> listInvitations(UserAccount actor, int page, int pageSize, String type, String status, String createdBy) {
        validatePage(page, pageSize);
        if (type != null && !Set.of("PLAYER", "ADMIN").contains(type)) {
            throw ApiException.badRequest("type");
        }
        if (status != null && !Set.of("ACTIVE", "DISABLED", "EXPIRED", "EXHAUSTED").contains(status)) {
            throw ApiException.badRequest("status");
        }
        List<Map<String, Object>> rows = invitationsById.values().stream()
                .filter(invitation -> type == null || invitation.type.equals(type))
                .filter(invitation -> status == null || invitationStatus(invitation).equals(status))
                .filter(invitation -> createdBy == null || invitation.createdBy.equals(createdBy))
                .sorted(Comparator.comparing((InvitationRecord invitation) -> invitation.createdAt).reversed())
                .map(this::invitationSummary)
                .toList();
        return page(rows, page, pageSize);
    }

    synchronized Map<String, Object> createInvitation(UserAccount actor, String type, Set<String> roles, Set<String> permissions, int maxUses, Instant expiresAt, String reason, String idempotencyKey) {
        String signature = signature(actor.id, type, roles, permissions, maxUses, expiresAt);
        if (idempotencyKey != null) {
            IdempotencyRecord old = invitationIdempotency.get(actor.id + ":" + idempotencyKey);
            if (old != null) {
                if (!old.signature.equals(signature)) {
                    throw new ApiException(43002, HttpStatus.CONFLICT, "idempotency key conflict");
                }
                return old.payload;
            }
        }
        if ("ADMIN".equals(type) && !actor.roles.contains("OWNER")) {
            throw new ApiException(42102, HttpStatus.FORBIDDEN, "admin invitation requires owner");
        }
        if (!actor.roles.contains("OWNER") && (roles.contains("ADMIN") || roles.contains("OWNER") || !permissions.isEmpty())) {
            throw new ApiException(42103, HttpStatus.FORBIDDEN, "cannot grant permissions");
        }
        if (roles.contains("OWNER")) {
            throw new ApiException(42101, HttpStatus.FORBIDDEN, "cannot grant owner");
        }
        String rawCode = "BM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
        InvitationRecord invitation = new InvitationRecord("inv_" + UUID.randomUUID(), rawCode.substring(0, 8), encoder.encode(rawCode), type, roles, permissions, maxUses, 0, expiresAt, actor.id, Instant.now());
        invitationsById.put(invitation.id, invitation);
        invitationIdByCode.put(rawCode, invitation.id);
        Map<String, Object> payload = AuthController.mapOf("invitation", invitationSummary(invitation), "rawCode", rawCode);
        audit("AUTH_INVITATION_CREATED", actor, null, "SUCCESS", reason, null, null);
        if (idempotencyKey != null) {
            invitationIdempotency.put(actor.id + ":" + idempotencyKey, new IdempotencyRecord(signature, payload, rawCode));
        }
        return payload;
    }

    synchronized Map<String, Object> disableInvitation(UserAccount actor, String invitationId, String reason) {
        InvitationRecord invitation = invitationsById.get(invitationId);
        if (invitation == null) {
            throw new ApiException(43101, HttpStatus.NOT_FOUND, "invitation not found");
        }
        if (!actor.roles.contains("OWNER") && (!Objects.equals(invitation.createdBy, actor.id) || !"PLAYER".equals(invitation.type))) {
            throw new ApiException(42001, HttpStatus.FORBIDDEN, "role insufficient");
        }
        if (invitation.disabledAt == null) {
            invitation.disabledAt = Instant.now();
            audit("AUTH_INVITATION_DISABLED", actor, null, "SUCCESS", reason, null, null);
        }
        return invitationSummary(invitation);
    }

    synchronized Map<String, Object> invitationUsageRecords(UserAccount actor, String invitationId, int page, int pageSize) {
        validatePage(page, pageSize);
        InvitationRecord invitation = invitationsById.get(invitationId);
        if (invitation == null) {
            throw new ApiException(43101, HttpStatus.NOT_FOUND, "invitation not found");
        }
        if (!actor.roles.contains("OWNER") && !Objects.equals(invitation.createdBy, actor.id)) {
            throw new ApiException(42001, HttpStatus.FORBIDDEN, "role insufficient");
        }
        return page(invitation.usageRecords, page, pageSize);
    }

    synchronized CurrentSession requireSession(String authorization) {
        return requireSessionInternal(authorization, false);
    }

    synchronized CurrentSession requireSessionForLogout(String authorization) {
        return requireSessionInternal(authorization, true);
    }

    private CurrentSession requireSessionInternal(String authorization, boolean allowRevoked) {
        if (authorization == null || authorization.isBlank()) {
            throw new ApiException(41000, HttpStatus.UNAUTHORIZED, "unauthenticated");
        }
        if (!authorization.startsWith("Bearer ")) {
            throw new ApiException(41003, HttpStatus.UNAUTHORIZED, "invalid token format");
        }
        String token = authorization.substring("Bearer ".length());
        SessionRecord session = sessions.get(token);
        if (session == null) {
            throw new ApiException(41001, HttpStatus.UNAUTHORIZED, "invalid session");
        }
        if (session.revoked && !allowRevoked) {
            throw new ApiException(41103, HttpStatus.UNAUTHORIZED, "session revoked");
        }
        if (session.expiresAt.isBefore(Instant.now())) {
            throw new ApiException(41002, HttpStatus.UNAUTHORIZED, "session expired");
        }
        UserAccount user = usersById.get(session.userId);
        if (user == null) {
            throw new ApiException(41001, HttpStatus.UNAUTHORIZED, "invalid session");
        }
        if ("DISABLED".equals(user.status)) {
            throw new ApiException(41101, HttpStatus.UNAUTHORIZED, "user disabled");
        }
        if ("BANNED".equals(user.status)) {
            throw new ApiException(41102, HttpStatus.UNAUTHORIZED, "user banned");
        }
        if ("DELETED".equals(user.status)) {
            throw new ApiException(43116, HttpStatus.CONFLICT, "user deleted");
        }
        session.lastSeenAt = Instant.now();
        return new CurrentSession(token, session, user, session.expiresAt);
    }

    void requireAdmin(CurrentSession session) {
        if (!session.user.roles.contains("ADMIN") && !session.user.roles.contains("OWNER")) {
            throw new ApiException(42001, HttpStatus.FORBIDDEN, "role insufficient");
        }
    }

    void requireOwner(CurrentSession session) {
        if (!session.user.roles.contains("OWNER")) {
            throw new ApiException(42001, HttpStatus.FORBIDDEN, "role insufficient");
        }
    }

    void validateRolesAndPermissions(Set<String> roles, Set<String> permissions) {
        if (!VALID_ROLES.containsAll(roles) || !VALID_PERMISSIONS.containsAll(permissions)) {
            throw ApiException.badRequest("roles");
        }
    }

    Map<String, Object> userSummary(UserAccount user) {
        return AuthController.mapOf(
                "id", user.id,
                "username", user.username,
                "displayName", user.displayName,
                "roles", new ArrayList<>(user.roles),
                "permissions", new ArrayList<>(user.permissions),
                "status", user.status,
                "minecraftBinding", user.minecraftBinding == null ? null : minecraftBindingMap(user.minecraftBinding),
                "createdAt", user.createdAt.toString(),
                "updatedAt", user.updatedAt.toString(),
                "lastLoginAt", user.lastLoginAt == null ? null : user.lastLoginAt.toString()
        );
    }

    private Map<String, Object> sessionPayload(UserAccount user) {
        if (failNextSessionCreation) {
            failNextSessionCreation = false;
            throw new SessionCreationException("session creation failed");
        }
        String token = "ses_" + UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(2, ChronoUnit.HOURS);
        sessions.put(token, new SessionRecord("ssn_" + UUID.randomUUID(), token, user.id, Instant.now(), expiresAt));
        return AuthController.mapOf("accessToken", token, "tokenType", "Bearer", "expiresAt", expiresAt.toString(), "user", userSummary(user));
    }

    private InvitationRecord invitationByRawCode(String rawCode) {
        String id = invitationIdByCode.get(rawCode);
        if (id == null) {
            throw new ApiException(43101, HttpStatus.NOT_FOUND, "invitation not found");
        }
        return invitationsById.get(id);
    }

    private InvitationRecord invitationByRawCodeForUse(String rawCode) {
        InvitationRecord invitation = invitationByRawCode(rawCode);
        if (invitation.disabledAt != null) {
            throw new ApiException(43112, HttpStatus.CONFLICT, "invitation disabled");
        }
        if (invitation.expiresAt != null && invitation.expiresAt.isBefore(Instant.now())) {
            throw new ApiException(43113, HttpStatus.CONFLICT, "invitation expired");
        }
        if (invitation.usedCount >= invitation.maxUses) {
            throw new ApiException(43114, HttpStatus.CONFLICT, "invitation exhausted");
        }
        return invitation;
    }

    private UserAccount findByUsername(String username) {
        String id = userIdByUsername.get(username.toLowerCase(Locale.ROOT));
        if (id == null) {
            throw new ApiException(43100, HttpStatus.NOT_FOUND, "user not found");
        }
        return usersById.get(id);
    }

    private String invitationStatus(InvitationRecord invitation) {
        if (invitation.disabledAt != null) {
            return "DISABLED";
        }
        if (invitation.expiresAt != null && invitation.expiresAt.isBefore(Instant.now())) {
            return "EXPIRED";
        }
        if (invitation.usedCount >= invitation.maxUses) {
            return "EXHAUSTED";
        }
        return "ACTIVE";
    }

    private Map<String, Object> invitationSummary(InvitationRecord invitation) {
        return AuthController.mapOf(
                "id", invitation.id,
                "codePrefix", invitation.codePrefix,
                "type", invitation.type,
                "status", invitationStatus(invitation),
                "boundRoles", new ArrayList<>(invitation.boundRoles),
                "boundPermissions", new ArrayList<>(invitation.boundPermissions),
                "maxUses", invitation.maxUses,
                "usedCount", invitation.usedCount,
                "expiresAt", invitation.expiresAt == null ? null : invitation.expiresAt.toString(),
                "createdBy", invitation.createdBy,
                "createdAt", invitation.createdAt.toString(),
                "disabledAt", invitation.disabledAt == null ? null : invitation.disabledAt.toString()
        );
    }

    private Map<String, Object> minecraftBindingMap(MinecraftBinding binding) {
        return AuthController.mapOf("minecraftId", binding.minecraftId, "minecraftUuid", binding.minecraftUuid, "verifiedAt", binding.verifiedAt.toString(), "source", binding.source);
    }

    private Map<String, Object> sessionSummary(SessionRecord session, String currentSessionId) {
        return AuthController.mapOf(
                "id", session.id,
                "current", session.id.equals(currentSessionId),
                "createdAt", session.createdAt.toString(),
                "lastSeenAt", session.lastSeenAt.toString(),
                "expiresAt", session.expiresAt.toString(),
                "revoked", session.revoked
        );
    }

    private void revokeSessions(String userId) {
        sessions.values().stream().filter(session -> session.userId.equals(userId)).forEach(session -> session.revoked = true);
    }

    private int ownerCount() {
        return (int) usersById.values().stream().filter(user -> user.roles.contains("OWNER") && !"DELETED".equals(user.status)).count();
    }

    private void audit(String action, UserAccount actor, UserAccount target, String result, String reason, String before, String after) {
        if (failNextAudit) {
            failNextAudit = false;
            throw new AuditWriteException("audit write failed");
        }
        audits.add(new AuditRecord("aud_" + UUID.randomUUID(), RequestIdFilter.currentRequestId(), actor == null ? null : actor.id, actor == null ? null : String.join(",", actor.roles), target == null ? null : target.id, action, reason, before, after, result, null, Instant.now()));
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ApiException(40002, HttpStatus.BAD_REQUEST, "invalid page");
        }
    }

    private Map<String, Object> page(List<Map<String, Object>> rows, int page, int pageSize) {
        int from = Math.min((page - 1) * pageSize, rows.size());
        int to = Math.min(from + pageSize, rows.size());
        return AuthController.mapOf("items", rows.subList(from, to), "page", page, "pageSize", pageSize, "total", rows.size());
    }

    private String signature(Object... values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            return String.valueOf(List.of(values));
        }
    }

    private String state(UserAccount user) {
        return user.username + "|" + user.displayName + "|" + user.status + "|" + user.roles + "|" + user.permissions;
    }
}

@RestControllerAdvice
class AuthExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> api(ApiException exception) {
        Map<String, Object> body = AuthController.envelope(exception.code, exception.getMessage(), null);
        body.put("requestId", RequestIdFilter.currentRequestId());
        if (exception.code == 40001) {
            body.put("errors", List.of(Map.of("field", exception.field == null ? "request" : exception.field, "reason", exception.getMessage())));
        }
        return ResponseEntity.status(exception.status).body(body);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> any(Exception exception) {
        int code = exception instanceof AuditWriteException || exception instanceof SessionCreationException ? 51100 : 50000;
        Map<String, Object> body = AuthController.envelope(code, "internal server error", null);
        body.put("requestId", RequestIdFilter.currentRequestId());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

@Component
class RequestIdFilter extends OncePerRequestFilter {
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    static String currentRequestId() {
        String id = REQUEST_ID.get();
        return id == null ? "req_unknown" : id;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = "req_" + UUID.randomUUID();
        }
        REQUEST_ID.set(requestId);
        response.setHeader("X-Request-Id", requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            REQUEST_ID.remove();
        }
    }
}

class ApiException extends RuntimeException {
    final int code;
    final HttpStatus status;
    final String field;

    ApiException(int code, HttpStatus status, String message) {
        this(code, status, message, null);
    }

    ApiException(int code, HttpStatus status, String message, String field) {
        super(message);
        this.code = code;
        this.status = status;
        this.field = field;
    }

    static ApiException badRequest(String field) {
        return new ApiException(40001, HttpStatus.BAD_REQUEST, "invalid request", field);
    }
}

class AuditWriteException extends RuntimeException {
    AuditWriteException(String message) {
        super(message);
    }
}

class SessionCreationException extends RuntimeException {
    SessionCreationException(String message) {
        super(message);
    }
}

class UserAccount {
    final String id;
    final String username;
    String displayName;
    String passwordHash;
    Set<String> roles;
    Set<String> permissions;
    String status;
    final Instant createdAt;
    Instant updatedAt;
    Instant lastLoginAt;
    MinecraftBinding minecraftBinding;

    UserAccount(String id, String username, String displayName, String passwordHash, Set<String> roles, Set<String> permissions, String status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.roles = new LinkedHashSet<>(roles);
        this.permissions = new LinkedHashSet<>(permissions);
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

class SessionRecord {
    final String id;
    final String token;
    final String userId;
    final Instant createdAt;
    Instant lastSeenAt;
    Instant expiresAt;
    boolean revoked;

    SessionRecord(String id, String token, String userId, Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.token = token;
        this.userId = userId;
        this.createdAt = createdAt;
        this.lastSeenAt = createdAt;
        this.expiresAt = expiresAt;
    }
}

class CurrentSession {
    final String token;
    final SessionRecord session;
    final UserAccount user;
    final Instant expiresAt;

    CurrentSession(String token, SessionRecord session, UserAccount user, Instant expiresAt) {
        this.token = token;
        this.session = session;
        this.user = user;
        this.expiresAt = expiresAt;
    }
}

class InvitationRecord {
    final String id;
    final String codePrefix;
    final String codeHash;
    final String type;
    final Set<String> boundRoles;
    final Set<String> boundPermissions;
    final int maxUses;
    int usedCount;
    final Instant expiresAt;
    final String createdBy;
    final Instant createdAt;
    Instant disabledAt;
    final List<Map<String, Object>> usageRecords = new ArrayList<>();

    InvitationRecord(String id, String codePrefix, String codeHash, String type, Set<String> boundRoles, Set<String> boundPermissions, int maxUses, int usedCount, Instant expiresAt, String createdBy, Instant createdAt) {
        this.id = id;
        this.codePrefix = codePrefix;
        this.codeHash = codeHash;
        this.type = type;
        this.boundRoles = new LinkedHashSet<>(boundRoles);
        this.boundPermissions = new LinkedHashSet<>(boundPermissions);
        this.maxUses = maxUses;
        this.usedCount = usedCount;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}

class PasswordResetRecord {
    final String token;
    final String userId;
    final Instant createdAt;
    final Instant expiresAt;
    boolean used;

    PasswordResetRecord(String token, String userId, Instant createdAt, Instant expiresAt) {
        this.token = token;
        this.userId = userId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
}

class MinecraftBinding {
    final String minecraftId;
    final String minecraftUuid;
    final Instant verifiedAt;
    final String source;

    MinecraftBinding(String minecraftId, String minecraftUuid, Instant verifiedAt, String source) {
        this.minecraftId = minecraftId;
        this.minecraftUuid = minecraftUuid;
        this.verifiedAt = verifiedAt;
        this.source = source;
    }
}

class AuditRecord {
    final String id;
    final String requestId;
    final String actorUserId;
    final String actorRole;
    final String targetId;
    final String action;
    final String reason;
    final String beforeState;
    final String afterState;
    final String result;
    final String failureReason;
    final Instant createdAt;

    AuditRecord(String id, String requestId, String actorUserId, String actorRole, String targetId, String action, String reason, String beforeState, String afterState, String result, String failureReason, Instant createdAt) {
        this.id = id;
        this.requestId = requestId;
        this.actorUserId = actorUserId;
        this.actorRole = actorRole;
        this.targetId = targetId;
        this.action = action;
        this.reason = reason;
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.result = result;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
    }
}

class IdempotencyRecord {
    final String signature;
    final Map<String, Object> payload;
    final String rawSecret;

    IdempotencyRecord(String signature, Map<String, Object> payload, String rawSecret) {
        this.signature = signature;
        this.payload = payload;
        this.rawSecret = rawSecret;
    }
}
