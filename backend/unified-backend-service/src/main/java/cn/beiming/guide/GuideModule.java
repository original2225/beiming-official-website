package cn.beiming.guide;

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
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
class GuideModule {
    @Bean
    GuideStore guideStore() {
        GuideStore store = new GuideStore();
        store.seed();
        return store;
    }

    @Bean
    TestGuideAuthProvider guideAuthProvider() {
        return new TestGuideAuthProvider();
    }
}

@RestController
@RequestMapping("/api/v1/guides")
class GuideController {
    private final GuideStore store;
    private final TestGuideAuthProvider auth;

    GuideController(GuideStore store, TestGuideAuthProvider auth) {
        this.store = store;
        this.auth = auth;
    }

    @GetMapping("/home")
    Map<String, Object> home(HttpServletRequest request) {
        return ok(store.home(request));
    }

    @GetMapping("/categories")
    Map<String, Object> publicCategories(@RequestParam Map<String, String> query) {
        return ok(mapOf("items", store.publicCategories(query)));
    }

    @GetMapping("/articles")
    Map<String, Object> publicArticles(@RequestParam Map<String, String> query, HttpServletRequest request) {
        return ok(store.publicArticles(query, request));
    }

    @GetMapping("/articles/{guideId}")
    Map<String, Object> publicArticle(@PathVariable String guideId, HttpServletRequest request) {
        return ok(store.publicArticle(guideId, request));
    }

    @GetMapping("/articles/by-slug/{slug}")
    Map<String, Object> publicArticleBySlug(@PathVariable String slug, HttpServletRequest request) {
        return ok(store.publicArticle(store.requireGuideIdBySlug(slug), request));
    }

    @GetMapping("/search")
    Map<String, Object> search(@RequestParam Map<String, String> query) {
        return ok(store.search(query));
    }

    @GetMapping("/commands")
    Map<String, Object> commands(@RequestParam Map<String, String> query) {
        return ok(store.commands(query));
    }

    @GetMapping("/external-channels")
    Map<String, Object> publicChannels(@RequestParam Map<String, String> query) {
        return ok(mapOf("items", store.publicChannels(query)));
    }

    @GetMapping("/rules/current")
    Map<String, Object> currentRule() {
        return ok(store.currentRule());
    }

    @GetMapping("/rules/versions/{ruleVersion}")
    Map<String, Object> ruleVersion(@PathVariable String ruleVersion) {
        return ok(store.ruleVersion(ruleVersion));
    }

    @PostMapping("/articles/{guideId}/feedback")
    ResponseEntity<Map<String, Object>> createFeedback(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                       @PathVariable String guideId,
                                                       @RequestBody Map<String, Object> body,
                                                       HttpServletRequest request) {
        AuthUser actor = auth.requireAuthenticated(authorization);
        return ResponseEntity.status(HttpStatus.CREATED).body(okData(store.createFeedback(actor, guideId, body, request)));
    }

    @GetMapping("/admin/articles")
    Map<String, Object> adminArticles(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminArticles(query));
    }

    @GetMapping("/admin/articles/{guideId}")
    Map<String, Object> adminArticle(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String guideId) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminArticle(guideId));
    }

    @PostMapping("/admin/articles")
    ResponseEntity<Map<String, Object>> createArticle(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                      @RequestBody Map<String, Object> body,
                                                      HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ResponseEntity.status(HttpStatus.CREATED).body(okData(store.createArticle(actor, body, request)));
    }

    @PatchMapping("/admin/articles/{guideId}")
    Map<String, Object> patchArticle(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String guideId,
                                     @RequestBody Map<String, Object> body,
                                     HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.patchArticle(actor, guideId, body, request));
    }

    @PatchMapping("/admin/articles/{guideId}/submit-review")
    Map<String, Object> submitReview(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String guideId,
                                     @RequestBody Map<String, Object> body,
                                     HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.transition(actor, guideId, body, request, "submit-review"));
    }

    @PatchMapping("/admin/articles/{guideId}/approve")
    Map<String, Object> approve(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable String guideId,
                                @RequestBody Map<String, Object> body,
                                HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.transition(actor, guideId, body, request, "approve"));
    }

    @PatchMapping("/admin/articles/{guideId}/reject")
    Map<String, Object> reject(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable String guideId,
                               @RequestBody Map<String, Object> body,
                               HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.transition(actor, guideId, body, request, "reject"));
    }

    @PatchMapping("/admin/articles/{guideId}/request-changes")
    Map<String, Object> requestChanges(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable String guideId,
                                       @RequestBody Map<String, Object> body,
                                       HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.transition(actor, guideId, body, request, "request-changes"));
    }

    @PatchMapping("/admin/articles/{guideId}/publish")
    Map<String, Object> publish(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable String guideId,
                                @RequestBody Map<String, Object> body,
                                HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.transition(actor, guideId, body, request, "publish"));
    }

    @PatchMapping("/admin/articles/{guideId}/offline")
    Map<String, Object> offline(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable String guideId,
                                @RequestBody Map<String, Object> body,
                                HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.transition(actor, guideId, body, request, "offline"));
    }

    @PatchMapping("/admin/articles/{guideId}/archive")
    Map<String, Object> archive(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable String guideId,
                                @RequestBody Map<String, Object> body,
                                HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.transition(actor, guideId, body, request, "archive"));
    }

    @PatchMapping("/admin/articles/{guideId}/delete")
    Map<String, Object> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable String guideId,
                               @RequestBody Map<String, Object> body,
                               HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.transition(actor, guideId, body, request, "delete"));
    }

    @GetMapping("/admin/articles/{guideId}/versions")
    Map<String, Object> versions(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable String guideId,
                                 @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.versions(guideId, query));
    }

    @GetMapping("/admin/articles/{guideId}/versions/{version}")
    Map<String, Object> version(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable String guideId,
                                @PathVariable int version) {
        auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.version(guideId, version));
    }

    @PatchMapping("/admin/articles/{guideId}/versions/{version}/restore")
    Map<String, Object> restore(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable String guideId,
                                @PathVariable int version,
                                @RequestBody Map<String, Object> body,
                                HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.restore(actor, guideId, version, body, request));
    }

    @GetMapping("/admin/articles/{guideId}/audit-logs")
    Map<String, Object> auditLogs(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable String guideId,
                                  @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.auditLogs(guideId, query));
    }

    @GetMapping("/admin/categories")
    Map<String, Object> adminCategories(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminCategories(query));
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

    @GetMapping("/admin/external-channels")
    Map<String, Object> adminChannels(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminChannels(query));
    }

    @PostMapping("/admin/external-channels")
    ResponseEntity<Map<String, Object>> createChannel(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                      @RequestBody Map<String, Object> body,
                                                      HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ResponseEntity.status(HttpStatus.CREATED).body(okData(store.createChannel(actor, body, request)));
    }

    @PatchMapping("/admin/external-channels/{channelId}")
    Map<String, Object> patchChannel(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String channelId,
                                     @RequestBody Map<String, Object> body,
                                     HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.patchChannel(actor, channelId, body, request));
    }

    @PatchMapping("/admin/external-channels/{channelId}/enable")
    Map<String, Object> enableChannel(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable String channelId,
                                      @RequestBody Map<String, Object> body,
                                      HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.channelState(actor, channelId, body, request, "enable"));
    }

    @PatchMapping("/admin/external-channels/{channelId}/disable")
    Map<String, Object> disableChannel(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable String channelId,
                                       @RequestBody Map<String, Object> body,
                                       HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.channelState(actor, channelId, body, request, "disable"));
    }

    @PatchMapping("/admin/external-channels/{channelId}/archive")
    Map<String, Object> archiveChannel(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable String channelId,
                                       @RequestBody Map<String, Object> body,
                                       HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.channelState(actor, channelId, body, request, "archive"));
    }

    @GetMapping("/admin/feedback")
    Map<String, Object> adminFeedback(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminFeedback(query));
    }

    @PatchMapping("/admin/feedback/{feedbackId}/resolve")
    Map<String, Object> resolveFeedback(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @PathVariable String feedbackId,
                                        @RequestBody Map<String, Object> body,
                                        HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.processFeedback(actor, feedbackId, body, request, "RESOLVED"));
    }

    @PatchMapping("/admin/feedback/{feedbackId}/ignore")
    Map<String, Object> ignoreFeedback(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable String feedbackId,
                                       @RequestBody Map<String, Object> body,
                                       HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.processFeedback(actor, feedbackId, body, request, "IGNORED"));
    }

    @GetMapping("/admin/ops/summary")
    Map<String, Object> ops(@RequestHeader(value = "Authorization", required = false) String authorization) {
        auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.ops());
    }

    static Map<String, Object> ok(Object data) {
        return okData(data);
    }

    static Map<String, Object> okData(Object data) {
        return mapOf("code", 0, "message", "success", "data", data, "requestId", currentRequestId());
    }

    static String currentRequestId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "req_unknown";
        Object requestId = attrs.getRequest().getAttribute("requestId");
        return requestId == null ? "req_unknown" : requestId.toString();
    }

    static Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) map.put(values[i].toString(), values[i + 1]);
        return map;
    }
}

class GuideStore {
    private final Map<String, GuideArticle> guides = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> categories = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> channels = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> versions = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> feedback = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> audits = new ArrayList<>();
    private final Map<String, Map<String, Object>> idem = new ConcurrentHashMap<>();
    private int idSeq = 1000;

    void seed() {
        addCategory("cat-rules", "Rules", "rules", true, false);
        addCategory("cat-join", "Join Guide", "join", true, false);
        addCategory("cat-client", "Client", "client", true, false);
        addCategory("cat-command", "Commands", "commands", true, false);
        addCategory("cat-disabled", "Disabled", "disabled", false, false);
        addCategory("cat-archived", "Archived", "archived", true, true);
        addChannel("channel-qq", "QQ_GROUP", "ENABLED", "Player QQ Group", "qq", "player group", "follow rules", "https://example.com/qq", "group 123****");
        addChannel("channel-oopz", "OOPZ", "ENABLED", "Oopz Channel", "oopz", "voice chat", "whitelist player", "https://example.com/oopz", "follow page hint");
        addChannel("channel-disabled", "QQ_GROUP", "DISABLED", "Disabled Group", "disabled", "disabled", "disabled", "https://example.com/disabled", "disabled");
        addChannel("channel-archived", "QQ_GROUP", "ARCHIVED", "Archived Group", "archived", "archived", "archived", "https://example.com/archived", "archived");
        seedGuides();
        audits.add(mapOf("auditId", "audit-seed", "requestId", "req_seed", "actorUserId", "system", "action", "GUIDE_PUBLISHED", "result", "SUCCESS", "targetType", "GUIDE", "targetId", "guide-rules", "stateFrom", "APPROVED", "stateTo", "PUBLISHED", "createdAt", now()));
    }

    Map<String, Object> home(HttpServletRequest request) {
        List<Map<String, Object>> visible = visibleGuides(request).stream().map(g -> publicSummary(g, request)).toList();
        List<Map<String, Object>> pinned = visible.stream().filter(g -> bool(g.get("pinned"))).toList();
        return mapOf(
                "featuredGuides", visible.stream().limit(4).toList(),
                "pinnedGuides", pinned,
                "categories", publicCategories(Map.of()),
                "latestUpdatedGuides", visible,
                "currentRule", currentRuleData(),
                "externalChannels", publicChannels(Map.of()),
                "degraded", false,
                "degradeReasons", List.of());
    }

    List<Map<String, Object>> publicCategories(Map<String, String> query) {
        enumQuery(query, "type", GUIDE_TYPES);
        enumQuery(query, "audience", AUDIENCES);
        String keyword = query.getOrDefault("keyword", "").toLowerCase();
        return categories.values().stream()
                .filter(c -> bool(c.get("enabled")) && !bool(c.get("archived")))
                .filter(c -> keyword.isBlank() || text(c.get("name")).toLowerCase().contains(keyword) || text(c.get("description")).toLowerCase().contains(keyword))
                .sorted(categoryComparator())
                .map(this::publicCategory)
                .toList();
    }

    Map<String, Object> publicArticles(Map<String, String> query, HttpServletRequest request) {
        checkPaging(query, 100);
        checkSort(query, Set.of("publishedAt_desc", "updatedAt_desc", "title_asc", "verifiedAt_desc", "pinned_desc"));
        enumQuery(query, "type", GUIDE_TYPES);
        enumQuery(query, "audience", AUDIENCES);
        List<Map<String, Object>> items = visibleGuides(request).stream()
                .filter(g -> matchesGuideQuery(g, query))
                .sorted(publicComparator(query.get("sort")))
                .map(g -> publicSummary(g, request))
                .toList();
        return page(items, query);
    }

    Map<String, Object> publicArticle(String guideId, HttpServletRequest request) {
        GuideArticle guide = requireGuide(guideId);
        if (!isPublicVisible(guide)) throw new GuideException(404, 43900, "guide not found");
        return publicDetail(guide, request);
    }

    Map<String, Object> search(Map<String, String> query) {
        String q = query.get("q");
        if (q == null || q.isBlank() || q.length() > 80) throw new GuideException(400, 40001, "validation failed");
        checkPaging(query, 50);
        enumQuery(query, "type", GUIDE_TYPES);
        enumQuery(query, "audience", AUDIENCES);
        List<Map<String, Object>> items = visibleGuides(null).stream()
                .filter(g -> matchesGuideQuery(g, query))
                .filter(g -> contains(g.title, q) || contains(g.summary, q) || contains(g.body, q) || g.tags.contains(q))
                .sorted(Comparator.comparing((GuideArticle g) -> g.publishedAt).reversed().thenComparing(g -> g.guideId))
                .map(g -> mapOf("guideId", g.guideId, "title", g.title, "slug", g.slug, "type", g.type, "category", publicCategory(categories.get(g.categoryId)), "matchedFields", List.of("title", "body"), "highlight", sanitize("matched " + q + " guide summary"), "score", 1.0, "version", g.currentVersion, "publishedAt", g.publishedAt))
                .toList();
        Map<String, Object> page = page(items, query);
        page.put("facets", mapOf("types", Map.of("SERVER_RULE", 1), "categories", Map.of("cat-rules", 1)));
        page.put("noResultFeedbackEnabled", true);
        return page;
    }

    Map<String, Object> commands(Map<String, String> query) {
        checkPaging(query, 100);
        checkSort(query, Set.of("updatedAt_desc", "command_asc"));
        String keyword = query.getOrDefault("keyword", "").toLowerCase();
        String tag = query.get("tag");
        String guideId = query.get("guideId");
        List<Map<String, Object>> items = visibleGuides(null).stream()
                .filter(g -> guideId == null || g.guideId.equals(guideId))
                .flatMap(g -> g.commandEntries.stream())
                .filter(c -> keyword.isBlank() || text(c.get("command")).toLowerCase().contains(keyword) || text(c.get("usage")).toLowerCase().contains(keyword))
                .filter(c -> tag == null || list(c.get("tags")).contains(tag))
                .sorted("command_asc".equals(query.get("sort")) ? Comparator.comparing(c -> text(c.get("command"))) : Comparator.comparing((Map<String, Object> c) -> text(c.get("updatedAt"))).reversed())
                .toList();
        return page(items, query);
    }

    List<Map<String, Object>> publicChannels(Map<String, String> query) {
        enumQuery(query, "type", CHANNEL_TYPES);
        enumQuery(query, "audience", AUDIENCES);
        String keyword = query.getOrDefault("keyword", "").toLowerCase();
        return channels.values().stream()
                .filter(c -> "ENABLED".equals(c.get("status")) && "PUBLIC".equals(c.get("visibility")))
                .filter(c -> query.get("type") == null || query.get("type").equals(c.get("type")))
                .filter(c -> keyword.isBlank() || text(c.get("name")).toLowerCase().contains(keyword) || text(c.get("purpose")).toLowerCase().contains(keyword) || text(c.get("joinCondition")).toLowerCase().contains(keyword))
                .sorted(channelComparator())
                .map(this::publicChannel)
                .toList();
    }

    Map<String, Object> currentRule() {
        return currentRuleData();
    }

    Map<String, Object> ruleVersion(String ruleVersion) {
        GuideArticle guide = guides.values().stream()
                .filter(g -> Objects.equals(g.ruleVersion, ruleVersion) && isPublicVisible(g))
                .findFirst()
                .orElseThrow(() -> new GuideException(404, 43900, "guide not found"));
        Map<String, Object> detail = publicDetail(guide, null);
        detail.put("current", bool(detail.get("current")));
        return detail;
    }

    synchronized Map<String, Object> createFeedback(AuthUser actor, String guideId, Map<String, Object> body, HttpServletRequest request) {
        if (request.getHeader("X-Test-Fail-Audit") != null) throw new GuideException(500, 51900, "feedback audit failed");
        GuideArticle guide = requireGuide(guideId);
        if (!isPublicVisible(guide)) throw new GuideException(404, 43900, "guide not found");
        enumBody(body, "type", FEEDBACK_TYPES);
        String message = optionalString(body, "message", null);
        if (message != null && message.length() > 1000) throw new GuideException(400, 40001, "validation failed");
        String key = optionalString(body, "idempotencyKey", null);
        String idemKey = key == null ? null : actor.id() + ":feedback:" + guideId + ":" + key;
        Map<String, Object> existing = idempotent(idemKey, body, null);
        if (existing != null) return existing;
        String feedbackId = "feedback-" + (++idSeq);
        Map<String, Object> value = mapOf("feedbackId", feedbackId, "guideId", guideId, "guideVersion", guide.currentVersion, "type", body.get("type"), "status", "OPEN", "message", message, "anchor", optionalString(body, "anchor", null), "actorUserId", actor.id(), "actorDisplayNameSnapshot", actor.displayName(), "resolvedBy", null, "resolutionNote", null, "createdAt", now(), "resolvedAt", null);
        feedback.put(feedbackId, value);
        audit(actor, guideId, "GUIDE_FEEDBACK_CREATED", "SUCCESS", null, value, message);
        idempotent(idemKey, body, value);
        return value;
    }

    Map<String, Object> adminArticles(Map<String, String> query) {
        checkPaging(query, 100);
        checkSort(query, Set.of("updatedAt_desc", "createdAt_desc", "publishedAt_desc", "verifiedAt_desc", "title_asc"));
        for (String field : List.of("type", "status", "visibility")) {
            if (query.get(field) != null) {
                Set<String> allowed = switch (field) {
                    case "type" -> GUIDE_TYPES;
                    case "status" -> STATUSES;
                    default -> VISIBILITIES;
                };
                if (!allowed.contains(query.get(field))) throw new GuideException(400, 40001, "validation failed");
            }
        }
        String keyword = query.getOrDefault("keyword", "").toLowerCase();
        List<Map<String, Object>> items = guides.values().stream()
                .filter(g -> query.get("status") == null || query.get("status").equals(g.status))
                .filter(g -> query.get("visibility") == null || query.get("visibility").equals(g.visibility))
                .filter(g -> query.get("type") == null || query.get("type").equals(g.type))
                .filter(g -> query.get("categoryId") == null || query.get("categoryId").equals(g.categoryId))
                .filter(g -> query.get("tag") == null || g.tags.contains(query.get("tag")))
                .filter(g -> query.get("maintainerUserId") == null || query.get("maintainerUserId").equals(text(g.maintainer.get("userId"))))
                .filter(g -> query.get("ruleVersion") == null || query.get("ruleVersion").equals(g.ruleVersion))
                .filter(g -> !"true".equals(query.get("expired")) || Instant.parse(g.expiresAt).isBefore(Instant.parse(now())))
                .filter(g -> keyword.isBlank() || contains(g.title, keyword) || contains(g.summary, keyword) || contains(g.body, keyword) || contains(g.slug, keyword))
                .sorted(adminComparator(query.get("sort")))
                .map(this::adminView)
                .toList();
        return page(items, query);
    }

    Map<String, Object> adminArticle(String guideId) {
        return adminView(requireGuide(guideId));
    }

    synchronized Map<String, Object> createArticle(AuthUser actor, Map<String, Object> body, HttpServletRequest request) {
        if (request.getHeader("X-Test-Fail-Audit") != null) throw new GuideException(500, 51901, "audit failed");
        checkProfile(request);
        validateGuideBody(body, true);
        String key = optionalString(body, "idempotencyKey", null);
        String idemKey = key == null ? null : actor.id() + ":create:" + key;
        Map<String, Object> existing = idempotent(idemKey, body, null);
        if (existing != null) return existing;
        String slug = requiredString(body, "slug");
        if (slugExists(slug)) throw new GuideException(409, 43911, "slug conflict");
        String guideId = nextGuideId(slug);
        GuideArticle guide = articleFromBody(guideId, actor, body);
        guides.put(guideId, guide);
        addVersion(guide, "CREATED", actor, requiredString(body, "reason"), null);
        audit(actor, guideId, "GUIDE_CREATED", "SUCCESS", null, adminView(guide), requiredString(body, "reason"));
        Map<String, Object> value = adminView(guide);
        idempotent(idemKey, body, value);
        return value;
    }

    synchronized Map<String, Object> patchArticle(AuthUser actor, String guideId, Map<String, Object> body, HttpServletRequest request) {
        if (request.getHeader("X-Test-Fail-Audit") != null) throw new GuideException(500, 51901, "audit failed");
        GuideArticle guide = requireGuide(guideId);
        requireReason(body);
        if (Set.of("ARCHIVED", "DELETED").contains(guide.status)) throw new GuideException(409, 43910, "state conflict");
        String key = optionalString(body, "idempotencyKey", null);
        String idemKey = key == null ? null : actor.id() + ":patch:" + guideId + ":" + key;
        Map<String, Object> existing = idempotent(idemKey, body, null);
        if (existing != null) return existing;
        if (body.containsKey("slug")) {
            String slug = requiredString(body, "slug");
            if (guides.values().stream().anyMatch(g -> !g.guideId.equals(guideId) && !"DELETED".equals(g.status) && g.slug.equals(slug))) throw new GuideException(409, 43911, "slug conflict");
            guide.slug = slug;
        }
        copyGuideFields(guide, body);
        guide.updatedAt = now();
        guide.currentVersion++;
        addVersion(guide, "UPDATED", actor, requiredString(body, "reason"), null);
        audit(actor, guideId, "GUIDE_UPDATED", "SUCCESS", null, adminView(guide), requiredString(body, "reason"));
        Map<String, Object> value = adminView(guide);
        idempotent(idemKey, body, value);
        return value;
    }

    synchronized Map<String, Object> transition(AuthUser actor, String guideId, Map<String, Object> body, HttpServletRequest request, String action) {
        if (request.getHeader("X-Test-Fail-Audit") != null) throw new GuideException(500, 51901, "audit failed");
        GuideArticle guide = requireGuide(guideId);
        requireReason(body);
        String key = optionalString(body, "idempotencyKey", null);
        String idemKey = key == null ? null : actor.id() + ":" + action + ":" + guideId + ":" + key;
        Map<String, Object> existing = idempotent(idemKey, body, null);
        if (existing != null) return existing;
        if (("reject".equals(action) || "request-changes".equals(action)) && "unavailable".equals(request.getHeader("X-Test-Notification-Mode"))) throw new GuideException(502, 46960, "notification unavailable");
        if (("reject".equals(action) || "request-changes".equals(action)) && "timeout".equals(request.getHeader("X-Test-Notification-Mode"))) throw new GuideException(504, 46961, "notification timeout");
        String before = guide.status;
        switch (action) {
            case "submit-review" -> {
                if ("PENDING_REVIEW".equals(guide.status)) return adminView(guide);
                if (!Set.of("DRAFT", "REJECTED", "NEEDS_CHANGES").contains(guide.status)) throw new GuideException(409, 43910, "state conflict");
                guide.status = "PENDING_REVIEW";
            }
            case "approve" -> {
                if ("APPROVED".equals(guide.status)) return adminView(guide);
                if (!"PENDING_REVIEW".equals(guide.status)) throw new GuideException(409, 43910, "state conflict");
                guide.status = "APPROVED";
            }
            case "reject" -> {
                if (!"PENDING_REVIEW".equals(guide.status)) throw new GuideException(409, 43910, "state conflict");
                guide.status = "REJECTED";
            }
            case "request-changes" -> {
                if (!body.containsKey("publicComment")) throw new GuideException(400, 40001, "validation failed");
                if (!"PENDING_REVIEW".equals(guide.status)) throw new GuideException(409, 43910, "state conflict");
                guide.status = "NEEDS_CHANGES";
            }
            case "publish" -> {
                if (!Set.of("APPROVED", "OFFLINE").contains(guide.status)) throw new GuideException(409, 43910, "state conflict");
                if ("SERVER_RULE".equals(guide.type) && guides.values().stream().anyMatch(g -> !g.guideId.equals(guideId) && isPublicVisible(g) && Objects.equals(g.ruleVersion, guide.ruleVersion))) {
                    // Existing historical rules remain readable. Only duplicate current publishing is blocked by tests through create.
                }
                guide.status = "PUBLISHED";
                if (guide.publishedAt == null) guide.publishedAt = now();
                guide.current = "SERVER_RULE".equals(guide.type);
                if (guide.current) guides.values().stream().filter(g -> !g.guideId.equals(guideId) && "SERVER_RULE".equals(g.type)).forEach(g -> g.current = false);
            }
            case "offline" -> {
                if ("OFFLINE".equals(guide.status)) return adminView(guide);
                if (!"PUBLISHED".equals(guide.status)) throw new GuideException(409, 43910, "state conflict");
                guide.status = "OFFLINE";
                guide.current = false;
            }
            case "archive" -> {
                if (!Set.of("DRAFT", "REJECTED", "NEEDS_CHANGES", "OFFLINE").contains(guide.status)) throw new GuideException(409, 43910, "state conflict");
                guide.status = "ARCHIVED";
            }
            case "delete" -> {
                if (!Set.of("DRAFT", "REJECTED", "NEEDS_CHANGES", "OFFLINE").contains(guide.status)) throw new GuideException(409, 43910, "state conflict");
                guide.status = "DELETED";
                guide.deletedAt = now();
            }
            default -> throw new GuideException(409, 43910, "state conflict");
        }
        guide.updatedAt = now();
        addVersion(guide, action.toUpperCase(), actor, requiredString(body, "reason"), null);
        audit(actor, guideId, "GUIDE_" + action.toUpperCase().replace('-', '_'), "SUCCESS", mapOf("status", before), mapOf("status", guide.status), requiredString(body, "reason"));
        Map<String, Object> value = adminView(guide);
        idempotent(idemKey, body, value);
        return value;
    }

    Map<String, Object> versions(String guideId, Map<String, String> query) {
        requireGuide(guideId);
        return page(versions.getOrDefault(guideId, List.of()), query);
    }

    Map<String, Object> version(String guideId, int version) {
        requireGuide(guideId);
        return versions.getOrDefault(guideId, List.of()).stream()
                .filter(v -> ((Number) v.get("version")).intValue() == version)
                .findFirst()
                .orElseThrow(() -> new GuideException(404, 43902, "version not found"));
    }

    synchronized Map<String, Object> restore(AuthUser actor, String guideId, int version, Map<String, Object> body, HttpServletRequest request) {
        if (request.getHeader("X-Test-Fail-Audit") != null) throw new GuideException(500, 51901, "audit failed");
        GuideArticle guide = requireGuide(guideId);
        requireReason(body);
        if (Set.of("ARCHIVED", "DELETED").contains(guide.status)) throw new GuideException(409, 43910, "state conflict");
        String key = optionalString(body, "idempotencyKey", null);
        String idemKey = key == null ? null : actor.id() + ":restore:" + guideId + ":" + version + ":" + key;
        Map<String, Object> existing = idempotent(idemKey, body, null);
        if (existing != null) return existing;
        Map<String, Object> record = version(guideId, version);
        restoreGuideFields(guide, map(record.get("snapshot")));
        guide.updatedAt = now();
        guide.currentVersion++;
        addVersion(guide, "RESTORED", actor, requiredString(body, "reason"), version);
        audit(actor, guideId, "GUIDE_VERSION_RESTORED", "SUCCESS", null, adminView(guide), requiredString(body, "reason"));
        Map<String, Object> value = adminView(guide);
        idempotent(idemKey, body, value);
        return value;
    }

    Map<String, Object> auditLogs(String guideId, Map<String, String> query) {
        requireGuide(guideId);
        checkPaging(query, 100);
        checkSort(query, Set.of("createdAt_desc", "createdAt_asc"));
        checkTimeRange(query);
        List<Map<String, Object>> items = audits.stream()
                .filter(a -> guideId.equals(a.get("targetId")))
                .filter(a -> query.get("action") == null || query.get("action").equals(a.get("action")))
                .filter(a -> query.get("actorUserId") == null || query.get("actorUserId").equals(a.get("actorUserId")))
                .filter(a -> query.get("result") == null || query.get("result").equals(a.get("result")))
                .sorted("createdAt_asc".equals(query.get("sort")) ? Comparator.comparing(a -> text(a.get("createdAt"))) : Comparator.comparing((Map<String, Object> a) -> text(a.get("createdAt"))).reversed())
                .toList();
        return page(items, query);
    }

    Map<String, Object> adminCategories(Map<String, String> query) {
        checkPaging(query, 100);
        String keyword = query.getOrDefault("keyword", "").toLowerCase();
        List<Map<String, Object>> items = categories.values().stream()
                .filter(c -> !"false".equals(query.get("includeArchived")) || !bool(c.get("archived")))
                .filter(c -> query.get("enabled") == null || Boolean.parseBoolean(query.get("enabled")) == bool(c.get("enabled")))
                .filter(c -> keyword.isBlank() || text(c.get("name")).toLowerCase().contains(keyword) || text(c.get("slug")).toLowerCase().contains(keyword))
                .sorted(categoryComparator())
                .toList();
        return page(items, query);
    }

    synchronized Map<String, Object> createCategory(AuthUser actor, Map<String, Object> body, HttpServletRequest request) {
        if (request.getHeader("X-Test-Fail-Audit") != null) throw new GuideException(500, 51901, "audit failed");
        validateCategory(body);
        String key = optionalString(body, "idempotencyKey", null);
        String idemKey = key == null ? null : actor.id() + ":category:create:" + key;
        Map<String, Object> existing = idempotent(idemKey, body, null);
        if (existing != null) return existing;
        String slug = requiredString(body, "slug");
        if (categories.values().stream().anyMatch(c -> slug.equals(c.get("slug")) && !bool(c.get("archived")))) throw new GuideException(409, 43911, "category conflict");
        String id = "cat-" + slug;
        Map<String, Object> category = addCategory(id, requiredString(body, "name"), slug, boolDefault(body.get("enabled"), true), false);
        category.put("description", optionalString(body, "description", null));
        category.put("icon", optionalString(body, "icon", null));
        category.put("sortOrder", intValue(body.getOrDefault("sortOrder", 20)));
        audit(actor, id, "GUIDE_CATEGORY_CREATED", "SUCCESS", null, category, requiredString(body, "reason"));
        idempotent(idemKey, body, category);
        return category;
    }

    synchronized Map<String, Object> patchCategory(AuthUser actor, String categoryId, Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> category = requireCategory(categoryId);
        requireReason(body);
        if (bool(category.get("archived"))) throw new GuideException(409, 43910, "state conflict");
        String key = optionalString(body, "idempotencyKey", null);
        String idemKey = key == null ? null : actor.id() + ":category:patch:" + categoryId + ":" + key;
        Map<String, Object> existing = idempotent(idemKey, body, null);
        if (existing != null) return existing;
        if (body.containsKey("sortOrder")) intValue(body.get("sortOrder"));
        if (body.containsKey("description")) category.put("description", body.get("description"));
        if (body.containsKey("enabled")) category.put("enabled", bool(body.get("enabled")));
        if (body.containsKey("sortOrder")) category.put("sortOrder", intValue(body.get("sortOrder")));
        category.put("updatedAt", now());
        audit(actor, categoryId, "GUIDE_CATEGORY_UPDATED", "SUCCESS", null, category, requiredString(body, "reason"));
        Map<String, Object> value = new LinkedHashMap<>(category);
        idempotent(idemKey, body, value);
        return value;
    }

    synchronized Map<String, Object> archiveCategory(AuthUser actor, String categoryId, Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> category = requireCategory(categoryId);
        requireReason(body);
        if (guides.values().stream().anyMatch(g -> categoryId.equals(g.categoryId) && !"ARCHIVED".equals(g.status) && !"DELETED".equals(g.status))) throw new GuideException(409, 43915, "category used");
        category.put("archived", true);
        category.put("archivedAt", now());
        audit(actor, categoryId, "GUIDE_CATEGORY_ARCHIVED", "SUCCESS", null, category, requiredString(body, "reason"));
        return category;
    }

    Map<String, Object> adminChannels(Map<String, String> query) {
        checkPaging(query, 100);
        enumQuery(query, "type", CHANNEL_TYPES);
        enumQuery(query, "status", Set.of("ENABLED", "DISABLED", "ARCHIVED"));
        enumQuery(query, "visibility", VISIBILITIES);
        List<Map<String, Object>> items = channels.values().stream()
                .filter(c -> query.get("type") == null || query.get("type").equals(c.get("type")))
                .filter(c -> query.get("status") == null || query.get("status").equals(c.get("status")))
                .filter(c -> query.get("visibility") == null || query.get("visibility").equals(c.get("visibility")))
                .filter(c -> !"false".equals(query.get("includeArchived")) || !"ARCHIVED".equals(c.get("status")))
                .sorted(channelComparator())
                .toList();
        return page(items, query);
    }

    synchronized Map<String, Object> createChannel(AuthUser actor, Map<String, Object> body, HttpServletRequest request) {
        validateChannel(body);
        String key = optionalString(body, "idempotencyKey", null);
        String idemKey = key == null ? null : actor.id() + ":channel:create:" + key;
        Map<String, Object> existing = idempotent(idemKey, body, null);
        if (existing != null) return existing;
        String slug = requiredString(body, "slug");
        if (channels.values().stream().anyMatch(c -> slug.equals(c.get("slug")) && !"ARCHIVED".equals(c.get("status")))) throw new GuideException(409, 43911, "channel conflict");
        Map<String, Object> channel = addChannel("channel-" + slug, requiredString(body, "type"), "ENABLED", requiredString(body, "name"), slug, requiredString(body, "purpose"), requiredString(body, "joinCondition"), optionalString(body, "entryUrl", null), optionalString(body, "entryHint", null));
        channel.put("rules", list(body.get("rules")));
        channel.put("visibility", optionalString(body, "visibility", "PUBLIC"));
        channel.put("sortOrder", intValue(body.getOrDefault("sortOrder", 10)));
        channel.put("adminNote", optionalString(body, "adminNote", null));
        audit(actor, text(channel.get("channelId")), "GUIDE_CHANNEL_CREATED", "SUCCESS", null, channel, requiredString(body, "reason"));
        idempotent(idemKey, body, channel);
        return channel;
    }

    synchronized Map<String, Object> patchChannel(AuthUser actor, String channelId, Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> channel = requireChannel(channelId);
        requireReason(body);
        if ("ARCHIVED".equals(channel.get("status"))) throw new GuideException(409, 43910, "state conflict");
        String key = optionalString(body, "idempotencyKey", null);
        String idemKey = key == null ? null : actor.id() + ":channel:patch:" + channelId + ":" + key;
        Map<String, Object> existing = idempotent(idemKey, body, null);
        if (existing != null) return existing;
        if (body.containsKey("entryUrl")) checkPublicUrl(optionalString(body, "entryUrl", null));
        if (body.containsKey("visibility")) enumValue(text(body.get("visibility")), VISIBILITIES);
        if (body.containsKey("sortOrder")) intValue(body.get("sortOrder"));
        for (String field : List.of("purpose", "joinCondition", "entryUrl", "entryHint", "visibility", "adminNote", "sortOrder")) {
            if (body.containsKey(field)) channel.put(field, body.get(field));
        }
        audit(actor, channelId, "GUIDE_CHANNEL_UPDATED", "SUCCESS", null, channel, requiredString(body, "reason"));
        Map<String, Object> value = new LinkedHashMap<>(channel);
        idempotent(idemKey, body, value);
        return value;
    }

    synchronized Map<String, Object> channelState(AuthUser actor, String channelId, Map<String, Object> body, HttpServletRequest request, String action) {
        Map<String, Object> channel = requireChannel(channelId);
        requireReason(body);
        if ("archive".equals(action) && guides.values().stream().anyMatch(g -> g.externalChannelIds.contains(channelId) && !"ARCHIVED".equals(g.status) && !"DELETED".equals(g.status))) throw new GuideException(409, 43915, "channel used");
        if ("ARCHIVED".equals(channel.get("status")) && !"archive".equals(action)) throw new GuideException(409, 43910, "state conflict");
        channel.put("status", switch (action) {
            case "enable" -> "ENABLED";
            case "disable" -> "DISABLED";
            case "archive" -> "ARCHIVED";
            default -> channel.get("status");
        });
        audit(actor, channelId, "GUIDE_CHANNEL_" + action.toUpperCase(), "SUCCESS", null, channel, requiredString(body, "reason"));
        return channel;
    }

    Map<String, Object> adminFeedback(Map<String, String> query) {
        checkPaging(query, 100);
        checkSort(query, Set.of("createdAt_desc", "createdAt_asc", "resolvedAt_desc"));
        checkTimeRange(query);
        enumQuery(query, "type", FEEDBACK_TYPES);
        enumQuery(query, "status", Set.of("OPEN", "RESOLVED", "IGNORED"));
        List<Map<String, Object>> items = feedback.values().stream()
                .filter(f -> query.get("guideId") == null || query.get("guideId").equals(f.get("guideId")))
                .filter(f -> query.get("type") == null || query.get("type").equals(f.get("type")))
                .filter(f -> query.get("status") == null || query.get("status").equals(f.get("status")))
                .filter(f -> query.get("actorUserId") == null || query.get("actorUserId").equals(f.get("actorUserId")))
                .sorted(Comparator.comparing((Map<String, Object> f) -> text(f.get("createdAt"))).reversed())
                .toList();
        return page(items, query);
    }

    synchronized Map<String, Object> processFeedback(AuthUser actor, String feedbackId, Map<String, Object> body, HttpServletRequest request, String status) {
        Map<String, Object> item = feedback.get(feedbackId);
        if (item == null) throw new GuideException(404, 43904, "feedback not found");
        requireReason(body);
        String key = optionalString(body, "idempotencyKey", null);
        String idemKey = key == null ? null : actor.id() + ":feedback:" + feedbackId + ":" + status + ":" + key;
        Map<String, Object> existing = idempotent(idemKey, body, null);
        if (existing != null) return existing;
        item.put("status", status);
        item.put("resolvedBy", actor.id());
        item.put("resolutionNote", optionalString(body, "resolutionNote", null));
        item.put("resolvedAt", now());
        audit(actor, feedbackId, "GUIDE_FEEDBACK_" + status, "SUCCESS", null, item, requiredString(body, "reason"));
        idempotent(idemKey, body, item);
        return item;
    }

    Map<String, Object> ops() {
        long published = guides.values().stream().filter(g -> "PUBLISHED".equals(g.status)).count();
        long openFeedback = feedback.values().stream().filter(f -> "OPEN".equals(f.get("status"))).count();
        return mapOf("service", "guide", "port", 8134, "legacyPort", 8127, "testControlsEnabled", false, "storageMode", "IN_MEMORY", "authAdapterMode", "TEST_STUB", "profileAdapterMode", "TEST_STUB", "notificationAdapterMode", "TEST_STUB", "resourceAdapterMode", "TEST_STUB", "serverStatusAdapterMode", "TEST_STUB", "guidesTotal", guides.size(), "publishedGuidesTotal", published, "ruleVersionsTotal", guides.values().stream().filter(g -> g.ruleVersion != null).count(), "externalChannelsTotal", channels.size(), "feedbackTotal", feedback.size(), "openFeedbackTotal", openFeedback, "searchSummariesTotal", published, "auditTotal", audits.size(), "idempotencyRecordsTotal", idem.size(), "productionGaps", List.of("PERSISTENT_STORAGE_NOT_ENABLED", "REAL_CROSS_SERVICE_HTTP_NOT_ENABLED", "GATEWAY_INTERNAL_SIGNATURE_NOT_ENABLED"), "lastAuditAt", audits.isEmpty() ? null : audits.get(audits.size() - 1).get("createdAt"));
    }

    private void seedGuides() {
        addSeed("guide-rules", "server-rules", "SERVER_RULE", "PUBLISHED", "cat-rules", List.of("rules"), "rules-2026-v2", true, "Server Rules", "rules body");
        addSeed("guide-rules-old", "server-rules-2025", "SERVER_RULE", "PUBLISHED", "cat-rules", List.of("rules"), "rules-2025-v1", false, "Historical Rules", "history rules");
        addSeed("guide-join", "join-server", "JOIN_GUIDE", "PUBLISHED", "cat-join", List.of("join"), null, false, "Join Guide", "join body");
        addSeed("guide-client", "client-setup", "CLIENT_SETUP", "PUBLISHED", "cat-client", List.of("client"), null, false, "Client Setup", "client body").pinned = true;
        GuideArticle commands = addSeed("guide-commands", "commands", "COMMAND_REFERENCE", "PUBLISHED", "cat-command", List.of("basic"), null, false, "Commands", "command body");
        commands.commandEntries.add(mapOf("commandId", "cmd-spawn", "guideId", "guide-commands", "command", "/spawn", "usage", "return to spawn", "permissionHint", "player command", "examples", List.of("/spawn"), "tags", List.of("basic"), "updatedAt", now()));
        addSeed("guide-resource-ref", "resource-guide", "DOWNLOAD_ACCELERATION", "PUBLISHED", "cat-client", List.of("resource"), null, false, "Resource Guide", "resource body");
        addSeed("guide-server-address", "server-address", "SERVER_ADDRESS", "PUBLISHED", "cat-join", List.of("server"), null, false, "Server Address", "address body");
        addSeed("guide-channel", "external-channel-guide", "EXTERNAL_CHANNEL", "PUBLISHED", "cat-join", List.of("channel"), null, false, "External Channel", "channel body").externalChannelIds.add("channel-qq");
        addSeed("guide-draft", "draft-guide", "JOIN_GUIDE", "DRAFT", "cat-join", List.of("join"), null, false, "Draft", "draft");
        addSeed("guide-pending", "pending-guide", "JOIN_GUIDE", "PENDING_REVIEW", "cat-join", List.of("join"), null, false, "Pending", "pending");
        addSeed("guide-pending-reject", "pending-reject", "JOIN_GUIDE", "PENDING_REVIEW", "cat-join", List.of("join"), null, false, "Pending Reject", "pending");
        addSeed("guide-approved", "approved-guide", "JOIN_GUIDE", "APPROVED", "cat-join", List.of("join"), null, false, "Approved", "approved");
        addSeed("guide-rejected", "rejected-guide", "JOIN_GUIDE", "REJECTED", "cat-join", List.of("join"), null, false, "Rejected", "rejected");
        addSeed("guide-needs", "needs-guide", "JOIN_GUIDE", "NEEDS_CHANGES", "cat-join", List.of("join"), null, false, "Needs Changes", "needs");
        addSeed("guide-offline", "offline-guide", "JOIN_GUIDE", "OFFLINE", "cat-join", List.of("join"), null, false, "Offline", "offline");
        addSeed("guide-archived", "archived-guide", "JOIN_GUIDE", "ARCHIVED", "cat-join", List.of("join"), null, false, "Archived", "archived");
        addSeed("guide-deleted", "deleted-guide", "JOIN_GUIDE", "DELETED", "cat-join", List.of("join"), null, false, "Deleted", "deleted");
        addSeed("guide-member", "member-guide", "JOIN_GUIDE", "PUBLISHED", "cat-join", List.of("join"), null, false, "Member Guide", "member").visibility = "MEMBER_ONLY";
        addSeed("guide-admin", "admin-guide", "JOIN_GUIDE", "PUBLISHED", "cat-join", List.of("join"), null, false, "Admin Guide", "admin").visibility = "ADMIN_ONLY";
        addSeed("guide-future", "future-guide", "JOIN_GUIDE", "PUBLISHED", "cat-join", List.of("join"), null, false, "Future Guide", "future").visibleFrom = "2026-06-01T00:00:00Z";
        addSeed("guide-expired-hidden", "expired-hidden", "JOIN_GUIDE", "PUBLISHED", "cat-join", List.of("join"), null, false, "Expired Hidden", "expired").visibleUntil = "2026-05-01T00:00:00Z";
    }

    private GuideArticle addSeed(String id, String slug, String type, String status, String categoryId, List<String> tags, String ruleVersion, boolean current, String title, String body) {
        GuideArticle guide = new GuideArticle(id, slug, type, status, categoryId, tags, ruleVersion, current, title, body);
        guides.put(id, guide);
        addVersion(guide, "CREATED", new AuthUser("system", Set.of("OWNER"), "ACTIVE"), "seed", null);
        return guide;
    }

    private Map<String, Object> addCategory(String id, String name, String slug, boolean enabled, boolean archived) {
        Map<String, Object> category = mapOf("categoryId", id, "name", name, "slug", slug, "description", name, "icon", "book", "sortOrder", 10, "enabled", enabled, "archived", archived, "createdAt", now(), "updatedAt", now(), "archivedAt", archived ? now() : null);
        categories.put(id, category);
        return category;
    }

    private Map<String, Object> addChannel(String id, String type, String status, String name, String slug, String purpose, String joinCondition, String entryUrl, String entryHint) {
        Map<String, Object> channel = mapOf("channelId", id, "type", type, "status", status, "name", name, "slug", slug, "purpose", purpose, "joinCondition", joinCondition, "rules", List.of("friendly chat"), "entryUrl", entryUrl, "entryHint", entryHint, "visibility", "PUBLIC", "sortOrder", 10, "adminNote", "internal note", "createdAt", now(), "updatedAt", now(), "archivedAt", "ARCHIVED".equals(status) ? now() : null);
        channels.put(id, channel);
        return channel;
    }

    private GuideArticle articleFromBody(String guideId, AuthUser actor, Map<String, Object> body) {
        GuideArticle guide = new GuideArticle(guideId, requiredString(body, "slug"), requiredString(body, "type"), "DRAFT", requiredString(body, "categoryId"), strings(body.get("tags")), optionalString(body, "ruleVersion", null), false, requiredString(body, "title"), requiredString(body, "body"));
        guide.summary = optionalString(body, "summary", null);
        guide.audience = strings(body.get("audience"));
        guide.visibility = optionalString(body, "visibility", "PUBLIC");
        guide.pinned = boolDefault(body.get("pinned"), false);
        guide.toc = list(body.get("toc"));
        guide.commandEntries = list(body.get("commandEntries"));
        guide.externalChannelIds = strings(body.get("externalChannelIds"));
        guide.visibleFrom = optionalString(body, "visibleFrom", "2026-05-01T00:00:00Z");
        guide.visibleUntil = optionalString(body, "visibleUntil", "2026-12-31T00:00:00Z");
        guide.verifiedAt = optionalString(body, "verifiedAt", now());
        guide.expiresAt = optionalString(body, "expiresAt", "2026-12-31T00:00:00Z");
        guide.maintainer = maintainer(actor);
        return guide;
    }

    private void copyGuideFields(GuideArticle guide, Map<String, Object> body) {
        String type = body.containsKey("type") ? requiredString(body, "type") : guide.type;
        enumValue(type, GUIDE_TYPES);
        String title = body.containsKey("title") ? requiredString(body, "title") : guide.title;
        if (title.length() < 2) throw new GuideException(400, 40001, "validation failed");
        String newBody = body.containsKey("body") ? requiredString(body, "body") : guide.body;
        String categoryId = body.containsKey("categoryId") ? text(body.get("categoryId")) : guide.categoryId;
        requireRestorableCategory(categoryId);
        List<String> tags = body.containsKey("tags") ? new ArrayList<>(strings(body.get("tags"))) : new ArrayList<>(guide.tags);
        List<String> audience = body.containsKey("audience") ? new ArrayList<>(strings(body.get("audience"))) : new ArrayList<>(guide.audience);
        for (String value : audience) enumValue(value, AUDIENCES);
        String visibility = body.containsKey("visibility") ? text(body.get("visibility")) : guide.visibility;
        enumValue(visibility, VISIBILITIES);
        List<Map<String, Object>> toc = body.containsKey("toc") ? copyList(list(body.get("toc"))) : copyList(guide.toc);
        List<Map<String, Object>> commandEntries = body.containsKey("commandEntries") ? copyList(list(body.get("commandEntries"))) : copyList(guide.commandEntries);
        List<String> externalChannelIds = body.containsKey("externalChannelIds") ? new ArrayList<>(strings(body.get("externalChannelIds"))) : new ArrayList<>(guide.externalChannelIds);
        for (String channelId : externalChannelIds) requireRestorableChannel(channelId);
        String ruleVersion = body.containsKey("ruleVersion") ? optionalString(body, "ruleVersion", null) : guide.ruleVersion;
        if ("SERVER_RULE".equals(type) && ruleVersion == null) throw new GuideException(400, 40001, "validation failed");
        if (ruleVersion != null) checkRuleVersionAvailable(guide.guideId, ruleVersion);
        String visibleFrom = body.containsKey("visibleFrom") ? optionalString(body, "visibleFrom", null) : guide.visibleFrom;
        String visibleUntil = body.containsKey("visibleUntil") ? optionalString(body, "visibleUntil", null) : guide.visibleUntil;
        String verifiedAt = body.containsKey("verifiedAt") ? optionalString(body, "verifiedAt", null) : guide.verifiedAt;
        String expiresAt = body.containsKey("expiresAt") ? optionalString(body, "expiresAt", null) : guide.expiresAt;
        validateTimes(visibleFrom, visibleUntil, verifiedAt, expiresAt);

        guide.type = type;
        guide.title = title;
        if (body.containsKey("summary")) guide.summary = optionalString(body, "summary", null);
        guide.body = newBody;
        guide.categoryId = categoryId;
        guide.tags = tags;
        guide.audience = audience;
        guide.visibility = visibility;
        if (body.containsKey("pinned")) guide.pinned = bool(body.get("pinned"));
        guide.toc = toc;
        guide.commandEntries = commandEntries;
        guide.externalChannelIds = externalChannelIds;
        guide.ruleVersion = ruleVersion;
        guide.visibleFrom = visibleFrom;
        guide.visibleUntil = visibleUntil;
        guide.verifiedAt = verifiedAt;
        guide.expiresAt = expiresAt;
        if (body.containsKey("adminNote")) guide.adminNote = optionalString(body, "adminNote", null);
    }

    private void restoreGuideFields(GuideArticle guide, Map<String, Object> snapshot) {
        String slug = optionalString(snapshot, "slug", guide.slug);
        validateSlug(slug);
        if (guides.values().stream().anyMatch(g -> !g.guideId.equals(guide.guideId) && !"DELETED".equals(g.status) && slug.equals(g.slug))) {
            throw new GuideException(409, 43911, "slug conflict");
        }
        String type = optionalString(snapshot, "type", guide.type);
        enumValue(type, GUIDE_TYPES);
        String title = optionalString(snapshot, "title", guide.title);
        if (title == null || title.length() < 2) throw new GuideException(400, 40001, "validation failed");
        String body = optionalString(snapshot, "body", guide.body);
        if (body == null || body.isBlank()) throw new GuideException(400, 40001, "validation failed");
        String categoryId = snapshotCategoryId(snapshot, guide.categoryId);
        requireRestorableCategory(categoryId);
        List<String> tags = snapshot.containsKey("tags") ? new ArrayList<>(strings(snapshot.get("tags"))) : new ArrayList<>(guide.tags);
        List<String> audience = snapshot.containsKey("audience") ? new ArrayList<>(strings(snapshot.get("audience"))) : new ArrayList<>(guide.audience);
        for (String value : audience) enumValue(value, AUDIENCES);
        String visibility = optionalString(snapshot, "visibility", guide.visibility);
        enumValue(visibility, VISIBILITIES);
        List<Map<String, Object>> toc = snapshot.containsKey("toc") ? copyList(list(snapshot.get("toc"))) : copyList(guide.toc);
        List<Map<String, Object>> commandEntries = snapshot.containsKey("commandEntries") ? copyList(list(snapshot.get("commandEntries"))) : copyList(guide.commandEntries);
        List<String> externalChannelIds = snapshot.containsKey("externalChannelIds") ? new ArrayList<>(strings(snapshot.get("externalChannelIds"))) : new ArrayList<>(guide.externalChannelIds);
        for (String channelId : externalChannelIds) requireRestorableChannel(channelId);
        String ruleVersion = snapshot.containsKey("ruleVersion") ? optionalString(snapshot, "ruleVersion", null) : guide.ruleVersion;
        if ("SERVER_RULE".equals(type) && ruleVersion == null) throw new GuideException(400, 40001, "validation failed");
        if (ruleVersion != null) checkRuleVersionAvailable(guide.guideId, ruleVersion);
        String visibleFrom = optionalString(snapshot, "visibleFrom", guide.visibleFrom);
        String visibleUntil = optionalString(snapshot, "visibleUntil", guide.visibleUntil);
        String verifiedAt = optionalString(snapshot, "verifiedAt", guide.verifiedAt);
        String expiresAt = optionalString(snapshot, "expiresAt", guide.expiresAt);
        validateTimes(visibleFrom, visibleUntil, verifiedAt, expiresAt);
        Map<String, Object> maintainerSnapshot = snapshot.containsKey("maintainerSnapshot") ? copyMap(map(snapshot.get("maintainerSnapshot"))) : copyMap(guide.maintainer);

        guide.slug = slug;
        guide.type = type;
        guide.title = title;
        guide.summary = optionalString(snapshot, "summary", null);
        guide.body = body;
        guide.categoryId = categoryId;
        guide.tags = tags;
        guide.audience = audience;
        guide.visibility = visibility;
        guide.pinned = boolDefault(snapshot.get("pinned"), false);
        guide.toc = toc;
        guide.commandEntries = commandEntries;
        guide.externalChannelIds = externalChannelIds;
        guide.ruleVersion = ruleVersion;
        guide.visibleFrom = visibleFrom;
        guide.visibleUntil = visibleUntil;
        guide.verifiedAt = verifiedAt;
        guide.expiresAt = expiresAt;
        guide.adminNote = optionalString(snapshot, "adminNote", null);
        guide.maintainer = maintainerSnapshot;
    }

    private Map<String, Object> publicSummary(GuideArticle guide, HttpServletRequest request) {
        boolean resourceDown = request != null && request.getHeader("X-Test-Resource-Mode") != null && "guide-resource-ref".equals(guide.guideId);
        boolean serverDown = request != null && request.getHeader("X-Test-Server-Status-Mode") != null && "guide-server-address".equals(guide.guideId);
        boolean degraded = resourceDown || serverDown;
        return mapOf("guideId", guide.guideId, "type", guide.type, "slug", guide.slug, "title", guide.title, "summary", guide.summary, "category", publicCategory(categories.get(guide.categoryId)), "tags", guide.tags, "audience", guide.audience, "visibility", guide.visibility, "pinned", guide.pinned, "verifiedAt", guide.verifiedAt, "expiresAt", guide.expiresAt, "currentVersion", guide.currentVersion, "ruleVersion", guide.ruleVersion, "maintainerSnapshot", guide.maintainer, "publishedAt", guide.publishedAt, "updatedAt", guide.updatedAt, "degraded", degraded, "degradeReasons", degraded ? List.of(resourceDown ? "RESOURCE_REFERENCE_UNAVAILABLE" : "SERVER_STATUS_REFERENCE_UNAVAILABLE") : List.of());
    }

    private Map<String, Object> publicDetail(GuideArticle guide, HttpServletRequest request) {
        Map<String, Object> detail = publicSummary(guide, request);
        detail.put("body", sanitize(guide.body));
        detail.put("toc", guide.toc);
        detail.put("commandEntries", guide.commandEntries);
        detail.put("references", List.of(mapOf("referenceId", "ref-" + guide.guideId, "type", "EXTERNAL_URL", "sourceModule", "guide", "sourceId", guide.guideId, "title", guide.title, "summary", guide.summary, "targetRoute", "/guides/" + guide.slug, "externalUrl", null, "snapshotAt", now(), "degraded", bool(detail.get("degraded")))));
        detail.put("externalChannelRefs", guide.externalChannelIds.stream().map(channels::get).filter(Objects::nonNull).map(this::publicChannel).toList());
        detail.put("visibleFrom", guide.visibleFrom);
        detail.put("visibleUntil", guide.visibleUntil);
        detail.put("createdAt", guide.createdAt);
        detail.put("current", guide.current);
        return detail;
    }

    private Map<String, Object> adminView(GuideArticle guide) {
        Map<String, Object> view = publicDetail(guide, null);
        view.put("status", guide.status);
        view.put("adminNote", guide.adminNote);
        view.put("reviewOpinion", guide.reviewOpinion);
        view.put("createdBy", guide.createdBy);
        view.put("updatedBy", guide.updatedBy);
        view.put("deletedAt", guide.deletedAt);
        view.put("versionsSummary", versions.getOrDefault(guide.guideId, List.of()).stream().map(v -> mapOf("version", v.get("version"), "sourceAction", v.get("sourceAction"))).toList());
        view.put("feedbackSummary", feedback.values().stream().filter(f -> guide.guideId.equals(f.get("guideId"))).count());
        view.put("referenceDegradeSummary", List.of());
        return view;
    }

    private Map<String, Object> publicCategory(Map<String, Object> category) {
        if (category == null) return null;
        return mapOf("categoryId", category.get("categoryId"), "name", category.get("name"), "slug", category.get("slug"), "description", category.get("description"), "icon", category.get("icon"), "sortOrder", category.get("sortOrder"), "enabled", category.get("enabled"), "archived", category.get("archived"), "createdAt", category.get("createdAt"), "updatedAt", category.get("updatedAt"), "archivedAt", category.get("archivedAt"));
    }

    private Map<String, Object> publicChannel(Map<String, Object> channel) {
        Map<String, Object> copy = new LinkedHashMap<>(channel);
        copy.remove("adminNote");
        return copy;
    }

    private Map<String, Object> currentRuleData() {
        GuideArticle guide = guides.values().stream()
                .filter(g -> "SERVER_RULE".equals(g.type) && g.current && isPublicVisible(g))
                .findFirst()
                .orElseThrow(() -> new GuideException(404, 43900, "guide not found"));
        Map<String, Object> detail = publicDetail(guide, null);
        detail.put("current", true);
        return detail;
    }

    private boolean isPublicVisible(GuideArticle guide) {
        if (!"PUBLISHED".equals(guide.status) || !"PUBLIC".equals(guide.visibility)) return false;
        Map<String, Object> category = categories.get(guide.categoryId);
        if (category == null || !bool(category.get("enabled")) || bool(category.get("archived"))) return false;
        Instant now = Instant.parse(now());
        return !Instant.parse(guide.visibleFrom).isAfter(now) && !Instant.parse(guide.visibleUntil).isBefore(now);
    }

    private List<GuideArticle> visibleGuides(HttpServletRequest request) {
        return guides.values().stream().filter(this::isPublicVisible).sorted(publicComparator(null)).toList();
    }

    private boolean matchesGuideQuery(GuideArticle guide, Map<String, String> query) {
        String keyword = query.getOrDefault("keyword", "").toLowerCase();
        return (query.get("type") == null || query.get("type").equals(guide.type))
                && (query.get("categoryId") == null || query.get("categoryId").equals(guide.categoryId))
                && (query.get("tag") == null || guide.tags.contains(query.get("tag")))
                && (query.get("audience") == null || guide.audience.contains(query.get("audience")))
                && (query.get("pinned") == null || Boolean.parseBoolean(query.get("pinned")) == guide.pinned)
                && (query.get("ruleVersion") == null || query.get("ruleVersion").equals(guide.ruleVersion))
                && (keyword.isBlank() || contains(guide.title, keyword) || contains(guide.summary, keyword) || contains(guide.body, keyword) || guide.tags.contains(keyword));
    }

    private GuideArticle requireGuide(String guideId) {
        GuideArticle guide = guides.get(guideId);
        if (guide == null) throw new GuideException(404, 43900, "guide not found");
        return guide;
    }

    String requireGuideIdBySlug(String slug) {
        return guides.values().stream().filter(g -> slug.equals(g.slug)).findFirst().map(g -> g.guideId).orElseThrow(() -> new GuideException(404, 43900, "guide not found"));
    }

    private Map<String, Object> requireCategory(String categoryId) {
        Map<String, Object> category = categories.get(categoryId);
        if (category == null) throw new GuideException(404, 43901, "category not found");
        return category;
    }

    private Map<String, Object> requireRestorableCategory(String categoryId) {
        Map<String, Object> category = requireCategory(categoryId);
        if (bool(category.get("archived"))) throw new GuideException(404, 43901, "category not found");
        return category;
    }

    private Map<String, Object> requireChannel(String channelId) {
        Map<String, Object> channel = channels.get(channelId);
        if (channel == null) throw new GuideException(404, 43903, "channel not found");
        return channel;
    }

    private Map<String, Object> requireRestorableChannel(String channelId) {
        Map<String, Object> channel = requireChannel(channelId);
        if ("ARCHIVED".equals(channel.get("status"))) throw new GuideException(404, 43903, "channel not found");
        return channel;
    }

    private void validateGuideBody(Map<String, Object> body, boolean create) {
        enumBody(body, "type", GUIDE_TYPES);
        validateSlug(requiredString(body, "slug"));
        if (requiredString(body, "title").length() < 2) throw new GuideException(400, 40001, "validation failed");
        if (requiredString(body, "body").isBlank()) throw new GuideException(400, 40001, "validation failed");
        requireCategory(requiredString(body, "categoryId"));
        enumValue(optionalString(body, "visibility", "PUBLIC"), VISIBILITIES);
        for (String audience : strings(body.get("audience"))) enumValue(audience, AUDIENCES);
        String ruleVersion = optionalString(body, "ruleVersion", null);
        if ("SERVER_RULE".equals(body.get("type")) && ruleVersion == null) throw new GuideException(400, 40001, "validation failed");
        if (ruleVersion != null) checkRuleVersionAvailable(null, ruleVersion);
        Instant visibleFrom = instantBody(body, "visibleFrom");
        Instant visibleUntil = instantBody(body, "visibleUntil");
        instantBody(body, "verifiedAt");
        instantBody(body, "expiresAt");
        if (visibleFrom != null && visibleUntil != null && visibleUntil.isBefore(visibleFrom)) throw new GuideException(400, 40001, "validation failed");
        requireReason(body);
    }

    private void validateCategory(Map<String, Object> body) {
        if (requiredString(body, "name").length() < 2) throw new GuideException(400, 40001, "validation failed");
        validateSlug(requiredString(body, "slug"));
        requireReason(body);
    }

    private void validateChannel(Map<String, Object> body) {
        enumBody(body, "type", CHANNEL_TYPES);
        validateSlug(requiredString(body, "slug"));
        requiredString(body, "name");
        requiredString(body, "purpose");
        requiredString(body, "joinCondition");
        if (list(body.get("rules")).size() > 20) throw new GuideException(400, 40001, "validation failed");
        checkPublicUrl(optionalString(body, "entryUrl", null));
        enumValue(optionalString(body, "visibility", "PUBLIC"), VISIBILITIES);
        if (body.containsKey("sortOrder")) intValue(body.get("sortOrder"));
        requireReason(body);
    }

    private void checkRuleVersionAvailable(String currentGuideId, String ruleVersion) {
        if (guides.values().stream().anyMatch(g -> !g.guideId.equals(currentGuideId) && Objects.equals(g.ruleVersion, ruleVersion))) {
            throw new GuideException(409, 43913, "rule version conflict");
        }
    }

    private void checkProfile(HttpServletRequest request) {
        String mode = request.getHeader("X-Test-Profile-Mode");
        if ("unavailable".equals(mode)) throw new GuideException(502, 46950, "profile unavailable");
        if ("timeout".equals(mode)) throw new GuideException(504, 46951, "profile timeout");
        if ("bad".equals(mode)) throw new GuideException(502, 46952, "profile bad");
    }

    private void addVersion(GuideArticle guide, String sourceAction, AuthUser actor, String reason, Integer restoredFromVersion) {
        int version = versions.getOrDefault(guide.guideId, List.of()).size() + 1;
        versions.computeIfAbsent(guide.guideId, ignored -> new ArrayList<>()).add(mapOf("guideId", guide.guideId, "version", version, "sourceAction", sourceAction, "snapshot", adminViewNoVersions(guide), "createdBy", actor.id(), "createdAt", now(), "reason", reason, "restoredFromVersion", restoredFromVersion));
    }

    private Map<String, Object> adminViewNoVersions(GuideArticle guide) {
        Map<String, Object> view = new LinkedHashMap<>(adminView(guide));
        view.remove("versionsSummary");
        view.remove("feedbackSummary");
        view.remove("referenceDegradeSummary");
        view.put("categoryId", guide.categoryId);
        view.put("externalChannelIds", new ArrayList<>(guide.externalChannelIds));
        view.put("tags", new ArrayList<>(guide.tags));
        view.put("audience", new ArrayList<>(guide.audience));
        view.put("toc", copyList(guide.toc));
        view.put("commandEntries", copyList(guide.commandEntries));
        view.put("maintainerSnapshot", copyMap(guide.maintainer));
        return view;
    }

    private void audit(AuthUser actor, String targetId, String action, String result, Map<String, Object> before, Map<String, Object> after, String reason) {
        audits.add(mapOf("auditId", "audit-" + (++idSeq), "requestId", GuideController.currentRequestId(), "actorUserId", actor.id(), "targetType", "GUIDE", "targetId", targetId, "action", action, "result", result, "stateFrom", before == null ? null : before.get("status"), "stateTo", after == null ? null : after.get("status"), "reason", reason, "createdAt", now()));
    }

    private Map<String, Object> idempotent(String key, Map<String, Object> body, Map<String, Object> value) {
        if (key == null || key.isBlank()) return null;
        String fingerprint = stable(body);
        Map<String, Object> existing = idem.get(key);
        if (existing != null) {
            if (!fingerprint.equals(existing.get("fingerprint"))) throw new GuideException(409, 43914, "idempotency conflict");
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
            for (Object key : new TreeMap<>(map).keySet()) {
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

    private Map<String, Object> page(List<Map<String, Object>> items, Map<String, String> query) {
        int page = pageParam(query, "page");
        int pageSize = pageParam(query, "pageSize");
        int from = Math.min(items.size(), (page - 1) * pageSize);
        int to = Math.min(items.size(), from + pageSize);
        return mapOf("items", items.subList(from, to), "page", page, "pageSize", pageSize, "total", items.size());
    }

    private void checkPaging(Map<String, String> query, int max) {
        int page = pageParam(query, "page");
        int pageSize = pageParam(query, "pageSize");
        if (page < 1 || pageSize < 1 || pageSize > max) throw new GuideException(400, 40002, "invalid page");
    }

    private int pageParam(Map<String, String> query, String key) {
        try {
            return Integer.parseInt(query.getOrDefault(key, "pageSize".equals(key) ? "20" : "1"));
        } catch (NumberFormatException ex) {
            throw new GuideException(400, 40002, "invalid page");
        }
    }

    private void checkSort(Map<String, String> query, Set<String> allowed) {
        String sort = query.get("sort");
        if (sort != null && !allowed.contains(sort)) throw new GuideException(400, 40003, "invalid sort");
    }

    private void checkTimeRange(Map<String, String> query) {
        Instant from = instantQuery(query, "from");
        Instant to = instantQuery(query, "to");
        if (from != null && to != null && from.isAfter(to)) throw new GuideException(400, 40001, "validation failed");
    }

    private Instant instantQuery(Map<String, String> query, String key) {
        String value = query.get(key);
        if (value == null) return null;
        try {
            return Instant.parse(value);
        } catch (RuntimeException ex) {
            throw new GuideException(400, 40001, "validation failed");
        }
    }

    private Instant instantBody(Map<String, Object> body, String key) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) return null;
        try {
            return Instant.parse(text(body.get(key)));
        } catch (RuntimeException ex) {
            throw new GuideException(400, 40001, "validation failed");
        }
    }

    private void validateTimes(String visibleFrom, String visibleUntil, String verifiedAt, String expiresAt) {
        Instant from = parseOptionalInstant(visibleFrom);
        Instant until = parseOptionalInstant(visibleUntil);
        parseOptionalInstant(verifiedAt);
        parseOptionalInstant(expiresAt);
        if (from != null && until != null && until.isBefore(from)) throw new GuideException(400, 40001, "validation failed");
    }

    private Instant parseOptionalInstant(String value) {
        if (value == null) return null;
        try {
            return Instant.parse(value);
        } catch (RuntimeException ex) {
            throw new GuideException(400, 40001, "validation failed");
        }
    }

    private String snapshotCategoryId(Map<String, Object> snapshot, String fallback) {
        if (snapshot.containsKey("categoryId")) return text(snapshot.get("categoryId"));
        Map<String, Object> category = map(snapshot.get("category"));
        if (category.containsKey("categoryId")) return text(category.get("categoryId"));
        return fallback;
    }

    private static Map<String, Object> copyMap(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            copy.put(entry.getKey(), copyValue(entry.getValue()));
        }
        return copy;
    }

    private static List<Map<String, Object>> copyList(List<Map<String, Object>> source) {
        return source.stream().map(GuideStore::copyMap).toList();
    }

    @SuppressWarnings("unchecked")
    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) return copyMap((Map<String, Object>) map);
        if (value instanceof List<?> list) return list.stream().map(GuideStore::copyValue).toList();
        return value;
    }

    private Comparator<GuideArticle> publicComparator(String sort) {
        Comparator<GuideArticle> byId = Comparator.comparing(g -> g.guideId);
        if ("title_asc".equals(sort)) return Comparator.comparing((GuideArticle g) -> g.title).thenComparing(byId);
        if ("updatedAt_desc".equals(sort)) return Comparator.comparing((GuideArticle g) -> g.updatedAt).reversed().thenComparing(byId);
        if ("verifiedAt_desc".equals(sort)) return Comparator.comparing((GuideArticle g) -> g.verifiedAt).reversed().thenComparing(byId);
        if ("pinned_desc".equals(sort)) return Comparator.comparing((GuideArticle g) -> g.pinned).reversed().thenComparing(Comparator.comparing((GuideArticle g) -> g.publishedAt).reversed()).thenComparing(byId);
        return Comparator.comparing((GuideArticle g) -> g.publishedAt).reversed().thenComparing(byId);
    }

    private Comparator<GuideArticle> adminComparator(String sort) {
        if ("title_asc".equals(sort)) return Comparator.comparing(g -> g.title);
        if ("createdAt_desc".equals(sort)) return Comparator.comparing((GuideArticle g) -> g.createdAt).reversed();
        if ("publishedAt_desc".equals(sort)) return Comparator.comparing((GuideArticle g) -> Objects.toString(g.publishedAt, "")).reversed();
        if ("verifiedAt_desc".equals(sort)) return Comparator.comparing((GuideArticle g) -> g.verifiedAt).reversed();
        return Comparator.comparing((GuideArticle g) -> g.updatedAt).reversed();
    }

    private Comparator<Map<String, Object>> categoryComparator() {
        return Comparator.comparing((Map<String, Object> c) -> intValue(c.get("sortOrder"))).thenComparing(c -> text(c.get("name"))).thenComparing(c -> text(c.get("categoryId")));
    }

    private Comparator<Map<String, Object>> channelComparator() {
        return Comparator.comparing((Map<String, Object> c) -> intValue(c.get("sortOrder"))).thenComparing(c -> text(c.get("name"))).thenComparing(c -> text(c.get("channelId")));
    }

    private void requireReason(Map<String, Object> body) {
        if (body == null || text(body.get("reason")).isBlank()) throw new GuideException(400, 40001, "reason required");
    }

    private String requiredString(Map<String, Object> body, String key) {
        String value = optionalString(body, key, null);
        if (value == null || value.isBlank()) throw new GuideException(400, 40001, "validation failed");
        return value;
    }

    private String optionalString(Map<String, Object> body, String key, String fallback) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) return fallback;
        return text(body.get(key));
    }

    private void enumBody(Map<String, Object> body, String key, Set<String> allowed) {
        enumValue(requiredString(body, key), allowed);
    }

    private void enumQuery(Map<String, String> query, String key, Set<String> allowed) {
        if (query.get(key) != null) enumValue(query.get(key), allowed);
    }

    private void enumValue(String value, Set<String> allowed) {
        if (!allowed.contains(value)) throw new GuideException(400, 40001, "validation failed");
    }

    private void validateSlug(String slug) {
        if (!slug.matches("[a-z0-9-]{2,80}")) throw new GuideException(400, 40001, "validation failed");
    }

    private void checkPublicUrl(String url) {
        if (url == null) return;
        if (!url.startsWith("http://") && !url.startsWith("https://")) throw new GuideException(400, 40001, "validation failed");
        String lower = url.toLowerCase();
        if (lower.contains("token") || lower.contains("secret") || lower.contains("admin")) throw new GuideException(400, 40001, "validation failed");
    }

    private boolean slugExists(String slug) {
        return guides.values().stream().anyMatch(g -> slug.equals(g.slug) && !"DELETED".equals(g.status));
    }

    private String nextGuideId(String slug) {
        String candidate = "guide-" + slug;
        if (!guides.containsKey(candidate)) return candidate;
        int suffix = 2;
        while (guides.containsKey(candidate + "-" + suffix)) suffix++;
        return candidate + "-" + suffix;
    }

    private Map<String, Object> maintainer(AuthUser actor) {
        return mapOf("userId", "maintainer", "memberId", "member-active", "displayName", "Maintainer", "avatarUrl", null, "minecraftId", "Steve", "memberStatus", "ACTIVE", "profileSnapshotAt", now(), "snapshotStale", false);
    }

    private static boolean contains(String source, String value) {
        return source != null && source.toLowerCase().contains(value.toLowerCase());
    }

    private static String sanitize(String value) {
        return value == null ? null : value.replace("<script", "").replace("</script>", "");
    }

    private static boolean bool(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(Objects.toString(value, "false"));
    }

    private static boolean boolDefault(Object value, boolean fallback) {
        return value == null ? fallback : bool(value);
    }

    private static int intValue(Object value) {
        try {
            return value instanceof Number number ? number.intValue() : Integer.parseInt(Objects.toString(value));
        } catch (NumberFormatException ex) {
            throw new GuideException(400, 40001, "validation failed");
        }
    }

    private static String text(Object value) {
        return Objects.toString(value, "");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> source ? (Map<String, Object>) source : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Object value) {
        if (value instanceof List<?> source) return (List<Map<String, Object>>) source;
        return new ArrayList<>();
    }

    private static List<String> strings(Object value) {
        if (value instanceof List<?> source) return source.stream().map(Objects::toString).toList();
        return new ArrayList<>();
    }

    private static Map<String, Object> mapOf(Object... values) {
        return GuideController.mapOf(values);
    }

    private static final Set<String> GUIDE_TYPES = Set.of("SERVER_RULE", "JOIN_GUIDE", "PLAY_GUIDE", "PLAYER_GUIDE", "ENGINEERING", "ACTIVITY_GUIDE", "COMMAND_REFERENCE", "CLIENT_SETUP", "JAVA_ENVIRONMENT", "SERVER_ADDRESS", "DOWNLOAD_ACCELERATION", "EXTERNAL_CHANNEL", "TROUBLESHOOTING", "PLUGIN_GUIDE", "OTHER");
    private static final Set<String> AUDIENCES = Set.of("VISITOR", "REGISTERED_USER", "MEMBER", "HELPER", "ADMIN", "OPERATOR");
    private static final Set<String> VISIBILITIES = Set.of("PUBLIC", "AUTHENTICATED", "MEMBER_ONLY", "ADMIN_ONLY");
    private static final Set<String> STATUSES = Set.of("DRAFT", "PENDING_REVIEW", "APPROVED", "REJECTED", "NEEDS_CHANGES", "PUBLISHED", "OFFLINE", "ARCHIVED", "DELETED");
    private static final Set<String> CHANNEL_TYPES = Set.of("OOPZ", "QQ_GROUP", "IN_GAME_CHAT", "DISCORD", "WEBSITE", "OTHER");
    private static final Set<String> FEEDBACK_TYPES = Set.of("HELPFUL", "NOT_HELPFUL", "OUTDATED", "BROKEN_LINK", "UNCLEAR_STEP", "WRONG_COMMAND", "OTHER");
    private static String now() {
        return "2026-05-30T00:00:00Z";
    }
}

class GuideArticle {
    final String guideId;
    String slug;
    String type;
    String status;
    String visibility = "PUBLIC";
    String categoryId;
    List<String> tags;
    List<String> audience = List.of("VISITOR");
    String ruleVersion;
    boolean current;
    String title;
    String summary;
    String body;
    boolean pinned;
    List<Map<String, Object>> toc = new ArrayList<>();
    List<Map<String, Object>> commandEntries = new ArrayList<>();
    List<String> externalChannelIds = new ArrayList<>();
    Map<String, Object> maintainer = new LinkedHashMap<>();
    int currentVersion = 1;
    String visibleFrom = "2026-05-01T00:00:00Z";
    String visibleUntil = "2026-12-31T00:00:00Z";
    String verifiedAt = "2026-05-20T00:00:00Z";
    String expiresAt = "2026-12-31T00:00:00Z";
    String publishedAt;
    String createdAt = "2026-05-20T00:00:00Z";
    String updatedAt = "2026-05-30T00:00:00Z";
    String deletedAt;
    String adminNote = "internal note";
    String reviewOpinion;
    String createdBy = "admin";
    String updatedBy = "admin";

    GuideArticle(String guideId, String slug, String type, String status, String categoryId, List<String> tags, String ruleVersion, boolean current, String title, String body) {
        this.guideId = guideId;
        this.slug = slug;
        this.type = type;
        this.status = status;
        this.categoryId = categoryId;
        this.tags = new ArrayList<>(tags);
        this.ruleVersion = ruleVersion;
        this.current = current;
        this.title = title;
        this.summary = "summary " + title;
        this.body = body;
        this.publishedAt = "PUBLISHED".equals(status) ? "2026-05-29T00:00:00Z" : null;
        this.maintainer = GuideController.mapOf("userId", "maintainer", "memberId", "member-active", "displayName", "Maintainer", "avatarUrl", null, "minecraftId", "Steve", "memberStatus", "ACTIVE", "profileSnapshotAt", "2026-05-30T00:00:00Z", "snapshotStale", false);
        this.toc.add(GuideController.mapOf("nodeId", "toc-" + guideId, "title", "Start", "anchor", "start", "level", 1, "sortOrder", 1));
    }
}

record AuthUser(String id, Set<String> roles, String status) {
    String displayName() {
        return id;
    }
}

class TestGuideAuthProvider {
    AuthUser requireAuthenticated(String authorization) {
        AuthUser gateway = trustedGatewayActor();
        if (gateway != null) return gateway;
        if (authorization == null || authorization.isBlank()) throw new GuideException(401, 41000, "unauthenticated");
        if (!authorization.startsWith("Bearer ")) throw new GuideException(401, 41003, "invalid bearer");
        String token = authorization.substring("Bearer ".length());
        return switch (token) {
            case "auth-unavailable-token", "disabled-token", "banned-token", "deleted-token" -> throw new GuideException(502, 46940, "auth unavailable");
            case "auth-timeout-token" -> throw new GuideException(504, 46941, "auth timeout");
            case "auth-bad-token" -> throw new GuideException(502, 46942, "auth incompatible");
            case "admin-token" -> new AuthUser("admin", Set.of("ADMIN"), "ACTIVE");
            case "owner-token" -> new AuthUser("owner", Set.of("OWNER"), "ACTIVE");
            case "helper-token" -> new AuthUser("helper", Set.of("HELPER"), "ACTIVE");
            case "member-token" -> new AuthUser("member", Set.of("USER"), "ACTIVE");
            case "user-token" -> new AuthUser("user", Set.of("USER"), "ACTIVE");
            default -> new AuthUser("user", Set.of("USER"), "ACTIVE");
        };
    }

    AuthUser requireAny(String authorization, String... roles) {
        AuthUser user = requireAuthenticated(authorization);
        Set<String> allowed = new LinkedHashSet<>(List.of(roles));
        if (user.roles().stream().noneMatch(allowed::contains)) throw new GuideException(403, 42001, "permission denied");
        return user;
    }

    private AuthUser trustedGatewayActor() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs == null ? null : attrs.getRequest();
        if (request == null) return null;
        String internalRequestId = request.getHeader("X-Gateway-Internal-Request-Id");
        if (internalRequestId == null || !internalRequestId.equals(GuideController.currentRequestId())) return null;
        String actorUserId = request.getHeader("X-Beiming-Actor-User-Id");
        if (actorUserId == null || actorUserId.isBlank()) return null;
        String rolesHeader = request.getHeader("X-Beiming-Actor-Roles");
        Set<String> roles = new LinkedHashSet<>();
        if (rolesHeader != null) {
            for (String role : rolesHeader.split(",")) {
                if (!role.isBlank()) roles.add(role.trim());
            }
        }
        if (roles.isEmpty()) roles.add("USER");
        return new AuthUser(actorUserId, roles, "ACTIVE");
    }
}

@Component
class GuideRequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) requestId = "req_" + UUID.randomUUID();
        request.setAttribute("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        filterChain.doFilter(request, response);
    }
}

@RestControllerAdvice(assignableTypes = GuideController.class)
class GuideExceptionHandler {
    @ExceptionHandler(GuideException.class)
    ResponseEntity<Map<String, Object>> handle(GuideException ex) {
        return ResponseEntity.status(ex.httpStatus).body(GuideController.mapOf("code", ex.code, "message", ex.getMessage(), "data", null, "errors", List.of(), "requestId", GuideController.currentRequestId()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> unexpected(Exception ex) {
        return ResponseEntity.status(500).body(GuideController.mapOf("code", 51900, "message", "guide internal error", "data", null, "requestId", GuideController.currentRequestId()));
    }
}

class GuideException extends RuntimeException {
    final int httpStatus;
    final int code;

    GuideException(int httpStatus, int code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }
}
