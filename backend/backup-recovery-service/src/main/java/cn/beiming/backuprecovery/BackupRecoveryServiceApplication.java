package cn.beiming.backuprecovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
import java.time.temporal.ChronoUnit;
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
public class BackupRecoveryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackupRecoveryServiceApplication.class, args);
    }
}

@RestController
@RequestMapping("/api/v1/backup-recovery")
class BackupRecoveryController {
    private static final String VERSION = "0.1.0-contract";
    private final BackupRecoveryStore store;
    private final BackupRecoveryAuth auth;
    private final BackupRecoveryProperties properties;

    BackupRecoveryController(BackupRecoveryStore store, BackupRecoveryAuth auth, BackupRecoveryProperties properties) {
        this.store = store;
        this.auth = auth;
        this.properties = properties;
    }

    @GetMapping("/health")
    ResponseEntity<Map<String, Object>> health(HttpServletRequest request) {
        return ok(request, Map.of("service", "backup-recovery", "status", "READY", "version", VERSION));
    }

    @GetMapping("/ops/summary")
    ResponseEntity<Map<String, Object>> summary(HttpServletRequest request) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        if (properties.enabled() && "true".equals(request.getHeader("X-Test-Fail-Store"))) {
            throw new BackupRecoveryException(HttpStatus.INTERNAL_SERVER_ERROR, 55400, "backup-recovery internal error");
        }
        return ok(request, store.summary(properties.enabled()));
    }

    @GetMapping("/domains")
    ResponseEntity<Map<String, Object>> domains(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "displayName_asc", "criticality_desc");
        Boolean enabled = query.get("enabled") == null ? null : Boolean.parseBoolean(query.get("enabled"));
        return ok(request, page(store.domains.values().stream()
                .filter(domain -> matches(domain.displayName, query.get("keyword")) || matches(domain.domainKey, query.get("keyword")))
                .filter(domain -> query.get("sourceService") == null || domain.sourceService.equals(query.get("sourceService")))
                .filter(domain -> query.get("criticality") == null || domain.criticality.equals(query.get("criticality")))
                .filter(domain -> enabled == null || domain.enabled == enabled)
                .sorted(domainComparator(query.get("sort")))
                .map(BackupDomain::view)
                .toList(), query));
    }

    @GetMapping("/policies")
    ResponseEntity<Map<String, Object>> policies(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        validatePage(query);
        validateSort(query.get("sort"), "updatedAt_desc", "createdAt_desc", "displayName_asc");
        return ok(request, page(store.policies.values().stream()
                .filter(policy -> matches(policy.displayName, query.get("keyword")) || matches(policy.policyId, query.get("keyword")))
                .filter(policy -> query.get("status") == null || policy.status.equals(query.get("status")))
                .filter(policy -> query.get("domain") == null || policy.domains.contains(query.get("domain")))
                .sorted(policyComparator(query.get("sort")))
                .map(BackupPolicy::view)
                .toList(), query));
    }

    @GetMapping("/policies/{policyId}")
    ResponseEntity<Map<String, Object>> policy(HttpServletRequest request, @PathVariable String policyId) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        Map<String, Object> view = store.policy(policyId).view();
        view.put("recentJob", store.jobs.values().stream().filter(job -> job.policyId.equals(policyId)).findFirst().map(BackupJob::view).orElse(null));
        view.put("recentBackupPoint", store.points.values().stream().filter(point -> point.policyId.equals(policyId)).findFirst().map(BackupPoint::view).orElse(null));
        return ok(request, view);
    }

    @PostMapping("/policies")
    ResponseEntity<Map<String, Object>> createPolicy(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "NODE_WRITE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validatePolicyBody(body, true);
        return idempotent(request, actor, "policy:create", body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                String displayName = text(body.get("displayName"));
                if (store.policies.values().stream().anyMatch(policy -> policy.displayName.equalsIgnoreCase(displayName))) {
                    throw new BackupRecoveryException(HttpStatus.CONFLICT, 49811, "backup policy conflict");
                }
                String policyId = "policy-" + store.nextId();
                BackupPolicy policy = new BackupPolicy(policyId, displayName, stringList(body.get("domains")), objectMap(body.get("scheduleSummary")),
                        intValue(body.get("retentionDays"), 30), intValue(body.get("minimumCopies"), 2), safeStorageRef(body.get("storageRef")),
                        text(body.get("encryptionMode")), "DRAFT", actor.userId);
                store.policies.put(policyId, policy);
                store.audit("BACKUP_POLICY_CREATED", "POLICY", policyId, actor, request, body, "MEDIUM", "SUCCESS", null, null, policy.status);
                return created(request, policy.view());
            }
        });
    }

    @PatchMapping("/policies/{policyId}")
    ResponseEntity<Map<String, Object>> patchPolicy(HttpServletRequest request, @PathVariable String policyId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "NODE_WRITE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "policy:patch:" + policyId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                BackupPolicy policy = store.policy(policyId);
                if ("ARCHIVED".equals(policy.status)) throw new BackupRecoveryException(HttpStatus.CONFLICT, 49810, "policy state conflict");
                String before = policy.status;
                if (body.containsKey("displayName")) policy.displayName = text(body.get("displayName"));
                if (body.containsKey("domains")) {
                    List<String> domains = stringList(body.get("domains"));
                    validateDomains(domains);
                    policy.domains = domains;
                }
                if (body.containsKey("scheduleSummary")) policy.scheduleSummary = objectMap(body.get("scheduleSummary"));
                if (body.containsKey("retentionDays")) {
                    int retention = intValue(body.get("retentionDays"), policy.retentionDays);
                    if (retention < 1 || retention > 3650) throw new BackupRecoveryException(HttpStatus.BAD_REQUEST, 40001, "invalid retention");
                    policy.retentionDays = retention;
                }
                if (body.containsKey("minimumCopies")) {
                    int copies = intValue(body.get("minimumCopies"), policy.minimumCopies);
                    if (copies < 1 || copies > 30) throw new BackupRecoveryException(HttpStatus.BAD_REQUEST, 40001, "invalid minimum copies");
                    policy.minimumCopies = copies;
                }
                if (body.containsKey("storageRef")) policy.storageRef = safeStorageRef(body.get("storageRef"));
                if (body.containsKey("encryptionMode")) policy.encryptionMode = text(body.get("encryptionMode"));
                policy.updatedBy = actor.userId;
                policy.updatedAt = now();
                store.audit("BACKUP_POLICY_UPDATED", "POLICY", policyId, actor, request, body, "MEDIUM", "SUCCESS", null, before, policy.status);
                return ok(request, policy.view());
            }
        });
    }

    @PatchMapping("/policies/{policyId}/enable")
    ResponseEntity<Map<String, Object>> enablePolicy(HttpServletRequest request, @PathVariable String policyId, @RequestBody Map<String, Object> body) {
        return policyStatus(request, policyId, body, "ENABLED", "BACKUP_POLICY_ENABLED");
    }

    @PatchMapping("/policies/{policyId}/disable")
    ResponseEntity<Map<String, Object>> disablePolicy(HttpServletRequest request, @PathVariable String policyId, @RequestBody Map<String, Object> body) {
        return policyStatus(request, policyId, body, "DISABLED", "BACKUP_POLICY_DISABLED");
    }

    private ResponseEntity<Map<String, Object>> policyStatus(HttpServletRequest request, String policyId, Map<String, Object> body, String status, String action) {
        Actor actor = auth.requireAnyCapability(request, "NODE_WRITE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "policy:" + status + ":" + policyId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                BackupPolicy policy = store.policy(policyId);
                if ("ARCHIVED".equals(policy.status)) throw new BackupRecoveryException(HttpStatus.CONFLICT, 49810, "policy state conflict");
                String before = policy.status;
                policy.status = status;
                policy.updatedBy = actor.userId;
                policy.updatedAt = now();
                store.audit(action, "POLICY", policyId, actor, request, body, "MEDIUM", "SUCCESS", null, before, status);
                return ok(request, policy.view());
            }
        });
    }

    @PostMapping("/jobs")
    ResponseEntity<Map<String, Object>> createJob(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "NODE_WRITE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        if (properties.enabled()) {
            String opsMode = request.getHeader("X-Test-Ops-Control-Mode");
            if (body.get("opsControlTaskRef") != null && "unavailable".equals(opsMode)) throw new BackupRecoveryException(HttpStatus.BAD_GATEWAY, 46820, "ops-control unavailable");
            if (body.get("opsControlTaskRef") != null && "timeout".equals(opsMode)) throw new BackupRecoveryException(HttpStatus.GATEWAY_TIMEOUT, 46821, "ops-control timeout");
            if (body.get("opsControlTaskRef") != null && "bad-schema".equals(opsMode)) throw new BackupRecoveryException(HttpStatus.BAD_GATEWAY, 46822, "ops-control bad schema");
        }
        return idempotent(request, actor, "job:create", body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                BackupPolicy policy = store.policy(text(body.get("policyId")));
                if (!"ENABLED".equals(policy.status)) throw new BackupRecoveryException(HttpStatus.CONFLICT, 49810, "policy disabled");
                List<String> domains = body.containsKey("domains") ? stringList(body.get("domains")) : policy.domains;
                validateDomains(domains);
                String jobId = "job-" + store.nextId();
                String mode = properties.enabled() ? request.getHeader("X-Test-Backup-Mode") : null;
                String status = switch (String.valueOf(mode)) {
                    case "failed" -> "FAILED";
                    case "timeout" -> "TIMEOUT";
                    case "pending", "pending-approval" -> "PENDING";
                    default -> "SUCCEEDED";
                };
                BackupJob job = new BackupJob(jobId, policy.policyId, textOr(body.get("trigger"), "ADMIN_MANUAL"), status, domains, text(body.get("idempotencyKey")), actor.userId);
                store.jobs.put(jobId, job);
                policy.lastRunStatus = status;
                policy.updatedAt = now();
                if ("SUCCEEDED".equals(status)) {
                    String pointId = "point-" + store.nextId();
                    BackupPoint point = new BackupPoint(pointId, policy.policyId, jobId, domains, policy.storageRef, policy.encryptionMode);
                    store.points.put(pointId, point);
                    job.resultSummary = Map.of("backupPointId", pointId, "domainsTotal", domains.size(), "sizeBytes", point.sizeBytes);
                    domains.forEach(domainKey -> {
                        BackupDomain domain = store.domains.get(domainKey);
                        if (domain != null) domain.lastBackupPointId = pointId;
                    });
                } else {
                    job.failureReason = status.equals("FAILED") ? "SIMULATED_BACKUP_FAILURE" : null;
                }
                store.audit("BACKUP_JOB_CREATED", "JOB", jobId, actor, request, body, "HIGH", "SUCCESS", null, null, status);
                return created(request, job.view());
            }
        });
    }

    @GetMapping("/jobs")
    ResponseEntity<Map<String, Object>> jobs(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        validatePage(query);
        validateSort(query.get("sort"), "createdAt_desc", "updatedAt_desc", "finishedAt_desc");
        validateTimeRange(query);
        return ok(request, page(store.jobs.values().stream()
                .filter(job -> query.get("policyId") == null || job.policyId.equals(query.get("policyId")))
                .filter(job -> query.get("status") == null || job.status.equals(query.get("status")))
                .filter(job -> query.get("trigger") == null || job.trigger.equals(query.get("trigger")))
                .filter(job -> query.get("createdBy") == null || job.createdBy.equals(query.get("createdBy")))
                .filter(job -> inRange(job.createdAt, query))
                .sorted(jobComparator(query.get("sort")))
                .map(BackupJob::view)
                .toList(), query));
    }

    @GetMapping("/jobs/{jobId}")
    ResponseEntity<Map<String, Object>> job(HttpServletRequest request, @PathVariable String jobId) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        return ok(request, store.job(jobId).view());
    }

    @PatchMapping("/jobs/{jobId}/cancel")
    ResponseEntity<Map<String, Object>> cancelJob(HttpServletRequest request, @PathVariable String jobId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.current(request);
        if (!List.of("ADMIN", "OWNER").contains(actor.role)) throw new BackupRecoveryException(HttpStatus.FORBIDDEN, 42001, "role denied");
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "job:cancel:" + jobId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                BackupJob job = store.job(jobId);
                if (!List.of("PENDING", "RUNNING", "PENDING_APPROVAL").contains(job.status)) {
                    throw new BackupRecoveryException(HttpStatus.CONFLICT, 49810, "job state conflict");
                }
                String before = job.status;
                job.status = "CANCELLED";
                job.finishedAt = now();
                job.updatedAt = job.finishedAt;
                store.audit("BACKUP_JOB_CANCELLED", "JOB", jobId, actor, request, body, "MEDIUM", "SUCCESS", null, before, job.status);
                return ok(request, job.view());
            }
        });
    }

    @GetMapping("/backup-points")
    ResponseEntity<Map<String, Object>> points(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        validatePage(query);
        validateSort(query.get("sort"), "createdAt_desc", "expiresAt_asc", "sizeBytes_desc");
        validateTimeRange(query);
        Boolean verified = query.get("verified") == null ? null : Boolean.parseBoolean(query.get("verified"));
        return ok(request, page(store.points.values().stream()
                .filter(point -> query.get("policyId") == null || point.policyId.equals(query.get("policyId")))
                .filter(point -> query.get("jobId") == null || point.jobId.equals(query.get("jobId")))
                .filter(point -> query.get("domain") == null || point.domains.contains(query.get("domain")))
                .filter(point -> query.get("status") == null || point.status.equals(query.get("status")))
                .filter(point -> verified == null || point.verified == verified)
                .filter(point -> inRange(point.createdAt, query))
                .sorted(pointComparator(query.get("sort")))
                .map(BackupPoint::view)
                .toList(), query));
    }

    @GetMapping("/backup-points/{pointId}")
    ResponseEntity<Map<String, Object>> point(HttpServletRequest request, @PathVariable String pointId) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        return ok(request, store.point(pointId).view());
    }

    @PostMapping("/backup-points/{pointId}/verify")
    ResponseEntity<Map<String, Object>> verifyPoint(HttpServletRequest request, @PathVariable String pointId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "HIGH_RISK_APPROVE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "point:verify:" + pointId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                BackupPoint point = store.point(pointId);
                if (!List.of("AVAILABLE", "VERIFIED").contains(point.status)) throw new BackupRecoveryException(HttpStatus.CONFLICT, 49813, "backup point unavailable");
                String verificationId = "verification-" + store.nextId();
                boolean corrupt = properties.enabled() && "corrupt".equals(request.getHeader("X-Test-Backup-Mode"));
                BackupVerification verification = new BackupVerification(verificationId, pointId, corrupt ? "FAILED" : "PASSED", actor.userId);
                if (corrupt) {
                    point.status = "CORRUPTED";
                    point.verified = false;
                    verification.failureReason = "CHECKSUM_MISMATCH";
                } else {
                    point.status = "VERIFIED";
                    point.verified = true;
                    point.verifiedAt = now();
                    point.verifiedBy = actor.userId;
                    for (String domainKey : point.domains) {
                        BackupDomain domain = store.domains.get(domainKey);
                        if (domain != null) domain.lastVerifiedAt = point.verifiedAt;
                    }
                }
                store.verifications.put(verificationId, verification);
                store.audit("BACKUP_POINT_VERIFIED", "BACKUP_POINT", pointId, actor, request, body, "HIGH", "SUCCESS", null, null, point.status);
                return ok(request, verification.view());
            }
        });
    }

    @PostMapping("/restore-drills")
    ResponseEntity<Map<String, Object>> createDrill(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "HIGH_RISK_APPROVE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "drill:create", body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                BackupPoint point = store.point(text(body.get("backupPointId")));
                if (!List.of("AVAILABLE", "VERIFIED").contains(point.status)) throw new BackupRecoveryException(HttpStatus.CONFLICT, 49813, "backup point unavailable");
                List<String> domains = body.containsKey("domains") ? stringList(body.get("domains")) : point.domains;
                requireSubset(domains, point.domains);
                String mode = properties.enabled() ? request.getHeader("X-Test-Backup-Mode") : null;
                String drillId = "drill-" + store.nextId();
                RestoreDrill drill = new RestoreDrill(drillId, point.backupPointId, domains, "drill-failed".equals(mode) ? "FAILED" : "PASSED", actor.userId);
                store.drills.put(drillId, drill);
                store.audit("RESTORE_DRILL_CREATED", "RESTORE_DRILL", drillId, actor, request, body, "HIGH", "SUCCESS", null, null, drill.status);
                return created(request, drill.view());
            }
        });
    }

    @GetMapping("/restore-drills")
    ResponseEntity<Map<String, Object>> drills(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        validatePage(query);
        validateSort(query.get("sort"), "createdAt_desc", "finishedAt_desc");
        validateTimeRange(query);
        return ok(request, page(store.drills.values().stream()
                .filter(drill -> query.get("backupPointId") == null || drill.backupPointId.equals(query.get("backupPointId")))
                .filter(drill -> query.get("status") == null || drill.status.equals(query.get("status")))
                .filter(drill -> query.get("createdBy") == null || drill.createdBy.equals(query.get("createdBy")))
                .filter(drill -> inRange(drill.createdAt, query))
                .sorted(drillComparator(query.get("sort")))
                .map(RestoreDrill::view)
                .toList(), query));
    }

    @GetMapping("/restore-drills/{drillId}")
    ResponseEntity<Map<String, Object>> drill(HttpServletRequest request, @PathVariable String drillId) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        return ok(request, store.drill(drillId).view());
    }

    @PostMapping("/restore-requests")
    ResponseEntity<Map<String, Object>> createRestore(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "HIGH_RISK_APPROVE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        if (!"REQUEST_RESTORE_REVIEW".equals(text(body.get("confirmText")))) throw new BackupRecoveryException(HttpStatus.BAD_REQUEST, 40001, "confirm text mismatch");
        if (Boolean.TRUE.equals(objectMap(body.get("impactSummary")).get("writesProduction"))) {
            throw new BackupRecoveryException(HttpStatus.BAD_REQUEST, 40001, "production writes blocked");
        }
        return idempotent(request, actor, "restore:create", body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                BackupPoint point = store.point(text(body.get("backupPointId")));
                List<String> domains = stringList(body.get("domains"));
                requireSubset(domains, point.domains);
                String restoreMode = text(body.get("restoreMode"));
                String status = "PENDING_APPROVAL";
                if ("SANDBOX_RESTORE".equals(restoreMode)) {
                    RestoreDrill drill = body.get("drillId") == null ? null : store.drills.get(text(body.get("drillId")));
                    if (drill == null || !drill.backupPointId.equals(point.backupPointId) || !"PASSED".equals(drill.status)) {
                        throw new BackupRecoveryException(HttpStatus.CONFLICT, 49814, "restore drill required");
                    }
                }
                if ("FULL_RESTORE_BLOCKED".equals(restoreMode)) status = "EXECUTION_BLOCKED";
                String requestId = "restore-" + store.nextId();
                RestoreRequest restore = new RestoreRequest(requestId, point.backupPointId, domains, restoreMode, status, actor.userId, text(body.get("reason")));
                store.restores.put(requestId, restore);
                store.audit("RESTORE_REQUEST_CREATED", "RESTORE_REQUEST", requestId, actor, request, body, "CRITICAL", "SUCCESS", null, null, status);
                return created(request, restore.view());
            }
        });
    }

    @GetMapping("/restore-requests")
    ResponseEntity<Map<String, Object>> restoreRequests(HttpServletRequest request, @RequestParam Map<String, String> query) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        validatePage(query);
        validateSort(query.get("sort"), "createdAt_desc", "updatedAt_desc", "riskLevel_desc");
        validateTimeRange(query);
        return ok(request, page(store.restores.values().stream()
                .filter(restore -> query.get("backupPointId") == null || restore.backupPointId.equals(query.get("backupPointId")))
                .filter(restore -> query.get("status") == null || restore.status.equals(query.get("status")))
                .filter(restore -> query.get("requestedBy") == null || restore.requestedBy.equals(query.get("requestedBy")))
                .filter(restore -> query.get("riskLevel") == null || restore.riskLevel.equals(query.get("riskLevel")))
                .filter(restore -> inRange(restore.createdAt, query))
                .sorted(restoreComparator(query.get("sort")))
                .map(RestoreRequest::view)
                .toList(), query));
    }

    @GetMapping("/restore-requests/{restoreRequestId}")
    ResponseEntity<Map<String, Object>> restoreRequest(HttpServletRequest request, @PathVariable String restoreRequestId) {
        auth.requireAnyCapability(request, "NODE_READ", "HIGH_RISK_APPROVE");
        return ok(request, store.restore(restoreRequestId).view());
    }

    @PatchMapping("/restore-requests/{restoreRequestId}/approve")
    ResponseEntity<Map<String, Object>> approveRestore(HttpServletRequest request, @PathVariable String restoreRequestId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "HIGH_RISK_APPROVE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        if (!"APPROVE_SIMULATED_RESTORE".equals(text(body.get("confirmText")))) throw new BackupRecoveryException(HttpStatus.BAD_REQUEST, 40001, "confirm text mismatch");
        return idempotent(request, actor, "restore:approve:" + restoreRequestId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                RestoreRequest restore = store.restore(restoreRequestId);
                if (!"PENDING_APPROVAL".equals(restore.status)) throw new BackupRecoveryException(HttpStatus.CONFLICT, 49810, "restore state conflict");
                if ("CRITICAL".equals(restore.riskLevel) && restore.requestedBy.equals(actor.userId)) {
                    throw new BackupRecoveryException(HttpStatus.CONFLICT, 49810, "self approval denied");
                }
                String before = restore.status;
                restore.status = "COMPLETED_SIMULATED";
                restore.approvedBy = actor.userId;
                restore.updatedAt = now();
                restore.approvalSummary = Map.of("reviewComment", text(body.get("reviewComment")), "executionMode", "SIMULATED_ONLY", "reviewedAt", restore.updatedAt);
                store.audit("RESTORE_REQUEST_APPROVED", "RESTORE_REQUEST", restoreRequestId, actor, request, body, "CRITICAL", "SUCCESS", null, before, restore.status);
                return ok(request, restore.view());
            }
        });
    }

    @PatchMapping("/restore-requests/{restoreRequestId}/reject")
    ResponseEntity<Map<String, Object>> rejectRestore(HttpServletRequest request, @PathVariable String restoreRequestId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAnyCapability(request, "HIGH_RISK_APPROVE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateReason(body);
        return idempotent(request, actor, "restore:reject:" + restoreRequestId, body, () -> {
            store.failAuditIfRequested(request, properties.enabled());
            synchronized (store) {
                RestoreRequest restore = store.restore(restoreRequestId);
                if (!"PENDING_APPROVAL".equals(restore.status)) throw new BackupRecoveryException(HttpStatus.CONFLICT, 49810, "restore state conflict");
                String before = restore.status;
                restore.status = "REJECTED";
                restore.approvedBy = actor.userId;
                restore.updatedAt = now();
                restore.approvalSummary = Map.of("reviewComment", text(body.get("reviewComment")), "executionMode", "BLOCKED_BY_CONTRACT", "reviewedAt", restore.updatedAt);
                store.audit("RESTORE_REQUEST_REJECTED", "RESTORE_REQUEST", restoreRequestId, actor, request, body, "HIGH", "SUCCESS", null, before, restore.status);
                return ok(request, restore.view());
            }
        });
    }

    @GetMapping("/audit-logs")
    ResponseEntity<Map<String, Object>> audits(HttpServletRequest request, @RequestParam Map<String, String> query) {
        Actor actor = auth.current(request);
        if (!List.of("ADMIN", "OWNER").contains(actor.role)) throw new BackupRecoveryException(HttpStatus.FORBIDDEN, 42001, "role denied");
        validatePage(query);
        validateSort(query.get("sort"), "createdAt_desc", "createdAt_asc", "riskLevel_desc");
        validateTimeRange(query);
        return ok(request, page(store.audits.values().stream()
                .filter(audit -> query.get("actorUserId") == null || audit.actorUserId.equals(query.get("actorUserId")))
                .filter(audit -> query.get("policyId") == null || query.get("policyId").equals(audit.policyId))
                .filter(audit -> query.get("jobId") == null || query.get("jobId").equals(audit.jobId))
                .filter(audit -> query.get("backupPointId") == null || query.get("backupPointId").equals(audit.backupPointId))
                .filter(audit -> query.get("drillId") == null || query.get("drillId").equals(audit.drillId))
                .filter(audit -> query.get("restoreRequestId") == null || query.get("restoreRequestId").equals(audit.restoreRequestId))
                .filter(audit -> query.get("action") == null || audit.action.equals(query.get("action")))
                .filter(audit -> query.get("result") == null || audit.result.equals(query.get("result")))
                .filter(audit -> query.get("riskLevel") == null || audit.riskLevel.equals(query.get("riskLevel")))
                .filter(audit -> inRange(audit.createdAt, query))
                .sorted(auditComparator(query.get("sort")))
                .map(BackupAudit::view)
                .toList(), query));
    }

    private ResponseEntity<Map<String, Object>> ok(HttpServletRequest request, Object data) {
        return ResponseEntity.ok(envelope(request, data));
    }

    private ResponseEntity<Map<String, Object>> created(HttpServletRequest request, Object data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope(request, data));
    }

    private Map<String, Object> envelope(HttpServletRequest request, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 0);
        body.put("message", "success");
        body.put("data", data);
        body.put("requestId", request.getAttribute("requestId"));
        return body;
    }

    private ResponseEntity<Map<String, Object>> idempotent(HttpServletRequest request, Actor actor, String scope, Map<String, Object> body, Operation operation) {
        String key = text(body.get("idempotencyKey"));
        if (key.isBlank()) return operation.run();
        String storeKey = actor.userId + ":" + scope + ":" + key;
        String fingerprint = fingerprint(body);
        synchronized (store) {
            IdempotencyRecord existing = store.idempotency.get(storeKey);
            if (existing != null) {
                if (!existing.fingerprint.equals(fingerprint)) throw new BackupRecoveryException(HttpStatus.CONFLICT, 49812, "idempotency conflict");
                return ResponseEntity.status(existing.status).body(envelope(request, existing.data));
            }
            ResponseEntity<Map<String, Object>> response = operation.run();
            store.idempotency.put(storeKey, new IdempotencyRecord(fingerprint, response.getStatusCode(), response.getBody().get("data")));
            return response;
        }
    }

    private interface Operation {
        ResponseEntity<Map<String, Object>> run();
    }

    private static Map<String, Object> page(List<Map<String, Object>> items, Map<String, String> query) {
        int page = intValue(query.get("page"), 1);
        int pageSize = intValue(query.get("pageSize"), 20);
        int from = Math.min((page - 1) * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return Map.of("items", items.subList(from, to), "page", page, "pageSize", pageSize, "total", items.size());
    }

    private static void validatePage(Map<String, String> query) {
        int page = intValue(query.get("page"), 1);
        int pageSize = intValue(query.get("pageSize"), 20);
        if (page < 1 || pageSize < 1 || pageSize > 100) throw new BackupRecoveryException(HttpStatus.BAD_REQUEST, 40002, "invalid pagination");
    }

    private static void validateSort(String sort, String... allowed) {
        if (sort != null && List.of(allowed).stream().noneMatch(sort::equals)) throw new BackupRecoveryException(HttpStatus.BAD_REQUEST, 40003, "invalid sort");
    }

    private static void validateTimeRange(Map<String, String> query) {
        if (query.get("from") == null || query.get("to") == null) return;
        try {
            if (Instant.parse(query.get("from")).isAfter(Instant.parse(query.get("to")))) {
                throw new BackupRecoveryException(HttpStatus.BAD_REQUEST, 40001, "invalid time range");
            }
        } catch (BackupRecoveryException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BackupRecoveryException(HttpStatus.BAD_REQUEST, 40001, "invalid time range");
        }
    }

    private static boolean inRange(String createdAt, Map<String, String> query) {
        Instant created = Instant.parse(createdAt);
        if (query.get("from") != null && created.isBefore(Instant.parse(query.get("from")))) return false;
        return query.get("to") == null || !created.isAfter(Instant.parse(query.get("to")));
    }

    private static void validatePolicyBody(Map<String, Object> body, boolean create) {
        if (create && text(body.get("displayName")).isBlank()) throw new BackupRecoveryException(HttpStatus.BAD_REQUEST, 40001, "displayName required");
        List<String> domains = stringList(body.get("domains"));
        if (domains.isEmpty()) throw new BackupRecoveryException(HttpStatus.BAD_REQUEST, 40001, "domains required");
        validateDomains(domains);
        int retention = intValue(body.get("retentionDays"), 0);
        int copies = intValue(body.get("minimumCopies"), 0);
        if (retention < 1 || retention > 3650 || copies < 1 || copies > 30) throw new BackupRecoveryException(HttpStatus.BAD_REQUEST, 40001, "invalid retention");
        validateReason(body);
    }

    private static void validateDomains(List<String> domains) {
        if (domains.stream().anyMatch(domain -> !BackupRecoveryStore.KNOWN_DOMAINS.contains(domain))) {
            throw new BackupRecoveryException(HttpStatus.BAD_REQUEST, 40001, "unknown domain");
        }
    }

    private static void validateReason(Map<String, Object> body) {
        if (text(body.get("reason")).isBlank()) throw new BackupRecoveryException(HttpStatus.BAD_REQUEST, 40001, "reason required");
    }

    private static void rejectTrusted(Map<String, Object> body) {
        for (String key : body.keySet()) {
            String lower = key.toLowerCase();
            if (Set.of("actoruserid", "actorrole", "actorpermissions", "beforestate", "afterstate", "auditresult", "createdby", "updatedby", "verifiedby", "approvedby", "finishedat", "taskstatus").contains(lower)
                    || lower.contains("token") || lower.contains("secret") || lower.contains("cred" + "ential") || lower.contains("authorization") || lower.contains("internalpath") || lower.contains("resolvedpath")) {
                throw new BackupRecoveryException(HttpStatus.BAD_REQUEST, 40001, "trusted field rejected");
            }
        }
    }

    private static void requireSubset(List<String> requested, List<String> allowed) {
        validateDomains(requested);
        if (!allowed.containsAll(requested)) throw new BackupRecoveryException(HttpStatus.BAD_REQUEST, 40001, "domain overreach");
    }

    private static Map<String, Object> safeStorageRef(Object value) {
        Map<String, Object> source = objectMap(value);
        Map<String, Object> safe = new LinkedHashMap<>();
        safe.put("alias", textOr(source.get("alias"), "backup-vault-main"));
        safe.put("regionSummary", textOr(source.get("regionSummary"), "cn-local"));
        safe.put("tier", textOr(source.get("tier"), "WARM"));
        return safe;
    }

    private static String fingerprint(Object body) {
        try {
            return new ObjectMapper().writeValueAsString(normalize(body));
        } catch (JsonProcessingException exception) {
            throw new BackupRecoveryException(HttpStatus.BAD_REQUEST, 40001, "invalid json");
        }
    }

    private static Object normalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) sorted.put(String.valueOf(entry.getKey()), normalize(entry.getValue()));
            return sorted;
        }
        if (value instanceof List<?> list) return list.stream().map(BackupRecoveryController::normalize).toList();
        return value;
    }

    private static Comparator<BackupDomain> domainComparator(String sort) {
        if ("displayName_asc".equals(sort)) return Comparator.comparing(domain -> domain.displayName);
        if ("criticality_desc".equals(sort)) return Comparator.comparing((BackupDomain domain) -> riskOrder(domain.criticality)).reversed();
        return Comparator.comparing((BackupDomain domain) -> domain.updatedAt).reversed();
    }

    private static Comparator<BackupPolicy> policyComparator(String sort) {
        if ("displayName_asc".equals(sort)) return Comparator.comparing(policy -> policy.displayName);
        if ("createdAt_desc".equals(sort)) return Comparator.comparing((BackupPolicy policy) -> policy.createdAt).reversed();
        return Comparator.comparing((BackupPolicy policy) -> policy.updatedAt).reversed();
    }

    private static Comparator<BackupJob> jobComparator(String sort) {
        if ("finishedAt_desc".equals(sort)) return Comparator.comparing((BackupJob job) -> nullToEmpty(job.finishedAt)).reversed();
        if ("updatedAt_desc".equals(sort)) return Comparator.comparing((BackupJob job) -> job.updatedAt).reversed();
        return Comparator.comparing((BackupJob job) -> job.createdAt).reversed();
    }

    private static Comparator<BackupPoint> pointComparator(String sort) {
        if ("expiresAt_asc".equals(sort)) return Comparator.comparing(point -> point.expiresAt);
        if ("sizeBytes_desc".equals(sort)) return Comparator.comparing((BackupPoint point) -> point.sizeBytes).reversed();
        return Comparator.comparing((BackupPoint point) -> point.createdAt).reversed();
    }

    private static Comparator<RestoreDrill> drillComparator(String sort) {
        if ("finishedAt_desc".equals(sort)) return Comparator.comparing((RestoreDrill drill) -> nullToEmpty(drill.finishedAt)).reversed();
        return Comparator.comparing((RestoreDrill drill) -> drill.createdAt).reversed();
    }

    private static Comparator<RestoreRequest> restoreComparator(String sort) {
        if ("updatedAt_desc".equals(sort)) return Comparator.comparing((RestoreRequest restore) -> restore.updatedAt).reversed();
        if ("riskLevel_desc".equals(sort)) return Comparator.comparing((RestoreRequest restore) -> riskOrder(restore.riskLevel)).reversed();
        return Comparator.comparing((RestoreRequest restore) -> restore.createdAt).reversed();
    }

    private static Comparator<BackupAudit> auditComparator(String sort) {
        if ("createdAt_asc".equals(sort)) return Comparator.comparing(audit -> audit.createdAt);
        if ("riskLevel_desc".equals(sort)) return Comparator.comparing((BackupAudit audit) -> riskOrder(audit.riskLevel)).reversed();
        return Comparator.comparing((BackupAudit audit) -> audit.createdAt).reversed();
    }

    private static int riskOrder(String risk) {
        return switch (risk) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }

    private static boolean matches(String value, String keyword) {
        return keyword == null || keyword.isBlank() || value.toLowerCase().contains(keyword.toLowerCase());
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String textOr(Object value, String fallback) {
        String text = text(value);
        return text.isBlank() ? fallback : text;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static int intValue(Object value, int fallback) {
        if (value == null || value.toString().isBlank()) return fallback;
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(value.toString());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        if (value == null) return new LinkedHashMap<>();
        if (!(value instanceof Map<?, ?> map)) throw new BackupRecoveryException(HttpStatus.BAD_REQUEST, 40001, "invalid object");
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue));
        return result;
    }

    private static List<String> stringList(Object value) {
        if (value == null) return new ArrayList<>();
        if (!(value instanceof List<?> list)) throw new BackupRecoveryException(HttpStatus.BAD_REQUEST, 40001, "invalid list");
        return list.stream().map(String::valueOf).toList();
    }

    private static String now() {
        return Instant.now().toString();
    }
}

@Service
class BackupRecoveryStore {
    static final Set<String> KNOWN_DOMAINS = Set.of("DATABASE_AUTH", "DATABASE_PROFILE", "UPLOAD_CONTENT", "RESOURCE_METADATA", "INVITATION_DATA", "WHITELIST_AUDIT", "ATTENDANCE_LEDGER", "PUNISHMENT_RECORD", "REVIEW_RECORD", "OPS_CONTROL_CONFIG", "OPS_AUDIT_INDEX", "CLOUDREVE_SNAPSHOT");
    final Map<String, BackupDomain> domains = new ConcurrentHashMap<>();
    final Map<String, BackupPolicy> policies = new ConcurrentHashMap<>();
    final Map<String, BackupJob> jobs = new ConcurrentHashMap<>();
    final Map<String, BackupPoint> points = new ConcurrentHashMap<>();
    final Map<String, BackupVerification> verifications = new ConcurrentHashMap<>();
    final Map<String, RestoreDrill> drills = new ConcurrentHashMap<>();
    final Map<String, RestoreRequest> restores = new ConcurrentHashMap<>();
    final Map<String, BackupAudit> audits = new ConcurrentHashMap<>();
    final Map<String, IdempotencyRecord> idempotency = new ConcurrentHashMap<>();
    private int id = 100;

    BackupRecoveryStore() {
        seedDomains();
        BackupPolicy policy = new BackupPolicy("policy-main", "Main daily backup", List.of("DATABASE_AUTH", "RESOURCE_METADATA", "OPS_AUDIT_INDEX"),
                Map.of("mode", "DAILY", "timezone", "Asia/Shanghai", "windowMinutes", 90), 30, 2,
                Map.of("alias", "backup-vault-main", "regionSummary", "cn-local", "tier", "WARM"), "MANAGED_KEY", "ENABLED", "seed");
        policies.put(policy.policyId, policy);
    }

    synchronized String nextId() {
        return String.valueOf(++id);
    }

    private void seedDomains() {
        addDomain("DATABASE_AUTH", "Auth database", "auth", "CRITICAL");
        addDomain("DATABASE_PROFILE", "Profile database", "profile", "HIGH");
        addDomain("RESOURCE_METADATA", "Resource metadata", "resource", "HIGH");
        addDomain("WHITELIST_AUDIT", "Whitelist audit", "whitelist", "HIGH");
        addDomain("ATTENDANCE_LEDGER", "Attendance ledger", "attendance", "HIGH");
        addDomain("OPS_CONTROL_CONFIG", "Ops control config", "ops-control", "CRITICAL");
        addDomain("OPS_AUDIT_INDEX", "Ops audit index", "ops-control", "CRITICAL");
        addDomain("CLOUDREVE_SNAPSHOT", "Cloudreve snapshots", "cloudreve-sync", "MEDIUM");
    }

    private void addDomain(String key, String name, String source, String criticality) {
        domains.put(key, new BackupDomain(key, name, source, criticality));
    }

    BackupPolicy policy(String policyId) {
        BackupPolicy policy = policies.get(policyId);
        if (policy == null) throw new BackupRecoveryException(HttpStatus.NOT_FOUND, 49801, "backup policy not found");
        return policy;
    }

    BackupJob job(String jobId) {
        BackupJob job = jobs.get(jobId);
        if (job == null) throw new BackupRecoveryException(HttpStatus.NOT_FOUND, 49802, "backup job not found");
        return job;
    }

    BackupPoint point(String pointId) {
        BackupPoint point = points.get(pointId);
        if (point == null) throw new BackupRecoveryException(HttpStatus.NOT_FOUND, 49803, "backup point not found");
        return point;
    }

    RestoreDrill drill(String drillId) {
        RestoreDrill drill = drills.get(drillId);
        if (drill == null) throw new BackupRecoveryException(HttpStatus.NOT_FOUND, 49800, "restore drill not found");
        return drill;
    }

    RestoreRequest restore(String restoreRequestId) {
        RestoreRequest restore = restores.get(restoreRequestId);
        if (restore == null) throw new BackupRecoveryException(HttpStatus.NOT_FOUND, 49804, "restore request not found");
        return restore;
    }

    void failAuditIfRequested(HttpServletRequest request, boolean enabled) {
        if (enabled && "true".equals(request.getHeader("X-Test-Fail-Audit"))) {
            throw new BackupRecoveryException(HttpStatus.INTERNAL_SERVER_ERROR, 55401, "backup-recovery audit write failed");
        }
    }

    void audit(String action, String targetType, String targetId, Actor actor, HttpServletRequest request, Map<String, Object> params, String risk, String result, String failureReason, String before, String after) {
        String auditId = "audit-" + nextId();
        audits.put(auditId, new BackupAudit(auditId, action, targetType, targetId, actor, request, risk, result, failureReason, before, after));
    }

    Map<String, Object> summary(boolean testControlsEnabled) {
        long enabledPolicies = policies.values().stream().filter(policy -> "ENABLED".equals(policy.status)).count();
        long verifiedPoints = points.values().stream().filter(point -> point.verified).count();
        long pendingRestores = restores.values().stream().filter(restore -> "PENDING_APPROVAL".equals(restore.status)).count();
        Optional<BackupJob> lastSuccess = jobs.values().stream().filter(job -> "SUCCEEDED".equals(job.status)).findFirst();
        Optional<BackupJob> lastFailure = jobs.values().stream().filter(job -> "FAILED".equals(job.status)).findFirst();
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("service", "backup-recovery");
        view.put("port", 8119);
        view.put("storageMode", "IN_MEMORY");
        view.put("authMode", "TEST_STUB");
        view.put("opsControlAdapterMode", "TEST_STUB");
        view.put("notificationAdapterMode", "TEST_STUB");
        view.put("backupAdapterMode", "SIMULATED");
        view.put("testControlsEnabled", testControlsEnabled);
        view.put("domainsTotal", domains.size());
        view.put("policiesTotal", policies.size());
        view.put("enabledPoliciesTotal", enabledPolicies);
        view.put("jobsTotal", jobs.size());
        view.put("backupPointsTotal", points.size());
        view.put("verifiedBackupPointsTotal", verifiedPoints);
        view.put("restoreDrillsTotal", drills.size());
        view.put("restoreRequestsTotal", restores.size());
        view.put("pendingRestoreRequestsTotal", pendingRestores);
        view.put("auditsTotal", audits.size());
        view.put("idempotencyRecordsTotal", idempotency.size());
        view.put("lastSuccessfulBackupAt", lastSuccess.map(job -> job.finishedAt).orElse(null));
        view.put("lastFailedBackupAt", lastFailure.map(job -> job.finishedAt).orElse(null));
        view.put("degraded", false);
        view.put("degradeReasons", List.of());
        view.put("productionGaps", List.of("REAL_PERSISTENCE_NOT_CONNECTED", "REAL_BACKUP_MEDIA_NOT_CONNECTED", "REAL_CROSS_SERVICE_HTTP_NOT_CONNECTED", "REAL_RESTORE_EXECUTION_BLOCKED", "ADMIN_READ_ONLY_ENTRY_NOT_CONNECTED", "NODE_DAEMON_DIRECT_CALL_FORBIDDEN", testControlsEnabled ? "TEST_CONTROLS_ENABLED_FOR_LOCAL_TEST" : "TEST_CONTROLS_DISABLED_OUTSIDE_TEST"));
        return view;
    }
}

class BackupDomain {
    final String domainKey;
    final String displayName;
    final String sourceService;
    final String criticality;
    boolean enabled = true;
    String lastBackupPointId;
    String lastVerifiedAt;
    final String createdAt = Instant.now().toString();
    String updatedAt = createdAt;

    BackupDomain(String domainKey, String displayName, String sourceService, String criticality) {
        this.domainKey = domainKey;
        this.displayName = displayName;
        this.sourceService = sourceService;
        this.criticality = criticality;
    }

    Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("domainKey", domainKey);
        view.put("displayName", displayName);
        view.put("sourceService", sourceService);
        view.put("domainType", domainKey);
        view.put("criticality", criticality);
        view.put("enabled", enabled);
        view.put("dependencySummary", Map.of("status", "AVAILABLE", "source", "CONTRACT_SNAPSHOT"));
        view.put("lastBackupPointId", lastBackupPointId);
        view.put("lastVerifiedAt", lastVerifiedAt);
        view.put("createdAt", createdAt);
        view.put("updatedAt", updatedAt);
        return view;
    }
}

class BackupPolicy {
    final String policyId;
    String displayName;
    List<String> domains;
    Map<String, Object> scheduleSummary;
    int retentionDays;
    int minimumCopies;
    Map<String, Object> storageRef;
    String encryptionMode;
    String status;
    String lastRunStatus;
    final String createdBy;
    String updatedBy;
    final String createdAt = Instant.now().toString();
    String updatedAt = createdAt;

    BackupPolicy(String policyId, String displayName, List<String> domains, Map<String, Object> scheduleSummary,
                 int retentionDays, int minimumCopies, Map<String, Object> storageRef, String encryptionMode, String status, String createdBy) {
        this.policyId = policyId;
        this.displayName = displayName;
        this.domains = domains;
        this.scheduleSummary = scheduleSummary;
        this.retentionDays = retentionDays;
        this.minimumCopies = minimumCopies;
        this.storageRef = storageRef;
        this.encryptionMode = encryptionMode;
        this.status = status;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("policyId", policyId);
        view.put("displayName", displayName);
        view.put("domains", domains);
        view.put("scheduleSummary", scheduleSummary);
        view.put("retentionDays", retentionDays);
        view.put("minimumCopies", minimumCopies);
        view.put("storageRef", storageRef);
        view.put("encryptionMode", encryptionMode);
        view.put("status", status);
        view.put("lastRunStatus", lastRunStatus);
        view.put("createdBy", createdBy);
        view.put("updatedBy", updatedBy);
        view.put("createdAt", createdAt);
        view.put("updatedAt", updatedAt);
        return view;
    }
}

class BackupJob {
    final String jobId;
    final String policyId;
    final String trigger;
    String status;
    final List<String> domains;
    final String startedAt = Instant.now().toString();
    String finishedAt;
    Map<String, Object> resultSummary;
    String failureReason;
    final String idempotencyKey;
    final String createdBy;
    final String createdAt = startedAt;
    String updatedAt = createdAt;

    BackupJob(String jobId, String policyId, String trigger, String status, List<String> domains, String idempotencyKey, String createdBy) {
        this.jobId = jobId;
        this.policyId = policyId;
        this.trigger = trigger.isBlank() ? "ADMIN_MANUAL" : trigger;
        this.status = status;
        this.domains = domains;
        this.idempotencyKey = idempotencyKey;
        this.createdBy = createdBy;
        if (List.of("SUCCEEDED", "FAILED", "TIMEOUT").contains(status)) this.finishedAt = createdAt;
    }

    Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("jobId", jobId);
        view.put("policyId", policyId);
        view.put("trigger", trigger);
        view.put("status", status);
        view.put("domains", domains);
        view.put("startedAt", startedAt);
        view.put("finishedAt", finishedAt);
        view.put("resultSummary", resultSummary);
        view.put("failureReason", failureReason);
        view.put("idempotencyKey", idempotencyKey);
        view.put("opsControlTaskRef", null);
        view.put("createdBy", createdBy);
        view.put("createdAt", createdAt);
        view.put("updatedAt", updatedAt);
        return view;
    }
}

class BackupPoint {
    final String backupPointId;
    final String policyId;
    final String jobId;
    final List<String> domains;
    final Map<String, Object> storageRef;
    final Map<String, Object> checksumSummary = Map.of("algorithm", "SHA256", "digestPrefix", "a".repeat(12));
    final long sizeBytes = 1024L * 1024L * 8L;
    final boolean encrypted;
    boolean verified = false;
    String verifiedAt;
    String verifiedBy;
    final String expiresAt = Instant.now().plus(30, ChronoUnit.DAYS).toString();
    String status = "AVAILABLE";
    final String createdAt = Instant.now().toString();

    BackupPoint(String backupPointId, String policyId, String jobId, List<String> domains, Map<String, Object> storageRef, String encryptionMode) {
        this.backupPointId = backupPointId;
        this.policyId = policyId;
        this.jobId = jobId;
        this.domains = domains;
        this.storageRef = storageRef;
        this.encrypted = !"NONE".equals(encryptionMode);
    }

    Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("backupPointId", backupPointId);
        view.put("policyId", policyId);
        view.put("jobId", jobId);
        view.put("domains", domains);
        view.put("storageRef", storageRef);
        view.put("checksumSummary", checksumSummary);
        view.put("sizeBytes", sizeBytes);
        view.put("encrypted", encrypted);
        view.put("verified", verified);
        view.put("verifiedAt", verifiedAt);
        view.put("expiresAt", expiresAt);
        view.put("status", status);
        view.put("degradeReasons", List.of());
        view.put("createdAt", createdAt);
        return view;
    }
}

class BackupVerification {
    final String verificationId;
    final String backupPointId;
    final String status;
    final String startedAt = Instant.now().toString();
    final String finishedAt = startedAt;
    final String createdBy;
    final String createdAt = startedAt;
    String failureReason;

    BackupVerification(String verificationId, String backupPointId, String status, String createdBy) {
        this.verificationId = verificationId;
        this.backupPointId = backupPointId;
        this.status = status;
        this.createdBy = createdBy;
    }

    Map<String, Object> view() {
        return BackupMaps.linked("verificationId", verificationId, "backupPointId", backupPointId, "status", status,
                "validationSummary", Map.of("checks", List.of("METADATA", "CHECKSUM"), "simulated", true),
                "failureReason", failureReason, "startedAt", startedAt, "finishedAt", finishedAt, "createdBy", createdBy, "createdAt", createdAt);
    }
}

class RestoreDrill {
    final String drillId;
    final String backupPointId;
    final List<String> domains;
    final String status;
    final String startedAt = Instant.now().toString();
    final String finishedAt = startedAt;
    final String createdBy;
    final String createdAt = startedAt;
    String failureReason;

    RestoreDrill(String drillId, String backupPointId, List<String> domains, String status, String createdBy) {
        this.drillId = drillId;
        this.backupPointId = backupPointId;
        this.domains = domains;
        this.status = status;
        this.createdBy = createdBy;
        if ("FAILED".equals(status)) this.failureReason = "SIMULATED_DRILL_FAILURE";
    }

    Map<String, Object> view() {
        return BackupMaps.linked("drillId", drillId, "backupPointId", backupPointId, "domains", domains, "status", status,
                "validationSummary", Map.of("sandbox", true, "writesProduction", false), "startedAt", startedAt,
                "finishedAt", finishedAt, "failureReason", failureReason, "createdBy", createdBy, "createdAt", createdAt);
    }
}

class RestoreRequest {
    final String restoreRequestId;
    final String backupPointId;
    final List<String> domains;
    final String restoreMode;
    final String riskLevel = "CRITICAL";
    String status;
    Map<String, Object> approvalSummary;
    final String requestedBy;
    String approvedBy;
    final String reason;
    final String createdAt = Instant.now().toString();
    String updatedAt = createdAt;

    RestoreRequest(String restoreRequestId, String backupPointId, List<String> domains, String restoreMode, String status, String requestedBy, String reason) {
        this.restoreRequestId = restoreRequestId;
        this.backupPointId = backupPointId;
        this.domains = domains;
        this.restoreMode = restoreMode;
        this.status = status;
        this.requestedBy = requestedBy;
        this.reason = reason;
        if ("EXECUTION_BLOCKED".equals(status)) this.approvalSummary = Map.of("executionMode", "BLOCKED_BY_CONTRACT");
    }

    Map<String, Object> view() {
        return BackupMaps.linked("restoreRequestId", restoreRequestId, "backupPointId", backupPointId, "domains", domains,
                "restoreMode", restoreMode, "riskLevel", riskLevel, "status", status, "approvalSummary", approvalSummary,
                "requestedBy", requestedBy, "approvedBy", approvedBy, "reason", reason, "createdAt", createdAt, "updatedAt", updatedAt);
    }
}

class BackupAudit {
    final String id;
    final String action;
    final String targetType;
    final String targetId;
    final String actorUserId;
    final String actorRole;
    final List<String> actorPermissions;
    final String riskLevel;
    final String result;
    final String failureReason;
    final String beforeState;
    final String afterState;
    final String requestId;
    final String createdAt = Instant.now().toString();
    final String policyId;
    final String jobId;
    final String backupPointId;
    final String drillId;
    final String restoreRequestId;

    BackupAudit(String id, String action, String targetType, String targetId, Actor actor, HttpServletRequest request,
                String riskLevel, String result, String failureReason, String beforeState, String afterState) {
        this.id = id;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.actorUserId = actor.userId;
        this.actorRole = actor.role;
        this.actorPermissions = actor.permissions;
        this.riskLevel = riskLevel;
        this.result = result;
        this.failureReason = failureReason;
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.requestId = String.valueOf(request.getAttribute("requestId"));
        this.policyId = "POLICY".equals(targetType) ? targetId : null;
        this.jobId = "JOB".equals(targetType) ? targetId : null;
        this.backupPointId = "BACKUP_POINT".equals(targetType) ? targetId : null;
        this.drillId = "RESTORE_DRILL".equals(targetType) ? targetId : null;
        this.restoreRequestId = "RESTORE_REQUEST".equals(targetType) ? targetId : null;
    }

    Map<String, Object> view() {
        return BackupMaps.linked("id", id, "requestId", requestId, "actorUserId", actorUserId, "actorRole", actorRole,
                "actorPermissions", actorPermissions, "sourceIp", null, "targetType", targetType, "targetId", targetId,
                "action", action, "riskLevel", riskLevel, "reason", null, "paramsSummary", Map.of("sanitized", true),
                "beforeState", beforeState, "afterState", afterState, "result", result, "failureReason", failureReason,
                "policyId", policyId, "jobId", jobId, "backupPointId", backupPointId, "drillId", drillId,
                "restoreRequestId", restoreRequestId, "dependencyStatus", "AVAILABLE", "createdAt", createdAt);
    }
}

@Service
class BackupRecoveryAuth {
    private final BackupRecoveryProperties properties;

    BackupRecoveryAuth(BackupRecoveryProperties properties) {
        this.properties = properties;
    }

    Actor current(HttpServletRequest request) {
        if (properties.enabled()) {
            String mode = request.getHeader("X-Test-Auth-Mode");
            if ("unavailable".equals(mode)) throw new BackupRecoveryException(HttpStatus.BAD_GATEWAY, 46810, "auth unavailable");
            if ("timeout".equals(mode)) throw new BackupRecoveryException(HttpStatus.GATEWAY_TIMEOUT, 46811, "auth timeout");
            if ("bad-schema".equals(mode)) throw new BackupRecoveryException(HttpStatus.BAD_GATEWAY, 46812, "auth bad schema");
        }
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) throw new BackupRecoveryException(HttpStatus.UNAUTHORIZED, 41000, "unauthenticated");
        if (!header.startsWith("Bearer ")) throw new BackupRecoveryException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        return switch (header.substring("Bearer ".length())) {
            case "auth-unavailable-token" -> throw new BackupRecoveryException(HttpStatus.BAD_GATEWAY, 46810, "auth unavailable");
            case "auth-timeout-token" -> throw new BackupRecoveryException(HttpStatus.GATEWAY_TIMEOUT, 46811, "auth timeout");
            case "auth-bad-token" -> throw new BackupRecoveryException(HttpStatus.BAD_GATEWAY, 46812, "auth bad schema");
            case "br-viewer-token" -> new Actor("br-viewer-user", "Backup Viewer", "HELPER", List.of("NODE_READ"));
            case "br-no-cap-token" -> new Actor("br-no-cap-user", "No Cap", "ADMIN", List.of());
            case "br-admin-token" -> new Actor("br-admin-user", "Backup Admin", "ADMIN", List.of("NODE_READ", "NODE_WRITE", "HIGH_RISK_APPROVE"));
            case "br-approver-token" -> new Actor("br-approver-user", "Backup Approver", "ADMIN", List.of("NODE_READ", "HIGH_RISK_APPROVE"));
            case "owner-token" -> new Actor("owner-user", "Owner", "OWNER", List.of("NODE_READ", "NODE_WRITE", "HIGH_RISK_APPROVE"));
            case "user-token" -> new Actor("plain-user", "Plain User", "USER", List.of());
            default -> throw new BackupRecoveryException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        };
    }

    Actor requireAnyCapability(HttpServletRequest request, String... capabilities) {
        return requireAnyCapability(current(request), capabilities);
    }

    Actor requireAnyCapability(Actor actor, String... capabilities) {
        if ("USER".equals(actor.role)) throw new BackupRecoveryException(HttpStatus.FORBIDDEN, 42001, "role denied");
        if (List.of(capabilities).stream().noneMatch(actor.permissions::contains)) {
            throw new BackupRecoveryException(HttpStatus.FORBIDDEN, 42002, "capability denied");
        }
        return actor;
    }

    void requireAdmin(Actor actor) {
        if (!List.of("ADMIN", "OWNER").contains(actor.role)) throw new BackupRecoveryException(HttpStatus.FORBIDDEN, 42001, "role denied");
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

class IdempotencyRecord {
    final String fingerprint;
    final org.springframework.http.HttpStatusCode status;
    final Object data;

    IdempotencyRecord(String fingerprint, org.springframework.http.HttpStatusCode status, Object data) {
        this.fingerprint = fingerprint;
        this.status = status;
        this.data = data;
    }
}

@Component
class BackupRecoveryProperties {
    private final boolean enabled;

    BackupRecoveryProperties(@Value("${backup-recovery.test-controls.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    boolean enabled() {
        return enabled;
    }
}

@Component
class BackupRecoveryRequestIdFilter extends OncePerRequestFilter {
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
class BackupRecoveryExceptionHandler {
    @ExceptionHandler(BackupRecoveryException.class)
    ResponseEntity<Map<String, Object>> api(BackupRecoveryException exception, HttpServletRequest request) {
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
        body.put("code", 55400);
        body.put("message", "backup-recovery internal error");
        body.put("data", null);
        body.put("requestId", request.getAttribute("requestId"));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

class BackupRecoveryException extends RuntimeException {
    final HttpStatus status;
    final int code;

    BackupRecoveryException(HttpStatus status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}

class BackupMaps {
    static Map<String, Object> linked(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }
}
