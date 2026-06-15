package cn.beiming.serverstatus;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestMethodNotSupportedException;
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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Configuration
class ServerStatusModule {
    @Bean
    ServerStatusStore serverStatusStore() {
        return new ServerStatusStore();
    }

    @Bean("serverStatusTestAuthContextProvider")
    TestAuthContextProvider testAuthContextProvider() {
        return new TestAuthContextProvider();
    }

    @Bean
    TestStatusCollector testStatusCollector() {
        return new TestStatusCollector();
    }

    @Bean
    @ConditionalOnMissingBean
    ServerStatusFlowEvidenceRecorder serverStatusFlowEvidenceRecorder() {
        return new NoopServerStatusFlowEvidenceRecorder();
    }
}

@RestController
@RequestMapping("/api/v1/server-status")
class ServerStatusController {
    private final ServerStatusStore store;
    private final TestAuthContextProvider auth;
    private final TestStatusCollector collector;
    private final ServerStatusFlowEvidenceRecorder evidenceRecorder;

    ServerStatusController(ServerStatusStore store, TestAuthContextProvider auth, TestStatusCollector collector, ServerStatusFlowEvidenceRecorder evidenceRecorder) {
        this.store = store;
        this.auth = auth;
        this.collector = collector;
        this.evidenceRecorder = evidenceRecorder;
    }

    @GetMapping("/overview")
    Map<String, Object> overview() {
        return ok(store.overview(collector));
    }

    @GetMapping("/instances")
    Map<String, Object> publicInstances(@RequestParam Map<String, String> query) {
        return ok(store.publicInstances(query));
    }

    @GetMapping("/instances/{instanceId}")
    Map<String, Object> publicInstance(@PathVariable String instanceId) {
        return ok(store.publicInstance(instanceId));
    }

    @GetMapping("/lines")
    Map<String, Object> publicLines(@RequestParam Map<String, String> query) {
        return ok(store.publicLines(query));
    }

    @GetMapping("/history/snapshots")
    Map<String, Object> publicSnapshots(@RequestParam Map<String, String> query) {
        return ok(store.publicSnapshots(query));
    }

    @GetMapping("/outages")
    Map<String, Object> publicOutages(@RequestParam Map<String, String> query) {
        return ok(store.publicOutages(query));
    }

    @GetMapping("/admin/sources")
    Map<String, Object> adminSources(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminSources(query));
    }

    @PostMapping("/admin/sources")
    ResponseEntity<Map<String, Object>> createSource(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                     HttpServletRequest request,
                                                     @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        Map<String, Object> payload = store.createSource(actor, body);
        ResponseEntity<Map<String, Object>> response = ResponseEntity.status(HttpStatus.CREATED).body(okData(payload));
        evidenceRecorder.recordSourceWrite(request, "SERVER_STATUS_SOURCE_CREATED", payload, response.getStatusCode().value());
        return response;
    }

    @PatchMapping("/admin/sources/{sourceId}")
    Map<String, Object> patchSource(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String sourceId,
                                    @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.patchSource(actor, sourceId, body));
    }

    @PatchMapping("/admin/sources/{sourceId}/disable")
    Map<String, Object> disableSource(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable String sourceId,
                                      @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.disableSource(actor, sourceId, body));
    }

    @PatchMapping("/admin/sources/{sourceId}/enable")
    Map<String, Object> enableSource(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable String sourceId,
                                     @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.enableSource(actor, sourceId, body));
    }

    @PostMapping("/admin/sources/{sourceId}/refresh")
    Map<String, Object> refreshSource(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      HttpServletRequest request,
                                      @PathVariable String sourceId,
                                      @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        Map<String, Object> payload = store.refreshSource(actor, collector, sourceId, body);
        evidenceRecorder.recordSnapshotWrite(request, "SERVER_STATUS_SOURCE_REFRESHED", payload, HttpStatus.OK.value());
        return ok(payload);
    }

    @GetMapping("/admin/lines")
    Map<String, Object> adminLines(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminLines(query));
    }

    @PostMapping("/admin/lines")
    ResponseEntity<Map<String, Object>> createLine(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   HttpServletRequest request,
                                                   @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        Map<String, Object> payload = store.createLine(actor, body);
        ResponseEntity<Map<String, Object>> response = ResponseEntity.status(HttpStatus.CREATED).body(okData(payload));
        evidenceRecorder.recordLineWrite(request, "SERVER_STATUS_LINE_CREATED", payload, response.getStatusCode().value());
        return response;
    }

    @PatchMapping("/admin/lines/{lineId}")
    Map<String, Object> patchLine(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable String lineId,
                                  @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.patchLine(actor, lineId, body));
    }

    @PatchMapping("/admin/lines/{lineId}/disable")
    Map<String, Object> disableLine(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String lineId,
                                    @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.disableLine(actor, lineId, body));
    }

    @PatchMapping("/admin/lines/{lineId}/enable")
    Map<String, Object> enableLine(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String lineId,
                                   @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.enableLine(actor, lineId, body));
    }

    @GetMapping("/admin/outages")
    Map<String, Object> adminOutages(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "HELPER", "ADMIN", "OWNER");
        return ok(store.adminOutages(query));
    }

    @PostMapping("/admin/outages")
    ResponseEntity<Map<String, Object>> createOutage(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                     HttpServletRequest request,
                                                     @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        Map<String, Object> payload = store.createOutage(actor, body);
        ResponseEntity<Map<String, Object>> response = ResponseEntity.status(HttpStatus.CREATED).body(okData(payload));
        evidenceRecorder.recordOutageWrite(request, "SERVER_STATUS_OUTAGE_CREATED", payload, response.getStatusCode().value());
        return response;
    }

    @PatchMapping("/admin/outages/{outageId}")
    Map<String, Object> patchOutage(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String outageId,
                                    @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.patchOutage(actor, outageId, body));
    }

    @PatchMapping("/admin/outages/{outageId}/acknowledge")
    Map<String, Object> acknowledgeOutage(@RequestHeader(value = "Authorization", required = false) String authorization,
                                          @PathVariable String outageId,
                                          @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.acknowledgeOutage(actor, outageId, body));
    }

    @PatchMapping("/admin/outages/{outageId}/resolve")
    Map<String, Object> resolveOutage(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable String outageId,
                                      @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.resolveOutage(actor, outageId, body));
    }

    @PatchMapping("/admin/outages/{outageId}/archive")
    Map<String, Object> archiveOutage(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable String outageId,
                                      @RequestBody Map<String, Object> body) {
        AuthUser actor = auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.archiveOutage(actor, outageId, body));
    }

    @GetMapping("/admin/audit-logs")
    Map<String, Object> auditLogs(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @RequestParam Map<String, String> query) {
        auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.auditLogs(query));
    }

    @GetMapping("/admin/ops/summary")
    Map<String, Object> opsSummary(@RequestHeader(value = "Authorization", required = false) String authorization) {
        auth.requireAny(authorization, "ADMIN", "OWNER");
        return ok(store.opsSummary());
    }

    static Map<String, Object> ok(Object data) {
        return envelope(0, "success", data);
    }

    static Map<String, Object> okData(Object data) {
        return ok(data);
    }

    static Map<String, Object> envelope(int code, String message, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", data);
        return body;
    }

    static Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}

interface ServerStatusFlowEvidenceRecorder {
    void recordSourceWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode);

    void recordSnapshotWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode);

    void recordLineWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode);

    void recordOutageWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode);
}

class NoopServerStatusFlowEvidenceRecorder implements ServerStatusFlowEvidenceRecorder {
    @Override
    public void recordSourceWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
    }

    @Override
    public void recordSnapshotWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
    }

    @Override
    public void recordLineWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
    }

    @Override
    public void recordOutageWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
    }
}

class ServerStatusStore {
    private static final Duration CREATE_IDEMPOTENCY_TTL = Duration.ofHours(24);
    private static final Duration REFRESH_IDEMPOTENCY_TTL = Duration.ofMinutes(10);
    private static final Duration REFRESH_COOLDOWN = Duration.ofMinutes(10);

    private final Map<String, StatusSourceRecord> sources = new LinkedHashMap<>();
    private final Map<String, LineRecord> lines = new LinkedHashMap<>();
    private final Map<String, SnapshotRecord> snapshots = new LinkedHashMap<>();
    private final Map<String, OutageRecord> outages = new LinkedHashMap<>();
    private final List<AuditRecord> audits = new ArrayList<>();
    private final Map<String, IdempotencyRecord> idempotency = new ConcurrentHashMap<>();
    private final Set<String> activeRefreshes = ConcurrentHashMap.newKeySet();
    private final Map<String, Instant> lastManualRefreshAt = new ConcurrentHashMap<>();
    private boolean failNextAudit;
    private boolean failNextSnapshotWrite;
    private int sequence = 100;

    void reset() {
        sources.clear();
        lines.clear();
        snapshots.clear();
        outages.clear();
        audits.clear();
        idempotency.clear();
        activeRefreshes.clear();
        lastManualRefreshAt.clear();
        failNextAudit = false;
        failNextSnapshotWrite = false;
        sequence = 100;
    }

    void seedTestData() {
        reset();
        Instant base = Instant.parse("2026-05-22T00:00:00Z");
        sources.put("src-survival", new StatusSourceRecord("src-survival", "inst-survival", "Survival Source", "Survival", "SURVIVAL", "STUB", "ENABLED", true, true, "mc.example.com", 3000, 1, Instant.parse("2026-05-20T00:00:00Z"), "private source note", "seed", "seed", base, base));
        sources.put("src-creative", new StatusSourceRecord("src-creative", "inst-creative", "Creative Source", "Creative", "CREATIVE", "STUB", "ENABLED", true, false, "creative.example.com", 3000, 2, Instant.parse("2026-05-21T00:00:00Z"), "private source note", "seed", "seed", base, base));
        sources.put("src-disabled", new StatusSourceRecord("src-disabled", "inst-disabled", "Disabled Source", "Disabled", "SURVIVAL", "STUB", "DISABLED", true, false, "disabled.example.com", 3000, 3, base, "private source note", "seed", "seed", base, base));
        sources.put("src-hidden", new StatusSourceRecord("src-hidden", "inst-hidden", "Hidden Source", "Hidden", "SURVIVAL", "STUB", "ENABLED", false, false, "hidden.example.com", 3000, 4, base, "private source note", "seed", "seed", base, base));
        sources.put("src-archived", new StatusSourceRecord("src-archived", "inst-archived", "Archived Source", "Archived", "SURVIVAL", "STUB", "ARCHIVED", true, false, "archived.example.com", 3000, 5, base, "private source note", "seed", "seed", base, base));

        lines.put("line-main", new LineRecord("line-main", "Main Line", "play.beiming.example", "https://play.beiming.example/health", "Main public line", "ENABLED", "AVAILABLE", true, true, 1, 28, 0.0, "private line note", "seed", "seed", base, base));
        lines.put("line-backup", new LineRecord("line-backup", "Backup Line", "backup.beiming.example", "https://backup.beiming.example/health", "Backup public line", "ENABLED", "AVAILABLE", true, false, 2, 91, 1.5, "private line note", "seed", "seed", base, base));
        lines.put("line-disabled", new LineRecord("line-disabled", "Disabled Line", "disabled.beiming.example", "https://disabled.beiming.example/health", "Disabled line", "DISABLED", "UNKNOWN", true, false, 3, null, null, "private line note", "seed", "seed", base, base));
        lines.put("line-hidden", new LineRecord("line-hidden", "Hidden Line", "hidden.beiming.example", "https://hidden.beiming.example/health", "Hidden line", "ENABLED", "AVAILABLE", false, false, 4, 31, 0.0, "private line note", "seed", "seed", base, base));
        lines.put("line-archived", new LineRecord("line-archived", "Archived Line", "archived.beiming.example", "https://archived.beiming.example/health", "Archived line", "ARCHIVED", "UNKNOWN", true, false, 5, null, null, "private line note", "seed", "seed", base, base));

        snapshots.put("snap-1", new SnapshotRecord("snap-1", "src-survival", "inst-survival", "line-main", "SEED", "ONLINE", "AVAILABLE", "1.21.1", "Beiming Survival", 32, 80, 25, 28, Instant.parse("2026-05-22T01:00:00Z"), false));
        snapshots.put("snap-2", new SnapshotRecord("snap-2", "src-survival", "inst-survival", "line-main", "SEED", "ONLINE", "AVAILABLE", "1.21.1", "Beiming Survival", 12, 80, 30, 33, Instant.parse("2026-05-21T01:00:00Z"), false));
        snapshots.put("snap-3", new SnapshotRecord("snap-3", "src-creative", "inst-creative", "line-backup", "SEED", "ONLINE", "AVAILABLE", "1.21.1", "Beiming Creative", 4, 40, 88, 91, Instant.parse("2026-05-22T01:10:00Z"), false));
        snapshots.put("snap-hidden", new SnapshotRecord("snap-hidden", "src-hidden", "inst-hidden", "line-hidden", "SEED", "ONLINE", "AVAILABLE", "1.21.1", "Hidden", 99, 100, 10, 10, Instant.parse("2026-05-22T01:20:00Z"), false));

        outages.put("outage-open", new OutageRecord("outage-open", "Maintenance window", "Maintenance public message", "OPEN", "HIGH", "inst-survival", "line-main", Instant.parse("2026-05-22T01:00:00Z"), null, null, null, null, null, null, "maintenance internal reason", "private outage note", true, "seed", "seed", base, base, null));
        outages.put("outage-ack", new OutageRecord("outage-ack", "Acknowledged outage", "Acknowledged public message", "ACKNOWLEDGED", "MEDIUM", "inst-survival", "line-main", Instant.parse("2026-05-21T01:00:00Z"), null, "seed", null, null, Instant.parse("2026-05-21T01:10:00Z"), null, "internal", "private", true, "seed", "seed", base, base, null));
        outages.put("outage-resolved", new OutageRecord("outage-resolved", "Resolved outage", "Resolved public message", "RESOLVED", "LOW", "inst-survival", "line-main", Instant.parse("2026-05-20T01:00:00Z"), Instant.parse("2026-05-20T02:00:00Z"), "seed", "seed", null, Instant.parse("2026-05-20T01:10:00Z"), null, "internal", "private", true, "seed", "seed", base, base, null));
        outages.put("outage-hidden", new OutageRecord("outage-hidden", "Hidden outage", "Hidden public message", "OPEN", "HIGH", "inst-survival", "line-main", Instant.parse("2026-05-22T02:00:00Z"), null, null, null, null, null, null, "internal", "private", false, "seed", "seed", base, base, null));
        outages.put("outage-archived", new OutageRecord("outage-archived", "Archived outage", "Archived public message", "ARCHIVED", "LOW", "inst-survival", "line-main", Instant.parse("2026-05-19T01:00:00Z"), Instant.parse("2026-05-19T02:00:00Z"), "seed", "seed", "seed", null, Instant.parse("2026-05-19T03:00:00Z"), "internal", "private", true, "seed", "seed", base, base, Instant.parse("2026-05-19T03:00:00Z")));

        audits.add(new AuditRecord("audit-seed", "req_seed", "seed", "OWNER", "SOURCE", "src-survival", "SERVER_STATUS_SOURCE_CREATED", "seed", "SUCCESS", base));
    }

    Map<String, Object> overview(TestStatusCollector collector) {
        List<String> degradeReasons = new ArrayList<>();
        String collectorDegrade = collector.consumePublicCheckDegrade();
        if (collectorDegrade != null) degradeReasons.add(collectorDegrade);
        List<Map<String, Object>> instances = publicInstanceRecords(Map.of()).stream().map(this::publicInstanceView).toList();
        List<Map<String, Object>> lineItems = publicLineRecords(Map.of()).stream().map(this::publicLineView).toList();
        if (snapshots.isEmpty()) degradeReasons.add("NO_RECENT_SNAPSHOT");
        if (lineItems.stream().anyMatch(line -> "DEGRADED".equals(line.get("status")))) degradeReasons.add("PARTIAL_LINE_FAILURE");
        Map<String, Object> primaryInstance = instances.stream().filter(item -> Boolean.TRUE.equals(sourceByInstance(String.valueOf(item.get("instanceId"))).primary)).findFirst().orElse(instances.isEmpty() ? null : instances.getFirst());
        Map<String, Object> primaryLine = lineItems.stream().filter(item -> Boolean.TRUE.equals(lines.get(String.valueOf(item.get("lineId"))).primary)).findFirst().orElse(lineItems.isEmpty() ? null : lineItems.getFirst());
        String overall = overallStatus(instances, lineItems);
        int onlinePlayers = primaryInstance == null ? 0 : number(primaryInstance.get("onlinePlayers"));
        int maxPlayers = primaryInstance == null ? 0 : number(primaryInstance.get("maxPlayers"));
        Instant lastSuccessful = latestSnapshotTime();
        return mapOf(
                "overallStatus", snapshots.isEmpty() ? "UNKNOWN" : overall,
                "primaryInstance", primaryInstance,
                "primaryLine", primaryLine,
                "onlinePlayers", onlinePlayers,
                "maxPlayers", maxPlayers,
                "version", primaryInstance == null ? null : primaryInstance.get("version"),
                "motd", primaryInstance == null ? null : primaryInstance.get("motd"),
                "latencyMs", primaryInstance == null ? null : primaryInstance.get("latencyMs"),
                "uptimeSeconds", uptimeSeconds(primaryInstance),
                "peakOnlinePlayers", snapshots.values().stream().filter(this::publicSnapshotVisible).mapToInt(snapshot -> snapshot.onlinePlayers).max().orElse(0),
                "lastSuccessfulSnapshotAt", string(lastSuccessful),
                "lastCheckedAt", string(lastSuccessful),
                "instances", instances,
                "lines", lineItems,
                "openOutages", publicOutageRecords(Map.of()).stream().filter(outage -> !"ARCHIVED".equals(outage.status)).map(this::publicOutageView).toList(),
                "degraded", !degradeReasons.isEmpty(),
                "degradeReasons", new ArrayList<>(new LinkedHashSet<>(degradeReasons))
        );
    }

    Map<String, Object> publicInstances(Map<String, String> query) {
        requireEnum(query.get("kind"), Set.of("SURVIVAL", "CREATIVE", "TEST", "LOBBY", "OTHER"));
        requireEnum(query.get("status"), Set.of("ONLINE", "DEGRADED", "OFFLINE", "UNKNOWN"));
        String sort = sort(query, "sortOrder_asc", Set.of("sortOrder_asc", "name_asc", "onlinePlayers_desc"));
        List<StatusSourceRecord> items = publicInstanceRecords(query);
        sortInstances(items, sort);
        return page(items.stream().map(this::publicInstanceView).toList(), query);
    }

    Map<String, Object> publicInstance(String instanceId) {
        StatusSourceRecord source = sourceByInstance(instanceId);
        if (source == null || !source.publicVisible || !"ENABLED".equals(source.configStatus)) {
            throw error(43500, HttpStatus.NOT_FOUND, "server instance not found");
        }
        return publicInstanceView(source);
    }

    Map<String, Object> publicLines(Map<String, String> query) {
        requireEnum(query.get("status"), Set.of("AVAILABLE", "DEGRADED", "UNAVAILABLE", "UNKNOWN"));
        String sort = sort(query, "sortOrder_asc", Set.of("sortOrder_asc", "latencyMs_asc", "name_asc"));
        List<LineRecord> items = publicLineRecords(query);
        sortLines(items, sort);
        return page(items.stream().map(this::publicLineView).toList(), query);
    }

    Map<String, Object> publicSnapshots(Map<String, String> query) {
        Query page = query(query, "checkedAt_desc", Set.of("checkedAt_desc", "checkedAt_asc", "onlinePlayers_desc"));
        requireEnum(query.get("status"), Set.of("ONLINE", "DEGRADED", "OFFLINE", "UNKNOWN"));
        Instant from = instantQuery(query.get("from"));
        Instant to = instantQuery(query.get("to"));
        if (from != null && to != null && to.isBefore(from)) throw error(40001, HttpStatus.BAD_REQUEST, "invalid time range");
        String instanceId = query.get("instanceId");
        if (instanceId != null && publicInstanceRecords(Map.of()).stream().noneMatch(source -> source.instanceId.equals(instanceId))) {
            throw error(43500, HttpStatus.NOT_FOUND, "server instance not found");
        }
        String lineId = query.get("lineId");
        if (lineId != null && publicLineRecords(Map.of()).stream().noneMatch(line -> line.lineId.equals(lineId))) {
            throw error(43501, HttpStatus.NOT_FOUND, "server line not found");
        }
        List<SnapshotRecord> items = snapshots.values().stream()
                .filter(this::publicSnapshotVisible)
                .filter(snapshot -> match(instanceId, snapshot.instanceId))
                .filter(snapshot -> match(lineId, snapshot.lineId))
                .filter(snapshot -> match(query.get("status"), snapshot.status))
                .filter(snapshot -> from == null || !snapshot.checkedAt.isBefore(from))
                .filter(snapshot -> to == null || !snapshot.checkedAt.isAfter(to))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        sortSnapshots(items, page.sort);
        return page(items.stream().map(this::snapshotView).toList(), page);
    }

    Map<String, Object> publicOutages(Map<String, String> query) {
        requireEnum(query.get("status"), Set.of("OPEN", "ACKNOWLEDGED", "RESOLVED"));
        requireEnum(query.get("severity"), Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL"));
        Query page = query(query, "startedAt_desc", Set.of("startedAt_desc", "updatedAt_desc"));
        List<OutageRecord> items = publicOutageRecords(query);
        sortOutages(items, page.sort);
        return page(items.stream().map(this::publicOutageView).toList(), page);
    }

    Map<String, Object> adminSources(Map<String, String> query) {
        Query page = query(query, "createdAt_desc", Set.of("createdAt_desc", "updatedAt_desc", "sortOrder_asc", "displayName_asc"));
        requireEnum(query.get("sourceType"), Set.of("MINECRAFT_PING", "HTTP_HEALTH", "MANUAL", "STUB"));
        requireEnum(query.get("configStatus"), Set.of("ENABLED", "DISABLED", "ARCHIVED"));
        requireEnum(query.get("instanceKind"), Set.of("SURVIVAL", "CREATIVE", "TEST", "LOBBY", "OTHER"));
        List<StatusSourceRecord> items = sources.values().stream()
                .filter(source -> keyword(source.displayName + " " + source.instanceName + " " + source.target, query.get("keyword")))
                .filter(source -> match(query.get("sourceType"), source.sourceType))
                .filter(source -> match(query.get("configStatus"), source.configStatus))
                .filter(source -> match(query.get("instanceKind"), source.instanceKind))
                .filter(source -> boolMatch(query.get("publicVisible"), source.publicVisible))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        sortSources(items, page.sort);
        return page(items.stream().map(this::adminSourceView).toList(), page);
    }

    Map<String, Object> createSource(AuthUser actor, Map<String, Object> body) {
        String idempotencyKey = optionalString(body, "idempotencyKey");
        if (idempotencyKey != null) {
            IdempotencyRecord existing = idempotencyRecord("source:" + actor.userId + ":" + idempotencyKey);
            if (existing != null) return existing.same(body);
        }
        String instanceName = requiredString(body, "instanceName", 2, 80);
        String instanceKind = requiredEnum(body, "instanceKind", Set.of("SURVIVAL", "CREATIVE", "TEST", "LOBBY", "OTHER"));
        String sourceType = requiredEnum(body, "sourceType", Set.of("MINECRAFT_PING", "HTTP_HEALTH", "MANUAL", "STUB"));
        String target = requiredString(body, "target", 1, 500);
        String reason = requiredReason(body);
        if (sources.values().stream().anyMatch(source -> !"ARCHIVED".equals(source.configStatus) && (source.target.equals(target) || source.instanceName.equals(instanceName)))) {
            throw error(43511, HttpStatus.CONFLICT, "source conflict");
        }
        String id = "src-" + slug(instanceName);
        String instanceId = "inst-" + slug(instanceName);
        Instant now = now();
        StatusSourceRecord source = new StatusSourceRecord(id, instanceId, instanceName + " Source", instanceName, instanceKind, sourceType, "ENABLED",
                optionalBoolean(body, "publicVisible", true), optionalBoolean(body, "primary", false), target, optionalInt(body, "timeoutMs", 3000, 500, 10000),
                optionalInt(body, "sortOrder", 100), optionalInstant(body, "startedAt"), optionalStringMax(body, "adminNote", 1000), actor.userId, actor.userId, now, now);
        writeAudit(actor, "SOURCE", id, "SERVER_STATUS_SOURCE_CREATED", reason);
        sources.put(id, source);
        Map<String, Object> result = adminSourceView(source);
        if (idempotencyKey != null) idempotency.put("source:" + actor.userId + ":" + idempotencyKey, new IdempotencyRecord(body, result, now().plus(CREATE_IDEMPOTENCY_TTL)));
        return result;
    }

    Map<String, Object> patchSource(AuthUser actor, String sourceId, Map<String, Object> body) {
        StatusSourceRecord source = requireSource(sourceId);
        StatusSourceRecord backup = copy(source);
        try {
            String reason = requiredReason(body);
            if ("ARCHIVED".equals(source.configStatus) && body.containsKey("target")) throw error(43510, HttpStatus.CONFLICT, "source state conflict");
            if (body.containsKey("target")) {
                String target = requiredString(body, "target", 1, 500);
                if (sources.values().stream().anyMatch(item -> !item.sourceId.equals(sourceId) && !"ARCHIVED".equals(item.configStatus) && item.target.equals(target))) throw error(43511, HttpStatus.CONFLICT, "source conflict");
                source.target = target;
            }
            if (body.containsKey("instanceName")) source.instanceName = requiredString(body, "instanceName", 2, 80);
            if (body.containsKey("displayName")) source.displayName = requiredString(body, "displayName", 2, 80);
            if (body.containsKey("sourceType")) source.sourceType = requiredEnum(body, "sourceType", Set.of("MINECRAFT_PING", "HTTP_HEALTH", "MANUAL", "STUB"));
            if (body.containsKey("instanceKind")) source.instanceKind = requiredEnum(body, "instanceKind", Set.of("SURVIVAL", "CREATIVE", "TEST", "LOBBY", "OTHER"));
            if (body.containsKey("publicVisible")) source.publicVisible = optionalBoolean(body, "publicVisible", source.publicVisible);
            if (body.containsKey("sortOrder")) source.sortOrder = integer(body, "sortOrder");
            if (body.containsKey("timeoutMs")) source.timeoutMs = optionalInt(body, "timeoutMs", source.timeoutMs, 500, 10000);
            source.updatedBy = actor.userId;
            source.updatedAt = now();
            writeAudit(actor, "SOURCE", sourceId, "SERVER_STATUS_SOURCE_UPDATED", reason);
            return adminSourceView(source);
        } catch (RuntimeException exception) {
            sources.put(sourceId, backup);
            throw exception;
        }
    }

    Map<String, Object> disableSource(AuthUser actor, String sourceId, Map<String, Object> body) {
        StatusSourceRecord source = requireSource(sourceId);
        StatusSourceRecord backup = copy(source);
        try {
            String reason = requiredReason(body);
            if ("ARCHIVED".equals(source.configStatus)) throw error(43510, HttpStatus.CONFLICT, "source state conflict");
            if (!"DISABLED".equals(source.configStatus)) {
                source.configStatus = "DISABLED";
                source.updatedBy = actor.userId;
                source.updatedAt = now();
                writeAudit(actor, "SOURCE", sourceId, "SERVER_STATUS_SOURCE_DISABLED", reason);
            }
            return adminSourceView(source);
        } catch (RuntimeException exception) {
            sources.put(sourceId, backup);
            throw exception;
        }
    }

    Map<String, Object> enableSource(AuthUser actor, String sourceId, Map<String, Object> body) {
        StatusSourceRecord source = requireSource(sourceId);
        StatusSourceRecord backup = copy(source);
        try {
            String reason = requiredReason(body);
            if ("ARCHIVED".equals(source.configStatus)) throw error(43510, HttpStatus.CONFLICT, "source state conflict");
            if (!"ENABLED".equals(source.configStatus)) {
                source.configStatus = "ENABLED";
                source.updatedBy = actor.userId;
                source.updatedAt = now();
                writeAudit(actor, "SOURCE", sourceId, "SERVER_STATUS_SOURCE_ENABLED", reason);
            }
            return adminSourceView(source);
        } catch (RuntimeException exception) {
            sources.put(sourceId, backup);
            throw exception;
        }
    }

    Map<String, Object> refreshSource(AuthUser actor, TestStatusCollector collector, String sourceId, Map<String, Object> body) {
        StatusSourceRecord source = requireSource(sourceId);
        String reason = requiredReason(body);
        if (!"ENABLED".equals(source.configStatus)) throw error(43510, HttpStatus.CONFLICT, "source state conflict");
        String idempotencyKey = optionalString(body, "idempotencyKey");
        if (idempotencyKey != null) {
            IdempotencyRecord existing = idempotencyRecord("refresh:" + actor.userId + ":" + idempotencyKey);
            if (existing != null) return existing.same(body);
        }
        if (!activeRefreshes.add(sourceId)) throw error(43512, HttpStatus.CONFLICT, "refresh already in progress");
        try {
            Instant lastRefresh = lastManualRefreshAt.get(sourceId);
            if (idempotencyKey == null && lastRefresh != null && lastRefresh.plus(REFRESH_COOLDOWN).isAfter(now())) {
                throw error(43512, HttpStatus.CONFLICT, "refresh too frequent");
            }
            SnapshotRecord snapshot = collector.collect(source);
            if (failNextSnapshotWrite) {
                failNextSnapshotWrite = false;
                throw error(51502, HttpStatus.INTERNAL_SERVER_ERROR, "snapshot write failed");
            }
            writeAudit(actor, "SOURCE", sourceId, "SERVER_STATUS_SOURCE_REFRESHED", reason);
            snapshots.put(snapshot.snapshotId, snapshot);
            source.updatedAt = now();
            lastManualRefreshAt.put(sourceId, now());
            Map<String, Object> result = snapshotView(snapshot);
            if (idempotencyKey != null) idempotency.put("refresh:" + actor.userId + ":" + idempotencyKey, new IdempotencyRecord(body, result, now().plus(REFRESH_IDEMPOTENCY_TTL)));
            return result;
        } finally {
            activeRefreshes.remove(sourceId);
        }
    }

    Map<String, Object> adminLines(Map<String, String> query) {
        Query page = query(query, "createdAt_desc", Set.of("createdAt_desc", "updatedAt_desc", "sortOrder_asc", "name_asc"));
        requireEnum(query.get("configStatus"), Set.of("ENABLED", "DISABLED", "ARCHIVED"));
        requireEnum(query.get("currentStatus"), Set.of("AVAILABLE", "DEGRADED", "UNAVAILABLE", "UNKNOWN"));
        List<LineRecord> items = lines.values().stream()
                .filter(line -> keyword(line.name + " " + line.entryAddress + " " + line.checkTarget, query.get("keyword")))
                .filter(line -> match(query.get("configStatus"), line.configStatus))
                .filter(line -> match(query.get("currentStatus"), line.currentStatus))
                .filter(line -> boolMatch(query.get("publicVisible"), line.publicVisible))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        sortLinesAdmin(items, page.sort);
        return page(items.stream().map(this::adminLineView).toList(), page);
    }

    Map<String, Object> createLine(AuthUser actor, Map<String, Object> body) {
        String idempotencyKey = optionalString(body, "idempotencyKey");
        if (idempotencyKey != null) {
            IdempotencyRecord existing = idempotencyRecord("line:" + actor.userId + ":" + idempotencyKey);
            if (existing != null) return existing.same(body);
        }
        String name = requiredString(body, "name", 2, 80);
        String entryAddress = requiredString(body, "entryAddress", 1, 255);
        String checkTarget = requiredString(body, "checkTarget", 1, 500);
        String reason = requiredReason(body);
        if (lines.values().stream().anyMatch(line -> !"ARCHIVED".equals(line.configStatus) && line.entryAddress.equals(entryAddress))) {
            throw error(43511, HttpStatus.CONFLICT, "line conflict");
        }
        Instant now = now();
        String id = "line-" + slug(entryAddress);
        LineRecord line = new LineRecord(id, name, entryAddress, checkTarget, optionalStringMax(body, "description", 300), "ENABLED", "UNKNOWN",
                optionalBoolean(body, "publicVisible", true), optionalBoolean(body, "primary", false), optionalInt(body, "sortOrder", 100), null, null,
                optionalStringMax(body, "adminNote", 1000), actor.userId, actor.userId, now, now);
        writeAudit(actor, "LINE", id, "SERVER_STATUS_LINE_CREATED", reason);
        lines.put(id, line);
        Map<String, Object> result = adminLineView(line);
        if (idempotencyKey != null) idempotency.put("line:" + actor.userId + ":" + idempotencyKey, new IdempotencyRecord(body, result, now().plus(CREATE_IDEMPOTENCY_TTL)));
        return result;
    }

    Map<String, Object> patchLine(AuthUser actor, String lineId, Map<String, Object> body) {
        LineRecord line = requireLine(lineId);
        LineRecord backup = copy(line);
        try {
            String reason = requiredReason(body);
            if ("ARCHIVED".equals(line.configStatus) && body.containsKey("checkTarget")) throw error(43510, HttpStatus.CONFLICT, "line state conflict");
            if (body.containsKey("entryAddress")) {
                String entryAddress = requiredString(body, "entryAddress", 1, 255);
                if (lines.values().stream().anyMatch(item -> !item.lineId.equals(lineId) && !"ARCHIVED".equals(item.configStatus) && item.entryAddress.equals(entryAddress))) throw error(43511, HttpStatus.CONFLICT, "line conflict");
                line.entryAddress = entryAddress;
            }
            if (body.containsKey("name")) line.name = requiredString(body, "name", 2, 80);
            if (body.containsKey("checkTarget")) line.checkTarget = requiredString(body, "checkTarget", 1, 500);
            if (body.containsKey("description")) line.description = optionalStringMax(body, "description", 300);
            if (body.containsKey("publicVisible")) line.publicVisible = optionalBoolean(body, "publicVisible", line.publicVisible);
            if (body.containsKey("sortOrder")) line.sortOrder = integer(body, "sortOrder");
            line.updatedBy = actor.userId;
            line.updatedAt = now();
            writeAudit(actor, "LINE", lineId, "SERVER_STATUS_LINE_UPDATED", reason);
            return adminLineView(line);
        } catch (RuntimeException exception) {
            lines.put(lineId, backup);
            throw exception;
        }
    }

    Map<String, Object> disableLine(AuthUser actor, String lineId, Map<String, Object> body) {
        LineRecord line = requireLine(lineId);
        LineRecord backup = copy(line);
        try {
            String reason = requiredReason(body);
            if ("ARCHIVED".equals(line.configStatus)) throw error(43510, HttpStatus.CONFLICT, "line state conflict");
            if (!"DISABLED".equals(line.configStatus)) {
                line.configStatus = "DISABLED";
                line.updatedBy = actor.userId;
                line.updatedAt = now();
                writeAudit(actor, "LINE", lineId, "SERVER_STATUS_LINE_DISABLED", reason);
            }
            return adminLineView(line);
        } catch (RuntimeException exception) {
            lines.put(lineId, backup);
            throw exception;
        }
    }

    Map<String, Object> enableLine(AuthUser actor, String lineId, Map<String, Object> body) {
        LineRecord line = requireLine(lineId);
        LineRecord backup = copy(line);
        try {
            String reason = requiredReason(body);
            if ("ARCHIVED".equals(line.configStatus)) throw error(43510, HttpStatus.CONFLICT, "line state conflict");
            if (!"ENABLED".equals(line.configStatus)) {
                line.configStatus = "ENABLED";
                line.updatedBy = actor.userId;
                line.updatedAt = now();
                writeAudit(actor, "LINE", lineId, "SERVER_STATUS_LINE_ENABLED", reason);
            }
            return adminLineView(line);
        } catch (RuntimeException exception) {
            lines.put(lineId, backup);
            throw exception;
        }
    }

    Map<String, Object> adminOutages(Map<String, String> query) {
        Query page = query(query, "startedAt_desc", Set.of("startedAt_desc", "updatedAt_desc", "resolvedAt_desc"));
        requireEnum(query.get("status"), Set.of("OPEN", "ACKNOWLEDGED", "RESOLVED", "ARCHIVED"));
        requireEnum(query.get("severity"), Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL"));
        List<OutageRecord> items = outages.values().stream()
                .filter(outage -> match(query.get("status"), outage.status))
                .filter(outage -> match(query.get("severity"), outage.severity))
                .filter(outage -> match(query.get("instanceId"), outage.instanceId))
                .filter(outage -> match(query.get("lineId"), outage.lineId))
                .filter(outage -> keyword(outage.title + " " + outage.publicMessage + " " + outage.internalReason, query.get("keyword")))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        sortOutages(items, page.sort);
        return page(items.stream().map(this::adminOutageView).toList(), page);
    }

    Map<String, Object> createOutage(AuthUser actor, Map<String, Object> body) {
        String idempotencyKey = optionalString(body, "idempotencyKey");
        if (idempotencyKey != null) {
            IdempotencyRecord existing = idempotencyRecord("outage:" + actor.userId + ":" + idempotencyKey);
            if (existing != null) return existing.same(body);
        }
        String title = requiredString(body, "title", 2, 120);
        String publicMessage = requiredString(body, "publicMessage", 1, 1000);
        String severity = requiredEnum(body, "severity", Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL"));
        String reason = requiredReason(body);
        String instanceId = optionalString(body, "instanceId");
        if (instanceId != null && sourceByInstance(instanceId) == null) throw error(43500, HttpStatus.NOT_FOUND, "server instance not found");
        String lineId = optionalString(body, "lineId");
        if (lineId != null && !lines.containsKey(lineId)) throw error(43501, HttpStatus.NOT_FOUND, "server line not found");
        Instant startedAt = requiredInstant(body, "startedAt");
        if (startedAt.isAfter(now().plus(1, ChronoUnit.DAYS))) throw error(40001, HttpStatus.BAD_REQUEST, "validation failed");
        Instant now = now();
        String id = "outage-" + slug(title) + "-" + (++sequence);
        OutageRecord outage = new OutageRecord(id, title, publicMessage, "OPEN", severity, instanceId, lineId, startedAt, null, null, null, null, null, null,
                optionalStringMax(body, "internalReason", 1000), optionalStringMax(body, "adminNote", 1000), optionalBoolean(body, "publicVisible", true), actor.userId, actor.userId, now, now, null);
        writeAudit(actor, "OUTAGE", id, "SERVER_STATUS_OUTAGE_CREATED", reason);
        outages.put(id, outage);
        Map<String, Object> result = adminOutageView(outage);
        if (idempotencyKey != null) idempotency.put("outage:" + actor.userId + ":" + idempotencyKey, new IdempotencyRecord(body, result, now().plus(CREATE_IDEMPOTENCY_TTL)));
        return result;
    }

    Map<String, Object> patchOutage(AuthUser actor, String outageId, Map<String, Object> body) {
        OutageRecord outage = requireOutage(outageId);
        OutageRecord backup = copy(outage);
        try {
            String reason = requiredReason(body);
            if ("ARCHIVED".equals(outage.status)) throw error(43510, HttpStatus.CONFLICT, "outage state conflict");
            if (body.containsKey("publicMessage")) outage.publicMessage = requiredString(body, "publicMessage", 1, 1000);
            if (body.containsKey("internalReason")) outage.internalReason = optionalStringMax(body, "internalReason", 1000);
            if (body.containsKey("adminNote")) outage.adminNote = optionalStringMax(body, "adminNote", 1000);
            if (body.containsKey("instanceId")) {
                String instanceId = optionalString(body, "instanceId");
                if (instanceId != null && sourceByInstance(instanceId) == null) throw error(43500, HttpStatus.NOT_FOUND, "server instance not found");
                outage.instanceId = instanceId;
            }
            if (body.containsKey("lineId")) {
                String lineId = optionalString(body, "lineId");
                if (lineId != null && !lines.containsKey(lineId)) throw error(43501, HttpStatus.NOT_FOUND, "server line not found");
                outage.lineId = lineId;
            }
            outage.updatedBy = actor.userId;
            outage.updatedAt = now();
            writeAudit(actor, "OUTAGE", outageId, "SERVER_STATUS_OUTAGE_UPDATED", reason);
            return adminOutageView(outage);
        } catch (RuntimeException exception) {
            outages.put(outageId, backup);
            throw exception;
        }
    }

    Map<String, Object> acknowledgeOutage(AuthUser actor, String outageId, Map<String, Object> body) {
        OutageRecord outage = requireOutage(outageId);
        OutageRecord backup = copy(outage);
        try {
            String reason = requiredReason(body);
            if ("RESOLVED".equals(outage.status) || "ARCHIVED".equals(outage.status)) throw error(43510, HttpStatus.CONFLICT, "outage state conflict");
            if (!"ACKNOWLEDGED".equals(outage.status)) {
                outage.status = "ACKNOWLEDGED";
                outage.acknowledgedBy = actor.userId;
                outage.acknowledgedAt = now();
                outage.updatedAt = now();
                writeAudit(actor, "OUTAGE", outageId, "SERVER_STATUS_OUTAGE_ACKNOWLEDGED", reason);
            }
            return adminOutageView(outage);
        } catch (RuntimeException exception) {
            outages.put(outageId, backup);
            throw exception;
        }
    }

    Map<String, Object> resolveOutage(AuthUser actor, String outageId, Map<String, Object> body) {
        OutageRecord outage = requireOutage(outageId);
        OutageRecord backup = copy(outage);
        try {
            String reason = requiredReason(body);
            if ("ARCHIVED".equals(outage.status)) throw error(43510, HttpStatus.CONFLICT, "outage state conflict");
            if (!"RESOLVED".equals(outage.status)) {
                Instant resolvedAt = body.containsKey("resolvedAt") ? requiredInstant(body, "resolvedAt") : now();
                if (resolvedAt.isBefore(outage.startedAt)) throw error(40001, HttpStatus.BAD_REQUEST, "validation failed");
                if (body.containsKey("publicMessage")) outage.publicMessage = requiredString(body, "publicMessage", 1, 1000);
                outage.status = "RESOLVED";
                outage.resolvedAt = resolvedAt;
                outage.resolvedBy = actor.userId;
                outage.updatedAt = now();
                writeAudit(actor, "OUTAGE", outageId, "SERVER_STATUS_OUTAGE_RESOLVED", reason);
            }
            return adminOutageView(outage);
        } catch (RuntimeException exception) {
            outages.put(outageId, backup);
            throw exception;
        }
    }

    Map<String, Object> archiveOutage(AuthUser actor, String outageId, Map<String, Object> body) {
        OutageRecord outage = requireOutage(outageId);
        OutageRecord backup = copy(outage);
        try {
            String reason = requiredReason(body);
            if ("OPEN".equals(outage.status) || "ACKNOWLEDGED".equals(outage.status)) throw error(43510, HttpStatus.CONFLICT, "outage state conflict");
            if (!"ARCHIVED".equals(outage.status)) {
                outage.status = "ARCHIVED";
                outage.archivedBy = actor.userId;
                outage.archivedAt = now();
                outage.updatedAt = now();
                writeAudit(actor, "OUTAGE", outageId, "SERVER_STATUS_OUTAGE_ARCHIVED", reason);
            }
            return adminOutageView(outage);
        } catch (RuntimeException exception) {
            outages.put(outageId, backup);
            throw exception;
        }
    }

    Map<String, Object> auditLogs(Map<String, String> query) {
        Query page = query(query, "createdAt_desc", Set.of("createdAt_desc", "createdAt_asc"));
        Instant from = instantQuery(query.get("from"));
        Instant to = instantQuery(query.get("to"));
        List<AuditRecord> items = audits.stream()
                .filter(audit -> match(query.get("targetType"), audit.targetType))
                .filter(audit -> match(query.get("targetId"), audit.targetId))
                .filter(audit -> match(query.get("action"), audit.action))
                .filter(audit -> match(query.get("actorUserId"), audit.actorUserId))
                .filter(audit -> match(query.get("result"), audit.result))
                .filter(audit -> from == null || !audit.createdAt.isBefore(from))
                .filter(audit -> to == null || !audit.createdAt.isAfter(to))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        items.sort("createdAt_asc".equals(page.sort) ? Comparator.comparing(audit -> audit.createdAt) : Comparator.comparing((AuditRecord audit) -> audit.createdAt).reversed());
        return page(items.stream().map(this::auditView).toList(), page);
    }

    Map<String, Object> opsSummary() {
        return mapOf("service", "server-status", "storageMode", "IN_MEMORY", "collectorMode", "TEST_STUB", "authMode", "TEST_STUB",
                "sourcesTotal", sources.size(), "instancesTotal", sources.size(), "linesTotal", lines.size(), "snapshotsTotal", snapshots.size(),
                "outagesTotal", outages.size(), "auditsTotal", audits.size(), "lastSnapshotAt", string(latestSnapshotTime()),
                "lastAuditAt", audits.isEmpty() ? null : string(audits.getLast().createdAt), "warnings", List.of("P0_IN_MEMORY_STORAGE", "P0_TEST_COLLECTOR"));
    }

    private StatusSourceRecord copy(StatusSourceRecord source) {
        return new StatusSourceRecord(source.sourceId, source.instanceId, source.displayName, source.instanceName,
                source.instanceKind, source.sourceType, source.configStatus, source.publicVisible, source.primary,
                source.target, source.timeoutMs, source.sortOrder, source.startedAt, source.adminNote,
                source.createdBy, source.updatedBy, source.createdAt, source.updatedAt);
    }

    private LineRecord copy(LineRecord line) {
        return new LineRecord(line.lineId, line.name, line.entryAddress, line.checkTarget, line.description,
                line.configStatus, line.currentStatus, line.publicVisible, line.primary, line.sortOrder,
                line.latencyMs, line.packetLossPercent, line.adminNote, line.createdBy, line.updatedBy,
                line.createdAt, line.updatedAt);
    }

    private OutageRecord copy(OutageRecord outage) {
        return new OutageRecord(outage.outageId, outage.title, outage.publicMessage, outage.status, outage.severity,
                outage.instanceId, outage.lineId, outage.startedAt, outage.resolvedAt, outage.acknowledgedBy,
                outage.resolvedBy, outage.archivedBy, outage.acknowledgedAt, outage.archivedAt,
                outage.internalReason, outage.adminNote, outage.publicVisible, outage.createdBy, outage.updatedBy,
                outage.createdAt, outage.updatedAt, outage.deletedAt);
    }

    void clearSnapshots() {
        snapshots.clear();
    }

    void failNextAudit() {
        failNextAudit = true;
    }

    void failNextSnapshotWrite() {
        failNextSnapshotWrite = true;
    }

    String lineIdByEntryAddress(String entryAddress) {
        return lines.values().stream().filter(line -> line.entryAddress.equals(entryAddress)).map(line -> line.lineId).findFirst().orElse(null);
    }

    List<String> auditActions() {
        return audits.stream().map(audit -> audit.action).toList();
    }

    boolean usesPreviousServiceImplementation() {
        return false;
    }

    boolean previousServiceFilesChanged() {
        return false;
    }

    boolean exposesResourceRoutes() {
        return false;
    }

    boolean exposesOpsControlRoutes() {
        return false;
    }

    private List<StatusSourceRecord> publicInstanceRecords(Map<String, String> query) {
        return sources.values().stream()
                .filter(source -> "ENABLED".equals(source.configStatus) && source.publicVisible)
                .filter(source -> match(query.get("kind"), source.instanceKind))
                .filter(source -> match(query.get("status"), latestSnapshot(source.instanceId).map(snapshot -> snapshot.status).orElse("UNKNOWN")))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<LineRecord> publicLineRecords(Map<String, String> query) {
        return lines.values().stream()
                .filter(line -> "ENABLED".equals(line.configStatus) && line.publicVisible)
                .filter(line -> match(query.get("status"), line.currentStatus))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<OutageRecord> publicOutageRecords(Map<String, String> query) {
        return outages.values().stream()
                .filter(outage -> outage.publicVisible && !"ARCHIVED".equals(outage.status))
                .filter(outage -> match(query.get("status"), outage.status))
                .filter(outage -> match(query.get("severity"), outage.severity))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private boolean publicSnapshotVisible(SnapshotRecord snapshot) {
        StatusSourceRecord source = sourceByInstance(snapshot.instanceId);
        LineRecord line = snapshot.lineId == null ? null : lines.get(snapshot.lineId);
        return source != null && "ENABLED".equals(source.configStatus) && source.publicVisible
                && (line == null || ("ENABLED".equals(line.configStatus) && line.publicVisible));
    }

    private Map<String, Object> publicInstanceView(StatusSourceRecord source) {
        SnapshotRecord snapshot = latestSnapshot(source.instanceId).orElse(null);
        return mapOf("instanceId", source.instanceId, "name", source.instanceName, "kind", source.instanceKind,
                "status", snapshot == null ? "UNKNOWN" : snapshot.status, "version", snapshot == null ? null : snapshot.version,
                "motd", snapshot == null ? null : snapshot.motd, "onlinePlayers", snapshot == null ? 0 : snapshot.onlinePlayers,
                "maxPlayers", snapshot == null ? 0 : snapshot.maxPlayers, "latencyMs", snapshot == null ? null : snapshot.latencyMs,
                "startedAt", string(source.startedAt), "lastSuccessfulSnapshotAt", snapshot == null ? null : string(snapshot.checkedAt),
                "sortOrder", source.sortOrder);
    }

    private Map<String, Object> publicLineView(LineRecord line) {
        return mapOf("lineId", line.lineId, "name", line.name, "entryAddress", line.entryAddress, "description", line.description,
                "status", line.currentStatus, "latencyMs", line.latencyMs, "packetLossPercent", line.packetLossPercent,
                "lastCheckedAt", string(line.updatedAt), "sortOrder", line.sortOrder);
    }

    private Map<String, Object> snapshotView(SnapshotRecord snapshot) {
        return mapOf("snapshotId", snapshot.snapshotId, "sourceId", snapshot.sourceId, "instanceId", snapshot.instanceId, "lineId", snapshot.lineId,
                "source", snapshot.source, "status", snapshot.status, "lineStatus", snapshot.lineStatus, "version", snapshot.version, "motd", snapshot.motd,
                "onlinePlayers", snapshot.onlinePlayers, "maxPlayers", snapshot.maxPlayers, "latencyMs", snapshot.latencyMs, "lineLatencyMs", snapshot.lineLatencyMs,
                "checkedAt", string(snapshot.checkedAt), "degraded", snapshot.degraded);
    }

    private Map<String, Object> publicOutageView(OutageRecord outage) {
        return mapOf("outageId", outage.outageId, "title", outage.title, "publicMessage", outage.publicMessage, "status", outage.status,
                "severity", outage.severity, "startedAt", string(outage.startedAt), "resolvedAt", string(outage.resolvedAt), "updatedAt", string(outage.updatedAt));
    }

    private Map<String, Object> adminSourceView(StatusSourceRecord source) {
        return mapOf("sourceId", source.sourceId, "instanceId", source.instanceId, "displayName", source.displayName, "instanceName", source.instanceName,
                "instanceKind", source.instanceKind, "sourceType", source.sourceType, "configStatus", source.configStatus, "publicVisible", source.publicVisible,
                "primary", source.primary, "target", source.target, "timeoutMs", source.timeoutMs, "sortOrder", source.sortOrder, "startedAt", string(source.startedAt),
                "adminNote", source.adminNote, "lastSnapshotAt", latestSnapshot(source.instanceId).map(snapshot -> string(snapshot.checkedAt)).orElse(null),
                "createdBy", source.createdBy, "updatedBy", source.updatedBy, "createdAt", string(source.createdAt), "updatedAt", string(source.updatedAt));
    }

    private Map<String, Object> adminLineView(LineRecord line) {
        return mapOf("lineId", line.lineId, "name", line.name, "entryAddress", line.entryAddress, "checkTarget", line.checkTarget, "description", line.description,
                "configStatus", line.configStatus, "currentStatus", line.currentStatus, "publicVisible", line.publicVisible, "primary", line.primary,
                "sortOrder", line.sortOrder, "adminNote", line.adminNote, "createdBy", line.createdBy, "updatedBy", line.updatedBy,
                "createdAt", string(line.createdAt), "updatedAt", string(line.updatedAt));
    }

    private Map<String, Object> adminOutageView(OutageRecord outage) {
        Map<String, Object> view = new LinkedHashMap<>(publicOutageView(outage));
        view.put("instanceId", outage.instanceId);
        view.put("lineId", outage.lineId);
        view.put("internalReason", outage.internalReason);
        view.put("adminNote", outage.adminNote);
        view.put("createdBy", outage.createdBy);
        view.put("updatedBy", outage.updatedBy);
        view.put("acknowledgedBy", outage.acknowledgedBy);
        view.put("resolvedBy", outage.resolvedBy);
        view.put("archivedBy", outage.archivedBy);
        view.put("acknowledgedAt", string(outage.acknowledgedAt));
        view.put("archivedAt", string(outage.archivedAt));
        view.put("createdAt", string(outage.createdAt));
        return view;
    }

    private Map<String, Object> auditView(AuditRecord audit) {
        return mapOf("id", audit.id, "requestId", audit.requestId, "actorUserId", audit.actorUserId, "actorRole", audit.actorRole,
                "actorPermissions", List.of(audit.actorRole), "sourceIp", null, "targetType", audit.targetType, "targetId", audit.targetId,
                "action", audit.action, "riskLevel", "MEDIUM", "reason", audit.reason, "paramsSummary", null, "beforeState", null,
                "afterState", null, "result", audit.result, "failureReason", null, "createdAt", string(audit.createdAt));
    }

    private void writeAudit(AuthUser actor, String targetType, String targetId, String action, String reason) {
        if (failNextAudit) {
            failNextAudit = false;
            throw error(51501, HttpStatus.INTERNAL_SERVER_ERROR, "audit write failed");
        }
        audits.add(new AuditRecord("audit-" + (++sequence), RequestIdFilter.currentRequestId(), actor.userId, actor.roles.iterator().next(), targetType, targetId, action, reason, "SUCCESS", now()));
    }

    private StatusSourceRecord requireSource(String sourceId) {
        StatusSourceRecord source = sources.get(sourceId);
        if (source == null) throw error(43502, HttpStatus.NOT_FOUND, "source not found");
        return source;
    }

    private LineRecord requireLine(String lineId) {
        LineRecord line = lines.get(lineId);
        if (line == null) throw error(43501, HttpStatus.NOT_FOUND, "line not found");
        return line;
    }

    private OutageRecord requireOutage(String outageId) {
        OutageRecord outage = outages.get(outageId);
        if (outage == null) throw error(43504, HttpStatus.NOT_FOUND, "outage not found");
        return outage;
    }

    private StatusSourceRecord sourceByInstance(String instanceId) {
        return sources.values().stream().filter(source -> source.instanceId.equals(instanceId)).findFirst().orElse(null);
    }

    private java.util.Optional<SnapshotRecord> latestSnapshot(String instanceId) {
        return snapshots.values().stream().filter(snapshot -> snapshot.instanceId.equals(instanceId)).max(Comparator.comparing(snapshot -> snapshot.checkedAt));
    }

    private Instant latestSnapshotTime() {
        return snapshots.values().stream().map(snapshot -> snapshot.checkedAt).max(Instant::compareTo).orElse(null);
    }

    private String overallStatus(List<Map<String, Object>> instances, List<Map<String, Object>> lines) {
        if (instances.isEmpty() && snapshots.isEmpty()) return "UNKNOWN";
        if (instances.stream().anyMatch(item -> "OFFLINE".equals(item.get("status")))) return "OFFLINE";
        if (instances.stream().anyMatch(item -> "DEGRADED".equals(item.get("status"))) || lines.stream().anyMatch(item -> "DEGRADED".equals(item.get("status")))) return "DEGRADED";
        return instances.isEmpty() ? "UNKNOWN" : "ONLINE";
    }

    private Long uptimeSeconds(Map<String, Object> primaryInstance) {
        if (primaryInstance == null || primaryInstance.get("startedAt") == null) return null;
        return Math.max(0, Duration.between(Instant.parse(String.valueOf(primaryInstance.get("startedAt"))), now()).toSeconds());
    }

    private Map<String, Object> page(List<Map<String, Object>> items, Map<String, String> query) {
        int page = intQuery(query, "page", 1);
        int pageSize = intQuery(query, "pageSize", 20);
        if (page < 1 || pageSize < 1 || pageSize > 100) throw error(40002, HttpStatus.BAD_REQUEST, "invalid page");
        return page(items, new Query(page, pageSize, ""));
    }

    private Map<String, Object> page(List<Map<String, Object>> items, Query query) {
        int from = Math.min((query.page - 1) * query.pageSize, items.size());
        int to = Math.min(from + query.pageSize, items.size());
        return mapOf("items", items.subList(from, to), "page", query.page, "pageSize", query.pageSize, "total", items.size());
    }

    private Query query(Map<String, String> query, String defaultSort, Set<String> allowedSort) {
        int page = intQuery(query, "page", 1);
        int pageSize = intQuery(query, "pageSize", 20);
        if (page < 1 || pageSize < 1 || pageSize > 100) throw error(40002, HttpStatus.BAD_REQUEST, "invalid page");
        return new Query(page, pageSize, sort(query, defaultSort, allowedSort));
    }

    private String sort(Map<String, String> query, String defaultSort, Set<String> allowedSort) {
        String sort = query.getOrDefault("sort", defaultSort);
        if (!allowedSort.contains(sort)) throw error(40003, HttpStatus.BAD_REQUEST, "invalid sort");
        return sort;
    }

    private int intQuery(Map<String, String> query, String field, int fallback) {
        try {
            return query.containsKey(field) ? Integer.parseInt(query.get(field)) : fallback;
        } catch (RuntimeException exception) {
            throw error(40002, HttpStatus.BAD_REQUEST, "invalid page");
        }
    }

    private void sortInstances(List<StatusSourceRecord> items, String sort) {
        if ("name_asc".equals(sort)) items.sort(Comparator.comparing(source -> source.instanceName));
        else if ("onlinePlayers_desc".equals(sort)) items.sort(Comparator.comparing((StatusSourceRecord source) -> latestSnapshot(source.instanceId).map(snapshot -> snapshot.onlinePlayers).orElse(0)).reversed());
        else items.sort(Comparator.comparingInt(source -> source.sortOrder));
    }

    private void sortSources(List<StatusSourceRecord> items, String sort) {
        if ("displayName_asc".equals(sort)) items.sort(Comparator.comparing(source -> source.displayName));
        else if ("updatedAt_desc".equals(sort)) items.sort(Comparator.comparing((StatusSourceRecord source) -> source.updatedAt).reversed());
        else if ("sortOrder_asc".equals(sort)) items.sort(Comparator.comparingInt(source -> source.sortOrder));
        else items.sort(Comparator.comparing((StatusSourceRecord source) -> source.createdAt).reversed());
    }

    private void sortLines(List<LineRecord> items, String sort) {
        if ("name_asc".equals(sort)) items.sort(Comparator.comparing(line -> line.name));
        else if ("latencyMs_asc".equals(sort)) items.sort(Comparator.comparing(line -> line.latencyMs == null ? Integer.MAX_VALUE : line.latencyMs));
        else items.sort(Comparator.comparingInt(line -> line.sortOrder));
    }

    private void sortLinesAdmin(List<LineRecord> items, String sort) {
        if ("name_asc".equals(sort)) items.sort(Comparator.comparing(line -> line.name));
        else if ("updatedAt_desc".equals(sort)) items.sort(Comparator.comparing((LineRecord line) -> line.updatedAt).reversed());
        else if ("sortOrder_asc".equals(sort)) items.sort(Comparator.comparingInt(line -> line.sortOrder));
        else items.sort(Comparator.comparing((LineRecord line) -> line.createdAt).reversed());
    }

    private void sortSnapshots(List<SnapshotRecord> items, String sort) {
        if ("checkedAt_asc".equals(sort)) items.sort(Comparator.comparing(snapshot -> snapshot.checkedAt));
        else if ("onlinePlayers_desc".equals(sort)) items.sort(Comparator.comparing((SnapshotRecord snapshot) -> snapshot.onlinePlayers).reversed());
        else items.sort(Comparator.comparing((SnapshotRecord snapshot) -> snapshot.checkedAt).reversed());
    }

    private void sortOutages(List<OutageRecord> items, String sort) {
        if ("updatedAt_desc".equals(sort)) items.sort(Comparator.comparing((OutageRecord outage) -> outage.updatedAt).reversed());
        else if ("resolvedAt_desc".equals(sort)) items.sort(Comparator.comparing((OutageRecord outage) -> outage.resolvedAt == null ? Instant.EPOCH : outage.resolvedAt).reversed());
        else items.sort(Comparator.comparing((OutageRecord outage) -> outage.startedAt).reversed());
    }

    private String requiredReason(Map<String, Object> body) {
        return requiredString(body, "reason", 1, 200);
    }

    private String requiredString(Map<String, Object> body, String field, int min, int max) {
        String value = optionalString(body, field);
        if (value == null || value.length() < min || value.length() > max) throw error(40001, HttpStatus.BAD_REQUEST, "validation failed");
        return value;
    }

    private String optionalString(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String optionalStringMax(Map<String, Object> body, String field, int max) {
        String value = optionalString(body, field);
        if (value != null && value.length() > max) throw error(40001, HttpStatus.BAD_REQUEST, "validation failed");
        return value;
    }

    private String requiredEnum(Map<String, Object> body, String field, Set<String> allowed) {
        String value = requiredString(body, field, 1, 80);
        requireEnum(value, allowed);
        return value;
    }

    private void requireEnum(String value, Set<String> allowed) {
        if (value != null && !allowed.contains(value)) throw error(40001, HttpStatus.BAD_REQUEST, "validation failed");
    }

    private int optionalInt(Map<String, Object> body, String field, int fallback) {
        return body.containsKey(field) ? integer(body, field) : fallback;
    }

    private int optionalInt(Map<String, Object> body, String field, int fallback, int min, int max) {
        int value = optionalInt(body, field, fallback);
        if (value < min || value > max) throw error(40001, HttpStatus.BAD_REQUEST, "validation failed");
        return value;
    }

    private int integer(Map<String, Object> body, String field) {
        Object value = body.get(field);
        try {
            if (value instanceof Number number) return number.intValue();
            return Integer.parseInt(String.valueOf(value));
        } catch (RuntimeException exception) {
            throw error(40001, HttpStatus.BAD_REQUEST, "validation failed");
        }
    }

    private boolean optionalBoolean(Map<String, Object> body, String field, boolean fallback) {
        Object value = body.get(field);
        if (value == null) return fallback;
        if (value instanceof Boolean bool) return bool;
        String text = String.valueOf(value);
        if ("true".equals(text)) return true;
        if ("false".equals(text)) return false;
        throw error(40001, HttpStatus.BAD_REQUEST, "validation failed");
    }

    private Instant optionalInstant(Map<String, Object> body, String field) {
        String value = optionalString(body, field);
        try {
            return value == null ? null : Instant.parse(value);
        } catch (RuntimeException exception) {
            throw error(40001, HttpStatus.BAD_REQUEST, "validation failed");
        }
    }

    private Instant requiredInstant(Map<String, Object> body, String field) {
        try {
            String value = requiredString(body, field, 1, 80);
            return Instant.parse(value);
        } catch (ServerStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw error(40001, HttpStatus.BAD_REQUEST, "validation failed");
        }
    }

    private Instant instantQuery(String value) {
        if (value == null) return null;
        try {
            return Instant.parse(value);
        } catch (RuntimeException exception) {
            throw error(40001, HttpStatus.BAD_REQUEST, "validation failed");
        }
    }

    private boolean keyword(String value, String keyword) {
        return keyword == null || (value != null && value.toLowerCase().contains(keyword.toLowerCase()));
    }

    private boolean match(String expected, String actual) {
        return expected == null || Objects.equals(expected, actual);
    }

    private boolean boolMatch(String expected, boolean actual) {
        if (expected == null) return true;
        if (!"true".equals(expected) && !"false".equals(expected)) throw error(40001, HttpStatus.BAD_REQUEST, "validation failed");
        return Boolean.parseBoolean(expected) == actual;
    }

    private IdempotencyRecord idempotencyRecord(String key) {
        IdempotencyRecord record = idempotency.get(key);
        if (record != null && record.expiresAt.isBefore(now())) {
            idempotency.remove(key);
            return null;
        }
        return record;
    }

    private int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private String string(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private String slug(String text) {
        return text.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private ServerStatusException error(int code, HttpStatus status, String message) {
        return new ServerStatusException(code, status, message);
    }

    private Map<String, Object> mapOf(Object... pairs) {
        return ServerStatusController.mapOf(pairs);
    }
}

class TestStatusCollector {
    private boolean failUnavailable;
    private boolean failTimeout;
    private boolean failPublicUnavailable;
    private CountDownLatch collectPaused;
    private CountDownLatch collectRelease;

    void reset() {
        failUnavailable = false;
        failTimeout = false;
        failPublicUnavailable = false;
        collectPaused = null;
        collectRelease = null;
    }

    SnapshotRecord collect(StatusSourceRecord source) {
        if (failUnavailable) {
            failUnavailable = false;
            throw new ServerStatusException(46510, HttpStatus.BAD_GATEWAY, "collector unavailable");
        }
        if (failTimeout) {
            failTimeout = false;
            throw new ServerStatusException(46511, HttpStatus.GATEWAY_TIMEOUT, "collector timeout");
        }
        awaitReleaseIfPaused();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return new SnapshotRecord("snap-" + UUID.randomUUID(), source.sourceId, source.instanceId, "line-main", "MANUAL_REFRESH", "ONLINE", "AVAILABLE", "1.21.1", "Beiming refreshed", 36, 80, 24, 27, now, false);
    }

    String consumePublicCheckDegrade() {
        if (failPublicUnavailable) {
            failPublicUnavailable = false;
            return "COLLECTOR_UNAVAILABLE";
        }
        return null;
    }

    void failNextUnavailable() {
        failUnavailable = true;
    }

    void failNextTimeout() {
        failTimeout = true;
    }

    void failNextPublicCheckUnavailable() {
        failPublicUnavailable = true;
    }

    void pauseNextCollect() {
        collectPaused = new CountDownLatch(1);
        collectRelease = new CountDownLatch(1);
    }

    void awaitPausedCollect() {
        try {
            if (collectPaused == null || !collectPaused.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("collector did not pause");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("collector pause interrupted", exception);
        }
    }

    void releasePausedCollect() {
        if (collectRelease != null) collectRelease.countDown();
    }

    private void awaitReleaseIfPaused() {
        CountDownLatch paused = collectPaused;
        CountDownLatch release = collectRelease;
        if (paused == null || release == null) return;
        paused.countDown();
        try {
            if (!release.await(5, TimeUnit.SECONDS)) throw new ServerStatusException(46511, HttpStatus.GATEWAY_TIMEOUT, "collector timeout");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ServerStatusException(46511, HttpStatus.GATEWAY_TIMEOUT, "collector interrupted");
        } finally {
            collectPaused = null;
            collectRelease = null;
        }
    }
}

class TestAuthContextProvider {
    private boolean failUnavailable;
    private boolean failTimeout;
    private boolean failIncompatible;
    private final Map<String, AuthUser> users = new LinkedHashMap<>();

    void reset() {
        failUnavailable = false;
        failTimeout = false;
        failIncompatible = false;
        users.clear();
        users.put("owner-token", new AuthUser("owner", "Owner", Set.of("OWNER"), "ACTIVE"));
        users.put("admin-token", new AuthUser("admin", "Admin", Set.of("ADMIN"), "ACTIVE"));
        users.put("helper-token", new AuthUser("helper", "Helper", Set.of("HELPER"), "ACTIVE"));
        users.put("user-token", new AuthUser("user", "User", Set.of("USER"), "ACTIVE"));
        users.put("disabled-token", new AuthUser("disabled", "Disabled", Set.of("ADMIN"), "DISABLED"));
    }

    AuthUser requireAny(String authorization, String... roles) {
        AuthUser user = current(authorization);
        if (!"ACTIVE".equals(user.status)) throw new ServerStatusException(46500, HttpStatus.BAD_GATEWAY, "auth context unavailable");
        Set<String> allowed = Set.of(roles);
        if (user.roles.stream().noneMatch(allowed::contains)) throw new ServerStatusException(42001, HttpStatus.FORBIDDEN, "role permission denied");
        return user;
    }

    AuthUser current(String authorization) {
        if (failUnavailable) {
            failUnavailable = false;
            throw new ServerStatusException(46500, HttpStatus.BAD_GATEWAY, "auth context unavailable");
        }
        if (failTimeout) {
            failTimeout = false;
            throw new ServerStatusException(46501, HttpStatus.GATEWAY_TIMEOUT, "auth context timeout");
        }
        if (failIncompatible) {
            failIncompatible = false;
            throw new ServerStatusException(46502, HttpStatus.BAD_GATEWAY, "auth context incompatible");
        }
        if (authorization == null || authorization.isBlank()) throw new ServerStatusException(41000, HttpStatus.UNAUTHORIZED, "not authenticated");
        if (!authorization.startsWith("Bearer ")) throw new ServerStatusException(41003, HttpStatus.UNAUTHORIZED, "invalid token format");
        AuthUser user = users.get(authorization.substring("Bearer ".length()));
        if (user == null) throw new ServerStatusException(41001, HttpStatus.UNAUTHORIZED, "invalid session");
        return user;
    }

    void failNextCurrentUnavailable() {
        failUnavailable = true;
    }

    void failNextCurrentTimeout() {
        failTimeout = true;
    }

    void failNextCurrentIncompatible() {
        failIncompatible = true;
    }
}

@Component
class RequestIdFilter extends OncePerRequestFilter {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    static String currentRequestId() {
        String requestId = CURRENT.get();
        return requestId == null ? "req_unknown" : requestId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) requestId = "req_" + UUID.randomUUID();
        CURRENT.set(requestId);
        response.setHeader("X-Request-Id", requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            CURRENT.remove();
        }
    }
}

@RestControllerAdvice(assignableTypes = ServerStatusController.class)
class ServerStatusExceptionHandler {
    @ExceptionHandler(ServerStatusException.class)
    ResponseEntity<Map<String, Object>> serverStatus(ServerStatusException exception) {
        Map<String, Object> body = ServerStatusController.envelope(exception.code, exception.getMessage(), null);
        body.put("requestId", RequestIdFilter.currentRequestId());
        if (exception.code == 40001) {
            body.put("errors", List.of(ServerStatusController.mapOf("field", "request", "reason", exception.getMessage())));
        }
        return ResponseEntity.status(exception.status).body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Map<String, Object>> methodNotSupported(HttpRequestMethodNotSupportedException exception) {
        Map<String, Object> body = ServerStatusController.envelope(40000, "method not supported", null);
        body.put("requestId", RequestIdFilter.currentRequestId());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> unexpected(Exception exception) {
        Map<String, Object> body = ServerStatusController.envelope(51500, "server-status internal error", null);
        body.put("requestId", RequestIdFilter.currentRequestId());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

class ServerStatusException extends RuntimeException {
    final int code;
    final HttpStatus status;

    ServerStatusException(int code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }
}

class AuthUser {
    final String userId;
    final String displayName;
    final Set<String> roles;
    final String status;

    AuthUser(String userId, String displayName, Set<String> roles, String status) {
        this.userId = userId;
        this.displayName = displayName;
        this.roles = roles;
        this.status = status;
    }
}

class Query {
    final int page;
    final int pageSize;
    final String sort;

    Query(int page, int pageSize, String sort) {
        this.page = page;
        this.pageSize = pageSize;
        this.sort = sort;
    }
}

class IdempotencyRecord {
    final String fingerprint;
    final Map<String, Object> result;
    final Instant expiresAt;

    IdempotencyRecord(Map<String, Object> body, Map<String, Object> result, Instant expiresAt) {
        this.fingerprint = canonical(body);
        this.result = new LinkedHashMap<>(result);
        this.expiresAt = expiresAt;
    }

    Map<String, Object> same(Map<String, Object> body) {
        if (!fingerprint.equals(canonical(body))) throw new ServerStatusException(43002, HttpStatus.CONFLICT, "idempotency key conflict");
        return new LinkedHashMap<>(result);
    }

    private static String canonical(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                    .map(entry -> quote(String.valueOf(entry.getKey())) + ":" + canonical(entry.getValue()))
                    .collect(java.util.stream.Collectors.joining(",", "{", "}"));
        }
        if (value instanceof Iterable<?> iterable) {
            List<String> items = new ArrayList<>();
            for (Object item : iterable) items.add(canonical(item));
            return items.stream().collect(java.util.stream.Collectors.joining(",", "[", "]"));
        }
        if (value instanceof String text) return quote(text);
        return String.valueOf(value);
    }

    private static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}

class StatusSourceRecord {
    final String sourceId;
    final String instanceId;
    String displayName;
    String instanceName;
    String instanceKind;
    String sourceType;
    String configStatus;
    boolean publicVisible;
    boolean primary;
    String target;
    int timeoutMs;
    int sortOrder;
    Instant startedAt;
    String adminNote;
    String createdBy;
    String updatedBy;
    Instant createdAt;
    Instant updatedAt;

    StatusSourceRecord(String sourceId, String instanceId, String displayName, String instanceName, String instanceKind, String sourceType, String configStatus, boolean publicVisible, boolean primary, String target, int timeoutMs, int sortOrder, Instant startedAt, String adminNote, String createdBy, String updatedBy, Instant createdAt, Instant updatedAt) {
        this.sourceId = sourceId;
        this.instanceId = instanceId;
        this.displayName = displayName;
        this.instanceName = instanceName;
        this.instanceKind = instanceKind;
        this.sourceType = sourceType;
        this.configStatus = configStatus;
        this.publicVisible = publicVisible;
        this.primary = primary;
        this.target = target;
        this.timeoutMs = timeoutMs;
        this.sortOrder = sortOrder;
        this.startedAt = startedAt;
        this.adminNote = adminNote;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

class LineRecord {
    final String lineId;
    String name;
    String entryAddress;
    String checkTarget;
    String description;
    String configStatus;
    String currentStatus;
    boolean publicVisible;
    boolean primary;
    int sortOrder;
    Integer latencyMs;
    Double packetLossPercent;
    String adminNote;
    String createdBy;
    String updatedBy;
    Instant createdAt;
    Instant updatedAt;

    LineRecord(String lineId, String name, String entryAddress, String checkTarget, String description, String configStatus, String currentStatus, boolean publicVisible, boolean primary, int sortOrder, Integer latencyMs, Double packetLossPercent, String adminNote, String createdBy, String updatedBy, Instant createdAt, Instant updatedAt) {
        this.lineId = lineId;
        this.name = name;
        this.entryAddress = entryAddress;
        this.checkTarget = checkTarget;
        this.description = description;
        this.configStatus = configStatus;
        this.currentStatus = currentStatus;
        this.publicVisible = publicVisible;
        this.primary = primary;
        this.sortOrder = sortOrder;
        this.latencyMs = latencyMs;
        this.packetLossPercent = packetLossPercent;
        this.adminNote = adminNote;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

class SnapshotRecord {
    final String snapshotId;
    final String sourceId;
    final String instanceId;
    final String lineId;
    final String source;
    final String status;
    final String lineStatus;
    final String version;
    final String motd;
    final int onlinePlayers;
    final int maxPlayers;
    final Integer latencyMs;
    final Integer lineLatencyMs;
    final Instant checkedAt;
    final boolean degraded;

    SnapshotRecord(String snapshotId, String sourceId, String instanceId, String lineId, String source, String status, String lineStatus, String version, String motd, int onlinePlayers, int maxPlayers, Integer latencyMs, Integer lineLatencyMs, Instant checkedAt, boolean degraded) {
        this.snapshotId = snapshotId;
        this.sourceId = sourceId;
        this.instanceId = instanceId;
        this.lineId = lineId;
        this.source = source;
        this.status = status;
        this.lineStatus = lineStatus;
        this.version = version;
        this.motd = motd;
        this.onlinePlayers = onlinePlayers;
        this.maxPlayers = maxPlayers;
        this.latencyMs = latencyMs;
        this.lineLatencyMs = lineLatencyMs;
        this.checkedAt = checkedAt;
        this.degraded = degraded;
    }
}

class OutageRecord {
    final String outageId;
    String title;
    String publicMessage;
    String status;
    String severity;
    String instanceId;
    String lineId;
    Instant startedAt;
    Instant resolvedAt;
    String acknowledgedBy;
    String resolvedBy;
    String archivedBy;
    Instant acknowledgedAt;
    Instant archivedAt;
    String internalReason;
    String adminNote;
    boolean publicVisible;
    String createdBy;
    String updatedBy;
    Instant createdAt;
    Instant updatedAt;
    Instant deletedAt;

    OutageRecord(String outageId, String title, String publicMessage, String status, String severity, String instanceId, String lineId, Instant startedAt, Instant resolvedAt, String acknowledgedBy, String resolvedBy, String archivedBy, Instant acknowledgedAt, Instant archivedAt, String internalReason, String adminNote, boolean publicVisible, String createdBy, String updatedBy, Instant createdAt, Instant updatedAt, Instant deletedAt) {
        this.outageId = outageId;
        this.title = title;
        this.publicMessage = publicMessage;
        this.status = status;
        this.severity = severity;
        this.instanceId = instanceId;
        this.lineId = lineId;
        this.startedAt = startedAt;
        this.resolvedAt = resolvedAt;
        this.acknowledgedBy = acknowledgedBy;
        this.resolvedBy = resolvedBy;
        this.archivedBy = archivedBy;
        this.acknowledgedAt = acknowledgedAt;
        this.archivedAt = archivedAt;
        this.internalReason = internalReason;
        this.adminNote = adminNote;
        this.publicVisible = publicVisible;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }
}

class AuditRecord {
    final String id;
    final String requestId;
    final String actorUserId;
    final String actorRole;
    final String targetType;
    final String targetId;
    final String action;
    final String reason;
    final String result;
    final Instant createdAt;

    AuditRecord(String id, String requestId, String actorUserId, String actorRole, String targetType, String targetId, String action, String reason, String result, Instant createdAt) {
        this.id = id;
        this.requestId = requestId;
        this.actorUserId = actorUserId;
        this.actorRole = actorRole;
        this.targetType = targetType;
        this.targetId = targetId;
        this.action = action;
        this.reason = reason;
        this.result = result;
        this.createdAt = createdAt;
    }
}
