package cn.beiming.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController {
    private final NotificationStore store;
    private final TestAuthContextProvider auth;

    NotificationController(NotificationStore store, TestAuthContextProvider auth) {
        this.store = store;
        this.auth = auth;
    }

    @GetMapping("/me")
    ResponseEntity<Map<String, Object>> currentUserMessages(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "20") int pageSize,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(required = false) String type,
                                                            @RequestParam(required = false) String sourceModule,
                                                            @RequestParam(defaultValue = "false") boolean includeExpired,
                                                            @RequestParam(required = false) String sort) {
        AuthUser current = auth.requireCurrent(authorization);
        return ok(store.currentUserMessages(current.userId, page, pageSize, status, type, sourceModule, includeExpired, sort));
    }

    @GetMapping("/me/unread-count")
    ResponseEntity<Map<String, Object>> unreadCount(@RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthUser current = auth.requireCurrent(authorization);
        return ok(mapOf("unreadCount", store.unreadCount(current.userId)));
    }

    @GetMapping("/me/{notificationId}")
    ResponseEntity<Map<String, Object>> currentUserMessage(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                           @PathVariable String notificationId) {
        AuthUser current = auth.requireCurrent(authorization);
        return ok(store.currentUserMessage(current.userId, notificationId));
    }

    @PatchMapping("/me/{notificationId}/read")
    ResponseEntity<Map<String, Object>> markRead(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                 @PathVariable String notificationId,
                                                 @RequestBody(required = false) Map<String, Object> body) {
        AuthUser current = auth.requireCurrent(authorization);
        return ok(store.markRead(current.userId, notificationId));
    }

    @PatchMapping("/me/read-all")
    ResponseEntity<Map<String, Object>> markAllRead(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                    @RequestBody(required = false) Map<String, Object> body) {
        AuthUser current = auth.requireCurrent(authorization);
        body = bodyOrEmpty(body);
        return ok(mapOf("updatedCount", store.markAllRead(current.userId, optionalString(body, "type"), optionalString(body, "sourceModule"))));
    }

    @PatchMapping("/me/{notificationId}/archive")
    ResponseEntity<Map<String, Object>> archive(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                @PathVariable String notificationId,
                                                @RequestBody(required = false) Map<String, Object> body) {
        AuthUser current = auth.requireCurrent(authorization);
        body = bodyOrEmpty(body);
        String reason = optionalString(body, "reason");
        if (reason != null && reason.length() > 200) {
            throw ApiException.badRequest("reason");
        }
        return ok(store.archive(current, notificationId, reason));
    }

    @GetMapping("/admin/messages")
    ResponseEntity<Map<String, Object>> adminMessages(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int pageSize,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String type,
                                                      @RequestParam(required = false) String sourceModule,
                                                      @RequestParam(required = false) String recipientUserId,
                                                      @RequestParam(required = false) String deliveryStatus,
                                                      @RequestParam(required = false) String createdBy,
                                                      @RequestParam(required = false) String sort) {
        AuthUser current = auth.requireCurrent(authorization);
        requireReader(current);
        return ok(store.adminMessages(page, pageSize, keyword, type, sourceModule, recipientUserId, deliveryStatus, createdBy, sort));
    }

    @GetMapping("/admin/messages/{notificationId}")
    ResponseEntity<Map<String, Object>> adminMessage(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                     @PathVariable String notificationId) {
        AuthUser current = auth.requireCurrent(authorization);
        requireReader(current);
        return ok(store.adminMessage(notificationId));
    }

    @PostMapping("/admin/messages")
    ResponseEntity<Map<String, Object>> createMessage(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                      @RequestBody(required = false) Map<String, Object> body) {
        AuthUser current = auth.requireCurrent(authorization);
        requireWriter(current);
        return created(store.createMessage(current, auth, bodyOrEmpty(body), false));
    }

    @PostMapping("/admin/messages/from-template")
    ResponseEntity<Map<String, Object>> createFromTemplate(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                           @RequestBody(required = false) Map<String, Object> body) {
        AuthUser current = auth.requireCurrent(authorization);
        requireWriter(current);
        return created(store.createFromTemplate(current, auth, bodyOrEmpty(body)));
    }

    @GetMapping("/admin/templates")
    ResponseEntity<Map<String, Object>> templates(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String type,
                                                  @RequestParam(required = false) String sort) {
        AuthUser current = auth.requireCurrent(authorization);
        requireReader(current);
        return ok(store.templates(page, pageSize, keyword, status, type, sort));
    }

    @GetMapping("/admin/templates/{templateId}")
    ResponseEntity<Map<String, Object>> template(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                 @PathVariable String templateId) {
        AuthUser current = auth.requireCurrent(authorization);
        requireReader(current);
        return ok(store.templateMap(templateId));
    }

    @PostMapping("/admin/templates/preview")
    ResponseEntity<Map<String, Object>> previewTemplate(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                        @RequestBody(required = false) Map<String, Object> body) {
        AuthUser current = auth.requireCurrent(authorization);
        requireReader(current);
        return ok(store.previewTemplate(bodyOrEmpty(body)));
    }

    @PostMapping("/admin/templates")
    ResponseEntity<Map<String, Object>> createTemplate(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                       @RequestBody(required = false) Map<String, Object> body) {
        AuthUser current = auth.requireCurrent(authorization);
        requireWriter(current);
        return created(store.createTemplate(current, bodyOrEmpty(body)));
    }

    @PatchMapping("/admin/templates/{templateId}")
    ResponseEntity<Map<String, Object>> patchTemplate(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                      @PathVariable String templateId,
                                                      @RequestBody(required = false) Map<String, Object> body) {
        AuthUser current = auth.requireCurrent(authorization);
        requireWriter(current);
        return ok(store.patchTemplate(current, templateId, bodyOrEmpty(body)));
    }

    @PatchMapping("/admin/templates/{templateId}/disable")
    ResponseEntity<Map<String, Object>> disableTemplate(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                        @PathVariable String templateId,
                                                        @RequestBody(required = false) Map<String, Object> body) {
        AuthUser current = auth.requireCurrent(authorization);
        requireWriter(current);
        return ok(store.disableTemplate(current, templateId, requiredString(bodyOrEmpty(body), "reason")));
    }

    @PatchMapping("/admin/templates/{templateId}/enable")
    ResponseEntity<Map<String, Object>> enableTemplate(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                       @PathVariable String templateId,
                                                       @RequestBody(required = false) Map<String, Object> body) {
        AuthUser current = auth.requireCurrent(authorization);
        requireWriter(current);
        return ok(store.enableTemplate(current, templateId, requiredString(bodyOrEmpty(body), "reason")));
    }

    @GetMapping("/admin/messages/{notificationId}/audit-logs")
    ResponseEntity<Map<String, Object>> auditLogs(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                  @PathVariable String notificationId,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize) {
        AuthUser current = auth.requireCurrent(authorization);
        requireAdminOrOwner(current);
        return ok(store.auditLogs(notificationId, page, pageSize));
    }

    @GetMapping("/admin/ops/summary")
    ResponseEntity<Map<String, Object>> opsSummary(@RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthUser current = auth.requireCurrent(authorization);
        requireAdminOrOwner(current);
        return ok(store.opsSummary());
    }

    private void requireReader(AuthUser current) {
        if (!current.roles.contains("HELPER") && !current.roles.contains("ADMIN") && !current.roles.contains("OWNER")) {
            throw new ApiException(42001, HttpStatus.FORBIDDEN, "role insufficient");
        }
    }

    private void requireWriter(AuthUser current) {
        if (!current.roles.contains("ADMIN") && !current.roles.contains("OWNER")) {
            throw new ApiException(42001, HttpStatus.FORBIDDEN, "role insufficient");
        }
    }

    private void requireAdminOrOwner(AuthUser current) {
        if (!current.roles.contains("ADMIN") && !current.roles.contains("OWNER")) {
            throw new ApiException(42001, HttpStatus.FORBIDDEN, "role insufficient");
        }
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
        if (!body.containsKey(field) || body.get(field) == null) {
            return null;
        }
        return String.valueOf(body.get(field));
    }

    static Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}

@Service
class TestAuthContextProvider {
    private final Map<String, AuthUser> usersByToken = new ConcurrentHashMap<>();
    private final Map<String, AuthUser> targetsByUserId = new ConcurrentHashMap<>();
    private boolean failCurrentUnavailable;
    private boolean failCurrentTimeout;
    private boolean failCurrentIncompatible;
    private boolean failTargetUnavailable;
    private boolean failTargetTimeout;
    private boolean failTargetIncompatible;
    private int writeCallCount;

    synchronized void reset() {
        usersByToken.clear();
        targetsByUserId.clear();
        failCurrentUnavailable = false;
        failCurrentTimeout = false;
        failCurrentIncompatible = false;
        failTargetUnavailable = false;
        failTargetTimeout = false;
        failTargetIncompatible = false;
        writeCallCount = 0;
        seedToken("owner-token", "owner", "Owner", List.of("OWNER"), "ACTIVE");
        seedToken("admin-token", "admin", "Admin", List.of("ADMIN"), "ACTIVE");
        seedToken("helper-token", "helper", "Helper", List.of("HELPER"), "ACTIVE");
        seedToken("user-token", "user", "User", List.of("USER"), "ACTIVE");
        seedToken("another-user-token", "another_user", "Another User", List.of("USER"), "ACTIVE");
        seedTarget("owner", "Owner", List.of("OWNER"), "ACTIVE");
        seedTarget("admin", "Admin", List.of("ADMIN"), "ACTIVE");
        seedTarget("helper", "Helper", List.of("HELPER"), "ACTIVE");
        seedTarget("user", "User", List.of("USER"), "ACTIVE");
        seedTarget("another_user", "Another User", List.of("USER"), "ACTIVE");
        seedTarget("disabled-user", "Disabled User", List.of("USER"), "DISABLED");
        seedTarget("banned-user", "Banned User", List.of("USER"), "BANNED");
        seedTarget("deleted-user", "Deleted User", List.of("USER"), "DELETED");
        seedTarget("bad-auth-user", null, List.of("USER"), "ACTIVE");
    }

    private void seedToken(String token, String userId, String displayName, List<String> roles, String status) {
        usersByToken.put(token, new AuthUser(userId, displayName, new LinkedHashSet<>(roles), new LinkedHashSet<>(), status));
    }

    private void seedTarget(String userId, String displayName, List<String> roles, String status) {
        targetsByUserId.put(userId, new AuthUser(userId, displayName, new LinkedHashSet<>(roles), new LinkedHashSet<>(), status));
    }

    AuthUser requireCurrent(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new ApiException(41000, HttpStatus.UNAUTHORIZED, "unauthenticated");
        }
        if (!authorization.startsWith("Bearer ")) {
            throw new ApiException(41003, HttpStatus.UNAUTHORIZED, "invalid token format");
        }
        if (failCurrentUnavailable) {
            failCurrentUnavailable = false;
            throw new ApiException(46300, HttpStatus.BAD_GATEWAY, "auth unavailable");
        }
        if (failCurrentTimeout) {
            failCurrentTimeout = false;
            throw new ApiException(46301, HttpStatus.GATEWAY_TIMEOUT, "auth timeout");
        }
        if (failCurrentIncompatible) {
            failCurrentIncompatible = false;
            throw new ApiException(46302, HttpStatus.BAD_GATEWAY, "auth incompatible");
        }
        AuthUser user = usersByToken.get(authorization.substring("Bearer ".length()));
        if (user == null) {
            throw new ApiException(41001, HttpStatus.UNAUTHORIZED, "invalid session");
        }
        if (user.userId == null || user.userId.isBlank() || user.displayName == null || user.roles == null) {
            throw new ApiException(46302, HttpStatus.BAD_GATEWAY, "auth incompatible");
        }
        return user.copy();
    }

    AuthUser targetUser(String userId) {
        if (failTargetUnavailable) {
            failTargetUnavailable = false;
            throw new ApiException(46300, HttpStatus.BAD_GATEWAY, "auth unavailable");
        }
        if (failTargetTimeout) {
            failTargetTimeout = false;
            throw new ApiException(46301, HttpStatus.GATEWAY_TIMEOUT, "auth timeout");
        }
        if (failTargetIncompatible) {
            failTargetIncompatible = false;
            throw new ApiException(46302, HttpStatus.BAD_GATEWAY, "auth incompatible");
        }
        AuthUser user = targetsByUserId.get(userId);
        if (user == null || Set.of("DISABLED", "BANNED", "DELETED").contains(user.status)) {
            throw new ApiException(43315, HttpStatus.NOT_FOUND, "recipient not deliverable");
        }
        if (user.userId == null || user.userId.isBlank() || user.displayName == null || user.roles == null || user.status == null) {
            throw new ApiException(46302, HttpStatus.BAD_GATEWAY, "auth incompatible");
        }
        return user.copy();
    }

    void failNextCurrentUnavailable() {
        failCurrentUnavailable = true;
    }

    void failNextCurrentTimeout() {
        failCurrentTimeout = true;
    }

    void failNextCurrentIncompatible() {
        failCurrentIncompatible = true;
    }

    void failNextTargetUnavailable() {
        failTargetUnavailable = true;
    }

    void failNextTargetTimeout() {
        failTargetTimeout = true;
    }

    void failNextTargetIncompatible() {
        failTargetIncompatible = true;
    }

    int writeCallCount() {
        return writeCallCount;
    }
}

@Service
class NotificationStore {
    private static final Set<String> TYPES = Set.of("SYSTEM", "AUDIT", "WHITELIST", "EXAM", "CONTENT", "RESOURCE", "ATTENDANCE", "COMMUNITY", "ACTIVITY", "OPS");
    private static final Set<String> RECIPIENT_STATUSES = Set.of("UNREAD", "READ", "ARCHIVED");
    private static final Set<String> DELIVERY_STATUSES = Set.of("PENDING", "DELIVERED", "FAILED", "CANCELED");
    private static final Set<String> TEMPLATE_STATUSES = Set.of("ENABLED", "DISABLED");
    private static final Pattern TEMPLATE_VARIABLE = Pattern.compile("\\$\\{([A-Za-z][A-Za-z0-9_]*)}");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, NotificationMessage> messages = new ConcurrentHashMap<>();
    private final Map<String, NotificationTemplateRecord> templates = new ConcurrentHashMap<>();
    private final Map<String, String> templateIdByCode = new ConcurrentHashMap<>();
    private final Map<String, String> idsByAlias = new ConcurrentHashMap<>();
    private final List<NotificationAudit> audits = new ArrayList<>();
    private final Map<String, IdempotencyRecord> messageIdempotency = new ConcurrentHashMap<>();
    private final Map<String, IdempotencyRecord> templateIdempotency = new ConcurrentHashMap<>();
    private boolean failNextAudit;
    private boolean failNextDeliveryWrite;
    private boolean examStatusChanged;
    private boolean whitelistStatusChanged;

    synchronized void reset() {
        messages.clear();
        templates.clear();
        templateIdByCode.clear();
        idsByAlias.clear();
        audits.clear();
        messageIdempotency.clear();
        templateIdempotency.clear();
        failNextAudit = false;
        failNextDeliveryWrite = false;
        examStatusChanged = false;
        whitelistStatusChanged = false;
    }

    synchronized void seedTestData(TestAuthContextProvider auth) {
        seedMessage("unread-user", "Exam Result", "Exam passed", "EXAM", "exam", "exam-1", "admin", null, List.of(recipient("user", "User", "UNREAD")));
        seedMessage("read-user", "Read Notice", "Already read", "SYSTEM", "notification", "notice-1", "admin", null, List.of(recipient("user", "User", "READ")));
        seedMessage("archived-user", "Archived Notice", "Archived", "SYSTEM", "notification", "notice-2", "admin", null, List.of(recipient("user", "User", "ARCHIVED")));
        seedMessage("expired-user", "Expired Notice", "Expired", "SYSTEM", "notification", "notice-3", "admin", Instant.now().minusSeconds(60), List.of(recipient("user", "User", "UNREAD")));
        seedMessage("unread-another", "Another Notice", "Another unread", "SYSTEM", "notification", "notice-4", "admin", null, List.of(recipient("another_user", "Another User", "UNREAD")));
        seedMessage("concurrent-read-user", "Concurrent Read", "Concurrent", "SYSTEM", "notification", "notice-5", "admin", null, List.of(recipient("user", "User", "UNREAD")));
        seedMessage("concurrent-archive-user", "Concurrent Archive", "Concurrent", "SYSTEM", "notification", "notice-6", "admin", null, List.of(recipient("user", "User", "UNREAD")));
        seedTemplate("enabled-template", "ENABLED_TEMPLATE", "Enabled Template", "Hello ${playerName}", "Result ${result}", "EXAM", "ENABLED");
        seedTemplate("disabled-template", "DISABLED_TEMPLATE", "Disabled Template", "Hello ${playerName}", "Result ${result}", "EXAM", "DISABLED");
        seedTemplate("duplicate-template", "DUPLICATE_TEMPLATE", "Duplicate Template", "Hello ${playerName}", "Result ${result}", "EXAM", "ENABLED");
        seedTemplate("broken-template", "BROKEN_TEMPLATE", "Broken Template", "Hello ${playerName}", "Result ${result}", "EXAM", "ENABLED");
    }

    private NotificationRecipient recipient(String userId, String displayName, String status) {
        Instant now = now();
        NotificationRecipient recipient = new NotificationRecipient(userId, displayName, status, "DELIVERED", null, now);
        if ("READ".equals(status)) {
            recipient.readAt = now;
        }
        if ("ARCHIVED".equals(status)) {
            recipient.archivedAt = now;
        }
        return recipient;
    }

    private void seedMessage(String alias, String title, String body, String type, String sourceModule, String sourceId, String createdBy, Instant expiresAt, List<NotificationRecipient> recipients) {
        String id = "msg_" + UUID.randomUUID();
        NotificationMessage message = new NotificationMessage(id, title, body, type, List.of("IN_APP"), sourceModule, sourceId, "LOW", null, null, null, null, null, createdBy, now(), expiresAt);
        recipients.forEach(recipient -> message.recipients.put(recipient.recipientUserId, recipient));
        messages.put(id, message);
        idsByAlias.put(alias, id);
        audits.add(new NotificationAudit("aud_" + UUID.randomUUID(), RequestIdFilter.currentRequestId(), createdBy, "ADMIN", id, "NOTIFICATION_MESSAGE_CREATED", "seed", "SUCCESS", now()));
    }

    private void seedTemplate(String alias, String code, String name, String title, String body, String type, String status) {
        String id = "tpl_" + UUID.randomUUID();
        NotificationTemplateRecord template = new NotificationTemplateRecord(id, code, name, title, body, variableDefinitions(), type, List.of("IN_APP"), status, 1, "admin", now(), "admin", now(), null);
        if ("DISABLED".equals(status)) {
            template.disabledAt = now();
        }
        templates.put(id, template);
        templateIdByCode.put(code, id);
        idsByAlias.put(alias, id);
    }

    synchronized Map<String, Object> currentUserMessages(String userId, int page, int pageSize, String status, String type, String sourceModule, boolean includeExpired, String sort) {
        validatePage(page, pageSize);
        if (status != null && !RECIPIENT_STATUSES.contains(status)) throw ApiException.badRequest("status");
        if (type != null && !TYPES.contains(type)) throw ApiException.badRequest("type");
        if (sort != null && !Set.of("createdAt_desc", "createdAt_asc", "readAt_desc").contains(sort)) throw new ApiException(40003, HttpStatus.BAD_REQUEST, "invalid sort");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (NotificationMessage message : messages.values()) {
            NotificationRecipient recipient = message.recipients.get(userId);
            if (recipient == null) continue;
            if (status == null && "ARCHIVED".equals(recipient.status)) continue;
            if (status != null && !status.equals(recipient.status)) continue;
            if (type != null && !type.equals(message.type)) continue;
            if (sourceModule != null && !sourceModule.equals(message.sourceModule)) continue;
            if (!includeExpired && isExpired(message)) continue;
            rows.add(recipientView(message, recipient));
        }
        sortRecipientRows(rows, sort);
        return page(rows, page, pageSize);
    }

    synchronized int unreadCount(String userId) {
        int count = 0;
        for (NotificationMessage message : messages.values()) {
            NotificationRecipient recipient = message.recipients.get(userId);
            if (recipient != null && "UNREAD".equals(recipient.status) && !isExpired(message)) {
                count++;
            }
        }
        return count;
    }

    synchronized Map<String, Object> currentUserMessage(String userId, String notificationId) {
        NotificationMessage message = message(notificationId);
        NotificationRecipient recipient = message.recipients.get(userId);
        if (recipient == null) {
            throw notFoundMessage();
        }
        return recipientView(message, recipient);
    }

    synchronized Map<String, Object> markRead(String userId, String notificationId) {
        NotificationMessage message = message(notificationId);
        NotificationRecipient recipient = message.recipients.get(userId);
        if (recipient == null) throw notFoundMessage();
        if ("ARCHIVED".equals(recipient.status)) {
            throw new ApiException(43311, HttpStatus.CONFLICT, "recipient state conflict");
        }
        if ("UNREAD".equals(recipient.status)) {
            recipient.status = "READ";
            recipient.readAt = now();
        }
        return recipientView(message, recipient);
    }

    synchronized int markAllRead(String userId, String type, String sourceModule) {
        if (type != null && !TYPES.contains(type)) throw ApiException.badRequest("type");
        int updated = 0;
        for (NotificationMessage message : messages.values()) {
            NotificationRecipient recipient = message.recipients.get(userId);
            if (recipient == null) continue;
            if (!"UNREAD".equals(recipient.status) || isExpired(message)) continue;
            if (type != null && !type.equals(message.type)) continue;
            if (sourceModule != null && !sourceModule.equals(message.sourceModule)) continue;
            recipient.status = "READ";
            recipient.readAt = now();
            updated++;
        }
        return updated;
    }

    synchronized Map<String, Object> archive(AuthUser current, String notificationId, String reason) {
        NotificationMessage message = message(notificationId);
        NotificationRecipient recipient = message.recipients.get(current.userId);
        if (recipient == null) throw notFoundMessage();
        if (!"ARCHIVED".equals(recipient.status)) {
            recipient.status = "ARCHIVED";
            recipient.archivedAt = now();
            audit("NOTIFICATION_RECIPIENT_ARCHIVED", current, notificationId, reason, "LOW");
        }
        return recipientView(message, recipient);
    }

    synchronized Map<String, Object> adminMessages(int page, int pageSize, String keyword, String type, String sourceModule, String recipientUserId, String deliveryStatus, String createdBy, String sort) {
        validatePage(page, pageSize);
        if (type != null && !TYPES.contains(type)) throw ApiException.badRequest("type");
        if (deliveryStatus != null && !DELIVERY_STATUSES.contains(deliveryStatus)) throw ApiException.badRequest("deliveryStatus");
        if (sort != null && !Set.of("createdAt_desc", "createdAt_asc", "recipientTotal_desc").contains(sort)) throw new ApiException(40003, HttpStatus.BAD_REQUEST, "invalid sort");
        String lowerKeyword = keyword == null ? null : keyword.toLowerCase(Locale.ROOT);
        List<Map<String, Object>> rows = messages.values().stream()
                .filter(message -> type == null || type.equals(message.type))
                .filter(message -> sourceModule == null || sourceModule.equals(message.sourceModule))
                .filter(message -> createdBy == null || createdBy.equals(message.createdBy))
                .filter(message -> recipientUserId == null || message.recipients.containsKey(recipientUserId))
                .filter(message -> deliveryStatus == null || message.recipients.values().stream().anyMatch(recipient -> deliveryStatus.equals(recipient.deliveryStatus)))
                .filter(message -> lowerKeyword == null || message.title.toLowerCase(Locale.ROOT).contains(lowerKeyword)
                        || message.body.toLowerCase(Locale.ROOT).contains(lowerKeyword)
                        || (message.sourceId != null && message.sourceId.toLowerCase(Locale.ROOT).contains(lowerKeyword))
                        || message.recipients.values().stream().anyMatch(recipient -> recipient.recipientDisplayNameSnapshot.toLowerCase(Locale.ROOT).contains(lowerKeyword)))
                .map(this::adminMessageMap)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        sortAdminRows(rows, sort);
        return page(rows, page, pageSize);
    }

    synchronized Map<String, Object> adminMessage(String notificationId) {
        return adminMessageMap(message(notificationId));
    }

    synchronized Map<String, Object> createMessage(AuthUser actor, TestAuthContextProvider auth, Map<String, Object> body, boolean fromTemplate) {
        String idempotencyKey = optionalString(body, "idempotencyKey");
        String idempotencyScope = actor.userId + ":" + (fromTemplate ? "template:" : "direct:") + idempotencyKey;
        String signature = signature(body);
        if (idempotencyKey != null && messageIdempotency.containsKey(idempotencyScope)) {
            IdempotencyRecord record = messageIdempotency.get(idempotencyScope);
            if (!record.signature.equals(signature)) {
                throw new ApiException(43002, HttpStatus.CONFLICT, "idempotency conflict");
            }
            return record.payload;
        }
        List<String> recipientUserIds = stringList(body.get("recipientUserIds"));
        LinkedHashSet<String> uniqueRecipients = new LinkedHashSet<>(recipientUserIds);
        if (uniqueRecipients.isEmpty() || uniqueRecipients.size() > 200) {
            throw new ApiException(43316, HttpStatus.BAD_REQUEST, "invalid recipients");
        }
        String title = requiredString(body, "title");
        String text = requiredString(body, "body");
        validateLength(title, 2, 80, "title");
        validateLength(text, 1, 2000, "body");
        String type = requiredString(body, "type");
        if (!TYPES.contains(type)) throw ApiException.badRequest("type");
        List<String> channels = channels(body.get("channels"));
        String sourceModule = optionalString(body, "sourceModule");
        String sourceId = optionalString(body, "sourceId");
        String riskLevel = optionalStringOrDefault(body, "riskLevel", "LOW");
        String actionUrl = optionalString(body, "actionUrl");
        validateActionUrl(actionUrl);
        Instant expiresAt = optionalInstant(body, "expiresAt");
        if (expiresAt != null && !expiresAt.isAfter(now())) throw ApiException.badRequest("expiresAt");
        String reason = requiredString(body, "reason");
        validateLength(reason, 1, 200, "reason");
        String templateId = optionalString(body, "templateId");
        String templateCode = optionalString(body, "templateCode");
        Integer templateVersion = body.containsKey("templateVersion") ? intValue(body, "templateVersion") : null;
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = body.get("variables") instanceof Map<?, ?> ? new LinkedHashMap<>((Map<String, Object>) body.get("variables")) : null;
        List<NotificationRecipient> recipients = new ArrayList<>();
        for (String userId : uniqueRecipients) {
            AuthUser target = auth.targetUser(userId);
            recipients.add(new NotificationRecipient(target.userId, target.displayName, "UNREAD", "DELIVERED", null, now()));
        }
        audit("NOTIFICATION_MESSAGE_CREATED", actor, null, reason, riskLevel);
        if (failNextDeliveryWrite) {
            failNextDeliveryWrite = false;
            throw new ApiException(51302, HttpStatus.INTERNAL_SERVER_ERROR, "delivery write failed");
        }
        NotificationMessage message = new NotificationMessage("msg_" + UUID.randomUUID(), title, text, type, channels, sourceModule, sourceId, riskLevel, actionUrl, templateId, templateCode, templateVersion, variables, actor.userId, now(), expiresAt);
        recipients.forEach(recipient -> message.recipients.put(recipient.recipientUserId, recipient));
        messages.put(message.notificationId, message);
        Map<String, Object> payload = adminMessageMap(message);
        if (idempotencyKey != null) {
            messageIdempotency.put(idempotencyScope, new IdempotencyRecord(signature, payload));
        }
        return payload;
    }

    synchronized Map<String, Object> createFromTemplate(AuthUser actor, TestAuthContextProvider auth, Map<String, Object> body) {
        String templateCode = requiredString(body, "templateCode");
        NotificationTemplateRecord template = templateByCode(templateCode);
        if ("DISABLED".equals(template.status)) {
            throw new ApiException(43312, HttpStatus.CONFLICT, "template disabled");
        }
        String reason = requiredString(body, "reason");
        List<String> channels = body.containsKey("channels") ? channels(body.get("channels")) : template.channels;
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = body.get("variables") instanceof Map<?, ?> raw ? new LinkedHashMap<>((Map<String, Object>) raw) : null;
        if (variables == null) {
            throw new ApiException(43313, HttpStatus.BAD_REQUEST, "template variables invalid");
        }
        Map<String, String> renderVariables = validateTemplateVariables(template, variables);
        String title = render(template.titleTemplate, renderVariables, template);
        String text = render(template.bodyTemplate, renderVariables, template);
        if (title.length() > 80 || text.length() > 2000) {
            throw new ApiException(43314, HttpStatus.BAD_REQUEST, "template render failed");
        }
        Map<String, Object> messageBody = new LinkedHashMap<>(body);
        messageBody.put("title", title);
        messageBody.put("body", text);
        messageBody.put("type", template.type);
        messageBody.put("channels", channels);
        messageBody.put("templateId", template.templateId);
        messageBody.put("templateCode", template.code);
        messageBody.put("templateVersion", template.version);
        messageBody.put("variables", new LinkedHashMap<>(variables));
        messageBody.put("reason", reason);
        if (!messageBody.containsKey("sourceModule")) {
            messageBody.put("sourceModule", "notification");
        }
        return createMessage(actor, auth, messageBody, true);
    }

    synchronized Map<String, Object> templates(int page, int pageSize, String keyword, String status, String type, String sort) {
        validatePage(page, pageSize);
        if (status != null && !TEMPLATE_STATUSES.contains(status)) throw ApiException.badRequest("status");
        if (type != null && !TYPES.contains(type)) throw ApiException.badRequest("type");
        if (sort != null && !Set.of("updatedAt_desc", "createdAt_desc", "code_asc").contains(sort)) throw new ApiException(40003, HttpStatus.BAD_REQUEST, "invalid sort");
        String lowerKeyword = keyword == null ? null : keyword.toLowerCase(Locale.ROOT);
        List<Map<String, Object>> rows = templates.values().stream()
                .filter(template -> status == null || status.equals(template.status))
                .filter(template -> type == null || type.equals(template.type))
                .filter(template -> lowerKeyword == null || template.code.toLowerCase(Locale.ROOT).contains(lowerKeyword) || template.name.toLowerCase(Locale.ROOT).contains(lowerKeyword))
                .map(this::templateMapRecord)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        rows.sort(templateComparator(sort));
        return page(rows, page, pageSize);
    }

    synchronized Map<String, Object> templateMap(String templateId) {
        return templateMapRecord(template(templateId));
    }

    @SuppressWarnings("unchecked")
    synchronized Map<String, Object> previewTemplate(Map<String, Object> body) {
        NotificationTemplateRecord template = templateByCode(requiredString(body, "templateCode"));
        Map<String, Object> variables = body.get("variables") instanceof Map<?, ?> raw ? new LinkedHashMap<>((Map<String, Object>) raw) : null;
        if (variables == null) {
            throw new ApiException(43313, HttpStatus.BAD_REQUEST, "template variables invalid");
        }
        Map<String, String> renderVariables = validateTemplateVariables(template, variables);
        String title = render(template.titleTemplate, renderVariables, template);
        String text = render(template.bodyTemplate, renderVariables, template);
        if (title.length() > 80 || text.length() > 2000) {
            throw new ApiException(43314, HttpStatus.BAD_REQUEST, "template render failed");
        }
        return NotificationController.mapOf(
                "templateId", template.templateId,
                "templateCode", template.code,
                "templateVersion", template.version,
                "templateStatus", template.status,
                "sendable", "ENABLED".equals(template.status),
                "title", title,
                "body", text,
                "variables", new LinkedHashMap<>(renderVariables),
                "createdNotification", false
        );
    }

    synchronized Map<String, Object> createTemplate(AuthUser actor, Map<String, Object> body) {
        String idempotencyKey = optionalString(body, "idempotencyKey");
        String scope = actor.userId + ":" + idempotencyKey;
        String signature = signature(body);
        if (idempotencyKey != null && templateIdempotency.containsKey(scope)) {
            IdempotencyRecord record = templateIdempotency.get(scope);
            if (!record.signature.equals(signature)) {
                throw new ApiException(43002, HttpStatus.CONFLICT, "idempotency conflict");
            }
            return record.payload;
        }
        NotificationTemplateRecord candidate = templateFromBody(null, actor, body, false);
        if (templateIdByCode.containsKey(candidate.code)) {
            throw new ApiException(43317, HttpStatus.CONFLICT, "template code exists");
        }
        audit("NOTIFICATION_TEMPLATE_CREATED", actor, candidate.templateId, requiredString(body, "reason"), "MEDIUM");
        templates.put(candidate.templateId, candidate);
        templateIdByCode.put(candidate.code, candidate.templateId);
        Map<String, Object> payload = templateMapRecord(candidate);
        if (idempotencyKey != null) {
            templateIdempotency.put(scope, new IdempotencyRecord(signature, payload));
        }
        return payload;
    }

    synchronized Map<String, Object> patchTemplate(AuthUser actor, String templateId, Map<String, Object> body) {
        NotificationTemplateRecord existing = template(templateId);
        String reason = requiredString(body, "reason");
        NotificationTemplateRecord candidate = existing.copy();
        applyTemplatePatch(candidate, actor, body);
        if (!candidate.code.equals(existing.code) && templateIdByCode.containsKey(candidate.code)) {
            throw new ApiException(43317, HttpStatus.CONFLICT, "template code exists");
        }
        validateTemplate(candidate);
        audit("NOTIFICATION_TEMPLATE_UPDATED", actor, templateId, reason, "MEDIUM");
        if (!candidate.code.equals(existing.code)) {
            templateIdByCode.remove(existing.code);
            templateIdByCode.put(candidate.code, templateId);
        }
        candidate.version = existing.version + 1;
        candidate.updatedBy = actor.userId;
        candidate.updatedAt = now();
        templates.put(templateId, candidate);
        return templateMapRecord(candidate);
    }

    synchronized Map<String, Object> disableTemplate(AuthUser actor, String templateId, String reason) {
        NotificationTemplateRecord template = template(templateId);
        if (!"DISABLED".equals(template.status)) {
            audit("NOTIFICATION_TEMPLATE_DISABLED", actor, templateId, reason, "MEDIUM");
            template.status = "DISABLED";
            template.disabledAt = now();
            template.updatedBy = actor.userId;
            template.updatedAt = now();
        }
        return templateMapRecord(template);
    }

    synchronized Map<String, Object> enableTemplate(AuthUser actor, String templateId, String reason) {
        NotificationTemplateRecord template = template(templateId);
        validateTemplate(template);
        if (!"ENABLED".equals(template.status)) {
            audit("NOTIFICATION_TEMPLATE_ENABLED", actor, templateId, reason, "MEDIUM");
            template.status = "ENABLED";
            template.disabledAt = null;
            template.updatedBy = actor.userId;
            template.updatedAt = now();
        }
        return templateMapRecord(template);
    }

    synchronized Map<String, Object> auditLogs(String notificationId, int page, int pageSize) {
        validatePage(page, pageSize);
        if (!messages.containsKey(notificationId)) {
            throw notFoundMessage();
        }
        List<Map<String, Object>> rows = audits.stream()
                .filter(audit -> notificationId.equals(audit.targetId))
                .sorted(Comparator.comparing((NotificationAudit audit) -> audit.createdAt).reversed())
                .map(this::auditMap)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        return page(rows, page, pageSize);
    }

    synchronized Map<String, Object> opsSummary() {
        int recipientsTotal = messages.values().stream().mapToInt(message -> message.recipients.size()).sum();
        long unreadTotal = messages.values().stream()
                .flatMap(message -> message.recipients.values().stream())
                .filter(recipient -> "UNREAD".equals(recipient.status))
                .count();
        long archivedTotal = messages.values().stream()
                .flatMap(message -> message.recipients.values().stream())
                .filter(recipient -> "ARCHIVED".equals(recipient.status))
                .count();
        long deliveredTotal = messages.values().stream()
                .flatMap(message -> message.recipients.values().stream())
                .filter(recipient -> "DELIVERED".equals(recipient.deliveryStatus))
                .count();
        long failedTotal = messages.values().stream()
                .flatMap(message -> message.recipients.values().stream())
                .filter(recipient -> "FAILED".equals(recipient.deliveryStatus))
                .count();
        String lastAuditAt = audits.stream()
                .map(audit -> audit.createdAt)
                .max(Comparator.naturalOrder())
                .map(Instant::toString)
                .orElse(null);
        return NotificationController.mapOf(
                "service", "notification",
                "storageMode", "IN_MEMORY",
                "authMode", "TEST_STUB",
                "messagesTotal", messages.size(),
                "templatesTotal", templates.size(),
                "auditsTotal", audits.size(),
                "recipientsTotal", recipientsTotal,
                "unreadTotal", (int) unreadTotal,
                "archivedTotal", (int) archivedTotal,
                "deliveredTotal", (int) deliveredTotal,
                "failedTotal", (int) failedTotal,
                "pendingExternalDeliveries", 0,
                "lastAuditAt", lastAuditAt,
                "warnings", List.of("P0_IN_MEMORY_STORAGE", "P0_AUTH_STUB")
        );
    }

    private NotificationTemplateRecord templateFromBody(String templateId, AuthUser actor, Map<String, Object> body, boolean patch) {
        String id = templateId == null ? "tpl_" + UUID.randomUUID() : templateId;
        NotificationTemplateRecord template = new NotificationTemplateRecord(
                id,
                requiredString(body, "code"),
                requiredString(body, "name"),
                requiredString(body, "titleTemplate"),
                requiredString(body, "bodyTemplate"),
                variableDefinitions(body.get("variableDefinitions")),
                requiredString(body, "type"),
                body.containsKey("channels") ? channels(body.get("channels")) : List.of("IN_APP"),
                "ENABLED",
                1,
                actor.userId,
                now(),
                actor.userId,
                now(),
                null
        );
        requiredString(body, "reason");
        validateTemplate(template);
        return template;
    }

    private void applyTemplatePatch(NotificationTemplateRecord template, AuthUser actor, Map<String, Object> body) {
        if (body.containsKey("code")) template.code = requiredString(body, "code");
        if (body.containsKey("name")) template.name = requiredString(body, "name");
        if (body.containsKey("titleTemplate")) template.titleTemplate = requiredString(body, "titleTemplate");
        if (body.containsKey("bodyTemplate")) template.bodyTemplate = requiredString(body, "bodyTemplate");
        if (body.containsKey("variableDefinitions")) template.variableDefinitions = variableDefinitions(body.get("variableDefinitions"));
        if (body.containsKey("type")) template.type = requiredString(body, "type");
        if (body.containsKey("channels")) template.channels = channels(body.get("channels"));
    }

    private void validateTemplate(NotificationTemplateRecord template) {
        if (template.invalid) {
            throw new ApiException(43313, HttpStatus.BAD_REQUEST, "template variables invalid");
        }
        if (!Pattern.matches("[A-Z0-9_.]{3,64}", template.code)) throw ApiException.badRequest("code");
        validateLength(template.name, 2, 50, "name");
        validateLength(template.titleTemplate, 2, 120, "titleTemplate");
        validateLength(template.bodyTemplate, 1, 3000, "bodyTemplate");
        if (!TYPES.contains(template.type)) throw ApiException.badRequest("type");
        if (template.channels.stream().anyMatch(channel -> !"IN_APP".equals(channel))) throw ApiException.badRequest("channels");
        Set<String> defined = new LinkedHashSet<>();
        for (TemplateVariable variable : template.variableDefinitions) {
            if (!Pattern.matches("[A-Za-z][A-Za-z0-9_]{0,39}", variable.name) || !defined.add(variable.name)) {
                throw new ApiException(43313, HttpStatus.BAD_REQUEST, "template variables invalid");
            }
        }
        Set<String> referenced = referencedVariables(template.titleTemplate + "\n" + template.bodyTemplate);
        if (!defined.containsAll(referenced)) {
            throw new ApiException(43313, HttpStatus.BAD_REQUEST, "template variables invalid");
        }
    }

    private Map<String, String> validateTemplateVariables(NotificationTemplateRecord template, Map<String, Object> variables) {
        Set<String> defined = template.variableDefinitions.stream().map(variable -> variable.name).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<String, String> rendered = new LinkedHashMap<>();
        for (String key : variables.keySet()) {
            if (!Pattern.matches("[A-Za-z][A-Za-z0-9_]{0,39}", key) || !defined.contains(key)) {
                throw new ApiException(43313, HttpStatus.BAD_REQUEST, "template variables invalid");
            }
            String value = String.valueOf(variables.get(key));
            if (value.length() > 500) throw ApiException.badRequest(key);
            rendered.put(key, value);
        }
        for (TemplateVariable variable : template.variableDefinitions) {
            if (variable.required && !rendered.containsKey(variable.name)) {
                throw new ApiException(43313, HttpStatus.BAD_REQUEST, "template variables invalid");
            }
        }
        return rendered;
    }

    private String render(String raw, Map<String, String> variables, NotificationTemplateRecord template) {
        if (template.renderBroken) {
            throw new ApiException(43314, HttpStatus.BAD_REQUEST, "template render failed");
        }
        String rendered = raw;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        if (rendered.contains("${")) {
            throw new ApiException(43314, HttpStatus.BAD_REQUEST, "template render failed");
        }
        return rendered;
    }

    private Set<String> referencedVariables(String text) {
        Set<String> values = new LinkedHashSet<>();
        Matcher matcher = TEMPLATE_VARIABLE.matcher(text);
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }

    private List<TemplateVariable> variableDefinitions() {
        return List.of(
                new TemplateVariable("playerName", true, "player", "Steve"),
                new TemplateVariable("result", true, "result", "PASS")
        );
    }

    @SuppressWarnings("unchecked")
    private List<TemplateVariable> variableDefinitions(Object raw) {
        if (!(raw instanceof List<?> list) || list.size() > 30) {
            throw ApiException.badRequest("variableDefinitions");
        }
        List<TemplateVariable> variables = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                throw ApiException.badRequest("variableDefinitions");
            }
            Map<String, Object> map = (Map<String, Object>) rawMap;
            variables.add(new TemplateVariable(
                    requiredString(map, "name"),
                    booleanValue(map, "required"),
                    optionalString(map, "description"),
                    optionalString(map, "example")
            ));
        }
        return variables;
    }

    private void audit(String action, AuthUser actor, String targetId, String reason, String riskLevel) {
        if (failNextAudit) {
            failNextAudit = false;
            throw new ApiException(51301, HttpStatus.INTERNAL_SERVER_ERROR, "notification audit failed");
        }
        audits.add(new NotificationAudit("aud_" + UUID.randomUUID(), RequestIdFilter.currentRequestId(), actor.userId, String.join(",", actor.roles), targetId, action, reason, "SUCCESS", now()));
    }

    private Map<String, Object> recipientView(NotificationMessage message, NotificationRecipient recipient) {
        return NotificationController.mapOf(
                "notificationId", message.notificationId,
                "recipientUserId", recipient.recipientUserId,
                "recipientDisplayNameSnapshot", recipient.recipientDisplayNameSnapshot,
                "title", message.title,
                "body", message.body,
                "type", message.type,
                "sourceModule", message.sourceModule,
                "sourceId", message.sourceId,
                "riskLevel", message.riskLevel,
                "actionUrl", message.actionUrl,
                "status", recipient.status,
                "deliveryStatus", recipient.deliveryStatus,
                "failureReason", recipient.failureReason,
                "createdBy", message.createdBy,
                "createdAt", message.createdAt.toString(),
                "readAt", recipient.readAt == null ? null : recipient.readAt.toString(),
                "archivedAt", recipient.archivedAt == null ? null : recipient.archivedAt.toString(),
                "expiresAt", message.expiresAt == null ? null : message.expiresAt.toString()
        );
    }

    private Map<String, Object> adminMessageMap(NotificationMessage message) {
        List<Map<String, Object>> recipients = message.recipients.values().stream().map(this::adminRecipientMap).toList();
        long delivered = message.recipients.values().stream().filter(recipient -> "DELIVERED".equals(recipient.deliveryStatus)).count();
        long failed = message.recipients.values().stream().filter(recipient -> "FAILED".equals(recipient.deliveryStatus)).count();
        return NotificationController.mapOf(
                "notificationId", message.notificationId,
                "title", message.title,
                "body", message.body,
                "type", message.type,
                "channels", message.channels,
                "sourceModule", message.sourceModule,
                "sourceId", message.sourceId,
                "riskLevel", message.riskLevel,
                "actionUrl", message.actionUrl,
                "templateId", message.templateId,
                "templateCode", message.templateCode,
                "templateVersion", message.templateVersion,
                "variables", message.variables,
                "recipientTotal", message.recipients.size(),
                "deliveredTotal", (int) delivered,
                "failedTotal", (int) failed,
                "recipients", recipients,
                "createdBy", message.createdBy,
                "createdAt", message.createdAt.toString(),
                "expiresAt", message.expiresAt == null ? null : message.expiresAt.toString()
        );
    }

    private Map<String, Object> adminRecipientMap(NotificationRecipient recipient) {
        return NotificationController.mapOf(
                "recipientUserId", recipient.recipientUserId,
                "recipientDisplayNameSnapshot", recipient.recipientDisplayNameSnapshot,
                "status", recipient.status,
                "deliveryStatus", recipient.deliveryStatus,
                "failureReason", recipient.failureReason,
                "readAt", recipient.readAt == null ? null : recipient.readAt.toString(),
                "archivedAt", recipient.archivedAt == null ? null : recipient.archivedAt.toString(),
                "deliveredAt", recipient.deliveredAt == null ? null : recipient.deliveredAt.toString()
        );
    }

    private Map<String, Object> templateMapRecord(NotificationTemplateRecord template) {
        return NotificationController.mapOf(
                "templateId", template.templateId,
                "code", template.code,
                "name", template.name,
                "titleTemplate", template.titleTemplate,
                "bodyTemplate", template.bodyTemplate,
                "variableDefinitions", template.variableDefinitions.stream().map(this::variableMap).toList(),
                "type", template.type,
                "channels", template.channels,
                "status", template.status,
                "version", template.version,
                "createdBy", template.createdBy,
                "createdAt", template.createdAt.toString(),
                "updatedBy", template.updatedBy,
                "updatedAt", template.updatedAt.toString(),
                "disabledAt", template.disabledAt == null ? null : template.disabledAt.toString()
        );
    }

    private Map<String, Object> variableMap(TemplateVariable variable) {
        return NotificationController.mapOf("name", variable.name, "required", variable.required, "description", variable.description, "example", variable.example);
    }

    private Map<String, Object> auditMap(NotificationAudit audit) {
        return NotificationController.mapOf(
                "id", audit.id,
                "requestId", audit.requestId,
                "actorUserId", audit.actorUserId,
                "actorRole", audit.actorRole,
                "actorPermissions", List.of(),
                "sourceIp", null,
                "targetType", "NOTIFICATION",
                "targetId", audit.targetId,
                "action", audit.action,
                "riskLevel", "MEDIUM",
                "reason", audit.reason,
                "paramsSummary", null,
                "beforeState", null,
                "afterState", null,
                "result", audit.result,
                "failureReason", null,
                "createdAt", audit.createdAt.toString()
        );
    }

    private NotificationMessage message(String notificationId) {
        NotificationMessage message = messages.get(notificationId);
        if (message == null) throw notFoundMessage();
        return message;
    }

    private NotificationTemplateRecord template(String templateId) {
        NotificationTemplateRecord template = templates.get(templateId);
        if (template == null) {
            throw new ApiException(43301, HttpStatus.NOT_FOUND, "template not found");
        }
        return template;
    }

    private NotificationTemplateRecord templateByCode(String code) {
        String templateId = templateIdByCode.get(code);
        if (templateId == null) {
            throw new ApiException(43301, HttpStatus.NOT_FOUND, "template not found");
        }
        return template(templateId);
    }

    private ApiException notFoundMessage() {
        return new ApiException(43300, HttpStatus.NOT_FOUND, "notification not found");
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ApiException(40002, HttpStatus.BAD_REQUEST, "invalid page");
        }
    }

    private Map<String, Object> page(List<Map<String, Object>> rows, int page, int pageSize) {
        int from = Math.min((page - 1) * pageSize, rows.size());
        int to = Math.min(from + pageSize, rows.size());
        return NotificationController.mapOf("items", rows.subList(from, to), "page", page, "pageSize", pageSize, "total", rows.size());
    }

    private void sortRecipientRows(List<Map<String, Object>> rows, String sort) {
        Comparator<Map<String, Object>> comparator = Comparator.comparing(row -> String.valueOf(row.get("createdAt")));
        if ("createdAt_asc".equals(sort)) {
            rows.sort(comparator);
        } else if ("readAt_desc".equals(sort)) {
            rows.sort(Comparator.comparing((Map<String, Object> row) -> String.valueOf(row.get("readAt"))).reversed());
        } else {
            rows.sort(comparator.reversed());
        }
    }

    private void sortAdminRows(List<Map<String, Object>> rows, String sort) {
        if ("createdAt_asc".equals(sort)) {
            rows.sort(Comparator.comparing(row -> String.valueOf(row.get("createdAt"))));
        } else if ("recipientTotal_desc".equals(sort)) {
            rows.sort(Comparator.comparing((Map<String, Object> row) -> (Integer) row.get("recipientTotal")).reversed());
        } else {
            rows.sort(Comparator.comparing((Map<String, Object> row) -> String.valueOf(row.get("createdAt"))).reversed());
        }
    }

    private Comparator<Map<String, Object>> templateComparator(String sort) {
        if ("createdAt_desc".equals(sort)) {
            return Comparator.comparing((Map<String, Object> row) -> String.valueOf(row.get("createdAt"))).reversed();
        }
        if ("code_asc".equals(sort)) {
            return Comparator.comparing(row -> String.valueOf(row.get("code")));
        }
        return Comparator.comparing((Map<String, Object> row) -> String.valueOf(row.get("updatedAt"))).reversed();
    }

    private boolean isExpired(NotificationMessage message) {
        return message.expiresAt != null && message.expiresAt.isBefore(now());
    }

    private List<String> channels(Object raw) {
        if (raw == null) return List.of("IN_APP");
        List<String> channels = stringList(raw);
        if (channels.isEmpty() || channels.stream().anyMatch(channel -> !"IN_APP".equals(channel))) {
            throw ApiException.badRequest("channels");
        }
        return new ArrayList<>(new LinkedHashSet<>(channels));
    }

    private List<String> stringList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            throw ApiException.badRequest("array");
        }
        return list.stream().map(String::valueOf).toList();
    }

    private String requiredString(Map<String, Object> body, String field) {
        return NotificationController.requiredString(body, field);
    }

    private String optionalString(Map<String, Object> body, String field) {
        return NotificationController.optionalString(body, field);
    }

    private String optionalStringOrDefault(Map<String, Object> body, String field, String fallback) {
        String value = optionalString(body, field);
        return value == null ? fallback : value;
    }

    private int intValue(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (value instanceof Number number) return number.intValue();
        throw ApiException.badRequest(field);
    }

    private boolean booleanValue(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (value instanceof Boolean bool) return bool;
        throw ApiException.badRequest(field);
    }

    private Instant optionalInstant(Map<String, Object> body, String field) {
        String value = optionalString(body, field);
        return value == null ? null : Instant.parse(value);
    }

    private void validateLength(String value, int min, int max, String field) {
        if (value == null || value.length() < min || value.length() > max) {
            throw ApiException.badRequest(field);
        }
    }

    private void validateActionUrl(String value) {
        if (value == null) return;
        if (value.length() > 500 || !(value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/"))) {
            throw ApiException.badRequest("actionUrl");
        }
    }

    private String signature(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private Instant now() {
        return Instant.now();
    }

    String notificationId(String alias) {
        return idsByAlias.get(alias);
    }

    String templateId(String alias) {
        return idsByAlias.get(alias);
    }

    String latestAuditRequestId(String action) {
        return audits.stream().filter(audit -> action.equals(audit.action)).reduce((first, second) -> second).map(audit -> audit.requestId).orElse(null);
    }

    String readAt(String userId, String notificationId) {
        NotificationRecipient recipient = message(notificationId).recipients.get(userId);
        return recipient == null || recipient.readAt == null ? null : recipient.readAt.toString();
    }

    String archivedAt(String userId, String notificationId) {
        NotificationRecipient recipient = message(notificationId).recipients.get(userId);
        return recipient == null || recipient.archivedAt == null ? null : recipient.archivedAt.toString();
    }

    int messageCountByTitle(String title) {
        return (int) messages.values().stream().filter(message -> title.equals(message.title)).count();
    }

    boolean templateExists(String code) {
        return templateIdByCode.containsKey(code);
    }

    int templateVersion(String templateId) {
        return template(templateId).version;
    }

    void failNextAudit() {
        failNextAudit = true;
    }

    void failNextDeliveryWrite() {
        failNextDeliveryWrite = true;
    }

    void markTemplateInvalid(String templateId) {
        template(templateId).invalid = true;
        template(templateId).status = "DISABLED";
    }

    void markTemplateRenderBroken(String code) {
        templateByCode(code).renderBroken = true;
    }

    void patchTemplateName(String code, String name) {
        templateByCode(code).name = name;
        templateByCode(code).version++;
    }

    boolean usesAuthImplementation() {
        return false;
    }

    boolean examStatusChanged() {
        return examStatusChanged;
    }

    boolean whitelistStatusChanged() {
        return whitelistStatusChanged;
    }
}

@RestControllerAdvice
class NotificationExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> api(ApiException exception) {
        Map<String, Object> body = NotificationController.envelope(exception.code, exception.getMessage(), null);
        body.put("requestId", RequestIdFilter.currentRequestId());
        if (exception.code == 40001) {
            body.put("errors", List.of(Map.of("field", exception.field == null ? "request" : exception.field, "reason", exception.getMessage())));
        }
        return ResponseEntity.status(exception.status).body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Map<String, Object>> methodNotSupported(HttpRequestMethodNotSupportedException exception) {
        Map<String, Object> body = NotificationController.envelope(40000, "invalid request", null);
        body.put("requestId", RequestIdFilter.currentRequestId());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> any(Exception exception) {
        Map<String, Object> body = NotificationController.envelope(51300, "notification internal error", null);
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

class AuthUser {
    final String userId;
    final String displayName;
    final LinkedHashSet<String> roles;
    final LinkedHashSet<String> permissions;
    final String status;

    AuthUser(String userId, String displayName, Set<String> roles, Set<String> permissions, String status) {
        this.userId = userId;
        this.displayName = displayName;
        this.roles = new LinkedHashSet<>(roles);
        this.permissions = new LinkedHashSet<>(permissions);
        this.status = status;
    }

    AuthUser copy() {
        return new AuthUser(userId, displayName, roles, permissions, status);
    }
}

class NotificationMessage {
    final String notificationId;
    final String title;
    final String body;
    final String type;
    final List<String> channels;
    final String sourceModule;
    final String sourceId;
    final String riskLevel;
    final String actionUrl;
    final String templateId;
    final String templateCode;
    final Integer templateVersion;
    final Map<String, Object> variables;
    final String createdBy;
    final Instant createdAt;
    final Instant expiresAt;
    final Map<String, NotificationRecipient> recipients = new LinkedHashMap<>();

    NotificationMessage(String notificationId, String title, String body, String type, List<String> channels, String sourceModule, String sourceId, String riskLevel, String actionUrl, String templateId, String templateCode, Integer templateVersion, Map<String, Object> variables, String createdBy, Instant createdAt, Instant expiresAt) {
        this.notificationId = notificationId;
        this.title = title;
        this.body = body;
        this.type = type;
        this.channels = channels;
        this.sourceModule = sourceModule;
        this.sourceId = sourceId;
        this.riskLevel = riskLevel;
        this.actionUrl = actionUrl;
        this.templateId = templateId;
        this.templateCode = templateCode;
        this.templateVersion = templateVersion;
        this.variables = variables;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
}

class NotificationRecipient {
    final String recipientUserId;
    final String recipientDisplayNameSnapshot;
    String status;
    final String deliveryStatus;
    final String failureReason;
    Instant readAt;
    Instant archivedAt;
    final Instant deliveredAt;

    NotificationRecipient(String recipientUserId, String recipientDisplayNameSnapshot, String status, String deliveryStatus, String failureReason, Instant deliveredAt) {
        this.recipientUserId = recipientUserId;
        this.recipientDisplayNameSnapshot = recipientDisplayNameSnapshot;
        this.status = status;
        this.deliveryStatus = deliveryStatus;
        this.failureReason = failureReason;
        this.deliveredAt = deliveredAt;
    }
}

class NotificationTemplateRecord {
    final String templateId;
    String code;
    String name;
    String titleTemplate;
    String bodyTemplate;
    List<TemplateVariable> variableDefinitions;
    String type;
    List<String> channels;
    String status;
    int version;
    final String createdBy;
    final Instant createdAt;
    String updatedBy;
    Instant updatedAt;
    Instant disabledAt;
    boolean invalid;
    boolean renderBroken;

    NotificationTemplateRecord(String templateId, String code, String name, String titleTemplate, String bodyTemplate, List<TemplateVariable> variableDefinitions, String type, List<String> channels, String status, int version, String createdBy, Instant createdAt, String updatedBy, Instant updatedAt, Instant disabledAt) {
        this.templateId = templateId;
        this.code = code;
        this.name = name;
        this.titleTemplate = titleTemplate;
        this.bodyTemplate = bodyTemplate;
        this.variableDefinitions = new ArrayList<>(variableDefinitions);
        this.type = type;
        this.channels = new ArrayList<>(channels);
        this.status = status;
        this.version = version;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
        this.disabledAt = disabledAt;
    }

    NotificationTemplateRecord copy() {
        NotificationTemplateRecord copy = new NotificationTemplateRecord(templateId, code, name, titleTemplate, bodyTemplate, variableDefinitions, type, channels, status, version, createdBy, createdAt, updatedBy, updatedAt, disabledAt);
        copy.invalid = invalid;
        copy.renderBroken = renderBroken;
        return copy;
    }
}

class TemplateVariable {
    final String name;
    final boolean required;
    final String description;
    final String example;

    TemplateVariable(String name, boolean required, String description, String example) {
        this.name = name;
        this.required = required;
        this.description = description;
        this.example = example;
    }
}

class NotificationAudit {
    final String id;
    final String requestId;
    final String actorUserId;
    final String actorRole;
    final String targetId;
    final String action;
    final String reason;
    final String result;
    final Instant createdAt;

    NotificationAudit(String id, String requestId, String actorUserId, String actorRole, String targetId, String action, String reason, String result, Instant createdAt) {
        this.id = id;
        this.requestId = requestId;
        this.actorUserId = actorUserId;
        this.actorRole = actorRole;
        this.targetId = targetId;
        this.action = action;
        this.reason = reason;
        this.result = result;
        this.createdAt = createdAt;
    }
}

class IdempotencyRecord {
    final String signature;
    final Map<String, Object> payload;

    IdempotencyRecord(String signature, Map<String, Object> payload) {
        this.signature = signature;
        this.payload = payload;
    }
}
