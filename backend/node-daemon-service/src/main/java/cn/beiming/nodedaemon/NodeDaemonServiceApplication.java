package cn.beiming.nodedaemon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class NodeDaemonServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NodeDaemonServiceApplication.class, args);
    }
}

@RestController
@RequestMapping("/api/v1/node-daemon")
class NodeDaemonController {
    private static final String VERSION = "0.1.0-simulated";
    private static final String VALID_SIGNATURE = "test-signature";
    private static final String NOW = "2026-05-25T15:00:00Z";

    private final String nodeId;
    private final String nodeToken;
    private final String nodeSigningSecret;
    private final long allowedClockSkewSeconds;
    private final boolean testControlsEnabled;
    private final ObjectMapper objectMapper;
    private final Map<String, IdempotencyRecord> idempotency = new ConcurrentHashMap<>();
    private final Map<String, String> nodeRequestSignatures = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> tasks = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> audits = Collections.synchronizedList(new ArrayList<>());

    NodeDaemonController(
            @Value("${node-daemon.node-id:node-main}") String nodeId,
            @Value("${node-daemon.node-token:node-token-valid}") String nodeToken,
            @Value("${node-daemon.node-signing-secret:local-node-signing-secret}") String nodeSigningSecret,
            @Value("${node-daemon.allowed-clock-skew-seconds:300}") long allowedClockSkewSeconds,
            @Value("${node-daemon.test-controls.enabled:false}") boolean testControlsEnabled,
            ObjectMapper objectMapper
    ) {
        this.nodeId = nodeId;
        this.nodeToken = nodeToken;
        this.nodeSigningSecret = nodeSigningSecret;
        this.allowedClockSkewSeconds = allowedClockSkewSeconds;
        this.testControlsEnabled = testControlsEnabled;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/health")
    ResponseEntity<Map<String, Object>> health(HttpServletRequest request) {
        return ok(request, Map.of(
                "service", "node-daemon",
                "status", "READY",
                "version", VERSION
        ));
    }

    @GetMapping("/ops/summary")
    ResponseEntity<Map<String, Object>> summary(HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> auth = requireNodeAuth(request);
        if (auth != null) {
            return auth;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "node-daemon");
        data.put("port", 8117);
        data.put("nodeId", nodeId);
        data.put("mode", "SIMULATED");
        data.put("status", "READY");
        data.put("version", VERSION);
        data.put("authMode", "HMAC_CONFIGURED");
        data.put("signatureAlgorithm", "HmacSHA256");
        data.put("nodeIdBound", !nodeId.isBlank());
        data.put("allowedClockSkewSeconds", allowedClockSkewSeconds);
        data.put("replayWindowMode", "NODE_REQUEST_ID_IN_MEMORY");
        data.put("opsControlEndpointSummary", "ops-control:8116");
        data.put("testControlsEnabled", testControlsEnabled);
        data.put("runtimeAdapters", Map.of(
                "container", "SIMULATED",
                "vm", "SIMULATED",
                "minecraft", "SIMULATED",
                "file", "READ_ONLY",
                "log", "SUMMARY_ONLY"
        ));
        data.put("productionGaps", testControlsEnabled
                ? List.of("TEST_CONTROLS_ENABLED_FOR_LOCAL_TESTS", "SIMULATED_RUNTIME", "REAL_TERMINAL_DISABLED")
                : List.of("TEST_CONTROLS_DISABLED_OUTSIDE_TEST", "SIMULATED_RUNTIME", "REAL_TERMINAL_DISABLED"));
        return ok(request, data);
    }

    @GetMapping("/capabilities")
    ResponseEntity<Map<String, Object>> capabilities(HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> auth = requireNodeAuth(request);
        if (auth != null) {
            return auth;
        }
        return ok(request, Map.of(
                "supportedCapabilities", List.of("NODE_READ", "NODE_WRITE", "FILE_MANAGE", "CONTAINER_OPERATE", "TERMINAL_ACCESS"),
                "controlPlaneAllowedCapabilities", List.of("NODE_READ", "NODE_WRITE", "FILE_MANAGE", "CONTAINER_OPERATE"),
                "executableCapabilities", List.of("NODE_READ", "NODE_WRITE", "FILE_MANAGE", "CONTAINER_OPERATE"),
                "plannedCapabilities", List.of(Map.of("capability", "TERMINAL_ACCESS", "available", false))
        ));
    }

    @PostMapping("/registration/handshake")
    ResponseEntity<Map<String, Object>> registrationHandshake(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body
    ) {
        ResponseEntity<Map<String, Object>> auth = requireNodeAuth(request, body);
        if (auth != null) {
            return auth;
        }
        ResponseEntity<Map<String, Object>> trustedError = rejectTrustedFields(request, body);
        if (trustedError != null) {
            return trustedError;
        }
        if (!nodeId.equals(text(body.get("controlPlaneNodeId")))) {
            return error(request, HttpStatus.FORBIDDEN, 49603, "node id mismatch");
        }
        ResponseEntity<Map<String, Object>> replay = replayOrConflict(request, body, "handshake:" + text(body.get("idempotencyKey")));
        if (replay != null) {
            return replay;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("handshakeId", "handshake-" + text(body.get("idempotencyKey")));
        data.put("nodeId", nodeId);
        data.put("daemonVersion", text(body.get("daemonVersion")));
        data.put("signatureAlgorithm", "HMAC-SHA256");
        data.put("capabilities", body.getOrDefault("capabilities", List.of()));
        remember(body, "handshake:" + text(body.get("idempotencyKey")), data, HttpStatus.OK);
        audit(request, "REGISTRATION_HANDSHAKE", null, "SUCCESS");
        return ok(request, data);
    }

    @GetMapping("/runtime/snapshot")
    ResponseEntity<Map<String, Object>> runtimeSnapshot(HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> auth = requireNodeAuth(request);
        if (auth != null) {
            return auth;
        }
        boolean degraded = testControlsEnabled && "unavailable".equals(request.getHeader("X-Test-Runtime-Mode"));
        return ok(request, snapshot(degraded));
    }

    @PostMapping("/runtime/heartbeat")
    ResponseEntity<Map<String, Object>> heartbeat(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        ResponseEntity<Map<String, Object>> auth = requireNodeAuth(request, body);
        if (auth != null) {
            return auth;
        }
        ResponseEntity<Map<String, Object>> trustedError = rejectTrustedFields(request, body);
        if (trustedError != null) {
            return trustedError;
        }
        if (testControlsEnabled && "true".equals(request.getHeader("X-Test-Fail-Audit"))) {
            return error(request, HttpStatus.INTERNAL_SERVER_ERROR, 55201, "local audit write failed");
        }
        boolean dryRun = Boolean.TRUE.equals(body.get("dryRun"));
        if (!dryRun && testControlsEnabled && "unavailable".equals(request.getHeader("X-Test-Ops-Control-Mode"))) {
            return error(request, HttpStatus.BAD_GATEWAY, 49611, "ops-control unavailable");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dryRun", dryRun);
        data.put("nodeId", nodeId);
        data.put("callbackPath", "/api/v1/ops-control/nodes/" + nodeId + "/heartbeat");
        data.put("payloadSummary", snapshot(false));
        data.put("opsControlStatus", dryRun ? "DRY_RUN" : "AVAILABLE");
        audit(request, "HEARTBEAT_TRIGGERED", null, "SUCCESS");
        return ok(request, data);
    }

    @PostMapping("/tasks")
    ResponseEntity<Map<String, Object>> receiveTask(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        ResponseEntity<Map<String, Object>> auth = requireNodeAuth(request, body);
        if (auth != null) {
            return auth;
        }
        ResponseEntity<Map<String, Object>> trustedError = rejectTrustedFields(request, body);
        if (trustedError != null) {
            return trustedError;
        }
        String nodeRequestId = text(body.get("nodeRequestId"));
        ResponseEntity<Map<String, Object>> replay = replayOrConflict(request, body, "task:" + nodeRequestId);
        if (replay != null) {
            return replay;
        }
        if (!nodeId.equals(text(body.get("nodeId")))) {
            return error(request, HttpStatus.FORBIDDEN, 49603, "node id mismatch");
        }
        String taskType = text(body.get("taskType"));
        String targetType = text(body.get("targetType"));
        String targetId = text(body.get("targetId"));
        if (!allowedTask(taskType)) {
            return error(request, HttpStatus.FORBIDDEN, 49604, "capability unsupported");
        }
        if (testControlsEnabled && "unavailable".equals(request.getHeader("X-Test-Runtime-Mode"))) {
            return error(request, HttpStatus.CONFLICT, 49607, "runtime unavailable");
        }
        if (expired(text(body.get("expiresAt")))) {
            return error(request, HttpStatus.CONFLICT, 49606, "task expired");
        }
        if (!targetExists(targetType, targetId)) {
            return error(request, HttpStatus.NOT_FOUND, 49608, "target not found");
        }
        String status = testControlsEnabled && "queued".equals(request.getHeader("X-Test-Runtime-Mode")) ? "RECEIVED" : "SUCCEEDED";
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("taskId", text(body.get("taskId")));
        task.put("nodeRequestId", nodeRequestId);
        task.put("taskType", taskType);
        task.put("status", status);
        task.put("riskLevel", text(body.get("riskLevel")));
        task.put("nodeId", nodeId);
        task.put("targetType", targetType);
        task.put("targetId", targetId);
        task.put("paramsSummary", body.getOrDefault("paramsSummary", Map.of()));
        task.put("reason", text(body.get("reason")));
        task.put("receivedAt", NOW);
        task.put("startedAt", "RECEIVED".equals(status) ? null : NOW);
        task.put("finishedAt", "RECEIVED".equals(status) ? null : NOW);
        task.put("expiresAt", text(body.get("expiresAt")));
        task.put("resultSummary", "RECEIVED".equals(status) ? null : Map.of("mode", "SIMULATED", "message", "accepted by safe local adapter"));
        task.put("failureReason", null);
        tasks.put(nodeRequestId, task);
        remember(body, "task:" + nodeRequestId, task, HttpStatus.CREATED);
        audit(request, "TASK_RECEIVED", nodeRequestId, "SUCCESS");
        return created(request, task);
    }

    @GetMapping("/tasks")
    ResponseEntity<Map<String, Object>> listTasks(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String sort
    ) {
        ResponseEntity<Map<String, Object>> auth = requireNodeAuth(request);
        if (auth != null) {
            return auth;
        }
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            return error(request, HttpStatus.BAD_REQUEST, 40002, "invalid page");
        }
        if (sort != null && !List.of("receivedAt_desc", "finishedAt_desc").contains(sort)) {
            return error(request, HttpStatus.BAD_REQUEST, 40003, "invalid sort");
        }
        List<Map<String, Object>> items = tasks.values().stream()
                .filter(task -> status == null || status.equals(task.get("status")))
                .filter(task -> taskType == null || taskType.equals(task.get("taskType")))
                .filter(task -> targetType == null || targetType.equals(task.get("targetType")))
                .sorted(Comparator.comparing(task -> text(task.get("receivedAt")), Comparator.reverseOrder()))
                .toList();
        return ok(request, page(items, page, pageSize));
    }

    @GetMapping("/tasks/{nodeRequestId}")
    ResponseEntity<Map<String, Object>> taskDetail(HttpServletRequest request, @PathVariable String nodeRequestId) {
        ResponseEntity<Map<String, Object>> auth = requireNodeAuth(request);
        if (auth != null) {
            return auth;
        }
        Map<String, Object> task = tasks.get(nodeRequestId);
        if (task == null) {
            return error(request, HttpStatus.NOT_FOUND, 49608, "task not found");
        }
        return ok(request, task);
    }

    @PatchMapping("/tasks/{nodeRequestId}/cancel")
    ResponseEntity<Map<String, Object>> cancelTask(
            HttpServletRequest request,
            @PathVariable String nodeRequestId,
            @RequestBody Map<String, Object> body
    ) {
        ResponseEntity<Map<String, Object>> auth = requireNodeAuth(request, body);
        if (auth != null) {
            return auth;
        }
        Map<String, Object> task = tasks.get(nodeRequestId);
        if (task == null) {
            return error(request, HttpStatus.NOT_FOUND, 49608, "task not found");
        }
        String status = text(task.get("status"));
        if (!"RECEIVED".equals(status) && !"RUNNING".equals(status)) {
            return error(request, HttpStatus.CONFLICT, 49606, "task state conflict");
        }
        task.put("status", "CANCELED");
        task.put("finishedAt", NOW);
        task.put("resultSummary", Map.of("mode", "SIMULATED", "message", "canceled before execution"));
        audit(request, "TASK_CANCELED", nodeRequestId, "SUCCESS");
        return ok(request, task);
    }

    @GetMapping("/tasks/{nodeRequestId}/result")
    ResponseEntity<Map<String, Object>> taskResult(HttpServletRequest request, @PathVariable String nodeRequestId) {
        return taskDetail(request, nodeRequestId);
    }

    @GetMapping("/files")
    ResponseEntity<Map<String, Object>> listFiles(
            HttpServletRequest request,
            @RequestParam String rootAlias,
            @RequestParam String path
    ) {
        ResponseEntity<Map<String, Object>> auth = requireNodeAuth(request);
        if (auth != null) {
            return auth;
        }
        ResponseEntity<Map<String, Object>> pathError = validatePath(request, rootAlias, path);
        if (pathError != null) {
            return pathError;
        }
        List<Map<String, Object>> files = fileEntries().stream()
                .filter(file -> rootAlias.equals(file.get("rootAlias")))
                .filter(file -> fileInDirectory(text(file.get("path")), path))
                .toList();
        return ok(request, page(files, 1, 100));
    }

    @PostMapping("/files/read")
    ResponseEntity<Map<String, Object>> readFile(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        ResponseEntity<Map<String, Object>> auth = requireNodeAuth(request, body);
        if (auth != null) {
            return auth;
        }
        ResponseEntity<Map<String, Object>> trustedError = rejectTrustedFields(request, body);
        if (trustedError != null) {
            return trustedError;
        }
        if (testControlsEnabled && "true".equals(request.getHeader("X-Test-Fail-Audit"))) {
            return error(request, HttpStatus.INTERNAL_SERVER_ERROR, 55201, "local audit write failed");
        }
        String rootAlias = text(body.get("rootAlias"));
        String path = text(body.get("path"));
        ResponseEntity<Map<String, Object>> pathError = validatePath(request, rootAlias, path);
        if (pathError != null) {
            return pathError;
        }
        Map<String, Object> entry = fileEntries().stream()
                .filter(file -> rootAlias.equals(file.get("rootAlias")) && path.equals(file.get("path")))
                .findFirst()
                .orElse(null);
        if (entry == null) {
            return error(request, HttpStatus.NOT_FOUND, 49608, "file not found");
        }
        if (!Boolean.TRUE.equals(entry.get("editableText"))) {
            return error(request, HttpStatus.CONFLICT, 49609, "file not readable");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rootAlias", rootAlias);
        data.put("path", path);
        data.put("contentSummary", "text configuration summary with sensitive values redacted");
        data.put("truncated", false);
        data.put("sizeBytes", entry.get("sizeBytes"));
        data.put("hashSummary", "sha256:local-summary");
        data.put("redactionApplied", true);
        audit(request, "FILE_READ_SUMMARY", null, "SUCCESS");
        return ok(request, data);
    }

    @PostMapping("/logs/query")
    ResponseEntity<Map<String, Object>> queryLogs(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        ResponseEntity<Map<String, Object>> auth = requireNodeAuth(request, body);
        if (auth != null) {
            return auth;
        }
        ResponseEntity<Map<String, Object>> trustedError = rejectTrustedFields(request, body);
        if (trustedError != null) {
            return trustedError;
        }
        int tailLines = number(body.get("tailLines"));
        if (tailLines < 1 || tailLines > 1000) {
            return error(request, HttpStatus.BAD_REQUEST, 40001, "tailLines invalid");
        }
        String targetType = text(body.get("targetType"));
        String targetId = text(body.get("targetId"));
        if (!targetExists(targetType, targetId)) {
            return error(request, HttpStatus.NOT_FOUND, 49608, "target not found");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("targetType", targetType);
        data.put("targetId", targetId);
        data.put("tailLines", tailLines);
        data.put("logSummary", List.of("[INFO] player joined summary", "[INFO] runtime healthy summary"));
        data.put("redactionApplied", true);
        audit(request, "LOG_QUERY_SUMMARY", null, "SUCCESS");
        return ok(request, data);
    }

    @GetMapping("/audit-logs")
    ResponseEntity<Map<String, Object>> auditLogs(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String nodeRequestId,
            @RequestParam(required = false) String result
    ) {
        ResponseEntity<Map<String, Object>> auth = requireNodeAuth(request);
        if (auth != null) {
            return auth;
        }
        List<Map<String, Object>> items = audits.stream()
                .filter(audit -> nodeRequestId == null || nodeRequestId.equals(audit.get("nodeRequestId")))
                .filter(audit -> result == null || result.equals(audit.get("result")))
                .toList();
        return ok(request, page(items, page, pageSize));
    }

    private ResponseEntity<Map<String, Object>> requireNodeAuth(HttpServletRequest request) {
        return requireNodeAuth(request, Map.of());
    }

    private ResponseEntity<Map<String, Object>> requireNodeAuth(HttpServletRequest request, Map<String, Object> body) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            return error(request, HttpStatus.UNAUTHORIZED, 49600, "node auth missing");
        }
        if (!authorization.startsWith("Bearer ")) {
            return error(request, HttpStatus.UNAUTHORIZED, 41003, "bad token format");
        }
        if (!("Bearer " + nodeToken).equals(authorization)) {
            return error(request, HttpStatus.UNAUTHORIZED, 49601, "node auth invalid");
        }
        if (!nodeId.equals(request.getHeader("X-Node-Id"))) {
            return error(request, HttpStatus.FORBIDDEN, 49603, "node id mismatch");
        }
        String timestamp = request.getHeader("X-Node-Timestamp");
        String nodeRequestId = request.getHeader("X-Node-Request-Id");
        String signature = request.getHeader("X-Node-Signature");
        if (timestamp == null || timestamp.isBlank() || nodeRequestId == null || nodeRequestId.isBlank() || signature == null || signature.isBlank()) {
            return error(request, HttpStatus.UNAUTHORIZED, 49601, "signature invalid");
        }
        if (testControlsEnabled) {
            if (!VALID_SIGNATURE.equals(signature)) {
                return error(request, HttpStatus.UNAUTHORIZED, 49601, "signature invalid");
            }
            if ("2020-01-01T00:00:00Z".equals(timestamp)) {
                return error(request, HttpStatus.UNAUTHORIZED, 49602, "timestamp expired");
            }
            return null;
        }
        if (timestampOutsideWindow(timestamp)) {
            return error(request, HttpStatus.UNAUTHORIZED, 49602, "timestamp expired");
        }
        String signingText = signingText(request.getMethod(), request.getRequestURI(), body, timestamp, nodeRequestId);
        if (!hmac(signingText).equals(signature)) {
            return error(request, HttpStatus.UNAUTHORIZED, 49601, "signature invalid");
        }
        String previous = nodeRequestSignatures.putIfAbsent(nodeRequestId, signingText);
        if (previous != null && !previous.equals(signingText)) {
            return error(request, HttpStatus.CONFLICT, 49612, "node request replay conflict");
        }
        return null;
    }

    private boolean timestampOutsideWindow(String timestamp) {
        try {
            long delta = Math.abs(Duration.between(Instant.parse(timestamp), Instant.now()).getSeconds());
            return delta > allowedClockSkewSeconds;
        } catch (RuntimeException exception) {
            return true;
        }
    }

    private String signingText(String method, String path, Map<String, Object> body, String timestamp, String nodeRequestId) {
        return method.toUpperCase() + "\n" + path + "\n" + canonicalJson(body == null ? Map.of() : body) + "\n" + timestamp + "\n" + nodeRequestId;
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(nodeSigningSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte item : digest) {
                hex.append("%02x".formatted(item));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String canonicalJson(Object value) {
        try {
            return objectMapper.writeValueAsString(canonicalize(value));
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private ResponseEntity<Map<String, Object>> replayOrConflict(HttpServletRequest request, Map<String, Object> body, String key) {
        IdempotencyRecord record = idempotency.get(key);
        if (record == null) {
            return null;
        }
        String fingerprint = fingerprint(body);
        if (!record.fingerprint.equals(fingerprint)) {
            return error(request, HttpStatus.CONFLICT, 49612, "idempotency conflict");
        }
        return respond(request, record.status, 0, "success", record.data);
    }

    private void remember(Map<String, Object> body, String key, Map<String, Object> data, HttpStatus status) {
        idempotency.put(key, new IdempotencyRecord(fingerprint(body), data, status));
    }

    private String fingerprint(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(canonicalize(body));
        } catch (JsonProcessingException exception) {
            return body.toString();
        }
    }

    private Map<String, Object> snapshot(boolean degraded) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("nodeId", nodeId);
        data.put("status", degraded ? "DEGRADED" : "READY");
        data.put("version", VERSION);
        data.put("capabilities", List.of("NODE_READ", "NODE_WRITE", "FILE_MANAGE", "CONTAINER_OPERATE"));
        data.put("metrics", Map.of(
                "cpuUsagePercent", 22.5,
                "memoryUsagePercent", 44.0,
                "diskUsagePercent", 58.0,
                "networkRxBytes", 1024,
                "networkTxBytes", 2048,
                "loadAverage", List.of(0.2, 0.3, 0.4)
        ));
        data.put("containers", List.of(Map.of(
                "containerId", "container-seed-1",
                "nodeId", nodeId,
                "name", "beiming-runtime",
                "image", "beiming/runtime:simulated",
                "runtime", "SIMULATED",
                "status", "RUNNING",
                "ports", List.of(Map.of("containerPort", 25565, "protocol", "TCP")),
                "mounts", List.of(Map.of("alias", "mc-config")),
                "resourceUsage", Map.of("cpuUsagePercent", 12.0, "memoryMb", 512),
                "lastSyncedAt", NOW
        )));
        data.put("vms", List.of(Map.of(
                "vmId", "vm-build-1",
                "nodeId", nodeId,
                "name", "build-vm",
                "platform", "SIMULATED",
                "status", "STOPPED",
                "cpuCores", 2,
                "memoryMb", 2048,
                "diskGb", 32,
                "networkSummary", Map.of("mode", "masked"),
                "lastSyncedAt", NOW
        )));
        data.put("minecraftInstances", List.of(Map.of(
                "instanceId", "mc-survival",
                "nodeId", nodeId,
                "publicInstanceId", "public-survival",
                "name", "北冥生存服",
                "version", "1.20.4",
                "status", "RUNNING",
                "onlinePlayers", 3,
                "directoryAlias", "mc-config",
                "startCommandSummary", "java startup summary",
                "lastSyncedAt", NOW
        )));
        data.put("files", fileEntries());
        data.put("recentEvents", degraded ? List.of(Map.of("level", "WARN", "message", "runtime adapter unavailable")) : List.of());
        data.put("collectedAt", NOW);
        data.put("degraded", degraded);
        return data;
    }

    private List<Map<String, Object>> fileEntries() {
        return List.of(
                file("mc-config", "/runtime-config.txt", "runtime-config.txt", "FILE", 128, true),
                file("mc-config", "/binary.dat", "binary.dat", "FILE", 4096, false),
                file("mc-config", "/folder/item.txt", "item.txt", "FILE", 16, true),
                file("mc-config", "/folder-other/item.txt", "item.txt", "FILE", 16, true)
        );
    }

    private Map<String, Object> file(String rootAlias, String path, String name, String type, int size, boolean editable) {
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("rootAlias", rootAlias);
        file.put("path", path);
        file.put("name", name);
        file.put("type", type);
        file.put("sizeBytes", size);
        file.put("editableText", editable);
        file.put("modifiedAt", NOW);
        return file;
    }

    private ResponseEntity<Map<String, Object>> validatePath(HttpServletRequest request, String rootAlias, String path) {
        if (!"mc-config".equals(rootAlias)) {
            return error(request, HttpStatus.NOT_FOUND, 49608, "root not found");
        }
        String lowerPath = path == null ? "" : path.toLowerCase();
        if (path == null || !path.startsWith("/") || path.contains("..") || path.contains("\\") || lowerPath.contains("%2e") || lowerPath.contains("%5c") || hasControl(path)) {
            return error(request, HttpStatus.CONFLICT, 49605, "path escaped root");
        }
        return null;
    }

    private boolean hasControl(String value) {
        return value.chars().anyMatch(character -> character < 32);
    }

    private boolean fileInDirectory(String filePath, String requestedPath) {
        if ("/".equals(requestedPath)) {
            return filePath.chars().filter(ch -> ch == '/').count() == 1;
        }
        String normalized = requestedPath.endsWith("/") ? requestedPath.substring(0, requestedPath.length() - 1) : requestedPath;
        return filePath.startsWith(normalized + "/");
    }

    private boolean allowedTask(String taskType) {
        return List.of("CONTAINER_START", "CONTAINER_STOP", "CONTAINER_RESTART", "MC_START", "MC_STOP", "MC_RESTART", "FILE_READ", "LOG_QUERY").contains(taskType);
    }

    private boolean targetExists(String targetType, String targetId) {
        if ("CONTAINER".equals(targetType)) {
            return "container-seed-1".equals(targetId) || "container-seed-2".equals(targetId);
        }
        if ("MINECRAFT_INSTANCE".equals(targetType)) {
            return "mc-survival".equals(targetId);
        }
        if ("FILE".equals(targetType)) {
            return fileEntries().stream().anyMatch(file -> Objects.equals(file.get("path"), targetId));
        }
        if ("NODE".equals(targetType)) {
            return nodeId.equals(targetId);
        }
        return false;
    }

    private void audit(HttpServletRequest request, String action, String nodeRequestId, String result) {
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("id", "audit-" + UUID.randomUUID());
        audit.put("requestId", requestId(request));
        audit.put("nodeId", nodeId);
        audit.put("nodeRequestId", nodeRequestId);
        audit.put("localAction", action);
        audit.put("result", result);
        audit.put("createdAt", NOW);
        audits.add(audit);
    }

    private Map<String, Object> page(List<Map<String, Object>> items, int page, int pageSize) {
        int from = Math.min((page - 1) * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items.subList(from, to));
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("total", items.size());
        return data;
    }

    private int number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(text(value));
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private ResponseEntity<Map<String, Object>> rejectTrustedFields(HttpServletRequest request, Map<String, Object> body) {
        Set<String> trustedFields = Set.of(
                "trusted", "localRootPath", "resolvedPath", "tokenDigest", "credential",
                "beforeState", "afterState", "auditResult", "createdBy", "updatedBy", "finishedAt"
        );
        return containsAnyKey(body, trustedFields)
                ? error(request, HttpStatus.BAD_REQUEST, 40001, "trusted field is not accepted")
                : null;
    }

    private boolean containsAnyKey(Object value, Set<String> keys) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (keys.contains(String.valueOf(entry.getKey())) || containsAnyKey(entry.getValue(), keys)) {
                    return true;
                }
            }
        }
        if (value instanceof List<?> list) {
            return list.stream().anyMatch(item -> containsAnyKey(item, keys));
        }
        return false;
    }

    private Object canonicalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sorted.put(String.valueOf(entry.getKey()), canonicalize(entry.getValue()));
            }
            return sorted;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::canonicalize).toList();
        }
        return value;
    }

    private boolean expired(String expiresAt) {
        try {
            return !expiresAt.isBlank() && Instant.parse(expiresAt).isBefore(Instant.parse(NOW));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private ResponseEntity<Map<String, Object>> ok(HttpServletRequest request, Object data) {
        return respond(request, HttpStatus.OK, 0, "success", data);
    }

    private ResponseEntity<Map<String, Object>> created(HttpServletRequest request, Object data) {
        return respond(request, HttpStatus.CREATED, 0, "success", data);
    }

    private ResponseEntity<Map<String, Object>> error(HttpServletRequest request, HttpStatus status, int code, String message) {
        return respond(request, status, code, message, null);
    }

    private ResponseEntity<Map<String, Object>> respond(HttpServletRequest request, HttpStatus status, int code, String message, Object data) {
        String requestId = requestId(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", data);
        body.put("requestId", requestId);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", requestId);
        return new ResponseEntity<>(body, headers, status);
    }

    private String requestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }
        String nodeRequestId = request.getHeader("X-Node-Request-Id");
        if (nodeRequestId != null && !nodeRequestId.isBlank()) {
            return nodeRequestId;
        }
        return "req-" + UUID.randomUUID();
    }

    private record IdempotencyRecord(String fingerprint, Map<String, Object> data, HttpStatus status) {
    }
}
