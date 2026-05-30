package cn.beiming.material;

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
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
class MaterialModule {
    @Bean
    MaterialStore materialStore() {
        MaterialStore store = new MaterialStore();
        store.seed();
        return store;
    }

    @Bean
    TestMaterialAuthProvider materialAuthProvider() {
        return new TestMaterialAuthProvider();
    }
}

@RestController
@RequestMapping("/api/v1/materials")
class MaterialController {
    private final MaterialStore store;
    private final TestMaterialAuthProvider auth;

    MaterialController(MaterialStore store, TestMaterialAuthProvider auth) {
        this.store = store;
        this.auth = auth;
    }

    @GetMapping("/featured")
    Map<String, Object> featured(@RequestParam Map<String, String> query) {
        return ok(Map.of("items", store.featured(query)));
    }

    @GetMapping
    Map<String, Object> publicMaterials(@RequestParam Map<String, String> query) {
        return ok(store.publicMaterials(query));
    }

    @GetMapping("/{materialId}")
    Map<String, Object> publicMaterial(@PathVariable String materialId) {
        return ok(store.publicMaterial(materialId));
    }

    @GetMapping("/by-slug/{slug}")
    Map<String, Object> publicMaterialBySlug(@PathVariable String slug) {
        return ok(store.publicMaterialBySlug(slug));
    }

    @GetMapping("/categories")
    Map<String, Object> publicCategories(@RequestParam Map<String, String> query) {
        return ok(Map.of("items", store.publicCategories(query)));
    }

    @GetMapping("/{materialId}/assets")
    Map<String, Object> publicAssets(@PathVariable String materialId) {
        return ok(Map.of("items", store.publicAssets(materialId)));
    }

    @PostMapping("/me/upload-sessions")
    ResponseEntity<Map<String, Object>> createUploadSession(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                            @RequestBody Map<String, Object> body,
                                                            HttpServletRequest request) {
        AuthUser user = auth.requireAuthenticated(authorization);
        return ResponseEntity.status(HttpStatus.CREATED).body(okData(store.createUploadSession(user, body, request)));
    }

    @PatchMapping("/me/upload-sessions/{uploadSessionId}/complete")
    Map<String, Object> completeUploadSession(@RequestHeader(value = "Authorization", required = false) String authorization,
                                              @PathVariable String uploadSessionId,
                                              @RequestBody Map<String, Object> body,
                                              HttpServletRequest request) {
        AuthUser user = auth.requireAuthenticated(authorization);
        return ok(Map.of("items", store.completeUploadSession(user, uploadSessionId, body, request)));
    }

    @PostMapping("/me/submissions")
    ResponseEntity<Map<String, Object>> createSubmission(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                        @RequestBody Map<String, Object> body,
                                                        HttpServletRequest request) {
        AuthUser user = auth.requireAuthenticated(authorization);
        return ResponseEntity.status(HttpStatus.CREATED).body(okData(store.createSubmission(user, body, request)));
    }

    @GetMapping("/me/submissions")
    Map<String, Object> mySubmissions(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestParam Map<String, String> query) {
        AuthUser user = auth.requireAuthenticated(authorization);
        return ok(store.mySubmissions(user, query));
    }

    @GetMapping("/me/submissions/{materialId}")
    Map<String, Object> mySubmission(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String materialId) {
        AuthUser user = auth.requireAuthenticated(authorization);
        return ok(store.mySubmission(user, materialId));
    }

    @PatchMapping("/me/submissions/{materialId}")
    Map<String, Object> patchSubmission(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @PathVariable String materialId,
                                        @RequestBody Map<String, Object> body,
                                        HttpServletRequest request) {
        AuthUser user = auth.requireAuthenticated(authorization);
        return ok(store.patchSubmission(user, materialId, body, request));
    }

    @PatchMapping("/me/submissions/{materialId}/submit-review")
    Map<String, Object> submitReview(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String materialId,
                                     @RequestBody Map<String, Object> body,
                                     HttpServletRequest request) {
        AuthUser user = auth.requireAuthenticated(authorization);
        return ok(store.submitReview(user, materialId, body, request, false));
    }

    @PatchMapping("/me/submissions/{materialId}/withdraw")
    Map<String, Object> withdraw(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable String materialId,
                                 @RequestBody Map<String, Object> body,
                                 HttpServletRequest request) {
        AuthUser user = auth.requireAuthenticated(authorization);
        return ok(store.withdraw(user, materialId, body, request));
    }

    @PatchMapping("/me/submissions/{materialId}/resubmit")
    Map<String, Object> resubmit(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable String materialId,
                                 @RequestBody Map<String, Object> body,
                                 HttpServletRequest request) {
        AuthUser user = auth.requireAuthenticated(authorization);
        return ok(store.submitReview(user, materialId, body, request, true));
    }

    @GetMapping("/admin/items")
    Map<String, Object> adminItems(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @RequestParam Map<String, String> query,
                                   HttpServletRequest request) {
        checkStoreFailure(request);
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminItems(query));
    }

    @GetMapping("/admin/items/{materialId}")
    Map<String, Object> adminItem(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable String materialId) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminItem(materialId));
    }

    @PatchMapping("/admin/items/{materialId}/approve")
    Map<String, Object> approve(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable String materialId,
                                @RequestBody Map<String, Object> body,
                                HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.review(actor, materialId, body, request, "approve"));
    }

    @PatchMapping("/admin/items/{materialId}/reject")
    Map<String, Object> reject(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable String materialId,
                               @RequestBody Map<String, Object> body,
                               HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.review(actor, materialId, body, request, "reject"));
    }

    @PatchMapping("/admin/items/{materialId}/request-changes")
    Map<String, Object> requestChanges(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable String materialId,
                                       @RequestBody Map<String, Object> body,
                                       HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.review(actor, materialId, body, request, "request-changes"));
    }

    @PatchMapping("/admin/items/{materialId}/feature")
    Map<String, Object> feature(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable String materialId,
                                @RequestBody Map<String, Object> body,
                                HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.adminState(actor, materialId, body, request, "feature"));
    }

    @PatchMapping("/admin/items/{materialId}/unfeature")
    Map<String, Object> unfeature(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable String materialId,
                                  @RequestBody Map<String, Object> body,
                                  HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.adminState(actor, materialId, body, request, "unfeature"));
    }

    @PatchMapping("/admin/items/{materialId}/offline")
    Map<String, Object> offline(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable String materialId,
                                @RequestBody Map<String, Object> body,
                                HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.adminState(actor, materialId, body, request, "offline"));
    }

    @PatchMapping("/admin/items/{materialId}/archive")
    Map<String, Object> archive(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @PathVariable String materialId,
                                @RequestBody Map<String, Object> body,
                                HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.adminState(actor, materialId, body, request, "archive"));
    }

    @PatchMapping("/admin/items/{materialId}/delete")
    Map<String, Object> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable String materialId,
                               @RequestBody Map<String, Object> body,
                               HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.adminState(actor, materialId, body, request, "delete"));
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

    @GetMapping("/admin/assets")
    Map<String, Object> adminAssets(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminAssets(query));
    }

    @PatchMapping("/admin/assets/{assetId}/security-status")
    Map<String, Object> patchAssetStatus(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @PathVariable String assetId,
                                         @RequestBody Map<String, Object> body,
                                         HttpServletRequest request) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.patchAssetStatus(actor, assetId, body, request));
    }

    @GetMapping("/admin/items/{materialId}/audit-logs")
    Map<String, Object> auditLogs(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable String materialId,
                                  @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.auditLogs(materialId, query));
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
            throw new MaterialException(500, 51700, "material internal error");
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

class MaterialStore {
    private static final String NOW = "2026-05-30T00:00:00Z";
    private static final Set<String> KINDS = Set.of("IMAGE", "VIDEO", "BUILD_SCREENSHOT", "PROJECT_RECORD", "EVENT_MEMORY", "DOCUMENT_ATTACHMENT", "OTHER");
    private static final Set<String> SAFE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "gif", "mp4", "webm", "pdf", "txt", "md");
    private static final Set<String> IMAGE_MIMES = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");
    private static final Set<String> VIDEO_MIMES = Set.of("video/mp4", "video/webm");
    private static final Set<String> DOCUMENT_MIMES = Set.of("application/pdf", "text/plain", "text/markdown");
    private static final Set<String> IMAGE_VIDEO_MIMES = Set.of("image/png", "image/jpeg", "image/webp", "image/gif", "video/mp4", "video/webm");
    private static final Set<String> ALL_SAFE_MIMES = Set.of("image/png", "image/jpeg", "image/webp", "image/gif", "video/mp4", "video/webm", "application/pdf", "text/plain", "text/markdown");
    private static final Set<String> PUBLIC_SORTS = Set.of("publishedAt_desc", "updatedAt_desc", "title_asc", "featured_desc");
    private static final Set<String> ADMIN_SORTS = Set.of("submittedAt_desc", "updatedAt_desc", "publishedAt_desc", "title_asc");
    private static final Set<String> ASSET_SORTS = Set.of("createdAt_desc", "createdAt_asc", "size_desc");
    private static final Set<String> AUDIT_SORTS = Set.of("createdAt_desc", "createdAt_asc");

    private final Map<String, Map<String, Object>> materials = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> categories = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> assets = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> sessions = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> idem = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> audits = new ArrayList<>();
    private int idSeq = 1000;

    void seed() {
        addCategory("cat-builds", "Builds", "builds", true, false);
        addCategory("cat-events", "Events", "events", true, false);
        addCategory("cat-free", "Free", "free", true, false);
        addCategory("cat-disabled", "Disabled", "disabled", false, false);
        addCategory("cat-archived", "Archived", "archived", true, true);

        addAsset(asset("asset-featured-cover", "mat-featured", "member", "SAFE", "featured.png"));
        addAsset(asset("asset-approved-cover", "mat-approved", "member", "SAFE", "approved.png"));
        addAsset(asset("asset-public-cover", "mat-public", "member", "SAFE", "public.png"));
        addAsset(asset("asset-unsafe", "mat-unsafe", "member", "REJECTED", "unsafe.png"));
        addAsset(asset("asset-owned-safe", null, "member", "SAFE", "owned.png"));
        addAsset(asset("asset-scanning", null, "member", "SCANNING", "scan.png"));

        addMaterial("mat-featured", "featured-spawn", "IMAGE", "FEATURED", "PUBLIC", "cat-builds", List.of("spawn", "build"), List.of("asset-featured-cover"), true, "member", true);
        addMaterial("mat-approved", "approved-spawn", "IMAGE", "APPROVED", "PUBLIC", "cat-builds", List.of("spawn"), List.of("asset-approved-cover"), true, "member", true);
        addMaterial("mat-public", "public-road", "IMAGE", "APPROVED", "PUBLIC", "cat-events", List.of("road"), List.of("asset-public-cover"), true, "member", true);
        addMaterial("mat-draft", "draft-spawn", "IMAGE", "DRAFT", "PUBLIC", "cat-builds", List.of("draft"), List.of("asset-owned-safe"), true, "member", true);
        addMaterial("mat-pending", "pending-spawn", "IMAGE", "PENDING_REVIEW", "PUBLIC", "cat-builds", List.of("pending"), List.of("asset-owned-safe"), true, "member", true);
        addMaterial("mat-pending-approve", "pending-approve", "IMAGE", "PENDING_REVIEW", "PUBLIC", "cat-builds", List.of("pending"), List.of("asset-owned-safe"), true, "member", true);
        addMaterial("mat-pending-reject", "pending-reject", "IMAGE", "PENDING_REVIEW", "PUBLIC", "cat-builds", List.of("pending"), List.of("asset-owned-safe"), true, "member", true);
        addMaterial("mat-pending-changes", "pending-changes", "IMAGE", "PENDING_REVIEW", "PUBLIC", "cat-builds", List.of("pending"), List.of("asset-owned-safe"), true, "member", true);
        addMaterial("mat-pending-unsafe", "pending-unsafe", "IMAGE", "PENDING_REVIEW", "PUBLIC", "cat-builds", List.of("pending"), List.of("asset-unsafe"), true, "member", true);
        addMaterial("mat-pending-aux", "pending-aux", "IMAGE", "PENDING_REVIEW", "PUBLIC", "cat-builds", List.of("pending"), List.of("asset-owned-safe"), true, "member", true);
        addMaterial("mat-rejected", "rejected-spawn", "IMAGE", "REJECTED", "PUBLIC", "cat-builds", List.of("reject"), List.of("asset-owned-safe"), true, "member", true);
        addMaterial("mat-needs", "needs-spawn", "IMAGE", "NEEDS_CHANGES", "PUBLIC", "cat-builds", List.of("needs"), List.of("asset-owned-safe"), true, "member", true);
        addMaterial("mat-offline", "offline-spawn", "IMAGE", "OFFLINE", "PUBLIC", "cat-builds", List.of("offline"), List.of("asset-owned-safe"), true, "member", true);
        addMaterial("mat-archived", "archived-spawn", "IMAGE", "ARCHIVED", "PUBLIC", "cat-builds", List.of("archived"), List.of("asset-owned-safe"), true, "member", true);
        addMaterial("mat-deleted", "deleted-spawn", "IMAGE", "DELETED", "PUBLIC", "cat-builds", List.of("deleted"), List.of("asset-owned-safe"), true, "member", true);
        addMaterial("mat-private", "private-spawn", "IMAGE", "APPROVED", "PRIVATE", "cat-builds", List.of("private"), List.of("asset-owned-safe"), true, "member", true);
        addMaterial("mat-unsafe", "unsafe-spawn", "IMAGE", "APPROVED", "PUBLIC", "cat-builds", List.of("unsafe"), List.of("asset-unsafe"), true, "member", true);
        addMaterial("mat-no-license", "no-license", "IMAGE", "DRAFT", "PUBLIC", "cat-builds", List.of("bad"), List.of("asset-owned-safe"), false, "member", true);
        addMaterial("mat-unsafe-draft", "unsafe-draft", "IMAGE", "DRAFT", "PUBLIC", "cat-builds", List.of("bad"), List.of("asset-unsafe"), true, "member", true);
        addMaterial("mat-no-feature-license", "no-feature-license", "IMAGE", "APPROVED", "PUBLIC", "cat-builds", List.of("license"), List.of("asset-approved-cover"), true, "member", false);
        addMaterial("mat-other-user", "other-user", "IMAGE", "DRAFT", "PUBLIC", "cat-builds", List.of("other"), List.of("asset-owned-safe"), true, "other", true);
        addMaterial("mat-no-author", "no-author", "IMAGE", "PENDING_REVIEW", "PUBLIC", "cat-builds", List.of("pending"), List.of("asset-owned-safe"), true, null, true);

        sessions.put("sess-expired", mapOf("uploadSessionId", "sess-expired", "ownerUserId", "member", "uploadTicket", "expired-ticket", "status", "EXPIRED", "expiresAt", "2020-01-01T00:00:00Z", "createdAt", NOW, "expectedFileNames", List.of("expired.png"), "checksumSha256", "a".repeat(64)));
        audit(null, "mat-featured", "MATERIAL_FEATURED", "SUCCESS", null, materials.get("mat-featured"), "seed");
    }

    List<Map<String, Object>> featured(Map<String, String> query) {
        int limit = intQuery(query, "limit", 12);
        if (limit < 1 || limit > 50) throw new MaterialException(400, 40001, "invalid limit");
        return publicStream(query).stream()
                .filter(item -> "FEATURED".equals(item.get("status")))
                .limit(limit)
                .map(this::publicSummary)
                .toList();
    }

    Map<String, Object> publicMaterials(Map<String, String> query) {
        Page page = page(query);
        validateSort(query.get("sort"), PUBLIC_SORTS, "publishedAt_desc");
        List<Map<String, Object>> items = publicStream(query).stream()
                .sorted(publicComparator(query.getOrDefault("sort", "publishedAt_desc")))
                .toList();
        return page(items.stream().map(this::publicSummary).toList(), page);
    }

    Map<String, Object> publicMaterial(String materialId) {
        return publicDetail(requirePublic(materialId));
    }

    Map<String, Object> publicMaterialBySlug(String slug) {
        Map<String, Object> match = materials.values().stream()
                .filter(item -> slug.equals(item.get("slug")))
                .findFirst()
                .orElseThrow(() -> new MaterialException(404, 43700, "material not found"));
        return publicDetail(requirePublic(str(match.get("materialId"))));
    }

    List<Map<String, Object>> publicCategories(Map<String, String> query) {
        String kind = query.get("kind");
        if (kind != null && !KINDS.contains(kind)) {
            throw new MaterialException(400, 40001, "invalid kind");
        }
        String keyword = lower(query.get("keyword"));
        return categories.values().stream()
                .filter(item -> bool(item.get("enabled")))
                .filter(item -> !bool(item.get("archived")))
                .filter(item -> kind == null || kind.equals(item.get("kind")))
                .filter(item -> keyword == null || lower(str(item.get("name"))).contains(keyword) || lower(str(item.get("slug"))).contains(keyword))
                .sorted(Comparator.<Map<String, Object>>comparingInt(item -> intValue(item.get("sortOrder"), 100)).thenComparing(item -> str(item.get("name"))))
                .map(this::categoryPublicView)
                .toList();
    }

    List<Map<String, Object>> publicAssets(String materialId) {
        Map<String, Object> material = requirePublic(materialId);
        return materialAssets(material).stream()
                .filter(asset -> "SAFE".equals(asset.get("status")))
                .map(this::publicAsset)
                .toList();
    }

    synchronized Map<String, Object> createUploadSession(AuthUser user, Map<String, Object> body, HttpServletRequest request) {
        checkStorage(request);
        String key = str(body.get("idempotencyKey"));
        String idemKey = user.id() + ":upload-session:" + key;
        @SuppressWarnings("unchecked")
        Map<String, Object> existing = (Map<String, Object>) replay(idemKey, body);
        if (existing != null) return existing;
        validateUploadSessionBody(body);
        List<String> expectedFileNames = list(body.get("expectedFileNames"));
        List<String> expectedMimeTypes = list(body.get("expectedMimeTypes"));

        String id = "sess-" + (++idSeq);
        Map<String, Object> session = mapOf(
                "uploadSessionId", id,
                "provider", "LOCAL_STUB",
                "purpose", "MATERIAL_SUBMISSION",
                "ownerUserId", user.id(),
                "kind", body.get("kind"),
                "allowedExtensions", expectedFileNames.stream().map(this::extensionOf).toList(),
                "allowedMimeTypes", expectedMimeTypes,
                "maxFileSizeBytes", body.get("maxFileSizeBytes"),
                "maxFiles", expectedFileNames.size(),
                "uploadTicket", "ticket-" + id,
                "uploadTarget", "/local-stub/materials/" + id,
                "status", "PENDING_UPLOAD",
                "expiresAt", "2026-05-30T01:00:00Z",
                "createdAt", NOW,
                "expectedFileNames", expectedFileNames,
                "checksumSha256", body.get("checksumSha256")
        );
        sessions.put(id, session);
        remember(idemKey, body, session);
        audit(user, id, "MATERIAL_UPLOAD_SESSION_CREATED", "SUCCESS", null, safeSession(session), "upload session");
        return new LinkedHashMap<>(session);
    }

    synchronized List<Map<String, Object>> completeUploadSession(AuthUser user, String sessionId, Map<String, Object> body, HttpServletRequest request) {
        checkStorage(request);
        if ("true".equals(request.getHeader("X-Test-Fail-Upload-Record"))) {
            throw new MaterialException(500, 51702, "upload record failed");
        }
        Map<String, Object> session = sessions.get(sessionId);
        if (session == null || !user.id().equals(session.get("ownerUserId"))) {
            throw new MaterialException(404, 43701, "upload session not found");
        }
        if (!Objects.equals(body.get("uploadTicket"), session.get("uploadTicket"))) {
            throw new MaterialException(404, 43701, "upload ticket invalid");
        }
        String key = str(body.get("idempotencyKey"));
        String idemKey = user.id() + ":complete:" + sessionId + ":" + key;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> existing = (List<Map<String, Object>>) replay(idemKey, body);
        if (existing != null) return existing;
        String status = str(session.get("status"));
        if ("EXPIRED".equals(status)) {
            throw new MaterialException(404, 43701, "upload session not found");
        }
        if (!"PENDING_UPLOAD".equals(status)) {
            throw new MaterialException(409, 43710, "upload session already completed");
        }
        List<Map<String, Object>> files = listOfMaps(body.get("files"));
        if (files.isEmpty() || files.size() > intValue(session.get("maxFiles"), 1)) {
            throw new MaterialException(400, 43712, "invalid file count");
        }
        List<Map<String, Object>> created = new ArrayList<>();
        for (Map<String, Object> file : files) {
            validateUploadedFile(session, file);
            String assetId = "asset-" + (++idSeq);
            Map<String, Object> asset = asset(assetId, null, user.id(), "SAFE", str(file.get("displayName")));
            asset.put("uploadSessionId", sessionId);
            asset.put("mimeType", file.get("mimeType"));
            asset.put("extension", lower(str(file.get("extension"))));
            asset.put("fileSizeBytes", intValue(file.get("fileSizeBytes"), 0));
            asset.put("checksumSha256", file.get("checksumSha256"));
            asset.put("width", file.get("width"));
            asset.put("height", file.get("height"));
            asset.put("durationSeconds", file.get("durationSeconds"));
            addAsset(asset);
            created.add(publicAsset(asset));
        }
        session.put("status", "UPLOADED");
        remember(idemKey, body, created);
        audit(user, sessionId, "MATERIAL_UPLOAD_COMPLETED", "SUCCESS", null, Map.of("files", created.size()), "complete upload");
        return created;
    }

    synchronized Map<String, Object> createSubmission(AuthUser user, Map<String, Object> body, HttpServletRequest request) {
        validateSubmissionBody(body, true);
        String key = str(body.get("idempotencyKey"));
        String idemKey = user.id() + ":submission:" + key;
        @SuppressWarnings("unchecked")
        Map<String, Object> existing = (Map<String, Object>) replay(idemKey, body);
        if (existing != null) return existing;
        if (slugExists(str(body.get("slug")))) throw new MaterialException(409, 43711, "slug conflict");
        if (!categories.containsKey(str(body.get("categoryId")))) throw new MaterialException(404, 43703, "category not found");
        List<String> assetIds = list(body.get("assetIds"));
        for (String assetId : assetIds) {
            Map<String, Object> asset = assets.get(assetId);
            if (asset == null) throw new MaterialException(404, 43702, "asset not found");
            if (!user.id().equals(asset.get("ownerUserId"))) throw new MaterialException(404, 43702, "asset not found");
            if (!"SAFE".equals(asset.get("status"))) throw new MaterialException(409, 43715, "asset unsafe");
        }
        String id = "mat-" + (++idSeq);
        Map<String, Object> material = baseMaterial(id, str(body.get("slug")), str(body.get("kind")), "DRAFT", str(body.get("visibility")), str(body.get("categoryId")), list(body.get("tags")), assetIds, user.id(), license(body));
        copySubmissionFields(body, material);
        materials.put(id, material);
        for (String assetId : assetIds) {
            assets.get(assetId).put("materialId", id);
        }
        Map<String, Object> view = myView(material);
        remember(idemKey, body, view);
        audit(user, id, "MATERIAL_CREATED", "SUCCESS", null, material, "create material");
        return view;
    }

    Map<String, Object> mySubmissions(AuthUser user, Map<String, String> query) {
        Page page = page(query);
        String status = query.get("status");
        String kind = query.get("kind");
        if (kind != null && !KINDS.contains(kind)) throw new MaterialException(400, 40001, "invalid kind");
        String keyword = lower(query.get("keyword"));
        List<Map<String, Object>> items = materials.values().stream()
                .filter(item -> user.id().equals(item.get("authorUserId")))
                .filter(item -> status == null || status.equals(item.get("status")))
                .filter(item -> kind == null || kind.equals(item.get("kind")))
                .filter(item -> keyword == null || lower(str(item.get("title"))).contains(keyword) || lower(str(item.get("slug"))).contains(keyword))
                .sorted(Comparator.comparing(item -> str(item.get("updatedAt")), Comparator.reverseOrder()))
                .map(this::myView)
                .toList();
        return page(items, page);
    }

    Map<String, Object> mySubmission(AuthUser user, String materialId) {
        Map<String, Object> material = materials.get(materialId);
        if (material == null || !user.id().equals(material.get("authorUserId"))) {
            throw new MaterialException(404, 43700, "material not found");
        }
        return myView(material);
    }

    synchronized Map<String, Object> patchSubmission(AuthUser user, String materialId, Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> material = requireMine(user, materialId);
        if (!Set.of("DRAFT", "NEEDS_CHANGES").contains(material.get("status"))) {
            throw new MaterialException(409, 43710, "invalid state");
        }
        failAudit(request);
        Map<String, Object> before = snapshot(material);
        copySubmissionFields(body, material);
        material.put("updatedBy", user.id());
        material.put("updatedAt", NOW);
        audit(user, materialId, "MATERIAL_UPDATED", "SUCCESS", before, material, str(body.get("reason")));
        return myView(material);
    }

    synchronized Map<String, Object> submitReview(AuthUser user, String materialId, Map<String, Object> body, HttpServletRequest request, boolean resubmit) {
        Map<String, Object> material = requireMine(user, materialId);
        validateReason(body);
        validateProfile(user);
        String status = str(material.get("status"));
        Set<String> allowed = resubmit ? Set.of("REJECTED", "NEEDS_CHANGES") : Set.of("DRAFT", "REJECTED", "NEEDS_CHANGES", "PENDING_REVIEW");
        if (!allowed.contains(status)) throw new MaterialException(409, 43710, "invalid state");
        validateMaterialReady(material);
        if ("PENDING_REVIEW".equals(status)) return myView(material);
        failAudit(request);
        Map<String, Object> before = snapshot(material);
        material.put("status", "PENDING_REVIEW");
        material.put("submittedAt", NOW);
        material.put("updatedAt", NOW);
        audit(user, materialId, resubmit ? "MATERIAL_RESUBMITTED" : "MATERIAL_SUBMITTED", "SUCCESS", before, material, str(body.get("reason")));
        return myView(material);
    }

    synchronized Map<String, Object> withdraw(AuthUser user, String materialId, Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> material = requireMine(user, materialId);
        validateReason(body);
        if ("DRAFT".equals(material.get("status"))) return myView(material);
        if (!"PENDING_REVIEW".equals(material.get("status"))) throw new MaterialException(409, 43710, "invalid state");
        failAudit(request);
        Map<String, Object> before = snapshot(material);
        material.put("status", "DRAFT");
        material.put("updatedAt", NOW);
        audit(user, materialId, "MATERIAL_WITHDRAWN", "SUCCESS", before, material, str(body.get("reason")));
        return myView(material);
    }

    Map<String, Object> adminItems(Map<String, String> query) {
        Page page = page(query);
        validateSort(query.get("sort"), ADMIN_SORTS, "updatedAt_desc");
        String status = query.get("status");
        String kind = query.get("kind");
        if (kind != null && !KINDS.contains(kind)) throw new MaterialException(400, 40001, "invalid kind");
        String visibility = query.get("visibility");
        String categoryId = query.get("categoryId");
        String authorUserId = query.get("authorUserId");
        String assetStatus = query.get("assetStatus");
        String keyword = lower(query.get("keyword"));
        List<Map<String, Object>> items = materials.values().stream()
                .filter(item -> status == null || status.equals(item.get("status")))
                .filter(item -> kind == null || kind.equals(item.get("kind")))
                .filter(item -> visibility == null || visibility.equals(item.get("visibility")))
                .filter(item -> categoryId == null || categoryId.equals(item.get("categoryId")))
                .filter(item -> authorUserId == null || authorUserId.equals(item.get("authorUserId")))
                .filter(item -> assetStatus == null || materialAssets(item).stream().anyMatch(asset -> assetStatus.equals(asset.get("status"))))
                .filter(item -> keyword == null || lower(str(item.get("title"))).contains(keyword) || lower(str(item.get("slug"))).contains(keyword) || lower(str(item.get("summary"))).contains(keyword))
                .sorted(adminComparator(query.getOrDefault("sort", "updatedAt_desc")))
                .map(this::adminView)
                .toList();
        return page(items, page);
    }

    Map<String, Object> adminItem(String materialId) {
        return adminView(requireMaterial(materialId));
    }

    synchronized Map<String, Object> review(AuthUser actor, String materialId, Map<String, Object> body, HttpServletRequest request, String action) {
        Map<String, Object> material = requireMaterial(materialId);
        validateReason(body);
        requireReview(body);
        String key = str(body.get("idempotencyKey"));
        String idemKey = actor.id() + ":review:" + action + ":" + materialId + ":" + key;
        @SuppressWarnings("unchecked")
        Map<String, Object> existing = (Map<String, Object>) replay(idemKey, body);
        if (existing != null) return existing;
        if ("request-changes".equals(action) && str(body.get("publicComment")) == null) {
            throw new MaterialException(400, 40001, "public comment required");
        }
        String status = str(material.get("status"));
        if ("approve".equals(action) && "APPROVED".equals(status)) return adminView(material);
        if ("reject".equals(action) && "REJECTED".equals(status)) return adminView(material);
        if (!"PENDING_REVIEW".equals(status)) throw new MaterialException(409, 43710, "invalid state");
        if ("approve".equals(action)) validateMaterialReady(material);
        if (!"approve".equals(action)) requireNotification(material, request);
        failAudit(request);
        Map<String, Object> before = snapshot(material);
        switch (action) {
            case "approve" -> {
                material.put("status", "APPROVED");
                material.put("publishedAt", NOW);
                if ("aux-fail".equals(request.getHeader("X-Test-Notification-Mode"))) {
                    material.put("notificationStatus", "FAILED");
                }
            }
            case "reject" -> material.put("status", "REJECTED");
            case "request-changes" -> {
                material.put("status", "NEEDS_CHANGES");
                material.put("publicComment", body.get("publicComment"));
            }
            default -> throw new MaterialException(409, 43710, "invalid state");
        }
        material.put("reviewOpinion", body.get("reviewOpinion"));
        material.put("reviewedAt", NOW);
        material.put("reviewedBy", actor.id());
        material.put("updatedAt", NOW);
        audit(actor, materialId, "MATERIAL_" + action.toUpperCase().replace('-', '_'), "SUCCESS", before, material, str(body.get("reason")));
        Map<String, Object> view = adminView(material);
        remember(idemKey, body, view);
        return view;
    }

    synchronized Map<String, Object> adminState(AuthUser actor, String materialId, Map<String, Object> body, HttpServletRequest request, String action) {
        Map<String, Object> material = requireMaterial(materialId);
        validateReason(body);
        String key = str(body.get("idempotencyKey"));
        String idemKey = actor.id() + ":admin-state:" + action + ":" + materialId + ":" + key;
        @SuppressWarnings("unchecked")
        Map<String, Object> existing = (Map<String, Object>) replay(idemKey, body);
        if (existing != null) return existing;
        String status = str(material.get("status"));
        String next = null;
        switch (action) {
            case "feature" -> {
                if ("FEATURED".equals(status)) return adminView(material);
                if (!"APPROVED".equals(status)) throw new MaterialException(409, 43710, "invalid state");
                if (!bool(map(material.get("license")).get("allowHomepageFeature"))) throw new MaterialException(400, 43713, "license disallows feature");
                next = "FEATURED";
            }
            case "unfeature" -> {
                if (!"FEATURED".equals(status)) throw new MaterialException(409, 43710, "invalid state");
                next = "APPROVED";
            }
            case "offline" -> {
                if ("OFFLINE".equals(status)) return adminView(material);
                if (!Set.of("APPROVED", "FEATURED").contains(status)) throw new MaterialException(409, 43710, "invalid state");
                next = "OFFLINE";
            }
            case "archive" -> {
                if ("ARCHIVED".equals(status)) return adminView(material);
                if (!Set.of("DRAFT", "REJECTED", "NEEDS_CHANGES", "OFFLINE").contains(status)) throw new MaterialException(409, 43710, "invalid state");
                next = "ARCHIVED";
            }
            case "delete" -> {
                if ("DELETED".equals(status)) return adminView(material);
                if (Set.of("APPROVED", "FEATURED", "PENDING_REVIEW").contains(status)) throw new MaterialException(409, 43710, "invalid state");
                next = "DELETED";
            }
            default -> throw new MaterialException(409, 43710, "invalid state");
        }
        failAudit(request);
        Map<String, Object> before = snapshot(material);
        material.put("status", next);
        material.put("featured", "FEATURED".equals(next));
        if ("delete".equals(action)) material.put("deletedAt", NOW);
        material.put("updatedAt", NOW);
        audit(actor, materialId, "MATERIAL_" + action.toUpperCase(), "SUCCESS", before, material, str(body.get("reason")));
        Map<String, Object> view = adminView(material);
        remember(idemKey, body, view);
        return view;
    }

    List<Map<String, Object>> adminCategories(Map<String, String> query) {
        Boolean includeArchived = boolQuery(query.get("includeArchived"), true);
        Boolean enabled = query.containsKey("enabled") ? boolQuery(query.get("enabled"), true) : null;
        String kind = query.get("kind");
        if (kind != null && !KINDS.contains(kind)) throw new MaterialException(400, 40001, "invalid kind");
        String keyword = lower(query.get("keyword"));
        return categories.values().stream()
                .filter(item -> includeArchived || !bool(item.get("archived")))
                .filter(item -> enabled == null || bool(item.get("enabled")) == enabled)
                .filter(item -> kind == null || kind.equals(item.get("kind")))
                .filter(item -> keyword == null || lower(str(item.get("name"))).contains(keyword) || lower(str(item.get("slug"))).contains(keyword))
                .sorted(Comparator.comparingInt(item -> intValue(item.get("sortOrder"), 100)))
                .map(this::snapshot)
                .toList();
    }

    synchronized Map<String, Object> createCategory(AuthUser actor, Map<String, Object> body, HttpServletRequest request) {
        validateCategoryBody(body, true);
        String key = str(body.get("idempotencyKey"));
        String idemKey = key == null ? null : actor.id() + ":category:" + key;
        if (idemKey != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> existing = (Map<String, Object>) replay(idemKey, body);
            if (existing != null) return existing;
        }
        if (categorySlugExists(str(body.get("slug")))) throw new MaterialException(409, 43711, "category conflict");
        failAudit(request);
        Map<String, Object> category = addCategory("cat-" + (++idSeq), str(body.get("name")), str(body.get("slug")), boolValue(body.getOrDefault("enabled", true)), false);
        category.put("description", body.getOrDefault("description", null));
        category.put("sortOrder", intValue(body.get("sortOrder"), 100));
        category.put("kind", body.getOrDefault("kind", "IMAGE"));
        if (idemKey != null) remember(idemKey, body, category);
        audit(actor, str(category.get("categoryId")), "MATERIAL_CATEGORY_CREATED", "SUCCESS", null, category, str(body.get("reason")));
        return category;
    }

    synchronized Map<String, Object> patchCategory(AuthUser actor, String categoryId, Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> category = categories.get(categoryId);
        if (category == null) throw new MaterialException(404, 43703, "category not found");
        validateReason(body);
        validateCategoryBody(body, false);
        String key = str(body.get("idempotencyKey"));
        String idemKey = actor.id() + ":category-patch:" + categoryId + ":" + key;
        @SuppressWarnings("unchecked")
        Map<String, Object> existing = (Map<String, Object>) replay(idemKey, body);
        if (existing != null) return existing;
        if (body.containsKey("slug") && categories.values().stream().anyMatch(item -> !categoryId.equals(item.get("categoryId")) && str(body.get("slug")).equals(item.get("slug")) && !bool(item.get("archived")))) {
            throw new MaterialException(409, 43711, "category conflict");
        }
        failAudit(request);
        Map<String, Object> before = snapshot(category);
        for (String field : List.of("name", "slug", "description", "sortOrder", "enabled", "kind")) {
            if (body.containsKey(field)) category.put(field, body.get(field));
        }
        category.put("updatedAt", NOW);
        audit(actor, categoryId, "MATERIAL_CATEGORY_UPDATED", "SUCCESS", before, category, str(body.get("reason")));
        Map<String, Object> view = snapshot(category);
        remember(idemKey, body, view);
        return view;
    }

    synchronized Map<String, Object> archiveCategory(AuthUser actor, String categoryId, Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> category = categories.get(categoryId);
        if (category == null) throw new MaterialException(404, 43703, "category not found");
        validateReason(body);
        if (bool(category.get("archived"))) return snapshot(category);
        boolean used = materials.values().stream().anyMatch(item -> categoryId.equals(item.get("categoryId")) && !Set.of("ARCHIVED", "DELETED").contains(item.get("status")));
        if (used) throw new MaterialException(409, 43716, "category in use");
        failAudit(request);
        Map<String, Object> before = snapshot(category);
        category.put("archived", true);
        category.put("archivedAt", NOW);
        category.put("updatedAt", NOW);
        audit(actor, categoryId, "MATERIAL_CATEGORY_ARCHIVED", "SUCCESS", before, category, str(body.get("reason")));
        return snapshot(category);
    }

    Map<String, Object> adminAssets(Map<String, String> query) {
        Page page = page(query);
        validateSort(query.get("sort"), ASSET_SORTS, "createdAt_asc");
        String status = query.get("status");
        String owner = query.get("ownerUserId");
        String materialId = query.get("materialId");
        String extension = lower(query.get("extension"));
        String mimeType = query.get("mimeType");
        List<Map<String, Object>> items = assets.values().stream()
                .filter(asset -> status == null || status.equals(asset.get("status")))
                .filter(asset -> owner == null || owner.equals(asset.get("ownerUserId")))
                .filter(asset -> materialId == null || materialId.equals(asset.get("materialId")))
                .filter(asset -> extension == null || extension.equals(asset.get("extension")))
                .filter(asset -> mimeType == null || mimeType.equals(asset.get("mimeType")))
                .sorted(assetComparator(query.getOrDefault("sort", "createdAt_asc")))
                .map(this::adminAsset)
                .toList();
        return page(items, page);
    }

    synchronized Map<String, Object> patchAssetStatus(AuthUser actor, String assetId, Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> asset = assets.get(assetId);
        if (asset == null) throw new MaterialException(404, 43702, "asset not found");
        validateReason(body);
        String status = str(body.get("status"));
        if (!Set.of("SCANNING", "SAFE", "REJECTED", "QUARANTINED").contains(status)) throw new MaterialException(400, 40001, "invalid status");
        if ("REJECTED".equals(status) && str(body.get("securityRejectReason")) == null) throw new MaterialException(400, 40001, "security reason required");
        String key = str(body.get("idempotencyKey"));
        String idemKey = actor.id() + ":asset-status:" + assetId + ":" + key;
        @SuppressWarnings("unchecked")
        Map<String, Object> existing = (Map<String, Object>) replay(idemKey, body);
        if (existing != null) return existing;
        failAudit(request);
        Map<String, Object> before = snapshot(asset);
        asset.put("status", status);
        asset.put("securityRejectReason", body.getOrDefault("securityRejectReason", null));
        asset.put("updatedAt", NOW);
        audit(actor, assetId, "MATERIAL_ASSET_SECURITY_UPDATED", "SUCCESS", before, asset, str(body.get("reason")));
        Map<String, Object> view = adminAsset(asset);
        remember(idemKey, body, view);
        return view;
    }

    Map<String, Object> auditLogs(String materialId, Map<String, String> query) {
        if (!materials.containsKey(materialId)) throw new MaterialException(404, 43700, "material not found");
        Page page = page(query);
        Instant from = parseInstant(query.get("from"));
        Instant to = parseInstant(query.get("to"));
        if (from != null && to != null && from.isAfter(to)) throw new MaterialException(400, 40001, "invalid time range");
        String action = query.get("action");
        String actorUserId = query.get("actorUserId");
        String result = query.get("result");
        validateSort(query.get("sort"), AUDIT_SORTS, "createdAt_desc");
        List<Map<String, Object>> items = audits.stream()
                .filter(audit -> materialId.equals(audit.get("targetId")))
                .filter(audit -> action == null || action.equals(audit.get("action")))
                .filter(audit -> actorUserId == null || actorUserId.equals(audit.get("actorUserId")))
                .filter(audit -> result == null || result.equals(audit.get("result")))
                .filter(audit -> from == null || !Instant.parse(str(audit.get("createdAt"))).isBefore(from))
                .filter(audit -> to == null || !Instant.parse(str(audit.get("createdAt"))).isAfter(to))
                .sorted(auditComparator(query.getOrDefault("sort", "createdAt_desc")))
                .map(this::snapshot)
                .toList();
        return page(items, page);
    }

    Map<String, Object> opsSummary() {
        Map<String, Long> assetCounts = new LinkedHashMap<>();
        for (Map<String, Object> asset : assets.values()) {
            assetCounts.merge(str(asset.get("status")), 1L, Long::sum);
        }
        return mapOf(
                "service", "material",
                "port", 8126,
                "storageMode", "IN_MEMORY",
                "authMode", "TEST_STUB",
                "profileMode", "TEST_STUB",
                "notificationMode", "TEST_STUB",
                "uploadProvider", "LOCAL_STUB",
                "materialsTotal", materials.size(),
                "pendingReviewTotal", materials.values().stream().filter(item -> "PENDING_REVIEW".equals(item.get("status"))).count(),
                "featuredTotal", materials.values().stream().filter(item -> "FEATURED".equals(item.get("status"))).count(),
                "assetsTotal", assets.size(),
                "assetStatusCounts", assetCounts,
                "auditsTotal", audits.size(),
                "idempotencyRecordsTotal", idem.size(),
                "lastAuditAt", audits.isEmpty() ? null : audits.get(audits.size() - 1).get("createdAt"),
                "productionGaps", List.of("PERSISTENT_STORAGE_NOT_ENABLED", "REAL_AUTH_ADAPTER_NOT_ENABLED", "REAL_PROFILE_ADAPTER_NOT_ENABLED", "REAL_NOTIFICATION_ADAPTER_NOT_ENABLED", "REAL_OBJECT_STORAGE_NOT_ENABLED", "GATEWAY_INTERNAL_SIGNATURE_NOT_ENABLED")
        );
    }

    private List<Map<String, Object>> publicStream(Map<String, String> query) {
        String kind = query.get("kind");
        if (kind != null && !KINDS.contains(kind)) throw new MaterialException(400, 40001, "invalid kind");
        String categoryId = query.get("categoryId");
        String tag = query.get("tag");
        String authorUserId = query.get("authorUserId");
        String keyword = lower(query.get("keyword"));
        return materials.values().stream()
                .filter(this::isPublicVisible)
                .filter(item -> kind == null || kind.equals(item.get("kind")))
                .filter(item -> categoryId == null || categoryId.equals(item.get("categoryId")))
                .filter(item -> tag == null || list(item.get("tags")).contains(tag))
                .filter(item -> authorUserId == null || authorUserId.equals(item.get("authorUserId")))
                .filter(item -> keyword == null || lower(str(item.get("title"))).contains(keyword) || lower(str(item.get("summary"))).contains(keyword))
                .toList();
    }

    private boolean isPublicVisible(Map<String, Object> material) {
        String status = str(material.get("status"));
        Instant now = Instant.parse(NOW);
        String visibleFrom = str(material.get("visibleFrom"));
        String visibleUntil = str(material.get("visibleUntil"));
        return Set.of("APPROVED", "FEATURED").contains(status)
                && "PUBLIC".equals(material.get("visibility"))
                && (visibleFrom == null || !now.isBefore(Instant.parse(visibleFrom)))
                && (visibleUntil == null || !now.isAfter(Instant.parse(visibleUntil)))
                && materialAssets(material).stream().allMatch(asset -> "SAFE".equals(asset.get("status")));
    }

    private Map<String, Object> requirePublic(String materialId) {
        Map<String, Object> material = materials.get(materialId);
        if (material == null || !isPublicVisible(material)) throw new MaterialException(404, 43700, "material not found");
        return material;
    }

    private Map<String, Object> requireMaterial(String materialId) {
        Map<String, Object> material = materials.get(materialId);
        if (material == null) throw new MaterialException(404, 43700, "material not found");
        return material;
    }

    private Map<String, Object> requireMine(AuthUser user, String materialId) {
        Map<String, Object> material = materials.get(materialId);
        if (material == null || !user.id().equals(material.get("authorUserId"))) throw new MaterialException(404, 43700, "material not found");
        return material;
    }

    private Map<String, Object> publicSummary(Map<String, Object> material) {
        Map<String, Object> view = new LinkedHashMap<>();
        copy(view, material, "materialId", "slug", "kind", "status", "title", "summary", "tags", "publishedAt", "updatedAt");
        view.put("coverAsset", publicAsset(assets.get(str(material.get("coverAssetId")))));
        view.put("category", categoryPublicView(categories.get(str(material.get("categoryId")))));
        view.put("author", material.get("author"));
        view.put("license", material.get("license"));
        view.put("featured", "FEATURED".equals(material.get("status")));
        return view;
    }

    private Map<String, Object> publicDetail(Map<String, Object> material) {
        Map<String, Object> view = publicSummary(material);
        copy(view, material, "description", "visibleFrom", "visibleUntil", "createdAt");
        view.put("assets", materialAssets(material).stream().filter(asset -> "SAFE".equals(asset.get("status"))).map(this::publicAsset).toList());
        return view;
    }

    private Map<String, Object> myView(Map<String, Object> material) {
        Map<String, Object> view = adminView(material);
        view.remove("adminNote");
        view.remove("auditsTotal");
        return view;
    }

    private Map<String, Object> adminView(Map<String, Object> material) {
        Map<String, Object> view = snapshot(material);
        view.put("assets", materialAssets(material).stream().map(this::adminAsset).toList());
        view.put("category", categories.get(str(material.get("categoryId"))));
        view.put("auditsTotal", audits.stream().filter(audit -> Objects.equals(audit.get("targetId"), material.get("materialId"))).count());
        return view;
    }

    private Map<String, Object> publicAsset(Map<String, Object> asset) {
        if (asset == null) return null;
        Map<String, Object> view = new LinkedHashMap<>();
        copy(view, asset, "assetId", "materialId", "uploadSessionId", "provider", "status", "displayName", "extension", "mimeType", "fileSizeBytes", "checksumSha256", "width", "height", "durationSeconds", "publicAssetUrl", "createdAt", "updatedAt");
        return view;
    }

    private Map<String, Object> adminAsset(Map<String, Object> asset) {
        Map<String, Object> view = publicAsset(asset);
        view.put("ownerUserId", asset.get("ownerUserId"));
        view.put("securityRejectReason", asset.get("securityRejectReason"));
        return view;
    }

    private Map<String, Object> categoryPublicView(Map<String, Object> category) {
        if (category == null || bool(category.get("archived")) || !bool(category.get("enabled"))) return null;
        return snapshot(category);
    }

    private List<Map<String, Object>> materialAssets(Map<String, Object> material) {
        return list(material.get("assetIds")).stream()
                .map(assets::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private void validateMaterialReady(Map<String, Object> material) {
        if (material.get("license") == null) throw new MaterialException(400, 43713, "license missing");
        if (materialAssets(material).isEmpty() || materialAssets(material).stream().anyMatch(asset -> !"SAFE".equals(asset.get("status")))) {
            throw new MaterialException(409, 43715, "asset unsafe");
        }
    }

    private void validateProfile(AuthUser user) {
        switch (user.profileMode()) {
            case "UNAVAILABLE" -> throw new MaterialException(502, 46710, "profile unavailable");
            case "TIMEOUT" -> throw new MaterialException(504, 46711, "profile timeout");
            case "BAD" -> throw new MaterialException(502, 46712, "profile incompatible");
            case "SUSPENDED" -> throw new MaterialException(403, 42001, "member suspended");
            default -> {
            }
        }
    }

    private void requireNotification(Map<String, Object> material, HttpServletRequest request) {
        if (material.get("authorUserId") == null) throw new MaterialException(502, 46722, "notification recipient missing");
        String mode = request.getHeader("X-Test-Notification-Mode");
        if ("unavailable".equals(mode)) throw new MaterialException(502, 46720, "notification unavailable");
        if ("timeout".equals(mode)) throw new MaterialException(504, 46721, "notification timeout");
    }

    private void checkStorage(HttpServletRequest request) {
        String mode = request.getHeader("X-Test-Storage-Mode");
        if ("unavailable".equals(mode)) throw new MaterialException(502, 46730, "storage unavailable");
        if ("timeout".equals(mode)) throw new MaterialException(504, 46731, "storage timeout");
    }

    private void validateUploadSessionBody(Map<String, Object> body) {
        String kind = str(body.get("kind"));
        if (!KINDS.contains(kind)) throw new MaterialException(400, 40001, "invalid kind");
        List<String> names = list(body.get("expectedFileNames"));
        if (names.isEmpty() || names.size() > 10) throw new MaterialException(400, 40001, "invalid file count");
        if (str(body.get("checksumSha256")) == null || !str(body.get("checksumSha256")).matches("[a-f0-9]{64}")) throw new MaterialException(400, 40001, "invalid checksum");
        if (intValue(body.get("maxFileSizeBytes"), 0) < 1 || intValue(body.get("maxFileSizeBytes"), 0) > 10_485_760) throw new MaterialException(400, 40001, "invalid size");
        for (String name : names) validateFileName(name);
        for (String mime : list(body.get("expectedMimeTypes"))) {
            if (!allowedMimeTypes(kind).contains(mime)) throw new MaterialException(400, 43712, "invalid mime");
        }
    }

    private void validateUploadedFile(Map<String, Object> session, Map<String, Object> file) {
        String displayName = str(file.get("displayName"));
        validateFileName(displayName);
        if (intValue(file.get("fileSizeBytes"), 0) > intValue(session.get("maxFileSizeBytes"), 0)) throw new MaterialException(400, 43712, "file too large");
        String extension = lower(str(file.get("extension")));
        if (!displayName.toLowerCase().endsWith("." + extension)) throw new MaterialException(400, 43712, "extension mismatch");
        if (!list(session.get("allowedExtensions")).contains(extension)) throw new MaterialException(400, 43712, "extension invalid");
        if (!list(session.get("allowedMimeTypes")).contains(str(file.get("mimeType")))) throw new MaterialException(400, 43712, "mime invalid");
        if (!Objects.equals(file.get("checksumSha256"), session.get("checksumSha256"))) throw new MaterialException(400, 43712, "checksum mismatch");
        if ("png".equals(extension) && !"PNG".equals(file.get("signature"))) throw new MaterialException(400, 43712, "signature mismatch");
        if ("mp4".equals(extension) && !"MP4".equals(file.get("signature"))) throw new MaterialException(400, 43712, "signature mismatch");
        if ("pdf".equals(extension) && !"PDF".equals(file.get("signature"))) throw new MaterialException(400, 43712, "signature mismatch");
    }

    private void validateFileName(String name) {
        if (name == null || name.isBlank() || name.contains("\u0000") || name.contains("../") || name.contains("..\\") || name.contains("/") || name.contains("\\")) {
            throw new MaterialException(400, 43712, "invalid file name");
        }
        String lower = name.toLowerCase();
        if (lower.endsWith(".php.png") || lower.endsWith(".exe") || lower.endsWith(".bat") || lower.endsWith(".cmd") || lower.endsWith(".js") || lower.endsWith(".zip")) {
            throw new MaterialException(400, 43712, "dangerous file");
        }
        String extension = lower.substring(lower.lastIndexOf('.') + 1);
        if (!SAFE_EXTENSIONS.contains(extension)) throw new MaterialException(400, 43712, "invalid extension");
    }

    private void validateSubmissionBody(Map<String, Object> body, boolean create) {
        if (create && (str(body.get("kind")) == null || str(body.get("slug")) == null || str(body.get("title")) == null)) throw new MaterialException(400, 40001, "invalid submission");
        if (body.containsKey("kind") && !KINDS.contains(str(body.get("kind")))) throw new MaterialException(400, 40001, "invalid kind");
        if (body.containsKey("slug") && !str(body.get("slug")).matches("[a-z0-9/-]{3,120}")) throw new MaterialException(400, 40001, "invalid slug");
        if (body.containsKey("visibleUntil") && "2020-01-01T00:00:00Z".equals(body.get("visibleUntil"))) throw new MaterialException(400, 40001, "invalid time");
        license(body);
    }

    private Map<String, Object> license(Map<String, Object> body) {
        Map<String, Object> license = map(body.get("license"));
        if (!Boolean.TRUE.equals(license.get("authorConfirmed"))) throw new MaterialException(400, 43713, "license missing");
        if ("AUTHORIZED_REPOST".equals(license.get("licenseType")) && str(license.get("sourceUrl")) == null) {
            throw new MaterialException(400, 43713, "source url required");
        }
        return new LinkedHashMap<>(license);
    }

    private void validateCategoryBody(Map<String, Object> body, boolean create) {
        validateReason(body);
        if (create && (str(body.get("name")) == null || str(body.get("slug")) == null)) throw new MaterialException(400, 40001, "invalid category");
        if (body.containsKey("slug") && !str(body.get("slug")).matches("[a-z0-9-]{2,80}")) throw new MaterialException(400, 40001, "invalid slug");
        if (body.containsKey("name") && str(body.get("name")).length() < 2) throw new MaterialException(400, 40001, "invalid name");
        if (body.containsKey("kind") && !KINDS.contains(str(body.get("kind")))) throw new MaterialException(400, 40001, "invalid kind");
    }

    private void validateReason(Map<String, Object> body) {
        if (str(body.get("reason")) == null || str(body.get("reason")).isBlank()) throw new MaterialException(400, 40001, "reason required");
    }

    private void requireReview(Map<String, Object> body) {
        if (str(body.get("reviewOpinion")) == null || str(body.get("reviewOpinion")).isBlank()) throw new MaterialException(400, 40001, "review required");
    }

    private Object replay(String key, Map<String, Object> body) {
        if (key == null || key.endsWith(":null")) return null;
        String fingerprint = stable(body);
        Map<String, Object> existing = idem.get(key);
        if (existing == null) return null;
        if (!fingerprint.equals(existing.get("fingerprint"))) throw new MaterialException(409, 43714, "idempotency conflict");
        return existing.get("value");
    }

    private void remember(String key, Map<String, Object> body, Object value) {
        if (key != null && !key.endsWith(":null")) idem.put(key, mapOf("fingerprint", stable(body), "value", value));
    }

    private String stable(Object value) {
        if (value instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Object key : new TreeSet<>(map.keySet())) {
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

    private Page page(Map<String, String> query) {
        int page = intQuery(query, "page", 1);
        int pageSize = intQuery(query, "pageSize", 20);
        if (page < 1 || pageSize < 1 || pageSize > 100) throw new MaterialException(400, 40002, "invalid page");
        return new Page(page, pageSize);
    }

    private Map<String, Object> page(List<Map<String, Object>> items, Page page) {
        int from = Math.min(items.size(), (page.page() - 1) * page.pageSize());
        int to = Math.min(items.size(), from + page.pageSize());
        return mapOf("items", items.subList(from, to), "page", page.page(), "pageSize", page.pageSize(), "total", items.size());
    }

    private void validateSort(String sort, Set<String> allowed, String ignoredDefaultSort) {
        if (sort != null && !allowed.contains(sort)) throw new MaterialException(400, 40003, "invalid sort");
    }

    private Comparator<Map<String, Object>> publicComparator(String sort) {
        if ("title_asc".equals(sort)) return Comparator.comparing(item -> str(item.get("title")));
        if ("featured_desc".equals(sort)) return Comparator.comparing((Map<String, Object> item) -> "FEATURED".equals(item.get("status"))).reversed();
        return Comparator.comparing((Map<String, Object> item) -> str(item.get("publishedAt")), Comparator.reverseOrder());
    }

    private Comparator<Map<String, Object>> adminComparator(String sort) {
        if ("submittedAt_desc".equals(sort)) return Comparator.comparing((Map<String, Object> item) -> str(item.get("submittedAt")), Comparator.nullsLast(Comparator.reverseOrder()));
        if ("publishedAt_desc".equals(sort)) return Comparator.comparing((Map<String, Object> item) -> str(item.get("publishedAt")), Comparator.nullsLast(Comparator.reverseOrder()));
        if ("title_asc".equals(sort)) return Comparator.comparing(item -> str(item.get("title")));
        return Comparator.comparing((Map<String, Object> item) -> str(item.get("updatedAt")), Comparator.reverseOrder());
    }

    private Comparator<Map<String, Object>> assetComparator(String sort) {
        if ("createdAt_desc".equals(sort)) return Comparator.comparing((Map<String, Object> asset) -> str(asset.get("createdAt")), Comparator.reverseOrder());
        if ("size_desc".equals(sort)) return Comparator.comparingInt((Map<String, Object> asset) -> intValue(asset.get("fileSizeBytes"), 0)).reversed();
        return Comparator.comparing(asset -> str(asset.get("createdAt")));
    }

    private Comparator<Map<String, Object>> auditComparator(String sort) {
        if ("createdAt_asc".equals(sort)) return Comparator.comparing(audit -> str(audit.get("createdAt")));
        return Comparator.comparing((Map<String, Object> audit) -> str(audit.get("createdAt")), Comparator.reverseOrder());
    }

    private int intQuery(Map<String, String> query, String key, int fallback) {
        try {
            return query.containsKey(key) ? Integer.parseInt(query.get(key)) : fallback;
        } catch (Exception ex) {
            throw new MaterialException(400, 40002, "invalid page");
        }
    }

    private Boolean boolQuery(String value, boolean fallback) {
        if (value == null) return fallback;
        if ("true".equals(value) || "false".equals(value)) return Boolean.parseBoolean(value);
        throw new MaterialException(400, 40001, "invalid boolean");
    }

    private Instant parseInstant(String value) {
        if (value == null) return null;
        try {
            return Instant.parse(value);
        } catch (Exception ex) {
            throw new MaterialException(400, 40001, "invalid time");
        }
    }

    private void failAudit(HttpServletRequest request) {
        if ("true".equals(request.getHeader("X-Test-Fail-Audit"))) throw new MaterialException(500, 51701, "audit failed");
    }

    private Map<String, Object> addCategory(String id, String name, String slug, boolean enabled, boolean archived) {
        Map<String, Object> category = mapOf("categoryId", id, "name", name, "slug", slug, "description", null, "sortOrder", 10, "enabled", enabled, "archived", archived, "kind", "IMAGE", "createdAt", NOW, "updatedAt", NOW, "archivedAt", archived ? NOW : null);
        categories.put(id, category);
        return category;
    }

    private void addAsset(Map<String, Object> asset) {
        assets.put(str(asset.get("assetId")), asset);
    }

    private Map<String, Object> asset(String assetId, String materialId, String owner, String status, String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return mapOf("assetId", assetId, "materialId", materialId, "uploadSessionId", "seed-session", "provider", "LOCAL_STUB", "status", status, "displayName", fileName, "extension", extension, "mimeType", extension.equals("png") ? "image/png" : "application/octet-stream", "fileSizeBytes", 2048, "checksumSha256", "a".repeat(64), "width", 1920, "height", 1080, "durationSeconds", null, "publicAssetUrl", "SAFE".equals(status) ? "/api/v1/materials/assets/" + assetId : null, "securityRejectReason", "SAFE".equals(status) ? null : "unsafe file", "ownerUserId", owner, "createdAt", NOW, "updatedAt", NOW);
    }

    private String extensionOf(String fileName) {
        String value = str(fileName);
        int index = value == null ? -1 : value.lastIndexOf('.');
        return index < 0 ? "" : value.substring(index + 1).toLowerCase();
    }

    private Set<String> allowedMimeTypes(String kind) {
        return switch (kind) {
            case "IMAGE" -> IMAGE_MIMES;
            case "VIDEO" -> VIDEO_MIMES;
            case "DOCUMENT_ATTACHMENT" -> DOCUMENT_MIMES;
            case "BUILD_SCREENSHOT", "PROJECT_RECORD", "EVENT_MEMORY" -> IMAGE_VIDEO_MIMES;
            default -> ALL_SAFE_MIMES;
        };
    }

    private void addMaterial(String id, String slug, String kind, String status, String visibility, String categoryId, List<String> tags, List<String> assetIds, boolean hasLicense, String authorUserId, boolean allowFeature) {
        Map<String, Object> material = baseMaterial(id, slug, kind, status, visibility, categoryId, tags, assetIds, authorUserId, hasLicense ? license(allowFeature) : null);
        materials.put(id, material);
    }

    private Map<String, Object> baseMaterial(String id, String slug, String kind, String status, String visibility, String categoryId, List<String> tags, List<String> assetIds, String authorUserId, Map<String, Object> license) {
        return mapOf("materialId", id, "slug", slug, "kind", kind, "status", status, "visibility", visibility, "title", title(slug), "summary", "summary " + slug, "description", "description " + slug, "categoryId", categoryId, "tags", tags, "assetIds", assetIds, "coverAssetId", assetIds.isEmpty() ? null : assetIds.get(0), "authorUserId", authorUserId, "author", authorUserId == null ? null : author(authorUserId), "license", license, "featured", "FEATURED".equals(status), "adminNote", "internal note", "reviewOpinion", null, "publicComment", null, "notificationStatus", null, "submittedAt", "PENDING_REVIEW".equals(status) ? NOW : null, "reviewedAt", null, "publishedAt", Set.of("APPROVED", "FEATURED").contains(status) ? NOW : null, "visibleFrom", "2026-05-20T00:00:00Z", "visibleUntil", "2026-12-31T00:00:00Z", "createdBy", authorUserId, "updatedBy", authorUserId, "createdAt", NOW, "updatedAt", NOW, "deletedAt", "DELETED".equals(status) ? NOW : null);
    }

    private Map<String, Object> author(String userId) {
        String display = "other".equals(userId) ? "Other Member" : "Steve";
        return mapOf("userId", userId, "memberId", "member-" + userId, "displayName", display, "avatarUrl", null, "minecraftId", display, "memberStatus", "ACTIVE", "profileSnapshotAt", NOW);
    }

    private Map<String, Object> license(boolean allowFeature) {
        return mapOf("licenseType", "ORIGINAL", "authorConfirmed", true, "allowHomepageFeature", allowFeature, "allowDerivativeUse", true, "sourceUrl", null, "creditText", "Steve");
    }

    private void copySubmissionFields(Map<String, Object> from, Map<String, Object> to) {
        for (String key : List.of("kind", "slug", "title", "summary", "description", "categoryId", "tags", "coverAssetId", "visibility", "visibleFrom", "visibleUntil")) {
            if (from.containsKey(key)) to.put(key, from.get(key));
        }
        if (from.containsKey("assetIds")) to.put("assetIds", list(from.get("assetIds")));
        if (from.containsKey("license")) to.put("license", map(from.get("license")));
    }

    private boolean slugExists(String slug) {
        return materials.values().stream().anyMatch(item -> slug.equals(item.get("slug")) && !"DELETED".equals(item.get("status")));
    }

    private boolean categorySlugExists(String slug) {
        return categories.values().stream().anyMatch(item -> slug.equals(item.get("slug")) && !bool(item.get("archived")));
    }

    private Map<String, Object> safeSession(Map<String, Object> session) {
        Map<String, Object> safe = snapshot(session);
        safe.remove("uploadTicket");
        return safe;
    }

    private Map<String, Object> snapshot(Map<String, Object> value) {
        return new LinkedHashMap<>(value);
    }

    private void audit(AuthUser actor, String targetId, String action, String result, Map<String, Object> before, Map<String, Object> after, String reason) {
        String actorId = actor == null ? "system" : actor.id();
        String actorRole = actor == null || actor.roles().isEmpty() ? "SYSTEM" : actor.roles().iterator().next();
        audits.add(mapOf("id", "audit-" + (++idSeq), "requestId", MaterialController.currentRequestId(), "actorUserId", actorId, "actorRole", actorRole, "actorPermissions", List.of(), "sourceIp", "127.0.0.1", "targetType", "MATERIAL", "targetId", targetId, "action", action, "riskLevel", "MEDIUM", "reason", reason == null ? "contract" : reason, "paramsSummary", Map.of("safe", true), "beforeState", before, "afterState", after, "result", result, "failureReason", null, "createdAt", NOW));
    }

    private String title(String slug) {
        StringBuilder builder = new StringBuilder();
        for (String part : slug.split("-")) {
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private static void copy(Map<String, Object> target, Map<String, Object> source, String... keys) {
        for (String key : keys) target.put(key, source.get(key));
    }

    @SafeVarargs
    private static Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) map.put(values[i].toString(), values[i + 1]);
        return map;
    }

    @SuppressWarnings("unchecked")
    private static List<String> list(Object value) {
        if (value instanceof List<?> list) return (List<String>) list;
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> listOfMaps(Object value) {
        if (value instanceof List<?> list) return (List<Map<String, Object>>) list;
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> map) return (Map<String, Object>) map;
        throw new MaterialException(400, 40001, "invalid object");
    }

    private static String str(Object value) {
        return value == null ? null : value.toString();
    }

    private static String lower(String value) {
        return value == null ? null : value.toLowerCase();
    }

    private static boolean bool(Object value) {
        return Boolean.TRUE.equals(value);
    }

    private static boolean boolValue(Object value) {
        if (value instanceof Boolean b) return b;
        if ("true".equals(value) || "false".equals(value)) return Boolean.parseBoolean(value.toString());
        throw new MaterialException(400, 40001, "invalid boolean");
    }

    private static int intValue(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(value.toString());
    }
}

record Page(int page, int pageSize) {
}

record AuthUser(String id, Set<String> roles, String profileMode) {
}

class TestMaterialAuthProvider {
    AuthUser requireAuthenticated(String authorization) {
        AuthUser trustedActor = trustedGatewayActor();
        if (trustedActor != null) return trustedActor;
        if (authorization == null || authorization.isBlank()) throw new MaterialException(401, 41000, "not logged in");
        if (!authorization.startsWith("Bearer ")) throw new MaterialException(401, 41003, "invalid token");
        String token = authorization.substring("Bearer ".length());
        return switch (token) {
            case "auth-unavailable-token" -> throw new MaterialException(502, 46700, "auth unavailable");
            case "auth-timeout-token" -> throw new MaterialException(504, 46701, "auth timeout");
            case "auth-bad-token" -> throw new MaterialException(502, 46702, "auth incompatible");
            case "admin-token" -> new AuthUser("admin", Set.of("ADMIN"), "ACTIVE");
            case "owner-token" -> new AuthUser("owner", Set.of("OWNER"), "ACTIVE");
            case "helper-token" -> new AuthUser("helper", Set.of("HELPER"), "ACTIVE");
            case "member-token" -> new AuthUser("member", Set.of("USER"), "ACTIVE");
            case "profile-unavailable-token" -> new AuthUser("member", Set.of("USER"), "UNAVAILABLE");
            case "profile-timeout-token" -> new AuthUser("member", Set.of("USER"), "TIMEOUT");
            case "profile-bad-token" -> new AuthUser("member", Set.of("USER"), "BAD");
            case "suspended-member-token" -> new AuthUser("member", Set.of("USER"), "SUSPENDED");
            default -> new AuthUser("user", Set.of("USER"), "NONE");
        };
    }

    AuthUser requireAny(String authorization, String... roles) {
        AuthUser user = requireAuthenticated(authorization);
        Set<String> allowed = new LinkedHashSet<>(List.of(roles));
        if (user.roles().stream().noneMatch(allowed::contains)) {
            throw new MaterialException(403, 42001, "role permission denied");
        }
        return user;
    }

    private AuthUser trustedGatewayActor() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs == null ? null : attrs.getRequest();
        if (request == null) return null;
        String internalRequestId = request.getHeader("X-Gateway-Internal-Request-Id");
        String currentRequestId = MaterialController.currentRequestId();
        if (internalRequestId == null || !internalRequestId.equals(currentRequestId)) return null;
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
class MaterialRequestIdFilter extends OncePerRequestFilter {
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
class MaterialExceptionHandler {
    @ExceptionHandler(MaterialException.class)
    ResponseEntity<Map<String, Object>> handle(MaterialException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.code);
        body.put("message", ex.getMessage());
        body.put("data", null);
        body.put("errors", List.of());
        body.put("requestId", MaterialController.currentRequestId());
        return ResponseEntity.status(ex.httpStatus).body(body);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> unexpected(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 51700);
        body.put("message", "material internal error");
        body.put("data", null);
        body.put("requestId", MaterialController.currentRequestId());
        return ResponseEntity.status(500).body(body);
    }
}

class MaterialException extends RuntimeException {
    final int httpStatus;
    final int code;

    MaterialException(int httpStatus, int code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }
}
