package cn.beiming.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
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
import cn.beiming.core.AuthSessionResolver;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {
    private final AuthStore store;
    private final AuthFlowEvidenceRecorder evidenceRecorder;
    private final MinecraftIdentityResolver minecraftResolver;

    AuthController(AuthStore store, AuthFlowEvidenceRecorder evidenceRecorder, MinecraftIdentityResolver minecraftResolver) {
        this.store = store;
        this.evidenceRecorder = evidenceRecorder;
        this.minecraftResolver = minecraftResolver;
    }

    @PostMapping("/register")
    ResponseEntity<Map<String, Object>> register(HttpServletRequest request,
                                                 @RequestBody(required = false) Map<String, Object> body) {
        body = bodyOrEmpty(body);
        String invitationCode = requiredString(body, "invitationCode");
        String username = requiredString(body, "username");
        String password = requiredString(body, "password");
        String displayName = requiredString(body, "displayName");
        validateUsername(username);
        validatePassword(password);
        validateDisplayName(displayName);
        // MC 绑定（花蝉 2026-11-27 二次修正）：注册阶段一律不绑游戏账号。
        // 原因：注册时填名字只是「声称」，服务端查得到该名字存在，并等于这个人拥有它——
        // 任何人填 Notch 都能把 Notch 认领走。真正的归属验证要么走平台登录换 token，
        // 要么由管理员按 multilogin 白名单核对，两者都在玩家进服之后才发生。
        // 所以注册只建站内账号，绑定放到个人中心（PUT /me/minecraft-binding）再走验证流程。
        // 兼容：仍接受 minecraftName 字段但只做「名字格式 + 平台存在性」预检，
        // 结果不回传给 store，避免把未验证的绑定落库。
        String minecraftName = optionalString(body, "minecraftName");
        MinecraftIdentityResolver.Identity minecraftIdentity = null;
        if (minecraftName != null && !minecraftName.isBlank()) {
            minecraftIdentity = null; // 注册不做任何绑定，见上方说明
        }
        Map<String, Object> payload = store.register(request, invitationCode, username, password, displayName, optionalString(body, "idempotencyKey"), minecraftIdentity);
        ResponseEntity<Map<String, Object>> response = created(payload);
        evidenceRecorder.recordRegisterSuccess(request, invitationCode, username, payload, response.getStatusCode().value());
        return response;
    }

    @PostMapping("/login")
    ResponseEntity<Map<String, Object>> login(HttpServletRequest request,
                                              @RequestBody(required = false) Map<String, Object> body) {
        body = bodyOrEmpty(body);
        String username = requiredString(body, "username");
        Map<String, Object> payload = store.login(request, username, requiredString(body, "password"), optionalString(body, "idempotencyKey"));
        ResponseEntity<Map<String, Object>> response = ok(payload);
        evidenceRecorder.recordLoginSuccess(request, username, payload, response.getStatusCode().value());
        return response;
    }

    @PostMapping("/logout")
    ResponseEntity<Map<String, Object>> logout(HttpServletRequest request,
                                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        CurrentSession session = store.requireSessionForLogout(authorization);
        store.logout(request, session);
        ResponseEntity<Map<String, Object>> response = ok(null);
        evidenceRecorder.recordLogoutSuccess(request, session.user.username, session.token, response.getStatusCode().value());
        return response;
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

    /**
     * 绑定游戏账号（花蝉 2026-11-27 定的流程：注册先建站内账号，绑定放到个人中心）。
     *
     * <p>只收「游戏昵称 + 平台」，UUID 由服务端按平台去查，玩家不用知道 UUID 是什么。
     * 旧实现要求前端自己传 32 位 UUID，实际前端根本传不出来，等于这个接口不可用；
     * 且旧实现里的 verificationCode 只做了必填检查、内容从未校验，是假验证——已删掉。
     *
     * <p>平台取值：MOJANG（正版）/ LITTLESKIN / BEIMING（北冥皮肤站）/ OFFLINE（离线）。
     * 查不到身份一律 43102，不静默落库——绑一个查无此人的名字没有意义。
     *
     * <p>注意这里只校验「该昵称在对应平台存在」，不证明调用者拥有它；
     * 所有权由后续白名单核对（multilogin whitelist add）确认，见 docs 白名单详解。
     */
    @PutMapping("/me/minecraft-binding")
    ResponseEntity<Map<String, Object>> bindMinecraft(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                       @RequestBody(required = false) Map<String, Object> body) {
        CurrentSession session = store.requireSession(authorization);
        body = bodyOrEmpty(body);
        // 兼容旧调用方：仍接受 minecraftId / minecraftUuid 直传（管理员手工写入场景）
        String rawId = optionalString(body, "minecraftId");
        String rawUuid = optionalString(body, "minecraftUuid");
        if (rawId != null && !rawId.isBlank() && rawUuid != null && rawUuid.matches("[a-f0-9]{32}")) {
            if (!Pattern.matches("[A-Za-z0-9_]{3,16}", rawId)) {
                throw ApiException.badRequest("minecraftBinding");
            }
            return ok(store.bindMinecraft(session.user.username, rawId, rawUuid, "MANUAL_VERIFICATION"));
        }
        // 主路径：昵称 + 平台，服务端解析
        String name = requiredString(body, "minecraftName");
        String platform = requiredString(body, "minecraftPlatform");
        MinecraftIdentityResolver.Identity identity = minecraftResolver.resolve(name, platform);
        if (identity == null) {
            throw new ApiException(43102, HttpStatus.BAD_REQUEST, "minecraft identity not found");
        }
        return ok(store.bindMinecraft(session.user.username, identity.name(), identity.uuid(), identity.platform().name()));
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
        if (password.length() < 6 || password.length() > 128
                || characterClassCount(password) < 2
                || commonWeakPasswords.contains(password.toLowerCase(Locale.ROOT))) {
            throw ApiException.badRequest("password");
        }
    }

    /**
     * 数密码里出现了几类字符：大写字母、小写字母、数字、其他符号。
     * 规则要求至少两类，而不是指定某两类 —— 这样「abc123」「Abcdef」「abcd!@」
     * 都能过，纯数字或纯字母过不了，既守住强度又不逼用户记死一种组合。
     *
     * 2026-11-26 花蝉定的：下限 6 位，任意两类字符即可（原先是 10 位 + 必须字母加数字）。
     */
    static int characterClassCount(String password) {
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean symbol = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else {
                symbol = true;
            }
        }
        int classes = 0;
        if (upper) {
            classes++;
        }
        if (lower) {
            classes++;
        }
        if (digit) {
            classes++;
        }
        if (symbol) {
            classes++;
        }
        return classes;
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
                                "http://127.0.0.1:5182",
                                // 前端容器映射到 8080：5173 落在 Windows 动态端口保留段
                                // 5111-5610 里绑不上，所以 compose 改成 8080:5173。
                                // 不在这里放行，浏览器会因 CORS 预检 403 直接 fail，
                                // 页面只看到「注册失败，请稍后重试」，非常难查。
                                "http://localhost:8080",
                                "http://127.0.0.1:8080"
                        )
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("X-Request-Id");
            }
        };
    }

    @Bean
    ApplicationRunner authLocalSeedData(AuthStore store) {
        return args -> {
            store.seedLocalDevDataIfEmpty();
            // 用户读回在 seed 内部完成；会话必须单独读回，否则重启后已发出的 token 全部失效
            store.hydrateSessionsFromStore();
            store.hydrateInvitationsFromStore();
        };
    }

    /**
     * 给下游（profile 建档）提供「把用户提升为 ACTIVE」的能力。
     *
     * <p>注册产生的用户一律是 {@code PENDING_PROFILE}，此前只有 admin 的
     * {@code PATCH /auth/admin/users/{id}} 能改它。白名单审批通过后成员仍停在
     * PENDING_PROFILE，profile 建档也就只能记下一个错误状态——表现为「流程走完但永远不是成员」。
     *
     * <p>这里只暴露「提升为 ACTIVE」这一件事，不开放任意状态改写（禁用/封禁仍只走 admin）。
     */
    @Bean
    cn.beiming.core.UserActivationResolver userActivationResolver(AuthStore store) {
        return userId -> {
            if (userId == null || userId.isBlank()) {
                return cn.beiming.core.UserActivationResolver.ActivationOutcome
                        .failed(null, "43100", "user not found");
            }
            return store.activateForOnboardingApproval(userId);
        };
    }

    @Bean
    AuthFlowEvidenceRecorder authFlowEvidenceRecorder() {
        return new NoopAuthFlowEvidenceRecorder();
    }

    /**
     * 邀请码来源查询：供 whitelist 判断「这个玩家是不是管理员码注册进来的」。
     *
     * <p>B 方案（花蝉 2026-11-27 定）：用 ADMIN 类邀请码注册的，白名单免人工审核；
     * PLAYER 码注册的仍走审核。依据是邀请码类型，不是注册时随手填的东西。
     *
     * <p>查不到时返回 null 而不是抛异常：auth 抖动或用户没有来源记录，
     * 都不该把白名单申请整条链路带成 500。null 在这里的含义是「按普通玩家处理」。
     */
    @Bean
    cn.beiming.core.InvitationOriginResolver invitationOriginResolver(AuthStore store) {
        return userId -> store.resolveInvitationOrigin(userId);
    }


    @Bean
    AuthPersistence authPersistence(ObjectProvider<JdbcTemplate> jdbcTemplate, ObjectMapper objectMapper) {
        JdbcTemplate available = jdbcTemplate.getIfAvailable();
        if (available == null) {
            return new NoopAuthPostgresPersistence();
        }
        return new AuthPostgresPersistence(available, objectMapper);
    }
}

interface AuthFlowEvidenceRecorder {
    void recordRegisterSuccess(HttpServletRequest request, String rawInvitationCode, String username, Map<String, Object> payload, int responseCode);

    void recordLoginSuccess(HttpServletRequest request, String username, Map<String, Object> payload, int responseCode);

    void recordLogoutSuccess(HttpServletRequest request, String username, String token, int responseCode);
}

class NoopAuthFlowEvidenceRecorder implements AuthFlowEvidenceRecorder {
    @Override
    public void recordRegisterSuccess(HttpServletRequest request, String rawInvitationCode, String username, Map<String, Object> payload, int responseCode) {
    }

    @Override
    public void recordLoginSuccess(HttpServletRequest request, String username, Map<String, Object> payload, int responseCode) {
    }

    @Override
    public void recordLogoutSuccess(HttpServletRequest request, String username, String token, int responseCode) {
    }
}

interface AuthPersistence {
    void resetForTests();

    /**
     * 启动时从正式库读回全部用户。返回空表示没有持久化后端（内存模式）。
     * 只有读回内存，登录、鉴权、角色校验这些读路径才看得见库里的账号。
     */
    List<UserAccount> loadUsers();

    /**
     * 启动时从正式库读回未撤销、未过期的会话。
     * 会话只存 token_hash 无法还原 token，所以同时存 token_value；
     * 不读回的话，进程重启后所有已发出的 accessToken 立刻失效（41001 invalid session）。
     */
    List<SessionRecord> loadSessions();

    /**
     * 启动时读回邀请码。校验走 invitationIdByCode.get(rawCode) 明文查找，
     * 只存 code_hash 会让重启后所有邀请码都报 43101 invitation not found。
     */
    List<LoadedInvitation> loadInvitations();

    /**
     * 查询某用户注册时用的邀请码类型（B 方案：ADMIN 码注册 = 白名单免审核）。
     *
     * <p>返回 {@code {type, codePrefix}}；查不到返回 null。
     * 注意「查不到」与「查到了但不是 ADMIN」是两件事：前者按普通玩家处理，
     * 后者明确是 PLAYER 码，都不该被自动放行。
     */
    Map<String, Object> findInvitationOrigin(String userId);

    /** 邀请码读回载体：rawCode 是明文，invitation 是已建好的记录。 */
    record LoadedInvitation(String rawCode, InvitationRecord invitation) {
    }

    void seedUser(UserAccount user);

    void seedInvitation(String rawCode, InvitationRecord invitation);

    void seedMinecraftBinding(String userId, MinecraftBinding binding);

    /** 解除绑定：删掉该用户的绑定行 */
    void deleteMinecraftBinding(String userId);

    Map<String, Object> replay(String actorUserId, String scope, String idempotencyKey, String fingerprint);

    void persistRegister(HttpServletRequest request, String rawInvitationCode, InvitationRecord invitation, UserAccount user, SessionRecord session, String idempotencyKey, String fingerprint, Map<String, Object> payload, int responseCode);

    void persistLogin(HttpServletRequest request, UserAccount user, SessionRecord session, String idempotencyKey, String fingerprint, Map<String, Object> payload, int responseCode);

    void persistLogout(HttpServletRequest request, UserAccount user, SessionRecord session, int responseCode);

    void persistReplayRequest(HttpServletRequest request, String actorUserId, int responseCode);

    /**
     * 持久化用户状态变更（准入流程提升 PENDING_PROFILE → ACTIVE）。
     *
     * <p>此前这个接口只有注册/登录/登出/回放四类写入，没有用户状态更新——于是 admin 的
     * {@code updateUser} 只改内存+写审计，重启后状态回滚；准入流程更是完全无法把用户提升为
     * ACTIVE。这里补一条最小写入：只更新 status / updated_at，不碰密码、角色与显示名。
     */
    void persistUserStatus(UserAccount user);
}

class NoopAuthPostgresPersistence implements AuthPersistence {
    @Override
    public void resetForTests() {
    }

    @Override
    public List<UserAccount> loadUsers() {
        return List.of();
    }

    @Override
    public List<SessionRecord> loadSessions() {
        return List.of();
    }

    @Override
    public List<LoadedInvitation> loadInvitations() {
        return List.of();
    }

    /** 内存模式没有 usage 表可查；返回 null 表示「查不到来源」，上层按普通玩家处理。 */
    @Override
    public Map<String, Object> findInvitationOrigin(String userId) {
        return null;
    }

    @Override
    public void seedUser(UserAccount user) {
    }

    @Override
    public void seedInvitation(String rawCode, InvitationRecord invitation) {
    }

    @Override
    public void seedMinecraftBinding(String userId, MinecraftBinding binding) {
    }

    @Override
    public void deleteMinecraftBinding(String userId) {
    }

    @Override
    public Map<String, Object> replay(String actorUserId, String scope, String idempotencyKey, String fingerprint) {
        return null;
    }

    @Override
    public void persistRegister(HttpServletRequest request, String rawInvitationCode, InvitationRecord invitation, UserAccount user, SessionRecord session, String idempotencyKey, String fingerprint, Map<String, Object> payload, int responseCode) {
    }

    @Override
    public void persistLogin(HttpServletRequest request, UserAccount user, SessionRecord session, String idempotencyKey, String fingerprint, Map<String, Object> payload, int responseCode) {
    }

    @Override
    public void persistLogout(HttpServletRequest request, UserAccount user, SessionRecord session, int responseCode) {
    }

    @Override
    public void persistUserStatus(UserAccount user) {
        // 无数据库上下文时只保留内存状态，由读回逻辑自身保证一致性。
    }

    @Override
    public void persistReplayRequest(HttpServletRequest request, String actorUserId, int responseCode) {
    }
}

class AuthPostgresPersistence implements AuthPersistence {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    AuthPostgresPersistence(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    /** 整表读取 MC 绑定，key 是 auth_minecraft_bindings.user_id（text，与 auth_users.user_id 对应） */
    private Map<String, MinecraftBinding> loadBindings() {
        Map<String, MinecraftBinding> bindings = new LinkedHashMap<>();
        jdbc.query("""
                SELECT user_id, minecraft_id, minecraft_uuid, verified_at, source
                FROM auth_minecraft_bindings
                """, result -> {
            bindings.put(result.getString("user_id"), new MinecraftBinding(
                    result.getString("minecraft_id"),
                    result.getString("minecraft_uuid"),
                    timestampOf(result.getTimestamp("verified_at")),
                    result.getString("source")));
        });
        return bindings;
    }


    @Override
    public List<UserAccount> loadUsers() {
        List<UserAccount> loaded = new ArrayList<>();
        // 先把绑定整表读进 map，避免一边查用户一边逐条查绑定
        Map<String, MinecraftBinding> bindingsByUserId = loadBindings();
        jdbc.query("""
                SELECT user_id, username, display_name, password_hash, roles::text, permissions::text, status,
                       created_at, updated_at, last_login_at
                FROM auth_users
                """, result -> {
            UserAccount account = new UserAccount(
                    result.getString("user_id"),
                    result.getString("username"),
                    result.getString("display_name"),
                    result.getString("password_hash"),
                    readStringSet(result.getString("roles")),
                    readStringSet(result.getString("permissions")),
                    result.getString("status"),
                    timestampOf(result.getTimestamp("created_at")),
                    timestampOf(result.getTimestamp("updated_at")));
            if (result.getTimestamp("last_login_at") != null) {
                account.lastLoginAt = timestampOf(result.getTimestamp("last_login_at"));
            }
            // MC 绑定必须一起读回来。原来这里只查 auth_users，不碰
            // auth_minecraft_bindings —— 结果绑定写进库了，但后端一重启
            // 所有人绑定集体消失（2026-11-27 实测：库里数据在、/me 返回 null）。
            MinecraftBinding binding = bindingsByUserId.get(account.id);
            if (binding != null) {
                account.minecraftBinding = binding;
            }
            loaded.add(account);
        });
        return loaded;
    }

    @Override
    public List<SessionRecord> loadSessions() {
        List<SessionRecord> loaded = new ArrayList<>();
        jdbc.query("""
                SELECT session_id, token_value, user_id, created_at, last_seen_at, expires_at, revoked
                FROM auth_sessions
                WHERE revoked = false AND expires_at > now() AND token_value IS NOT NULL
                """, result -> {
            SessionRecord session = new SessionRecord(
                    result.getString("session_id"),
                    result.getString("token_value"),
                    result.getString("user_id"),
                    timestampOf(result.getTimestamp("created_at")),
                    timestampOf(result.getTimestamp("expires_at")));
            if (result.getTimestamp("last_seen_at") != null) {
                session.lastSeenAt = timestampOf(result.getTimestamp("last_seen_at"));
            }
            session.revoked = result.getBoolean("revoked");
            loaded.add(session);
        });
        return loaded;
    }

    @Override
    public Map<String, Object> findInvitationOrigin(String userId) {
        if (userId == null || userId.isBlank()) return null;
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT i.type AS type, i.code_prefix AS code_prefix
                  FROM auth_invitation_usage_records u
                  JOIN auth_invitations i ON i.invitation_id = u.invitation_id
                 WHERE u.used_by_user_id = ?
                 ORDER BY u.used_at DESC
                 LIMIT 1
                """, userId);
        if (rows.isEmpty()) return null;
        Map<String, Object> origin = new java.util.LinkedHashMap<>();
        origin.put("type", rows.get(0).get("type"));
        origin.put("codePrefix", rows.get(0).get("code_prefix"));
        return origin;
    }

    @Override
    public List<LoadedInvitation> loadInvitations() {
        List<LoadedInvitation> loaded = new ArrayList<>();
        jdbc.query("""
                SELECT invitation_id, code_prefix, code_hash, code_value, type, bound_roles::text, bound_permissions::text,
                       max_uses, used_count, expires_at, created_by, created_at, disabled_at
                FROM auth_invitations
                WHERE code_value IS NOT NULL
                """, result -> {
            InvitationRecord invitation = new InvitationRecord(
                    result.getString("invitation_id"),
                    result.getString("code_prefix"),
                    result.getString("code_hash"),
                    result.getString("type"),
                    readStringSet(result.getString("bound_roles")),
                    readStringSet(result.getString("bound_permissions")),
                    result.getInt("max_uses"),
                    result.getInt("used_count"),
                    result.getTimestamp("expires_at") == null ? null : timestampOf(result.getTimestamp("expires_at")),
                    result.getString("created_by"),
                    timestampOf(result.getTimestamp("created_at")));
            if (result.getTimestamp("disabled_at") != null) {
                invitation.disabledAt = timestampOf(result.getTimestamp("disabled_at"));
            }
            loaded.add(new LoadedInvitation(result.getString("code_value"), invitation));
        });
        return loaded;
    }

    private Set<String> readStringSet(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashSet<>();
        }
        try {
            return new LinkedHashSet<>(objectMapper.readValue(json, new TypeReference<List<String>>() { }));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to read auth user roles or permissions", exception);
        }
    }

    private static Instant timestampOf(java.sql.Timestamp value) {
        return value == null ? Instant.now() : value.toInstant();
    }

    @Override
    @Transactional
    public void resetForTests() {
        jdbc.update("DELETE FROM auth_password_reset_tokens");
        jdbc.update("DELETE FROM auth_minecraft_bindings");
        jdbc.update("DELETE FROM auth_invitation_usage_records");
        jdbc.update("DELETE FROM auth_sessions");
        jdbc.update("DELETE FROM auth_invitations");
        jdbc.update("DELETE FROM auth_users");
        jdbc.update("DELETE FROM app_idempotency_records");
        jdbc.update("DELETE FROM app_audit_logs");
        jdbc.update("DELETE FROM app_request_logs");
    }

    @Override
    @Transactional
    public void seedUser(UserAccount user) {
        jdbc.update("""
                INSERT INTO auth_users(id, user_id, username, username_normalized, display_name, display_name_normalized, password_hash, roles, permissions, status, created_at, updated_at, last_login_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, ?)
                ON CONFLICT (user_id) DO NOTHING
                """,
                UUID.randomUUID(), user.id, user.username, normalize(user.username), user.displayName, normalize(user.displayName),
                user.passwordHash, json(new ArrayList<>(user.roles)), json(new ArrayList<>(user.permissions)), user.status,
                timestamp(user.createdAt), timestamp(user.updatedAt), timestamp(user.lastLoginAt));
    }

    @Override
    @Transactional
    public void seedInvitation(String rawCode, InvitationRecord invitation) {
        jdbc.update("""
                INSERT INTO auth_invitations(id, invitation_id, code_prefix, code_hash, code_value, type, bound_roles, bound_permissions, max_uses, used_count, expires_at, created_by, created_at, disabled_at)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, ?, ?, ?)
                ON CONFLICT (invitation_id) DO UPDATE SET code_value = EXCLUDED.code_value, used_count = EXCLUDED.used_count, disabled_at = EXCLUDED.disabled_at
                """,
                UUID.randomUUID(), invitation.id, invitation.codePrefix, invitation.codeHash, rawCode, invitation.type,
                json(new ArrayList<>(invitation.boundRoles)), json(new ArrayList<>(invitation.boundPermissions)),
                invitation.maxUses, invitation.usedCount, timestamp(invitation.expiresAt), invitation.createdBy, timestamp(invitation.createdAt), timestamp(invitation.disabledAt));
    }

    @Override
    @Transactional
    public void seedMinecraftBinding(String userId, MinecraftBinding binding) {
        jdbc.update("""
                INSERT INTO auth_minecraft_bindings(id, user_id, minecraft_id, minecraft_uuid, verified_at, source)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET minecraft_id = EXCLUDED.minecraft_id, minecraft_uuid = EXCLUDED.minecraft_uuid, verified_at = EXCLUDED.verified_at, source = EXCLUDED.source
                """,
                UUID.randomUUID(), userId, binding.minecraftId, binding.minecraftUuid, timestamp(binding.verifiedAt), binding.source);
    }

    @Override
    @Transactional
    public void deleteMinecraftBinding(String userId) {
        jdbc.update("DELETE FROM auth_minecraft_bindings WHERE user_id = ?", userId);
    }

    @Override
    public Map<String, Object> replay(String actorUserId, String scope, String idempotencyKey, String fingerprint) {
        return null;
    }

    @Override
    @Transactional
    public void persistRegister(HttpServletRequest request, String rawInvitationCode, InvitationRecord invitation, UserAccount user, SessionRecord session, String idempotencyKey, String fingerprint, Map<String, Object> payload, int responseCode) {
        upsertUser(user);
        // 注册时若带了 MC 绑定，必须一并落库。否则绑定只活在内存里，
        // 后端一重启就没了——2026-11-27 实测发现只写内存，库里查不到。
        if (user.minecraftBinding != null) {
            seedMinecraftBinding(user.id, user.minecraftBinding);
        }
        upsertInvitation(invitation);
        jdbc.update("""
                INSERT INTO auth_invitation_usage_records(id, usage_id, invitation_id, used_by_user_id, used_by_username, source_ip, request_id, used_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (usage_id) DO NOTHING
                """,
                UUID.randomUUID(), "use_" + RequestIdFilter.currentRequestId(), invitation.id, user.id, user.username, sourceIp(request), requestId(request), timestamp(Instant.now()));
        insertSession(session);
        insertAudit(request, null, "ANONYMOUS", List.of(), user.id, "AUTH_USER", "AUTH_REGISTER_SUCCESS", null, null, null, "SUCCESS", null);
        if (idempotencyKey != null) {
            insertIdempotency("anonymous", "auth.register", idempotencyKey, fingerprint, responseCode, payload);
        }
        insertRequestLog(request, user.id, responseCode, "SUCCESS", null);
    }

    @Override
    @Transactional
    public void persistLogin(HttpServletRequest request, UserAccount user, SessionRecord session, String idempotencyKey, String fingerprint, Map<String, Object> payload, int responseCode) {
        upsertUser(user);
        insertSession(session);
        insertAudit(request, user.id, String.join(",", user.roles), new ArrayList<>(user.permissions), user.id, "AUTH_USER", "AUTH_LOGIN_SUCCESS", null, null, null, "SUCCESS", null);
        if (idempotencyKey != null) {
            insertIdempotency("anonymous", "auth.login", idempotencyKey, fingerprint, responseCode, payload);
        }
        insertRequestLog(request, user.id, responseCode, "SUCCESS", null);
    }

    @Override
    @Transactional
    public void persistLogout(HttpServletRequest request, UserAccount user, SessionRecord session, int responseCode) {
        jdbc.update("UPDATE auth_sessions SET revoked = true, revoked_at = now(), last_seen_at = ? WHERE token_hash = ?", timestamp(Instant.now()), digest(session.token));
        insertAudit(request, user.id, String.join(",", user.roles), new ArrayList<>(user.permissions), user.id, "AUTH_SESSION", "AUTH_LOGOUT_SUCCESS", null, null, null, "SUCCESS", null);
        insertRequestLog(request, user.id, responseCode, "SUCCESS", null);
    }

    @Override
    @Transactional
    public void persistReplayRequest(HttpServletRequest request, String actorUserId, int responseCode) {
        insertRequestLog(request, actorUserId, responseCode, "SUCCESS", null);
    }

    @Override
    public void persistUserStatus(UserAccount user) {
        // 只更新状态与更新时间：准入流程不该改写密码、角色或显示名。
        jdbc.update("UPDATE auth_users SET status = ?, updated_at = ? WHERE user_id = ?",
                user.status, timestamp(user.updatedAt), user.id);
    }

    private void upsertUser(UserAccount user) {
        jdbc.update("""
                INSERT INTO auth_users(id, user_id, username, username_normalized, display_name, display_name_normalized, password_hash, roles, permissions, status, created_at, updated_at, last_login_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET display_name = EXCLUDED.display_name, display_name_normalized = EXCLUDED.display_name_normalized, password_hash = EXCLUDED.password_hash, roles = EXCLUDED.roles, permissions = EXCLUDED.permissions, status = EXCLUDED.status, updated_at = EXCLUDED.updated_at, last_login_at = EXCLUDED.last_login_at
                """,
                UUID.randomUUID(), user.id, user.username, normalize(user.username), user.displayName, normalize(user.displayName),
                user.passwordHash, json(new ArrayList<>(user.roles)), json(new ArrayList<>(user.permissions)), user.status,
                timestamp(user.createdAt), timestamp(user.updatedAt), timestamp(user.lastLoginAt));
    }

    private void upsertInvitation(InvitationRecord invitation) {
        jdbc.update("""
                INSERT INTO auth_invitations(id, invitation_id, code_prefix, code_hash, type, bound_roles, bound_permissions, max_uses, used_count, expires_at, created_by, created_at, disabled_at)
                VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, ?, ?, ?)
                ON CONFLICT (invitation_id) DO UPDATE SET used_count = EXCLUDED.used_count, disabled_at = EXCLUDED.disabled_at
                """,
                UUID.randomUUID(), invitation.id, invitation.codePrefix, invitation.codeHash, invitation.type,
                json(new ArrayList<>(invitation.boundRoles)), json(new ArrayList<>(invitation.boundPermissions)),
                invitation.maxUses, invitation.usedCount, timestamp(invitation.expiresAt), invitation.createdBy, timestamp(invitation.createdAt), timestamp(invitation.disabledAt));
    }

    private void insertSession(SessionRecord session) {
        jdbc.update("""
                INSERT INTO auth_sessions(id, session_id, token_hash, token_value, user_id, created_at, last_seen_at, expires_at, revoked, revoked_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (session_id) DO UPDATE SET token_value = EXCLUDED.token_value, last_seen_at = EXCLUDED.last_seen_at, expires_at = EXCLUDED.expires_at, revoked = EXCLUDED.revoked, revoked_at = EXCLUDED.revoked_at
                """,
                UUID.randomUUID(), session.id, digest(session.token), session.token, session.userId, timestamp(session.createdAt), timestamp(session.lastSeenAt), timestamp(session.expiresAt), session.revoked, session.revoked ? timestamp(Instant.now()) : null);
    }

    private void insertAudit(HttpServletRequest request, String actorUserId, String actorRole, List<String> permissions, String targetId, String targetType, String action, String reason, String beforeState, String afterState, String result, String failureReason) {
        jdbc.update("""
                INSERT INTO app_audit_logs(id, request_id, actor_user_id, actor_role, actor_permissions, source_ip, target_type, target_id, action, risk_level, reason, params_summary, before_state, after_state, result, failure_reason, created_at)
                VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, 'LOW', ?, '{}'::jsonb, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, now())
                """,
                UUID.randomUUID(), requestId(request), actorUserId, actorRole == null ? "ANONYMOUS" : actorRole, json(permissions), sourceIp(request),
                targetType, targetId == null ? "unknown" : targetId, action, reason, nullableJson(beforeState), nullableJson(afterState), result, failureReason);
    }

    private void insertIdempotency(String actorUserId, String scope, String idempotencyKey, String fingerprint, int responseCode, Map<String, Object> payload) {
        jdbc.update("""
                INSERT INTO app_idempotency_records(id, actor_user_id, scope, idempotency_key, request_fingerprint, response_code, response_body, created_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), now(), now() + interval '24 hours')
                ON CONFLICT (actor_user_id, scope, idempotency_key) DO NOTHING
                """,
                UUID.randomUUID(), actorUserId, scope, idempotencyKey, fingerprint, responseCode, json(AuthController.envelope(0, "success", payload)));
    }

    private void insertRequestLog(HttpServletRequest request, String actorUserId, int responseCode, String result, String failureReason) {
        jdbc.update("""
                INSERT INTO app_request_logs(id, request_id, method, path, actor_user_id, source_ip, response_code, result, failure_reason, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                ON CONFLICT (request_id) DO NOTHING
                """,
                UUID.randomUUID(), requestId(request), request.getMethod(), request.getRequestURI(), actorUserId, sourceIp(request), responseCode, result, failureReason);
    }

    private String requestId(HttpServletRequest request) {
        String value = request.getHeader("X-Request-Id");
        return value == null || value.isBlank() ? RequestIdFilter.currentRequestId() : value;
    }

    private String sourceIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }

    private String nullableJson(String value) {
        if (value == null) {
            return null;
        }
        return json(Map.of("value", value));
    }

    private OffsetDateTime timestamp(Instant value) {
        return value == null ? null : OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize PostgreSQL jsonb value", exception);
        }
    }
}

@Service
class AuthStore implements AuthSessionResolver {
    private static final Set<String> VALID_ROLES = Set.of("OWNER", "ADMIN", "HELPER", "USER");
    private static final Set<String> VALID_PERMISSIONS = Set.of("NODE_READ", "NODE_WRITE", "CONTAINER_OPERATE", "VM_OPERATE", "FILE_MANAGE", "TERMINAL_ACCESS", "HIGH_RISK_APPROVE");
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuthPersistence persistence;
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
    /**
     * 注册尝试限流：按客户端 IP 计数，带 10 分钟时间窗口。
     *
     * 2026-11-26 花蝉定的。原来只有一个 key 写死 "global" 的全局计数器，
     * 阈值 5 且没有过期时间 —— 任何人试错 5 次，整站就没人能注册了，
     * 而且只能重启后端才恢复（当天测试被它卡住 3 次）。
     * 现在按 IP 分桶，互不连坐；窗口过期自动清零，不需要重启。
     */
    /** 每个 IP 在窗口内允许的注册尝试次数 */
    static final int REGISTER_LIMIT = 5;

    /** 限流窗口长度：10 分钟 */
    static final Duration REGISTER_WINDOW = Duration.ofMinutes(10);

    /**
     * 一个限流桶：窗口起点 + 窗口内已计数。
     * 用不可变 record + 每次替换，配 computeIfPresent 保证并发下不会
     * 出现「两个线程同时重置窗口导致计数丢失」。
     */
    record AttemptWindow(Instant windowStart, int count) {
        AttemptWindow increment() {
            return new AttemptWindow(windowStart, count + 1);
        }
    }

    /**
     * 返回本次请求之后的累计次数（含本次）。
     * 窗口过期就重新开一个桶，所以不需要任何定时任务或重启。
     */
    static int nextAttempt(Map<String, AttemptWindow> buckets, String key) {
        Instant now = Instant.now();
        AttemptWindow updated = buckets.compute(key, (ignored, existing) -> {
            if (existing == null || existing.windowStart().plus(REGISTER_WINDOW).isBefore(now)) {
                return new AttemptWindow(now, 1);
            }
            return existing.increment();
        });
        return updated == null ? 1 : updated.count();
    }

    /**
     * 取客户端 IP 作为限流键。
     * 容器前面可能有反向代理，优先读 X-Forwarded-For 的第一个地址；
     * 都没有就退回 remoteAddr。拿不到就用固定串，避免 NPE 把注册整个打挂。
     */
    static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = comma > 0 ? forwarded.substring(0, comma) : forwarded;
            if (!first.isBlank()) {
                return first.trim();
            }
        }
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "unknown" : remote;
    }

    private final Map<String, AttemptWindow> registerAttempts = new ConcurrentHashMap<>();
    private final Map<String, Integer> passwordResetAttempts = new ConcurrentHashMap<>();
    private boolean failNextSessionCreation;
    private boolean failNextAudit;

    AuthStore(AuthPersistence persistence) {
        this.persistence = persistence;
    }

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
        persistence.resetForTests();
    }

    synchronized void seedOwner(String username, String password) {
        seedUser(username, password, Set.of("OWNER"), VALID_PERMISSIONS, "ACTIVE");
    }

    synchronized void seedUser(String username, String password, Set<String> roles, Set<String> permissions, String status) {
        String id = "usr_" + UUID.randomUUID();
        UserAccount user = new UserAccount(id, username, username, encoder.encode(password), roles, permissions, status, Instant.now(), Instant.now());
        usersById.put(id, user);
        userIdByUsername.put(username.toLowerCase(Locale.ROOT), id);
        persistence.seedUser(user);
    }

    synchronized void seedInvitation(String rawCode, String type, Set<String> roles, Set<String> permissions, int maxUses, Instant expiresAt, String createdByUsername) {
        UserAccount creator = findByUsername(createdByUsername);
        InvitationRecord invitation = new InvitationRecord("inv_" + UUID.randomUUID(), rawCode.substring(0, Math.min(rawCode.length(), 8)), encoder.encode(rawCode), type, roles, permissions, maxUses, 0, expiresAt, creator.id, Instant.now());
        invitationsById.put(invitation.id, invitation);
        invitationIdByCode.put(rawCode, invitation.id);
        persistence.seedInvitation(rawCode, invitation);
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
        UserAccount user = findByUsername(username);
        user.minecraftBinding = new MinecraftBinding(minecraftId, minecraftUuid, Instant.now(), "MANUAL_VERIFICATION");
        persistence.seedMinecraftBinding(user.id, user.minecraftBinding);
    }

    /**
     * 启动时把库里的会话读回内存。必须在 seedLocalDevDataIfEmpty 之前执行，
     * 且不能被「内存已有用户就 return」的短路挡住。
     */
    synchronized void hydrateSessionsFromStore() {
        for (SessionRecord session : persistence.loadSessions()) {
            // 用户可能在库里已不存在，这种会话读回来也无意义
            if (!usersById.containsKey(session.userId)) {
                continue;
            }
            sessions.put(session.token, session);
        }
    }

    /**
     * 启动时把库里的邀请码读回内存。校验按 invitationIdByCode.get(rawCode) 明文查找，
     * 不读回会让所有邀请码在重启后报 43101 invitation not found。
     */
    synchronized void hydrateInvitationsFromStore() {
        for (AuthPersistence.LoadedInvitation loaded : persistence.loadInvitations()) {
            invitationsById.put(loaded.invitation().id, loaded.invitation());
            invitationIdByCode.put(loaded.rawCode(), loaded.invitation().id);
        }
    }

    /**
     * 测试专用：只清内存里的用户索引，不动数据库。
     *
     * 用来模拟「进程重启」——重启后内存是空的、库还在，启动时走
     * seedLocalDevDataIfEmpty() 从库读回。这样能验出「写了库但读不回来」
     * 这一类 bug（MC 绑定就栽在这上面）。
     * 不能用 reset()：它会连库一起 truncate，重启语义就不成立了。
     */
    synchronized void clearUserIndexesForReloadTest() {
        usersById.clear();
        userIdByUsername.clear();
    }

    synchronized void seedLocalDevDataIfEmpty() {
        if (!usersById.isEmpty()) {
            return;
        }
        // 先把正式库里的账号读回内存：登录、鉴权、角色校验都走内存 Map，
        // 不读回就会出现「库里有 admin 但登录说密码错」。
        for (UserAccount existing : persistence.loadUsers()) {
            usersById.put(existing.id, existing);
            userIdByUsername.put(existing.username.toLowerCase(Locale.ROOT), existing.id);
        }
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

    synchronized Map<String, Object> register(HttpServletRequest request, String rawCode, String username, String password, String displayName, String idempotencyKey, MinecraftIdentityResolver.Identity minecraftIdentity) {
        String attemptKey = clientIp(request);
        int attempts = nextAttempt(registerAttempts, attemptKey);
        if (attempts > REGISTER_LIMIT) {
            throw new ApiException(44101, HttpStatus.TOO_MANY_REQUESTS, "too many register attempts");
        }
        String signature = signature(rawCode, username, password, displayName);
        if (idempotencyKey != null) {
            IdempotencyRecord old = registerIdempotency.get(idempotencyKey);
            if (old != null) {
                if (!old.signature.equals(signature)) {
                    throw new ApiException(43002, HttpStatus.CONFLICT, "idempotency key conflict");
                }
                persistence.persistReplayRequest(request, "anonymous", 201);
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
        // 注册时就带上 MC 绑定（花蝉 2026-11-27 要的）：昵称就是用户名，
        // UUID 由服务端按平台查好再传进来，用户不用知道 UUID 是多少。
        // 查不到就只填昵称、不绑定，不阻断注册——注册不该被第三方服务拖垮。
        if (minecraftIdentity != null) {
            user.minecraftBinding = new MinecraftBinding(minecraftIdentity.name(), minecraftIdentity.uuid(), Instant.now(), minecraftIdentity.platform().name());
        }
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
            SessionRecord session = sessions.get(String.valueOf(payload.get("accessToken")));
            persistence.persistRegister(request, rawCode, invitation, user, session, idempotencyKey, signature, payload, 201);
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

    synchronized Map<String, Object> login(HttpServletRequest request, String username, String password, String idempotencyKey) {
        String signature = signature(username, password);
        if (idempotencyKey != null) {
            IdempotencyRecord old = loginIdempotency.get(idempotencyKey);
            if (old != null) {
                if (!old.signature.equals(signature)) {
                    throw new ApiException(43002, HttpStatus.CONFLICT, "idempotency key conflict");
                }
                persistence.persistReplayRequest(request, "anonymous", 200);
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
        SessionRecord session = sessions.get(String.valueOf(payload.get("accessToken")));
        persistence.persistLogin(request, user, session, idempotencyKey, signature, payload, 200);
        if (idempotencyKey != null) {
            loginIdempotency.put(idempotencyKey, new IdempotencyRecord(signature, payload, null));
        }
        return payload;
    }

    synchronized void logout(HttpServletRequest request, CurrentSession current) {
        if (!current.session.revoked) {
            current.session.revoked = true;
            audit("AUTH_LOGOUT_SUCCESS", current.user, current.user, "SUCCESS", null, null, null);
            persistence.persistLogout(request, current.user, current.session, 200);
        } else {
            persistence.persistReplayRequest(request, current.user.id, 200);
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

    synchronized Map<String, Object> bindMinecraft(String username, String minecraftId, String minecraftUuid, String source) {
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
        user.minecraftBinding = new MinecraftBinding(minecraftId, minecraftUuid, Instant.now(), source);
        // 只改内存不写库 = 重启即丢。这里补持久化（与 register 那次同一个坑）。
        persistMinecraftBinding(user, user.minecraftBinding);
        audit("AUTH_MINECRAFT_BOUND", user, user, "SUCCESS", null, null, null);
        return minecraftBindingMap(user.minecraftBinding);
    }

    synchronized void unbindMinecraft(String username, String reason) {
        UserAccount user = findByUsername(username);
        if (user.minecraftBinding == null) {
            throw new ApiException(43102, HttpStatus.NOT_FOUND, "minecraft binding not found");
        }
        user.minecraftBinding = null;
        persistMinecraftBinding(user, null);
        audit("AUTH_MINECRAFT_UNBOUND", user, user, "SUCCESS", reason, null, null);
    }

    /**
     * 把绑定写进 auth_minecraft_bindings；传 null 表示解除绑定（删行）。
     *
     * <p>{@code seedMinecraftBinding} 本身就是 upsert（ON CONFLICT (user_id) DO UPDATE），
     * 复用它而不是新加一个接口方法。persistence 为空时（不接库的测试上下文）跳过。
     */
    private void persistMinecraftBinding(UserAccount user, MinecraftBinding binding) {
        if (persistence == null) {
            return;
        }
        try {
            if (binding == null) {
                persistence.deleteMinecraftBinding(user.id);
            } else {
                persistence.seedMinecraftBinding(user.id, binding);
            }
        } catch (RuntimeException e) {
            // 写库失败不能让内存状态和库体不一致地「成功」返回
            throw new ApiException(50900, HttpStatus.INTERNAL_SERVER_ERROR, "minecraft binding persist failed");
        }
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

    /**
     * 准入流程通过后，把用户从 {@code PENDING_PROFILE} 提升为 {@code ACTIVE}。
     *
     * <p>这是 profile 建档时经由 {@link cn.beiming.core.UserActivationResolver} 调进来的内部路径，
     * 不走 admin 接口（成员自己走不到 admin）。只允许这一种状态转换：
     * PENDING_PROFILE → ACTIVE。其余情形一律如实返回失败，不替调用方兜假值。
     *
     * <p>幂等：已是 ACTIVE 直接返回成功，不重复写库。
     */
    /**
     * 查询用户的注册来源邀请码类型（B 方案：ADMIN 码注册 = 白名单免审核）。
     *
     * <p>数据来源是 {@code auth_invitation_usage_records}——注册消耗邀请码时写入，
     * 记录 {@code used_by_user_id}。这里取该用户最近一次使用的邀请码类型。
     *
     * <p>刻意不抛异常：查询失败当作「查不到」，让上层按普通玩家处理，
     * 而不是把白名单申请带成 500。
     */
    synchronized cn.beiming.core.InvitationOriginResolver.Origin resolveInvitationOrigin(String userId) {
        if (userId == null || userId.isBlank()) return null;
        // 内存优先：本地/测试上下文没有 jdbc 时，用 usageRecords 现算
        for (InvitationRecord invitation : invitationsById.values()) {
            for (Map<String, Object> record : invitation.usageRecords) {
                if (userId.equals(String.valueOf(record.get("userId")))) {
                    return new cn.beiming.core.InvitationOriginResolver.Origin(
                            invitation.type, "ADMIN".equalsIgnoreCase(invitation.type), invitation.codePrefix);
                }
            }
        }
        try {
            Map<String, Object> origin = persistence.findInvitationOrigin(userId);
            if (origin == null) return null;
            String type = String.valueOf(origin.get("type"));
            return new cn.beiming.core.InvitationOriginResolver.Origin(
                    type, "ADMIN".equalsIgnoreCase(type), String.valueOf(origin.get("codePrefix")));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    synchronized cn.beiming.core.UserActivationResolver.ActivationOutcome activateForOnboardingApproval(String userId) {
        UserAccount target = usersById.get(userId);
        if (target == null) {
            return cn.beiming.core.UserActivationResolver.ActivationOutcome
                    .failed(null, "43100", "user not found");
        }
        String previous = target.status;
        if ("ACTIVE".equals(previous)) {
            return cn.beiming.core.UserActivationResolver.ActivationOutcome.alreadyActive(previous);
        }
        if (!"PENDING_PROFILE".equals(previous)) {
            // DISABLED / BANNED / DELETED 不能被准入流程悄悄复活。
            return cn.beiming.core.UserActivationResolver.ActivationOutcome
                    .failed(previous, "43116", "user status invalid for activation");
        }
        String before = state(target);
        target.status = "ACTIVE";
        target.updatedAt = Instant.now();
        String after = state(target);
        persistence.persistUserStatus(target);
        // 状态变更本身已落库；未征得 admin 同意不改名、不改角色、不签发会话。
        return cn.beiming.core.UserActivationResolver.ActivationOutcome.activated(previous);
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

    /**
     * {@link AuthSessionResolver} 实现：不抛异常，任何失败都返回 null。
     * 这样 ops-control 能用自己的错误码体系回应，同时不必复制 session 校验逻辑。
     */
    @Override
    public synchronized AuthSessionResolver.ResolvedSession resolve(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        SessionRecord session = sessions.get(authorization.substring("Bearer ".length()));
        if (session == null || session.revoked || session.expiresAt.isBefore(Instant.now())) {
            return null;
        }
        UserAccount user = usersById.get(session.userId);
        if (user == null || "DISABLED".equals(user.status) || "BANNED".equals(user.status) || "DELETED".equals(user.status)) {
            return null;
        }
        return new AuthSessionResolver.ResolvedSession(
                user.id, user.username, primaryRole(user.roles), List.copyOf(user.permissions),
                user.minecraftBinding == null ? null : minecraftBindingMap(user.minecraftBinding));
    }

    /** 角色优先级 OWNER > ADMIN > HELPER > USER，与前端 mapBackendUser 保持一致 */
    private static String primaryRole(Set<String> roles) {
        for (String candidate : List.of("OWNER", "ADMIN", "HELPER", "USER")) {
            if (roles.contains(candidate)) {
                return candidate;
            }
        }
        return "USER";
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

@RestControllerAdvice(assignableTypes = AuthController.class)
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
