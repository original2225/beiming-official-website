package cn.beiming.apigateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Predicate;

@RestController
class GatewayController {
    private static final Set<String> ROUTE_SORTS = Set.of("routeId_asc", "serviceKey_asc", "upstreamPort_asc", "updatedAt_desc");
    private static final Set<String> UPSTREAM_SORTS = Set.of("serviceKey_asc", "status_asc", "lastCheckedAt_desc");
    private static final Set<String> LOG_SORTS = Set.of("createdAt_desc", "createdAt_asc", "durationMs_desc");
    private static final Set<String> HEALTH_STATUSES = Set.of("UNKNOWN", "UP", "DEGRADED", "DOWN", "TIMEOUT");
    private static final Set<String> LOG_RESULTS = Set.of("SUCCESS", "FAILED");
    private static final Set<String> ALLOWED_PROXY_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private final GatewayState state;
    private final GatewayHttpClient client;
    private final ObjectMapper objectMapper;

    GatewayController(GatewayState state, GatewayHttpClient client, ObjectMapper objectMapper) {
        this.state = state;
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/api/v1/gateway/health")
    ResponseEntity<Map<String, Object>> health() {
        return ok(map(
                "service", "api-gateway",
                "status", "UP",
                "port", 8125,
                "routesTotal", state.routes().size(),
                "generatedAt", now()
        ));
    }

    @GetMapping("/api/v1/gateway/admin/ops/summary")
    ResponseEntity<Map<String, Object>> summary(HttpServletRequest request) {
        requireAnyRole(request, Set.of("HELPER", "ADMIN", "OWNER"));
        Map<String, Long> counts = state.healthCounts();
        return ok(map(
                "service", "api-gateway",
                "port", 8125,
                "routesTotal", state.routes().size(),
                "enabledRoutesTotal", state.routes().stream().filter(GatewayRoute::enabled).count(),
                "upstreamsUp", counts.getOrDefault("UP", 0L),
                "upstreamsDegraded", counts.getOrDefault("DEGRADED", 0L),
                "upstreamsDown", counts.getOrDefault("DOWN", 0L) + counts.getOrDefault("TIMEOUT", 0L),
                "requestLogsRetained", state.logCount(),
                "productionGaps", productionGaps(),
                "generatedAt", now()
        ));
    }

    @GetMapping("/api/v1/gateway/admin/routes")
    ResponseEntity<Map<String, Object>> routes(HttpServletRequest request, @RequestParam Map<String, String> query) {
        requireAnyRole(request, Set.of("HELPER", "ADMIN", "OWNER"));
        Page page = page(query);
        validateSort(query.get("sort"), ROUTE_SORTS, "routeId_asc");
        String serviceKey = query.get("serviceKey");
        if (serviceKey != null && state.routeByServiceKey(serviceKey).isEmpty()) {
            throw GatewayApiException.gatewayBadQuery();
        }
        Boolean enabled = boolFilter(query.get("enabled"));
        String keyword = lower(query.get("keyword"));
        List<Map<String, Object>> items = state.routes().stream()
                .filter(route -> serviceKey == null || route.serviceKey().equals(serviceKey))
                .filter(route -> enabled == null || route.enabled() == enabled)
                .filter(route -> keyword == null
                        || lower(route.routeId()).contains(keyword)
                        || lower(route.serviceKey()).contains(keyword)
                        || lower(route.pathPrefix()).contains(keyword))
                .sorted(routeComparator(query.getOrDefault("sort", "routeId_asc")))
                .map(GatewayRoute::toView)
                .toList();
        return ok(page(items, page));
    }

    @GetMapping("/api/v1/gateway/admin/routes/{routeId}")
    ResponseEntity<Map<String, Object>> route(HttpServletRequest request, @PathVariable String routeId) {
        requireAnyRole(request, Set.of("HELPER", "ADMIN", "OWNER"));
        if (!routeId.matches("[a-z0-9-]+")) {
            throw GatewayApiException.field("routeId");
        }
        GatewayRoute route = state.routeById(routeId)
                .orElseThrow(() -> new GatewayApiException(HttpStatus.NOT_FOUND, 43000, "route not found"));
        return ok(route.toView());
    }

    @GetMapping("/api/v1/gateway/admin/upstreams")
    ResponseEntity<Map<String, Object>> upstreams(HttpServletRequest request, @RequestParam Map<String, String> query) {
        requireAnyRole(request, Set.of("HELPER", "ADMIN", "OWNER"));
        Page page = page(query);
        validateSort(query.get("sort"), UPSTREAM_SORTS, "serviceKey_asc");
        String status = query.get("status");
        if (status != null && !HEALTH_STATUSES.contains(status)) {
            throw GatewayApiException.gatewayBadQuery();
        }
        String serviceKey = query.get("serviceKey");
        if (serviceKey != null && state.routeByServiceKey(serviceKey).isEmpty()) {
            throw GatewayApiException.gatewayBadQuery();
        }
        List<Map<String, Object>> items = state.healthViews().stream()
                .filter(item -> status == null || status.equals(item.get("status")))
                .filter(item -> serviceKey == null || serviceKey.equals(item.get("serviceKey")))
                .sorted(healthComparator(query.getOrDefault("sort", "serviceKey_asc")))
                .toList();
        return ok(page(items, page));
    }

    @PostMapping("/api/v1/gateway/admin/upstreams/{serviceKey}/health-refresh")
    ResponseEntity<Map<String, Object>> refresh(HttpServletRequest request, @PathVariable String serviceKey) {
        requireAnyRole(request, Set.of("HELPER", "ADMIN", "OWNER"));
        GatewayRoute route = state.routeByServiceKey(serviceKey)
                .orElseThrow(() -> new GatewayApiException(HttpStatus.NOT_FOUND, 43000, "upstream not found"));
        Instant started = Instant.now();
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("X-Request-Id", List.of(RequestIdFilter.currentRequestId()));
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && !auth.isBlank()) {
            headers.put(HttpHeaders.AUTHORIZATION, List.of(auth));
        }
        GatewayHttpRequest upstreamRequest = new GatewayHttpRequest("GET", route.healthCheckPath(), null, headers, "");
        try {
            GatewayHttpResponse response = client.exchange(route, upstreamRequest);
            String status = response.status() < 500 ? "UP" : "DEGRADED";
            return ok(state.updateHealth(route, status, response.status(), null, null, duration(started)).toView());
        } catch (GatewayUpstreamException ex) {
            int code = upstreamFailureCode(ex.type());
            String status = ex.type() == GatewayFailureType.TIMEOUT ? "TIMEOUT" : "DOWN";
            return ok(state.updateHealth(route, status, null, code, ex.getMessage(), duration(started)).toView());
        }
    }

    @GetMapping("/api/v1/gateway/admin/request-logs")
    ResponseEntity<Map<String, Object>> requestLogs(HttpServletRequest request, @RequestParam Map<String, String> query) {
        requireAnyRole(request, Set.of("ADMIN", "OWNER"));
        Page page = page(query);
        validateSort(query.get("sort"), LOG_SORTS, "createdAt_desc");
        String routeId = query.get("routeId");
        if (routeId != null && state.routeById(routeId).isEmpty()) {
            throw GatewayApiException.gatewayBadQuery();
        }
        String serviceKey = query.get("serviceKey");
        if (serviceKey != null && state.routeByServiceKey(serviceKey).isEmpty()) {
            throw GatewayApiException.gatewayBadQuery();
        }
        String result = query.get("result");
        if (result != null && !LOG_RESULTS.contains(result)) {
            throw GatewayApiException.gatewayBadQuery();
        }
        Instant from = parseTime(query.get("from"));
        Instant to = parseTime(query.get("to"));
        if (from != null && to != null && from.isAfter(to)) {
            throw GatewayApiException.gatewayBadQuery();
        }
        List<Map<String, Object>> items = state.logs().stream()
                .filter(log -> routeId == null || routeId.equals(log.routeId()))
                .filter(log -> serviceKey == null || serviceKey.equals(log.serviceKey()))
                .filter(log -> result == null || result.equals(log.result()))
                .filter(log -> from == null || !log.createdAt().isBefore(from))
                .filter(log -> to == null || !log.createdAt().isAfter(to))
                .sorted(logComparator(query.getOrDefault("sort", "createdAt_desc")))
                .map(GatewayRequestLog::toView)
                .toList();
        return ok(page(items, page));
    }

    @RequestMapping("/api/v1/**")
    ResponseEntity<?> proxy(HttpServletRequest request, @org.springframework.web.bind.annotation.RequestBody(required = false) byte[] body) {
        Instant started = Instant.now();
        String path = request.getRequestURI();
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        GatewayRoute route = state.match(path).orElse(null);
        if (route == null) {
            recordLog(started, method, path, null, null, HttpStatus.NOT_FOUND.value(), "FAILED", 46200, request);
            throw new GatewayApiException(HttpStatus.NOT_FOUND, 46200, "gateway route not found");
        }
        if (!ALLOWED_PROXY_METHODS.contains(method) || !route.methods().contains(method)) {
            recordLog(started, method, path, route, null, HttpStatus.METHOD_NOT_ALLOWED.value(), "FAILED", 46201, request);
            throw new GatewayApiException(HttpStatus.METHOD_NOT_ALLOWED, 46201, "gateway method not allowed");
        }
        if ("OPTIONS".equals(method)) {
            return ResponseEntity.ok()
                    .body(envelope(0, "success", null));
        }

        GatewayHttpRequest upstreamRequest = new GatewayHttpRequest(
                method,
                path,
                request.getQueryString(),
                sanitizedHeaders(request),
                body == null ? "" : new String(body, StandardCharsets.UTF_8)
        );
        try {
            GatewayHttpResponse upstream = client.exchange(route, upstreamRequest);
            int errorCode = upstream.status() >= 400 ? extractErrorCode(upstream.body()).orElse(null) : 0;
            recordLog(started, method, path, route, upstream.status(), upstream.status(), upstream.status() < 400 ? "SUCCESS" : "FAILED", upstream.status() < 400 ? null : errorCode, request);
            HttpHeaders headers = new HttpHeaders();
            if (upstream.contentType() != null && !upstream.contentType().isBlank()) {
                headers.set(HttpHeaders.CONTENT_TYPE, upstream.contentType());
            }
            return new ResponseEntity<>(upstream.body(), headers, HttpStatusCode.valueOf(upstream.status()));
        } catch (GatewayUpstreamException ex) {
            int code = upstreamFailureCode(ex.type());
            HttpStatus status = upstreamFailureStatus(ex.type());
            recordLog(started, method, path, route, null, status.value(), "FAILED", code, request);
            throw new GatewayApiException(status, code, ex.getMessage());
        }
    }

    private Map<String, List<String>> sanitizedHeaders(HttpServletRequest request) {
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            String lower = name.toLowerCase(Locale.ROOT);
            if (Set.of("connection", "transfer-encoding", "upgrade", "keep-alive", "te", "trailer", "proxy-authorization", "content-length", "host").contains(lower)) {
                continue;
            }
            if (lower.startsWith("x-beiming-actor-") || lower.startsWith("x-gateway-internal-")) {
                continue;
            }
            headers.put(name, Collections.list(request.getHeaders(name)));
        }
        headers.put("X-Request-Id", List.of(RequestIdFilter.currentRequestId()));
        String remote = request.getRemoteAddr();
        if (remote != null && !remote.isBlank()) {
            headers.put("X-Forwarded-For", List.of(remote));
        }
        return headers;
    }

    private void requireAnyRole(HttpServletRequest request, Set<String> allowed) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || header.isBlank()) {
            throw new GatewayApiException(HttpStatus.UNAUTHORIZED, 41000, "unauthenticated");
        }
        if (!header.startsWith("Bearer ")) {
            throw new GatewayApiException(HttpStatus.UNAUTHORIZED, 41003, "invalid token format");
        }
        Set<String> roles = switch (header.substring("Bearer ".length())) {
            case "owner-token" -> Set.of("OWNER");
            case "admin-token" -> Set.of("ADMIN");
            case "helper-token" -> Set.of("HELPER");
            case "user-token" -> Set.of("USER");
            default -> throw new GatewayApiException(HttpStatus.UNAUTHORIZED, 41001, "invalid session");
        };
        if (roles.stream().noneMatch(allowed::contains)) {
            throw new GatewayApiException(HttpStatus.FORBIDDEN, 42001, "role insufficient");
        }
    }

    private void recordLog(Instant started, String method, String path, GatewayRoute route, Integer upstreamStatus, int gatewayStatus, String result, Integer errorCode, HttpServletRequest request) {
        state.record(new GatewayRequestLog(
                RequestIdFilter.currentRequestId(),
                method,
                path,
                route == null ? null : route.routeId(),
                route == null ? null : route.serviceKey(),
                upstreamStatus,
                gatewayStatus,
                result,
                errorCode == null || errorCode == 0 ? null : errorCode,
                duration(started),
                clientIp(request),
                null,
                Instant.now()
        ));
    }

    private int duration(Instant started) {
        return Math.max(0, (int) Duration.between(started, Instant.now()).toMillis());
    }

    private int upstreamFailureCode(GatewayFailureType type) {
        return switch (type) {
            case TIMEOUT -> 46211;
            case INVALID_RESPONSE -> 46212;
            case INVALID_UPSTREAM -> 46213;
            default -> 46210;
        };
    }

    private HttpStatus upstreamFailureStatus(GatewayFailureType type) {
        return type == GatewayFailureType.TIMEOUT ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY;
    }

    private String clientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? null : remote;
    }

    private Optional<Integer> extractErrorCode(byte[] body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            return node.has("code") && node.get("code").canConvertToInt() ? Optional.of(node.get("code").asInt()) : Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private Page page(Map<String, String> query) {
        try {
            int page = Integer.parseInt(query.getOrDefault("page", "1"));
            int pageSize = Integer.parseInt(query.getOrDefault("pageSize", "20"));
            if (page < 1 || pageSize < 1 || pageSize > 100) {
                throw GatewayApiException.gatewayBadQuery();
            }
            return new Page(page, pageSize);
        } catch (NumberFormatException ex) {
            throw GatewayApiException.gatewayBadQuery();
        }
    }

    private Map<String, Object> page(List<Map<String, Object>> items, Page page) {
        int from = Math.min((page.page() - 1) * page.pageSize(), items.size());
        int to = Math.min(from + page.pageSize(), items.size());
        return map("items", items.subList(from, to), "page", page.page(), "pageSize", page.pageSize(), "total", items.size());
    }

    private void validateSort(String sort, Set<String> allowed, String defaultSort) {
        String value = sort == null || sort.isBlank() ? defaultSort : sort;
        if (!allowed.contains(value)) {
            throw GatewayApiException.gatewayBadQuery();
        }
    }

    private Boolean boolFilter(String value) {
        if (value == null) {
            return null;
        }
        if ("true".equals(value)) {
            return Boolean.TRUE;
        }
        if ("false".equals(value)) {
            return Boolean.FALSE;
        }
        throw GatewayApiException.gatewayBadQuery();
    }

    private Instant parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw GatewayApiException.gatewayBadQuery();
        }
    }

    private Comparator<GatewayRoute> routeComparator(String sort) {
        return switch (sort) {
            case "serviceKey_asc" -> Comparator.comparing(GatewayRoute::serviceKey);
            case "upstreamPort_asc" -> Comparator.comparingInt(GatewayRoute::upstreamPort);
            case "updatedAt_desc" -> Comparator.comparing(GatewayRoute::updatedAt).reversed();
            default -> Comparator.comparing(GatewayRoute::routeId);
        };
    }

    private Comparator<Map<String, Object>> healthComparator(String sort) {
        return switch (sort) {
            case "status_asc" -> Comparator.comparing(item -> String.valueOf(item.get("status")));
            case "lastCheckedAt_desc" -> Comparator.comparing((Map<String, Object> item) -> String.valueOf(item.get("lastCheckedAt")), Comparator.nullsLast(Comparator.naturalOrder())).reversed();
            default -> Comparator.comparing(item -> String.valueOf(item.get("serviceKey")));
        };
    }

    private Comparator<GatewayRequestLog> logComparator(String sort) {
        return switch (sort) {
            case "createdAt_asc" -> Comparator.comparing(GatewayRequestLog::createdAt);
            case "durationMs_desc" -> Comparator.comparingInt(GatewayRequestLog::durationMs).reversed();
            default -> Comparator.comparing(GatewayRequestLog::createdAt).reversed();
        };
    }

    private List<String> productionGaps() {
        return List.of(
                "SERVICE_DISCOVERY_NOT_CONNECTED",
                "CENTRAL_CONFIG_NOT_CONNECTED",
                "DISTRIBUTED_RATE_LIMIT_NOT_CONNECTED",
                "REAL_AUTH_CONTEXT_NOT_INJECTED",
                "PERSISTENT_AUDIT_NOT_CONNECTED",
                "WEBSOCKET_PROXY_NOT_ENABLED",
                "LARGE_STREAM_PROXY_NOT_ENABLED"
        );
    }

    private ResponseEntity<Map<String, Object>> ok(Object data) {
        return ResponseEntity.ok(envelope(0, "success", data));
    }

    private Map<String, Object> envelope(int code, String message, Object data) {
        Map<String, Object> body = map("code", code, "message", message, "data", data);
        body.put("requestId", RequestIdFilter.currentRequestId());
        return body;
    }

    private Map<String, Object> map(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }

    private String now() {
        return Instant.now().toString();
    }

    private String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private record Page(int page, int pageSize) {
    }
}

@Component
class GatewayState {
    private static final Instant REGISTERED_AT = Instant.parse("2026-05-29T00:00:00Z");
    private final List<GatewayRoute> routes = createRoutes();
    private final Map<String, GatewayUpstreamHealth> health = new LinkedHashMap<>();
    private final ArrayDeque<GatewayRequestLog> logs = new ArrayDeque<>();

    GatewayState() {
        resetRuntimeState();
    }

    synchronized void resetRuntimeState() {
        health.clear();
        for (GatewayRoute route : routes) {
            health.put(route.serviceKey(), GatewayUpstreamHealth.unknown(route));
        }
        logs.clear();
    }

    List<GatewayRoute> routes() {
        return routes;
    }

    Optional<GatewayRoute> routeById(String routeId) {
        return routes.stream().filter(route -> route.routeId().equals(routeId)).findFirst();
    }

    Optional<GatewayRoute> routeByServiceKey(String serviceKey) {
        return routes.stream().filter(route -> route.serviceKey().equals(serviceKey)).findFirst();
    }

    Optional<GatewayRoute> match(String path) {
        return routes.stream()
                .filter(route -> path.equals(route.pathPrefix()) || path.startsWith(route.pathPrefix() + "/"))
                .max(Comparator.comparingInt(route -> route.pathPrefix().length()));
    }

    synchronized GatewayUpstreamHealth updateHealth(GatewayRoute route, String status, Integer httpStatus, Integer errorCode, String errorMessage, int durationMs) {
        GatewayUpstreamHealth value = new GatewayUpstreamHealth(route.serviceKey(), route.routeId(), route.pathPrefix(), status, httpStatus, errorCode, errorMessage, Instant.now(), durationMs);
        health.put(route.serviceKey(), value);
        return value;
    }

    synchronized List<Map<String, Object>> healthViews() {
        return health.values().stream().map(GatewayUpstreamHealth::toView).toList();
    }

    synchronized Map<String, Long> healthCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (GatewayUpstreamHealth item : health.values()) {
            counts.merge(item.status(), 1L, Long::sum);
        }
        return counts;
    }

    synchronized void record(GatewayRequestLog log) {
        logs.addFirst(log);
        while (logs.size() > 200) {
            logs.removeLast();
        }
    }

    synchronized int logCount() {
        return logs.size();
    }

    synchronized List<GatewayRequestLog> logs() {
        return new ArrayList<>(logs);
    }

    private List<GatewayRoute> createRoutes() {
        List<GatewayRoute> items = new ArrayList<>();
        items.add(route("auth", "AUTH", "auth", "/api/v1/auth", 8101, "/api/v1/auth/session/verify"));
        items.add(route("profile", "PROFILE", "profile", "/api/v1/profile", 8102, "/api/v1/profile/members"));
        items.add(route("notification", "NOTIFICATION", "notification", "/api/v1/notifications", 8103, "/api/v1/notifications/me/unread-count"));
        items.add(route("content", "CONTENT", "content", "/api/v1/content", 8104, "/api/v1/content/homepage"));
        items.add(route("server-status", "SERVER_STATUS", "server-status", "/api/v1/server-status", 8105, "/api/v1/server-status/overview"));
        items.add(route("resource", "RESOURCE", "resource", "/api/v1/resources", 8106, "/api/v1/resources"));
        items.add(route("admin", "ADMIN", "admin", "/api/v1/admin", 8107, "/api/v1/admin/overview"));
        items.add(route("onboarding", "ONBOARDING", "onboarding", "/api/v1/onboarding", 8108, "/api/v1/onboarding/me/progress"));
        items.add(route("exam", "EXAM", "exam", "/api/v1/exams", 8109, "/api/v1/exams/me/sessions"));
        items.add(route("whitelist", "WHITELIST", "whitelist", "/api/v1/whitelist", 8110, "/api/v1/whitelist/me/applications/current"));
        items.add(route("attendance", "ATTENDANCE", "attendance", "/api/v1/attendance", 8111, "/api/v1/attendance/me/summary"));
        items.add(route("community", "COMMUNITY", "community", "/api/v1/community", 8112, "/api/v1/community/boards"));
        items.add(route("activity", "ACTIVITY", "activity", "/api/v1/activity", 8113, "/api/v1/activity/events"));
        items.add(route("calendar", "CALENDAR", "calendar", "/api/v1/calendar", 8114, "/api/v1/calendar/upcoming"));
        items.add(route("changelog", "CHANGELOG", "changelog", "/api/v1/changelog", 8115, "/api/v1/changelog/versions/latest"));
        items.add(route("ops-control", "OPS_CONTROL", "ops-control", "/api/v1/ops-control", 8116, "/api/v1/ops-control/overview"));
        items.add(route("node-daemon", "NODE_DAEMON", "node-daemon", "/api/v1/node-daemon", 8117, "/api/v1/node-daemon/health"));
        items.add(route("cloudreve-sync", "CLOUDREVE_SYNC", "cloudreve-sync", "/api/v1/cloudreve-sync", 8118, "/api/v1/cloudreve-sync/health"));
        items.add(route("backup-recovery", "BACKUP_RECOVERY", "backup-recovery", "/api/v1/backup-recovery", 8119, "/api/v1/backup-recovery/health"));
        items.add(route("alerting", "ALERTING", "alerting", "/api/v1/alerting", 8120, "/api/v1/alerting/health"));
        items.add(route("online-map", "ONLINE_MAP", "online-map", "/api/v1/online-map", 8121, "/api/v1/online-map/health"));
        items.add(route("plugin-integration", "PLUGIN_INTEGRATION", "plugin-integration", "/api/v1/plugin-integration", 8122, "/api/v1/plugin-integration/health"));
        items.add(route("cross-platform-notification", "CROSS_PLATFORM_NOTIFICATION", "cross-platform-notification", "/api/v1/cross-platform-notification", 8123, "/api/v1/cross-platform-notification/health"));
        items.add(route("ops-image-market", "OPS_IMAGE_MARKET", "ops-image-market", "/api/v1/ops-image-market", 8124, "/api/v1/ops-image-market/health"));
        return List.copyOf(items);
    }

    private GatewayRoute route(String routeId, String serviceKey, String serviceName, String pathPrefix, int port, String healthPath) {
        return new GatewayRoute(routeId, serviceKey, serviceName, pathPrefix, "http://127.0.0.1:" + port, port, healthPath, 1500, true,
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"), true, REGISTERED_AT, REGISTERED_AT);
    }
}

@Component
class JavaGatewayHttpClient implements GatewayHttpClient {
    @Override
    public GatewayHttpResponse exchange(GatewayRoute route, GatewayHttpRequest request) {
        try {
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(route.timeoutMs()))
                    .build();
            String url = route.upstreamBaseUrl() + request.path() + (request.query() == null || request.query().isBlank() ? "" : "?" + request.query());
            java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(route.timeoutMs()));
            request.headers().forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
            java.net.http.HttpRequest.BodyPublisher publisher = request.body() == null || request.body().isEmpty()
                    ? java.net.http.HttpRequest.BodyPublishers.noBody()
                    : java.net.http.HttpRequest.BodyPublishers.ofString(request.body(), StandardCharsets.UTF_8);
            builder.method(request.method(), publisher);
            java.net.http.HttpResponse<byte[]> response = httpClient.send(builder.build(), java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            return new GatewayHttpResponse(response.statusCode(), response.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse(MediaType.APPLICATION_JSON_VALUE), response.body(), response.headers().map());
        } catch (HttpTimeoutException ex) {
            throw GatewayUpstreamException.timeout("upstream timeout");
        } catch (ConnectException ex) {
            throw GatewayUpstreamException.connection("upstream unavailable");
        } catch (IllegalArgumentException ex) {
            throw GatewayUpstreamException.invalidUpstream("upstream address invalid");
        } catch (java.net.ProtocolException ex) {
            throw GatewayUpstreamException.invalidResponse("invalid upstream response");
        } catch (IOException ex) {
            throw GatewayUpstreamException.connection("upstream unavailable");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw GatewayUpstreamException.timeout("upstream timeout");
        }
    }
}

interface GatewayHttpClient {
    GatewayHttpResponse exchange(GatewayRoute route, GatewayHttpRequest request);
}

record GatewayRoute(String routeId, String serviceKey, String serviceName, String pathPrefix, String upstreamBaseUrl,
                    int upstreamPort, String healthCheckPath, int timeoutMs, boolean enabled, List<String> methods,
                    boolean authDelegated, Instant createdAt, Instant updatedAt) {
    Map<String, Object> toView() {
        return view(
                "routeId", routeId,
                "serviceKey", serviceKey,
                "serviceName", serviceName,
                "pathPrefix", pathPrefix,
                "upstreamBaseUrl", upstreamBaseUrl,
                "upstreamPort", upstreamPort,
                "healthCheckPath", healthCheckPath,
                "timeoutMs", timeoutMs,
                "enabled", enabled,
                "methods", methods,
                "authDelegated", authDelegated,
                "createdAt", createdAt.toString(),
                "updatedAt", updatedAt.toString()
        );
    }

    private Map<String, Object> view(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}

record GatewayUpstreamHealth(String serviceKey, String routeId, String pathPrefix, String status, Integer lastHttpStatus,
                             Integer lastErrorCode, String lastErrorMessage, Instant lastCheckedAt, Integer durationMs) {
    static GatewayUpstreamHealth unknown(GatewayRoute route) {
        return new GatewayUpstreamHealth(route.serviceKey(), route.routeId(), route.pathPrefix(), "UNKNOWN", null, null, null, null, null);
    }

    Map<String, Object> toView() {
        return map(
                "serviceKey", serviceKey,
                "routeId", routeId,
                "pathPrefix", pathPrefix,
                "status", status,
                "lastHttpStatus", lastHttpStatus,
                "lastErrorCode", lastErrorCode,
                "lastErrorMessage", lastErrorMessage,
                "lastCheckedAt", lastCheckedAt == null ? null : lastCheckedAt.toString(),
                "durationMs", durationMs
        );
    }

    private Map<String, Object> map(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}

record GatewayRequestLog(String requestId, String method, String path, String routeId, String serviceKey,
                         Integer upstreamStatus, int gatewayStatus, String result, Integer errorCode, int durationMs,
                         String clientIp, String actorUserId, Instant createdAt) {
    Map<String, Object> toView() {
        return map(
                "requestId", requestId,
                "method", method,
                "path", path,
                "routeId", routeId,
                "serviceKey", serviceKey,
                "upstreamStatus", upstreamStatus,
                "gatewayStatus", gatewayStatus,
                "result", result,
                "errorCode", errorCode,
                "durationMs", durationMs,
                "clientIp", clientIp,
                "actorUserId", actorUserId,
                "createdAt", createdAt.toString()
        );
    }

    private Map<String, Object> map(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}

record GatewayHttpRequest(String method, String path, String query, Map<String, List<String>> headers, String body) {
}

record GatewayHttpResponse(int status, String contentType, byte[] body, Map<String, List<String>> headers) {
}

enum GatewayFailureType {
    CONNECTION,
    TIMEOUT,
    INVALID_RESPONSE,
    INVALID_UPSTREAM
}

class GatewayUpstreamException extends RuntimeException {
    private final GatewayFailureType type;

    private GatewayUpstreamException(GatewayFailureType type, String message) {
        super(message);
        this.type = type;
    }

    GatewayFailureType type() {
        return type;
    }

    static GatewayUpstreamException connection(String message) {
        return new GatewayUpstreamException(GatewayFailureType.CONNECTION, message);
    }

    static GatewayUpstreamException timeout(String message) {
        return new GatewayUpstreamException(GatewayFailureType.TIMEOUT, message);
    }

    static GatewayUpstreamException invalidResponse(String message) {
        return new GatewayUpstreamException(GatewayFailureType.INVALID_RESPONSE, message);
    }

    static GatewayUpstreamException invalidUpstream(String message) {
        return new GatewayUpstreamException(GatewayFailureType.INVALID_UPSTREAM, message);
    }
}

class GatewayApiException extends RuntimeException {
    final HttpStatus status;
    final int code;
    final String field;

    GatewayApiException(HttpStatus status, int code, String message) {
        this(status, code, message, null);
    }

    GatewayApiException(HttpStatus status, int code, String message, String field) {
        super(message);
        this.status = status;
        this.code = code;
        this.field = field;
    }

    static GatewayApiException gatewayBadQuery() {
        return new GatewayApiException(HttpStatus.BAD_REQUEST, 46203, "invalid gateway query");
    }

    static GatewayApiException field(String field) {
        return new GatewayApiException(HttpStatus.BAD_REQUEST, 40001, "invalid request", field);
    }
}

@RestControllerAdvice
class GatewayExceptionHandler {
    @ExceptionHandler(GatewayApiException.class)
    ResponseEntity<Map<String, Object>> api(GatewayApiException ex) {
        Map<String, Object> body = envelope(ex.code, ex.getMessage(), null);
        if (ex.code == 40001) {
            body.put("errors", List.of(Map.of("field", ex.field == null ? "request" : ex.field, "reason", ex.getMessage())));
        }
        return ResponseEntity.status(ex.status).body(body);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> any(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(envelope(51250, "api gateway internal error", null));
    }

    private Map<String, Object> envelope(int code, String message, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", data);
        body.put("requestId", RequestIdFilter.currentRequestId());
        return body;
    }
}

@Component
@Order(1)
class GatewayAdminRequestLogFilter extends OncePerRequestFilter {
    private final GatewayState state;

    GatewayAdminRequestLogFilter(GatewayState state) {
        this.state = state;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Instant started = Instant.now();
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (request.getRequestURI().startsWith("/api/v1/gateway/admin/")) {
                int status = response.getStatus();
                String result = status < 400 ? "SUCCESS" : "FAILED";
                state.record(new GatewayRequestLog(
                        RequestIdFilter.currentRequestId(),
                        request.getMethod().toUpperCase(Locale.ROOT),
                        request.getRequestURI(),
                        null,
                        null,
                        null,
                        status,
                        result,
                        null,
                        Math.max(0, (int) Duration.between(started, Instant.now()).toMillis()),
                        request.getRemoteAddr() == null || request.getRemoteAddr().isBlank() ? null : request.getRemoteAddr(),
                        null,
                        Instant.now()
                ));
            }
        }
    }
}

@Component
@Order(0)
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

@Configuration
class GatewayWebConfig {
    @Bean
    WebMvcConfigurer gatewayCorsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/v1/**")
                        .allowedOrigins(
                                "http://localhost:5173",
                                "http://127.0.0.1:5173",
                                "http://localhost:5174",
                                "http://127.0.0.1:5174",
                                "http://localhost:5182",
                                "http://127.0.0.1:5182"
                        )
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("Authorization", "Content-Type", "Accept-Language", "X-Request-Id", "X-Requested-With", "Accept")
                        .exposedHeaders("X-Request-Id");
            }
        };
    }
}
