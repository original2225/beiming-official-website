package cn.beiming.admin;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
    AdminStore adminStore(@Value("${beiming.admin.test-mode:false}") boolean testMode) {
        AdminStore store = new AdminStore(testMode);
        store.seed();
        return store;
    }

    @Bean
    TestAdminAuthProvider adminAuthProvider(@Value("${beiming.admin.test-mode:false}") boolean testMode) {
        return new TestAdminAuthProvider(testMode);
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
        AuthUser actor = auth.requireAny(request, authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.overview(actor, query, request));
    }

    @GetMapping("/modules")
    Map<String, Object> modules(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestParam Map<String, String> query,
                                 HttpServletRequest request) {
        AuthUser actor = auth.requireAny(request, authorization, "HELPER", "ADMIN", "OWNER");
        return ok(Map.of("items", store.modules(actor, query, request)));
    }

    @GetMapping("/modules/{moduleKey}")
    Map<String, Object> moduleDetail(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable String moduleKey,
                                      HttpServletRequest request) {
        AuthUser actor = auth.requireAny(request, authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.moduleDetail(actor, moduleKey, request));
    }

    @GetMapping("/todos")
    Map<String, Object> todos(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @RequestParam Map<String, String> query,
                               HttpServletRequest request) {
        AuthUser actor = auth.requireAny(request, authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.todos(actor, query, request));
    }

    @GetMapping("/todos/{todoId}")
    Map<String, Object> todoDetail(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String todoId,
                                    HttpServletRequest request) {
        AuthUser actor = auth.requireAny(request, authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.todoDetail(actor, todoId, request));
    }

    @GetMapping("/metrics/summary")
    Map<String, Object> metrics(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestParam Map<String, String> query,
                                 HttpServletRequest request) {
        AuthUser actor = auth.requireAny(request, authorization, "HELPER", "ADMIN", "OWNER");
        return ok(Map.of("items", store.metrics(actor, query, request)));
    }

    @GetMapping("/audit-logs")
    Map<String, Object> auditLogs(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @RequestParam Map<String, String> query,
                                   HttpServletRequest request) {
        auth.requireAny(request, authorization, "ADMIN", "OWNER");
        return ok(store.auditLogs(query, request));
    }

    @GetMapping("/settings")
    Map<String, Object> settings(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @RequestParam Map<String, String> query,
                                  HttpServletRequest request) {
        AuthUser actor = auth.requireAny(request, authorization, "ADMIN", "OWNER");
        return ok(store.settings(actor, query));
    }

    @PatchMapping("/settings")
    Map<String, Object> patchSettings(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestBody(required = false) Map<String, Object> body,
                                       HttpServletRequest request) {
        AuthUser actor = auth.requireAny(request, authorization, "ADMIN", "OWNER");
        return ok(store.patchSettings(actor, body == null ? Map.of() : body, request));
    }

    @GetMapping("/ops/summary")
    Map<String, Object> ops(@RequestHeader(value = "Authorization", required = false) String authorization,
                             HttpServletRequest request) {
        AuthUser actor = auth.requireAny(request, authorization, "ADMIN", "OWNER");
        return ok(store.ops(actor, request));
    }

    private Map<String, Object> ok(Object data) {
        return AdminResponses.ok(data);
    }
}

class AdminStore {
    private static final String NOW = "2026-05-22T12:00:00Z";
    private static final List<String> MODULE_KEYS = List.of(
            "AUTH", "PROFILE", "NOTIFICATION", "CONTENT", "SERVER_STATUS", "RESOURCE", "ADMIN",
            "ONBOARDING", "EXAM", "WHITELIST", "ATTENDANCE", "COMMUNITY", "ACTIVITY", "CALENDAR",
            "CHANGELOG", "OPS_CONTROL", "NODE_DAEMON", "CLOUDREVE_SYNC", "BACKUP_RECOVERY", "ALERTING",
            "ONLINE_MAP", "PLUGIN_INTEGRATION", "CROSS_PLATFORM_NOTIFICATION", "OPS_IMAGE_MARKET",
            "MATERIAL", "GUIDE");
    private static final Set<String> IMPLEMENTED = new LinkedHashSet<>(MODULE_KEYS);
    private static final Set<String> NOT_IMPLEMENTED = Set.<String>of();
    private static final Set<String> AUDIT_SOURCES = new LinkedHashSet<>(MODULE_KEYS);
    static {
        AUDIT_SOURCES.add("API_GATEWAY");
    }
    private final Map<String, ModuleConfig> moduleConfigs = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> todoSeeds = new ArrayList<>();
    private final List<Map<String, Object>> auditSeeds = new ArrayList<>();
    private final Map<String, Object> layout = new ConcurrentHashMap<>();
    private final Map<String, Setting> settings = new ConcurrentHashMap<>();
    private final Map<String, IdempotencyRecord> idempotency = new ConcurrentHashMap<>();
    private final boolean testMode;

    AdminStore(boolean testMode) {
        this.testMode = testMode;
    }

    void seed() {
        int order = 10;
        for (String key : MODULE_KEYS) {
            moduleConfigs.put(key, new ModuleConfig(key, order, true));
            order += 10;
        }
        layout.put("dashboardCards", new ArrayList<>(List.of("todos", "metrics", "health")));
        layout.put("navigationModuleOrder", new ArrayList<>(MODULE_KEYS));
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
        List<String> degraded = degradedModules(request).stream()
                .filter(moduleKey -> canAccessModule(actor, moduleKey))
                .toList();
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
        data.put("platformDependencies", platformDependencies(request));
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
            if (!canAccessModule(actor, config.key)) {
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
        if ((!config.enabled || hiddenModules().contains(moduleKey)) && !actor.hasRole("OWNER")) {
            throw new AdminException(409, 43713, "module disabled");
        }
        requireModuleAccess(actor, moduleKey);
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
        addMetric(metrics, "onboarding.activeApplications", "Onboarding applications", "ONBOARDING", 3, "/admin/onboarding", request);
        addMetric(metrics, "exam.pendingManualReview", "Pending exams", "EXAM", 2, "/admin/exams", request);
        addMetric(metrics, "whitelist.pendingReview", "Pending whitelist", "WHITELIST", 2, "/admin/whitelist", request);
        addMetric(metrics, "attendance.removalCandidates", "Removal candidates", "ATTENDANCE", 1, "/admin/attendance", request);
        addMetric(metrics, "community.openReports", "Open community reports", "COMMUNITY", 3, "/admin/community", request);
        addMetric(metrics, "activity.pendingResults", "Pending activity results", "ACTIVITY", 1, "/admin/activity", request);
        addMetric(metrics, "calendar.pendingEvents", "Pending calendar events", "CALENDAR", 1, "/admin/calendar", request);
        addMetric(metrics, "changelog.pendingRelease", "Pending changelog", "CHANGELOG", 1, "/admin/changelog", request);
        addMetric(metrics, "opsControl.pendingTasks", "Ops tasks", "OPS_CONTROL", 1, "/admin/ops-control", request);
        addMetric(metrics, "nodeDaemon.connectedNodes", "Connected daemons", "NODE_DAEMON", 1, "/admin/node-daemon", request);
        addMetric(metrics, "cloudreveSync.providers", "Cloudreve providers", "CLOUDREVE_SYNC", 1, "/admin/cloudreve-sync", request);
        addMetric(metrics, "backupRecovery.activePolicies", "Backup policies", "BACKUP_RECOVERY", 2, "/admin/backup-recovery", request);
        addMetric(metrics, "alerting.openAlerts", "Open alerts", "ALERTING", 1, "/admin/alerting", request);
        addMetric(metrics, "onlineMap.providers", "Map providers", "ONLINE_MAP", 1, "/admin/online-map", request);
        addMetric(metrics, "pluginIntegration.providers", "Plugin providers", "PLUGIN_INTEGRATION", 1, "/admin/plugin-integration", request);
        addMetric(metrics, "crossPlatformNotification.pendingDeliveries", "Cross-platform deliveries", "CROSS_PLATFORM_NOTIFICATION", 1, "/admin/cross-platform-notification", request);
        addMetric(metrics, "opsImageMarket.pendingReview", "Image market reviews", "OPS_IMAGE_MARKET", 1, "/admin/ops-image-market", request);
        addMetric(metrics, "material.pendingReview", "Pending materials", "MATERIAL", 2, "/admin/materials", request);
        addMetric(metrics, "guide.pendingReview", "Pending guides", "GUIDE", 2, "/admin/guides", request);
        return metrics.stream()
                .filter(metric -> source == null || source.equals(metric.get("sourceModule")))
                .filter(metric -> canAccessModule(actor, metric.get("sourceModule").toString()))
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
        if (query.containsKey("sourceModule") && !AUDIT_SOURCES.contains(query.get("sourceModule"))) {
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
        if (testMode && "true".equals(request.getHeader("X-Test-Fail-Audit"))) {
            throw new AdminException(500, 51701, "admin audit write failed");
        }
        if (testMode && "true".equals(request.getHeader("X-Test-Fail-Settings"))) {
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

    Map<String, Object> ops(AuthUser actor, HttpServletRequest request) {
        List<Map<String, Object>> health = MODULE_KEYS.stream().map(key -> health(key, request)).toList();
        long degraded = health.stream().filter(row -> "DEGRADED".equals(row.get("status")) || "UNAVAILABLE".equals(row.get("status"))).count();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "admin");
        data.put("port", 8107);
        data.put("storageMode", "IN_MEMORY");
        data.put("authMode", actor.authMode());
        data.put("moduleAdapterMode", "TEST_STUB");
        data.put("testMode", testMode);
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
        data.put("platformDependencies", platformDependencies(request));
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
        row.put("frontendRoute", frontendRoute(moduleKey));
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
        rows.add(capability(moduleKey.toLowerCase() + ".entry", NOT_IMPLEMENTED.contains(moduleKey) ? "OPS_PLACEHOLDER" : "ENTRY", "Entry", frontendRoute(moduleKey), targetApiOrNull(moduleKey), requiredRoles(moduleKey), requiredPermissions(moduleKey), "LOW", available, true));
        if (IMPLEMENTED.contains(moduleKey)) {
            rows.add(capability(moduleKey.toLowerCase() + ".read", "READ", "Read", frontendRoute(moduleKey), targetApi(moduleKey), List.of("HELPER", "ADMIN", "OWNER"), requiredPermissions(moduleKey), "LOW", available, true));
        }
        if (actor.hasAny("ADMIN", "OWNER") && IMPLEMENTED.contains(moduleKey) && !"ADMIN".equals(moduleKey)) {
            rows.add(capability(moduleKey.toLowerCase() + ".source", "WRITE", "Source service", frontendRoute(moduleKey), targetApi(moduleKey), List.of("ADMIN", "OWNER"), requiredPermissions(moduleKey), "MEDIUM", available, true));
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
        row.put("authMode", IMPLEMENTED.contains(moduleKey) ? (testMode ? "TEST_STUB" : "TRUSTED_GATEWAY_CONTEXT") : null);
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
            rows.add(todo("todo-" + degraded.toLowerCase() + "-unavailable", degraded, degraded + "_UNAVAILABLE", degraded.toLowerCase() + "-unavailable", "HEALTH", "HIGH", "SOURCE_UNAVAILABLE", moduleName(degraded) + " unavailable", moduleName(degraded) + " adapter degraded.", frontendRoute(degraded), targetApiOrNull(degraded)));
        }
        return rows.stream()
                .filter(todo -> source == null || source.equals(todo.get("sourceModule")))
                .filter(todo -> canAccessModule(actor, todo.get("sourceModule").toString()))
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
                List<String> order = moduleKeyList(patch.get("navigationModuleOrder"));
                layout.put("navigationModuleOrder", order);
            }
            if (patch.containsKey("hiddenModules")) {
                List<String> hidden = moduleKeyList(patch.get("hiddenModules"));
                if (hidden.containsAll(IMPLEMENTED)) {
                    throw new AdminException(409, 43710, "cannot hide all implemented modules");
                }
                layout.put("hiddenModules", hidden);
            }
            if (patch.containsKey("dashboardCards")) {
                layout.put("dashboardCards", dashboardCardList(patch.get("dashboardCards")));
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
                && !sensitiveQuickActionRoute(route)
                && canAccessModule(actor, moduleKey);
    }

    private boolean sensitiveQuickActionRoute(String route) {
        return route.startsWith("/admin/ops-control/terminal")
                || route.startsWith("/admin/ops-control/files")
                || route.startsWith("/admin/ops-control/containers")
                || route.startsWith("/admin/ops-control/nodes")
                || route.startsWith("/admin/ops-control/tasks")
                || route.startsWith("/admin/ops-control/approvals")
                || route.startsWith("/admin/ops-control/logs")
                || route.startsWith("/admin/node-daemon/nodes")
                || route.startsWith("/admin/node-daemon/keys")
                || route.startsWith("/admin/node-daemon/tasks")
                || route.startsWith("/admin/node-daemon/logs");
    }

    private String moduleKeyForRoute(String route) {
        if (!route.startsWith("/admin/")) {
            return null;
        }
        for (String moduleKey : MODULE_KEYS) {
            String moduleRoute = frontendRoute(moduleKey);
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
            return moduleKeyList(patch.get("hiddenModules")).stream().anyMatch(key -> Set.of("AUTH", "ADMIN").contains(key));
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

    private List<String> moduleKeyList(Object value) {
        if (!(value instanceof List<?> list)) {
            throw new AdminException(400, 40001, "invalid list");
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            String text = Objects.toString(item, "");
            if (!MODULE_KEYS.contains(text)) {
                throw new AdminException(400, 40001, "invalid list item");
            }
            result.add(text);
        }
        return result;
    }

    private List<String> dashboardCardList(Object value) {
        if (!(value instanceof List<?> list)) {
            throw new AdminException(400, 40001, "invalid list");
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            String text = Objects.toString(item, "");
            if (!Set.of("todos", "metrics", "health").contains(text)) {
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
            case "CONTENT" -> "/api/v1/content/admin";
            case "SERVER_STATUS" -> "/api/v1/server-status/admin";
            case "RESOURCE" -> "/api/v1/resources/admin";
            case "ADMIN" -> "/api/v1/admin";
            case "ONBOARDING" -> "/api/v1/onboarding/admin";
            case "EXAM" -> "/api/v1/exams/admin";
            case "WHITELIST" -> "/api/v1/whitelist/admin";
            case "ATTENDANCE" -> "/api/v1/attendance/admin";
            case "COMMUNITY" -> "/api/v1/community/admin";
            case "ACTIVITY" -> "/api/v1/activity/admin";
            case "CALENDAR" -> "/api/v1/calendar/admin";
            case "CHANGELOG" -> "/api/v1/changelog/admin";
            case "OPS_CONTROL" -> "/api/v1/ops-control";
            case "NODE_DAEMON" -> "/api/v1/node-daemon";
            case "CLOUDREVE_SYNC" -> "/api/v1/cloudreve-sync";
            case "BACKUP_RECOVERY" -> "/api/v1/backup-recovery";
            case "ALERTING" -> "/api/v1/alerting";
            case "ONLINE_MAP" -> "/api/v1/online-map/admin";
            case "PLUGIN_INTEGRATION" -> "/api/v1/plugin-integration/admin";
            case "CROSS_PLATFORM_NOTIFICATION" -> "/api/v1/cross-platform-notification/admin";
            case "OPS_IMAGE_MARKET" -> "/api/v1/ops-image-market/admin";
            case "MATERIAL" -> "/api/v1/materials/admin";
            case "GUIDE" -> "/api/v1/guides/admin";
            default -> null;
        };
    }

    private String frontendRoute(String key) {
        return switch (key) {
            case "ADMIN" -> "/admin";
            case "NOTIFICATION" -> "/admin/notifications";
            case "RESOURCE" -> "/admin/resources";
            case "EXAM" -> "/admin/exams";
            case "MATERIAL" -> "/admin/materials";
            case "GUIDE" -> "/admin/guides";
            default -> "/admin/" + key.toLowerCase().replace('_', '-');
        };
    }

    private String moduleName(String key) {
        return switch (key) {
            case "SERVER_STATUS" -> "Server Status";
            case "NODE_DAEMON" -> "Node Daemon";
            case "OPS_CONTROL" -> "Ops Control";
            case "CLOUDREVE_SYNC" -> "Cloudreve Sync";
            case "BACKUP_RECOVERY" -> "Backup Recovery";
            case "ONLINE_MAP" -> "Online Map";
            case "PLUGIN_INTEGRATION" -> "Plugin Integration";
            case "CROSS_PLATFORM_NOTIFICATION" -> "Cross-platform Notification";
            case "OPS_IMAGE_MARKET" -> "Ops Image Market";
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
            case "ONBOARDING" -> 8108;
            case "EXAM" -> 8109;
            case "WHITELIST" -> 8110;
            case "ATTENDANCE" -> 8111;
            case "COMMUNITY" -> 8112;
            case "ACTIVITY" -> 8113;
            case "CALENDAR" -> 8114;
            case "CHANGELOG" -> 8115;
            case "OPS_CONTROL" -> 8116;
            case "NODE_DAEMON" -> 8117;
            case "CLOUDREVE_SYNC" -> 8118;
            case "BACKUP_RECOVERY" -> 8119;
            case "ALERTING" -> 8120;
            case "ONLINE_MAP" -> 8121;
            case "PLUGIN_INTEGRATION" -> 8122;
            case "CROSS_PLATFORM_NOTIFICATION" -> 8123;
            case "OPS_IMAGE_MARKET" -> 8124;
            case "MATERIAL" -> 8126;
            case "GUIDE" -> 8127;
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
        if (Set.of("OPS_CONTROL", "NODE_DAEMON", "ONLINE_MAP", "PLUGIN_INTEGRATION", "OPS_IMAGE_MARKET").contains(key)) {
            return List.of("NODE_READ");
        }
        return List.of();
    }

    private boolean canAccessModule(AuthUser actor, String moduleKey) {
        return actor.hasAny(requiredRoles(moduleKey).toArray(String[]::new))
                && actor.hasPermissions(requiredPermissions(moduleKey));
    }

    private void requireModuleAccess(AuthUser actor, String moduleKey) {
        if (!actor.hasAny(requiredRoles(moduleKey).toArray(String[]::new))) {
            throw new AdminException(403, 42001, "role permission denied");
        }
        if (!actor.hasPermissions(requiredPermissions(moduleKey))) {
            throw new AdminException(403, 42002, "capability permission denied");
        }
    }

    private List<String> degradedModules(HttpServletRequest request) {
        return new ArrayList<>(moduleModes(request).keySet());
    }

    private List<Map<String, Object>> platformDependencies(HttpServletRequest request) {
        String status = platformModes(request).getOrDefault("API_GATEWAY", "AVAILABLE");
        if ("TIMEOUT".equals(status)) {
            status = "UNAVAILABLE";
        }
        if (!Set.of("AVAILABLE", "DEGRADED", "UNAVAILABLE").contains(status)) {
            status = "DEGRADED";
        }
        boolean degraded = !"AVAILABLE".equals(status);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", "API_GATEWAY");
        row.put("name", "API Gateway");
        row.put("status", status);
        row.put("service", "api-gateway");
        row.put("port", 8125);
        row.put("targetApiBase", "/api/v1/gateway/admin");
        row.put("frontendRoute", "/admin/platform/api-gateway");
        row.put("routeCount", degraded ? 0 : 26);
        row.put("lastCheckedAt", NOW);
        row.put("degraded", degraded);
        row.put("degradeReason", degraded ? "api gateway adapter degraded" : null);
        row.put("productionGaps", List.of("gateway internal signature not enabled", "real upstream health polling not enabled"));
        return List.of(row);
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
        if (!testMode || request == null || request.getHeader("X-Test-Module-Mode") == null) {
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

    private Map<String, String> platformModes(HttpServletRequest request) {
        if (!testMode || request == null || request.getHeader("X-Test-Platform-Mode") == null) {
            return Map.of();
        }
        Map<String, String> modes = new LinkedHashMap<>();
        for (String pair : request.getHeader("X-Test-Platform-Mode").split(",")) {
            String[] parts = pair.split(":");
            if (parts.length == 2 && "API_GATEWAY".equals(parts[0])) {
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
        todoSeeds.add(todo("todo-onboarding-1", "ONBOARDING", "ONBOARDING_REVIEW", "onboarding-1", "FOLLOW_UP", "MEDIUM", "READ_ONLY", "Onboarding flow needs attention", "Inspect onboarding flow in source module.", "/admin/onboarding", "/api/v1/onboarding/admin/applications/onboarding-1"));
        todoSeeds.add(todo("todo-exam-1", "EXAM", "EXAM_MANUAL_REVIEW", "exam-1", "REVIEW", "HIGH", "READ_ONLY", "Exam answer pending manual review", "Review exam session in source module.", "/admin/exams", "/api/v1/exams/admin/sessions/exam-1"));
        todoSeeds.add(todo("todo-whitelist-1", "WHITELIST", "WHITELIST_REVIEW", "whitelist-1", "REVIEW", "HIGH", "READ_ONLY", "Whitelist application pending review", "Review whitelist application in source module.", "/admin/whitelist", "/api/v1/whitelist/admin/applications/whitelist-1"));
        todoSeeds.add(todo("todo-attendance-1", "ATTENDANCE", "ATTENDANCE_REMOVAL_CANDIDATE", "candidate-1", "FOLLOW_UP", "MEDIUM", "READ_ONLY", "Attendance removal candidate", "Inspect removal candidate in source module.", "/admin/attendance", "/api/v1/attendance/admin/removal-candidates"));
        todoSeeds.add(todo("todo-community-1", "COMMUNITY", "COMMUNITY_REPORT", "report-1", "REVIEW", "HIGH", "READ_ONLY", "Community report pending review", "Handle report in community module.", "/admin/community", "/api/v1/community/admin/reports/report-1"));
        todoSeeds.add(todo("todo-activity-1", "ACTIVITY", "ACTIVITY_RESULT_PENDING", "activity-1", "FOLLOW_UP", "MEDIUM", "READ_ONLY", "Activity result pending publication", "Publish activity result in source module.", "/admin/activity", "/api/v1/activity/admin/events/activity-1/result"));
        todoSeeds.add(todo("todo-calendar-1", "CALENDAR", "CALENDAR_EVENT_PENDING", "calendar-1", "CONFIG", "LOW", "READ_ONLY", "Calendar event pending publish", "Publish calendar event in source module.", "/admin/calendar", "/api/v1/calendar/admin/events/calendar-1"));
        todoSeeds.add(todo("todo-changelog-1", "CHANGELOG", "CHANGELOG_RELEASE_PENDING", "release-1", "CONFIG", "LOW", "READ_ONLY", "Changelog release pending publish", "Publish changelog in source module.", "/admin/changelog", "/api/v1/changelog/admin/releases/release-1"));
        todoSeeds.add(todo("todo-material-1", "MATERIAL", "MATERIAL_REVIEW", "material-1", "REVIEW", "HIGH", "READ_ONLY", "Material pending review", "Review material submission in source module.", "/admin/materials", "/api/v1/materials/admin/items/material-1"));
        todoSeeds.add(todo("todo-guide-1", "GUIDE", "GUIDE_REVIEW", "guide-1", "REVIEW", "MEDIUM", "READ_ONLY", "Guide article pending review", "Review guide article in source module.", "/admin/guides", "/api/v1/guides/admin/articles/guide-1"));
        todoSeeds.add(todo("todo-ops-control-1", "OPS_CONTROL", "OPS_CONTROL_HEALTH", "ops-health", "HEALTH", "MEDIUM", "READ_ONLY", "Ops control health needs review", "Inspect ops control summary.", "/admin/ops-control", "/api/v1/ops-control/ops/summary"));
        todoSeeds.add(todo("todo-node-daemon-1", "NODE_DAEMON", "NODE_DAEMON_HEALTH", "node-health", "HEALTH", "MEDIUM", "READ_ONLY", "Node daemon health needs review", "Inspect node daemon summary.", "/admin/node-daemon", "/api/v1/node-daemon/ops/summary"));
        todoSeeds.add(todo("todo-cloudreve-sync-1", "CLOUDREVE_SYNC", "CLOUDREVE_SYNC_HEALTH", "cloudreve-health", "HEALTH", "LOW", "READ_ONLY", "Cloudreve sync health summary", "Inspect Cloudreve sync module.", "/admin/cloudreve-sync", "/api/v1/cloudreve-sync/ops/summary"));
        todoSeeds.add(todo("todo-backup-recovery-1", "BACKUP_RECOVERY", "BACKUP_RECOVERY_DRILL", "backup-drill", "FOLLOW_UP", "MEDIUM", "READ_ONLY", "Backup drill needs review", "Inspect backup recovery module.", "/admin/backup-recovery", "/api/v1/backup-recovery/restore-drills"));
        todoSeeds.add(todo("todo-alerting-1", "ALERTING", "ALERTING_OPEN_ALERT", "alert-1", "HEALTH", "HIGH", "READ_ONLY", "Open alert needs acknowledgement", "Inspect alerting module.", "/admin/alerting", "/api/v1/alerting/alerts/alert-1"));
        todoSeeds.add(todo("todo-online-map-1", "ONLINE_MAP", "ONLINE_MAP_PROVIDER_HEALTH", "map-provider-1", "HEALTH", "LOW", "READ_ONLY", "Online map provider health summary", "Inspect online map module.", "/admin/online-map", "/api/v1/online-map/admin/providers/map-provider-1"));
        todoSeeds.add(todo("todo-plugin-integration-1", "PLUGIN_INTEGRATION", "PLUGIN_EVENT_REPLAY", "plugin-event-1", "FOLLOW_UP", "MEDIUM", "READ_ONLY", "Plugin event replay candidate", "Inspect plugin integration module.", "/admin/plugin-integration", "/api/v1/plugin-integration/admin/events/plugin-event-1"));
        todoSeeds.add(todo("todo-cross-platform-notification-1", "CROSS_PLATFORM_NOTIFICATION", "CROSS_PLATFORM_DELIVERY", "delivery-1", "FAILURE", "MEDIUM", "READ_ONLY", "Cross-platform delivery failed", "Inspect cross-platform notification module.", "/admin/cross-platform-notification", "/api/v1/cross-platform-notification/admin/deliveries/delivery-1"));
        todoSeeds.add(todo("todo-ops-image-market-1", "OPS_IMAGE_MARKET", "OPS_IMAGE_MARKET_REVIEW", "image-1", "REVIEW", "MEDIUM", "READ_ONLY", "Ops image version pending review", "Inspect image market module.", "/admin/ops-image-market", "/api/v1/ops-image-market/admin/images/image-1"));
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
        auditSeeds.add(audit("audit-onboarding-1", "ONBOARDING", "ONBOARDING_REVIEWED", "admin", "ONBOARDING", "onboarding-1", "LOW", "SUCCESS"));
        auditSeeds.add(audit("audit-exam-1", "EXAM", "EXAM_REVIEWED", "admin", "EXAM", "exam-1", "MEDIUM", "SUCCESS"));
        auditSeeds.add(audit("audit-whitelist-1", "WHITELIST", "WHITELIST_REVIEWED", "admin", "WHITELIST", "whitelist-1", "MEDIUM", "SUCCESS"));
        auditSeeds.add(audit("audit-attendance-1", "ATTENDANCE", "ATTENDANCE_ADJUSTED", "admin", "ATTENDANCE", "attendance-1", "MEDIUM", "SUCCESS"));
        auditSeeds.add(audit("audit-community-1", "COMMUNITY", "COMMUNITY_REPORT_REVIEWED", "admin", "REPORT", "report-1", "MEDIUM", "SUCCESS"));
        auditSeeds.add(audit("audit-activity-1", "ACTIVITY", "ACTIVITY_RESULT_PUBLISHED", "admin", "ACTIVITY", "activity-1", "MEDIUM", "SUCCESS"));
        auditSeeds.add(audit("audit-calendar-1", "CALENDAR", "CALENDAR_EVENT_PUBLISHED", "admin", "CALENDAR_EVENT", "calendar-1", "LOW", "SUCCESS"));
        auditSeeds.add(audit("audit-changelog-1", "CHANGELOG", "CHANGELOG_PUBLISHED", "admin", "CHANGELOG", "release-1", "LOW", "SUCCESS"));
        auditSeeds.add(audit("audit-ops-control-1", "OPS_CONTROL", "OPS_TASK_INDEXED", "admin", "OPS_TASK", "task-1", "LOW", "SUCCESS"));
        auditSeeds.add(audit("audit-node-daemon-1", "NODE_DAEMON", "NODE_HEARTBEAT_INDEXED", "admin", "NODE", "node-1", "LOW", "SUCCESS"));
        auditSeeds.add(audit("audit-cloudreve-sync-1", "CLOUDREVE_SYNC", "CLOUDREVE_SYNC_JOB_INDEXED", "admin", "SYNC_JOB", "sync-1", "LOW", "SUCCESS"));
        auditSeeds.add(audit("audit-backup-recovery-1", "BACKUP_RECOVERY", "BACKUP_POLICY_INDEXED", "admin", "BACKUP_POLICY", "backup-1", "LOW", "SUCCESS"));
        auditSeeds.add(audit("audit-alerting-1", "ALERTING", "ALERT_ACKNOWLEDGED", "admin", "ALERT", "alert-1", "LOW", "SUCCESS"));
        auditSeeds.add(audit("audit-online-map-1", "ONLINE_MAP", "MAP_PROVIDER_INDEXED", "admin", "MAP_PROVIDER", "map-provider-1", "LOW", "SUCCESS"));
        auditSeeds.add(audit("audit-plugin-integration-1", "PLUGIN_INTEGRATION", "PLUGIN_EVENT_INDEXED", "admin", "PLUGIN_EVENT", "plugin-event-1", "LOW", "SUCCESS"));
        auditSeeds.add(audit("audit-cross-platform-notification-1", "CROSS_PLATFORM_NOTIFICATION", "CROSS_PLATFORM_DELIVERY_INDEXED", "admin", "DELIVERY", "delivery-1", "LOW", "SUCCESS"));
        auditSeeds.add(audit("audit-ops-image-market-1", "OPS_IMAGE_MARKET", "OPS_IMAGE_VERSION_REVIEWED", "admin", "OPS_IMAGE", "image-1", "MEDIUM", "SUCCESS"));
        auditSeeds.add(audit("audit-material-1", "MATERIAL", "MATERIAL_REVIEWED", "admin", "MATERIAL", "material-1", "MEDIUM", "SUCCESS"));
        auditSeeds.add(audit("audit-guide-1", "GUIDE", "GUIDE_REVIEWED", "admin", "GUIDE", "guide-1", "MEDIUM", "SUCCESS"));
        auditSeeds.add(audit("audit-api-gateway-1", "API_GATEWAY", "GATEWAY_ROUTE_INDEXED", "admin", "GATEWAY_ROUTE", "route-1", "LOW", "SUCCESS"));
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
        row.put("targetRoute", "API_GATEWAY".equals(source) ? "/admin/platform/api-gateway" : frontendRoute(source));
        row.put("indexedAt", NOW);
        row.put("createdAt", NOW);
        return row;
    }
}

class TestAdminAuthProvider {
    private static final Set<String> VALID_ROLES = Set.of("OWNER", "ADMIN", "HELPER", "USER");
    private final boolean testMode;

    TestAdminAuthProvider(boolean testMode) {
        this.testMode = testMode;
    }

    AuthUser requireAny(HttpServletRequest request, String authorization, String... roles) {
        AuthUser trusted = trustedActor(request);
        if (trusted != null) {
            if (!trusted.hasAny(roles)) {
                throw new AdminException(403, 42001, "role permission denied");
            }
            return trusted;
        }
        if (!testMode) {
            throw new AdminException(401, 41000, "unauthorized");
        }
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
            case "owner-token" -> new AuthUser("owner", Set.of("OWNER"), Set.of("NODE_READ"), "TEST_STUB");
            case "admin-token" -> new AuthUser("admin", Set.of("ADMIN"), Set.of("NODE_READ"), "TEST_STUB");
            case "admin-no-node-token" -> new AuthUser("admin-no-node", Set.of("ADMIN"), Set.of(), "TEST_STUB");
            case "helper-token" -> new AuthUser("helper", Set.of("HELPER"), Set.of(), "TEST_STUB");
            case "user-token" -> new AuthUser("user", Set.of("USER"), Set.of(), "TEST_STUB");
            default -> throw new AdminException(401, 41001, "invalid session");
        };
        if (!user.hasAny(roles)) {
            throw new AdminException(403, 42001, "role permission denied");
        }
        return user;
    }

    private AuthUser trustedActor(HttpServletRequest request) {
        if (request == null || !hasTrustedActorHeader(request)) {
            return null;
        }
        String userId = request.getHeader("X-Beiming-Actor-User-Id");
        String roleHeader = request.getHeader("X-Beiming-Actor-Roles");
        if (userId == null || userId.isBlank()) {
            throw new AdminException(502, 46703, "auth context unavailable");
        }
        Set<String> roles = csv(roleHeader);
        if (roles.isEmpty()) {
            throw new AdminException(502, 46703, "auth context unavailable");
        }
        if (!VALID_ROLES.containsAll(roles)) {
            throw new AdminException(502, 46702, "auth context incompatible");
        }
        return new AuthUser(userId.trim(), roles, csv(request.getHeader("X-Beiming-Actor-Permissions")), "TRUSTED_GATEWAY_CONTEXT");
    }

    private boolean hasTrustedActorHeader(HttpServletRequest request) {
        return request.getHeader("X-Beiming-Actor-User-Id") != null
                || request.getHeader("X-Beiming-Actor-Roles") != null
                || request.getHeader("X-Beiming-Actor-Permissions") != null;
    }

    private Set<String> csv(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        Set<String> values = new LinkedHashSet<>();
        for (String item : value.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isBlank()) {
                values.add(trimmed);
            }
        }
        return values;
    }
}

record AuthUser(String userId, Set<String> roles, Set<String> permissions, String authMode) {
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

    boolean hasPermissions(List<String> required) {
        return permissions.containsAll(required);
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
