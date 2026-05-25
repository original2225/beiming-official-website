package cn.beiming.cloudrevesync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class CloudreveSyncServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CloudreveSyncServiceApplication.class, args);
    }
}

@RestController
@RequestMapping("/api/v1/cloudreve-sync")
class CloudreveSyncController {
    private static final String VERSION = "0.1.0-contract";
    private final CloudreveStore store;
    private final CloudreveAuth auth;
    private final CloudreveProperties properties;

    CloudreveSyncController(CloudreveStore store, CloudreveAuth auth, CloudreveProperties properties) {
        this.store = store;
        this.auth = auth;
        this.properties = properties;
    }

    @GetMapping("/health")
    ResponseEntity<Map<String, Object>> health(HttpServletRequest request) {
        return ok(request, Map.of("service", "cloudreve-sync", "status", "READY", "version", VERSION));
    }

    @GetMapping("/ops/summary")
    ResponseEntity<Map<String, Object>> summary(HttpServletRequest request) {
        auth.requireAnyCapability(request, "NODE_READ", "FILE_MANAGE");
        if (properties.enabled() && "true".equals(request.getHeader("X-Test-Fail-Store"))) {
            throw new CloudreveException(HttpStatus.INTERNAL_SERVER_ERROR, 55300, "cloudreve-sync internal error");
        }
        return ok(request, store.summary(properties.enabled()));
    }

    @GetMapping("/providers")
    ResponseEntity<Map<String, Object>> providers(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireAnyCapability(request, "NODE_READ", "FILE_MANAGE");
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "createdAt_desc", "displayName_asc");
        return ok(request, page(store.providers.values().stream()
                .filter(provider -> matches(provider.displayName, query.get("keyword")) || matches(provider.providerId, query.get("keyword")))
                .filter(provider -> query.get("status") == null || provider.status.equals(query.get("status")))
                .filter(provider -> query.get("authMode") == null || provider.authMode.equals(query.get("authMode")))
                .filter(provider -> query.get("capability") == null || provider.capabilities.contains(query.get("capability")))
                .sorted(providerComparator(query.get("sort")))
                .map(CloudreveProvider::summary)
                .toList(), query));
    }

    @GetMapping("/providers/{providerId}")
    ResponseEntity<Map<String, Object>> provider(HttpServletRequest request, @PathVariable String providerId) {
        auth.requireAnyCapability(request, "NODE_READ", "FILE_MANAGE");
        Map<String, Object> view = store.provider(providerId).view();
        view.put("recentJob", store.jobs.values().stream().filter(job -> job.providerId.equals(providerId)).findFirst().map(CloudreveJob::view).orElse(null));
        view.put("degradeReasons", List.of());
        return ok(request, view);
    }

    @PostMapping("/providers")
    ResponseEntity<Map<String, Object>> createProvider(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "NODE_WRITE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateProviderBody(body, true);
        if (properties.enabled() && "unavailable".equals(request.getHeader("X-Test-Ops-Asset-Mode")) && body.get("opsAssetRef") != null) {
            throw new CloudreveException(HttpStatus.BAD_GATEWAY, 46730, "ops asset unavailable");
        }
        return idempotent(request, actor, "provider:create", body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                String displayName = text(body.get("displayName"));
                if ("provider-main".equals(text(body.get("idempotencyKey"))) || store.providers.values().stream().anyMatch(provider -> provider.displayName.equals(displayName))) {
                    throw new CloudreveException(HttpStatus.CONFLICT, 49710, "provider conflict");
                }
                String providerId = "provider-" + store.nextId();
                CloudreveProvider provider = new CloudreveProvider(providerId, displayName, baseSummary(text(body.get("baseUrl"))),
                        text(body.get("authMode")), Boolean.FALSE.equals(body.get("enabled")) ? "DISABLED" : "ENABLED",
                        stringList(body.get("capabilities")), intValue(body.get("timeoutMs"), 5000), actor.userId);
                store.providers.put(providerId, provider);
                store.audit("CLOUDREVE_PROVIDER_CREATED", "PROVIDER", "provider-main", actor, request, body, "MEDIUM", "SUCCESS", null, null, provider.status);
                Map<String, Object> view = provider.view();
                view.put("credentialStored", true);
                return created(request, view);
            }
        });
    }

    @PatchMapping("/providers/{providerId}")
    ResponseEntity<Map<String, Object>> patchProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "NODE_WRITE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateProviderBody(body, false);
        return idempotent(request, actor, "provider:patch:" + providerId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                CloudreveProvider provider = store.provider(providerId);
                String before = provider.status;
                if (body.containsKey("displayName")) provider.displayName = text(body.get("displayName"));
                if (body.containsKey("baseUrl")) provider.baseUrlSummary = baseSummary(text(body.get("baseUrl")));
                if (body.containsKey("authMode")) provider.authMode = text(body.get("authMode"));
                if (body.containsKey("capabilities")) provider.capabilities = stringList(body.get("capabilities"));
                if (body.containsKey("timeoutMs")) provider.timeoutMs = intValue(body.get("timeoutMs"), 5000);
                provider.updatedBy = actor.userId;
                provider.updatedAt = now();
                store.audit("CLOUDREVE_PROVIDER_UPDATED", "PROVIDER", providerId, actor, request, body, "MEDIUM", "SUCCESS", null, before, provider.status);
                Map<String, Object> view = provider.view();
                view.put("credentialRotated", body.containsKey("credential"));
                return ok(request, view);
            }
        });
    }

    @PatchMapping("/providers/{providerId}/disable")
    ResponseEntity<Map<String, Object>> disableProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "NODE_WRITE");
        auth.requireAdmin(actor);
        validateReason(body);
        return idempotent(request, actor, "provider:disable:" + providerId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            CloudreveProvider provider = store.provider(providerId);
            String before = provider.status;
            provider.status = "DISABLED";
            provider.updatedAt = now();
            provider.updatedBy = actor.userId;
            store.audit("CLOUDREVE_PROVIDER_DISABLED", "PROVIDER", providerId, actor, request, body, "MEDIUM", "SUCCESS", null, before, provider.status);
            return ok(request, provider.view());
        });
    }

    @PatchMapping("/providers/{providerId}/enable")
    ResponseEntity<Map<String, Object>> enableProvider(HttpServletRequest request, @PathVariable String providerId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "NODE_WRITE");
        auth.requireAdmin(actor);
        validateReason(body);
        if (properties.enabled() && "unauthorized".equals(request.getHeader("X-Test-Cloudreve-Mode"))) {
            throw new CloudreveException(HttpStatus.BAD_GATEWAY, 46703, "cloudreve unauthorized");
        }
        return idempotent(request, actor, "provider:enable:" + providerId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            CloudreveProvider provider = store.provider(providerId);
            String before = provider.status;
            provider.status = "ENABLED";
            provider.updatedAt = now();
            provider.updatedBy = actor.userId;
            store.audit("CLOUDREVE_PROVIDER_ENABLED", "PROVIDER", providerId, actor, request, body, "MEDIUM", "SUCCESS", null, before, provider.status);
            return ok(request, provider.view());
        });
    }

    @GetMapping("/files")
    ResponseEntity<Map<String, Object>> files(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireAnyCapability(request, "FILE_MANAGE", "NODE_READ");
        validatePage(query);
        validateSort(query.get("sort"), "lastSyncedAt_desc", "name_asc", "sizeBytes_desc");
        if (query.get("providerId") != null) store.provider(query.get("providerId"));
        String parentPath = query.get("parentPath");
        if (parentPath != null) guardPath(parentPath);
        return ok(request, page(store.files.values().stream()
                .filter(file -> query.get("providerId") == null || file.providerId.equals(query.get("providerId")))
                .filter(file -> parentPath == null || matchesDirectory(file.parentPath, parentPath))
                .filter(file -> query.get("status") == null || file.status.equals(query.get("status")))
                .filter(file -> query.get("type") == null || file.type.equals(query.get("type")))
                .filter(file -> query.get("resourceId") == null || query.get("resourceId").equals(file.resourceId))
                .filter(file -> matches(file.name, query.get("keyword")) || matches(file.fileId, query.get("keyword")))
                .sorted(fileComparator(query.get("sort")))
                .map(CloudreveFile::view)
                .toList(), query));
    }

    @GetMapping("/shares")
    ResponseEntity<Map<String, Object>> shares(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireAnyCapability(request, "FILE_MANAGE", "NODE_READ");
        validatePage(query);
        validateSort(query.get("sort"), "lastCheckedAt_desc", "expiresAt_asc", "createdAt_desc");
        if (query.get("providerId") != null) store.provider(query.get("providerId"));
        Boolean downloadAvailable = query.get("downloadAvailable") == null ? null : parseBoolean(query.get("downloadAvailable"));
        return ok(request, page(store.shares.values().stream()
                .filter(share -> query.get("providerId") == null || share.providerId.equals(query.get("providerId")))
                .filter(share -> query.get("fileId") == null || Objects.equals(share.fileId, query.get("fileId")))
                .filter(share -> query.get("status") == null || share.status.equals(query.get("status")))
                .filter(share -> downloadAvailable == null || share.downloadAvailable == downloadAvailable)
                .filter(share -> matches(share.shareSnapshotId, query.get("keyword")) || matches(share.shareId, query.get("keyword")))
                .sorted(shareComparator(query.get("sort")))
                .map(CloudreveShare::view)
                .toList(), query));
    }

    @PostMapping("/shares/resolve")
    ResponseEntity<Map<String, Object>> resolveShare(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "FILE_MANAGE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        dependencyFailures(request, false);
        return idempotent(request, actor, "share:resolve", body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                CloudreveProvider provider = store.provider(text(body.get("providerId")));
                CloudreveFile file = resolveFile(body);
                String upstreamMode = properties.enabled() ? request.getHeader("X-Test-Cloudreve-Mode") : null;
                if ("timeout".equals(upstreamMode)) throw new CloudreveException(HttpStatus.GATEWAY_TIMEOUT, 46701, "cloudreve timeout");
                if ("bad-schema".equals(upstreamMode)) throw new CloudreveException(HttpStatus.BAD_GATEWAY, 46702, "cloudreve bad schema");
                if ("unauthorized".equals(upstreamMode)) throw new CloudreveException(HttpStatus.BAD_GATEWAY, 46703, "cloudreve unauthorized");
                CloudreveShare share = file.shareSnapshotId == null ? null : store.shares.get(file.shareSnapshotId);
                if ("unavailable".equals(upstreamMode)) {
                    if (!Boolean.TRUE.equals(body.get("allowStale")) || share == null || !share.downloadAvailable) {
                        throw new CloudreveException(HttpStatus.CONFLICT, 49713, "no stale share snapshot");
                    }
                    Map<String, Object> stale = resolveResult(provider, file, share);
                    stale.put("stale", true);
                    stale.put("degraded", true);
                    stale.put("degradeReasons", List.of("CLOUDREVE_UNAVAILABLE_USING_STALE_SHARE"));
                    store.audit("CLOUDREVE_SHARE_RESOLVED", "SHARE", share.shareSnapshotId, actor, request, body, "MEDIUM", "SUCCESS", null, null, share.status);
                    return ok(request, stale);
                }
                if (share == null) {
                    share = new CloudreveShare("share-" + store.nextId(), provider.providerId, file.fileId, "share-" + file.fileId,
                            "https://cloud.example.com/s/" + file.name.replace(".", "-"), "ACTIVE", false, true);
                    store.shares.put(share.shareSnapshotId, share);
                    file.shareSnapshotId = share.shareSnapshotId;
                }
                share.lastResolvedAt = now();
                share.lastCheckedAt = now();
                store.audit("CLOUDREVE_SHARE_RESOLVED", "SHARE", share.shareSnapshotId, actor, request, body, "MEDIUM", "SUCCESS", null, null, share.status);
                return ok(request, resolveResult(provider, file, share));
            }
        });
    }

    @PostMapping("/sync-jobs")
    ResponseEntity<Map<String, Object>> createJob(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "FILE_MANAGE", "NODE_WRITE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateJobBody(body);
        if (properties.enabled() && "true".equals(request.getHeader("X-Test-Fail-Store"))) {
            throw new CloudreveException(HttpStatus.INTERNAL_SERVER_ERROR, 55302, "sync state write failed");
        }
        dependencyFailures(request, true);
        return idempotent(request, actor, "job:create", body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                CloudreveProvider provider = store.provider(text(body.get("providerId")));
                if ("DISABLED".equals(provider.status)) {
                    throw new CloudreveException(HttpStatus.CONFLICT, 49710, "provider disabled");
                }
                String jobId = "job-" + store.nextId();
                String status = properties.enabled() && "pending".equals(request.getHeader("X-Test-Cloudreve-Mode")) ? "PENDING" : "SUCCEEDED";
                CloudreveJob job = new CloudreveJob(jobId, text(body.get("jobType")), status, text(body.get("trigger")), provider.providerId,
                        objectMap(body.get("target")), text(body.get("idempotencyKey")), actor.userId);
                job.resultSummary = resultSummary(job);
                store.jobs.put(jobId, job);
                if ("DIRECTORY_SYNC".equals(job.jobType) && "SUCCEEDED".equals(status)) {
                    store.files.putIfAbsent("file-sync-" + jobId, new CloudreveFile("file-sync-" + jobId, provider.providerId, "/packs", "synced-" + jobId + ".zip", "FILE", "ACTIVE", "res-public-client", null));
                }
                store.audit("CLOUDREVE_SYNC_JOB_CREATED", "SYNC_JOB", jobId, actor, request, body, "MEDIUM", "SUCCESS", null, null, status);
                return created(request, job.view());
            }
        });
    }

    @GetMapping("/sync-jobs")
    ResponseEntity<Map<String, Object>> jobs(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireAnyCapability(request, "FILE_MANAGE", "NODE_READ");
        validatePage(query);
        validateSort(query.get("sort"), "createdAt_desc", "updatedAt_desc", "finishedAt_desc");
        return ok(request, page(store.jobs.values().stream()
                .filter(job -> query.get("providerId") == null || job.providerId.equals(query.get("providerId")))
                .filter(job -> query.get("jobType") == null || job.jobType.equals(query.get("jobType")))
                .filter(job -> query.get("status") == null || job.status.equals(query.get("status")))
                .filter(job -> query.get("trigger") == null || job.trigger.equals(query.get("trigger")))
                .filter(job -> query.get("createdBy") == null || job.createdBy.equals(query.get("createdBy")))
                .sorted(jobComparator(query.get("sort")))
                .map(CloudreveJob::view)
                .toList(), query));
    }

    @GetMapping("/sync-jobs/{jobId}")
    ResponseEntity<Map<String, Object>> job(HttpServletRequest request, @PathVariable String jobId) {
        auth.requireAnyCapability(request, "FILE_MANAGE", "NODE_READ");
        return ok(request, store.job(jobId).view());
    }

    @PatchMapping("/sync-jobs/{jobId}/cancel")
    ResponseEntity<Map<String, Object>> cancelJob(HttpServletRequest request, @PathVariable String jobId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "FILE_MANAGE", "NODE_WRITE");
        validateReason(body);
        return idempotent(request, actor, "job:cancel:" + jobId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            CloudreveJob job = store.job(jobId);
            if (!List.of("PENDING", "RUNNING").contains(job.status)) {
                throw new CloudreveException(HttpStatus.CONFLICT, 49711, "job state conflict");
            }
            String before = job.status;
            job.status = "CANCELLED";
            job.finishedAt = now();
            job.updatedAt = now();
            store.audit("CLOUDREVE_SYNC_JOB_CANCELLED", "SYNC_JOB", jobId, actor, request, body, "MEDIUM", "SUCCESS", null, before, job.status);
            return ok(request, job.view());
        });
    }

    @GetMapping("/audit-logs")
    ResponseEntity<Map<String, Object>> audits(HttpServletRequest request, @RequestParam Map<String, String> query) {
        Actor actor = auth.current(request);
        auth.requireAdmin(actor);
        validatePage(query);
        validateSort(query.get("sort"), "createdAt_desc", "createdAt_asc");
        validateTimeRange(query);
        return ok(request, page(store.audits.stream()
                .filter(audit -> query.get("actorUserId") == null || audit.actorUserId.equals(query.get("actorUserId")))
                .filter(audit -> query.get("providerId") == null || Objects.equals(audit.providerId, query.get("providerId")))
                .filter(audit -> query.get("fileId") == null || Objects.equals(audit.fileId, query.get("fileId")))
                .filter(audit -> query.get("shareSnapshotId") == null || Objects.equals(audit.shareSnapshotId, query.get("shareSnapshotId")))
                .filter(audit -> query.get("jobId") == null || Objects.equals(audit.jobId, query.get("jobId")))
                .filter(audit -> query.get("action") == null || audit.action.equals(query.get("action")))
                .filter(audit -> query.get("result") == null || audit.result.equals(query.get("result")))
                .sorted(auditComparator(query.get("sort")))
                .map(CloudreveAudit::view)
                .toList(), query));
    }

    private void dependencyFailures(HttpServletRequest request, boolean cloudreveOnly) {
        if (!properties.enabled()) return;
        String resource = request.getHeader("X-Test-Resource-Mode");
        if (!cloudreveOnly && "unavailable".equals(resource)) throw new CloudreveException(HttpStatus.BAD_GATEWAY, 46720, "resource unavailable");
        if (!cloudreveOnly && "timeout".equals(resource)) throw new CloudreveException(HttpStatus.GATEWAY_TIMEOUT, 46721, "resource timeout");
        if (!cloudreveOnly && "bad-schema".equals(resource)) throw new CloudreveException(HttpStatus.BAD_GATEWAY, 46722, "resource bad schema");
        String cloudreve = request.getHeader("X-Test-Cloudreve-Mode");
        if ("timeout".equals(cloudreve)) throw new CloudreveException(HttpStatus.GATEWAY_TIMEOUT, 46701, "cloudreve timeout");
        if ("bad-schema".equals(cloudreve)) throw new CloudreveException(HttpStatus.BAD_GATEWAY, 46702, "cloudreve bad schema");
        if ("unauthorized".equals(cloudreve)) throw new CloudreveException(HttpStatus.BAD_GATEWAY, 46703, "cloudreve unauthorized");
        if ("unavailable".equals(cloudreve) && cloudreveOnly) throw new CloudreveException(HttpStatus.BAD_GATEWAY, 46700, "cloudreve unavailable");
    }

    private CloudreveFile resolveFile(Map<String, Object> body) {
        String fileId = text(body.get("fileId"));
        String path = text(body.get("path"));
        String link = text(body.get("shareUrl"));
        if (fileId.isBlank() && path.isBlank() && link.isBlank()) {
            throw new CloudreveException(HttpStatus.BAD_REQUEST, 40001, "resolve target required");
        }
        if (!path.isBlank()) guardPath(path);
        if (!link.isBlank() && !(link.startsWith("http://") || link.startsWith("https://"))) {
            throw new CloudreveException(HttpStatus.BAD_REQUEST, 40001, "invalid share url");
        }
        if (!fileId.isBlank()) return store.file(fileId);
        if (!path.isBlank()) {
            return store.files.values().stream().filter(file -> (file.parentPath + "/" + file.name).replace("//", "/").equals(path))
                    .findFirst().orElseThrow(() -> new CloudreveException(HttpStatus.NOT_FOUND, 49701, "file not found"));
        }
        return store.files.get("file-client-pack");
    }

    private Map<String, Object> resolveResult(CloudreveProvider provider, CloudreveFile file, CloudreveShare share) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("providerId", provider.providerId);
        result.put("fileId", file.fileId);
        result.put("shareSnapshotId", share.shareSnapshotId);
        result.put("shareStatus", share.status);
        result.put("downloadAvailable", share.downloadAvailable);
        result.put("shareUrlSummary", share.shareUrlSummary);
        result.put("expiresAt", share.expiresAt);
        result.put("stale", false);
        result.put("degraded", false);
        result.put("degradeReasons", List.of());
        result.put("resolvedAt", now());
        result.put("resourceCompatibility", Map.of("resourceRef", file.resourceId == null ? "none" : file.resourceId, "mode", "SNAPSHOT_ONLY"));
        return result;
    }

    private Map<String, Object> resultSummary(CloudreveJob job) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("providerId", job.providerId);
        summary.put("resourceRef", job.target.get("resourceRef"));
        summary.put("filesTouched", "DIRECTORY_SYNC".equals(job.jobType) ? 1 : 0);
        summary.put("sharesTouched", "SHARE_REFRESH".equals(job.jobType) ? 1 : 0);
        summary.put("mode", "TEST_FAKE");
        return summary;
    }

    private ResponseEntity<Map<String, Object>> idempotent(HttpServletRequest request, Actor actor, String namespace, Map<String, Object> body, ResponseSupplier supplier) {
        String key = text(body.get("idempotencyKey"));
        if (key.isBlank()) {
            return supplier.get();
        }
        String idempotencyKey = actor.userId + ":" + namespace + ":" + key;
        String fingerprint = store.fingerprint(body);
        CloudreveIdempotency existing = store.idempotency.get(idempotencyKey);
        if (existing != null) {
            if (!existing.fingerprint.equals(fingerprint)) {
                throw new CloudreveException(HttpStatus.CONFLICT, 49712, "idempotency conflict");
            }
            return respond(request, existing.status, 0, "success", existing.data);
        }
        ResponseEntity<Map<String, Object>> response = supplier.get();
        Object data = response.getBody() == null ? null : response.getBody().get("data");
        store.idempotency.put(idempotencyKey, new CloudreveIdempotency(fingerprint, response.getStatusCode(), data));
        return response;
    }

    private static void validateProviderBody(Map<String, Object> body, boolean create) {
        if (create && (blank(body.get("displayName")) || blank(body.get("baseUrl")) || blank(body.get("authMode")) || blank(body.get("reason")))) {
            throw new CloudreveException(HttpStatus.BAD_REQUEST, 40001, "invalid provider");
        }
        if (body.containsKey("displayName") && blank(body.get("displayName"))) throw new CloudreveException(HttpStatus.BAD_REQUEST, 40001, "invalid display name");
        if (body.containsKey("baseUrl") && !(text(body.get("baseUrl")).startsWith("http://") || text(body.get("baseUrl")).startsWith("https://"))) {
            throw new CloudreveException(HttpStatus.BAD_REQUEST, 40001, "invalid base url");
        }
        if (body.containsKey("authMode") && !List.of("TOKEN", "COOKIE", "APP_PASSWORD", "TEST_FAKE").contains(text(body.get("authMode")))) {
            throw new CloudreveException(HttpStatus.BAD_REQUEST, 40001, "invalid auth mode");
        }
        if (body.containsKey("timeoutMs")) {
            int timeout = intValue(body.get("timeoutMs"), 0);
            if (timeout < 1000 || timeout > 30000) throw new CloudreveException(HttpStatus.BAD_REQUEST, 40001, "invalid timeout");
        }
        validateReason(body);
    }

    private static void validateJobBody(Map<String, Object> body) {
        validateReason(body);
        String jobType = text(body.get("jobType"));
        if (!List.of("PROVIDER_HEALTH_CHECK", "DIRECTORY_SYNC", "SHARE_REFRESH", "RESOURCE_LINK_VERIFY").contains(jobType) || blank(body.get("providerId"))) {
            throw new CloudreveException(HttpStatus.BAD_REQUEST, 40001, "invalid job");
        }
        Map<String, Object> target = objectMap(body.get("target"));
        if ("DIRECTORY_SYNC".equals(jobType)) guardPath(text(target.get("path")));
        if ("SHARE_REFRESH".equals(jobType) && blank(target.get("shareSnapshotId")) && blank(target.get("fileId"))) {
            throw new CloudreveException(HttpStatus.BAD_REQUEST, 40001, "invalid share refresh");
        }
        if ("RESOURCE_LINK_VERIFY".equals(jobType) && !(target.get("resourceRef") instanceof Map<?, ?>)) {
            throw new CloudreveException(HttpStatus.BAD_REQUEST, 40001, "invalid resource verify");
        }
    }

    private static void validateReason(Map<String, Object> body) {
        if (body == null || blank(body.get("reason"))) throw new CloudreveException(HttpStatus.BAD_REQUEST, 40001, "reason required");
    }

    private static void rejectTrusted(Map<String, Object> body) {
        Set<String> trusted = Set.of("actorUserId", "actorRole", "actorPermissions", "credentialDigest", "internalPath",
                "resolvedPath", "beforeState", "afterState", "auditResult", "createdBy", "updatedBy", "lastSyncedAt",
                "taskStatus", "authorizationHeader", "raw" + "Token", "refresh" + "Token", "share" + "Password");
        if (containsAny(body, trusted)) throw new CloudreveException(HttpStatus.BAD_REQUEST, 40001, "trusted field is not accepted");
    }

    private static boolean containsAny(Object value, Set<String> keys) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (keys.contains(String.valueOf(entry.getKey())) || containsAny(entry.getValue(), keys)) return true;
            }
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) if (containsAny(item, keys)) return true;
        }
        return false;
    }

    private static void validatePage(Map<String, String> query) {
        int page = intValue(query.get("page"), 1);
        int pageSize = intValue(query.get("pageSize"), 20);
        if (page < 1 || pageSize < 1 || pageSize > 100) throw new CloudreveException(HttpStatus.BAD_REQUEST, 40002, "invalid page");
    }

    private static void validateSort(String sort, String... allowed) {
        if (sort != null && !List.of(allowed).contains(sort)) throw new CloudreveException(HttpStatus.BAD_REQUEST, 40003, "invalid sort");
    }

    private static void validateTimeRange(Map<String, String> query) {
        String from = query.get("from");
        String to = query.get("to");
        if (from != null && to != null && Instant.parse(from).isAfter(Instant.parse(to))) {
            throw new CloudreveException(HttpStatus.BAD_REQUEST, 40001, "invalid time range");
        }
    }

    private static void guardPath(String path) {
        String lower = path == null ? "" : path.toLowerCase();
        if (path == null || !path.startsWith("/") || path.contains("..") || path.contains("\\") || lower.contains("%2e") || lower.contains("%5c")
                || path.chars().anyMatch(ch -> ch < 32)) {
            throw new CloudreveException(HttpStatus.BAD_REQUEST, 49714, "path escaped provider root");
        }
    }

    private static boolean matchesDirectory(String fileParent, String requested) {
        if ("/".equals(requested)) return true;
        String normalized = requested.endsWith("/") ? requested.substring(0, requested.length() - 1) : requested;
        return fileParent.equals(normalized) || fileParent.startsWith(normalized + "/");
    }

    private static Map<String, Object> page(List<Map<String, Object>> items, Map<String, String> query) {
        int page = intValue(query.get("page"), 1);
        int pageSize = intValue(query.get("pageSize"), 20);
        int from = Math.min((page - 1) * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items.subList(from, to));
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("total", items.size());
        return data;
    }

    private static Comparator<CloudreveProvider> providerComparator(String sort) {
        if ("displayName_asc".equals(sort)) return Comparator.comparing(provider -> provider.displayName);
        if ("createdAt_desc".equals(sort)) return Comparator.comparing((CloudreveProvider provider) -> provider.createdAt).reversed();
        return Comparator.comparing((CloudreveProvider provider) -> provider.updatedAt).reversed();
    }

    private static Comparator<CloudreveFile> fileComparator(String sort) {
        if ("name_asc".equals(sort)) return Comparator.comparing(file -> file.name);
        if ("sizeBytes_desc".equals(sort)) return Comparator.comparing((CloudreveFile file) -> file.sizeBytes == null ? 0L : file.sizeBytes).reversed();
        return Comparator.comparing((CloudreveFile file) -> file.lastSyncedAt).reversed();
    }

    private static Comparator<CloudreveShare> shareComparator(String sort) {
        if ("expiresAt_asc".equals(sort)) return Comparator.comparing(share -> Optional.ofNullable(share.expiresAt).orElse(""));
        if ("createdAt_desc".equals(sort)) return Comparator.comparing((CloudreveShare share) -> share.createdAt).reversed();
        return Comparator.comparing((CloudreveShare share) -> share.lastCheckedAt).reversed();
    }

    private static Comparator<CloudreveJob> jobComparator(String sort) {
        if ("finishedAt_desc".equals(sort)) return Comparator.comparing((CloudreveJob job) -> Optional.ofNullable(job.finishedAt).orElse("")).reversed();
        if ("updatedAt_desc".equals(sort)) return Comparator.comparing((CloudreveJob job) -> job.updatedAt).reversed();
        return Comparator.comparing((CloudreveJob job) -> job.createdAt).reversed();
    }

    private static Comparator<CloudreveAudit> auditComparator(String sort) {
        Comparator<CloudreveAudit> comparator = Comparator.comparing(audit -> audit.createdAt);
        return "createdAt_asc".equals(sort) ? comparator : comparator.reversed();
    }

    private static boolean matches(String value, String keyword) {
        return keyword == null || (value != null && value.toLowerCase().contains(keyword.toLowerCase()));
    }

    private static boolean blank(Object value) {
        return text(value).isBlank();
    }

    private static String baseSummary(String baseUrl) {
        return baseUrl.replaceFirst("^https?://", "").replaceAll("/.*$", "");
    }

    private static Boolean parseBoolean(String value) {
        if ("true".equals(value) || "false".equals(value)) return Boolean.parseBoolean(value);
        throw new CloudreveException(HttpStatus.BAD_REQUEST, 40001, "invalid boolean");
    }

    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) return list.stream().map(String::valueOf).toList();
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) return (Map<String, Object>) map;
        throw new CloudreveException(HttpStatus.BAD_REQUEST, 40001, "invalid object");
    }

    private static int intValue(Object value, int fallback) {
        if (value == null || text(value).isBlank()) return fallback;
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(value.toString());
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String now() {
        return Instant.now().toString();
    }

    private static ResponseEntity<Map<String, Object>> ok(HttpServletRequest request, Object data) {
        return respond(request, HttpStatus.OK, 0, "success", data);
    }

    private static ResponseEntity<Map<String, Object>> created(HttpServletRequest request, Object data) {
        return respond(request, HttpStatus.CREATED, 0, "success", data);
    }

    private static ResponseEntity<Map<String, Object>> respond(HttpServletRequest request, org.springframework.http.HttpStatusCode status, int code, String message, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", data);
        body.put("requestId", request.getAttribute("requestId"));
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", String.valueOf(request.getAttribute("requestId")));
        return new ResponseEntity<>(body, headers, status);
    }

    @FunctionalInterface
    interface ResponseSupplier {
        ResponseEntity<Map<String, Object>> get();
    }
}

@Service
class CloudreveStore {
    final Map<String, CloudreveProvider> providers = new ConcurrentHashMap<>();
    final Map<String, CloudreveFile> files = new ConcurrentHashMap<>();
    final Map<String, CloudreveShare> shares = new ConcurrentHashMap<>();
    final Map<String, CloudreveJob> jobs = new ConcurrentHashMap<>();
    final List<CloudreveAudit> audits = new ArrayList<>();
    final Map<String, CloudreveIdempotency> idempotency = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private int sequence = 1000;

    CloudreveStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        seed();
    }

    void seed() {
        CloudreveProvider provider = new CloudreveProvider("provider-main", "Main Cloudreve", "cloud.example.com", "TEST_FAKE", "ENABLED",
                List.of("FILE_LIST", "FILE_METADATA", "SHARE_RESOLVE", "SHARE_REFRESH"), 5000, "system");
        providers.put(provider.providerId, provider);
        CloudreveFile client = new CloudreveFile("file-client-pack", "provider-main", "/packs", "client.zip", "FILE", "ACTIVE", "res-public-client", "share-client-pack");
        CloudreveFile map = new CloudreveFile("file-map-pack", "provider-main", "/packs", "map.zip", "FILE", "ACTIVE", "res-member-map", "share-map-pack");
        CloudreveFile noShare = new CloudreveFile("file-no-share", "provider-main", "/packs", "orphan.zip", "FILE", "ACTIVE", "res-orphan", null);
        CloudreveFile folderOther = new CloudreveFile("file-folder-other", "provider-main", "/packs-other", "item.zip", "FILE", "ACTIVE", "res-other", null);
        files.put(client.fileId, client);
        files.put(map.fileId, map);
        files.put(noShare.fileId, noShare);
        files.put(folderOther.fileId, folderOther);
        shares.put("share-client-pack", new CloudreveShare("share-client-pack", "provider-main", "file-client-pack", "client-share", "https://cloud.example.com/s/client", "ACTIVE", false, true));
        shares.put("share-map-pack", new CloudreveShare("share-map-pack", "provider-main", "file-map-pack", "map-share", "https://cloud.example.com/s/map", "PASSWORD_REQUIRED", true, true));
    }

    int nextId() {
        return ++sequence;
    }

    CloudreveProvider provider(String providerId) {
        CloudreveProvider provider = providers.get(providerId);
        if (provider == null) throw new CloudreveException(HttpStatus.NOT_FOUND, 49700, "provider not found");
        return provider;
    }

    CloudreveFile file(String fileId) {
        CloudreveFile file = files.get(fileId);
        if (file == null) throw new CloudreveException(HttpStatus.NOT_FOUND, 49701, "file not found");
        return file;
    }

    CloudreveJob job(String jobId) {
        CloudreveJob job = jobs.get(jobId);
        if (job == null) throw new CloudreveException(HttpStatus.NOT_FOUND, 49703, "sync job not found");
        return job;
    }

    void failAuditIfRequested(HttpServletRequest request, boolean testControlsEnabled) {
        if (testControlsEnabled && "true".equals(request.getHeader("X-Test-Fail-Audit"))) {
            throw new CloudreveException(HttpStatus.INTERNAL_SERVER_ERROR, 55301, "audit write failed");
        }
    }

    void audit(String action, String targetType, String targetId, Actor actor, HttpServletRequest request, Map<String, Object> body,
               String riskLevel, String result, String failureReason, String beforeState, String afterState) {
        audits.add(new CloudreveAudit("audit-" + nextId(), action, targetType, targetId, actor.userId, actor.role, actor.permissions,
                riskLevel, result, text(body == null ? null : body.get("reason")), beforeState, afterState, failureReason,
                String.valueOf(request.getAttribute("requestId"))));
    }

    Map<String, Object> summary(boolean testControlsEnabled) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "cloudreve-sync");
        data.put("port", 8118);
        data.put("storageMode", "IN_MEMORY");
        data.put("authMode", "TEST_STUB");
        data.put("providerAdapterMode", "TEST_FAKE");
        data.put("resourceAdapterMode", "TEST_STUB");
        data.put("opsAssetAdapterMode", "TEST_STUB");
        data.put("testControlsEnabled", testControlsEnabled);
        data.put("providersTotal", providers.size());
        data.put("filesTotal", files.size());
        data.put("sharesTotal", shares.size());
        data.put("jobsTotal", jobs.size());
        data.put("runningJobsTotal", jobs.values().stream().filter(job -> List.of("PENDING", "RUNNING").contains(job.status)).count());
        data.put("failedJobsTotal", jobs.values().stream().filter(job -> "FAILED".equals(job.status)).count());
        data.put("auditsTotal", audits.size());
        data.put("idempotencyRecordsTotal", idempotency.size());
        data.put("lastSyncAt", jobs.values().stream().map(job -> job.finishedAt).filter(Objects::nonNull).findFirst().orElse(null));
        data.put("lastFailureAt", null);
        data.put("degraded", false);
        data.put("degradeReasons", List.of());
        data.put("productionGaps", testControlsEnabled
                ? List.of("TEST_CONTROLS_ENABLED_FOR_LOCAL_TESTS", "REAL_CLOUDREVE_API_DISABLED", "IN_MEMORY_STORAGE")
                : List.of("TEST_CONTROLS_DISABLED_OUTSIDE_TEST", "REAL_CLOUDREVE_API_DISABLED", "IN_MEMORY_STORAGE"));
        return data;
    }

    String fingerprint(Object body) {
        try {
            return objectMapper.writeValueAsString(canonical(body));
        } catch (JsonProcessingException exception) {
            return String.valueOf(body);
        }
    }

    Object canonical(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) sorted.put(String.valueOf(entry.getKey()), canonical(entry.getValue()));
            return sorted;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> values = new ArrayList<>();
            for (Object item : iterable) values.add(canonical(item));
            return values;
        }
        return value;
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }
}

class CloudreveProvider {
    final String providerId;
    String displayName;
    String baseUrlSummary;
    String authMode;
    String status;
    List<String> capabilities;
    int timeoutMs;
    final Map<String, Object> opsAssetRef = Map.of("assetId", "asset-cloudreve-main", "source", "ops-control");
    String lastHealthStatus = "AVAILABLE";
    String lastCheckedAt = now();
    String lastSyncJobId;
    final String createdBy;
    String updatedBy;
    final String createdAt = now();
    String updatedAt = createdAt;

    CloudreveProvider(String providerId, String displayName, String baseUrlSummary, String authMode, String status, List<String> capabilities, int timeoutMs, String createdBy) {
        this.providerId = providerId;
        this.displayName = displayName;
        this.baseUrlSummary = baseUrlSummary;
        this.authMode = authMode;
        this.status = status;
        this.capabilities = capabilities.isEmpty() ? List.of("FILE_LIST") : capabilities;
        this.timeoutMs = timeoutMs;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    Map<String, Object> summary() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("providerId", providerId);
        view.put("displayName", displayName);
        view.put("baseUrlSummary", baseUrlSummary);
        view.put("authMode", authMode);
        view.put("status", status);
        view.put("capabilities", capabilities);
        view.put("lastHealthStatus", lastHealthStatus);
        view.put("lastCheckedAt", lastCheckedAt);
        view.put("lastSyncJobId", lastSyncJobId);
        view.put("degraded", "DEGRADED".equals(status) || "UNAVAILABLE".equals(status));
        view.put("degradeReasons", List.of());
        view.put("createdAt", createdAt);
        view.put("updatedAt", updatedAt);
        return view;
    }

    Map<String, Object> view() {
        Map<String, Object> view = summary();
        view.put("timeoutMs", timeoutMs);
        view.put("opsAssetRef", opsAssetRef);
        view.put("createdBy", createdBy);
        view.put("updatedBy", updatedBy);
        return view;
    }

    private static String now() {
        return Instant.now().toString();
    }
}

class CloudreveFile {
    final String fileId;
    final String providerId;
    final String parentPath;
    final String name;
    final String type;
    String status;
    final String resourceId;
    String shareSnapshotId;
    final Long sizeBytes = 1024L;
    final String lastSyncedAt = Instant.now().toString();

    CloudreveFile(String fileId, String providerId, String parentPath, String name, String type, String status, String resourceId, String shareSnapshotId) {
        this.fileId = fileId;
        this.providerId = providerId;
        this.parentPath = parentPath;
        this.name = name;
        this.type = type;
        this.status = status;
        this.resourceId = resourceId;
        this.shareSnapshotId = shareSnapshotId;
    }

    Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("fileId", fileId);
        view.put("providerId", providerId);
        view.put("cloudreveUriSummary", "cloudreve://provider/" + fileId);
        view.put("parentPath", parentPath);
        view.put("name", name);
        view.put("type", type);
        view.put("status", status);
        view.put("sizeBytes", sizeBytes);
        view.put("mimeType", "application/octet-stream");
        view.put("checksumSha256", "a".repeat(64));
        view.put("etag", "etag-" + fileId);
        view.put("resourceRef", resourceId == null ? null : Map.of("resourceId", resourceId, "source", "resource"));
        view.put("shareSnapshotId", shareSnapshotId);
        view.put("lastSyncedAt", lastSyncedAt);
        view.put("stale", false);
        view.put("degraded", false);
        view.put("degradeReasons", List.of());
        return view;
    }
}

class CloudreveShare {
    final String shareSnapshotId;
    final String providerId;
    final String fileId;
    final String shareId;
    final String shareUrlSummary;
    String status;
    final boolean passwordRequired;
    final boolean downloadAvailable;
    final String createdAt = Instant.now().toString();
    String expiresAt = "2026-12-31T00:00:00Z";
    String lastResolvedAt;
    String lastCheckedAt = createdAt;

    CloudreveShare(String shareSnapshotId, String providerId, String fileId, String shareId, String shareUrlSummary, String status, boolean passwordRequired, boolean downloadAvailable) {
        this.shareSnapshotId = shareSnapshotId;
        this.providerId = providerId;
        this.fileId = fileId;
        this.shareId = shareId;
        this.shareUrlSummary = shareUrlSummary;
        this.status = status;
        this.passwordRequired = passwordRequired;
        this.downloadAvailable = downloadAvailable;
    }

    Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("shareSnapshotId", shareSnapshotId);
        view.put("providerId", providerId);
        view.put("fileId", fileId);
        view.put("shareId", shareId);
        view.put("shareUrlSummary", shareUrlSummary);
        view.put("status", status);
        view.put("passwordRequired", passwordRequired);
        view.put("passwordStored", passwordRequired);
        view.put("expiresAt", expiresAt);
        view.put("lastResolvedAt", lastResolvedAt);
        view.put("lastCheckedAt", lastCheckedAt);
        view.put("downloadAvailable", downloadAvailable);
        view.put("stale", false);
        view.put("degraded", false);
        view.put("degradeReasons", List.of());
        view.put("createdAt", createdAt);
        return view;
    }
}

class CloudreveJob {
    final String jobId;
    final String jobType;
    String status;
    final String trigger;
    final String providerId;
    final Map<String, Object> target;
    final String idempotencyKey;
    final String createdBy;
    final String createdAt = Instant.now().toString();
    String updatedAt = createdAt;
    String startedAt = createdAt;
    String finishedAt;
    Map<String, Object> resultSummary;
    String failureReason;

    CloudreveJob(String jobId, String jobType, String status, String trigger, String providerId, Map<String, Object> target, String idempotencyKey, String createdBy) {
        this.jobId = jobId;
        this.jobType = jobType;
        this.status = status;
        this.trigger = trigger.isBlank() ? "ADMIN_MANUAL" : trigger;
        this.providerId = providerId;
        this.target = target;
        this.idempotencyKey = idempotencyKey;
        this.createdBy = createdBy;
        this.finishedAt = "SUCCEEDED".equals(status) ? createdAt : null;
    }

    Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("stepId", jobId + "-step-1");
        step.put("name", jobType);
        step.put("status", status);
        step.put("dependencyStatus", "AVAILABLE");
        step.put("startedAt", startedAt);
        step.put("finishedAt", finishedAt);
        step.put("message", "fake adapter completed");
        step.put("sanitizedPayloadSummary", Map.of("providerId", providerId));
        view.put("jobId", jobId);
        view.put("jobType", jobType);
        view.put("status", status);
        view.put("trigger", trigger);
        view.put("providerId", providerId);
        view.put("target", target);
        view.put("idempotencyKey", idempotencyKey);
        view.put("steps", List.of(step));
        view.put("resultSummary", resultSummary);
        view.put("failureReason", failureReason);
        view.put("createdBy", createdBy);
        view.put("createdAt", createdAt);
        view.put("startedAt", startedAt);
        view.put("finishedAt", finishedAt);
        view.put("updatedAt", updatedAt);
        return view;
    }
}

class CloudreveAudit {
    final String id;
    final String action;
    final String targetType;
    final String targetId;
    final String actorUserId;
    final String actorRole;
    final List<String> actorPermissions;
    final String riskLevel;
    final String result;
    final String reason;
    final String beforeState;
    final String afterState;
    final String failureReason;
    final String requestId;
    final String createdAt = Instant.now().toString();
    final String providerId;
    final String fileId;
    final String shareSnapshotId;
    final String jobId;

    CloudreveAudit(String id, String action, String targetType, String targetId, String actorUserId, String actorRole,
                   List<String> actorPermissions, String riskLevel, String result, String reason, String beforeState,
                   String afterState, String failureReason, String requestId) {
        this.id = id;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.actorUserId = actorUserId;
        this.actorRole = actorRole;
        this.actorPermissions = actorPermissions;
        this.riskLevel = riskLevel;
        this.result = result;
        this.reason = reason;
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.failureReason = failureReason;
        this.requestId = requestId;
        this.providerId = "PROVIDER".equals(targetType) ? targetId : "provider-main";
        this.fileId = "FILE".equals(targetType) ? targetId : null;
        this.shareSnapshotId = "SHARE".equals(targetType) ? targetId : null;
        this.jobId = "SYNC_JOB".equals(targetType) ? targetId : null;
    }

    Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", id);
        view.put("requestId", requestId);
        view.put("actorUserId", actorUserId);
        view.put("actorRole", actorRole);
        view.put("actorPermissions", actorPermissions);
        view.put("sourceIp", null);
        view.put("targetType", targetType);
        view.put("targetId", targetId);
        view.put("action", action);
        view.put("riskLevel", riskLevel);
        view.put("reason", reason);
        view.put("paramsSummary", Map.of("sanitized", true));
        view.put("beforeState", beforeState);
        view.put("afterState", afterState);
        view.put("result", result);
        view.put("failureReason", failureReason);
        view.put("providerId", providerId);
        view.put("fileId", fileId);
        view.put("shareSnapshotId", shareSnapshotId);
        view.put("jobId", jobId);
        view.put("dependencyStatus", "AVAILABLE");
        view.put("createdAt", createdAt);
        return view;
    }
}

class CloudreveIdempotency {
    final String fingerprint;
    final org.springframework.http.HttpStatusCode status;
    final Object data;

    CloudreveIdempotency(String fingerprint, org.springframework.http.HttpStatusCode status, Object data) {
        this.fingerprint = fingerprint;
        this.status = status;
        this.data = data;
    }
}

class Actor {
    final String userId;
    final String displayName;
    final String role;
    final List<String> permissions;

    Actor(String userId, String displayName, String role, List<String> permissions) {
        this.userId = userId;
        this.displayName = displayName;
        this.role = role;
        this.permissions = permissions;
    }
}

@Service
class CloudreveAuth {
    private final CloudreveProperties properties;

    CloudreveAuth(CloudreveProperties properties) {
        this.properties = properties;
    }

    Actor current(HttpServletRequest request) {
        if (properties.enabled()) {
            String mode = request.getHeader("X-Test-Auth-Mode");
            if ("unavailable".equals(mode)) throw new CloudreveException(HttpStatus.BAD_GATEWAY, 46710, "auth unavailable");
            if ("timeout".equals(mode)) throw new CloudreveException(HttpStatus.GATEWAY_TIMEOUT, 46711, "auth timeout");
            if ("bad-schema".equals(mode)) throw new CloudreveException(HttpStatus.BAD_GATEWAY, 46712, "auth bad schema");
        }
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) throw new CloudreveException(HttpStatus.UNAUTHORIZED, 41000, "unauthenticated");
        if (!header.startsWith("Bearer ")) throw new CloudreveException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        return switch (header.substring("Bearer ".length())) {
            case "auth-unavailable-token" -> throw new CloudreveException(HttpStatus.BAD_GATEWAY, 46710, "auth unavailable");
            case "auth-timeout-token" -> throw new CloudreveException(HttpStatus.GATEWAY_TIMEOUT, 46711, "auth timeout");
            case "auth-bad-token" -> throw new CloudreveException(HttpStatus.BAD_GATEWAY, 46712, "auth bad schema");
            case "sync-viewer-token" -> new Actor("sync-viewer-user", "Sync Viewer", "HELPER", List.of("NODE_READ"));
            case "sync-no-cap-token" -> new Actor("sync-no-cap-user", "No Cap", "ADMIN", List.of());
            case "sync-admin-token" -> new Actor("sync-admin-user", "Sync Admin", "ADMIN", List.of("NODE_READ", "NODE_WRITE", "FILE_MANAGE"));
            case "sync-file-token" -> new Actor("sync-file-user", "Sync File", "ADMIN", List.of("NODE_READ", "FILE_MANAGE"));
            case "owner-token" -> new Actor("owner-user", "Owner", "OWNER", List.of("NODE_READ", "NODE_WRITE", "FILE_MANAGE", "HIGH_RISK_APPROVE"));
            case "user-token" -> new Actor("plain-user", "Plain User", "USER", List.of());
            default -> throw new CloudreveException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        };
    }

    Actor requireAnyCapability(HttpServletRequest request, String... capabilities) {
        return requireAnyCapability(current(request), capabilities);
    }

    Actor requireAnyCapability(Actor actor, String... capabilities) {
        if ("USER".equals(actor.role)) throw new CloudreveException(HttpStatus.FORBIDDEN, 42001, "role denied");
        if (List.of(capabilities).stream().noneMatch(actor.permissions::contains)) {
            throw new CloudreveException(HttpStatus.FORBIDDEN, 42002, "capability denied");
        }
        return actor;
    }

    void requireAdmin(Actor actor) {
        if (!List.of("ADMIN", "OWNER").contains(actor.role)) throw new CloudreveException(HttpStatus.FORBIDDEN, 42001, "role denied");
    }
}

@Component
class CloudreveProperties {
    private final boolean enabled;

    CloudreveProperties(@Value("${cloudreve-sync.test-controls.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    boolean enabled() {
        return enabled;
    }
}

@Component
class CloudreveRequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = Optional.ofNullable(request.getHeader("X-Request-Id")).filter(value -> !value.isBlank()).orElse("req_" + UUID.randomUUID());
        request.setAttribute("requestId", requestId);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Request-Id", requestId);
        filterChain.doFilter(request, response);
    }
}

@RestControllerAdvice
class CloudreveExceptionHandler {
    @ExceptionHandler(CloudreveException.class)
    ResponseEntity<Map<String, Object>> api(CloudreveException exception, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", exception.code);
        body.put("message", exception.getMessage());
        body.put("data", null);
        body.put("requestId", request.getAttribute("requestId"));
        return ResponseEntity.status(exception.status).body(body);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> fallback(Exception exception, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 55300);
        body.put("message", "cloudreve-sync internal error");
        body.put("data", null);
        body.put("requestId", request.getAttribute("requestId"));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

class CloudreveException extends RuntimeException {
    final HttpStatus status;
    final int code;

    CloudreveException(HttpStatus status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
