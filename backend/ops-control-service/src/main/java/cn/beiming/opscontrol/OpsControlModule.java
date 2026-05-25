package cn.beiming.opscontrol;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/v1/ops-control")
class OpsControlController {
    private final OpsStore store;
    private final OpsAuth auth;
    private final OpsProperties properties;

    OpsControlController(OpsStore store, OpsAuth auth, OpsProperties properties) {
        this.store = store;
        this.auth = auth;
        this.properties = properties;
    }

    @GetMapping("/overview")
    ResponseEntity<Map<String, Object>> overview(HttpServletRequest request) {
        Actor actor = auth.requireCapability(request, "NODE_READ");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("nodesTotal", store.nodes.size());
        data.put("onlineNodesTotal", store.nodes.values().stream().filter(node -> "ONLINE".equals(node.status)).count());
        data.put("offlineNodesTotal", store.nodes.values().stream().filter(node -> List.of("OFFLINE", "DISABLED").contains(node.status)).count());
        data.put("assetsTotal", store.assets.size());
        data.put("pendingApprovalsTotal", store.approvals.values().stream().filter(approval -> "PENDING".equals(approval.status)).count());
        data.put("runningTasksTotal", store.tasks.values().stream().filter(task -> List.of("QUEUED", "DISPATCHED", "RUNNING").contains(task.status)).count());
        data.put("degradedModules", List.of("NODE_DAEMON"));
        data.put("recentAudits", store.audits.stream().limit(10).map(OpsAudit::view).toList());
        data.put("generatedFor", actor.userId);
        return ok(request, data);
    }

    @GetMapping("/assets")
    ResponseEntity<Map<String, Object>> assets(HttpServletRequest request,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int pageSize,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String nodeId,
                                               @RequestParam(required = false) String assetType,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String ownerModule,
                                               @RequestParam(required = false) String riskTag,
                                               @RequestParam(required = false) String sort) {
        auth.requireCapability(request, "NODE_READ");
        validatePage(page, pageSize);
        validateSort(sort, "updatedAt_desc", "createdAt_desc", "displayName_asc", "assetType_asc");
        List<Map<String, Object>> items = store.assets.values().stream()
                .filter(asset -> keyword == null || asset.displayName.contains(keyword) || asset.assetId.contains(keyword))
                .filter(asset -> nodeId == null || Objects.equals(asset.nodeId, nodeId))
                .filter(asset -> assetType == null || asset.assetType.equals(assetType))
                .filter(asset -> status == null || asset.status.equals(status))
                .filter(asset -> ownerModule == null || Objects.equals(asset.ownerModule, ownerModule))
                .filter(asset -> riskTag == null || asset.riskTags.contains(riskTag))
                .sorted(assetComparator(sort))
                .map(OpsAsset::view)
                .toList();
        return ok(request, page(items, page, pageSize));
    }

    @GetMapping("/assets/{assetId}")
    ResponseEntity<Map<String, Object>> asset(HttpServletRequest request, @PathVariable String assetId) {
        auth.requireCapability(request, "NODE_READ");
        return ok(request, store.asset(assetId).view());
    }

    @GetMapping("/nodes")
    ResponseEntity<Map<String, Object>> nodes(HttpServletRequest request,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int pageSize,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String capability,
                                              @RequestParam(required = false) String sort) {
        auth.requireCapability(request, "NODE_READ");
        validatePage(page, pageSize);
        validateSort(sort, "updatedAt_desc", "createdAt_desc", "displayName_asc");
        List<Map<String, Object>> items = store.nodes.values().stream()
                .filter(node -> keyword == null || node.displayName.contains(keyword) || node.nodeId.contains(keyword))
                .filter(node -> status == null || node.status.equals(status))
                .filter(node -> capability == null || node.capabilities.contains(capability))
                .sorted(nodeComparator(sort))
                .map(OpsNode::view)
                .toList();
        return ok(request, page(items, page, pageSize));
    }

    @GetMapping("/nodes/{nodeId}")
    ResponseEntity<Map<String, Object>> node(HttpServletRequest request, @PathVariable String nodeId) {
        auth.requireCapability(request, "NODE_READ");
        OpsNode node = store.node(nodeId);
        Map<String, Object> data = node.view();
        OpsMetric metric = store.metrics.get(nodeId);
        data.put("latestMetric", metric == null ? null : metric.view());
        data.put("degraded", !"ONLINE".equals(node.status));
        return ok(request, data);
    }

    @PostMapping("/nodes")
    ResponseEntity<Map<String, Object>> createNode(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireCapability(request, "NODE_WRITE");
        auth.requireAdmin(actor);
        rejectTrusted(body);
        validateText(body, "displayName");
        validateText(body, "reason");
        return idempotent(request, actor, "node:create", body, () -> {
            store.failAuditIfRequested(request);
            synchronized (store) {
                String displayName = text(body, "displayName", "");
                if (store.nodes.values().stream().anyMatch(node -> node.displayName.equals(displayName))) {
                    throw new OpsException(HttpStatus.CONFLICT, 49411, "node conflict");
                }
                String nodeId = "node-" + store.nextId();
                OpsNode node = new OpsNode(nodeId, displayName, text(body, "endpointSummary", "masked endpoint"),
                        stringList(body.get("capabilities")), actor.userId);
                store.nodes.put(nodeId, node);
                store.audit("OPS_NODE_CREATED", "NODE", nodeId, actor, request, body, "MEDIUM", "SUCCESS", null, null, node.status);
                return created(request, Map.of("node", node.view(), "registrationTokenMasked", "ops-reg-****"));
            }
        });
    }

    @PatchMapping("/nodes/{nodeId}/disable")
    ResponseEntity<Map<String, Object>> disableNode(HttpServletRequest request, @PathVariable String nodeId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireCapability(request, "NODE_WRITE");
        auth.requireAdmin(actor);
        validateText(body, "reason");
        if (!"DISABLE_OPS_NODE".equals(text(body, "confirmText", ""))) {
            throw new OpsException(HttpStatus.CONFLICT, 49413, "confirm text mismatch");
        }
        return idempotent(request, actor, "node:disable:" + nodeId, body, () -> {
            store.failAuditIfRequested(request);
            synchronized (store) {
                OpsNode node = store.node(nodeId);
                String before = node.status;
                node.status = "DISABLED";
                node.updatedAt = now();
                store.audit("OPS_NODE_DISABLED", "NODE", nodeId, actor, request, body, "HIGH", "SUCCESS", null, before, node.status);
                return ok(request, node.view());
            }
        });
    }

    @PatchMapping("/nodes/{nodeId}/enable")
    ResponseEntity<Map<String, Object>> enableNode(HttpServletRequest request, @PathVariable String nodeId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireCapability(request, "NODE_WRITE");
        auth.requireAdmin(actor);
        validateText(body, "reason");
        return idempotent(request, actor, "node:enable:" + nodeId, body, () -> {
            store.failAuditIfRequested(request);
            synchronized (store) {
                OpsNode node = store.node(nodeId);
                if ("REVOKED".equals(node.status)) {
                    throw new OpsException(HttpStatus.CONFLICT, 49410, "node revoked");
                }
                String before = node.status;
                node.status = "OFFLINE";
                node.updatedAt = now();
                store.audit("OPS_NODE_ENABLED", "NODE", nodeId, actor, request, body, "MEDIUM", "SUCCESS", null, before, node.status);
                return ok(request, node.view());
            }
        });
    }

    @PostMapping("/nodes/{nodeId}/heartbeat")
    ResponseEntity<Map<String, Object>> heartbeat(HttpServletRequest request, @PathVariable String nodeId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireCapability(request, "NODE_WRITE");
        store.failAuditIfRequested(request);
        synchronized (store) {
            OpsNode node = store.node(nodeId);
            String before = node.status;
            node.status = text(body, "status", "ONLINE");
            node.version = text(body, "version", node.version);
            node.capabilities = stringList(body.getOrDefault("capabilities", node.capabilities));
            node.lastHeartbeatAt = now();
            node.updatedAt = node.lastHeartbeatAt;
            Object metrics = body.get("metrics");
            if (metrics instanceof Map<?, ?> metricMap) {
                store.metrics.put(nodeId, OpsMetric.from(nodeId, metricMap));
            }
            store.audit("OPS_NODE_HEARTBEAT", "NODE", nodeId, actor, request, body, "MEDIUM", "SUCCESS", null, before, node.status);
            return ok(request, node.view());
        }
    }

    @GetMapping("/nodes/{nodeId}/capabilities")
    ResponseEntity<Map<String, Object>> capabilities(HttpServletRequest request, @PathVariable String nodeId) {
        Actor actor = auth.requireCapability(request, "NODE_READ");
        OpsNode node = store.node(nodeId);
        List<String> currentUserCapabilities = node.capabilities.stream().filter(actor.permissions::contains).toList();
        return ok(request, Map.of("nodeId", nodeId, "nodeCapabilities", node.capabilities, "currentUserCapabilities", currentUserCapabilities));
    }

    @GetMapping("/nodes/{nodeId}/metrics/latest")
    ResponseEntity<Map<String, Object>> metrics(HttpServletRequest request, @PathVariable String nodeId) {
        auth.requireCapability(request, "NODE_READ");
        store.node(nodeId);
        OpsMetric metric = store.metrics.get(nodeId);
        Map<String, Object> data = metric == null ? new LinkedHashMap<>() : metric.view();
        data.putIfAbsent("nodeId", nodeId);
        data.putIfAbsent("degraded", true);
        return ok(request, data);
    }

    @GetMapping("/nodes/{nodeId}/containers")
    ResponseEntity<Map<String, Object>> containers(HttpServletRequest request, @PathVariable String nodeId,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int pageSize) {
        auth.requireCapability(request, "NODE_READ");
        validatePage(page, pageSize);
        OpsNode node = store.node(nodeId);
        List<Map<String, Object>> items = store.containers.values().stream()
                .filter(container -> container.nodeId.equals(nodeId))
                .map(container -> container.view(!"ONLINE".equals(node.status)))
                .toList();
        return ok(request, page(items, page, pageSize));
    }

    @GetMapping("/nodes/{nodeId}/containers/{containerId}")
    ResponseEntity<Map<String, Object>> container(HttpServletRequest request, @PathVariable String nodeId, @PathVariable String containerId) {
        auth.requireCapability(request, "NODE_READ");
        OpsNode node = store.node(nodeId);
        OpsContainer container = Optional.ofNullable(store.containers.get(containerId))
                .filter(item -> item.nodeId.equals(nodeId))
                .orElseThrow(() -> new OpsException(HttpStatus.NOT_FOUND, 49400, "container not found"));
        return ok(request, container.view(!"ONLINE".equals(node.status)));
    }

    @GetMapping("/nodes/{nodeId}/vms")
    ResponseEntity<Map<String, Object>> vms(HttpServletRequest request, @PathVariable String nodeId) {
        auth.requireCapability(request, "NODE_READ");
        store.node(nodeId);
        return ok(request, Map.of("items", store.vms.values().stream().filter(vm -> vm.nodeId.equals(nodeId)).map(OpsVm::view).toList()));
    }

    @GetMapping("/nodes/{nodeId}/vms/{vmId}")
    ResponseEntity<Map<String, Object>> vm(HttpServletRequest request, @PathVariable String nodeId, @PathVariable String vmId) {
        auth.requireCapability(request, "NODE_READ");
        store.node(nodeId);
        return ok(request, Optional.ofNullable(store.vms.get(vmId)).filter(item -> item.nodeId.equals(nodeId))
                .orElseThrow(() -> new OpsException(HttpStatus.NOT_FOUND, 49400, "vm not found")).view());
    }

    @GetMapping("/nodes/{nodeId}/minecraft-instances")
    ResponseEntity<Map<String, Object>> minecraftInstances(HttpServletRequest request, @PathVariable String nodeId) {
        auth.requireCapability(request, "NODE_READ");
        store.node(nodeId);
        return ok(request, Map.of("items", store.minecraft.values().stream().filter(instance -> instance.nodeId.equals(nodeId)).map(OpsMinecraft::view).toList()));
    }

    @GetMapping("/nodes/{nodeId}/minecraft-instances/{instanceId}")
    ResponseEntity<Map<String, Object>> minecraftInstance(HttpServletRequest request, @PathVariable String nodeId, @PathVariable String instanceId) {
        auth.requireCapability(request, "NODE_READ");
        store.node(nodeId);
        return ok(request, Optional.ofNullable(store.minecraft.get(instanceId)).filter(item -> item.nodeId.equals(nodeId))
                .orElseThrow(() -> new OpsException(HttpStatus.NOT_FOUND, 49400, "minecraft instance not found")).view());
    }

    @GetMapping("/nodes/{nodeId}/files")
    ResponseEntity<Map<String, Object>> files(HttpServletRequest request, @PathVariable String nodeId,
                                              @RequestParam String rootAlias,
                                              @RequestParam String path) {
        auth.requireCapability(request, "FILE_MANAGE");
        store.node(nodeId);
        guardPath(path);
        List<Map<String, Object>> items = store.files.values().stream()
                .filter(file -> file.nodeId.equals(nodeId) && file.rootAlias.equals(rootAlias))
                .filter(file -> "/".equals(path) ? file.path.lastIndexOf('/') == 0 : file.path.startsWith(path))
                .map(OpsFile::view)
                .toList();
        return ok(request, Map.of("items", items, "rootAlias", rootAlias, "path", path, "stale", false));
    }

    @PostMapping("/nodes/{nodeId}/files/read")
    ResponseEntity<Map<String, Object>> readFile(HttpServletRequest request, @PathVariable String nodeId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireCapability(request, "FILE_MANAGE");
        store.node(nodeId);
        String path = text(body, "path", "");
        guardPath(path);
        validateText(body, "reason");
        return idempotent(request, actor, "file:read:" + nodeId + ":" + path, body, () -> {
            OpsFile file = store.files.values().stream()
                    .filter(item -> item.nodeId.equals(nodeId) && item.rootAlias.equals(text(body, "rootAlias", "")) && item.path.equals(path))
                    .findFirst()
                    .orElseThrow(() -> new OpsException(HttpStatus.NOT_FOUND, 49400, "file not found"));
            if (!file.editableText) {
                throw new OpsException(HttpStatus.CONFLICT, 49410, "file is not editable text");
            }
            store.audit("OPS_FILE_READ_REQUESTED", "FILE", file.path, actor, request, body, "MEDIUM", "SUCCESS", null, null, null);
            return ok(request, Map.of("nodeId", nodeId, "rootAlias", file.rootAlias, "path", file.path, "contentSummary", "text configuration snapshot redacted"));
        });
    }

    @PostMapping("/nodes/{nodeId}/logs/query")
    ResponseEntity<Map<String, Object>> logs(HttpServletRequest request, @PathVariable String nodeId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireCapability(request, "NODE_READ");
        store.node(nodeId);
        int tailLines = number(body.get("tailLines"), 100);
        if (tailLines < 1 || tailLines > 1000) {
            throw new OpsException(HttpStatus.BAD_REQUEST, 40001, "invalid tail lines");
        }
        validateText(body, "reason");
        return idempotent(request, actor, "logs:" + nodeId + ":" + text(body, "targetId", ""), body, () -> {
            store.audit("OPS_LOG_QUERY_REQUESTED", "LOG", text(body, "targetId", "unknown"), actor, request, body, "MEDIUM", "SUCCESS", null, null, null);
            return ok(request, Map.of("nodeId", nodeId, "targetType", text(body, "targetType", "UNKNOWN"), "targetId", text(body, "targetId", "unknown"),
                    "logSummary", List.of("[INFO] simulated log line", "[INFO] sensitive values redacted"), "stale", false));
        });
    }

    @PostMapping("/tasks")
    ResponseEntity<Map<String, Object>> createTask(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.current(request);
        String taskType = text(body, "taskType", "");
        validateTaskType(taskType);
        requireTaskCapability(actor, taskType);
        validateText(body, "reason");
        return idempotent(request, actor, "task:create", body, () -> {
            store.failAuditIfRequested(request);
            synchronized (store) {
                OpsNode node = store.node(text(body, "nodeId", ""));
                if (properties.enabled() && "offline".equals(request.getHeader("X-Test-Node-Mode"))) {
                    throw new OpsException(HttpStatus.CONFLICT, 49415, "node offline");
                }
                if (!"ONLINE".equals(node.status) && requiresRealtime(taskType)) {
                    throw new OpsException(HttpStatus.CONFLICT, 49415, "node offline");
                }
                String risk = risk(taskType);
                if ("HIGH".equals(risk) && !validHighConfirm(taskType, text(body, "confirmText", ""))) {
                    throw new OpsException(HttpStatus.FORBIDDEN, 42003, "high risk not confirmed");
                }
                String taskId = "task-" + store.nextId();
                OpsTask task = new OpsTask(taskId, taskType, text(body, "nodeId", ""), text(body, "targetType", "UNKNOWN"),
                        text(body, "targetId", ""), text(body, "reason", ""), actor.userId, risk, store.summary(body));
                if ("HIGH".equals(risk) || "CRITICAL".equals(risk)) {
                    task.status = "PENDING_APPROVAL";
                    String approvalId = "approval-" + store.nextId();
                    OpsApproval approval = new OpsApproval(approvalId, taskId, risk, actor.userId);
                    task.approvalId = approvalId;
                    store.approvals.put(approvalId, approval);
                } else if (properties.enabled() && "queued".equals(request.getHeader("X-Test-Node-Mode"))) {
                    task.status = "QUEUED";
                } else if (properties.enabled() && "dispatched".equals(request.getHeader("X-Test-Node-Mode"))) {
                    task.status = "DISPATCHED";
                    task.nodeRequestId = "node-req-" + store.nextId();
                } else {
                    task.status = "SUCCEEDED";
                    task.resultSummary = Map.of("adapter", "SIMULATED", "message", "operation accepted by simulated adapter");
                }
                store.tasks.put(taskId, task);
                store.audit("OPS_TASK_CREATED", "TASK", taskId, actor, request, body, risk, "SUCCESS", null, null, task.status);
                return created(request, task.view());
            }
        });
    }

    @GetMapping("/tasks")
    ResponseEntity<Map<String, Object>> tasks(HttpServletRequest request,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int pageSize,
                                              @RequestParam(required = false) String status) {
        auth.requireCapability(request, "NODE_READ");
        validatePage(page, pageSize);
        List<Map<String, Object>> items = store.tasks.values().stream()
                .filter(task -> status == null || task.status.equals(status))
                .map(OpsTask::view)
                .toList();
        return ok(request, page(items, page, pageSize));
    }

    @GetMapping("/tasks/{taskId}")
    ResponseEntity<Map<String, Object>> task(HttpServletRequest request, @PathVariable String taskId) {
        auth.requireCapability(request, "NODE_READ");
        return ok(request, store.task(taskId).view());
    }

    @PatchMapping("/tasks/{taskId}/cancel")
    ResponseEntity<Map<String, Object>> cancelTask(HttpServletRequest request, @PathVariable String taskId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.current(request);
        validateText(body, "reason");
        return idempotent(request, actor, "task:cancel:" + taskId, body, () -> {
            synchronized (store) {
                OpsTask task = store.task(taskId);
                if (!task.createdBy.equals(actor.userId) && !List.of("ADMIN", "OWNER").contains(actor.role)) {
                    throw new OpsException(HttpStatus.FORBIDDEN, 42001, "role denied");
                }
                if (!List.of("PENDING_APPROVAL", "QUEUED", "DISPATCHED").contains(task.status)) {
                    throw new OpsException(HttpStatus.CONFLICT, 49410, "task cannot be canceled");
                }
                String before = task.status;
                task.status = "CANCELED";
                task.updatedAt = now();
                store.audit("OPS_TASK_CANCELED", "TASK", taskId, actor, request, body, "MEDIUM", "SUCCESS", null, before, task.status);
                return ok(request, task.view());
            }
        });
    }

    @PostMapping("/tasks/{taskId}/node-result")
    ResponseEntity<Map<String, Object>> nodeResult(HttpServletRequest request, @PathVariable String taskId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireCapability(request, "NODE_WRITE");
        synchronized (store) {
            OpsTask task = store.task(taskId);
            if (!List.of("DISPATCHED", "RUNNING").contains(task.status)) {
                throw new OpsException(HttpStatus.CONFLICT, 49410, "task cannot receive node result");
            }
            String status = text(body, "status", "SUCCEEDED");
            validateNodeResultStatus(status);
            String before = task.status;
            task.status = status;
            task.nodeRequestId = text(body, "nodeRequestId", task.nodeRequestId);
            task.resultSummary = safeMap(body.get("resultSummary"));
            task.failureReason = text(body, "failureReason", null);
            task.updatedAt = now();
            store.audit("OPS_TASK_NODE_RESULT_RECORDED", "TASK", taskId, actor, request, body, "MEDIUM", "SUCCESS", task.failureReason, before, task.status);
            return ok(request, task.view());
        }
    }

    @GetMapping("/approvals")
    ResponseEntity<Map<String, Object>> approvals(HttpServletRequest request,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize) {
        auth.requireCapability(request, "HIGH_RISK_APPROVE");
        validatePage(page, pageSize);
        return ok(request, page(store.approvals.values().stream().map(OpsApproval::view).toList(), page, pageSize));
    }

    @GetMapping("/approvals/{approvalId}")
    ResponseEntity<Map<String, Object>> approval(HttpServletRequest request, @PathVariable String approvalId) {
        auth.requireCapability(request, "HIGH_RISK_APPROVE");
        return ok(request, store.approval(approvalId).view());
    }

    @PatchMapping("/approvals/{approvalId}/approve")
    ResponseEntity<Map<String, Object>> approve(HttpServletRequest request, @PathVariable String approvalId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireCapability(request, "HIGH_RISK_APPROVE");
        return reviewApproval(request, approvalId, body, actor, true);
    }

    @PatchMapping("/approvals/{approvalId}/reject")
    ResponseEntity<Map<String, Object>> reject(HttpServletRequest request, @PathVariable String approvalId, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireCapability(request, "HIGH_RISK_APPROVE");
        return reviewApproval(request, approvalId, body, actor, false);
    }

    @GetMapping("/audit-logs")
    ResponseEntity<Map<String, Object>> auditLogs(HttpServletRequest request,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize,
                                                  @RequestParam(required = false) String targetType,
                                                  @RequestParam(required = false) String targetId,
                                                  @RequestParam(required = false) String action,
                                                  @RequestParam(required = false) String result,
                                                  @RequestParam(required = false) String from,
                                                  @RequestParam(required = false) String to) {
        auth.requireAdmin(auth.current(request));
        validatePage(page, pageSize);
        Instant fromInstant = parseInstant(from);
        Instant toInstant = parseInstant(to);
        List<Map<String, Object>> items = store.audits.stream()
                .filter(audit -> targetType == null || audit.targetType.equals(targetType))
                .filter(audit -> targetId == null || audit.targetId.equals(targetId))
                .filter(audit -> action == null || audit.action.equals(action))
                .filter(audit -> result == null || audit.result.equals(result))
                .filter(audit -> inRange(audit.createdAt, fromInstant, toInstant))
                .map(OpsAudit::view)
                .toList();
        return ok(request, page(items, page, pageSize));
    }

    @GetMapping("/ops/summary")
    ResponseEntity<Map<String, Object>> ops(HttpServletRequest request) {
        auth.requireCapability(request, "NODE_READ");
        return ok(request, store.ops(properties.enabled()));
    }

    private ResponseEntity<Map<String, Object>> reviewApproval(HttpServletRequest request, String approvalId, Map<String, Object> body, Actor actor, boolean approved) {
        validateText(body, "reason");
        return idempotent(request, actor, "approval:" + approvalId + ":" + approved, body, () -> {
            synchronized (store) {
                OpsApproval approval = store.approval(approvalId);
                OpsTask task = store.task(approval.taskId);
                if (!"PENDING".equals(approval.status)) {
                    throw new OpsException(HttpStatus.CONFLICT, 49410, "approval is not pending");
                }
                if ("CRITICAL".equals(approval.riskLevel) && approval.requestedBy.equals(actor.userId)) {
                    throw new OpsException(HttpStatus.CONFLICT, 49416, "self approval denied");
                }
                String before = task.status;
                approval.status = approved ? "APPROVED" : "REJECTED";
                approval.approvedBy = actor.userId;
                approval.reviewComment = text(body, "reviewComment", null);
                approval.reviewedAt = now();
                task.status = approved ? "FAILED" : "FAILED";
                task.failureReason = approved ? "NODE_DAEMON_NOT_CONNECTED" : "APPROVAL_REJECTED";
                task.updatedAt = now();
                store.audit(approved ? "OPS_APPROVAL_APPROVED" : "OPS_APPROVAL_REJECTED", "APPROVAL", approvalId, actor, request, body,
                        approval.riskLevel, "SUCCESS", task.failureReason, before, task.status);
                return ok(request, Map.of("approval", approval.view(), "task", task.view()));
            }
        });
    }

    private void requireTaskCapability(Actor actor, String taskType) {
        String required = switch (taskType) {
            case "CONTAINER_START", "CONTAINER_STOP", "CONTAINER_RESTART", "CONTAINER_DELETE", "MC_START", "MC_STOP", "MC_RESTART" -> "CONTAINER_OPERATE";
            case "VM_START", "VM_SHUTDOWN", "VM_REBOOT", "VM_FORCE_STOP" -> "VM_OPERATE";
            case "FILE_READ", "FILE_WRITE", "FILE_RENAME", "FILE_MOVE", "FILE_DELETE" -> "FILE_MANAGE";
            case "MC_COMMAND", "TERMINAL_COMMAND" -> "TERMINAL_ACCESS";
            default -> "NODE_WRITE";
        };
        auth.requireCapability(actor, required);
    }

    private static void validateTaskType(String taskType) {
        if (!List.of("NODE_REGISTER", "NODE_DISABLE", "NODE_ENABLE", "NODE_TOKEN_ROTATE",
                "CONTAINER_START", "CONTAINER_STOP", "CONTAINER_RESTART", "CONTAINER_DELETE",
                "MC_START", "MC_STOP", "MC_RESTART", "VM_START", "VM_SHUTDOWN", "VM_REBOOT",
                "VM_FORCE_STOP", "FILE_READ", "FILE_WRITE", "FILE_RENAME", "FILE_MOVE",
                "FILE_DELETE", "MC_COMMAND", "TERMINAL_COMMAND", "LOG_QUERY",
                "BACKUP_CREATE", "BACKUP_RESTORE").contains(taskType)) {
            throw new OpsException(HttpStatus.BAD_REQUEST, 40001, "invalid task type");
        }
    }

    private static void validateNodeResultStatus(String status) {
        if (!List.of("SUCCEEDED", "FAILED", "TIMEOUT").contains(status)) {
            throw new OpsException(HttpStatus.BAD_REQUEST, 40001, "invalid task result status");
        }
    }

    private boolean requiresRealtime(String taskType) {
        return !List.of("LOG_QUERY", "FILE_READ").contains(taskType);
    }

    private String risk(String taskType) {
        return switch (taskType) {
            case "CONTAINER_DELETE", "MC_COMMAND", "TERMINAL_COMMAND", "BACKUP_RESTORE", "VM_FORCE_STOP" -> "CRITICAL";
            case "FILE_DELETE", "NODE_DISABLE" -> "HIGH";
            default -> "MEDIUM";
        };
    }

    private boolean validHighConfirm(String taskType, String confirmText) {
        if ("FILE_DELETE".equals(taskType)) {
            return "DELETE_FILE".equals(confirmText);
        }
        return !confirmText.isBlank();
    }

    private void guardPath(String path) {
        if (path == null || !path.startsWith("/") || path.contains("..") || path.contains("\\") || path.chars().anyMatch(ch -> ch < 32)) {
            throw new OpsException(HttpStatus.CONFLICT, 49414, "path escapes root");
        }
    }

    private void rejectTrusted(Map<String, Object> body) {
        for (String key : List.of("actorUserId", "actorRole", "actorPermissions", "createdBy", "updatedBy", "taskStatus", "approvalStatus", "auditResult")) {
            if (body.containsKey(key)) {
                throw new OpsException(HttpStatus.BAD_REQUEST, 40001, "trusted field is not accepted");
            }
        }
    }

    private ResponseEntity<Map<String, Object>> idempotent(HttpServletRequest request, Actor actor, String scope, Map<String, Object> body,
                                                           Supplier<ResponseEntity<Map<String, Object>>> supplier) {
        String key = text(body, "idempotencyKey", null);
        if (key == null || key.isBlank()) {
            return supplier.get();
        }
        String compound = actor.userId + ":" + scope + ":" + key;
        String fingerprint = store.fingerprint(body);
        OpsIdempotency previous = store.idempotency.get(compound);
        if (previous != null) {
            if (!previous.fingerprint().equals(fingerprint)) {
                throw new OpsException(HttpStatus.CONFLICT, 49412, "idempotency fingerprint conflict");
            }
            return envelope(request, previous.status(), previous.data());
        }
        ResponseEntity<Map<String, Object>> response = supplier.get();
        store.idempotency.put(compound, new OpsIdempotency(fingerprint, HttpStatus.valueOf(response.getStatusCode().value()), response.getBody().get("data")));
        return response;
    }

    private static ResponseEntity<Map<String, Object>> ok(HttpServletRequest request, Object data) {
        return envelope(request, HttpStatus.OK, data);
    }

    private static ResponseEntity<Map<String, Object>> created(HttpServletRequest request, Object data) {
        return envelope(request, HttpStatus.CREATED, data);
    }

    private static ResponseEntity<Map<String, Object>> envelope(HttpServletRequest request, HttpStatus status, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 0);
        body.put("message", "success");
        body.put("data", data);
        body.put("requestId", request.getAttribute("requestId"));
        return ResponseEntity.status(status).body(body);
    }

    private static Map<String, Object> page(List<Map<String, Object>> items, int page, int pageSize) {
        int from = Math.min((page - 1) * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items.subList(from, to));
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("total", items.size());
        return data;
    }

    private static void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new OpsException(HttpStatus.BAD_REQUEST, 40002, "invalid pagination");
        }
    }

    private static void validateSort(String sort, String... allowed) {
        if (sort != null && !List.of(allowed).contains(sort)) {
            throw new OpsException(HttpStatus.BAD_REQUEST, 40003, "invalid sort");
        }
    }

    private static void validateText(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new OpsException(HttpStatus.BAD_REQUEST, 40001, key + " is required");
        }
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception exception) {
            throw new OpsException(HttpStatus.BAD_REQUEST, 40001, "invalid time");
        }
    }

    private static boolean inRange(String createdAt, Instant from, Instant to) {
        Instant instant = Instant.parse(createdAt);
        return (from == null || !instant.isBefore(from)) && (to == null || !instant.isAfter(to));
    }

    private static Comparator<OpsAsset> assetComparator(String sort) {
        return switch (sort == null ? "updatedAt_desc" : sort) {
            case "displayName_asc" -> Comparator.comparing(asset -> asset.displayName);
            case "assetType_asc" -> Comparator.comparing(asset -> asset.assetType);
            case "createdAt_desc" -> Comparator.comparing((OpsAsset asset) -> asset.createdAt).reversed();
            default -> Comparator.comparing((OpsAsset asset) -> asset.updatedAt).reversed();
        };
    }

    private static Comparator<OpsNode> nodeComparator(String sort) {
        return switch (sort == null ? "updatedAt_desc" : sort) {
            case "displayName_asc" -> Comparator.comparing(node -> node.displayName);
            case "createdAt_desc" -> Comparator.comparing((OpsNode node) -> node.createdAt).reversed();
            default -> Comparator.comparing((OpsNode node) -> node.updatedAt).reversed();
        };
    }

    private static String text(Map<String, Object> body, String key, String fallback) {
        Object value = body == null ? null : body.get(key);
        return value == null ? fallback : value.toString();
    }

    private static int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> safeMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> safe = new LinkedHashMap<>();
            map.forEach((key, nested) -> safe.put(key.toString(), nested));
            return safe;
        }
        return Map.of();
    }

    private static String now() {
        return Instant.now().toString();
    }
}

@Service
class OpsStore {
    final Map<String, OpsNode> nodes = new ConcurrentHashMap<>();
    final Map<String, OpsAsset> assets = new ConcurrentHashMap<>();
    final Map<String, OpsMetric> metrics = new ConcurrentHashMap<>();
    final Map<String, OpsContainer> containers = new ConcurrentHashMap<>();
    final Map<String, OpsVm> vms = new ConcurrentHashMap<>();
    final Map<String, OpsMinecraft> minecraft = new ConcurrentHashMap<>();
    final Map<String, OpsFile> files = new ConcurrentHashMap<>();
    final Map<String, OpsTask> tasks = new ConcurrentHashMap<>();
    final Map<String, OpsApproval> approvals = new ConcurrentHashMap<>();
    final Map<String, OpsIdempotency> idempotency = new ConcurrentHashMap<>();
    final List<OpsAudit> audits = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private int sequence = 1000;
    private final OpsProperties properties;

    OpsStore(OpsProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void seed() {
        OpsNode node = new OpsNode("node-main", "北冥主节点", "https://node-main.example:9443", List.of("NODE_READ", "NODE_WRITE", "CONTAINER_OPERATE", "FILE_MANAGE", "TERMINAL_ACCESS"), "system");
        node.status = "ONLINE";
        node.version = "0.1.0-simulated";
        node.lastHeartbeatAt = now();
        nodes.put(node.nodeId, node);
        assets.put("asset-mc-survival", new OpsAsset("asset-mc-survival", "node-main", "MINECRAFT_INSTANCE", "生存主服", "ACTIVE", "SERVER_STATUS", List.of("core")));
        assets.put("asset-container-main", new OpsAsset("asset-container-main", "node-main", "DOCKER_CONTAINER", "主服容器", "ACTIVE", null, List.of("runtime")));
        metrics.put("node-main", new OpsMetric("metric-seed-1", "node-main", 28.5, 46.2, 60.1));
        containers.put("container-seed-1", new OpsContainer("container-seed-1", "node-main", "beiming-survival", "itzg/minecraft-server:latest", "RUNNING"));
        vms.put("vm-build-1", new OpsVm("vm-build-1", "node-main", "build-runner", "SIMULATED", "STOPPED"));
        minecraft.put("mc-survival", new OpsMinecraft("mc-survival", "node-main", "survival-main", "北冥生存服", "1.20.4", "RUNNING"));
        String configName = "runtime-config.txt";
        files.put("/" + configName, new OpsFile("node-main", "mc-config", "/" + configName, configName, "FILE", 1024L, true));
        files.put("/binary.dat", new OpsFile("node-main", "mc-config", "/binary.dat", "binary.dat", "FILE", 4096L, false));
        audits.add(new OpsAudit("audit-seed-1", "OPS_NODE_HEARTBEAT", "NODE", "node-main", "system", "SYSTEM", "LOW", "SUCCESS", "seed", Map.of(), null, "ONLINE", null));
    }

    String nextId() {
        sequence += 1;
        return Integer.toString(sequence);
    }

    OpsNode node(String nodeId) {
        return Optional.ofNullable(nodes.get(nodeId)).orElseThrow(() -> new OpsException(HttpStatus.NOT_FOUND, 49401, "node not found"));
    }

    OpsAsset asset(String assetId) {
        return Optional.ofNullable(assets.get(assetId)).orElseThrow(() -> new OpsException(HttpStatus.NOT_FOUND, 49400, "asset not found"));
    }

    OpsTask task(String taskId) {
        return Optional.ofNullable(tasks.get(taskId)).orElseThrow(() -> new OpsException(HttpStatus.NOT_FOUND, 49402, "task not found"));
    }

    OpsApproval approval(String approvalId) {
        return Optional.ofNullable(approvals.get(approvalId)).orElseThrow(() -> new OpsException(HttpStatus.NOT_FOUND, 49403, "approval not found"));
    }

    void failAuditIfRequested(HttpServletRequest request) {
        if (properties.enabled() && "true".equals(request.getHeader("X-Test-Fail-Audit"))) {
            throw new OpsException(HttpStatus.INTERNAL_SERVER_ERROR, 55001, "audit write failed");
        }
    }

    void audit(String action, String targetType, String targetId, Actor actor, HttpServletRequest request, Map<String, Object> body,
               String riskLevel, String result, String failureReason, String beforeState, String afterState) {
        audits.add(0, new OpsAudit("audit-" + nextId(), action, targetType, targetId, actor.userId, actor.role, riskLevel,
                result, text(body, "reason", "system"), summary(body), beforeState, afterState, failureReason));
    }

    Map<String, Object> ops(boolean testControlsEnabled) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "ops-control");
        data.put("port", 8116);
        data.put("storageMode", "IN_MEMORY");
        data.put("authMode", "TEST_STUB");
        data.put("adminAdapterMode", "TEST_STUB");
        data.put("nodeAdapterMode", "SIMULATED");
        data.put("nodeDaemonConnected", false);
        data.put("testControlsEnabled", testControlsEnabled);
        data.put("nodesTotal", nodes.size());
        data.put("assetsTotal", assets.size());
        data.put("tasksTotal", tasks.size());
        data.put("pendingApprovalsTotal", approvals.values().stream().filter(approval -> "PENDING".equals(approval.status)).count());
        data.put("auditsTotal", audits.size());
        data.put("idempotencyRecordsTotal", idempotency.size());
        data.put("lastHeartbeatAt", nodes.values().stream().map(node -> node.lastHeartbeatAt).filter(Objects::nonNull).max(String::compareTo).orElse(null));
        data.put("lastAuditAt", audits.isEmpty() ? null : audits.get(0).createdAt);
        data.put("productionGaps", List.of("P1_IN_MEMORY_STORAGE", "P1_AUTH_STUB", "P1_ADMIN_STUB", "NODE_DAEMON_NOT_CONNECTED",
                "REAL_DOCKER_NOT_CONNECTED", "REAL_VM_PLATFORM_NOT_CONNECTED", "REAL_MCSMANAGER_NOT_CONNECTED", "TEST_CONTROLS_DISABLED_OUTSIDE_TEST"));
        return data;
    }

    String fingerprint(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(canonical(body));
        } catch (Exception exception) {
            throw new OpsException(HttpStatus.BAD_REQUEST, 40001, "invalid json body");
        }
    }

    Map<String, Object> summary(Map<String, Object> body) {
        Map<String, Object> summary = new LinkedHashMap<>();
        body.forEach((key, value) -> {
            if (!key.toLowerCase().contains("secret") && !key.toLowerCase().contains("authorization")) {
                summary.put(key, value instanceof Map<?, ?> ? "object" : value);
            }
        });
        return summary;
    }

    private Object canonical(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            map.forEach((key, nested) -> sorted.put(key.toString(), canonical(nested)));
            return sorted;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::canonical).toList();
        }
        return value;
    }

    private static String text(Map<String, Object> body, String key, String fallback) {
        Object value = body == null ? null : body.get(key);
        return value == null ? fallback : value.toString();
    }

    private static String now() {
        return Instant.now().toString();
    }
}

record OpsIdempotency(String fingerprint, HttpStatus status, Object data) {
}

class OpsNode {
    final String nodeId;
    final String displayName;
    final String endpointSummary;
    String status = "PENDING_REGISTRATION";
    String version;
    List<String> capabilities;
    final Map<String, Object> labels = new LinkedHashMap<>();
    String lastHeartbeatAt;
    String lastSeenRequestId;
    final String tokenDigest = "sha256:****";
    final String createdBy;
    String updatedBy;
    final String createdAt = Instant.now().toString();
    String updatedAt = createdAt;

    OpsNode(String nodeId, String displayName, String endpointSummary, List<String> capabilities, String createdBy) {
        this.nodeId = nodeId;
        this.displayName = displayName;
        this.endpointSummary = endpointSummary;
        this.capabilities = capabilities.isEmpty() ? List.of("NODE_READ") : capabilities;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("nodeId", nodeId);
        view.put("displayName", displayName);
        view.put("status", status);
        view.put("endpointSummary", endpointSummary);
        view.put("version", version);
        view.put("capabilities", capabilities);
        view.put("labels", labels);
        view.put("lastHeartbeatAt", lastHeartbeatAt);
        view.put("lastSeenRequestId", lastSeenRequestId);
        view.put("tokenDigest", tokenDigest);
        view.put("createdBy", createdBy);
        view.put("updatedBy", updatedBy);
        view.put("createdAt", createdAt);
        view.put("updatedAt", updatedAt);
        return view;
    }
}

class OpsAsset {
    final String assetId;
    final String nodeId;
    final String assetType;
    final String displayName;
    final String status;
    final String ownerModule;
    final List<String> riskTags;
    final String createdAt = Instant.now().toString();
    final String updatedAt = createdAt;

    OpsAsset(String assetId, String nodeId, String assetType, String displayName, String status, String ownerModule, List<String> riskTags) {
        this.assetId = assetId;
        this.nodeId = nodeId;
        this.assetType = assetType;
        this.displayName = displayName;
        this.status = status;
        this.ownerModule = ownerModule;
        this.riskTags = riskTags;
    }

    Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("assetId", assetId);
        view.put("nodeId", nodeId);
        view.put("assetType", assetType);
        view.put("displayName", displayName);
        view.put("status", status);
        view.put("ownerModule", ownerModule);
        view.put("sourceRef", ownerModule == null ? null : Map.of("sourceModule", ownerModule, "stale", false));
        view.put("runtimeStatus", assetType.contains("MINECRAFT") ? "RUNNING" : null);
        view.put("publicVisible", true);
        view.put("riskTags", riskTags);
        view.put("createdAt", createdAt);
        view.put("updatedAt", updatedAt);
        return view;
    }
}

class OpsMetric {
    final String snapshotId;
    final String nodeId;
    final double cpuUsagePercent;
    final double memoryUsagePercent;
    final double diskUsagePercent;
    final String collectedAt = Instant.now().toString();

    OpsMetric(String snapshotId, String nodeId, double cpuUsagePercent, double memoryUsagePercent, double diskUsagePercent) {
        this.snapshotId = snapshotId;
        this.nodeId = nodeId;
        this.cpuUsagePercent = cpuUsagePercent;
        this.memoryUsagePercent = memoryUsagePercent;
        this.diskUsagePercent = diskUsagePercent;
    }

    static OpsMetric from(String nodeId, Map<?, ?> source) {
        return new OpsMetric("metric-" + UUID.randomUUID(), nodeId, number(source.get("cpuUsagePercent"), 0), number(source.get("memoryUsagePercent"), 0), number(source.get("diskUsagePercent"), 0));
    }

    Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("snapshotId", snapshotId);
        view.put("nodeId", nodeId);
        view.put("cpuUsagePercent", cpuUsagePercent);
        view.put("memoryUsagePercent", memoryUsagePercent);
        view.put("diskUsagePercent", diskUsagePercent);
        view.put("networkRxBytes", 1024);
        view.put("networkTxBytes", 2048);
        view.put("loadAverage", List.of(0.2, 0.3, 0.4));
        view.put("recentEvents", List.of());
        view.put("collectedAt", collectedAt);
        view.put("degraded", false);
        return view;
    }

    private static double number(Object value, double fallback) {
        return value instanceof Number number ? number.doubleValue() : fallback;
    }
}

class OpsContainer {
    final String containerId;
    final String nodeId;
    final String name;
    final String image;
    final String status;
    final String lastSyncedAt = Instant.now().toString();

    OpsContainer(String containerId, String nodeId, String name, String image, String status) {
        this.containerId = containerId;
        this.nodeId = nodeId;
        this.name = name;
        this.image = image;
        this.status = status;
    }

    Map<String, Object> view(boolean stale) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("containerId", containerId);
        view.put("nodeId", nodeId);
        view.put("name", name);
        view.put("image", image);
        view.put("runtime", "DOCKER");
        view.put("status", status);
        view.put("ports", List.of(Map.of("containerPort", 25565, "public", true)));
        view.put("mounts", List.of(Map.of("alias", "mc-config", "mode", "rw")));
        view.put("resourceUsage", Map.of("cpu", 12, "memoryMb", 2048));
        view.put("lastSyncedAt", lastSyncedAt);
        view.put("stale", stale);
        return view;
    }
}

class OpsVm {
    final String vmId;
    final String nodeId;
    final String name;
    final String platform;
    final String status;
    final String lastSyncedAt = Instant.now().toString();

    OpsVm(String vmId, String nodeId, String name, String platform, String status) {
        this.vmId = vmId;
        this.nodeId = nodeId;
        this.name = name;
        this.platform = platform;
        this.status = status;
    }

    Map<String, Object> view() {
        return Map.of("vmId", vmId, "nodeId", nodeId, "name", name, "platform", platform, "status", status,
                "cpuCores", 4, "memoryMb", 8192, "diskGb", 80, "networkSummary", Map.of("primary", "masked"), "lastSyncedAt", lastSyncedAt);
    }
}

class OpsMinecraft {
    final String instanceId;
    final String nodeId;
    final String publicInstanceId;
    final String name;
    final String version;
    final String status;
    final String lastSyncedAt = Instant.now().toString();

    OpsMinecraft(String instanceId, String nodeId, String publicInstanceId, String name, String version, String status) {
        this.instanceId = instanceId;
        this.nodeId = nodeId;
        this.publicInstanceId = publicInstanceId;
        this.name = name;
        this.version = version;
        this.status = status;
    }

    Map<String, Object> view() {
        return Map.of("instanceId", instanceId, "nodeId", nodeId, "publicInstanceId", publicInstanceId, "name", name, "version", version,
                "status", status, "onlinePlayers", 3, "directoryAlias", "mc-config", "startCommandSummary", "java -jar <redacted>", "lastSyncedAt", lastSyncedAt);
    }
}

class OpsFile {
    final String nodeId;
    final String rootAlias;
    final String path;
    final String name;
    final String type;
    final Long sizeBytes;
    final boolean editableText;
    final String modifiedAt = Instant.now().toString();

    OpsFile(String nodeId, String rootAlias, String path, String name, String type, Long sizeBytes, boolean editableText) {
        this.nodeId = nodeId;
        this.rootAlias = rootAlias;
        this.path = path;
        this.name = name;
        this.type = type;
        this.sizeBytes = sizeBytes;
        this.editableText = editableText;
    }

    Map<String, Object> view() {
        return Map.of("nodeId", nodeId, "rootAlias", rootAlias, "path", path, "name", name, "type", type,
                "sizeBytes", sizeBytes, "editableText", editableText, "modifiedAt", modifiedAt);
    }
}

class OpsTask {
    final String taskId;
    final String taskType;
    String status = "QUEUED";
    final String riskLevel;
    final String nodeId;
    final String targetType;
    final String targetId;
    final String reason;
    final Map<String, Object> paramsSummary;
    String approvalId;
    String idempotencyKey;
    String nodeRequestId;
    Map<String, Object> resultSummary;
    String failureReason;
    final String createdBy;
    final String createdAt = Instant.now().toString();
    String updatedAt = createdAt;
    final String expiresAt = Instant.now().plusSeconds(900).toString();

    OpsTask(String taskId, String taskType, String nodeId, String targetType, String targetId, String reason, String createdBy,
            String riskLevel, Map<String, Object> paramsSummary) {
        this.taskId = taskId;
        this.taskType = taskType;
        this.nodeId = nodeId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.createdBy = createdBy;
        this.riskLevel = riskLevel;
        this.paramsSummary = paramsSummary;
    }

    Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("taskId", taskId);
        view.put("taskType", taskType);
        view.put("status", status);
        view.put("riskLevel", riskLevel);
        view.put("nodeId", nodeId);
        view.put("targetType", targetType);
        view.put("targetId", targetId);
        view.put("reason", reason);
        view.put("paramsSummary", paramsSummary);
        view.put("approvalId", approvalId);
        view.put("idempotencyKey", idempotencyKey);
        view.put("nodeRequestId", nodeRequestId);
        view.put("resultSummary", resultSummary);
        view.put("failureReason", failureReason);
        view.put("createdBy", createdBy);
        view.put("createdAt", createdAt);
        view.put("updatedAt", updatedAt);
        view.put("expiresAt", expiresAt);
        return view;
    }
}

class OpsApproval {
    final String approvalId;
    final String taskId;
    String status = "PENDING";
    final String riskLevel;
    final String requestedBy;
    String approvedBy;
    String reviewComment;
    final String createdAt = Instant.now().toString();
    String reviewedAt;
    final String expiresAt = Instant.now().plusSeconds(3600).toString();

    OpsApproval(String approvalId, String taskId, String riskLevel, String requestedBy) {
        this.approvalId = approvalId;
        this.taskId = taskId;
        this.riskLevel = riskLevel;
        this.requestedBy = requestedBy;
    }

    Map<String, Object> view() {
        return map("approvalId", approvalId, "taskId", taskId, "status", status, "riskLevel", riskLevel,
                "requestedBy", requestedBy, "approvedBy", approvedBy, "reviewComment", reviewComment,
                "createdAt", createdAt, "reviewedAt", reviewedAt, "expiresAt", expiresAt);
    }

    private Map<String, Object> map(Object... values) {
        Map<String, Object> view = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            view.put(values[i].toString(), values[i + 1]);
        }
        return view;
    }
}

class OpsAudit {
    final String auditId;
    final String action;
    final String targetType;
    final String targetId;
    final String actorUserId;
    final String actorRole;
    final String riskLevel;
    final String result;
    final String reason;
    final Map<String, Object> paramsSummary;
    final String beforeState;
    final String afterState;
    final String failureReason;
    final String requestId = "req-audit";
    final String createdAt = Instant.now().toString();

    OpsAudit(String auditId, String action, String targetType, String targetId, String actorUserId, String actorRole,
             String riskLevel, String result, String reason, Map<String, Object> paramsSummary, String beforeState,
             String afterState, String failureReason) {
        this.auditId = auditId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.actorUserId = actorUserId;
        this.actorRole = actorRole;
        this.riskLevel = riskLevel;
        this.result = result;
        this.reason = reason;
        this.paramsSummary = paramsSummary;
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.failureReason = failureReason;
    }

    Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", auditId);
        view.put("requestId", requestId);
        view.put("actorUserId", actorUserId);
        view.put("actorRole", actorRole);
        view.put("actorPermissions", List.of());
        view.put("sourceIp", null);
        view.put("targetType", targetType);
        view.put("targetId", targetId);
        view.put("action", action);
        view.put("riskLevel", riskLevel);
        view.put("reason", reason);
        view.put("paramsSummary", paramsSummary);
        view.put("beforeState", beforeState);
        view.put("afterState", afterState);
        view.put("result", result);
        view.put("failureReason", failureReason);
        view.put("createdAt", createdAt);
        return view;
    }
}

@Service
class OpsAuth {
    private final OpsProperties properties;

    OpsAuth(OpsProperties properties) {
        this.properties = properties;
    }

    Actor current(HttpServletRequest request) {
        if (properties.enabled() && "unavailable".equals(request.getHeader("X-Test-Auth-Mode"))) {
            throw new OpsException(HttpStatus.BAD_GATEWAY, 49200, "auth unavailable");
        }
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            throw new OpsException(HttpStatus.UNAUTHORIZED, 41000, "unauthenticated");
        }
        if (!header.startsWith("Bearer ")) {
            throw new OpsException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        }
        return switch (header.substring("Bearer ".length())) {
            case "owner-token" -> new Actor("owner-user", "OWNER", List.of("NODE_READ", "NODE_WRITE", "CONTAINER_OPERATE", "VM_OPERATE", "FILE_MANAGE", "TERMINAL_ACCESS", "HIGH_RISK_APPROVE"));
            case "ops-admin-token" -> new Actor("ops-admin-user", "ADMIN", List.of("NODE_READ", "NODE_WRITE", "CONTAINER_OPERATE", "VM_OPERATE", "FILE_MANAGE"));
            case "ops-viewer-token" -> new Actor("ops-viewer-user", "HELPER", List.of("NODE_READ"));
            case "ops-node-writer-token" -> new Actor("ops-node-writer-user", "ADMIN", List.of("NODE_READ", "NODE_WRITE"));
            case "ops-container-token" -> new Actor("ops-container-user", "ADMIN", List.of("NODE_READ", "CONTAINER_OPERATE"));
            case "ops-file-token" -> new Actor("ops-file-user", "ADMIN", List.of("NODE_READ", "FILE_MANAGE"));
            case "ops-terminal-token" -> new Actor("ops-terminal-user", "ADMIN", List.of("NODE_READ", "TERMINAL_ACCESS", "HIGH_RISK_APPROVE"));
            case "ops-approver-token" -> new Actor("ops-approver-user", "ADMIN", List.of("NODE_READ", "HIGH_RISK_APPROVE"));
            case "user-token" -> new Actor("plain-user", "USER", List.of());
            default -> throw new OpsException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        };
    }

    Actor requireCapability(HttpServletRequest request, String capability) {
        return requireCapability(current(request), capability);
    }

    Actor requireCapability(Actor actor, String capability) {
        if (!actor.permissions.contains(capability)) {
            int code = "USER".equals(actor.role) ? 42001 : 42002;
            throw new OpsException(HttpStatus.FORBIDDEN, code, "capability denied");
        }
        return actor;
    }

    void requireAdmin(Actor actor) {
        if (!List.of("ADMIN", "OWNER").contains(actor.role)) {
            throw new OpsException(HttpStatus.FORBIDDEN, 42001, "role denied");
        }
    }
}

class Actor {
    final String userId;
    final String role;
    final List<String> permissions;

    Actor(String userId, String role, List<String> permissions) {
        this.userId = userId;
        this.role = role;
        this.permissions = permissions;
    }
}

@Component
class OpsProperties {
    private final boolean enabled;

    OpsProperties(@Value("${ops-control.test-controls.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    boolean enabled() {
        return enabled;
    }
}

@Component
class OpsRequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = Optional.ofNullable(request.getHeader("X-Request-Id"))
                .filter(value -> !value.isBlank())
                .orElse("req_" + UUID.randomUUID());
        request.setAttribute("requestId", requestId);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Request-Id", requestId);
        filterChain.doFilter(request, response);
    }
}

@RestControllerAdvice
class OpsExceptionHandler {
    @ExceptionHandler(OpsException.class)
    ResponseEntity<Map<String, Object>> api(OpsException exception, HttpServletRequest request) {
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
        body.put("code", 55000);
        body.put("message", "ops-control internal error");
        body.put("data", null);
        body.put("requestId", request.getAttribute("requestId"));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

class OpsException extends RuntimeException {
    final HttpStatus status;
    final int code;

    OpsException(HttpStatus status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
