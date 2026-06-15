package cn.beiming.cloudrevesync;

import cn.beiming.opscore.OpsCoreServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = OpsCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "cloudreve-sync.test-controls.enabled=true"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CloudreveSyncRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "cloudreve-sync-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:cloudreve_sync_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            createEvidenceTables(statement);
            for (String table : List.of(
                    "cloudreve_flow_request_log",
                    "cloudreve_flow_providers",
                    "cloudreve_flow_shares",
                    "cloudreve_flow_jobs",
                    "cloudreve_flow_files",
                    "cloudreve_flow_audits"
            )) {
                deleteFlowRows(statement, table);
            }
        }
    }

    @Test
    void createProviderRunsThroughBackendAndDatabaseThenReturnsEnabledProvider() throws Exception {
        String requestId = "req-cloudreve-provider-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/cloudreve-sync/providers",
                bearerHeaders("sync-admin-token", requestId),
                providerBody("provider-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/status").asText()).isEqualTo("ENABLED");
        assertThat(json.at("/data/createdBy").asText()).isEqualTo("sync-admin-user");
        String providerId = json.at("/data/providerId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM cloudreve_flow_providers WHERE flow_id = ? AND provider_id = ? AND action = 'CLOUDREVE_PROVIDER_CREATED'",
                    FLOW_ID, providerId, "ENABLED");
            assertSingleValue(connection,
                    "SELECT created_by FROM cloudreve_flow_providers WHERE flow_id = ? AND provider_id = ? AND action = 'CLOUDREVE_PROVIDER_CREATED'",
                    FLOW_ID, providerId, "sync-admin-user");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM cloudreve_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'CLOUDREVE_PROVIDER_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM cloudreve_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/cloudreve-sync/providers'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: cloudreve-sync provider create reached backend, wrote provider/audit/request rows, and returned 201.");
    }

    @Test
    void resolveShareRunsThroughBackendAndDatabaseThenReturnsShareSnapshot() throws Exception {
        String requestId = "req-cloudreve-share-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/cloudreve-sync/shares/resolve",
                bearerHeaders("sync-file-token", requestId),
                resolveBody("resolve-" + randomKey(), "file-no-share")
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/data/providerId").asText()).isEqualTo("provider-main");
        assertThat(json.at("/data/fileId").asText()).isEqualTo("file-no-share");
        assertThat(json.at("/data/shareSnapshotId").asText()).isNotBlank();
        String shareSnapshotId = json.at("/data/shareSnapshotId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM cloudreve_flow_shares WHERE flow_id = ? AND share_snapshot_id = ? AND action = 'CLOUDREVE_SHARE_RESOLVED'",
                    FLOW_ID, shareSnapshotId, "ACTIVE");
            assertSingleValue(connection,
                    "SELECT file_id FROM cloudreve_flow_shares WHERE flow_id = ? AND share_snapshot_id = ? AND action = 'CLOUDREVE_SHARE_RESOLVED'",
                    FLOW_ID, shareSnapshotId, "file-no-share");
            assertSingleValue(connection,
                    "SELECT response_code FROM cloudreve_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/cloudreve-sync/shares/resolve'",
                    FLOW_ID, requestId, 200);
        }
        System.out.println("SQL evidence: cloudreve-sync share resolve reached backend, wrote share/audit/request rows, and returned 200.");
    }

    @Test
    void syncJobRunsThroughBackendAndDatabaseThenReturnsSucceededJobAndFileSnapshot() throws Exception {
        String requestId = "req-cloudreve-job-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/cloudreve-sync/sync-jobs",
                bearerHeaders("sync-file-token", requestId),
                jobBody("DIRECTORY_SYNC", "job-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/data/status").asText()).isEqualTo("SUCCEEDED");
        assertThat(json.at("/data/providerId").asText()).isEqualTo("provider-main");
        String jobId = json.at("/data/jobId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM cloudreve_flow_jobs WHERE flow_id = ? AND job_id = ? AND action = 'CLOUDREVE_SYNC_JOB_CREATED'",
                    FLOW_ID, jobId, "SUCCEEDED");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM cloudreve_flow_files WHERE flow_id = ? AND job_id = ? AND action = 'CLOUDREVE_SYNC_JOB_CREATED'",
                    FLOW_ID, jobId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM cloudreve_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/cloudreve-sync/sync-jobs'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: cloudreve-sync sync job reached backend, wrote job/file/audit/request rows, and returned 201.");
    }

    @Test
    void cancelSyncJobRunsThroughBackendAndDatabaseThenReturnsCancelledJob() throws Exception {
        TestHttpResponse created = exchange(
                HttpMethod.POST,
                "/api/v1/cloudreve-sync/sync-jobs",
                bearerHeaders("sync-file-token", "req-cloudreve-setup-" + FLOW_ID, Map.of("X-Test-Cloudreve-Mode", "pending")),
                jobBody("DIRECTORY_SYNC", "pending-" + randomKey())
        );
        assertThat(created.statusCode()).isEqualTo(201);
        String jobId = objectMapper.readTree(created.body()).at("/data/jobId").asText();
        String requestId = "req-cloudreve-cancel-" + FLOW_ID;

        TestHttpResponse cancelled = exchange(
                HttpMethod.PATCH,
                "/api/v1/cloudreve-sync/sync-jobs/" + jobId + "/cancel",
                bearerHeaders("sync-file-token", requestId),
                Map.of("reason", "取消待执行数据库流任务", "idempotencyKey", "cancel-" + randomKey())
        );

        assertThat(cancelled.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(cancelled.body());
        assertThat(json.at("/data/status").asText()).isEqualTo("CANCELLED");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM cloudreve_flow_jobs WHERE flow_id = ? AND job_id = ? AND action = 'CLOUDREVE_SYNC_JOB_CANCELLED'",
                    FLOW_ID, jobId, "CANCELLED");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM cloudreve_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'CLOUDREVE_SYNC_JOB_CANCELLED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM cloudreve_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, requestId, "/api/v1/cloudreve-sync/sync-jobs/" + jobId + "/cancel", 200);
        }
        System.out.println("SQL evidence: cloudreve-sync sync job cancel reached backend, wrote cancelled job/audit/request rows, and returned 200.");
    }

    private TestHttpResponse exchange(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method.name(), publisher);
        headers.forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
        HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new TestHttpResponse(response.statusCode(), response.body());
    }

    private HttpHeaders bearerHeaders(String token, String requestId) {
        return bearerHeaders(token, requestId, Map.of());
    }

    private HttpHeaders bearerHeaders(String token, String requestId, Map<String, String> extraHeaders) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("X-Request-Id", requestId);
        headers.set("X-Test-Flow-Id", FLOW_ID);
        extraHeaders.forEach(headers::set);
        return headers;
    }

    private Map<String, Object> providerBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "Database Flow Cloudreve " + idempotencyKey);
        body.put("baseUrl", "https://cloud.example.com");
        body.put("authMode", "TEST_FAKE");
        body.put("credential", "cloudreve-secret-token");
        body.put("capabilities", List.of("FILE_LIST", "FILE_METADATA", "SHARE_RESOLVE", "SHARE_REFRESH"));
        body.put("timeoutMs", 5000);
        body.put("opsAssetRef", Map.of("assetId", "asset-cloudreve-flow", "source", "ops-control"));
        body.put("enabled", true);
        body.put("reason", "创建 cloudreve-sync 数据库流 provider");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> resolveBody(String idempotencyKey, String fileId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providerId", "provider-main");
        body.put("fileId", fileId);
        body.put("resourceRef", Map.of("resourceId", "res-orphan", "versionId", "ver-flow-1"));
        body.put("allowStale", false);
        body.put("reason", "解析 cloudreve-sync 数据库流分享快照");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> jobBody(String jobType, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobType", jobType);
        body.put("providerId", "provider-main");
        body.put("trigger", "ADMIN_MANUAL");
        body.put("target", Map.of("path", "/packs"));
        body.put("reason", "创建 cloudreve-sync 数据库流同步任务");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private static void assertSingleValue(Connection connection, String sql, Object first, Object second, Object expected) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, first);
            statement.setObject(2, second);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).as(sql).isTrue();
                assertThat(result.getObject(1)).isEqualTo(expected);
                assertThat(result.next()).as(sql + " must return one row").isFalse();
            }
        }
    }

    private static void assertSingleValue(Connection connection, String sql, Object first, Object second, Object third, Object expected) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, first);
            statement.setObject(2, second);
            statement.setObject(3, third);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).as(sql).isTrue();
                assertThat(result.getObject(1)).isEqualTo(expected);
                assertThat(result.next()).as(sql + " must return one row").isFalse();
            }
        }
    }

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private static void createEvidenceTables(Statement statement) throws Exception {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS cloudreve_flow_providers (
                    flow_id VARCHAR(128) NOT NULL,
                    provider_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS cloudreve_flow_shares (
                    flow_id VARCHAR(128) NOT NULL,
                    share_snapshot_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    provider_id VARCHAR(128) NOT NULL,
                    file_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS cloudreve_flow_jobs (
                    flow_id VARCHAR(128) NOT NULL,
                    job_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    job_type VARCHAR(64) NOT NULL,
                    provider_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS cloudreve_flow_files (
                    flow_id VARCHAR(128) NOT NULL,
                    job_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    file_id VARCHAR(128) NOT NULL,
                    provider_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS cloudreve_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS cloudreve_flow_request_log (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    path VARCHAR(256) NOT NULL,
                    response_code INT NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
    }

    private static void deleteFlowRows(Statement statement, String table) {
        try {
            statement.executeUpdate("DELETE FROM " + table + " WHERE flow_id = '" + FLOW_ID + "'");
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String randomKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @TestConfiguration
    static class EvidenceConfiguration {
        @Bean
        CloudreveSyncFlowEvidenceRecorder cloudreveSyncFlowEvidenceRecorder() {
            return new JdbcCloudreveSyncFlowEvidenceRecorder();
        }
    }

    static class JdbcCloudreveSyncFlowEvidenceRecorder implements CloudreveSyncFlowEvidenceRecorder {
        @Override
        public void recordProviderWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO cloudreve_flow_providers(flow_id, provider_id, action, status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("providerId"), action, payload.get("status"), payload.get("createdBy"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("providerId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write cloudreve-sync provider database evidence", exception);
            }
        }

        @Override
        public void recordShareWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO cloudreve_flow_shares(flow_id, share_snapshot_id, action, provider_id, file_id, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("shareSnapshotId"), action, payload.get("providerId"), payload.get("fileId"), payload.get("shareStatus"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("shareSnapshotId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write cloudreve-sync share database evidence", exception);
            }
        }

        @Override
        public void recordJobWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO cloudreve_flow_jobs(flow_id, job_id, action, job_type, provider_id, status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("jobId"), action, payload.get("jobType"), payload.get("providerId"), payload.get("status"), payload.get("createdBy"), Timestamp.from(Instant.now()));
                insertGeneratedFile(connection, flowId, action, payload);
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("jobId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write cloudreve-sync job database evidence", exception);
            }
        }

        private static void insertGeneratedFile(Connection connection, String flowId, String action, Map<String, Object> payload) throws Exception {
            Object generated = payload.get("generatedFile");
            if (!(generated instanceof Map<?, ?> file)) {
                return;
            }
            insert(connection,
                    "INSERT INTO cloudreve_flow_files(flow_id, job_id, action, file_id, provider_id, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    flowId, payload.get("jobId"), action, file.get("fileId"), file.get("providerId"), file.get("status"), Timestamp.from(Instant.now()));
        }

        private static void insertAuditAndRequest(Connection connection, String flowId, String requestId, String action, String targetId, String path, int responseCode) throws Exception {
            insert(connection,
                    "INSERT INTO cloudreve_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, requestId, action, targetId, "SUCCESS", Timestamp.from(Instant.now()));
            insert(connection,
                    "INSERT INTO cloudreve_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
                    flowId, requestId, path, responseCode, Timestamp.from(Instant.now()));
        }

        private static void insert(Connection connection, String sql, Object... values) throws Exception {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int index = 0; index < values.length; index++) {
                    statement.setObject(index + 1, values[index]);
                }
                statement.executeUpdate();
            }
        }
    }

    record TestHttpResponse(int statusCode, String body) {
    }
}
