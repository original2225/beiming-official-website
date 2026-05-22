package cn.beiming.admin;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
class AdminModule {
    @Bean
    AdminStore adminStore() {
        AdminStore store = new AdminStore();
        store.seed();
        return store;
    }

    @Bean
    TestAdminAuthProvider adminAuthProvider() {
        return new TestAdminAuthProvider();
    }
}

@RestController
@RequestMapping("/api/v1/admin")
class AdminController {
    private final AdminStore store;
    private final TestAdminAuthProvider auth;

    AdminController(AdminStore store, TestAdminAuthProvider auth) {
        this.store = store;
        this.auth = auth;
    }

    @GetMapping("/overview")
    Map<String, Object> overview(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestParam Map<String, String> query,
                                 HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.overview(actor, query, request));
    }

    @GetMapping("/modules")
    Map<String, Object> modules(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @RequestParam Map<String, String> query,
                                HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(Map.of("items", store.modules(actor, query, request)));
    }

    @GetMapping("/modules/{moduleKey}")
    Map<String, Object> moduleDetail(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String moduleKey,
                                     HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.moduleDetail(actor, moduleKey, request));
    }

    @GetMapping("/todos")
    Map<String, Object> todos(@RequestHeader(value = "Authorization", required = false) String authorization,
                              @RequestParam Map<String, String> query,
                              HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.todos(actor, query, request));
    }

    @GetMapping("/todos/{todoId}")
    Map<String, Object> todoDetail(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String todoId,
                                   HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.todoDetail(actor, todoId, request));
    }

    @GetMapping("/metrics/summary")
    Map<String, Object> metrics(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @RequestParam Map<String, String> query,
                                HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(Map.of("items", store.metrics(actor, query, request)));
    }

    @GetMapping("/audit-logs")
    Map<String, Object> auditLogs(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @RequestParam Map<String, String> query,
                                  HttpServletRequest request) {
        auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.auditLogs(query, request));
    }

    @GetMapping("/settings")
    Map<String, Object> settings(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestParam Map<String, String> query) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.settings(actor, query));
    }

    @PatchMapping("/settings")
    Map<String, Object> patchSettings(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestBody(required = false) Map<String, Object> body,
                                      HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.patchSettings(actor, body == null ? Map.of() : body, request));
    }

    @GetMapping("/ops/summary")
    Map<String, Object> ops(@RequestHeader(value = "Authorization", required = false) String authorization,
                            HttpServletRequest request) {
        auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.ops(request));
    }

    private Map<String, Object> ok(Object data) {
        return AdminResponses.ok(data);
    }
}

class AdminStore {
    private static final String NOW = "2026-05-22T12:00:00Z";
    private static final Set<String> MODULE_KEYS = Set.of(
            "AUTH", "PROFILE", "NOTIFICATION", "CONTENT", "SERVER_STATUS", "RESOURCE", "ADMIN",
            "ONBOARDING", "EXAM", "WHITELIST", "ATTENDANCE", "COMMUNITY", "ACTIVITY", "CALENDAR",
            "CHANGELOG", "OPS_CONTROL", "NODE_DAEMON");
    private static final Set<String> IMPLEMENTED = Set.of("AUTH", "PROFILE", "NOTIFICATION", "CONTENT", "SERVER_STATUS", "RESOURCE", "ADMIN");
    private static final Set<String> NOT_IMPLEMENTED = Set.of("ONBOARDING", "EXAM", "WHITELIST", "ATTENDANCE", "COMMUNITY", "ACTIVITY", "CALENDAR", "CHANGELOG", "OPS_CONTROL", "NODE_DAEMON");
    private final Map<String, ModuleConfig> moduleConfigs = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> todoSeeds = new ArrayList<>();
    private final List<Map<String, Object>> auditSeeds = new ArrayList<>();
    private final Map<String, Object> layout = new ConcurrentHashMap<>();
    private final Map<String, Setting> settings = new ConcurrentHashMap<>();
    private final Map<String, IdempotencyRecord> idempotency = new ConcurrentHashMap<>();

    void seed() {
        int order = 10;
        for (String key : List.of("AUTH", "PROFILE", "NOTIFICATION", "CONTENT", "SERVER_STATUS", "RESOURCE", "ADMIN", "ONBOARDING", "EXAM", "WHITELIST", "ATTENDANCE", "COMMUNITY", "ACTIVITY", "CALENDAR", "CHANGELOG", "OPS_CONTROL", "NODE_DAEMON")) {
            moduleConfigs.put(key, new ModuleConfig(key, order, true));
            order += 10;
        }
        layout.put("dashboardCards", new ArrayList<>(List.of("todos", "metrics", "health")));
        layout.put("navigationModuleOrder", new ArrayList<>(List.of("AUTH", "PROFILE", "NOTIFICATION", "CONTENT", "SERVER_STATUS", "RESOURCE", "ADMIN")));
        layout.put("hiddenModules", new ArrayList<>());
        layout.put("quickActions", new ArrayList<>(List.of(Map.of("key", "content-review", "targetRoute", "/admin/content"))));
        settings.put("dashboard.refreshSeconds", new Setting("dashboard.refreshSeconds", "DASHBOARD", "INTEGER", 30, false, false, "Dashboard refresh interval."));
        settings.put("audit.retentionDays", new Setting("audit.retentionDays", "AUDIT", "INTEGER", 90, false, true, "Audit index retention days."));
        settings.put("navigation.showPlaceholders", new Setting("navigation.showPlaceholders", "NAVIGATION", "BOOLEAN", true, false, false, "Show not implemented module placeholders."));
        seedTodos();
        seedAudits();
    }

    Map<String, Object> overview(AuthUser actor, Map<String, String> query, HttpServletRequest request) {
        boolean includeDisabled = bool(query, "includeDisabled", false);
        if (includeDisabled && !actor.hasRole("OWNER")) {
            throw new AdminException(403, 42001, "role permission denied");
        }
        int moduleLimit = intRange(query, "moduleLimit", 50, 1, 50, 40001);
        int todoLimit = intRange(query, "todoLimit", 10, 0, 50, 40001);
        int auditLimit = intRange(query, "auditLimit", 10, 0, 50, 40001);
        List<Map<String, Object>> modules = modules(actor, Map.of("includeDisabled", Boolean.toString(includeDisabled), "includeNotImplemented", "true"), request);
        if (modules.size() > moduleLimit) {
            modules = new ArrayList<>(modules.subList(0, moduleLimit));
        }
        List<String> degraded = degradedModules(request);
        Map<String, Object> todoSummary = new LinkedHashMap<>();
        List<Map<String, Object>> todos = filteredTodos(actor, Map.of(), request);
        todoSummary.put("total", todos.size());
        todoSummary.put("critical", todos.stream().filter(todo -> "CRITICAL".equals(todo.get("severity"))).count());
        todoSummary.put("high", todos.stream().filter(todo -> "HIGH".equals(todo.get("severity"))).count());
        todoSummary.put("recent", todos.subList(0, Math.min(todoLimit, todos.size())));
        List<Map<String, Object>> metrics = metrics(actor, Map.of(), request);
        if (!actor.hasAny("ADMIN", "OWNER")) {
            metrics = metrics.stream()
                    .filter(metric -> !"ADMIN".equals(metric.get("sourceModule")))
                    .toList();
        }
        List<Map<String, Object>> recentAudits = actor.hasAny("ADMIN", "OWNER") ? auditRows(Map.of(), request) : List.of();
        if (recentAudits.size() > auditLimit) {
            recentAudits = new ArrayList<>(recentAudits.subList(0, auditLimit));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("modules", modules);
        data.put("todoSummary", todoSummary);
        data.put("metrics", metrics);
        data.put("recentAudits", recentAudits);
        data.put("degradedModules", degraded);
        data.put("notImplementedModules", new ArrayList<>(NOT_IMPLEMENTED));
        data.put("generatedAt", NOW);
        return data;
    }

    List<Map<String, Object>> modules(AuthUser actor, Map<String, String> query, HttpServletRequest request) {
        boolean includeDisabled = bool(query, "includeDisabled", false);
        if (includeDisabled && !actor.hasRole("OWNER")) {
            throw new AdminException(403, 42001, "role permission denied");
        }
        boolean includeNotImplemented = bool(query, "includeNotImplemented", true);
        String status = query.get("status");
        if (status != null && !Set.of("AVAILABLE", "DEGRADED", "UNAVAILABLE", "NOT_IMPLEMENTED", "DISABLED").contains(status)) {
            throw new AdminException(400, 40001, "invalid module status");
        }
        String sort = query.getOrDefault("sort", "sortOrder_asc");
        if (!Set.of("sortOrder_asc", "moduleKey_asc", "updatedAt_desc").contains(sort)) {
            throw new AdminException(400, 40003, "invalid sort");
        }
        String keyword = query.getOrDefault("keyword", "").toLowerCase();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ModuleConfig config : moduleConfigs.values()) {
            if (!includeNotImplemented && NOT_IMPLEMENTED.contains(config.key)) {
                continue;
            }
            if (!includeDisabled && (!config.enabled || hiddenModules().contains(config.key))) {
                continue;
            }
            Map<String, Object> row = moduleEntry(actor, config.key, request);
            if (status != null && !status.equals(row.get("status"))) {
                continue;
            }
            if (!keyword.isBlank() && !(config.key.toLowerCase().contains(keyword) || row.get("name").toString().toLowerCase().contains(keyword) || row.get("description").toString().toLowerCase().contains(keyword))) {
                continue;
            }
            rows.add(row);
        }
        Comparator<Map<String, Object>> comparator = switch (sort) {
            case "moduleKey_asc" -> Comparator.comparing(row -> row.get("moduleKey").toString());
            case "updatedAt_desc" -> Comparator.comparing((Map<String, Object> row) -> row.get("updatedAt").toString()).reversed();
            default -> Comparator.comparing(row -> ((Number) row.get("sortOrder")).intValue());
        };
        rows.sort(comparator.thenComparing(row -> row.get("moduleKey").toString()));
        return rows;
    }

    Map<String, Object> moduleDetail(AuthUser actor, String moduleKey, HttpServletRequest request) {
        if (!MODULE_KEYS.contains(moduleKey)) {
            throw new AdminException(400, 40001, "invalid module key");
        }
        ModuleConfig config = moduleConfigs.get(moduleKey);
        if (config == null) {
            throw new AdminException(404, 43700, "module not found");
        }
        if (!config.enabled && !actor.hasRole("OWNER")) {
            throw new AdminException(409, 43713, "module disabled");
        }
        return moduleEntry(actor, moduleKey, request);
    }

    Map<String, Object> todos(AuthUser actor, Map<String, String> query, HttpServletRequest request) {
        int page = page(query);
        int pageSize = pageSize(query);
        String sort = query.getOrDefault("sort", "severity_desc");
        if (!Set.of("severity_desc", "updatedAt_desc", "createdAt_desc", "sourceModule_asc").contains(sort)) {
            throw new AdminException(400, 40003, "invalid sort");
        }
        List<Map<String, Object>> rows = new ArrayList<>(filteredTodos(actor, query, request));
        rows.sort(todoComparator(sort));
        return page(rows, page, pageSize);
    }

    Map<String, Object> todoDetail(AuthUser actor, String todoId, HttpServletRequest request) {
        return filteredTodos(actor, Map.of(), request).stream()
                .filter(todo -> todoId.equals(todo.get("todoId")))
                .findFirst()
                .map(todo -> {
                    Map<String, Object> detail = new LinkedHashMap<>(todo);
                    detail.put("context", Map.of("sourceStatus", todo.get("status"), "nextRoute", todo.get("targetRoute"), "summary", todo.get("summary")));
                    return detail;
                })
                .orElseThrow(() -> new AdminException(404, 43701, "todo not found"));
    }

    List<Map<String, Object>> metrics(AuthUser actor, Map<String, String> query, HttpServletRequest request) {
        String source = query.get("sourceModule");
        if (source != null && !IMPLEMENTED.contains(source)) {
            throw new AdminException(400, 40001, "invalid source module");
        }
        boolean includeDegraded = bool(query, "includeDegraded", true);
        List<Map<String, Object>> metrics = new ArrayList<>();
        addMetric(metrics, "auth.usersTotal", "Users", "AUTH", 18, "/admin/auth/users", request);
        addMetric(metrics, "profile.membersTotal", "Members", "PROFILE", 8, "/admin/profile", request);
        addMetric(metrics, "notification.failedDeliveries", "Failed deliveries", "NOTIFICATION", 1, "/admin/notifications", request);
        addMetric(metrics, "content.pendingReview", "Pending content", "CONTENT", 2, "/admin/content", request);
        addMetric(metrics, "serverStatus.openOutages", "Open outages", "SERVER_STATUS", 1, "/admin/server-status", request);
        addMetric(metrics, "resource.pendingReview", "Pending resources", "RESOURCE", 2, "/admin/resources", request);
        addMetric(metrics, "admin.settingsTotal", "Settings", "ADMIN", settings.size(), "/admin", request);
        return metrics.stream()
                .filter(metric -> source == null || source.equals(metric.get("sourceModule")))
                .filter(metric -> includeDegraded || !Boolean.TRUE.equals(metric.get("degraded")))
                .toList();
    }

    Map<String, Object> auditLogs(Map<String, String> query, HttpServletRequest request) {
        int page = page(query);
        int pageSize = pageSize(query);
        String sort = query.getOrDefault("sort", "createdAt_desc");
        if (!Set.of("createdAt_desc", "createdAt_asc", "riskLevel_desc").contains(sort)) {
            throw new AdminException(400, 40003, "invalid sort");
        }
        if (query.containsKey("sourceModule") && !Set.of("ADMIN", "AUTH", "PROFILE", "NOTIFICATION", "CONTENT", "SERVER_STATUS", "RESOURCE").contains(query.get("sourceModule"))) {
            throw new AdminException(400, 40001, "invalid audit source");
        }
        if (query.containsKey("result") && !Set.of("SUCCESS", "FAILED").contains(query.get("result"))) {
            throw new AdminException(400, 40001, "invalid audit result");
        }
        validateTimeRange(query);
        List<Map<String, Object>> rows = new ArrayList<>(auditRows(query, request));
        Comparator<Map<String, Object>> comparator = "createdAt_asc".equals(sort)
                ? Comparator.comparing(row -> row.get("createdAt").toString())
                : Comparator.comparing((Map<String, Object> row) -> row.get("createdAt").toString()).reversed();
        rows.sort(comparator.thenComparing(row -> row.get("id").toString()));
        return page(rows, page, pageSize);
    }

    Map<String, Object> settings(AuthUser actor, Map<String, String> query) {
        String scope = query.get("scope");
        if (scope != null && !Set.of("GLOBAL", "MODULE", "DASHBOARD", "NAVIGATION", "AUDIT").contains(scope)) {
            throw new AdminException(400, 40001, "invalid setting scope");
        }
        boolean includeHighImpact = bool(query, "includeHighImpact", false);
        if (includeHighImpact && !actor.hasRole("OWNER")) {
            throw new AdminException(403, 42001, "role permission denied");
        }
        return settingsSnapshot(actor, scope, includeHighImpact, null);
    }

    Map<String, Object> patchSettings(AuthUser actor, Map<String, Object> body, HttpServletRequest request) {
        String reason = string(body, "reason", true);
        if (reason.length() > 200) {
            throw new AdminException(400, 40001, "reason too long");
        }
        String key = string(body, "idempotencyKey", true);
        if (key.length() < 8 || key.length() > 80) {
            throw new AdminException(400, 40001, "invalid idempotency key");
        }
        String fingerprint = canonical(body);
        String idemKey = actor.userId() + ":" + key;
        IdempotencyRecord existing = idempotency.get(idemKey);
        if (existing != null) {
            if (!existing.fingerprint().equals(fingerprint)) {
                throw new AdminException(409, 43712, "idempotency conflict");
            }
            Map<String, Object> replay = new LinkedHashMap<>(existing.response());
            replay.put("idempotency", Map.of("replayed", true));
            return replay;
        }
        if ("true".equals(request.getHeader("X-Test-Fail-Audit"))) {
            throw new AdminException(500, 51701, "admin audit write failed");
        }
        if ("true".equals(request.getHeader("X-Test-Fail-Settings"))) {
            throw new AdminException(500, 51702, "admin setting write failed");
        }
        boolean highImpact = highImpact(body);
        if (highImpact && !actor.hasRole("OWNER")) {
            throw new AdminException(403, 42001, "role permission denied");
        }
        applySettings(body, actor);
        auditSeeds.add(0, audit("audit-admin-" + UUID.randomUUID(), "ADMIN", "ADMIN_SETTINGS_UPDATED", actor.userId(), "ADMIN_SETTING", "settings", "MEDIUM", "SUCCESS"));
        Map<String, Object> response = settingsSnapshot(actor, null, actor.hasRole("OWNER"), Map.of("replayed", false));
        idempotency.put(idemKey, new IdempotencyRecord(fingerprint, response));
        return response;
    }

    Map<String, Object> ops(HttpServletRequest request) {
        List<Map<String, Object>> health = MODULE_KEYS.stream().map(key -> health(key, request)).toList();
        long degraded = health.stream().filter(row -> "DEGRADED".equals(row.get("status")) || "UNAVAILABLE".equals(row.get("status"))).count();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "admin");
        data.put("port", 8107);
        data.put("storageMode", "IN_MEMORY");
        data.put("authMode", "TEST_STUB");
        data.put("moduleAdapterMode", "TEST_STUB");
        data.put("modulesTotal", MODULE_KEYS.size());
        data.put("availableModulesTotal", IMPLEMENTED.size());
        data.put("degradedModulesTotal", degraded);
        data.put("notImplementedModulesTotal", NOT_IMPLEMENTED.size());
        data.put("todosIndexedTotal", todoSeeds.size());
        data.put("auditIndexesTotal", auditSeeds.size());
        data.put("settingsTotal", settings.size());
        data.put("idempotencyRecordsTotal", idempotency.size());
        data.put("lastAggregatedAt", NOW);
        data.put("productionGaps", List.of("persistent storage not enabled", "real auth adapter not enabled", "real module HTTP adapters not enabled", "real audit index sync not enabled", "scheduled aggregation not enabled"));
        data.put("moduleHealth", health);
        return data;
    }

    private Map<String, Object> moduleEntry(AuthUser actor, String moduleKey, HttpServletRequest request) {
        ModuleConfig config = moduleConfigs.get(moduleKey);
        Map<String, Object> health = health(moduleKey, request);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("moduleKey", moduleKey);
        row.put("name", moduleName(moduleKey));
        row.put("description", moduleName(moduleKey) + " management entry.");
        boolean disabled = !config.enabled || hiddenModules().contains(moduleKey);
        row.put("status", disabled ? "DISABLED" : health.get("status"));
        row.put("implemented", IMPLEMENTED.contains(moduleKey));
        row.put("enabled", !disabled);
        row.put("requiredRoles", requiredRoles(moduleKey));
        row.put("requiredPermissions", requiredPermissions(moduleKey));
        row.put("frontendRoute", "/admin/" + moduleKey.toLowerCase().replace('_', '-'));
        row.put("targetApiBase", IMPLEMENTED.contains(moduleKey) ? targetApi(moduleKey) : null);
        row.put("sortOrder", config.sortOrder);
        row.put("badgeCount", badgeCount(moduleKey));
        row.put("capabilities", capabilities(actor, moduleKey, health));
        row.put("health", health);
        row.put("updatedAt", NOW);
        return row;
    }

    private List<Map<String, Object>> capabilities(AuthUser actor, String moduleKey, Map<String, Object> health) {
        List<Map<String, Object>> rows = new ArrayList<>();
        boolean available = "AVAILABLE".equals(health.get("status"));
        rows.add(capability(moduleKey.toLowerCase() + ".entry", NOT_IMPLEMENTED.contains(moduleKey) ? "OPS_PLACEHOLDER" : "ENTRY", "Entry", "/admin/" + moduleKey.toLowerCase().replace('_', '-'), targetApiOrNull(moduleKey), requiredRoles(moduleKey), requiredPermissions(moduleKey), "LOW", available, true));
        if (IMPLEMENTED.contains(moduleKey)) {
            rows.add(capability(moduleKey.toLowerCase() + ".read", "READ", "Read", "/admin/" + moduleKey.toLowerCase().replace('_', '-'), targetApi(moduleKey), List.of("HELPER", "ADMIN", "OWNER"), List.of(), "LOW", available, true));
        }
        if (actor.hasAny("ADMIN", "OWNER") && IMPLEMENTED.contains(moduleKey) && !"ADMIN".equals(moduleKey)) {
            rows.add(capability(moduleKey.toLowerCase() + ".source", "WRITE", "Source service", "/admin/" + moduleKey.toLowerCase().replace('_', '-'), targetApi(moduleKey), List.of("ADMIN", "OWNER"), List.of(), "MEDIUM", available, true));
        }
        return rows;
    }

    private Map<String, Object> capability(String key, String type, String label, String route, String api, List<String> roles, List<String> permissions, String risk, boolean available, boolean readOnly) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("type", type);
        row.put("label", label);
        row.put("targetRoute", route);
        row.put("targetApi", api);
        row.put("requiredRoles", roles);
        row.put("requiredPermissions", permissions);
        row.put("riskLevel", risk);
        row.put("available", available);
        row.put("readOnly", readOnly);
        return row;
    }

    private Map<String, Object> health(String moduleKey, HttpServletRequest request) {
        String status = NOT_IMPLEMENTED.contains(moduleKey) ? "NOT_IMPLEMENTED" : "AVAILABLE";
        Map<String, String> modes = moduleModes(request);
        if (modes.containsKey(moduleKey)) {
            String mode = modes.get(moduleKey);
            status = "TIMEOUT".equals(mode) ? "UNAVAILABLE" : "DEGRADED";
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("moduleKey", moduleKey);
        row.put("status", status);
        row.put("service", moduleKey.toLowerCase().replace('_', '-'));
        row.put("port", port(moduleKey));
        row.put("storageMode", IMPLEMENTED.contains(moduleKey) ? "IN_MEMORY" : null);
        row.put("authMode", IMPLEMENTED.contains(moduleKey) ? "TEST_STUB" : null);
        row.put("lastCheckedAt", NOW);
        row.put("latencyMs", "UNAVAILABLE".equals(status) ? null : 3);
        row.put("degraded", "DEGRADED".equals(status) || "UNAVAILABLE".equals(status));
        row.put("degradeReason", row.get("degraded").equals(Boolean.TRUE) ? moduleKey + " adapter degraded" : null);
        row.put("productionGaps", IMPLEMENTED.contains(moduleKey) ? List.of("real persistence not enabled", "real module adapter not enabled") : List.of("module not implemented"));
        return row;
    }

    private List<Map<String, Object>> filteredTodos(AuthUser actor, Map<String, String> query, HttpServletRequest request) {
        String source = query.get("sourceModule");
        if (source != null && !IMPLEMENTED.contains(source)) {
            throw new AdminException(400, 40001, "invalid todo source");
        }
        String type = query.get("type");
        if (type != null && !Set.of("REVIEW", "CONFIG", "FAILURE", "HEALTH", "SECURITY", "FOLLOW_UP").contains(type)) {
            throw new AdminException(400, 40001, "invalid todo type");
        }
        String severity = query.get("severity");
        if (severity != null && !Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(severity)) {
            throw new AdminException(400, 40001, "invalid severity");
        }
        String status = query.get("status");
        if (status != null && !Set.of("OPEN", "READ_ONLY", "SOURCE_UNAVAILABLE", "STALE").contains(status)) {
            throw new AdminException(400, 40001, "invalid todo status");
        }
        String keyword = query.getOrDefault("keyword", "").toLowerCase();
        List<Map<String, Object>> rows = new ArrayList<>(todoSeeds);
        for (String degraded : degradedModules(request)) {
            rows.add(todo("todo-" + degraded.toLowerCase() + "-unavailable", degraded, degraded + "_UNAVAILABLE", degraded.toLowerCase() + "-unavailable", "HEALTH", "HIGH", "SOURCE_UNAVAILABLE", moduleName(degraded) + " unavailable", moduleName(degraded) + " adapter degraded.", "/admin/" + degraded.toLowerCase().replace('_', '-'), targetApiOrNull(degraded)));
        }
        return rows.stream()
                .filter(todo -> source == null || source.equals(todo.get("sourceModule")))
                .filter(todo -> type == null || type.equals(todo.get("type")))
                .filter(todo -> severity == null || severity.equals(todo.get("severity")))
                .filter(todo -> status == null || status.equals(todo.get("status")))
                .filter(todo -> keyword.isBlank() || todo.get("title").toString().toLowerCase().contains(keyword) || todo.get("summary").toString().toLowerCase().contains(keyword) || todo.get("sourceId").toString().toLowerCase().contains(keyword))
                .toList();
    }

    private List<Map<String, Object>> auditRows(Map<String, String> query, HttpServletRequest request) {
        List<Map<String, Object>> rows = new ArrayList<>(auditSeeds);
        for (String degraded : degradedModules(request)) {
            rows.add(audit("audit-" + degraded.toLowerCase() + "-degraded", degraded, "MODULE_ADAPTER_DEGRADED", "admin", "MODULE", degraded, "LOW", "FAILED"));
        }
        return rows.stream()
                .filter(row -> query.get("sourceModule") == null || query.get("sourceModule").equals(row.get("sourceModule")))
                .filter(row -> query.get("actorUserId") == null || query.get("actorUserId").equals(row.get("actorUserId")))
                .filter(row -> query.get("action") == null || query.get("action").equals(row.get("action")))
                .filter(row -> query.get("result") == null || query.get("result").equals(row.get("result")))
                .filter(row -> query.get("riskLevel") == null || query.get("riskLevel").equals(row.get("riskLevel")))
                .filter(row -> query.get("targetType") == null || query.get("targetType").equals(row.get("targetType")))
                .filter(row -> query.get("targetId") == null || query.get("targetId").equals(row.get("targetId")))
                .filter(row -> inTimeRange(row, query))
                .toList();
    }

    private Map<String, Object> settingsSnapshot(AuthUser actor, String scope, boolean includeHighImpact, Map<String, Object> idem) {
        List<Map<String, Object>> items = settings.values().stream()
                .filter(setting -> scope == null || scope.equals(setting.scope))
                .filter(setting -> includeHighImpact || !setting.highImpact)
                .sorted(Comparator.comparing(setting -> setting.key))
                .map(Setting::toMap)
                .toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("layout", layoutSnapshot(actor));
        data.put("modules", modules(actor, Map.of("includeNotImplemented", "true"), currentRequest()));
        data.put("updatedAt", NOW);
        if (idem != null) {
            data.put("idempotency", idem);
        }
        return data;
    }

    private Map<String, Object> layoutSnapshot(AuthUser actor) {
        Map<String, Object> snapshot = new LinkedHashMap<>(layout);
        Object quickActions = snapshot.get("quickActions");
        if (quickActions instanceof List<?> list) {
            snapshot.put("quickActions", list.stream()
                    .filter(item -> item instanceof Map<?, ?> action && quickActionVisible(action, actor))
                    .map(item -> {
                        Map<?, ?> action = (Map<?, ?>) item;
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("key", action.get("key"));
                        row.put("targetRoute", action.get("targetRoute"));
                        return row;
                    })
                    .toList());
        }
        return snapshot;
    }

    private void applySettings(Map<String, Object> body, AuthUser actor) {
        Object itemsObj = body.get("items");
        if (itemsObj instanceof List<?> items) {
            for (Object raw : items) {
                if (!(raw instanceof Map<?, ?> item)) {
                    throw new AdminException(400, 40001, "invalid setting item");
                }
                String key = Objects.toString(item.get("key"), "");
                Setting setting = settings.get(key);
                if (setting == null) {
                    throw new AdminException(404, 43700, "setting not found");
                }
                Object value = item.get("value");
                if ("INTEGER".equals(setting.valueType) && !(value instanceof Number)) {
                    throw new AdminException(400, 40001, "invalid setting type");
                }
                if ("BOOLEAN".equals(setting.valueType) && !(value instanceof Boolean)) {
                    throw new AdminException(400, 40001, "invalid setting type");
                }
                setting.value = value;
                setting.updatedBy = actor.userId();
                setting.updatedAt = NOW;
            }
        }
        Object layoutObj = body.get("layout");
        if (layoutObj instanceof Map<?, ?> patch) {
            if (patch.containsKey("navigationModuleOrder")) {
                List<String> order = stringList(patch.get("navigationModuleOrder"));
                layout.put("navigationModuleOrder", order);
            }
            if (patch.containsKey("hiddenModules")) {
                List<String> hidden = stringList(patch.get("hiddenModules"));
                if (hidden.containsAll(IMPLEMENTED)) {
                    throw new AdminException(409, 43710, "cannot hide all implemented modules");
                }
                layout.put("hiddenModules", hidden);
            }
            if (patch.containsKey("dashboardCards")) {
                layout.put("dashboardCards", stringList(patch.get("dashboardCards")));
            }
            if (patch.containsKey("quickActions")) {
                layout.put("quickActions", quickActions(patch.get("quickActions"), actor));
            }
        }
    }

    private List<Map<String, Object>> quickActions(Object value, AuthUser actor) {
        if (!(value instanceof List<?> list)) {
            throw new AdminException(400, 40001, "invalid quick actions");
        }
        Set<String> keys = new HashSet<>();
        List<Map<String, Object>> actions = new ArrayList<>();
        for (Object raw : list) {
            if (!(raw instanceof Map<?, ?> action)) {
                throw new AdminException(400, 40001, "invalid quick action");
            }
            Object keyValue = action.get("key");
            Object routeValue = action.get("targetRoute");
            if (!(keyValue instanceof String key) || !key.matches("[a-z0-9-]{1,80}") || !keys.add(key)) {
                throw new AdminException(400, 40001, "invalid quick action key");
            }
            if (!(routeValue instanceof String route) || route.isBlank() || route.startsWith("/api") || route.startsWith("http://") || route.startsWith("https://")) {
                throw new AdminException(400, 40001, "invalid quick action route");
            }
            String moduleKey = moduleKeyForRoute(route);
            if (moduleKey == null || !quickActionVisible(action, actor)) {
                throw new AdminException(409, 43713, "quick action unavailable");
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", key);
            row.put("targetRoute", route);
            actions.add(row);
        }
        return actions;
    }

    private boolean quickActionVisible(Map<?, ?> action, AuthUser actor) {
        Object routeValue = action.get("targetRoute");
        if (!(routeValue instanceof String route)) {
            return false;
        }
        String moduleKey = moduleKeyForRoute(route);
        return moduleKey != null
                && IMPLEMENTED.contains(moduleKey)
                && !hiddenModules().contains(moduleKey)
                && actor.hasAny(requiredRoles(moduleKey).toArray(String[]::new));
    }

    private String moduleKeyForRoute(String route) {
        if (!route.startsWith("/admin/")) {
            return null;
        }
        for (String moduleKey : MODULE_KEYS) {
            String moduleRoute = "/admin/" + moduleKey.toLowerCase().replace('_', '-');
            if (route.equals(moduleRoute) || route.startsWith(moduleRoute + "/")) {
                return moduleKey;
            }
        }
        return null;
    }

    private boolean inTimeRange(Map<String, Object> row, Map<String, String> query) {
        Instant createdAt = Instant.parse(row.get("createdAt").toString());
        if (query.containsKey("from") && createdAt.isBefore(Instant.parse(query.get("from")))) {
            return false;
        }
        return !query.containsKey("to") || !createdAt.isAfter(Instant.parse(query.get("to")));
    }

    private boolean highImpact(Map<String, Object> body) {
        Object itemsObj = body.get("items");
        if (itemsObj instanceof List<?> items) {
            for (Object raw : items) {
                if (raw instanceof Map<?, ?> item) {
                    Setting setting = settings.get(Objects.toString(item.get("key"), ""));
                    if (setting != null && setting.highImpact) {
                        return true;
                    }
                }
            }
        }
        Object layoutObj = body.get("layout");
        if (layoutObj instanceof Map<?, ?> patch && patch.containsKey("hiddenModules")) {
            return stringList(patch.get("hiddenModules")).stream().anyMatch(key -> Set.of("AUTH", "ADMIN").contains(key));
        }
        return false;
    }

    private void addMetric(List<Map<String, Object>> rows, String key, String label, String module, int value, String route, HttpServletRequest request) {
        boolean degraded = degradedModules(request).contains(module);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("metricKey", key);
        row.put("label", label);
        row.put("sourceModule", module);
        row.put("value", degraded ? 0 : value);
        row.put("unit", "count");
        row.put("trend", null);
        row.put("targetRoute", route);
        row.put("degraded", degraded);
        row.put("updatedAt", NOW);
        rows.add(row);
    }

    private Map<String, Object> page(List<Map<String, Object>> rows, int page, int pageSize) {
        int from = Math.min((page - 1) * pageSize, rows.size());
        int to = Math.min(from + pageSize, rows.size());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", new ArrayList<>(rows.subList(from, to)));
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("total", rows.size());
        return data;
    }

    private int page(Map<String, String> query) {
        return intRange(query, "page", 1, 1, Integer.MAX_VALUE, 40002);
    }

    private int pageSize(Map<String, String> query) {
        return intRange(query, "pageSize", 20, 1, 100, 40002);
    }

    private int intRange(Map<String, String> query, String key, int defaultValue, int min, int max, int code) {
        if (!query.containsKey(key)) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(query.get(key));
            if (value < min || value > max) {
                throw new AdminException(400, code, "invalid number range");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new AdminException(400, code, "invalid number");
        }
    }

    private boolean bool(Map<String, String> query, String key, boolean defaultValue) {
        if (!query.containsKey(key)) {
            return defaultValue;
        }
        String value = query.get(key);
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        throw new AdminException(400, 40001, "invalid boolean");
    }

    private String string(Map<String, Object> body, String key, boolean required) {
        Object value = body.get(key);
        if (value == null) {
            if (required) {
                throw new AdminException(400, 40001, key + " is required");
            }
            return "";
        }
        if (!(value instanceof String text) || text.isBlank()) {
            throw new AdminException(400, 40001, "invalid string");
        }
        return text;
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            throw new AdminException(400, 40001, "invalid list");
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            String text = Objects.toString(item, "");
            if (!MODULE_KEYS.contains(text) && !Set.of("todos", "metrics", "health").contains(text)) {
                throw new AdminException(400, 40001, "invalid list item");
            }
            result.add(text);
        }
        return result;
    }

    private void validateTimeRange(Map<String, String> query) {
        Instant from = null;
        Instant to = null;
        try {
            if (query.containsKey("from")) {
                from = Instant.parse(query.get("from"));
            }
            if (query.containsKey("to")) {
                to = Instant.parse(query.get("to"));
            }
        } catch (DateTimeParseException exception) {
            throw new AdminException(400, 40001, "invalid time");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new AdminException(400, 40001, "invalid time range");
        }
    }

    private Comparator<Map<String, Object>> todoComparator(String sort) {
        return switch (sort) {
            case "updatedAt_desc" -> Comparator.comparing((Map<String, Object> row) -> row.get("updatedAt").toString()).reversed();
            case "createdAt_desc" -> Comparator.comparing((Map<String, Object> row) -> row.get("createdAt").toString()).reversed();
            case "sourceModule_asc" -> Comparator.comparing(row -> row.get("sourceModule").toString());
            default -> Comparator.comparingInt((Map<String, Object> row) -> severityRank(row.get("severity").toString())).reversed()
                    .thenComparing((Map<String, Object> row) -> row.get("updatedAt").toString(), Comparator.reverseOrder());
        };
    }

    private int severityRank(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }

    private String targetApiOrNull(String key) {
        return IMPLEMENTED.contains(key) ? targetApi(key) : null;
    }

    private String targetApi(String key) {
        return switch (key) {
            case "AUTH" -> "/api/v1/auth/admin";
            case "PROFILE" -> "/api/v1/profile/admin";
            case "NOTIFICATION" -> "/api/v1/notifications/admin";
            case "CONTENT" -> "/api/v1/content";
            case "SERVER_STATUS" -> "/api/v1/server-status/admin";
            case "RESOURCE" -> "/api/v1/resources";
            case "ADMIN" -> "/api/v1/admin";
            default -> null;
        };
    }

    private String moduleName(String key) {
        return switch (key) {
            case "SERVER_STATUS" -> "Server Status";
            case "NODE_DAEMON" -> "Node Daemon";
            case "OPS_CONTROL" -> "Ops Control";
            default -> key.charAt(0) + key.substring(1).toLowerCase().replace('_', ' ');
        };
    }

    private Integer port(String key) {
        return switch (key) {
            case "AUTH" -> 8101;
            case "PROFILE" -> 8102;
            case "NOTIFICATION" -> 8103;
            case "CONTENT" -> 8104;
            case "SERVER_STATUS" -> 8105;
            case "RESOURCE" -> 8106;
            case "ADMIN" -> 8107;
            default -> null;
        };
    }

    private int badgeCount(String key) {
        return (int) todoSeeds.stream().filter(todo -> key.equals(todo.get("sourceModule"))).count();
    }

    private List<String> requiredRoles(String key) {
        if (Set.of("OPS_CONTROL", "NODE_DAEMON").contains(key)) {
            return List.of("OWNER");
        }
        return List.of("HELPER", "ADMIN", "OWNER");
    }

    private List<String> requiredPermissions(String key) {
        if (Set.of("OPS_CONTROL", "NODE_DAEMON").contains(key)) {
            return List.of("NODE_READ");
        }
        return List.of();
    }

    private List<String> degradedModules(HttpServletRequest request) {
        return new ArrayList<>(moduleModes(request).keySet());
    }

    private Set<String> hiddenModules() {
        Object value = layout.get("hiddenModules");
        if (!(value instanceof List<?> list)) {
            return Set.of();
        }
        Set<String> hidden = new HashSet<>();
        for (Object item : list) {
            String key = Objects.toString(item, "");
            if (MODULE_KEYS.contains(key)) {
                hidden.add(key);
            }
        }
        return hidden;
    }

    private Map<String, String> moduleModes(HttpServletRequest request) {
        if (request == null || request.getHeader("X-Test-Module-Mode") == null) {
            return Map.of();
        }
        Map<String, String> modes = new LinkedHashMap<>();
        for (String pair : request.getHeader("X-Test-Module-Mode").split(",")) {
            String[] parts = pair.split(":");
            if (parts.length == 2 && MODULE_KEYS.contains(parts[0])) {
                modes.put(parts[0], parts[1]);
            }
        }
        return modes;
    }

    private String canonical(Object value) {
        if (value instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder("{");
            map.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> Objects.toString(entry.getKey())))
                    .forEach(entry -> builder
                            .append(Objects.toString(entry.getKey()))
                            .append('=')
                            .append(canonical(entry.getValue()))
                            .append(';'));
            return builder.append('}').toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder builder = new StringBuilder("[");
            for (Object item : list) {
                builder.append(canonical(item)).append(';');
            }
            return builder.append(']').toString();
        }
        if (value instanceof String text) {
            return "s:" + text;
        }
        if (value instanceof Number number) {
            return "n:" + number;
        }
        if (value instanceof Boolean bool) {
            return "b:" + bool;
        }
        if (value == null) {
            return "null";
        }
        return "o:" + Objects.toString(value);
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private void seedTodos() {
        todoSeeds.add(todo("todo-content-review-1", "CONTENT", "CONTENT_REVIEW", "content-1", "REVIEW", "HIGH", "READ_ONLY", "Content pending review", "Review content item.", "/admin/content", "/api/v1/content/admin/items/content-1"));
        todoSeeds.add(todo("todo-content-config-1", "CONTENT", "CONTENT_HOME_DRAFT", "home-config", "CONFIG", "MEDIUM", "READ_ONLY", "Homepage draft not published", "Publish homepage config from source module.", "/admin/content/home", "/api/v1/content/admin/homepage"));
        todoSeeds.add(todo("todo-resource-review-1", "RESOURCE", "RESOURCE_REVIEW", "resource-1", "REVIEW", "HIGH", "READ_ONLY", "Resource pending review", "Review resource item.", "/admin/resources", "/api/v1/resources/admin/items/resource-1"));
        todoSeeds.add(todo("todo-resource-entry-1", "RESOURCE", "RESOURCE_DOWNLOAD_ENTRY", "download-entry-1", "FAILURE", "MEDIUM", "READ_ONLY", "Resource download entry expired", "Refresh source resource entry.", "/admin/resources", "/api/v1/resources/admin/items/resource-1"));
        todoSeeds.add(todo("todo-notification-failed-1", "NOTIFICATION", "NOTIFICATION_FAILED", "message-1", "FAILURE", "MEDIUM", "READ_ONLY", "Notification delivery failed", "Inspect delivery summary.", "/admin/notifications", "/api/v1/notifications/admin/messages/message-1"));
        todoSeeds.add(todo("todo-status-outage-1", "SERVER_STATUS", "SERVER_OUTAGE", "outage-1", "HEALTH", "HIGH", "READ_ONLY", "Server outage pending confirmation", "Confirm outage in status service.", "/admin/server-status", "/api/v1/server-status/admin/outages/outage-1"));
        todoSeeds.add(todo("todo-profile-activation-1", "PROFILE", "PROFILE_ACTIVATION", "member-1", "FOLLOW_UP", "LOW", "READ_ONLY", "Profile pending activation", "Activate member in profile service.", "/admin/profile", "/api/v1/profile/admin/members/member-1"));
        todoSeeds.add(todo("todo-auth-security-1", "AUTH", "AUTH_INVITATION_RISK", "invitation-1", "SECURITY", "CRITICAL", "READ_ONLY", "Admin invitation risk", "Inspect invitation security summary.", "/admin/auth", "/api/v1/auth/admin/invitations"));
    }

    private Map<String, Object> todo(String id, String module, String sourceType, String sourceId, String type, String severity, String status, String title, String summary, String route, String api) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("todoId", id);
        row.put("sourceModule", module);
        row.put("sourceType", sourceType);
        row.put("sourceId", sourceId);
        row.put("type", type);
        row.put("severity", severity);
        row.put("status", status);
        row.put("title", title);
        row.put("summary", summary);
        row.put("targetRoute", route);
        row.put("targetApi", api);
        row.put("readOnly", true);
        row.put("createdAt", NOW);
        row.put("updatedAt", NOW);
        row.put("indexedAt", NOW);
        return row;
    }

    private void seedAudits() {
        auditSeeds.add(audit("audit-admin-1", "ADMIN", "ADMIN_SETTINGS_UPDATED", "admin", "ADMIN_SETTING", "settings", "MEDIUM", "SUCCESS"));
        auditSeeds.add(audit("audit-auth-1", "AUTH", "AUTH_INVITATION_CREATED", "admin", "INVITATION", "invitation-1", "MEDIUM", "SUCCESS"));
        auditSeeds.add(audit("audit-profile-1", "PROFILE", "PROFILE_MEMBER_ACTIVATED", "admin", "MEMBER", "member-1", "MEDIUM", "SUCCESS"));
        auditSeeds.add(audit("audit-notification-1", "NOTIFICATION", "NOTIFICATION_DELIVERY_FAILED", "admin", "MESSAGE", "message-1", "LOW", "FAILED"));
        auditSeeds.add(audit("audit-content-1", "CONTENT", "CONTENT_REVIEWED", "admin", "CONTENT", "content-1", "MEDIUM", "SUCCESS"));
        auditSeeds.add(audit("audit-status-1", "SERVER_STATUS", "SERVER_OUTAGE_CONFIRMED", "admin", "OUTAGE", "outage-1", "LOW", "SUCCESS"));
        auditSeeds.add(audit("audit-resource-1", "RESOURCE", "RESOURCE_REVIEWED", "admin", "RESOURCE", "resource-1", "MEDIUM", "SUCCESS"));
    }

    private Map<String, Object> audit(String id, String source, String action, String actor, String targetType, String targetId, String risk, String result) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("sourceModule", source);
        row.put("sourceAuditId", id.replace("audit-", "source-audit-"));
        row.put("requestId", "req-" + id);
        row.put("actorUserId", actor);
        row.put("actorDisplayName", actor);
        row.put("actorRole", "ADMIN");
        row.put("targetType", targetType);
        row.put("targetId", targetId);
        row.put("action", action);
        row.put("riskLevel", risk);
        row.put("result", result);
        row.put("reasonSummary", "sanitized reason");
        row.put("failureReason", "FAILED".equals(result) ? "sanitized failure" : null);
        row.put("targetRoute", "/admin/" + source.toLowerCase().replace('_', '-'));
        row.put("indexedAt", NOW);
        row.put("createdAt", NOW);
        return row;
    }
}

class TestAdminAuthProvider {
    AuthUser requireAny(String authorization, String... roles) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AdminException(401, 41000, "unauthorized");
        }
        String token = authorization.substring("Bearer ".length());
        if (Set.of("disabled-token", "banned-token", "deleted-token", "auth-unavailable-token").contains(token)) {
            throw new AdminException(502, 46703, "auth context unavailable");
        }
        if ("auth-timeout-token".equals(token)) {
            throw new AdminException(504, 46704, "auth context timeout");
        }
        AuthUser user = switch (token) {
            case "owner-token" -> new AuthUser("owner", Set.of("OWNER"));
            case "admin-token" -> new AuthUser("admin", Set.of("ADMIN"));
            case "helper-token" -> new AuthUser("helper", Set.of("HELPER"));
            case "user-token" -> new AuthUser("user", Set.of("USER"));
            default -> throw new AdminException(401, 41001, "invalid session");
        };
        if (!user.hasAny(roles)) {
            throw new AdminException(403, 42001, "role permission denied");
        }
        return user;
    }
}

record AuthUser(String userId, Set<String> roles) {
    boolean hasRole(String role) {
        return roles.contains(role);
    }

    boolean hasAny(String... candidates) {
        for (String candidate : candidates) {
            if (roles.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}

class ModuleConfig {
    final String key;
    final int sortOrder;
    final boolean enabled;

    ModuleConfig(String key, int sortOrder, boolean enabled) {
        this.key = key;
        this.sortOrder = sortOrder;
        this.enabled = enabled;
    }
}

class Setting {
    final String key;
    final String scope;
    final String valueType;
    Object value;
    final boolean sensitive;
    final boolean highImpact;
    final String description;
    String updatedBy;
    String updatedAt = "2026-05-22T12:00:00Z";

    Setting(String key, String scope, String valueType, Object value, boolean sensitive, boolean highImpact, String description) {
        this.key = key;
        this.scope = scope;
        this.valueType = valueType;
        this.value = value;
        this.sensitive = sensitive;
        this.highImpact = highImpact;
        this.description = description;
    }

    Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("scope", scope);
        row.put("valueType", valueType);
        row.put("value", sensitive ? "***" : value);
        row.put("description", description);
        row.put("sensitive", sensitive);
        row.put("highImpact", highImpact);
        row.put("updatedBy", updatedBy);
        row.put("updatedAt", updatedAt);
        return row;
    }
}

record IdempotencyRecord(String fingerprint, Map<String, Object> response) {
}

class TreeStableMap extends LinkedHashMap<String, String> {
    TreeStableMap(Map<?, ?> source) {
        source.keySet().stream()
                .map(Objects::toString)
                .sorted()
                .forEach(key -> put(key, Objects.toString(source.get(key))));
    }
}

class AdminException extends RuntimeException {
    final int httpStatus;
    final int code;

    AdminException(int httpStatus, int code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }
}

class AdminResponses {
    static Map<String, Object> ok(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 0);
        body.put("message", "success");
        body.put("data", data);
        body.put("requestId", requestId());
        return body;
    }

    static Map<String, Object> error(int code, String message, Object errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", null);
        if (errors != null) {
            body.put("errors", errors);
        }
        body.put("requestId", requestId());
        return body;
    }

    static String requestId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "req-" + UUID.randomUUID();
        }
        Object requestId = attributes.getRequest().getAttribute("requestId");
        return requestId == null ? "req-" + UUID.randomUUID() : requestId.toString();
    }
}

@RestControllerAdvice
class AdminExceptionHandler {
    @ExceptionHandler(AdminException.class)
    ResponseEntity<Map<String, Object>> handleAdmin(AdminException exception) {
        return ResponseEntity.status(exception.httpStatus)
                .body(AdminResponses.error(exception.code, exception.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AdminResponses.error(51700, "admin internal error", null));
    }
}

@Configuration
class AdminRequestIdFilterConfig {
    @Bean
    AdminRequestIdFilter adminRequestIdFilter() {
        return new AdminRequestIdFilter();
    }
}

class AdminRequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = "req-" + UUID.randomUUID();
        }
        request.setAttribute("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        filterChain.doFilter(request, response);
    }
}
