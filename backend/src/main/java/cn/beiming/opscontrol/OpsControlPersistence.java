package cn.beiming.opscontrol;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * ops-control 正式持久化入口。没有 JdbcTemplate 时退化为 Noop，保持本地无容器场景可启动。
 */
interface OpsControlPersistence {
    void persistWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String idempotencyKey,
                      String fingerprint, String action, Map<String, Object> payload, int responseCode);

    Map<String, Object> counts();
}

@Configuration
class OpsControlPersistenceConfiguration {
    @Bean
    OpsControlPersistence opsControlPersistence(ObjectProvider<JdbcTemplate> jdbcTemplate, ObjectMapper objectMapper) {
        JdbcTemplate available = jdbcTemplate.getIfAvailable();
        if (available == null) {
            return new NoopOpsControlPersistence();
        }
        return new PostgresOpsControlPersistence(available, objectMapper);
    }
}

class NoopOpsControlPersistence implements OpsControlPersistence {
    @Override
    public void persistWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String idempotencyKey,
                             String fingerprint, String action, Map<String, Object> payload, int responseCode) {
    }

    @Override
    public Map<String, Object> counts() {
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("storageMode", "IN_MEMORY");
        return counts;
    }
}

class PostgresOpsControlPersistence implements OpsControlPersistence {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    PostgresOpsControlPersistence(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void persistWrite(HttpServletRequest request, String actorUserId, String actorRole, String scope, String idempotencyKey,
                             String fingerprint, String action, Map<String, Object> payload, int responseCode) {
        if (action.startsWith("OPS_NODE_")) {
            upsertNode(payload);
            persistRuntimeEvidence(payload);
        } else if (action.startsWith("OPS_TASK_")) {
            upsertTask(payload);
        } else if (action.startsWith("OPS_APPROVAL_")) {
            upsertApproval(map(payload.get("approval")));
            Map<?, ?> task = map(payload.get("task"));
            if (!task.isEmpty()) {
                upsertTask(task);
            }
        }
        insertAudit(request, actorUserId, actorRole, action, payload);
        insertIdempotency(actorUserId, scope, idempotencyKey, fingerprint, responseCode, payload);
        insertRequestLog(request, actorUserId, responseCode);
    }

    @Override
    public Map<String, Object> counts() {
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("storageMode", "POSTGRESQL_PRIMARY");
        counts.put("nodesTotal", count("ops_nodes"));
        counts.put("runtimeSnapshotsTotal", count("ops_runtime_snapshots"));
        counts.put("tasksTotal", count("ops_tasks"));
        counts.put("pendingApprovalsTotal", countWhere("ops_approvals", "status = 'PENDING'"));
        counts.put("metricsTotal", count("ops_metrics"));
        return counts;
    }

    private void upsertNode(Map<?, ?> node) {
        String nodeId = text(node.get("nodeId"));
        if (nodeId == null) {
            return;
        }
        jdbc.update("""
                INSERT INTO ops_nodes(id, node_id, display_name, endpoint_summary, status, version, capabilities, labels,
                    last_heartbeat_at, last_seen_request_id, token_digest, created_by, updated_by, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS timestamptz), ?, ?, ?, ?, CAST(? AS timestamptz), CAST(? AS timestamptz))
                ON CONFLICT (node_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    version = COALESCE(EXCLUDED.version, ops_nodes.version),
                    capabilities = EXCLUDED.capabilities,
                    labels = EXCLUDED.labels,
                    last_heartbeat_at = COALESCE(EXCLUDED.last_heartbeat_at, ops_nodes.last_heartbeat_at),
                    last_seen_request_id = COALESCE(EXCLUDED.last_seen_request_id, ops_nodes.last_seen_request_id),
                    updated_by = EXCLUDED.updated_by,
                    updated_at = EXCLUDED.updated_at
                """, UUID.randomUUID(), nodeId, textOr(node.get("displayName"), nodeId),
                textOr(node.get("endpointSummary"), "masked endpoint"), textOr(node.get("status"), "PENDING_REGISTRATION"),
                text(node.get("version")), json(node.get("capabilities")), json(node.get("labels")),
                text(node.get("lastHeartbeatAt")), text(node.get("lastSeenRequestId")),
                textOr(node.get("tokenDigest"), "sha256:****"), textOr(node.get("createdBy"), "system"),
                textOr(node.get("updatedBy"), textOr(node.get("createdBy"), "system")),
                text(node.get("createdAt")), textOr(node.get("updatedAt"), text(node.get("createdAt"))));
    }

    private void persistRuntimeEvidence(Map<?, ?> payload) {
        Map<?, ?> evidence = map(payload.get("runtimeEvidence"));
        if (evidence.isEmpty()) {
            return;
        }
        String nodeId = text(payload.get("nodeId"));
        upsertSnapshots(nodeId, "CONTAINER", evidence.get("containers"), "containerId");
        upsertSnapshots(nodeId, "VM", evidence.get("vms"), "vmId");
        upsertSnapshots(nodeId, "MINECRAFT_INSTANCE", evidence.get("minecraftInstances"), "instanceId");
        upsertSnapshots(nodeId, "FILE", evidence.get("files"), "path");
        Map<?, ?> metric = map(payload.get("metricsSnapshot"));
        if (!metric.isEmpty()) {
            jdbc.update("""
                    INSERT INTO ops_metrics(id, snapshot_id, node_id, cpu_usage_percent, memory_usage_percent, disk_usage_percent, collected_at)
                    VALUES (?, ?, ?, ?, ?, ?, CAST(? AS timestamptz))
                    ON CONFLICT (snapshot_id) DO UPDATE SET
                        cpu_usage_percent = EXCLUDED.cpu_usage_percent,
                        memory_usage_percent = EXCLUDED.memory_usage_percent,
                        disk_usage_percent = EXCLUDED.disk_usage_percent,
                        collected_at = EXCLUDED.collected_at
                    """, UUID.randomUUID(), textOr(metric.get("snapshotId"), "metric-" + UUID.randomUUID()), nodeId,
                    number(metric.get("cpuUsagePercent")), number(metric.get("memoryUsagePercent")),
                    number(metric.get("diskUsagePercent")), textOr(metric.get("collectedAt"), now()));
        }
    }

    private void upsertSnapshots(String nodeId, String snapshotType, Object value, String idKey) {
        for (Object element : list(value)) {
            Map<?, ?> item = map(element);
            String targetId = text(item.get(idKey));
            if (targetId == null || targetId.isBlank()) {
                continue;
            }
            jdbc.update("""
                    INSERT INTO ops_runtime_snapshots(id, snapshot_type, node_id, target_id, status, payload, recorded_at)
                    VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), now())
                    ON CONFLICT (snapshot_type, node_id, target_id) DO UPDATE SET
                        status = EXCLUDED.status,
                        payload = EXCLUDED.payload,
                        recorded_at = now()
                    """, UUID.randomUUID(), snapshotType, nodeId, targetId, textOr(item.get("status"), "UNKNOWN"), json(item));
        }
    }

    private void upsertTask(Map<?, ?> task) {
        String taskId = text(task.get("taskId"));
        if (taskId == null) {
            return;
        }
        jdbc.update("""
                INSERT INTO ops_tasks(id, task_id, task_type, status, risk_level, node_id, target_type, target_id, reason,
                    params_summary, approval_id, idempotency_key, node_request_id, result_summary, failure_reason,
                    created_by, created_at, updated_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, CAST(? AS jsonb), ?, ?, CAST(? AS timestamptz), CAST(? AS timestamptz), CAST(? AS timestamptz))
                ON CONFLICT (task_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    node_request_id = COALESCE(EXCLUDED.node_request_id, ops_tasks.node_request_id),
                    result_summary = COALESCE(EXCLUDED.result_summary, ops_tasks.result_summary),
                    failure_reason = EXCLUDED.failure_reason,
                    approval_id = COALESCE(EXCLUDED.approval_id, ops_tasks.approval_id),
                    updated_at = EXCLUDED.updated_at
                """, UUID.randomUUID(), taskId, textOr(task.get("taskType"), "UNKNOWN"), textOr(task.get("status"), "QUEUED"),
                textOr(task.get("riskLevel"), "MEDIUM"), text(task.get("nodeId")), textOr(task.get("targetType"), "UNKNOWN"),
                textOr(task.get("targetId"), ""), textOr(task.get("reason"), ""), json(task.get("paramsSummary")),
                text(task.get("approvalId")), text(task.get("idempotencyKey")), text(task.get("nodeRequestId")),
                json(task.get("resultSummary")), text(task.get("failureReason")), textOr(task.get("createdBy"), "system"),
                text(task.get("createdAt")), textOr(task.get("updatedAt"), text(task.get("createdAt"))),
                textOr(task.get("expiresAt"), now()));
    }

    private void upsertApproval(Map<?, ?> approval) {
        String approvalId = text(approval.get("approvalId"));
        if (approvalId == null) {
            return;
        }
        jdbc.update("""
                INSERT INTO ops_approvals(id, approval_id, task_id, status, risk_level, requested_by, approved_by, review_comment,
                    created_at, reviewed_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS timestamptz), CAST(? AS timestamptz), CAST(? AS timestamptz))
                ON CONFLICT (approval_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    approved_by = EXCLUDED.approved_by,
                    review_comment = EXCLUDED.review_comment,
                    reviewed_at = EXCLUDED.reviewed_at
                """, UUID.randomUUID(), approvalId, textOr(approval.get("taskId"), ""),
                textOr(approval.get("status"), "PENDING"), textOr(approval.get("riskLevel"), "HIGH"),
                textOr(approval.get("requestedBy"), "system"), text(approval.get("approvedBy")),
                text(approval.get("reviewComment")), text(approval.get("createdAt")), text(approval.get("reviewedAt")),
                textOr(approval.get("expiresAt"), now()));
    }

    private void insertAudit(HttpServletRequest request, String actorUserId, String actorRole, String action, Map<String, Object> payload) {
        String targetType = action.startsWith("OPS_NODE_") ? "NODE" : action.startsWith("OPS_TASK_") ? "TASK" : "APPROVAL";
        String targetId = targetType.equals("NODE") ? text(payload.get("nodeId"))
                : targetType.equals("TASK") ? textOr(payload.get("taskId"), text(map(payload.get("task")).get("taskId")))
                : text(map(payload.get("approval")).get("approvalId"));
        String riskLevel = auditRiskLevel(textOr(payload.get("riskLevel"), text(map(payload.get("approval")).get("riskLevel"))));
        jdbc.update("""
                INSERT INTO app_audit_logs(id, request_id, actor_user_id, actor_role, actor_permissions, source_ip, target_type,
                    target_id, action, risk_level, reason, params_summary, before_state, after_state, result, failure_reason, created_at)
                VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), 'SUCCESS', ?, now())
                """, UUID.randomUUID(), requestId(request), actorUserId, actorRole == null ? "USER" : actorRole,
                json(List.of()), sourceIp(request), targetType, targetId == null ? "" : targetId, action, riskLevel,
                textOr(payload.get("reason"), action), json(Map.of("action", action)),
                json(Map.of("status", String.valueOf(payload.get("status")))), json(Map.of("status", String.valueOf(payload.get("status")))),
                null);
    }

    private void insertIdempotency(String actorUserId, String scope, String idempotencyKey, String fingerprint, int responseCode, Map<String, Object> payload) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || scope == null) {
            return;
        }
        jdbc.update("""
                INSERT INTO app_idempotency_records(id, actor_user_id, scope, idempotency_key, request_fingerprint, response_code, response_body, created_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), now(), now() + interval '24 hours')
                ON CONFLICT (actor_user_id, scope, idempotency_key) DO NOTHING
                """, UUID.randomUUID(), actorUserId, scope, idempotencyKey, fingerprint == null ? "" : fingerprint, responseCode,
                json(Map.of("code", 0, "message", "success", "data", payload)));
    }

    private void insertRequestLog(HttpServletRequest request, String actorUserId, int responseCode) {
        jdbc.update("""
                INSERT INTO app_request_logs(id, request_id, method, path, actor_user_id, source_ip, response_code, result, failure_reason, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'SUCCESS', NULL, now())
                ON CONFLICT (request_id) DO NOTHING
                """, UUID.randomUUID(), requestId(request), request.getMethod(), request.getRequestURI(),
                actorUserId, sourceIp(request), responseCode);
    }

    private String auditRiskLevel(String value) {
        if (value == null) {
            return "LOW";
        }
        return switch (value) {
            case "HIGH", "CRITICAL" -> "HIGH";
            case "MEDIUM" -> "MEDIUM";
            default -> "LOW";
        };
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private long countWhere(String table, String where) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + where, Long.class);
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute("requestId");
        return value == null ? Objects.toString(request.getHeader("X-Request-Id"), "req-" + UUID.randomUUID()) : value.toString();
    }

    private String sourceIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to serialize ops-control payload", exception);
        }
    }

    private static Map<?, ?> map(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> items ? items : List.of();
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static String textOr(Object value, String fallback) {
        String resolved = text(value);
        return resolved == null || resolved.isBlank() ? fallback : resolved;
    }

    private static double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0d;
    }

    private static String now() {
        return OffsetDateTime.now().toString();
    }
}
