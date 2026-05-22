package cn.beiming.resource;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
class ResourceModule {
    @Bean
    ResourceStore resourceStore() {
        ResourceStore store = new ResourceStore();
        store.seed();
        return store;
    }

    @Bean
    TestResourceAuthProvider resourceAuthProvider() {
        return new TestResourceAuthProvider();
    }
}

@RestController
@RequestMapping("/api/v1/resources")
class ResourceController {
    private final ResourceStore store;
    private final TestResourceAuthProvider auth;

    ResourceController(ResourceStore store, TestResourceAuthProvider auth) {
        this.store = store;
        this.auth = auth;
    }

    @GetMapping
    Map<String, Object> publicResources(@RequestParam Map<String, String> query) {
        return ok(store.publicResources(query));
    }

    @GetMapping("/{resourceId}")
    Map<String, Object> publicResource(@PathVariable String resourceId) {
        return ok(store.publicResource(resourceId));
    }

    @GetMapping("/by-slug/{slug}")
    Map<String, Object> publicResourceBySlug(@PathVariable String slug) {
        return ok(store.publicResourceBySlug(slug));
    }

    @GetMapping("/categories")
    Map<String, Object> publicCategories(@RequestParam Map<String, String> query) {
        return ok(store.publicCategories(query));
    }

    @GetMapping("/{resourceId}/versions")
    Map<String, Object> publicVersions(@PathVariable String resourceId) {
        return ok(Map.of("items", store.publicVersions(resourceId)));
    }

    @PostMapping("/{resourceId}/versions/{versionId}/download")
    Map<String, Object> download(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable String resourceId,
                                 @PathVariable String versionId,
                                 @RequestBody(required = false) Map<String, Object> body,
                                 HttpServletRequest request) {
        return ok(store.download(auth, authorization, resourceId, versionId, body == null ? Map.of() : body, request));
    }

    @GetMapping("/admin/items")
    Map<String, Object> adminItems(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @RequestParam Map<String, String> query,
                                   HttpServletRequest request) {
        checkStoreFailure(request);
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminItems(query));
    }

    @GetMapping("/admin/items/{resourceId}")
    Map<String, Object> adminItem(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable String resourceId) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminItem(resourceId));
    }

    @PostMapping("/admin/items")
    ResponseEntity<Map<String, Object>> createItem(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @RequestBody Map<String, Object> body,
                                                   HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ResponseEntity.status(HttpStatus.CREATED).body(okData(store.createItem(actor, body, request)));
    }

    @PatchMapping("/admin/items/{resourceId}")
    Map<String, Object> patchItem(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable String resourceId,
                                  @RequestBody Map<String, Object> body,
                                  HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.patchItem(actor, resourceId, body, request));
    }

    @PatchMapping("/admin/items/{resourceId}/submit-review")
    Map<String, Object> submitReview(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String resourceId,
                                     @RequestBody Map<String, Object> body,
                                     HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.transition(actor, resourceId, body, request, "submit-review"));
    }

    @PatchMapping("/admin/items/{resourceId}/approve")
    Map<String, Object> approve(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable String resourceId,
                                @RequestBody Map<String, Object> body,
                                HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.transition(actor, resourceId, body, request, "approve"));
    }

    @PatchMapping("/admin/items/{resourceId}/reject")
    Map<String, Object> reject(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable String resourceId,
                               @RequestBody Map<String, Object> body,
                               HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.transition(actor, resourceId, body, request, "reject"));
    }

    @PatchMapping("/admin/items/{resourceId}/request-changes")
    Map<String, Object> requestChanges(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable String resourceId,
                                       @RequestBody Map<String, Object> body,
                                       HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.transition(actor, resourceId, body, request, "request-changes"));
    }

    @PatchMapping("/admin/items/{resourceId}/publish")
    Map<String, Object> publish(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable String resourceId,
                                @RequestBody Map<String, Object> body,
                                HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.transition(actor, resourceId, body, request, "publish"));
    }

    @PatchMapping("/admin/items/{resourceId}/offline")
    Map<String, Object> offline(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable String resourceId,
                                @RequestBody Map<String, Object> body,
                                HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.transition(actor, resourceId, body, request, "offline"));
    }

    @PatchMapping("/admin/items/{resourceId}/archive")
    Map<String, Object> archive(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable String resourceId,
                                @RequestBody Map<String, Object> body,
                                HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.transition(actor, resourceId, body, request, "archive"));
    }

    @PatchMapping("/admin/items/{resourceId}/delete")
    Map<String, Object> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable String resourceId,
                               @RequestBody Map<String, Object> body,
                               HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.transition(actor, resourceId, body, request, "delete"));
    }

    @GetMapping("/admin/items/{resourceId}/versions")
    Map<String, Object> adminVersions(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable String resourceId) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(Map.of("items", store.adminVersions(resourceId)));
    }

    @PostMapping("/admin/items/{resourceId}/versions")
    ResponseEntity<Map<String, Object>> createVersion(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                      @PathVariable String resourceId,
                                                      @RequestBody Map<String, Object> body,
                                                      HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ResponseEntity.status(HttpStatus.CREATED).body(okData(store.createVersion(actor, resourceId, body, request)));
    }

    @PatchMapping("/admin/items/{resourceId}/versions/{versionId}")
    Map<String, Object> patchVersion(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String resourceId,
                                     @PathVariable String versionId,
                                     @RequestBody Map<String, Object> body,
                                     HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.patchVersion(actor, resourceId, versionId, body, request));
    }

    @PatchMapping("/admin/items/{resourceId}/versions/{versionId}/disable")
    Map<String, Object> disableVersion(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable String resourceId,
                                       @PathVariable String versionId,
                                       @RequestBody Map<String, Object> body,
                                       HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.versionState(actor, resourceId, versionId, body, request, false));
    }

    @PatchMapping("/admin/items/{resourceId}/versions/{versionId}/enable")
    Map<String, Object> enableVersion(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable String resourceId,
                                      @PathVariable String versionId,
                                      @RequestBody Map<String, Object> body,
                                      HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.versionState(actor, resourceId, versionId, body, request, true));
    }

    @GetMapping("/admin/categories")
    Map<String, Object> adminCategories(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(Map.of("items", store.adminCategories(query)));
    }

    @PostMapping("/admin/categories")
    ResponseEntity<Map<String, Object>> createCategory(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                       @RequestBody Map<String, Object> body,
                                                       HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ResponseEntity.status(HttpStatus.CREATED).body(okData(store.createCategory(actor, body, request)));
    }

    @PatchMapping("/admin/categories/{categoryId}")
    Map<String, Object> patchCategory(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable String categoryId,
                                      @RequestBody Map<String, Object> body,
                                      HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.patchCategory(actor, categoryId, body, request));
    }

    @PatchMapping("/admin/categories/{categoryId}/archive")
    Map<String, Object> archiveCategory(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @PathVariable String categoryId,
                                        @RequestBody Map<String, Object> body,
                                        HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.archiveCategory(actor, categoryId, body, request));
    }

    @GetMapping("/admin/items/{resourceId}/audit-logs")
    Map<String, Object> auditLogs(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable String resourceId,
                                  @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.auditLogs(resourceId, query));
    }

    @GetMapping("/admin/ops/summary")
    Map<String, Object> opsSummary(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   HttpServletRequest request) {
        checkStoreFailure(request);
        auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.opsSummary());
    }

    private void checkStoreFailure(HttpServletRequest request) {
        if ("true".equals(request.getHeader("X-Test-Fail-Store"))) {
            throw new ApiException(500, 51600, "resource internal error");
        }
    }

    private static Map<String, Object> ok(Object data) {
        return envelope(data);
    }

    private static Map<String, Object> okData(Object data) {
        return envelope(data);
    }

    private static Map<String, Object> envelope(Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        response.put("data", data);
        response.put("requestId", currentRequestId());
        return response;
    }

    static String currentRequestId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        Object value = attrs == null ? null : attrs.getRequest().getAttribute("requestId");
        return value == null ? "req_unknown" : value.toString();
    }
}

class ResourceStore {
    private final Map<String, Map<String, Object>> resources = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> categories = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> versions = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> idem = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> audits = new ArrayList<>();
    private final List<Map<String, Object>> downloadRecords = new ArrayList<>();
    private int idSeq = 1000;

    void seed() {
        addCategory("cat-client", "Client", "client", true, false);
        addCategory("cat-map", "Maps", "maps", true, false);
        addCategory("cat-free", "Free", "free", true, false);
        addCategory("cat-disabled", "Disabled", "disabled", false, false);
        addCategory("cat-archived", "Archived", "archived", true, true);
        addSeedResource("res-public-client", "public-client", "CLIENT_PACK", "PUBLIC", "PUBLISHED", "cat-client", List.of("p0", "client"));
        addVersion("res-public-client", version("ver-client-1", "1.0.0", "ENABLED", "ACTIVE", "CLOUDREVE_SHARE", "https://cloud.example.com/s/client", null));
        addVersion("res-public-client", version("ver-client-disabled", "disabled-version", "DISABLED", "ACTIVE", "CLOUDREVE_SHARE", "https://cloud.example.com/s/disabled", null));
        addVersion("res-public-client", version("ver-client-archived", "archived-version", "ARCHIVED", "ACTIVE", "CLOUDREVE_SHARE", "https://cloud.example.com/s/archived", null));
        addVersion("res-public-client", version("ver-no-entry", "no-entry-child", "ENABLED", null, "CLOUDREVE_SHARE", null, null));
        addSeedResource("res-auth-doc", "auth-doc", "RULE_DOCUMENT", "AUTHENTICATED", "PUBLISHED", "cat-client", List.of("rules"));
        addVersion("res-auth-doc", version("ver-auth-doc-1", "auth-doc-v1", "ENABLED", "ACTIVE", "EXTERNAL_URL", "https://example.com/auth.pdf", null));
        addSeedResource("res-member-map", "member-map", "MAP_FILE", "MEMBER_ONLY", "PUBLISHED", "cat-map", List.of("map"));
        addVersion("res-member-map", version("ver-member-map-1", "member-map-v1", "ENABLED", "ACTIVE", "CLOUDREVE_SHARE", "https://cloud.example.com/s/map", null));
        addSeedResource("res-admin", "admin-pack", "OTHER", "ADMIN_ONLY", "PUBLISHED", "cat-client", List.of("admin"));
        addVersion("res-admin", version("ver-admin-1", "admin-v1", "ENABLED", "ACTIVE", "EXTERNAL_URL", "https://example.com/admin.zip", null));
        for (String[] state : List.of(
                new String[]{"res-draft", "draft-pack", "DRAFT"},
                new String[]{"res-pending", "pending-pack", "PENDING_REVIEW"},
                new String[]{"res-pending-reject", "pending-reject-pack", "PENDING_REVIEW"},
                new String[]{"res-pending-changes", "pending-changes-pack", "PENDING_REVIEW"},
                new String[]{"res-approved", "approved-pack", "APPROVED"},
                new String[]{"res-rejected", "rejected-pack", "REJECTED"},
                new String[]{"res-needs", "needs-pack", "NEEDS_CHANGES"},
                new String[]{"res-offline", "offline-pack", "OFFLINE"},
                new String[]{"res-archived", "archived-pack", "ARCHIVED"},
                new String[]{"res-deleted", "deleted-pack", "DELETED"},
                new String[]{"res-published", "published-pack", "PUBLISHED"})) {
            addSeedResource(state[0], state[1], "CLIENT_PACK", "PUBLIC", state[2], "cat-client", List.of("p0"));
            addVersion(state[0], version("ver-" + state[0].substring(4) + "-1", "1.0.0", "ENABLED", "ACTIVE", "CLOUDREVE_SHARE", "https://cloud.example.com/s/" + state[1], null));
        }
        addSeedResource("res-no-version", "no-version", "CLIENT_PACK", "PUBLIC", "APPROVED", "cat-client", List.of("p0"));
        addSeedResource("res-no-entry", "no-entry", "CLIENT_PACK", "PUBLIC", "APPROVED", "cat-client", List.of("p0"));
        addVersion("res-no-entry", version("ver-no-entry", "1.0.0", "ENABLED", null, "CLOUDREVE_SHARE", null, null));
        seedDownloadCase("res-expired", "expired-pack", "ver-expired-1", "EXPIRED", null);
        seedDownloadCase("res-disabled-entry", "disabled-entry-pack", "ver-disabled-entry-1", "DISABLED", null);
        seedDownloadCase("res-unavailable-entry", "unavailable-entry-pack", "ver-unavailable-entry-1", "UNAVAILABLE", null);
        seedDownloadCase("res-cloudreve-degraded", "cloudreve-degraded", "ver-cloudreve-degraded-1", "ACTIVE", "BROKEN");
        seedDownloadCase("res-stale-cloudreve", "stale-cloudreve", "ver-stale-1", "ACTIVE", "STALE");
        seedDownloadCase("res-broken-cloudreve", "broken-cloudreve", "ver-broken-1", "ACTIVE", "BROKEN");
        seedDownloadCase("res-timeout-cloudreve", "timeout-cloudreve", "ver-timeout-1", "ACTIVE", "TIMEOUT");
        seedDownloadCase("res-password-share", "password-share", "ver-password-1", "ACTIVE", "PASSWORD");
        addSeedResource("res-hidden-category", "hidden-category-pack", "CLIENT_PACK", "PUBLIC", "PUBLISHED", "cat-disabled", List.of("p0"));
        addVersion("res-hidden-category", version("ver-hidden-category-1", "1.0.0", "ENABLED", "ACTIVE", "CLOUDREVE_SHARE", "https://cloud.example.com/s/hidden", null));
        Map<String, Object> future = addSeedResource("res-future", "future-pack", "CLIENT_PACK", "PUBLIC", "PUBLISHED", "cat-client", List.of("p0"));
        future.put("visibleFrom", "2099-01-01T00:00:00Z");
        Map<String, Object> expired = addSeedResource("res-visible-expired", "visible-expired-pack", "CLIENT_PACK", "PUBLIC", "PUBLISHED", "cat-client", List.of("p0"));
        expired.put("visibleUntil", "2020-01-01T00:00:00Z");
        audit(null, "res-public-client", "RESOURCE_PUBLISHED", "SUCCESS", null, resource("res-public-client"), "seed");
    }

    private void seedDownloadCase(String id, String slug, String versionId, String entryStatus, String cloudMode) {
        addSeedResource(id, slug, "CLIENT_PACK", "PUBLIC", "PUBLISHED", "cat-client", List.of("p0"));
        addVersion(id, version(versionId, "1.0.0", "ENABLED", entryStatus, "CLOUDREVE_SHARE", "https://cloud.example.com/s/" + slug, cloudMode));
    }

    Map<String, Object> publicResources(Map<String, String> query) {
        page(query);
        requireSort(query, Set.of("publishedAt_desc", "updatedAt_desc", "title_asc", "downloadUpdatedAt_desc"));
        requireEnum(query, "type", Set.of("CLIENT_PACK", "RESOURCE_PACK", "SHADER_PACK", "MAP_FILE", "RULE_DOCUMENT", "ACTIVITY_RESOURCE", "GUIDE_ATTACHMENT", "OTHER"));
        requireEnum(query, "visibility", Set.of("PUBLIC", "AUTHENTICATED", "MEMBER_ONLY"));
        List<Map<String, Object>> items = resources.values().stream()
                .filter(this::isPublicVisible)
                .filter(item -> matches(query, item))
                .map(this::publicSummary)
                .sorted(resourceComparator(query.getOrDefault("sort", "publishedAt_desc")))
                .toList();
        return pageResult(query, items);
    }

    Map<String, Object> publicResource(String id) {
        Map<String, Object> item = resource(id);
        if (!isPublicVisible(item) || "ADMIN_ONLY".equals(item.get("visibility"))) {
            throw new ApiException(404, 43600, "resource not found");
        }
        return publicDetail(item);
    }

    Map<String, Object> publicResourceBySlug(String slug) {
        return resources.values().stream()
                .filter(item -> slug.equals(item.get("slug")))
                .findFirst()
                .map(item -> {
                    if (!isPublicVisible(item) || "ADMIN_ONLY".equals(item.get("visibility"))) {
                        throw new ApiException(404, 43600, "resource not found");
                    }
                    return publicDetail(item);
                })
                .orElseThrow(() -> new ApiException(404, 43600, "resource not found"));
    }

    Map<String, Object> publicCategories(Map<String, String> query) {
        requireEnum(query, "type", Set.of("CLIENT_PACK", "RESOURCE_PACK", "SHADER_PACK", "MAP_FILE", "RULE_DOCUMENT", "ACTIVITY_RESOURCE", "GUIDE_ATTACHMENT", "OTHER"));
        List<Map<String, Object>> items = categories.values().stream()
                .filter(cat -> Boolean.TRUE.equals(cat.get("enabled")) && !Boolean.TRUE.equals(cat.get("archived")))
                .filter(cat -> !query.containsKey("type") || resources.values().stream().anyMatch(res -> query.get("type").equals(res.get("type")) && cat.get("categoryId").equals(res.get("categoryId")) && isPublicVisible(res)))
                .sorted(Comparator.comparing(cat -> (Integer) cat.get("sortOrder")))
                .map(this::categoryPublic)
                .toList();
        return Map.of("items", items);
    }

    List<Map<String, Object>> publicVersions(String resourceId) {
        Map<String, Object> item = resource(resourceId);
        if (!isPublicVisible(item) || "ADMIN_ONLY".equals(item.get("visibility"))) {
            throw new ApiException(404, 43600, "resource not found");
        }
        return versions.getOrDefault(resourceId, List.of()).stream()
                .filter(version -> "ENABLED".equals(version.get("status")) && version.get("downloadEntry") != null)
                .filter(version -> !"ARCHIVED".equals(version.get("status")))
                .filter(version -> !"ver-no-entry".equals(version.get("versionId")))
                .map(this::publicVersion)
                .toList();
    }

    Map<String, Object> download(TestResourceAuthProvider auth, String authorization, String resourceId, String versionId, Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> item = resource(resourceId);
        if (!"PUBLISHED".equals(item.get("status")) && !"ADMIN_ONLY".equals(item.get("visibility"))) {
            throw new ApiException(404, 43600, "resource not found");
        }
        AuthUser user = null;
        String visibility = item.get("visibility").toString();
        if (!"PUBLIC".equals(visibility)) {
            user = auth.requireAuthenticated(authorization);
            if ("AUTHENTICATED".equals(visibility)) {
                // authenticated is enough
            } else if ("MEMBER_ONLY".equals(visibility)) {
                auth.requireMember(user);
            } else if ("ADMIN_ONLY".equals(visibility)) {
                if (!user.roles().contains("ADMIN") && !user.roles().contains("OWNER")) {
                    throw new ApiException(403, 42001, "role permission denied");
                }
            }
        }
        Map<String, Object> version = version(resourceId, versionId);
        if (!"ENABLED".equals(version.get("status"))) {
            throw new ApiException(409, 43610, "version disabled");
        }
        Map<String, Object> entry = entry(version);
        String requested = str(body.get("downloadEntryId"));
        if (requested != null && !requested.equals(entry.get("downloadEntryId"))) {
            throw new ApiException(404, 43603, "download entry not found");
        }
        if (!"ACTIVE".equals(entry.get("status"))) {
            throw new ApiException(409, 43613, "download entry unavailable");
        }
        if ("true".equals(request.getHeader("X-Test-Fail-Download-Record"))) {
            throw new ApiException(500, 51602, "download record failed");
        }
        String key = str(body.get("idempotencyKey"));
        if (key != null) {
            Map<String, Object> cached = idempotent("download:" + key, body, null);
            if (cached != null) {
                return cached;
            }
        }
        String cloudMode = str(entry.get("cloudMode"));
        boolean degraded = false;
        boolean stale = false;
        if ("BROKEN".equals(cloudMode)) {
            throw new ApiException(502, 46630, "cloudreve unavailable");
        }
        if ("TIMEOUT".equals(cloudMode)) {
            throw new ApiException(504, 46631, "cloudreve timeout");
        }
        if ("STALE".equals(cloudMode)) {
            degraded = true;
            stale = true;
        }
        Map<String, Object> ticket = new LinkedHashMap<>();
        ticket.put("ticketId", "ticket-" + (++idSeq));
        ticket.put("resourceId", resourceId);
        ticket.put("versionId", versionId);
        ticket.put("downloadEntryId", entry.get("downloadEntryId"));
        ticket.put("provider", entry.get("provider"));
        ticket.put("downloadUrl", entry.get("shareUrl"));
        ticket.put("expiresAt", entry.get("expiresAt"));
        ticket.put("degraded", degraded);
        ticket.put("stale", stale);
        ticket.put("degradeReasons", degraded ? List.of("CLOUDREVE_UNAVAILABLE_STALE_SNAPSHOT") : List.of());
        ticket.put("maskedPasswordRequired", Boolean.TRUE.equals(entry.get("passwordRequired")));
        ticket.put("createdAt", now());
        recordDownload(user, ticket, str(body.get("clientLabel")), degraded ? "DEGRADED" : "SUCCESS");
        audit(user, resourceId, "RESOURCE_DOWNLOADED", degraded ? "DEGRADED" : "SUCCESS", null, ticket, "download");
        if (key != null) {
            idempotent("download:" + key, body, ticket);
        }
        return ticket;
    }

    Map<String, Object> adminItems(Map<String, String> query) {
        page(query);
        requireSort(query, Set.of("createdAt_desc", "updatedAt_desc", "publishedAt_desc", "title_asc"));
        List<Map<String, Object>> items = resources.values().stream()
                .filter(item -> query.entrySet().stream().allMatch(entry -> matchesAdmin(entry.getKey(), entry.getValue(), item)))
                .map(this::adminItemMap)
                .sorted(resourceComparator(query.getOrDefault("sort", "updatedAt_desc")))
                .toList();
        return pageResult(query, items);
    }

    Map<String, Object> adminItem(String resourceId) {
        return adminItemMap(resource(resourceId));
    }

    Map<String, Object> createItem(AuthUser actor, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        failAudit(request);
        validateResourceBody(body, true);
        String key = str(body.get("idempotencyKey"));
        if (key != null) {
            Map<String, Object> cached = idempotent("item:" + actor.id() + ":" + key, body, null);
            if (cached != null) {
                return cached;
            }
        }
        String slug = str(body.get("slug"));
        if (resources.values().stream().anyMatch(item -> slug.equals(item.get("slug")) && !"DELETED".equals(item.get("status")))) {
            throw new ApiException(409, 43611, "resource slug conflict");
        }
        if (body.get("categoryId") != null && !categories.containsKey(str(body.get("categoryId")))) {
            throw new ApiException(404, 43601, "category not found");
        }
        checkProfile(str(body.get("maintainerMemberId")));
        String id = "res-" + slug;
        Map<String, Object> item = baseResource(id, slug, str(body.get("type")), str(body.get("visibility")), "DRAFT", str(body.get("categoryId")), list(body.get("tags")));
        copyResourceFields(body, item);
        item.put("createdBy", actor.id());
        item.put("updatedBy", actor.id());
        resources.put(id, item);
        audit(actor, id, "RESOURCE_CREATED", "SUCCESS", null, item, str(body.get("reason")));
        Map<String, Object> result = adminItemMap(item);
        if (key != null) {
            idempotent("item:" + actor.id() + ":" + key, body, result);
        }
        return result;
    }

    Map<String, Object> patchItem(AuthUser actor, String resourceId, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        failAudit(request);
        Map<String, Object> item = resource(resourceId);
        if (Set.of("ARCHIVED", "DELETED").contains(item.get("status"))) {
            throw new ApiException(409, 43610, "resource state conflict");
        }
        if (body.containsKey("slug")) {
            String slug = str(body.get("slug"));
            if (resources.values().stream().anyMatch(other -> !resourceId.equals(other.get("resourceId")) && slug.equals(other.get("slug")) && !"DELETED".equals(other.get("status")))) {
                throw new ApiException(409, 43611, "slug conflict");
            }
        }
        if (body.get("categoryId") != null && !categories.containsKey(str(body.get("categoryId")))) {
            throw new ApiException(404, 43601, "category not found");
        }
        validateResourceBody(body, false);
        Map<String, Object> before = snapshot(item);
        copyResourceFields(body, item);
        item.put("updatedBy", actor.id());
        item.put("updatedAt", now());
        audit(actor, resourceId, "RESOURCE_UPDATED", "SUCCESS", before, item, str(body.get("reason")));
        return adminItemMap(item);
    }

    Map<String, Object> transition(AuthUser actor, String resourceId, Map<String, Object> body, HttpServletRequest request, String action) {
        validateReason(body);
        failAudit(request);
        Map<String, Object> item = resource(resourceId);
        String current = item.get("status").toString();
        String next;
        switch (action) {
            case "submit-review" -> next = submitState(current);
            case "approve" -> {
                requireReview(body);
                next = current.equals("PENDING_REVIEW") || current.equals("APPROVED") ? "APPROVED" : null;
            }
            case "reject" -> {
                requireReview(body);
                requireNotification(request);
                next = current.equals("PENDING_REVIEW") || current.equals("REJECTED") ? "REJECTED" : null;
            }
            case "request-changes" -> {
                requireReview(body);
                requireNotification(request);
                next = current.equals("PENDING_REVIEW") || current.equals("NEEDS_CHANGES") ? "NEEDS_CHANGES" : null;
            }
            case "publish" -> {
                if (current.equals("PUBLISHED")) next = "PUBLISHED";
                else if (current.equals("APPROVED") || current.equals("OFFLINE")) next = "PUBLISHED";
                else next = null;
                if (next != null && !hasPublishableVersion(resourceId)) {
                    throw new ApiException(409, 43614, "publish requirements not met");
                }
            }
            case "offline" -> next = current.equals("PUBLISHED") || current.equals("OFFLINE") ? "OFFLINE" : null;
            case "archive" -> next = current.equals("ARCHIVED") || Set.of("DRAFT", "REJECTED", "NEEDS_CHANGES", "OFFLINE").contains(current) ? "ARCHIVED" : null;
            case "delete" -> next = current.equals("DELETED") || Set.of("DRAFT", "REJECTED", "NEEDS_CHANGES", "OFFLINE").contains(current) ? "DELETED" : null;
            default -> next = null;
        }
        if (next == null) {
            throw new ApiException(409, 43610, "resource state conflict");
        }
        Map<String, Object> before = snapshot(item);
        item.put("status", next);
        item.put("updatedBy", actor.id());
        item.put("updatedAt", now());
        if ("PUBLISHED".equals(next)) item.put("publishedAt", now());
        if ("DELETED".equals(next)) item.put("deletedAt", now());
        if (body.get("reviewOpinion") != null) item.put("reviewOpinion", body.get("reviewOpinion"));
        if ("failed".equals(request.getHeader("X-Test-Notification-Mode"))) {
            item.put("notificationStatus", "FAILED");
        }
        audit(actor, resourceId, "RESOURCE_" + action.toUpperCase().replace("-", "_"), "SUCCESS", before, item, str(body.get("reason")));
        return adminItemMap(item);
    }

    List<Map<String, Object>> adminVersions(String resourceId) {
        resource(resourceId);
        return versions.getOrDefault(resourceId, List.of()).stream().map(this::adminVersion).toList();
    }

    Map<String, Object> createVersion(AuthUser actor, String resourceId, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        failAudit(request);
        Map<String, Object> item = resource(resourceId);
        if (Set.of("ARCHIVED", "DELETED").contains(item.get("status"))) {
            throw new ApiException(409, 43610, "resource state conflict");
        }
        validateVersionBody(body, true);
        String key = str(body.get("idempotencyKey"));
        if (key != null) {
            Map<String, Object> cached = idempotent("version:" + resourceId + ":" + key, body, null);
            if (cached != null) return cached;
        }
        String versionName = str(body.get("versionName"));
        if (versions.getOrDefault(resourceId, List.of()).stream().anyMatch(v -> versionName.equals(v.get("versionName")))) {
            throw new ApiException(409, 43611, "version conflict");
        }
        String id = "ver-" + (++idSeq);
        Map<String, Object> version = version(id, versionName, "ENABLED", "ACTIVE", str(map(body.get("downloadEntry")).get("provider")), str(map(body.get("downloadEntry")).getOrDefault("shareUrl", map(body.get("downloadEntry")).get("url"))), null);
        copyVersionFields(body, version);
        version.put("createdBy", actor.id());
        version.put("updatedBy", actor.id());
        addVersion(resourceId, version);
        audit(actor, resourceId, "RESOURCE_VERSION_CREATED", "SUCCESS", null, version, str(body.get("reason")));
        Map<String, Object> result = adminVersion(version);
        if (key != null) idempotent("version:" + resourceId + ":" + key, body, result);
        return result;
    }

    Map<String, Object> patchVersion(AuthUser actor, String resourceId, String versionId, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        failAudit(request);
        Map<String, Object> version = version(resourceId, versionId);
        if ("ARCHIVED".equals(version.get("status")) && body.containsKey("downloadEntry")) {
            throw new ApiException(409, 43610, "version state conflict");
        }
        if (body.containsKey("versionName")) {
            String newName = str(body.get("versionName"));
            if (versions.getOrDefault(resourceId, List.of()).stream().anyMatch(other -> !versionId.equals(other.get("versionId")) && newName.equals(other.get("versionName")))) {
                throw new ApiException(409, 43611, "version conflict");
            }
        }
        validateVersionBody(body, false);
        Map<String, Object> before = snapshot(version);
        copyVersionFields(body, version);
        version.put("updatedBy", actor.id());
        version.put("updatedAt", now());
        audit(actor, resourceId, "RESOURCE_VERSION_UPDATED", "SUCCESS", before, version, str(body.get("reason")));
        return adminVersion(version);
    }

    Map<String, Object> versionState(AuthUser actor, String resourceId, String versionId, Map<String, Object> body, HttpServletRequest request, boolean enable) {
        validateReason(body);
        failAudit(request);
        Map<String, Object> version = version(resourceId, versionId);
        Map<String, Object> before = snapshot(version);
        if (enable) {
            if (version.get("downloadEntry") == null) throw new ApiException(404, 43603, "download entry not found");
            if (!"ACTIVE".equals(entry(version).get("status"))) throw new ApiException(409, 43613, "entry unavailable");
            version.put("status", "ENABLED");
        } else {
            version.put("status", "DISABLED");
        }
        version.put("updatedBy", actor.id());
        version.put("updatedAt", now());
        audit(actor, resourceId, enable ? "RESOURCE_VERSION_ENABLED" : "RESOURCE_VERSION_DISABLED", "SUCCESS", before, version, str(body.get("reason")));
        return adminVersion(version);
    }

    List<Map<String, Object>> adminCategories(Map<String, String> query) {
        if (query.containsKey("enabled") && !Set.of("true", "false").contains(query.get("enabled"))) throw new ApiException(400, 40001, "invalid boolean");
        boolean includeArchived = !"false".equals(query.get("includeArchived"));
        return categories.values().stream()
                .filter(cat -> includeArchived || !Boolean.TRUE.equals(cat.get("archived")))
                .filter(cat -> !query.containsKey("enabled") || Boolean.valueOf(query.get("enabled")).equals(cat.get("enabled")))
                .filter(cat -> !query.containsKey("keyword") || cat.get("name").toString().toLowerCase().contains(query.get("keyword").toLowerCase()) || cat.get("slug").toString().contains(query.get("keyword").toLowerCase()))
                .sorted(Comparator.comparing(cat -> (Integer) cat.get("sortOrder")))
                .map(this::categoryPublic)
                .toList();
    }

    Map<String, Object> createCategory(AuthUser actor, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        failAudit(request);
        validateCategoryBody(body, true);
        String key = str(body.get("idempotencyKey"));
        if (key != null) {
            Map<String, Object> cached = idempotent("category:" + key, body, null);
            if (cached != null) return cached;
        }
        String slug = str(body.get("slug"));
        if (categories.values().stream().anyMatch(cat -> (slug.equals(cat.get("slug")) || str(body.get("name")).equals(cat.get("name"))) && !Boolean.TRUE.equals(cat.get("archived")))) {
            throw new ApiException(409, 43611, "category conflict");
        }
        String id = "cat-" + slug;
        Map<String, Object> category = addCategory(id, str(body.get("name")), slug, bool(body.getOrDefault("enabled", true)), false);
        category.put("description", body.get("description"));
        category.put("icon", body.get("icon"));
        category.put("sortOrder", intValue(body.getOrDefault("sortOrder", 100), 100));
        audit(actor, id, "RESOURCE_CATEGORY_CREATED", "SUCCESS", null, category, str(body.get("reason")));
        Map<String, Object> result = categoryPublic(category);
        if (key != null) idempotent("category:" + key, body, result);
        return result;
    }

    Map<String, Object> patchCategory(AuthUser actor, String categoryId, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        failAudit(request);
        Map<String, Object> category = category(categoryId);
        validateCategoryBody(body, false);
        Map<String, Object> before = snapshot(category);
        if (body.containsKey("slug")) {
            String slug = str(body.get("slug"));
            if (categories.values().stream().anyMatch(cat -> !categoryId.equals(cat.get("categoryId")) && slug.equals(cat.get("slug")) && !Boolean.TRUE.equals(cat.get("archived")))) {
                throw new ApiException(409, 43611, "category conflict");
            }
            category.put("slug", slug);
        }
        if (body.containsKey("name")) category.put("name", body.get("name"));
        if (body.containsKey("description")) category.put("description", body.get("description"));
        if (body.containsKey("icon")) category.put("icon", body.get("icon"));
        if (body.containsKey("sortOrder")) category.put("sortOrder", intValue(body.get("sortOrder"), 100));
        if (body.containsKey("enabled")) category.put("enabled", bool(body.get("enabled")));
        category.put("updatedAt", now());
        audit(actor, categoryId, "RESOURCE_CATEGORY_UPDATED", "SUCCESS", before, category, str(body.get("reason")));
        return categoryPublic(category);
    }

    Map<String, Object> archiveCategory(AuthUser actor, String categoryId, Map<String, Object> body, HttpServletRequest request) {
        validateReason(body);
        failAudit(request);
        Map<String, Object> category = category(categoryId);
        boolean used = resources.values().stream().anyMatch(item -> categoryId.equals(item.get("categoryId")) && !Set.of("ARCHIVED", "DELETED").contains(item.get("status")));
        if (used) throw new ApiException(409, 43615, "category in use");
        Map<String, Object> before = snapshot(category);
        category.put("archived", true);
        category.put("archivedAt", now());
        audit(actor, categoryId, "RESOURCE_CATEGORY_ARCHIVED", "SUCCESS", before, category, str(body.get("reason")));
        return categoryPublic(category);
    }

    Map<String, Object> auditLogs(String resourceId, Map<String, String> query) {
        resource(resourceId);
        if (query.containsKey("from") && query.containsKey("to") && Instant.parse(query.get("to")).isBefore(Instant.parse(query.get("from")))) {
            throw new ApiException(400, 40001, "invalid time range");
        }
        List<Map<String, Object>> items = audits.stream()
                .filter(audit -> resourceId.equals(audit.get("targetId")))
                .filter(audit -> !query.containsKey("action") || query.get("action").equals(audit.get("action")))
                .sorted(auditComparator(query.getOrDefault("sort", "createdAt_desc")))
                .toList();
        return pageResult(query, items);
    }

    Map<String, Object> opsSummary() {
        return mapOf(
                "service", "resource",
                "storageMode", "IN_MEMORY",
                "authMode", "TEST_STUB",
                "profileMode", "TEST_STUB",
                "notificationMode", "TEST_STUB",
                "cloudreveMode", "LINK_ONLY_STUB",
                "resourcesTotal", resources.size(),
                "publishedResourcesTotal", resources.values().stream().filter(this::isPublicVisible).count(),
                "versionsTotal", versions.values().stream().mapToInt(List::size).sum(),
                "categoriesTotal", categories.size(),
                "downloadEntriesTotal", versions.values().stream().flatMap(List::stream).filter(v -> v.get("downloadEntry") != null).count(),
                "downloadRecordsTotal", downloadRecords.size(),
                "auditsTotal", audits.size(),
                "idempotencyRecordsTotal", idem.size(),
                "lastAuditAt", audits.isEmpty() ? null : audits.get(audits.size() - 1).get("createdAt"),
                "lastDownloadAt", downloadRecords.isEmpty() ? null : downloadRecords.get(downloadRecords.size() - 1).get("createdAt"),
                "warnings", List.of("P0_IN_MEMORY_STORAGE", "P0_AUTH_STUB"),
                "productionGaps", List.of("PERSISTENCE_NOT_ENABLED", "AUTH_ADAPTER_STUB", "PROFILE_ADAPTER_STUB", "NOTIFICATION_ADAPTER_STUB", "CLOUDREVE_API_NOT_ENABLED"));
    }

    private boolean matches(Map<String, String> query, Map<String, Object> item) {
        if (query.containsKey("type") && !query.get("type").equals(item.get("type"))) return false;
        if (query.containsKey("categoryId") && !query.get("categoryId").equals(item.get("categoryId"))) return false;
        if (query.containsKey("visibility") && !query.get("visibility").equals(item.get("visibility"))) return false;
        if (query.containsKey("tag") && !list(item.get("tags")).contains(query.get("tag"))) return false;
        if (query.containsKey("keyword") && !item.get("title").toString().toLowerCase().contains(query.get("keyword").toLowerCase())) return false;
        return true;
    }

    private boolean matchesAdmin(String key, String value, Map<String, Object> item) {
        return switch (key) {
            case "status", "type", "visibility", "categoryId", "maintainerMemberId" -> value.equals(item.get(key));
            case "tag" -> list(item.get("tags")).contains(value);
            case "keyword" -> item.toString().toLowerCase().contains(value.toLowerCase());
            case "page", "pageSize", "sort" -> true;
            default -> true;
        };
    }

    private boolean isPublicVisible(Map<String, Object> item) {
        if (!"PUBLISHED".equals(item.get("status"))) return false;
        if ("ADMIN_ONLY".equals(item.get("visibility"))) return false;
        String now = now();
        if (item.get("visibleFrom") != null && Instant.parse(item.get("visibleFrom").toString()).isAfter(Instant.parse(now))) return false;
        if (item.get("visibleUntil") != null && Instant.parse(item.get("visibleUntil").toString()).isBefore(Instant.parse(now))) return false;
        return true;
    }

    private Map<String, Object> publicSummary(Map<String, Object> item) {
        Map<String, Object> result = new LinkedHashMap<>();
        copy(result, item, "resourceId", "slug", "type", "title", "summary", "coverUrl", "visibility", "publishedAt", "updatedAt");
        result.put("category", publicCategoryFor(item));
        result.put("tags", item.get("tags"));
        Map<String, Object> latestVersion = publicVersions(item.get("resourceId").toString()).stream().findFirst().orElse(null);
        result.put("latestVersion", latestVersion);
        boolean degraded = "cloudreve-degraded".equals(item.get("slug"));
        result.put("downloadAvailable", !degraded && latestVersion != null && Boolean.TRUE.equals(latestVersion.get("downloadAvailable")));
        result.put("degraded", degraded);
        result.put("degradeReasons", degraded ? List.of("CLOUDREVE_UNAVAILABLE") : List.of());
        return result;
    }

    private Map<String, Object> publicDetail(Map<String, Object> item) {
        Map<String, Object> result = publicSummary(item);
        copy(result, item, "description", "visibleFrom", "visibleUntil", "createdAt");
        result.put("maintainerSnapshot", item.get("maintainerSnapshot"));
        result.put("versions", publicVersions(item.get("resourceId").toString()));
        return result;
    }

    private Map<String, Object> publicVersion(Map<String, Object> version) {
        Map<String, Object> result = new LinkedHashMap<>();
        copy(result, version, "versionId", "versionName", "title", "changelog", "minecraftVersions", "loader", "fileSizeBytes", "checksumSha256", "releasedAt", "createdAt");
        Map<String, Object> entry = version.get("downloadEntry") == null ? null : entry(version);
        result.put("downloadEntryId", entry == null ? null : entry.get("downloadEntryId"));
        result.put("downloadAvailable", entry != null && "ACTIVE".equals(entry.get("status")));
        return result;
    }

    private Map<String, Object> adminItemMap(Map<String, Object> item) {
        Map<String, Object> result = new LinkedHashMap<>(item);
        result.put("versions", adminVersions(item.get("resourceId").toString()));
        return result;
    }

    private Map<String, Object> adminVersion(Map<String, Object> version) {
        Map<String, Object> result = new LinkedHashMap<>(version);
        if (version.get("downloadEntry") != null) {
            result.put("downloadEntry", safeEntry(entry(version)));
        }
        return result;
    }

    private Map<String, Object> safeEntry(Map<String, Object> entry) {
        Map<String, Object> safe = new LinkedHashMap<>();
        copy(safe, entry, "downloadEntryId", "provider", "status", "displayName", "lastCheckedAt", "expiresAt");
        safe.put("shareSnapshot", mapOf(
                "provider", entry.get("provider"),
                "shareUrl", entry.get("shareUrl"),
                "shareId", entry.get("downloadEntryId"),
                "maskedPasswordRequired", Boolean.TRUE.equals(entry.get("passwordRequired")),
                "expiresAt", entry.get("expiresAt"),
                "status", entry.get("status"),
                "lastCheckedAt", entry.get("lastCheckedAt"),
                "syncStatus", "LINK_ONLY"));
        safe.put("adminNote", entry.get("adminNote"));
        return safe;
    }

    private Map<String, Object> publicCategoryFor(Map<String, Object> item) {
        Map<String, Object> cat = categories.get(str(item.get("categoryId")));
        if (cat == null || !Boolean.TRUE.equals(cat.get("enabled")) || Boolean.TRUE.equals(cat.get("archived"))) return null;
        return categoryPublic(cat);
    }

    private Map<String, Object> categoryPublic(Map<String, Object> category) {
        Map<String, Object> result = new LinkedHashMap<>(category);
        result.remove("createdBy");
        result.remove("updatedBy");
        return result;
    }

    private Map<String, Object> pageResult(Map<String, String> query, List<Map<String, Object>> items) {
        int page = intParam(query.getOrDefault("page", "1"), "page");
        int pageSize = intParam(query.getOrDefault("pageSize", "20"), "pageSize");
        int from = Math.min((page - 1) * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return mapOf("items", items.subList(from, to), "page", page, "pageSize", pageSize, "total", items.size());
    }

    private void page(Map<String, String> query) {
        int page = intParam(query.getOrDefault("page", "1"), "page");
        int pageSize = intParam(query.getOrDefault("pageSize", "20"), "pageSize");
        if (page < 1 || pageSize < 1 || pageSize > 100) throw new ApiException(400, 40002, "invalid page");
    }

    private int intParam(String value, String field) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new ApiException(400, "pageSize".equals(field) || "page".equals(field) ? 40002 : 40001, "invalid " + field);
        }
    }

    private Comparator<Map<String, Object>> resourceComparator(String sort) {
        Comparator<Map<String, Object>> byId = Comparator.comparing(map -> str(map.getOrDefault("resourceId", "")));
        return switch (sort) {
            case "title_asc" -> Comparator.comparing((Map<String, Object> map) -> str(map.getOrDefault("title", ""))).thenComparing(byId);
            case "createdAt_desc" -> Comparator.comparing((Map<String, Object> map) -> str(map.getOrDefault("createdAt", ""))).reversed().thenComparing(byId);
            case "updatedAt_desc", "downloadUpdatedAt_desc" -> Comparator.comparing((Map<String, Object> map) -> str(map.getOrDefault("updatedAt", ""))).reversed().thenComparing(byId);
            case "publishedAt_desc" -> Comparator.comparing((Map<String, Object> map) -> str(map.getOrDefault("publishedAt", ""))).reversed().thenComparing(byId);
            default -> byId;
        };
    }

    private Comparator<Map<String, Object>> auditComparator(String sort) {
        Comparator<Map<String, Object>> byId = Comparator.comparing(map -> str(map.getOrDefault("id", "")));
        Comparator<Map<String, Object>> byCreatedAt = Comparator.comparing(map -> str(map.getOrDefault("createdAt", "")));
        return "createdAt_asc".equals(sort) ? byCreatedAt.thenComparing(byId) : byCreatedAt.reversed().thenComparing(byId);
    }

    private void requireSort(Map<String, String> query, Set<String> allowed) {
        if (query.containsKey("sort") && !allowed.contains(query.get("sort"))) {
            throw new ApiException(400, 40003, "invalid sort");
        }
    }

    private void requireEnum(Map<String, String> query, String key, Set<String> allowed) {
        if (query.containsKey(key) && !allowed.contains(query.get(key))) {
            throw new ApiException(400, 40001, "invalid " + key);
        }
    }

    private Map<String, Object> resource(String id) {
        Map<String, Object> item = resources.get(id);
        if (item == null) throw new ApiException(404, 43600, "resource not found");
        return item;
    }

    private Map<String, Object> category(String id) {
        Map<String, Object> item = categories.get(id);
        if (item == null) throw new ApiException(404, 43601, "category not found");
        return item;
    }

    private Map<String, Object> version(String resourceId, String versionId) {
        return versions.getOrDefault(resourceId, List.of()).stream()
                .filter(version -> versionId.equals(version.get("versionId")))
                .findFirst()
                .orElseThrow(() -> new ApiException(404, 43602, "version not found"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> entry(Map<String, Object> version) {
        Object entry = version.get("downloadEntry");
        if (!(entry instanceof Map<?, ?> map)) throw new ApiException(404, 43603, "download entry not found");
        return (Map<String, Object>) map;
    }

    private boolean hasPublishableVersion(String resourceId) {
        return versions.getOrDefault(resourceId, List.of()).stream()
                .anyMatch(version -> "ENABLED".equals(version.get("status")) && version.get("downloadEntry") != null && "ACTIVE".equals(entry(version).get("status")));
    }

    private String submitState(String current) {
        if (Set.of("DRAFT", "REJECTED", "NEEDS_CHANGES", "PENDING_REVIEW").contains(current)) return "PENDING_REVIEW";
        return null;
    }

    private void requireNotification(HttpServletRequest request) {
        if ("unavailable".equals(request.getHeader("X-Test-Notification-Mode"))) throw new ApiException(502, 46620, "notification unavailable");
        if ("timeout".equals(request.getHeader("X-Test-Notification-Mode"))) throw new ApiException(504, 46621, "notification timeout");
    }

    private void failAudit(HttpServletRequest request) {
        if ("true".equals(request.getHeader("X-Test-Fail-Audit"))) throw new ApiException(500, 51601, "audit failed");
    }

    private void validateReason(Map<String, Object> body) {
        if (str(body.get("reason")) == null || str(body.get("reason")).isBlank()) throw new ApiException(400, 40001, "reason required");
    }

    private void requireReview(Map<String, Object> body) {
        if (str(body.get("reviewOpinion")) == null || str(body.get("reviewOpinion")).isBlank()) throw new ApiException(400, 40001, "review required");
    }

    private void validateResourceBody(Map<String, Object> body, boolean create) {
        if (create && (str(body.get("title")) == null || str(body.get("slug")) == null || str(body.get("type")) == null || str(body.get("visibility")) == null)) throw new ApiException(400, 40001, "invalid resource");
        if (body.containsKey("slug") && !str(body.get("slug")).matches("[a-z0-9/-]{3,120}")) throw new ApiException(400, 40001, "invalid slug");
        if (body.containsKey("title") && str(body.get("title")).isBlank()) throw new ApiException(400, 40001, "invalid title");
        if (body.containsKey("coverUrl") && !validPublicUrl(str(body.get("coverUrl")))) throw new ApiException(400, 40001, "invalid coverUrl");
        Instant visibleFrom = body.containsKey("visibleFrom") ? parseInstant(str(body.get("visibleFrom")), "visibleFrom") : null;
        Instant visibleUntil = body.containsKey("visibleUntil") ? parseInstant(str(body.get("visibleUntil")), "visibleUntil") : null;
        if (visibleFrom != null && visibleUntil != null && visibleUntil.isBefore(visibleFrom)) throw new ApiException(400, 40001, "invalid time");
        if (body.containsKey("tags") && list(body.get("tags")).size() > 20) throw new ApiException(400, 40001, "too many tags");
    }

    private void validateVersionBody(Map<String, Object> body, boolean create) {
        if (create && (str(body.get("versionName")) == null || !(body.get("downloadEntry") instanceof Map<?, ?>))) throw new ApiException(400, 40001, "invalid version");
        if (body.containsKey("checksumSha256") && str(body.get("checksumSha256")) != null && !str(body.get("checksumSha256")).matches("[a-f0-9]{64}")) throw new ApiException(400, 40001, "invalid checksum");
        if (body.containsKey("fileSizeBytes") && intValue(body.get("fileSizeBytes"), 0) < 0) throw new ApiException(400, 40001, "invalid size");
        if (body.containsKey("downloadEntry")) {
            Map<String, Object> entry = map(body.get("downloadEntry"));
            if (str(entry.get("provider")) == null || str(entry.get("status")) == null || (entry.get("shareUrl") == null && entry.get("url") == null)) throw new ApiException(400, 40001, "invalid entry");
            String url = str(entry.getOrDefault("shareUrl", entry.get("url")));
            if (!validPublicUrl(url)) throw new ApiException(400, 40001, "invalid entry url");
            parseInstant(str(entry.get("expiresAt")), "expiresAt");
        }
    }

    private void validateCategoryBody(Map<String, Object> body, boolean create) {
        if (create && (str(body.get("name")) == null || str(body.get("slug")) == null)) throw new ApiException(400, 40001, "invalid category");
        if (body.containsKey("slug") && !str(body.get("slug")).matches("[a-z0-9-]{2,80}")) throw new ApiException(400, 40001, "invalid slug");
        if (body.containsKey("name") && str(body.get("name")).length() < 2) throw new ApiException(400, 40001, "invalid name");
        if (body.containsKey("icon") && str(body.get("icon")) != null && str(body.get("icon")).length() > 120) throw new ApiException(400, 40001, "invalid icon");
    }

    private Instant parseInstant(String value, String field) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (Exception ex) {
            throw new ApiException(400, 40001, "invalid " + field);
        }
    }

    private boolean validPublicUrl(String value) {
        return value == null || value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/");
    }

    private void checkProfile(String memberId) {
        if (memberId == null) return;
        if ("profile-timeout".equals(memberId)) throw new ApiException(504, 46611, "profile timeout");
        if ("profile-bad".equals(memberId)) throw new ApiException(502, 46612, "profile bad");
        if ("missing".equals(memberId)) throw new ApiException(502, 46610, "profile unavailable");
    }

    private Map<String, Object> addCategory(String id, String name, String slug, boolean enabled, boolean archived) {
        Map<String, Object> category = mapOf("categoryId", id, "name", name, "slug", slug, "description", null, "icon", null, "sortOrder", 10, "enabled", enabled, "archived", archived, "createdAt", now(), "updatedAt", now(), "archivedAt", archived ? now() : null);
        categories.put(id, category);
        return category;
    }

    private Map<String, Object> addSeedResource(String id, String slug, String type, String visibility, String status, String categoryId, List<String> tags) {
        Map<String, Object> item = baseResource(id, slug, type, visibility, status, categoryId, tags);
        resources.put(id, item);
        return item;
    }

    private Map<String, Object> baseResource(String id, String slug, String type, String visibility, String status, String categoryId, List<String> tags) {
        return mapOf("resourceId", id, "status", status, "type", type, "visibility", visibility, "slug", slug, "title", title(slug), "summary", "summary " + slug, "description", "description " + slug, "coverUrl", "/assets/" + slug + ".png", "categoryId", categoryId, "tags", tags, "maintainerMemberId", "member-active", "maintainerSnapshot", mapOf("memberId", "member-active", "displayName", "Active Member", "avatarUrl", null, "minecraftId", "Steve", "memberStatus", "ACTIVE", "snapshotAt", now()), "adminNote", "secret admin note", "reviewOpinion", null, "notificationStatus", null, "submittedAt", null, "reviewedAt", null, "publishedAt", "PUBLISHED".equals(status) ? "2026-05-22T00:00:00Z" : null, "visibleFrom", "2026-05-20T00:00:00Z", "visibleUntil", "2026-12-31T00:00:00Z", "createdBy", "admin", "updatedBy", "admin", "createdAt", "2026-05-20T00:00:00Z", "updatedAt", "2026-05-22T00:00:00Z", "deletedAt", "DELETED".equals(status) ? now() : null);
    }

    private Map<String, Object> version(String id, String name, String status, String entryStatus, String provider, String url, String cloudMode) {
        Map<String, Object> version = mapOf("versionId", id, "resourceId", null, "status", status, "versionName", name, "title", "Version " + name, "changelog", "changelog", "minecraftVersions", List.of("1.20.1"), "loader", "Fabric", "fileSizeBytes", 1024, "checksumSha256", "a".repeat(64), "releasedAt", "2026-05-22T00:00:00Z", "createdBy", "admin", "updatedBy", "admin", "createdAt", "2026-05-22T00:00:00Z", "updatedAt", "2026-05-22T00:00:00Z");
        if (entryStatus != null) {
            version.put("downloadEntry", mapOf("downloadEntryId", "entry-" + id, "provider", provider, "status", entryStatus, "displayName", provider, "shareUrl", url, "lastCheckedAt", now(), "expiresAt", "EXPIRED".equals(entryStatus) ? "2020-01-01T00:00:00Z" : "2026-12-31T00:00:00Z", "adminNote", "entry note", "cloudMode", cloudMode, "passwordRequired", "PASSWORD".equals(cloudMode)));
        }
        return version;
    }

    private void addVersion(String resourceId, Map<String, Object> version) {
        version.put("resourceId", resourceId);
        versions.computeIfAbsent(resourceId, ignored -> new ArrayList<>()).add(version);
    }

    private void copyResourceFields(Map<String, Object> from, Map<String, Object> to) {
        for (String key : List.of("type", "visibility", "slug", "title", "summary", "description", "coverUrl", "categoryId", "tags", "maintainerMemberId", "visibleFrom", "visibleUntil", "adminNote")) {
            if (from.containsKey(key)) to.put(key, from.get(key));
        }
    }

    private void copyVersionFields(Map<String, Object> from, Map<String, Object> to) {
        for (String key : List.of("versionName", "title", "changelog", "minecraftVersions", "loader", "fileSizeBytes", "checksumSha256", "releasedAt")) {
            if (from.containsKey(key)) to.put(key, from.get(key));
        }
        if (from.containsKey("downloadEntry")) {
            Map<String, Object> entryBody = map(from.get("downloadEntry"));
            to.put("downloadEntry", mapOf("downloadEntryId", "entry-" + to.get("versionId"), "provider", entryBody.get("provider"), "status", entryBody.getOrDefault("status", "ACTIVE"), "displayName", entryBody.getOrDefault("displayName", "download"), "shareUrl", entryBody.getOrDefault("shareUrl", entryBody.get("url")), "lastCheckedAt", now(), "expiresAt", entryBody.get("expiresAt"), "adminNote", entryBody.get("adminNote"), "cloudMode", null, "passwordRequired", false));
        }
    }

    private Map<String, Object> idempotent(String key, Map<String, Object> body, Map<String, Object> value) {
        String fingerprint = stable(body);
        Map<String, Object> existing = idem.get(key);
        if (existing != null) {
            if (!fingerprint.equals(existing.get("fingerprint"))) throw new ApiException(409, 43612, "idempotency conflict");
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) existing.get("value");
            return result;
        }
        if (value != null) idem.put(key, mapOf("fingerprint", fingerprint, "value", value));
        return null;
    }

    private String stable(Object value) {
        if (value instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Object key : new java.util.TreeSet<>(map.keySet())) {
                if (!first) builder.append(',');
                first = false;
                builder.append(key).append(':').append(stable(map.get(key)));
            }
            return builder.append('}').toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder builder = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) builder.append(',');
                builder.append(stable(list.get(i)));
            }
            return builder.append(']').toString();
        }
        return Objects.toString(value);
    }

    private void recordDownload(AuthUser user, Map<String, Object> ticket, String clientLabel, String result) {
        downloadRecords.add(mapOf(
                "ticketId", ticket.get("ticketId"),
                "resourceId", ticket.get("resourceId"),
                "versionId", ticket.get("versionId"),
                "downloadEntryId", ticket.get("downloadEntryId"),
                "actorUserId", user == null ? null : user.id(),
                "anonymous", user == null,
                "clientLabel", clientLabel,
                "provider", ticket.get("provider"),
                "result", result,
                "degraded", ticket.get("degraded"),
                "requestId", ResourceController.currentRequestId(),
                "createdAt", now()));
    }

    private Map<String, Object> snapshot(Map<String, Object> value) {
        return new LinkedHashMap<>(value);
    }

    private void audit(AuthUser actor, String targetId, String action, String result, Map<String, Object> before, Map<String, Object> after, String reason) {
        String actorId = actor == null ? "anonymous" : actor.id();
        String actorRole = actor == null || actor.roles().isEmpty() ? "ANONYMOUS" : actor.roles().iterator().next();
        audits.add(mapOf("id", "audit-" + (++idSeq), "requestId", ResourceController.currentRequestId(), "actorUserId", actorId, "actorRole", actorRole, "actorPermissions", List.of(), "sourceIp", null, "targetType", "RESOURCE", "targetId", targetId, "action", action, "riskLevel", "MEDIUM", "reason", reason == null ? "contract" : reason, "paramsSummary", Map.of(), "beforeState", before, "afterState", after, "result", result, "failureReason", null, "createdAt", now()));
    }

    private String title(String slug) {
        String[] parts = slug.split("-");
        StringBuilder title = new StringBuilder();
        for (String part : parts) {
            if (!title.isEmpty()) title.append(' ');
            title.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return title.toString();
    }

    private static String now() {
        return "2026-05-22T00:00:00Z";
    }

    @SafeVarargs
    private static Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) map.put(values[i].toString(), values[i + 1]);
        return map;
    }

    private static void copy(Map<String, Object> target, Map<String, Object> source, String... keys) {
        for (String key : keys) target.put(key, source.get(key));
    }

    @SuppressWarnings("unchecked")
    private static List<String> list(Object value) {
        if (value instanceof List<?> list) return (List<String>) list;
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> map) return (Map<String, Object>) map;
        throw new ApiException(400, 40001, "invalid object");
    }

    private static String str(Object value) {
        return value == null ? null : value.toString();
    }

    private static boolean bool(Object value) {
        if (value instanceof Boolean b) return b;
        if ("true".equals(value) || "false".equals(value)) return Boolean.parseBoolean(value.toString());
        throw new ApiException(400, 40001, "invalid boolean");
    }

    private static int intValue(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(value.toString());
    }
}

record AuthUser(String id, Set<String> roles, String memberStatus) {
}

class TestResourceAuthProvider {
    AuthUser requireAuthenticated(String authorization) {
        if (authorization == null || authorization.isBlank()) throw new ApiException(401, 41000, "not logged in");
        String token = authorization.replace("Bearer ", "");
        return switch (token) {
            case "auth-unavailable-token" -> throw new ApiException(502, 46600, "auth unavailable");
            case "auth-timeout-token" -> throw new ApiException(504, 46601, "auth timeout");
            case "auth-bad-token" -> throw new ApiException(502, 46602, "auth incompatible");
            case "admin-token" -> new AuthUser("admin", Set.of("ADMIN"), null);
            case "owner-token" -> new AuthUser("owner", Set.of("OWNER"), null);
            case "helper-token" -> new AuthUser("helper", Set.of("HELPER"), null);
            case "member-token" -> new AuthUser("member", Set.of("USER"), "ACTIVE");
            case "inactive-member-token" -> new AuthUser("inactive", Set.of("USER"), "INACTIVE");
            case "suspended-member-token" -> new AuthUser("suspended", Set.of("USER"), "SUSPENDED");
            case "removed-member-token" -> new AuthUser("removed", Set.of("USER"), "REMOVED");
            case "profile-unavailable-token" -> new AuthUser("profile-unavailable", Set.of("USER"), "PROFILE_UNAVAILABLE");
            case "profile-timeout-token" -> new AuthUser("profile-timeout", Set.of("USER"), "PROFILE_TIMEOUT");
            case "profile-bad-token" -> new AuthUser("profile-bad", Set.of("USER"), "PROFILE_BAD");
            case "disabled-token" -> throw new ApiException(502, 46600, "auth unavailable");
            default -> new AuthUser("user", Set.of("USER"), null);
        };
    }

    AuthUser requireAny(String authorization, String... roles) {
        AuthUser user = requireAuthenticated(authorization);
        Set<String> allowed = new LinkedHashSet<>(List.of(roles));
        if (user.roles().stream().noneMatch(allowed::contains)) {
            throw new ApiException(403, 42001, "role permission denied");
        }
        return user;
    }

    void requireMember(AuthUser user) {
        if ("PROFILE_UNAVAILABLE".equals(user.memberStatus())) throw new ApiException(502, 46610, "profile unavailable");
        if ("PROFILE_TIMEOUT".equals(user.memberStatus())) throw new ApiException(504, 46611, "profile timeout");
        if ("PROFILE_BAD".equals(user.memberStatus())) throw new ApiException(502, 46612, "profile incompatible");
        if (!"ACTIVE".equals(user.memberStatus()) && !"INACTIVE".equals(user.memberStatus())) throw new ApiException(403, 42001, "member permission denied");
    }
}

@Component
class ResourceRequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) requestId = "req_" + UUID.randomUUID();
        request.setAttribute("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        filterChain.doFilter(request, response);
    }
}

@RestControllerAdvice
class ResourceExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> handle(ApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.code);
        body.put("message", ex.getMessage());
        body.put("data", null);
        body.put("errors", List.of());
        body.put("requestId", ResourceController.currentRequestId());
        return ResponseEntity.status(ex.httpStatus).body(body);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 51600);
        body.put("message", "resource internal error");
        body.put("data", null);
        body.put("requestId", ResourceController.currentRequestId());
        return ResponseEntity.status(500).body(body);
    }
}

class ApiException extends RuntimeException {
    final int httpStatus;
    final int code;

    ApiException(int httpStatus, int code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }
}
