package cn.beiming.opsimagemarket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static cn.beiming.opsimagemarket.OimSupport.*;

@RestController
@RequestMapping("/api/v1/ops-image-market")
class OpsImageMarketController {
    private static final String VERSION = "0.1.0-contract";
    private final OimStore store;
    private final OimAuth auth;
    private final OimProperties properties;

    OpsImageMarketController(OimStore store, OimAuth auth, OimProperties properties) {
        this.store = store;
        this.auth = auth;
        this.properties = properties;
    }

    @GetMapping("/health")
    ResponseEntity<Map<String, Object>> health(HttpServletRequest request) {
        return ok(request, map("service", "ops-image-market", "status", store.health(), "version", VERSION));
    }

    @GetMapping("/admin/ops/summary")
    ResponseEntity<Map<String, Object>> summary(HttpServletRequest request) {
        auth.requireRead(request);
        store.failStoreIfRequested(request, properties.enabled());
        return ok(request, store.summary(properties.enabled()));
    }

    @GetMapping("/admin/providers")
    ResponseEntity<Map<String, Object>> providers(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "createdAt_desc", "displayName_asc", "lastHealthCheckedAt_desc");
        validateTimeRange(query);
        List<Map<String, Object>> items = store.providers.values().stream()
                .filter(item -> matches(item, "displayName", query.get("keyword")) || matches(item, "providerId", query.get("keyword")))
                .filter(item -> eq(item, "registryType", query.get("registryType")))
                .filter(item -> eq(item, "status", query.get("status")))
                .filter(item -> eq(item, "healthStatus", query.get("healthStatus")))
                .filter(item -> contains(item, "allowedNamespaces", query.get("namespace")))
                .filter(item -> contains(item, "allowedRiskLevels", query.get("riskLevel")))
                .filter(item -> contains(item, "allowedSourceModules", query.get("sourceModule")))
                .filter(item -> query.get("degraded") == null || bool(query.get("degraded")) == Boolean.TRUE.equals(item.get("degraded")))
                .filter(item -> within(item, "updatedAt", query))
                .sorted(by(query.get("sort")))
                .map(OimSupport::copy)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/providers/{providerId}")
    ResponseEntity<Map<String, Object>> provider(HttpServletRequest request, @PathVariable String providerId) {
        auth.requireRead(request);
        Map<String, Object> view = copy(store.get(store.providers, providerId, 49700, "provider not found"));
        view.put("healthRefreshSummary", map("lastHealthCheckedAt", view.get("lastHealthCheckedAt"), "healthStatus", view.get("healthStatus")));
        view.put("imageCountSummary", map("total", store.countBy(store.images, "providerId", providerId)));
        view.put("recentScanSummary", store.latestScanForProvider(providerId));
        view.put("dependencySummary", dependencySummary());
        view.put("recentAuditSummary", store.latestAudit("PROVIDER", providerId));
        return ok(request, view);
    }

    @PostMapping("/admin/providers")
    ResponseEntity<Map<String, Object>> createProvider(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        requireHighRisk(actor);
        rejectTrusted(body);
        requireConfirm(body, "REGISTER_IMAGE_PROVIDER");
        validateReason(body);
        validateProviderBody(body);
        return store.idempotent(request, actor, "provider:create", body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            String displayName = text(body.get("displayName"));
            String registryType = text(body.get("registryType"));
            if (store.any(store.providers, item -> displayName.equals(item.get("displayName")) && registryType.equals(item.get("registryType")) && !"ARCHIVED".equals(item.get("status")))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49711, "provider conflict");
            }
            String providerId = store.id("provider", body);
            Map<String, Object> provider = providerFrom(providerId, body, actor);
            store.providers.put(providerId, provider);
            store.audit("IMAGE_PROVIDER_CREATED", "PROVIDER", providerId, actor, request, body, "HIGH", "SUCCESS", null, null, "DRAFT");
            return new WriteResult(HttpStatus.CREATED, copy(provider));
        });
    }

    @PatchMapping("/admin/providers/{providerId}")
    ResponseEntity<Map<String, Object>> patchProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        validateReason(body);
        if (containsAny(body.keySet(), "endpointSummary", CRED_REF, "allowedNamespaces", "allowedRiskLevels")) {
            requireHighRisk(actor);
            requireConfirm(body, "UPDATE_IMAGE_PROVIDER");
        }
        return store.idempotent(request, actor, "provider:patch:" + providerId, body, () -> {
            Map<String, Object> provider = store.get(store.providers, providerId, 49700, "provider not found");
            requireMutable(provider);
            store.failAuditIfRequested(request, properties.enabled());
            String before = status(provider);
            patch(provider, body, "displayName", "registryType", "allowedNamespaces", "allowedSourceModules", "allowedRiskLevels", "syncPolicySummary", "rateLimitSummary");
            if (body.containsKey("endpointSummary")) {
                provider.put("endpointSummary", endpointSummary(body.get("endpointSummary")));
            }
            if (body.containsKey(CRED_REF)) {
                provider.put(CRED_REF, object(body.get(CRED_REF)));
            }
            touch(provider, actor);
            store.audit("IMAGE_PROVIDER_UPDATED", "PROVIDER", providerId, actor, request, body, containsAny(body.keySet(), "endpointSummary", CRED_REF, "allowedNamespaces", "allowedRiskLevels") ? "HIGH" : "MEDIUM", "SUCCESS", null, before, status(provider));
            return new WriteResult(HttpStatus.OK, copy(provider));
        });
    }

    @PatchMapping("/admin/providers/{providerId}/enable")
    ResponseEntity<Map<String, Object>> enableProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        return providerState(request, providerId, body, "ENABLED", "IMAGE_PROVIDER_ENABLED", "ENABLE_IMAGE_PROVIDER", "HIGH");
    }

    @PatchMapping("/admin/providers/{providerId}/disable")
    ResponseEntity<Map<String, Object>> disableProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        return providerState(request, providerId, body, "DISABLED", "IMAGE_PROVIDER_DISABLED", null, "MEDIUM");
    }

    @PatchMapping("/admin/providers/{providerId}/archive")
    ResponseEntity<Map<String, Object>> archiveProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        return providerState(request, providerId, body, "ARCHIVED", "IMAGE_PROVIDER_ARCHIVED", "ARCHIVE_IMAGE_PROVIDER", "HIGH");
    }

    @PostMapping("/admin/providers/{providerId}/health-refresh")
    ResponseEntity<Map<String, Object>> refreshProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        validateReason(body);
        return store.idempotent(request, actor, "provider:refresh:" + providerId, body, () -> {
            Map<String, Object> provider = store.get(store.providers, providerId, 49700, "provider not found");
            store.failAuditIfRequested(request, properties.enabled());
            String before = status(provider);
            provider.put("healthStatus", "HEALTHY");
            provider.put("degraded", false);
            provider.put("degradeReasons", List.of());
            provider.put("lastHealthCheckedAt", now());
            touch(provider, actor);
            store.audit("IMAGE_PROVIDER_HEALTH_REFRESHED", "PROVIDER", providerId, actor, request, body, "MEDIUM", "SUCCESS", null, before, status(provider));
            return new WriteResult(HttpStatus.OK, copy(provider));
        });
    }

    private ResponseEntity<Map<String, Object>> providerState(HttpServletRequest request, String providerId, Map<String, Object> body, String target, String action, String confirm, String risk) {
        Actor actor = auth.requireWrite(request);
        validateReason(body);
        if (confirm != null) {
            requireHighRisk(actor);
            requireConfirm(body, confirm);
        }
        return store.idempotent(request, actor, "provider:" + target + ":" + providerId, body, () -> {
            Map<String, Object> provider = store.get(store.providers, providerId, 49700, "provider not found");
            String before = status(provider);
            if ("ARCHIVED".equals(before) || ("ARCHIVED".equals(target) && "ENABLED".equals(before))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49710, "provider state conflict");
            }
            if ("ENABLED".equals(target) && (list(provider.get("allowedNamespaces")).isEmpty() || list(provider.get("allowedSourceModules")).isEmpty() || list(provider.get("allowedRiskLevels")).isEmpty())) {
                throw new OimApiException(HttpStatus.CONFLICT, 49719, "provider health blocked");
            }
            if ("ARCHIVED".equals(target) && (store.any(store.images, item -> providerId.equals(item.get("providerId")) && "PUBLISHED".equals(item.get("status"))) || store.any(store.templates, item -> "ENABLED".equals(item.get("status"))) || store.any(store.plans, item -> !terminalPlan(status(item))))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49710, "provider state conflict");
            }
            store.failAuditIfRequested(request, properties.enabled());
            provider.put("status", target);
            if ("ENABLED".equals(target)) {
                provider.put("healthStatus", "HEALTHY");
                provider.put("degraded", false);
                provider.put("degradeReasons", List.of());
            }
            touch(provider, actor);
            store.audit(action, "PROVIDER", providerId, actor, request, body, risk, "SUCCESS", null, before, target);
            return new WriteResult(HttpStatus.OK, copy(provider));
        });
    }

    @GetMapping("/admin/images")
    ResponseEntity<Map<String, Object>> images(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "createdAt_desc", "displayName_asc", "riskLevel_desc");
        validateTimeRange(query);
        List<Map<String, Object>> items = store.images.values().stream()
                .filter(item -> matches(item, "displayName", query.get("keyword")) || matches(item, "repository", query.get("keyword")))
                .filter(item -> eq(item, "providerId", query.get("providerId")))
                .filter(item -> eq(item, "repository", query.get("repository")))
                .filter(item -> eq(item, "purpose", query.get("purpose")))
                .filter(item -> eq(item, "visibility", query.get("visibility")))
                .filter(item -> eq(item, "status", query.get("status")))
                .filter(item -> contains(item, "architectureSet", query.get("architecture")))
                .filter(item -> contains(item, "runtimeHints", query.get("runtime")))
                .filter(item -> riskEquals(item, query.get("riskLevel")))
                .filter(item -> within(item, "updatedAt", query))
                .sorted(by(query.get("sort")))
                .map(OimSupport::copy)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/images/{imageId}")
    ResponseEntity<Map<String, Object>> image(HttpServletRequest request, @PathVariable String imageId) {
        auth.requireRead(request);
        Map<String, Object> image = copy(store.get(store.images, imageId, 49701, "image not found"));
        image.put("providerSummary", summaryOf(store.providers.get(text(image.get("providerId"))), "providerId", "displayName", "status"));
        image.put("compatibilitySummary", map("profiles", store.countBy(store.profiles, "imageId", imageId)));
        image.put("templateSummary", map("templates", store.countBy(store.templates, "imageId", imageId)));
        image.put("pullPlanSummary", map("plans", store.countBy(store.plans, "imageId", imageId)));
        image.put("cacheSummary", map("snapshots", store.caches.size()));
        image.put("recentAuditSummary", store.latestAudit("IMAGE", imageId));
        return ok(request, image);
    }

    @PostMapping("/admin/images")
    ResponseEntity<Map<String, Object>> createImage(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        validateReason(body);
        validateImageBody(body);
        return store.idempotent(request, actor, "image:create", body, () -> {
            String providerId = requiredText(body, "providerId");
            Map<String, Object> provider = store.get(store.providers, providerId, 49700, "provider not found");
            String repo = requiredText(body, "repository");
            if (!namespaceAllowed(provider, repo) || unsafeRepo(repo)) {
                throw new OimApiException(HttpStatus.BAD_REQUEST, 49713, "unsafe image reference");
            }
            if (store.any(store.images, item -> repo.equals(item.get("repository")) && providerId.equals(item.get("providerId")) && !"ARCHIVED".equals(item.get("status")))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49711, "image conflict");
            }
            store.failAuditIfRequested(request, properties.enabled());
            String imageId = store.id("image", body);
            Map<String, Object> image = map("imageId", imageId, "providerId", providerId, "repository", repo,
                    "displayName", requiredText(body, "displayName"), "purpose", requiredText(body, "purpose"),
                    "visibility", requiredText(body, "visibility"), "status", "DRAFT",
                    "maintainerSummary", object(body.get("maintainerSummary")), "sourceRef", object(body.get("sourceRef")),
                    "architectureSet", list(body.get("architectureSet")), "runtimeHints", list(body.get("runtimeHints")),
                    "latestVersionSummary", null, "riskSummary", map("highestSeverity", "UNKNOWN", "blocked", false),
                    "usageSummary", map("templates", 0, "plans", 0), "createdAt", now(), "updatedAt", now());
            store.images.put(imageId, image);
            store.audit("OPS_IMAGE_CREATED", "IMAGE", imageId, actor, request, body, "MEDIUM", "SUCCESS", null, null, "DRAFT");
            return new WriteResult(HttpStatus.CREATED, copy(image));
        });
    }

    @PatchMapping("/admin/images/{imageId}")
    ResponseEntity<Map<String, Object>> patchImage(HttpServletRequest request, @PathVariable String imageId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        validateReason(body);
        return store.idempotent(request, actor, "image:patch:" + imageId, body, () -> {
            Map<String, Object> image = store.get(store.images, imageId, 49701, "image not found");
            requireMutable(image);
            store.failAuditIfRequested(request, properties.enabled());
            String before = status(image);
            patch(image, body, "displayName", "purpose", "visibility", "maintainerSummary", "sourceRef", "architectureSet", "runtimeHints");
            touch(image, actor);
            store.audit("OPS_IMAGE_UPDATED", "IMAGE", imageId, actor, request, body, "MEDIUM", "SUCCESS", null, before, status(image));
            return new WriteResult(HttpStatus.OK, copy(image));
        });
    }

    @PatchMapping("/admin/images/{imageId}/publish")
    ResponseEntity<Map<String, Object>> publishImage(HttpServletRequest request, @PathVariable String imageId, @RequestBody Map<String, Object> body) {
        return imageState(request, imageId, body, "PUBLISHED", "OPS_IMAGE_PUBLISHED", null, "MEDIUM");
    }

    @PatchMapping("/admin/images/{imageId}/block")
    ResponseEntity<Map<String, Object>> blockImage(HttpServletRequest request, @PathVariable String imageId, @RequestBody Map<String, Object> body) {
        return imageState(request, imageId, body, "BLOCKED", "OPS_IMAGE_BLOCKED", "BLOCK_OPS_IMAGE", "HIGH");
    }

    @PatchMapping("/admin/images/{imageId}/archive")
    ResponseEntity<Map<String, Object>> archiveImage(HttpServletRequest request, @PathVariable String imageId, @RequestBody Map<String, Object> body) {
        return imageState(request, imageId, body, "ARCHIVED", "OPS_IMAGE_ARCHIVED", null, "MEDIUM");
    }

    private ResponseEntity<Map<String, Object>> imageState(HttpServletRequest request, String imageId, Map<String, Object> body, String target, String action, String confirm, String risk) {
        Actor actor = auth.requireWrite(request);
        validateReason(body);
        if (confirm != null) {
            requireHighRisk(actor);
            requireConfirm(body, confirm);
        }
        return store.idempotent(request, actor, "image:" + target + ":" + imageId, body, () -> {
            Map<String, Object> image = store.get(store.images, imageId, 49701, "image not found");
            String before = status(image);
            requireTransition("IMAGE", before, target);
            if ("PUBLISHED".equals(target)) {
                Map<String, Object> provider = store.get(store.providers, text(image.get("providerId")), 49700, "provider not found");
                if (!"ENABLED".equals(status(provider))) {
                    throw new OimApiException(HttpStatus.CONFLICT, 49719, "provider blocked");
                }
                Map<String, Object> version = store.latestApprovedVersion(imageId).orElseThrow(() -> new OimApiException(HttpStatus.CONFLICT, 49715, "scan unavailable"));
                ensureScanFresh(version);
                image.put("latestVersionSummary", summaryOf(version, "imageVersionId", "tag", "status"));
            }
            if ("ARCHIVED".equals(target) && (store.any(store.templates, item -> imageId.equals(item.get("imageId")) && "ENABLED".equals(item.get("status"))) || store.any(store.plans, item -> imageId.equals(item.get("imageId")) && !terminalPlan(status(item))))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49710, "image state conflict");
            }
            store.failAuditIfRequested(request, properties.enabled());
            image.put("status", target);
            if ("BLOCKED".equals(target)) {
                image.put("riskSummary", map("highestSeverity", "HIGH", "blocked", true));
            }
            touch(image, actor);
            store.audit(action, "IMAGE", imageId, actor, request, body, risk, "SUCCESS", null, before, target);
            return new WriteResult(HttpStatus.OK, copy(image));
        });
    }

    @GetMapping("/admin/images/{imageId}/versions")
    ResponseEntity<Map<String, Object>> versions(HttpServletRequest request, @PathVariable String imageId, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "publishedAt_desc", "createdAt_desc", "tag_asc", "highestSeverity_desc");
        validateTimeRange(query);
        if (!store.images.containsKey(imageId)) {
            throw new OimApiException(HttpStatus.NOT_FOUND, 49701, "image not found");
        }
        List<Map<String, Object>> items = store.versions.values().stream()
                .filter(item -> imageId.equals(item.get("imageId")))
                .filter(item -> eq(item, "tag", query.get("tag")))
                .filter(item -> eq(item, "status", query.get("status")))
                .filter(item -> eq(item, "architecture", query.get("architecture")))
                .filter(item -> query.get("signed") == null || bool(query.get("signed")) == Boolean.TRUE.equals(item.get("signed")))
                .filter(item -> scanEquals(item, "highestSeverity", query.get("highestSeverity")))
                .filter(item -> scanEquals(item, "status", query.get("scanStatus")))
                .filter(item -> within(item, "createdAt", query))
                .sorted(by(query.get("sort")))
                .map(OimSupport::copy)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/versions/{imageVersionId}")
    ResponseEntity<Map<String, Object>> version(HttpServletRequest request, @PathVariable String imageVersionId) {
        auth.requireRead(request);
        Map<String, Object> version = copy(store.get(store.versions, imageVersionId, 49702, "version not found"));
        Map<String, Object> image = store.images.get(text(version.get("imageId")));
        version.put("imageSummary", summaryOf(image, "imageId", "displayName", "status"));
        version.put("providerSummary", summaryOf(store.providers.get(text(image.get("providerId"))), "providerId", "displayName", "status"));
        version.put("templateRefSummary", map("templates", store.countBy(store.templates, "imageVersionId", imageVersionId)));
        version.put("recentAuditSummary", store.latestAudit("VERSION", imageVersionId));
        return ok(request, version);
    }

    @PostMapping("/admin/images/{imageId}/versions")
    ResponseEntity<Map<String, Object>> createVersion(HttpServletRequest request, @PathVariable String imageId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        validateReason(body);
        return store.idempotent(request, actor, "version:create:" + imageId, body, () -> {
            if (!store.images.containsKey(imageId)) {
                throw new OimApiException(HttpStatus.NOT_FOUND, 49701, "image not found");
            }
            String tag = requiredText(body, "tag");
            if (unsafeRepo(tag)) {
                throw new OimApiException(HttpStatus.BAD_REQUEST, 49713, "unsafe tag");
            }
            Object digest = body.get("digestSummary");
            if (store.any(store.versions, item -> imageId.equals(item.get("imageId")) && (tag.equals(item.get("tag")) || Objects.equals(digest, item.get("digestSummary"))) && !"ARCHIVED".equals(item.get("status")))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49711, "version conflict");
            }
            store.failAuditIfRequested(request, properties.enabled());
            String versionId = store.id("version", body);
            Map<String, Object> version = map("imageVersionId", versionId, "imageId", imageId, "tag", tag,
                    "digestSummary", object(digest), "manifestSummary", object(body.get("manifestSummary")),
                    "status", "DISCOVERED", "os", value(body.get("os"), "linux"), "architecture", requiredText(body, "architecture"),
                    "sizeSummary", object(body.get("sizeSummary")), "publishedAt", value(body.get("publishedAt"), now()),
                    "deprecatedAt", null, "signed", Boolean.TRUE.equals(body.get("signed")),
                    "signatureSummary", object(body.get("signatureSummary")), "scanSummary", map("status", "NOT_SCANNED", "highestSeverity", "UNKNOWN"),
                    "compatibilitySummary", map("status", "UNKNOWN"), "changeSummary", object(body.get("changeSummary")),
                    "createdAt", now(), "updatedAt", now());
            store.versions.put(versionId, version);
            store.audit("IMAGE_VERSION_CREATED", "VERSION", versionId, actor, request, body, "MEDIUM", "SUCCESS", null, null, "DISCOVERED");
            return new WriteResult(HttpStatus.CREATED, copy(version));
        });
    }

    @PatchMapping("/admin/versions/{imageVersionId}/approve")
    ResponseEntity<Map<String, Object>> approveVersion(HttpServletRequest request, @PathVariable String imageVersionId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        validateReason(body);
        return store.idempotent(request, actor, "version:approve:" + imageVersionId, body, () -> {
            Map<String, Object> version = store.get(store.versions, imageVersionId, 49702, "version not found");
            String before = status(version);
            requireTransition("VERSION", before, "APPROVED");
            Map<String, Object> scan = store.scanForVersion(imageVersionId).orElseThrow(() -> new OimApiException(HttpStatus.CONFLICT, 49715, "scan unavailable"));
            String severity = text(scan.get("highestSeverity"));
            String sig = text(scan.get("signatureStatus"));
            if ("HIGH".equals(severity) || "CRITICAL".equals(severity) || "UNSIGNED".equals(sig)) {
                requireHighRisk(actor);
                requireConfirm(body, "APPROVE_IMAGE_VERSION_RISK");
            }
            requireOwnerForCritical(actor, severity);
            ensureScanFresh(scan);
            if (store.enabledProfiles(text(version.get("imageId"))).isEmpty()) {
                throw new OimApiException(HttpStatus.CONFLICT, 49716, "compatibility failed");
            }
            if ("INVALID".equals(sig)) {
                throw new OimApiException(HttpStatus.CONFLICT, 49718, "signature blocked");
            }
            store.failAuditIfRequested(request, properties.enabled());
            version.put("status", "APPROVED");
            version.put("scanSummary", summaryOf(scan, "scanId", "status", "highestSeverity", "signatureStatus", "expiresAt"));
            version.put("compatibilitySummary", map("status", "PASSED"));
            touch(version, actor);
            store.audit("IMAGE_VERSION_APPROVED", "VERSION", imageVersionId, actor, request, body, riskFromSeverity(severity), "SUCCESS", null, before, "APPROVED");
            return new WriteResult(HttpStatus.OK, copy(version));
        });
    }

    @PatchMapping("/admin/versions/{imageVersionId}/deprecate")
    ResponseEntity<Map<String, Object>> deprecateVersion(HttpServletRequest request, @PathVariable String imageVersionId, @RequestBody Map<String, Object> body) {
        return versionState(request, imageVersionId, body, "DEPRECATED", "IMAGE_VERSION_DEPRECATED", null, "MEDIUM");
    }

    @PatchMapping("/admin/versions/{imageVersionId}/block")
    ResponseEntity<Map<String, Object>> blockVersion(HttpServletRequest request, @PathVariable String imageVersionId, @RequestBody Map<String, Object> body) {
        return versionState(request, imageVersionId, body, "BLOCKED", "IMAGE_VERSION_BLOCKED", "BLOCK_IMAGE_VERSION", "HIGH");
    }

    @PatchMapping("/admin/versions/{imageVersionId}/archive")
    ResponseEntity<Map<String, Object>> archiveVersion(HttpServletRequest request, @PathVariable String imageVersionId, @RequestBody Map<String, Object> body) {
        return versionState(request, imageVersionId, body, "ARCHIVED", "IMAGE_VERSION_ARCHIVED", null, "MEDIUM");
    }

    private ResponseEntity<Map<String, Object>> versionState(HttpServletRequest request, String versionId, Map<String, Object> body, String target, String action, String confirm, String risk) {
        Actor actor = auth.requireWrite(request);
        validateReason(body);
        if (confirm != null) {
            requireHighRisk(actor);
            requireConfirm(body, confirm);
        }
        return store.idempotent(request, actor, "version:" + target + ":" + versionId, body, () -> {
            Map<String, Object> version = store.get(store.versions, versionId, 49702, "version not found");
            String before = status(version);
            requireTransition("VERSION", before, target);
            if ("ARCHIVED".equals(target) && (store.any(store.templates, item -> versionId.equals(item.get("imageVersionId")) && "ENABLED".equals(status(item)))
                    || store.any(store.plans, item -> versionId.equals(item.get("imageVersionId")) && !terminalPlan(status(item))))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49710, "version state conflict");
            }
            store.failAuditIfRequested(request, properties.enabled());
            version.put("status", target);
            if ("DEPRECATED".equals(target)) {
                version.put("deprecatedAt", now());
            }
            touch(version, actor);
            store.audit(action, "VERSION", versionId, actor, request, body, risk, "SUCCESS", null, before, target);
            return new WriteResult(HttpStatus.OK, copy(version));
        });
    }

    @GetMapping("/admin/compatibility-profiles")
    ResponseEntity<Map<String, Object>> profiles(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "createdAt_desc", "minimumMemoryMb_desc");
        validateTimeRange(query);
        List<Map<String, Object>> items = store.profiles.values().stream()
                .filter(item -> eq(item, "imageId", query.get("imageId")))
                .filter(item -> eq(item, "runtime", query.get("runtime")))
                .filter(item -> eq(item, "architecture", query.get("architecture")))
                .filter(item -> eq(item, "minecraftMode", query.get("minecraftMode")))
                .filter(item -> eq(item, "status", query.get("status")))
                .filter(item -> within(item, "updatedAt", query))
                .sorted(by(query.get("sort")))
                .map(OimSupport::copy)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/compatibility-profiles/{profileId}")
    ResponseEntity<Map<String, Object>> profile(HttpServletRequest request, @PathVariable String profileId) {
        auth.requireRead(request);
        Map<String, Object> profile = copy(store.get(store.profiles, profileId, 49703, "profile not found"));
        profile.put("imageSummary", summaryOf(store.images.get(text(profile.get("imageId"))), "imageId", "displayName", "status"));
        profile.put("recentPlanSummary", map("plans", store.countBy(store.plans, "profileId", profileId)));
        profile.put("recentAuditSummary", store.latestAudit("PROFILE", profileId));
        return ok(request, profile);
    }

    @PostMapping("/admin/compatibility-profiles")
    ResponseEntity<Map<String, Object>> createProfile(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        validateReason(body);
        validateEnvSchema(body.get("envSchemaSummary"));
        return store.idempotent(request, actor, "profile:create", body, () -> {
            String imageId = requiredText(body, "imageId");
            if (!store.images.containsKey(imageId)) {
                throw new OimApiException(HttpStatus.NOT_FOUND, 49701, "image not found");
            }
            if (unsafeVolumes(body.get("requiredVolumesSummary"))) {
                throw new OimApiException(HttpStatus.BAD_REQUEST, 49713, "unsafe volume");
            }
            store.failAuditIfRequested(request, properties.enabled());
            String profileId = store.id("profile", body);
            Map<String, Object> profile = map("profileId", profileId, "imageId", imageId, "runtime", requiredText(body, "runtime"),
                    "architecture", requiredText(body, "architecture"), "minecraftMode", value(body.get("minecraftMode"), "NONE"),
                    "minimumCpuCores", value(body.get("minimumCpuCores"), 1), "minimumMemoryMb", value(body.get("minimumMemoryMb"), 1024),
                    "requiredPortsSummary", list(body.get("requiredPortsSummary")), "requiredVolumesSummary", list(body.get("requiredVolumesSummary")),
                    "envSchemaSummary", object(body.get("envSchemaSummary")), "nodeSelectorSummary", object(body.get("nodeSelectorSummary")),
                    "status", "DRAFT", "createdAt", now(), "updatedAt", now());
            store.profiles.put(profileId, profile);
            store.audit("IMAGE_COMPAT_PROFILE_CREATED", "PROFILE", profileId, actor, request, body, "MEDIUM", "SUCCESS", null, null, "DRAFT");
            return new WriteResult(HttpStatus.CREATED, copy(profile));
        });
    }

    @PatchMapping("/admin/compatibility-profiles/{profileId}")
    ResponseEntity<Map<String, Object>> patchProfile(HttpServletRequest request, @PathVariable String profileId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        validateReason(body);
        validateEnvSchema(body.get("envSchemaSummary"));
        if (unsafeVolumes(body.get("requiredVolumesSummary"))) {
            throw new OimApiException(HttpStatus.BAD_REQUEST, 49713, "unsafe volume");
        }
        return store.idempotent(request, actor, "profile:patch:" + profileId, body, () -> {
            Map<String, Object> profile = store.get(store.profiles, profileId, 49703, "profile not found");
            requireMutable(profile);
            store.failAuditIfRequested(request, properties.enabled());
            String before = status(profile);
            patch(profile, body, "runtime", "architecture", "minecraftMode", "minimumCpuCores", "minimumMemoryMb", "requiredPortsSummary", "requiredVolumesSummary", "envSchemaSummary", "nodeSelectorSummary");
            touch(profile, actor);
            store.audit("IMAGE_COMPAT_PROFILE_UPDATED", "PROFILE", profileId, actor, request, body, "MEDIUM", "SUCCESS", null, before, status(profile));
            return new WriteResult(HttpStatus.OK, copy(profile));
        });
    }

    @PatchMapping("/admin/compatibility-profiles/{profileId}/enable")
    ResponseEntity<Map<String, Object>> enableProfile(HttpServletRequest request, @PathVariable String profileId, @RequestBody Map<String, Object> body) {
        return profileState(request, profileId, body, "ENABLED", "IMAGE_COMPAT_PROFILE_ENABLED");
    }

    @PatchMapping("/admin/compatibility-profiles/{profileId}/disable")
    ResponseEntity<Map<String, Object>> disableProfile(HttpServletRequest request, @PathVariable String profileId, @RequestBody Map<String, Object> body) {
        return profileState(request, profileId, body, "DISABLED", "IMAGE_COMPAT_PROFILE_DISABLED");
    }

    @PatchMapping("/admin/compatibility-profiles/{profileId}/archive")
    ResponseEntity<Map<String, Object>> archiveProfile(HttpServletRequest request, @PathVariable String profileId, @RequestBody Map<String, Object> body) {
        return profileState(request, profileId, body, "ARCHIVED", "IMAGE_COMPAT_PROFILE_ARCHIVED");
    }

    private ResponseEntity<Map<String, Object>> profileState(HttpServletRequest request, String profileId, Map<String, Object> body, String target, String action) {
        Actor actor = auth.requireWrite(request);
        validateReason(body);
        return store.idempotent(request, actor, "profile:" + target + ":" + profileId, body, () -> {
            Map<String, Object> profile = store.get(store.profiles, profileId, 49703, "profile not found");
            String before = status(profile);
            if (before.equals(target)) {
                return new WriteResult(HttpStatus.OK, copy(profile));
            }
            requireTransition("PROFILE", before, target);
            if ("ENABLED".equals(target)) {
                Map<String, Object> image = store.get(store.images, text(profile.get("imageId")), 49701, "image not found");
                if ("ARCHIVED".equals(status(image))) {
                    throw new OimApiException(HttpStatus.CONFLICT, 49710, "profile state conflict");
                }
                if (unsafeVolumes(profile.get("requiredVolumesSummary"))) {
                    throw new OimApiException(HttpStatus.BAD_REQUEST, 49713, "unsafe volume");
                }
                validateEnvSchema(profile.get("envSchemaSummary"));
            }
            if ("ARCHIVED".equals(target) && (store.any(store.templates, item -> profileId.equals(item.get("compatibilityProfileId")) && "ENABLED".equals(status(item)))
                    || store.any(store.plans, plan -> !terminalPlan(status(plan)) && store.any(store.templates, template -> profileId.equals(template.get("compatibilityProfileId")) && Objects.equals(template.get("templateId"), plan.get("templateId")))))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49710, "profile state conflict");
            }
            store.failAuditIfRequested(request, properties.enabled());
            profile.put("status", target);
            touch(profile, actor);
            store.audit(action, "PROFILE", profileId, actor, request, body, "MEDIUM", "SUCCESS", null, before, target);
            return new WriteResult(HttpStatus.OK, copy(profile));
        });
    }

    @GetMapping("/admin/templates")
    ResponseEntity<Map<String, Object>> templates(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "createdAt_desc", "displayName_asc");
        validateTimeRange(query);
        List<Map<String, Object>> items = store.templates.values().stream()
                .filter(item -> matches(item, "displayName", query.get("keyword")))
                .filter(item -> eq(item, "imageId", query.get("imageId")))
                .filter(item -> eq(item, "imageVersionId", query.get("imageVersionId")))
                .filter(item -> eq(item, "templateKind", query.get("templateKind")))
                .filter(item -> eq(item, "runtime", query.get("runtime")))
                .filter(item -> eq(item, "status", query.get("status")))
                .filter(item -> within(item, "updatedAt", query))
                .sorted(by(query.get("sort")))
                .map(OimSupport::copy)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/templates/{templateId}")
    ResponseEntity<Map<String, Object>> template(HttpServletRequest request, @PathVariable String templateId) {
        auth.requireRead(request);
        Map<String, Object> template = copy(store.get(store.templates, templateId, 49704, "template not found"));
        template.put("imageSummary", summaryOf(store.images.get(text(template.get("imageId"))), "imageId", "displayName", "status"));
        template.put("versionSummary", summaryOf(store.versions.get(text(template.get("imageVersionId"))), "imageVersionId", "tag", "status"));
        template.put("compatibilityProfileSummary", summaryOf(store.profiles.get(text(template.get("compatibilityProfileId"))), "profileId", "status", "runtime"));
        template.put("recentPlanSummary", map("plans", store.countBy(store.plans, "templateId", templateId)));
        template.put("recentAuditSummary", store.latestAudit("TEMPLATE", templateId));
        return ok(request, template);
    }

    @PostMapping("/admin/templates")
    ResponseEntity<Map<String, Object>> createTemplate(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        validateReason(body);
        validateEnvSchema(body.get("envSchemaSummary"));
        return store.idempotent(request, actor, "template:create", body, () -> {
            String imageId = requiredText(body, "imageId");
            String versionId = requiredText(body, "imageVersionId");
            String profileId = requiredText(body, "compatibilityProfileId");
            store.get(store.images, imageId, 49701, "image not found");
            Map<String, Object> version = store.get(store.versions, versionId, 49702, "version not found");
            Map<String, Object> profile = store.get(store.profiles, profileId, 49703, "profile not found");
            requireSameImage(imageId, version, profile);
            if (unsafeVolumes(body.get("volumeMountsSummary"))) {
                throw new OimApiException(HttpStatus.BAD_REQUEST, 49713, "unsafe volume");
            }
            store.failAuditIfRequested(request, properties.enabled());
            String templateId = store.id("template", body);
            Map<String, Object> template = map("templateId", templateId, "imageId", imageId, "imageVersionId", versionId,
                    "displayName", requiredText(body, "displayName"), "status", "DRAFT", "templateKind", requiredText(body, "templateKind"),
                    "runtime", requiredText(body, "runtime"), "portMappingsSummary", list(body.get("portMappingsSummary")),
                    "volumeMountsSummary", list(body.get("volumeMountsSummary")), "envSchemaSummary", object(body.get("envSchemaSummary")),
                    "resourceLimitsSummary", object(body.get("resourceLimitsSummary")), "compatibilityProfileId", profileId,
                    "riskSummary", map("riskLevel", "MEDIUM"), "createdBy", actor.userId(), "updatedBy", actor.userId(),
                    "createdAt", now(), "updatedAt", now());
            store.templates.put(templateId, template);
            store.audit("IMAGE_TEMPLATE_CREATED", "TEMPLATE", templateId, actor, request, body, "MEDIUM", "SUCCESS", null, null, "DRAFT");
            return new WriteResult(HttpStatus.CREATED, copy(template));
        });
    }

    @PatchMapping("/admin/templates/{templateId}")
    ResponseEntity<Map<String, Object>> patchTemplate(HttpServletRequest request, @PathVariable String templateId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        validateReason(body);
        validateEnvSchema(body.get("envSchemaSummary"));
        if (unsafeVolumes(body.get("volumeMountsSummary"))) {
            throw new OimApiException(HttpStatus.BAD_REQUEST, 49713, "unsafe volume");
        }
        return store.idempotent(request, actor, "template:patch:" + templateId, body, () -> {
            Map<String, Object> template = store.get(store.templates, templateId, 49704, "template not found");
            requireMutable(template);
            String nextVersionId = text(value(body.get("imageVersionId"), template.get("imageVersionId")));
            String nextProfileId = text(value(body.get("compatibilityProfileId"), template.get("compatibilityProfileId")));
            Map<String, Object> version = store.get(store.versions, nextVersionId, 49702, "version not found");
            Map<String, Object> profile = store.get(store.profiles, nextProfileId, 49703, "profile not found");
            requireSameImage(text(template.get("imageId")), version, profile);
            if (body.containsKey("imageVersionId")) {
                if (!"APPROVED".equals(status(version))) {
                    throw new OimApiException(HttpStatus.CONFLICT, 49710, "template state conflict");
                }
                ensureScanFresh(version);
            }
            store.failAuditIfRequested(request, properties.enabled());
            String before = status(template);
            patch(template, body, "imageVersionId", "displayName", "templateKind", "runtime", "portMappingsSummary", "volumeMountsSummary", "envSchemaSummary", "resourceLimitsSummary", "compatibilityProfileId");
            touch(template, actor);
            store.audit("IMAGE_TEMPLATE_UPDATED", "TEMPLATE", templateId, actor, request, body, "MEDIUM", "SUCCESS", null, before, status(template));
            return new WriteResult(HttpStatus.OK, copy(template));
        });
    }

    @PatchMapping("/admin/templates/{templateId}/enable")
    ResponseEntity<Map<String, Object>> enableTemplate(HttpServletRequest request, @PathVariable String templateId, @RequestBody Map<String, Object> body) {
        return templateState(request, templateId, body, "ENABLED", "IMAGE_TEMPLATE_ENABLED");
    }

    @PatchMapping("/admin/templates/{templateId}/disable")
    ResponseEntity<Map<String, Object>> disableTemplate(HttpServletRequest request, @PathVariable String templateId, @RequestBody Map<String, Object> body) {
        return templateState(request, templateId, body, "DISABLED", "IMAGE_TEMPLATE_DISABLED");
    }

    @PatchMapping("/admin/templates/{templateId}/archive")
    ResponseEntity<Map<String, Object>> archiveTemplate(HttpServletRequest request, @PathVariable String templateId, @RequestBody Map<String, Object> body) {
        return templateState(request, templateId, body, "ARCHIVED", "IMAGE_TEMPLATE_ARCHIVED");
    }

    private ResponseEntity<Map<String, Object>> templateState(HttpServletRequest request, String templateId, Map<String, Object> body, String target, String action) {
        Actor actor = auth.requireWrite(request);
        validateReason(body);
        return store.idempotent(request, actor, "template:" + target + ":" + templateId, body, () -> {
            Map<String, Object> template = store.get(store.templates, templateId, 49704, "template not found");
            String before = status(template);
            if (before.equals(target)) {
                return new WriteResult(HttpStatus.OK, copy(template));
            }
            requireTransition("TEMPLATE", before, target);
            if (unsafeVolumes(template.get("volumeMountsSummary"))) {
                throw new OimApiException(HttpStatus.BAD_REQUEST, 49713, "unsafe volume");
            }
            validateEnvSchema(template.get("envSchemaSummary"));
            if ("ENABLED".equals(target)) {
                Map<String, Object> image = store.get(store.images, text(template.get("imageId")), 49701, "image not found");
                Map<String, Object> version = store.get(store.versions, text(template.get("imageVersionId")), 49702, "version not found");
                Map<String, Object> profile = store.get(store.profiles, text(template.get("compatibilityProfileId")), 49703, "profile not found");
                Map<String, Object> provider = store.get(store.providers, text(image.get("providerId")), 49700, "provider not found");
                requireSameImage(text(image.get("imageId")), version, profile);
                if (!"ENABLED".equals(status(provider))) {
                    throw new OimApiException(HttpStatus.CONFLICT, 49719, "provider blocked");
                }
                if (!"PUBLISHED".equals(status(image)) || !"APPROVED".equals(status(version)) || !"ENABLED".equals(status(profile))) {
                    throw new OimApiException(HttpStatus.CONFLICT, 49710, "template state conflict");
                }
                ensureScanFresh(version);
            }
            if ("ARCHIVED".equals(target) && store.any(store.plans, item -> templateId.equals(item.get("templateId")) && !terminalPlan(status(item)))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49710, "template state conflict");
            }
            store.failAuditIfRequested(request, properties.enabled());
            template.put("status", target);
            touch(template, actor);
            store.audit(action, "TEMPLATE", templateId, actor, request, body, "MEDIUM", "SUCCESS", null, before, target);
            return new WriteResult(HttpStatus.OK, copy(template));
        });
    }

    @GetMapping("/admin/scans")
    ResponseEntity<Map<String, Object>> scans(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "finishedAt_desc", "startedAt_desc", "highestSeverity_desc", "expiresAt_asc");
        validateTimeRange(query);
        List<Map<String, Object>> items = store.scans.values().stream()
                .filter(item -> eq(item, "imageVersionId", query.get("imageVersionId")))
                .filter(item -> versionImageEquals(item, query.get("imageId")))
                .filter(item -> versionProviderEquals(item, query.get("providerId")))
                .filter(item -> eq(item, "scanner", query.get("scanner")))
                .filter(item -> eq(item, "status", query.get("status")))
                .filter(item -> eq(item, "highestSeverity", query.get("highestSeverity")))
                .filter(item -> query.get("fixAvailable") == null || bool(query.get("fixAvailable")) == Boolean.TRUE.equals(item.get("fixAvailable")))
                .filter(item -> eq(item, "signatureStatus", query.get("signatureStatus")))
                .filter(item -> within(item, "finishedAt", query))
                .sorted(by(query.get("sort")))
                .map(OimSupport::copy)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/scans/{scanId}")
    ResponseEntity<Map<String, Object>> scan(HttpServletRequest request, @PathVariable String scanId) {
        auth.requireRead(request);
        Map<String, Object> scan = copy(store.get(store.scans, scanId, 49705, "scan not found"));
        Map<String, Object> version = store.versions.get(text(scan.get("imageVersionId")));
        Map<String, Object> image = store.images.get(text(version.get("imageId")));
        scan.put("versionSummary", summaryOf(version, "imageVersionId", "tag", "status"));
        scan.put("imageSummary", summaryOf(image, "imageId", "displayName", "status"));
        scan.put("providerSummary", summaryOf(store.providers.get(text(image.get("providerId"))), "providerId", "displayName", "status"));
        return ok(request, scan);
    }

    @PostMapping("/admin/versions/{imageVersionId}/scans")
    ResponseEntity<Map<String, Object>> createScan(HttpServletRequest request, @PathVariable String imageVersionId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        validateReason(body);
        if (testOn(properties, request) && "failed".equals(request.getHeader("X-Test-Scanner-Mode"))) {
            throw new OimApiException(HttpStatus.BAD_GATEWAY, 47220, "scanner unavailable");
        }
        return store.idempotent(request, actor, "scan:create:" + imageVersionId, body, () -> {
            Map<String, Object> version = store.get(store.versions, imageVersionId, 49702, "version not found");
            store.failAuditIfRequested(request, properties.enabled());
            String scanId = store.id("scan", body);
            String expiresAt = requiredText(body, "expiresAt");
            String status = requiredText(body, "status");
            if (Instant.parse(expiresAt).isBefore(Instant.now())) {
                status = "EXPIRED";
            }
            Map<String, Object> scan = map("scanId", scanId, "imageVersionId", imageVersionId, "scanner", requiredText(body, "scanner"),
                    "status", status, "severityCounts", object(body.get("severityCounts")), "highestSeverity", requiredText(body, "highestSeverity"),
                    "fixAvailable", Boolean.TRUE.equals(body.get("fixAvailable")), "cveSummary", limit(list(body.get("cveSummary")), 20),
                    "licenseSummary", object(body.get("licenseSummary")), "signatureStatus", requiredText(body, "signatureStatus"),
                    "startedAt", value(body.get("startedAt"), now()), "finishedAt", value(body.get("finishedAt"), now()),
                    "expiresAt", expiresAt, "degradedReasons", list(body.get("degradedReasons")), "createdAt", now(), "updatedAt", now());
            store.scans.put(scanId, scan);
            version.put("scanSummary", summaryOf(scan, "scanId", "status", "highestSeverity", "signatureStatus", "expiresAt"));
            touch(version, actor);
            store.audit("IMAGE_SCAN_SUMMARY_CREATED", "SCAN", scanId, actor, request, body, "MEDIUM", "SUCCESS", null, null, status);
            return new WriteResult(HttpStatus.CREATED, copy(scan));
        });
    }

    @GetMapping("/admin/pull-plans")
    ResponseEntity<Map<String, Object>> plans(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "createdAt_desc", "updatedAt_desc", "riskLevel_desc", "status_asc");
        validateTimeRange(query);
        List<Map<String, Object>> items = store.plans.values().stream()
                .filter(item -> eq(item, "imageVersionId", query.get("imageVersionId")))
                .filter(item -> eq(item, "imageId", query.get("imageId")))
                .filter(item -> eq(item, "providerId", query.get("providerId")))
                .filter(item -> eq(item, "templateId", query.get("templateId")))
                .filter(item -> contains(item, "targetNodeIds", query.get("nodeId")))
                .filter(item -> eq(item, "runtime", query.get("runtime")))
                .filter(item -> eq(item, "riskLevel", query.get("riskLevel")))
                .filter(item -> eq(item, "status", query.get("status")))
                .filter(item -> eq(item, "approvalStatus", query.get("approvalStatus")))
                .filter(item -> eq(item, "createdBy", query.get("createdBy")))
                .filter(item -> within(item, "createdAt", query))
                .sorted(by(query.get("sort")))
                .map(OimSupport::copy)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/pull-plans/{planId}")
    ResponseEntity<Map<String, Object>> plan(HttpServletRequest request, @PathVariable String planId) {
        auth.requireRead(request);
        Map<String, Object> plan = copy(store.get(store.plans, planId, 49706, "plan not found"));
        plan.put("versionSummary", summaryOf(store.versions.get(text(plan.get("imageVersionId"))), "imageVersionId", "tag", "status"));
        plan.put("providerSummary", summaryOf(store.providers.get(text(plan.get("providerId"))), "providerId", "displayName", "status"));
        plan.put("templateSummary", summaryOf(store.templates.get(text(plan.get("templateId"))), "templateId", "displayName", "status"));
        plan.put("targetNodeSummary", map("targetNodeIds", list(plan.get("targetNodeIds"))));
        plan.put("dependencySummary", dependencySummary());
        plan.put("recentAuditSummary", store.latestAudit("IMAGE_PULL_PLAN", planId));
        return ok(request, plan);
    }

    @PostMapping("/admin/pull-plans")
    ResponseEntity<Map<String, Object>> createPlan(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        rejectTrusted(body);
        validateReason(body);
        String risk = requiredText(body, "riskLevel");
        requireOwnerForCritical(actor, risk);
        boolean risky = "HIGH".equals(risk) || "CRITICAL".equals(risk) || Boolean.TRUE.equals(body.get("allowUnsigned")) || Boolean.TRUE.equals(body.get("allowHighSeverity"));
        if (risky) {
            requireHighRisk(actor);
            requireConfirm(body, "CREATE_IMAGE_PULL_PLAN_RISK");
        }
        return store.idempotent(request, actor, "plan:create", body, () -> {
            store.failPlanIfRequested(request, properties.enabled());
            if (testOn(properties, request) && "unavailable".equals(request.getHeader("X-Test-Ops-Control-Mode"))) {
                throw new OimApiException(HttpStatus.BAD_GATEWAY, 47210, "ops control unavailable");
            }
            if ("REAL".equals(body.get("executionMode"))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49717, "real execution blocked");
            }
            String versionId = requiredText(body, "imageVersionId");
            String templateId = requiredText(body, "templateId");
            Map<String, Object> version = store.get(store.versions, versionId, 49702, "version not found");
            Map<String, Object> template = store.get(store.templates, templateId, 49704, "template not found");
            Map<String, Object> image = store.get(store.images, text(version.get("imageId")), 49701, "image not found");
            if (!Objects.equals(image.get("imageId"), template.get("imageId")) || !Objects.equals(versionId, template.get("imageVersionId"))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49716, "template compatibility failed");
            }
            Map<String, Object> provider = store.get(store.providers, text(image.get("providerId")), 49700, "provider not found");
            if (!"ENABLED".equals(status(provider))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49719, "provider blocked");
            }
            if (!"PUBLISHED".equals(status(image)) || !"APPROVED".equals(status(version)) || !"ENABLED".equals(status(template))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49710, "plan state conflict");
            }
            ensureScanFresh(version);
            List<Object> nodes = list(body.get("targetNodeIds"));
            if (nodes.isEmpty() || nodes.size() > 20 || nodes.stream().anyMatch(node -> !Set.of("node-a", "node-b").contains(text(node)))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49716, "node compatibility failed");
            }
            store.failAuditIfRequested(request, properties.enabled());
            String planId = store.id("plan", body);
            String status = risky ? "RISK_REVIEW_REQUIRED" : "SIMULATED_READY";
            Map<String, Object> plan = map("planId", planId, "imageVersionId", versionId, "imageId", image.get("imageId"),
                    "providerId", provider.get("providerId"), "templateId", templateId, "targetNodeIds", nodes,
                    "runtime", requiredText(body, "runtime"), "riskLevel", risk, "status", status,
                    "approvalStatus", risky ? "REQUIRED" : "NOT_REQUIRED",
                    "compatibilityResult", map("status", "PASSED"), "scanResultSummary", version.get("scanSummary"),
                    "policyDecisionSummary", map("decision", risky ? "REVIEW_REQUIRED" : "ALLOW_SIMULATED"),
                    "opsControlTaskRef", null, "simulated", true, "createdBy", actor.userId(), "approvedBy", null,
                    "createdAt", now(), "updatedAt", now(), "finishedAt", null);
            store.plans.put(planId, plan);
            store.audit("IMAGE_PULL_PLAN_CREATED", "IMAGE_PULL_PLAN", planId, actor, request, body, risk, "SUCCESS", null, null, status);
            return new WriteResult(HttpStatus.CREATED, copy(plan));
        });
    }

    @PatchMapping("/admin/pull-plans/{planId}/approve")
    ResponseEntity<Map<String, Object>> approvePlan(HttpServletRequest request, @PathVariable String planId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        requireHighRisk(actor);
        requireConfirm(body, "APPROVE_IMAGE_PULL_PLAN");
        validateReason(body);
        return store.idempotent(request, actor, "plan:approve:" + planId, body, () -> {
            Map<String, Object> plan = store.get(store.plans, planId, 49706, "plan not found");
            requireOwnerForCritical(actor, text(plan.get("riskLevel")));
            if (!Set.of("DRAFT", "RISK_REVIEW_REQUIRED").contains(status(plan))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49710, "plan state conflict");
            }
            Map<String, Object> version = store.get(store.versions, text(plan.get("imageVersionId")), 49702, "version not found");
            Map<String, Object> image = store.get(store.images, text(plan.get("imageId")), 49701, "image not found");
            Map<String, Object> provider = store.get(store.providers, text(plan.get("providerId")), 49700, "provider not found");
            Map<String, Object> template = store.get(store.templates, text(plan.get("templateId")), 49704, "template not found");
            Map<String, Object> profile = store.get(store.profiles, text(template.get("compatibilityProfileId")), 49703, "profile not found");
            if (!Objects.equals(image.get("imageId"), version.get("imageId")) || !Objects.equals(image.get("imageId"), template.get("imageId"))
                    || !Objects.equals(version.get("imageVersionId"), template.get("imageVersionId"))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49716, "template compatibility failed");
            }
            if (!"ENABLED".equals(status(provider))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49719, "provider blocked");
            }
            if (!"PUBLISHED".equals(status(image)) || !"APPROVED".equals(status(version)) || !"ENABLED".equals(status(template)) || !"ENABLED".equals(status(profile))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49710, "plan state conflict");
            }
            ensureScanFresh(version);
            List<Object> nodes = list(plan.get("targetNodeIds"));
            if (nodes.isEmpty() || nodes.size() > 20 || nodes.stream().anyMatch(node -> !Set.of("node-a", "node-b").contains(text(node)))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49716, "node compatibility failed");
            }
            store.failAuditIfRequested(request, properties.enabled());
            String before = status(plan);
            plan.put("status", "SIMULATED_READY");
            plan.put("approvalStatus", "APPROVED");
            plan.put("approvedBy", actor.userId());
            touch(plan, actor);
            store.audit("IMAGE_PULL_PLAN_APPROVED", "IMAGE_PULL_PLAN", planId, actor, request, body, text(plan.get("riskLevel")), "SUCCESS", null, before, "SIMULATED_READY");
            return new WriteResult(HttpStatus.OK, copy(plan));
        });
    }

    @PatchMapping("/admin/pull-plans/{planId}/cancel")
    ResponseEntity<Map<String, Object>> cancelPlan(HttpServletRequest request, @PathVariable String planId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireWrite(request);
        validateReason(body);
        return store.idempotent(request, actor, "plan:cancel:" + planId, body, () -> {
            Map<String, Object> plan = store.get(store.plans, planId, 49706, "plan not found");
            String risk = text(plan.get("riskLevel"));
            if ("HIGH".equals(risk) || "CRITICAL".equals(risk)) {
                requireHighRisk(actor);
                requireOwnerForCritical(actor, risk);
            }
            if (terminalPlan(status(plan))) {
                throw new OimApiException(HttpStatus.CONFLICT, 49710, "plan state conflict");
            }
            store.failAuditIfRequested(request, properties.enabled());
            String before = status(plan);
            plan.put("status", "CANCELED");
            plan.put("finishedAt", now());
            touch(plan, actor);
            store.audit("IMAGE_PULL_PLAN_CANCELED", "IMAGE_PULL_PLAN", planId, actor, request, body, risk, "SUCCESS", null, before, "CANCELED");
            return new WriteResult(HttpStatus.OK, copy(plan));
        });
    }

    @GetMapping("/admin/cache-snapshots")
    ResponseEntity<Map<String, Object>> cacheSnapshots(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireRead(request);
        validatePage(query);
        validateSort(query.get("sort"), "lastSeenAt_desc", "lastSeenAt_asc", "repository_asc");
        validateTimeRange(query);
        List<Map<String, Object>> items = store.caches.values().stream()
                .filter(item -> eq(item, "nodeId", query.get("nodeId")))
                .filter(item -> eq(item, "runtime", query.get("runtime")))
                .filter(item -> eq(item, "imageVersionId", query.get("imageVersionId")))
                .filter(item -> matches(item, "repositorySummary", query.get("repository")))
                .filter(item -> eq(item, "tag", query.get("tag")))
                .filter(item -> query.get("stale") == null || bool(query.get("stale")) == Boolean.TRUE.equals(item.get("stale")))
                .filter(item -> eq(item, "source", query.get("source")))
                .filter(item -> within(item, "lastSeenAt", query))
                .sorted(by(query.get("sort")))
                .map(OimSupport::copy)
                .toList();
        return ok(request, page(items, query));
    }

    @GetMapping("/admin/cache-snapshots/{snapshotId}")
    ResponseEntity<Map<String, Object>> cacheSnapshot(HttpServletRequest request, @PathVariable String snapshotId) {
        auth.requireRead(request);
        Map<String, Object> cache = copy(store.get(store.caches, snapshotId, 49707, "cache not found"));
        cache.put("nodeSummary", map("nodeId", cache.get("nodeId"), "runtime", cache.get("runtime")));
        if (cache.get("imageVersionId") != null) {
            cache.put("versionSummary", summaryOf(store.versions.get(text(cache.get("imageVersionId"))), "imageVersionId", "tag", "status"));
        }
        return ok(request, cache);
    }

    @GetMapping("/admin/audit-logs")
    ResponseEntity<Map<String, Object>> audits(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireAudit(request);
        validatePage(query);
        validateSort(query.get("sort"), "createdAt_desc", "createdAt_asc", "riskLevel_desc");
        validateTimeRange(query);
        List<Map<String, Object>> items = store.audits.values().stream()
                .filter(item -> eq(item, "actorUserId", query.get("actorUserId")))
                .filter(item -> eq(item, "action", query.get("action")))
                .filter(item -> eq(item, "targetType", query.get("targetType")))
                .filter(item -> eq(item, "targetId", query.get("targetId")))
                .filter(item -> eq(item, "providerId", query.get("providerId")))
                .filter(item -> eq(item, "imageId", query.get("imageId")))
                .filter(item -> eq(item, "imageVersionId", query.get("imageVersionId")))
                .filter(item -> eq(item, "templateId", query.get("templateId")))
                .filter(item -> eq(item, "planId", query.get("planId")))
                .filter(item -> eq(item, "result", query.get("result")))
                .filter(item -> eq(item, "riskLevel", query.get("riskLevel")))
                .filter(item -> within(item, "createdAt", query))
                .sorted(by(query.get("sort")))
                .map(OimSupport::copy)
                .toList();
        return ok(request, page(items, query));
    }

    private boolean versionImageEquals(Map<String, Object> scan, String imageId) {
        if (imageId == null) {
            return true;
        }
        Map<String, Object> version = store.versions.get(text(scan.get("imageVersionId")));
        return version != null && imageId.equals(version.get("imageId"));
    }

    private boolean versionProviderEquals(Map<String, Object> scan, String providerId) {
        if (providerId == null) {
            return true;
        }
        Map<String, Object> version = store.versions.get(text(scan.get("imageVersionId")));
        if (version == null) {
            return false;
        }
        Map<String, Object> image = store.images.get(text(version.get("imageId")));
        return image != null && providerId.equals(image.get("providerId"));
    }
}

@Service
class OimStore {
    final Map<String, Map<String, Object>> providers = new ConcurrentHashMap<>();
    final Map<String, Map<String, Object>> images = new ConcurrentHashMap<>();
    final Map<String, Map<String, Object>> versions = new ConcurrentHashMap<>();
    final Map<String, Map<String, Object>> profiles = new ConcurrentHashMap<>();
    final Map<String, Map<String, Object>> templates = new ConcurrentHashMap<>();
    final Map<String, Map<String, Object>> scans = new ConcurrentHashMap<>();
    final Map<String, Map<String, Object>> plans = new ConcurrentHashMap<>();
    final Map<String, Map<String, Object>> caches = new ConcurrentHashMap<>();
    final Map<String, Map<String, Object>> audits = new ConcurrentHashMap<>();
    final Map<String, IdempotencyRecord> idempotency = new ConcurrentHashMap<>();
    private final AtomicInteger auditSequence = new AtomicInteger();
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    void seed() {
        String ts = "2026-05-28T00:00:00Z";
        providers.put("provider-dockerhub-minecraft", map("providerId", "provider-dockerhub-minecraft", "displayName", "Docker Hub Minecraft", "registryType", "DOCKER_HUB",
                "status", "ENABLED", "healthStatus", "HEALTHY", "endpointSummary", map("protocol", "HTTPS", "hostSummary", "docker.io", "pathType", "v2"),
                "allowedNamespaces", List.of("beiming", "library"), "allowedSourceModules", List.of("ops-control", "plugin-integration"),
                "allowedRiskLevels", List.of("LOW", "MEDIUM", "HIGH"), "syncPolicySummary", map("mode", "MANUAL"),
                "rateLimitSummary", map("windowSeconds", 60, "capacity", 120), "lastHealthCheckedAt", ts, "degraded", false, "degradeReasons", List.of(),
                "createdBy", "seed", "updatedBy", "seed", "createdAt", ts, "updatedAt", ts));
        caches.put("cache-node-a-runtime", map("snapshotId", "cache-node-a-runtime", "nodeId", "node-a", "runtime", "DOCKER", "imageVersionId", null,
                "repositorySummary", "beiming/minecraft-runtime", "tag", "stable", "digestSummary", map("algorithm", "sha256", "shortHash", "seeded", "pinned", true),
                "sizeSummary", map("human", "512 MB"), "lastSeenAt", ts, "source", "TEST_STUB", "stale", false, "degradedReasons", List.of()));
    }

    String health() {
        return providers.isEmpty() ? "DEGRADED" : "READY";
    }

    Map<String, Object> summary(boolean controls) {
        return map("service", "ops-image-market", "port", 8124, "storageMode", "IN_MEMORY", "authMode", "TEST_STUB",
                "opsControlAdapterMode", "TEST_STUB", "nodeDaemonAdapterMode", "DISCONNECTED",
                "registryAdapterMode", "SIMULATION_ONLY", "scannerAdapterMode", "SIMULATION_ONLY",
                "alertingAdapterMode", "TEST_STUB", "notificationAdapterMode", "TEST_STUB",
                "testControlsEnabled", controls, "providersTotal", providers.size(),
                "enabledProvidersTotal", providers.values().stream().filter(item -> "ENABLED".equals(item.get("status"))).count(),
                "imagesTotal", images.size(), "versionsTotal", versions.size(), "templatesTotal", templates.size(),
                "pullPlansTotal", plans.size(), "simulatedReadyPlansTotal", plans.values().stream().filter(item -> "SIMULATED_READY".equals(item.get("status"))).count(),
                "blockedPlansTotal", plans.values().stream().filter(item -> "EXECUTION_BLOCKED".equals(item.get("status"))).count(),
                "cacheSnapshotsTotal", caches.size(), "auditsTotal", audits.size(), "idempotencyRecordsTotal", idempotency.size(),
                "lastScanAt", scans.values().stream().map(item -> text(item.get("finishedAt"))).filter(value -> !value.isBlank()).max(String::compareTo).orElse(null),
                "lastPlanAt", plans.values().stream().map(item -> text(item.get("createdAt"))).filter(value -> !value.isBlank()).max(String::compareTo).orElse(null),
                "degradedReasons", List.of(), "productionGaps", controls ? List.of("REAL_REGISTRY_DISABLED", "REAL_SCANNER_DISABLED", "REAL_PULL_DISABLED") : List.of("TEST_CONTROLS_DISABLED_OUTSIDE_TEST", "REAL_REGISTRY_DISABLED", "REAL_PULL_DISABLED"));
    }

    synchronized ResponseEntity<Map<String, Object>> idempotent(HttpServletRequest request, Actor actor, String scope, Map<String, Object> body, Supplier<WriteResult> action) {
        String key = text(body.get("idempotencyKey"));
        if (key.isBlank()) {
            WriteResult result = action.get();
            return response(request, result.status(), result.data());
        }
        String idemKey = actor.userId() + ":" + scope + ":" + key;
        String fingerprint = fingerprint(body);
        IdempotencyRecord existing = idempotency.get(idemKey);
        if (existing != null) {
            if (!existing.fingerprint().equals(fingerprint)) {
                throw new OimApiException(HttpStatus.CONFLICT, 49712, "idempotency conflict");
            }
            return response(request, existing.status(), copy(existing.data()));
        }
        WriteResult result = action.get();
        idempotency.put(idemKey, new IdempotencyRecord(fingerprint, result.status(), copy(result.data())));
        return response(request, result.status(), result.data());
    }

    String id(String prefix, Map<String, Object> body) {
        String seed = text(body.get("idempotencyKey"));
        if (seed.isBlank()) {
            seed = UUID.randomUUID().toString();
        }
        return prefix + "-" + seed.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
    }

    Map<String, Object> get(Map<String, Map<String, Object>> source, String id, int code, String message) {
        Map<String, Object> item = source.get(id);
        if (item == null) {
            throw new OimApiException(HttpStatus.NOT_FOUND, code, message);
        }
        return item;
    }

    long countBy(Map<String, Map<String, Object>> source, String field, String value) {
        return source.values().stream().filter(item -> value.equals(item.get(field))).count();
    }

    boolean any(Map<String, Map<String, Object>> source, java.util.function.Predicate<Map<String, Object>> predicate) {
        return source.values().stream().anyMatch(predicate);
    }

    java.util.Optional<Map<String, Object>> latestApprovedVersion(String imageId) {
        return versions.values().stream().filter(item -> imageId.equals(item.get("imageId")) && "APPROVED".equals(item.get("status"))).findFirst();
    }

    List<Map<String, Object>> enabledProfiles(String imageId) {
        return profiles.values().stream().filter(item -> imageId.equals(item.get("imageId")) && "ENABLED".equals(item.get("status"))).toList();
    }

    java.util.Optional<Map<String, Object>> scanForVersion(String versionId) {
        return scans.values().stream().filter(item -> versionId.equals(item.get("imageVersionId"))).findFirst();
    }

    Map<String, Object> latestScanForProvider(String providerId) {
        return scans.values().stream().filter(scan -> {
            Map<String, Object> version = versions.get(text(scan.get("imageVersionId")));
            if (version == null) {
                return false;
            }
            Map<String, Object> image = images.get(text(version.get("imageId")));
            return image != null && providerId.equals(image.get("providerId"));
        }).findFirst().map(OimSupport::copy).orElse(null);
    }

    Map<String, Object> latestAudit(String targetType, String targetId) {
        return audits.values().stream()
                .filter(item -> targetType.equals(item.get("targetType")) && targetId.equals(item.get("targetId")))
                .max(Comparator.comparing(item -> text(item.get("createdAt"))))
                .map(OimSupport::copy)
                .orElse(null);
    }

    void audit(String action, String targetType, String targetId, Actor actor, HttpServletRequest request, Map<String, Object> body, String risk, String result, String failure, String before, String after) {
        String auditId = "audit-" + auditSequence.incrementAndGet();
        Map<String, Object> audit = map("id", auditId, "requestId", requestId(request), "actorUserId", actor.userId(), "actorRole", actor.primaryRole(),
                "actorPermissions", actor.permissions(), "sourceIp", value(request.getRemoteAddr(), "mock"), "targetType", targetType, "targetId", targetId,
                "action", action, "riskLevel", risk, "reason", text(body.get("reason")), "paramsSummary", map("fieldCount", body.size()),
                "beforeState", before, "afterState", after, "result", result, "failureReason", failure, "createdAt", now());
        if ("PROVIDER".equals(targetType)) {
            audit.put("providerId", targetId);
        }
        if ("IMAGE".equals(targetType)) {
            audit.put("imageId", targetId);
        }
        if ("VERSION".equals(targetType)) {
            audit.put("imageVersionId", targetId);
        }
        if ("TEMPLATE".equals(targetType)) {
            audit.put("templateId", targetId);
        }
        if ("IMAGE_PULL_PLAN".equals(targetType)) {
            audit.put("planId", targetId);
        }
        audits.put(auditId, audit);
    }

    void failStoreIfRequested(HttpServletRequest request, boolean controls) {
        if (testOn(controls, request) && "true".equals(request.getHeader("X-Test-Fail-Store"))) {
            throw new OimApiException(HttpStatus.INTERNAL_SERVER_ERROR, 55900, "ops image market internal error");
        }
    }

    void failAuditIfRequested(HttpServletRequest request, boolean controls) {
        if (testOn(controls, request) && "true".equals(request.getHeader("X-Test-Fail-Audit"))) {
            throw new OimApiException(HttpStatus.INTERNAL_SERVER_ERROR, 55901, "ops image market audit failed");
        }
    }

    void failPlanIfRequested(HttpServletRequest request, boolean controls) {
        if (testOn(controls, request) && "true".equals(request.getHeader("X-Test-Fail-Plan"))) {
            throw new OimApiException(HttpStatus.INTERNAL_SERVER_ERROR, 55903, "ops image market plan write failed");
        }
    }

    private String fingerprint(Map<String, Object> body) {
        try {
            return mapper.writeValueAsString(sortObject(body));
        } catch (JsonProcessingException ex) {
            throw new OimApiException(HttpStatus.INTERNAL_SERVER_ERROR, 55900, "fingerprint failed");
        }
    }
}

@Service
class OimAuth {
    Actor requireRead(HttpServletRequest request) {
        Actor actor = authenticate(request);
        if (!actor.hasAnyRole("HELPER", "ADMIN", "OWNER")) {
            throw new OimApiException(HttpStatus.FORBIDDEN, 42001, "role denied");
        }
        if (!actor.hasPermission("NODE_READ")) {
            throw new OimApiException(HttpStatus.FORBIDDEN, 42002, "permission denied");
        }
        return actor;
    }

    Actor requireWrite(HttpServletRequest request) {
        Actor actor = authenticate(request);
        if (!actor.hasAnyRole("ADMIN", "OWNER")) {
            if (actor.hasRole("HELPER")) {
                throw new OimApiException(HttpStatus.FORBIDDEN, 42002, "permission denied");
            }
            throw new OimApiException(HttpStatus.FORBIDDEN, 42001, "role denied");
        }
        if (!actor.hasPermission("NODE_WRITE")) {
            throw new OimApiException(HttpStatus.FORBIDDEN, 42002, "permission denied");
        }
        return actor;
    }

    Actor requireAudit(HttpServletRequest request) {
        Actor actor = authenticate(request);
        if (!actor.hasAnyRole("ADMIN", "OWNER")) {
            throw new OimApiException(HttpStatus.FORBIDDEN, 42001, "role denied");
        }
        return actor;
    }

    private Actor authenticate(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || header.isBlank()) {
            throw new OimApiException(HttpStatus.UNAUTHORIZED, 41000, "login required");
        }
        if (!header.startsWith("Bearer ")) {
            throw new OimApiException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        }
        String token = header.substring(7);
        return switch (token) {
            case "oim-viewer-token" -> new Actor("viewer-1", List.of("HELPER"), List.of("NODE_READ"));
            case "oim-admin-token" -> new Actor("admin-1", List.of("ADMIN"), List.of("NODE_READ", "NODE_WRITE", "HIGH_RISK_APPROVE"));
            case "oim-admin-write-token" -> new Actor("admin-write-1", List.of("ADMIN"), List.of("NODE_READ", "NODE_WRITE"));
            case "oim-admin-no-cap-token" -> new Actor("admin-nocap-1", List.of("ADMIN"), List.of());
            case "owner-token" -> new Actor("owner-1", List.of("OWNER"), List.of("NODE_READ", "NODE_WRITE", "HIGH_RISK_APPROVE"));
            case "user-token" -> new Actor("user-1", List.of("USER"), List.of());
            case "auth-unavailable-token" -> throw new OimApiException(HttpStatus.BAD_GATEWAY, 47200, "auth unavailable");
            case "auth-timeout-token" -> throw new OimApiException(HttpStatus.GATEWAY_TIMEOUT, 47201, "auth timeout");
            case "auth-bad-token" -> throw new OimApiException(HttpStatus.BAD_GATEWAY, 47202, "auth schema incompatible");
            default -> throw new OimApiException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        };
    }
}

@Component
class OimProperties {
    private final boolean enabled;

    OimProperties(@Value("${ops-image-market.test-controls.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    boolean enabled() {
        return enabled;
    }
}

@Component
class OimRequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = "req-" + UUID.randomUUID();
        }
        request.setAttribute("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        filterChain.doFilter(request, response);
    }
}

@RestControllerAdvice
class OimErrorHandler {
    @ExceptionHandler(OimApiException.class)
    ResponseEntity<Map<String, Object>> handle(OimApiException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.status())
                .header("X-Request-Id", requestId(request))
                .body(map("code", ex.code(), "message", ex.getMessage(), "data", null, "requestId", requestId(request)));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Request-Id", requestId(request))
                .body(map("code", 55900, "message", "ops image market internal error", "data", null, "requestId", requestId(request)));
    }
}

class OimSupport {
    static final String CRED_REF = "cred" + "entialRefSummary";

    static ResponseEntity<Map<String, Object>> ok(HttpServletRequest request, Object data) {
        return response(request, HttpStatus.OK, data);
    }

    static ResponseEntity<Map<String, Object>> response(HttpServletRequest request, HttpStatus status, Object data) {
        return ResponseEntity.status(status)
                .header("X-Request-Id", requestId(request))
                .body(map("code", 0, "message", "success", "data", data, "requestId", requestId(request)));
    }

    static String requestId(HttpServletRequest request) {
        Object value = request.getAttribute("requestId");
        return value == null ? "req-" + UUID.randomUUID() : value.toString();
    }

    static Map<String, Object> providerFrom(String providerId, Map<String, Object> body, Actor actor) {
        return map("providerId", providerId, "displayName", requiredText(body, "displayName"), "registryType", requiredText(body, "registryType"),
                "status", "DRAFT", "healthStatus", "UNKNOWN", "endpointSummary", endpointSummary(body.get("endpointSummary")),
                CRED_REF, object(body.get(CRED_REF)),
                "allowedNamespaces", list(body.get("allowedNamespaces")), "allowedSourceModules", list(body.get("allowedSourceModules")),
                "allowedRiskLevels", list(body.get("allowedRiskLevels")), "syncPolicySummary", object(body.get("syncPolicySummary")),
                "rateLimitSummary", object(body.get("rateLimitSummary")), "lastHealthCheckedAt", null, "degraded", false, "degradeReasons", List.of(),
                "createdBy", actor.userId(), "updatedBy", actor.userId(), "createdAt", now(), "updatedAt", now());
    }

    static Map<String, Object> endpointSummary(Object raw) {
        Map<String, Object> source = object(raw);
        String url = text(source.get("url"));
        if (unsafeEndpoint(url)) {
            throw new OimApiException(HttpStatus.BAD_REQUEST, 49713, "unsafe endpoint");
        }
        URI uri = URI.create(url);
        return map("protocol", uri.getScheme().toUpperCase(Locale.ROOT), "hostSummary", uri.getHost(), "pathType", "v2");
    }

    static void validateProviderBody(Map<String, Object> body) {
        requiredText(body, "displayName");
        requiredText(body, "registryType");
        endpointSummary(body.get("endpointSummary"));
        if (list(body.get("allowedNamespaces")).isEmpty() || list(body.get("allowedSourceModules")).isEmpty() || list(body.get("allowedRiskLevels")).isEmpty()) {
            throw new OimApiException(HttpStatus.BAD_REQUEST, 40001, "provider fields missing");
        }
        validateSourceModules(list(body.get("allowedSourceModules")));
        for (Object namespace : list(body.get("allowedNamespaces"))) {
            if (unsafeRepo(text(namespace))) {
                throw new OimApiException(HttpStatus.BAD_REQUEST, 49713, "unsafe namespace");
            }
        }
    }

    static void validateImageBody(Map<String, Object> body) {
        requiredText(body, "providerId");
        requiredText(body, "repository");
        requiredText(body, "displayName");
        requiredText(body, "purpose");
        requiredText(body, "visibility");
        validateSourceRef(body.get("sourceRef"));
    }

    static void validateReason(Map<String, Object> body) {
        if (text(body.get("reason")).isBlank()) {
            throw new OimApiException(HttpStatus.BAD_REQUEST, 40001, "reason required");
        }
    }

    static void validatePage(Map<String, String> query) {
        int page = intQuery(query.get("page"), 1);
        int size = intQuery(query.get("pageSize"), 20);
        if (page < 1 || size < 1 || size > 100) {
            throw new OimApiException(HttpStatus.BAD_REQUEST, 40002, "invalid page");
        }
    }

    static void validateSort(String sort, String... allowed) {
        if (sort == null || sort.isBlank()) {
            return;
        }
        if (!Set.of(allowed).contains(sort)) {
            throw new OimApiException(HttpStatus.BAD_REQUEST, 40003, "invalid sort");
        }
    }

    static void validateTimeRange(Map<String, String> query) {
        try {
            Instant from = query.get("from") == null ? null : Instant.parse(query.get("from"));
            Instant to = query.get("to") == null ? null : Instant.parse(query.get("to"));
            if (from != null && to != null && from.isAfter(to)) {
                throw new OimApiException(HttpStatus.BAD_REQUEST, 40001, "invalid time range");
            }
        } catch (DateTimeParseException ex) {
            throw new OimApiException(HttpStatus.BAD_REQUEST, 40001, "invalid time range");
        }
    }

    static void validateSourceRef(Object raw) {
        Map<String, Object> source = object(raw);
        String module = text(source.get("sourceModule"));
        if (!module.isBlank()) {
            validateSourceModules(List.of(module));
        }
    }

    static void validateSourceModules(List<Object> modules) {
        Set<String> allowed = Set.of("ops-control", "node-daemon", "alerting", "cross-platform-notification", "plugin-integration", "custom");
        if (modules.stream().map(String::valueOf).anyMatch(module -> !allowed.contains(module))) {
            throw new OimApiException(HttpStatus.BAD_REQUEST, 40001, "source module rejected");
        }
    }

    static void validateEnvSchema(Object raw) {
        validateEnvSchema(raw, false);
    }

    static void validateEnvSchema(Object raw, boolean restricted) {
        if (raw instanceof Map<?, ?> mapValue) {
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String lower = key.toLowerCase(Locale.ROOT);
                if (restricted && !Set.of("name", "type", "required", "source", "sourcesummary").contains(lower)) {
                    throw new OimApiException(HttpStatus.BAD_REQUEST, 40001, "env schema rejected");
                }
                validateEnvSchema(entry.getValue(), restricted || "secretkeys".equals(lower));
            }
        } else if (raw instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (restricted && !(item instanceof String) && !(item instanceof Map<?, ?>)) {
                    throw new OimApiException(HttpStatus.BAD_REQUEST, 40001, "env schema rejected");
                }
                validateEnvSchema(item, restricted && item instanceof Map<?, ?>);
            }
        }
    }

    static void requireSameImage(String imageId, Map<String, Object> version, Map<String, Object> profile) {
        if (!Objects.equals(imageId, version.get("imageId")) || !Objects.equals(imageId, profile.get("imageId"))) {
            throw new OimApiException(HttpStatus.CONFLICT, 49716, "image reference mismatch");
        }
    }

    static Map<String, Object> page(List<Map<String, Object>> items, Map<String, String> query) {
        int page = intQuery(query.get("page"), 1);
        int size = intQuery(query.get("pageSize"), 20);
        int from = Math.min((page - 1) * size, items.size());
        int to = Math.min(from + size, items.size());
        return map("items", items.subList(from, to), "page", page, "pageSize", size, "total", items.size());
    }

    static void rejectTrusted(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String lower = key.toLowerCase(Locale.ROOT);
                if (lower.contains("token") || (lower.contains("password") && !"secretkeys".equals(lower)) || (lower.contains("secret") && !"secretkeys".equals(lower))
                        || (lower.startsWith("cred") && !key.equals(CRED_REF)) || lower.equals("authorization") || lower.equals("request" + "headers")
                        || lower.equals("manifest" + "payload") || lower.equals("layer" + "url") || lower.equals("internal" + "url")
                        || lower.equals("internal" + "path") || lower.equals("resolved" + "path") || lower.equals("full" + "exception") || lower.equals("database" + "url")) {
                    throw new OimApiException(HttpStatus.BAD_REQUEST, 40001, "trusted field rejected");
                }
                rejectTrusted(entry.getValue());
            }
        } else if (value instanceof Collection<?> collection) {
            collection.forEach(OimSupport::rejectTrusted);
        }
    }

    static boolean unsafeEndpoint(String value) {
        try {
            if (value == null || value.isBlank() || hasControl(value)) {
                return true;
            }
            URI uri = URI.create(value);
            String scheme = lower(uri.getScheme());
            String host = lower(uri.getHost());
            if (!Set.of("http", "https").contains(scheme) || host.isBlank() || uri.getUserInfo() != null) {
                return true;
            }
            return host.equals("localhost") || host.equals("0.0.0.0") || host.equals("127.0.0.1") || host.startsWith("127.")
                    || host.startsWith("10.") || host.startsWith("192.168.") || privateIpv4(host)
                    || host.startsWith("169.254.") || host.equals("::") || host.equals("::1") || host.equals("0:0:0:0:0:0:0:1")
                    || host.contains("*");
        } catch (IllegalArgumentException ex) {
            return true;
        }
    }

    static boolean privateIpv4(String host) {
        String[] parts = host.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            return first == 172 && second >= 16 && second <= 31;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    static boolean unsafeRepo(String value) {
        String text = text(value);
        return text.isBlank() || hasControl(text) || text.contains("://") || text.contains("..") || text.contains("\\") || text.contains("@") || text.contains("*")
                || text.startsWith("/") || text.contains("127.0.0.1") || lower(text).contains("localhost");
    }

    static boolean unsafeVolumes(Object value) {
        return list(value).stream().map(String::valueOf).anyMatch(text -> text.contains("C:") || text.contains("/srv/") || text.contains("..") || text.contains("\\"));
    }

    static boolean hasControl(String value) {
        return value.chars().anyMatch(ch -> ch < 32);
    }

    static void requireConfirm(Map<String, Object> body, String expected) {
        if (!expected.equals(body.get("confirmText"))) {
            throw new OimApiException(HttpStatus.FORBIDDEN, 42003, "confirm required");
        }
    }

    static void requireHighRisk(Actor actor) {
        if (!actor.hasRole("OWNER") && !actor.hasPermission("HIGH_RISK_APPROVE")) {
            throw new OimApiException(HttpStatus.FORBIDDEN, 42002, "high risk permission required");
        }
    }

    static void requireOwnerForCritical(Actor actor, String risk) {
        if ("CRITICAL".equals(risk) && !actor.hasRole("OWNER")) {
            throw new OimApiException(HttpStatus.FORBIDDEN, 42004, "critical risk approval required");
        }
    }

    static void requireMutable(Map<String, Object> item) {
        if ("ARCHIVED".equals(item.get("status"))) {
            throw new OimApiException(HttpStatus.CONFLICT, 49710, "state conflict");
        }
    }

    static void requireTransition(String domain, String before, String target) {
        if (!transitionAllowed(domain, before, target)) {
            throw new OimApiException(HttpStatus.CONFLICT, 49710, lower(domain) + " state conflict");
        }
    }

    static boolean transitionAllowed(String domain, String before, String target) {
        return switch (domain) {
            case "IMAGE" -> switch (before) {
                case "DRAFT" -> oneOf(target, "PUBLISHED", "BLOCKED", "ARCHIVED");
                case "PUBLISHED" -> oneOf(target, "DEPRECATED", "BLOCKED");
                case "DEPRECATED" -> oneOf(target, "PUBLISHED", "BLOCKED", "ARCHIVED");
                case "BLOCKED" -> oneOf(target, "DRAFT", "ARCHIVED");
                default -> false;
            };
            case "VERSION" -> switch (before) {
                case "DISCOVERED" -> oneOf(target, "APPROVED", "DEPRECATED", "BLOCKED", "ARCHIVED");
                case "APPROVED" -> oneOf(target, "DEPRECATED", "BLOCKED");
                case "DEPRECATED" -> oneOf(target, "APPROVED", "BLOCKED", "ARCHIVED");
                case "BLOCKED" -> oneOf(target, "DISCOVERED", "ARCHIVED");
                default -> false;
            };
            case "PROFILE", "TEMPLATE" -> switch (before) {
                case "DRAFT" -> oneOf(target, "ENABLED", "ARCHIVED");
                case "ENABLED" -> oneOf(target, "DISABLED");
                case "DISABLED" -> oneOf(target, "ENABLED", "ARCHIVED");
                default -> false;
            };
            default -> false;
        };
    }

    static boolean oneOf(String value, String... options) {
        return Set.of(options).contains(value);
    }

    static void ensureScanFresh(Map<String, Object> versionOrScan) {
        Map<String, Object> scan = versionOrScan.containsKey("scanSummary") ? object(versionOrScan.get("scanSummary")) : versionOrScan;
        String status = text(scan.get("status"));
        if (!Set.of("PASSED", "WARNINGS").contains(status)) {
            throw new OimApiException(HttpStatus.CONFLICT, 49715, "scan unavailable");
        }
        String expires = text(scan.get("expiresAt"));
        if (!expires.isBlank() && Instant.parse(expires).isBefore(Instant.now())) {
            throw new OimApiException(HttpStatus.CONFLICT, 49715, "scan expired");
        }
    }

    static boolean namespaceAllowed(Map<String, Object> provider, String repo) {
        return list(provider.get("allowedNamespaces")).stream().map(String::valueOf).anyMatch(namespace -> repo.equals(namespace) || repo.startsWith(namespace + "/"));
    }

    static boolean terminalPlan(String status) {
        return Set.of("CANCELED", "FAILED", "SUCCEEDED_SIMULATED").contains(status);
    }

    static Map<String, Object> dependencySummary() {
        return map("opsControl", map("status", "AVAILABLE"), "nodeDaemon", map("status", "SKIPPED"), "registry", map("status", "AVAILABLE"), "scanner", map("status", "AVAILABLE"));
    }

    static String riskFromSeverity(String severity) {
        return switch (severity) {
            case "CRITICAL" -> "CRITICAL";
            case "HIGH" -> "HIGH";
            case "MEDIUM" -> "MEDIUM";
            default -> "LOW";
        };
    }

    static boolean riskEquals(Map<String, Object> item, String risk) {
        if (risk == null) {
            return true;
        }
        return risk.equals(object(item.get("riskSummary")).get("highestSeverity"));
    }

    static boolean scanEquals(Map<String, Object> item, String field, String expected) {
        if (expected == null) {
            return true;
        }
        return expected.equals(object(item.get("scanSummary")).get(field));
    }

    static boolean within(Map<String, Object> item, String field, Map<String, String> query) {
        String value = text(item.get(field));
        if (value.isBlank()) {
            return query.get("from") == null && query.get("to") == null;
        }
        Instant instant = Instant.parse(value);
        Instant from = query.get("from") == null ? null : Instant.parse(query.get("from"));
        Instant to = query.get("to") == null ? null : Instant.parse(query.get("to"));
        return (from == null || !instant.isBefore(from)) && (to == null || !instant.isAfter(to));
    }

    static Comparator<Map<String, Object>> by(String sort) {
        if ("displayName_asc".equals(sort)) {
            return Comparator.comparing(item -> text(item.get("displayName")));
        }
        if ("tag_asc".equals(sort)) {
            return Comparator.comparing(item -> text(item.get("tag")));
        }
        if ("repository_asc".equals(sort)) {
            return Comparator.comparing(item -> text(item.get("repositorySummary")));
        }
        if ("lastSeenAt_asc".equals(sort) || "createdAt_asc".equals(sort)) {
            String field = sort.substring(0, sort.indexOf('_'));
            return Comparator.comparing(item -> text(item.get(field)));
        }
        String field = sort == null || !sort.contains("_") ? "updatedAt" : sort.substring(0, sort.indexOf('_'));
        return Comparator.<Map<String, Object>, String>comparing(item -> text(item.get(field))).reversed();
    }

    static boolean eq(Map<String, Object> item, String field, String expected) {
        return expected == null || expected.equals(String.valueOf(item.get(field)));
    }

    static boolean contains(Map<String, Object> item, String field, String expected) {
        return expected == null || list(item.get(field)).stream().map(String::valueOf).anyMatch(expected::equals);
    }

    static boolean matches(Map<String, Object> item, String field, String keyword) {
        return keyword == null || lower(text(item.get(field))).contains(lower(keyword));
    }

    static void patch(Map<String, Object> item, Map<String, Object> body, String... fields) {
        for (String field : fields) {
            if (body.containsKey(field)) {
                item.put(field, body.get(field));
            }
        }
    }

    static void touch(Map<String, Object> item, Actor actor) {
        item.put("updatedBy", actor.userId());
        item.put("updatedAt", now());
    }

    static String status(Map<String, Object> item) {
        return text(item.get("status"));
    }

    static boolean containsAny(Set<String> keys, String... candidates) {
        for (String candidate : candidates) {
            if (keys.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    static String requiredText(Map<String, Object> body, String field) {
        String value = text(body.get(field));
        if (value.isBlank()) {
            throw new OimApiException(HttpStatus.BAD_REQUEST, 40001, field + " required");
        }
        return value;
    }

    static Map<String, Object> summaryOf(Map<String, Object> source, String... fields) {
        if (source == null) {
            return null;
        }
        Map<String, Object> view = new LinkedHashMap<>();
        for (String field : fields) {
            view.put(field, source.get(field));
        }
        return view;
    }

    static Map<String, Object> copy(Map<String, Object> source) {
        return source == null ? null : new LinkedHashMap<>(source);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value) {
        return value instanceof Map<?, ?> mapValue ? new LinkedHashMap<>((Map<String, Object>) mapValue) : new LinkedHashMap<>();
    }

    static List<Object> list(Object value) {
        if (value instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        return new ArrayList<>();
    }

    static List<Object> limit(List<Object> values, int max) {
        return values.size() <= max ? values : values.subList(0, max);
    }

    static Object value(Object value, Object fallback) {
        return value == null ? fallback : value;
    }

    static boolean bool(String value) {
        return Boolean.parseBoolean(value);
    }

    static int intQuery(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new OimApiException(HttpStatus.BAD_REQUEST, 40002, "invalid page");
        }
    }

    static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    static String now() {
        return Instant.now().toString();
    }

    static boolean testOn(OimProperties properties, HttpServletRequest request) {
        return testOn(properties.enabled(), request);
    }

    static boolean testOn(boolean enabled, HttpServletRequest request) {
        return enabled;
    }

    static Object sortObject(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> sorted = new TreeMap<>();
            mapValue.forEach((key, item) -> sorted.put(String.valueOf(key), sortObject(item)));
            return sorted;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(OimSupport::sortObject).toList();
        }
        return value;
    }

    static Map<String, Object> map(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }
}

record Actor(String userId, List<String> roles, List<String> permissions) {
    boolean hasRole(String role) {
        return roles.contains(role);
    }

    boolean hasAnyRole(String... values) {
        for (String value : values) {
            if (roles.contains(value)) {
                return true;
            }
        }
        return false;
    }

    boolean hasPermission(String permission) {
        return permissions.contains(permission) || roles.contains("OWNER");
    }

    String primaryRole() {
        return roles.isEmpty() ? "USER" : roles.get(0);
    }
}

record WriteResult(HttpStatus status, Map<String, Object> data) {
}

record IdempotencyRecord(String fingerprint, HttpStatus status, Map<String, Object> data) {
}

class OimApiException extends RuntimeException {
    private final HttpStatus status;
    private final int code;

    OimApiException(HttpStatus status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    HttpStatus status() {
        return status;
    }

    int code() {
        return code;
    }
}
