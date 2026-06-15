package cn.beiming.backuprecovery;

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
        properties = "backup-recovery.test-controls.enabled=true"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class BackupRecoveryRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "backup-recovery-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:backup_recovery_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
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
                    "backup_flow_request_log",
                    "backup_flow_policies",
                    "backup_flow_jobs",
                    "backup_flow_points",
                    "backup_flow_verifications",
                    "backup_flow_drills",
                    "backup_flow_restores",
                    "backup_flow_audits"
            )) {
                deleteFlowRows(statement, table);
            }
        }
    }

    @Test
    void createPolicyRunsThroughBackendAndDatabaseThenReturnsDraftPolicy() throws Exception {
        String requestId = "req-backup-policy-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/backup-recovery/policies",
                bearerHeaders("br-admin-token", requestId),
                policyBody("policy-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(json.at("/data/createdBy").asText()).isEqualTo("br-admin-user");
        String policyId = json.at("/data/policyId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM backup_flow_policies WHERE flow_id = ? AND policy_id = ? AND action = 'BACKUP_POLICY_CREATED'",
                    FLOW_ID, policyId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT created_by FROM backup_flow_policies WHERE flow_id = ? AND policy_id = ? AND action = 'BACKUP_POLICY_CREATED'",
                    FLOW_ID, policyId, "br-admin-user");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM backup_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'BACKUP_POLICY_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM backup_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/backup-recovery/policies'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: backup-recovery policy create reached backend, wrote policy/audit/request rows, and returned 201.");
    }

    @Test
    void createJobRunsThroughBackendAndDatabaseThenReturnsSucceededJobAndBackupPoint() throws Exception {
        String requestId = "req-backup-job-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/backup-recovery/jobs",
                bearerHeaders("br-admin-token", requestId),
                jobBody("job-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/data/status").asText()).isEqualTo("SUCCEEDED");
        String jobId = json.at("/data/jobId").asText();
        String pointId = json.at("/data/resultSummary/backupPointId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM backup_flow_jobs WHERE flow_id = ? AND job_id = ? AND action = 'BACKUP_JOB_CREATED'",
                    FLOW_ID, jobId, "SUCCEEDED");
            assertSingleValue(connection,
                    "SELECT status FROM backup_flow_points WHERE flow_id = ? AND backup_point_id = ? AND action = 'BACKUP_JOB_CREATED'",
                    FLOW_ID, pointId, "AVAILABLE");
            assertSingleValue(connection,
                    "SELECT response_code FROM backup_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/backup-recovery/jobs'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: backup-recovery job create reached backend, wrote job/point/audit/request rows, and returned 201.");
    }

    @Test
    void verifyPointRunsThroughBackendAndDatabaseThenReturnsPassedVerification() throws Exception {
        String pointId = createBackupPoint("verify");
        String requestId = "req-backup-verify-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/backup-recovery/backup-points/" + pointId + "/verify",
                bearerHeaders("br-approver-token", requestId),
                Map.of("validationLevel", "CHECKSUM", "reason", "校验数据库流备份点", "idempotencyKey", "verify-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/data/status").asText()).isEqualTo("PASSED");
        String verificationId = json.at("/data/verificationId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM backup_flow_verifications WHERE flow_id = ? AND verification_id = ? AND action = 'BACKUP_POINT_VERIFIED'",
                    FLOW_ID, verificationId, "PASSED");
            assertSingleValue(connection,
                    "SELECT status FROM backup_flow_points WHERE flow_id = ? AND backup_point_id = ? AND action = 'BACKUP_POINT_VERIFIED'",
                    FLOW_ID, pointId, "VERIFIED");
            assertSingleValue(connection,
                    "SELECT response_code FROM backup_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, requestId, "/api/v1/backup-recovery/backup-points/" + pointId + "/verify", 200);
        }
        System.out.println("SQL evidence: backup-recovery point verify reached backend, wrote verification/point/audit/request rows, and returned 200.");
    }

    @Test
    void restoreDrillAndApprovalRunThroughBackendAndDatabaseThenReturnCompletedRestore() throws Exception {
        String pointId = createBackupPoint("restore");
        String drillRequestId = "req-backup-drill-" + FLOW_ID;
        TestHttpResponse drillResponse = exchange(
                HttpMethod.POST,
                "/api/v1/backup-recovery/restore-drills",
                bearerHeaders("br-approver-token", drillRequestId),
                drillBody(pointId, "drill-" + randomKey())
        );

        assertThat(drillResponse.statusCode()).isEqualTo(201);
        JsonNode drillJson = objectMapper.readTree(drillResponse.body());
        assertThat(drillJson.at("/data/status").asText()).isEqualTo("PASSED");
        String drillId = drillJson.at("/data/drillId").asText();

        String restoreRequestId = "req-backup-restore-" + FLOW_ID;
        TestHttpResponse restoreResponse = exchange(
                HttpMethod.POST,
                "/api/v1/backup-recovery/restore-requests",
                bearerHeaders("br-admin-token", restoreRequestId),
                restoreBody(pointId, drillId, "restore-" + randomKey())
        );

        assertThat(restoreResponse.statusCode()).isEqualTo(201);
        JsonNode restoreJson = objectMapper.readTree(restoreResponse.body());
        assertThat(restoreJson.at("/data/status").asText()).isEqualTo("PENDING_APPROVAL");
        String restoreId = restoreJson.at("/data/restoreRequestId").asText();

        String approveRequestId = "req-backup-approve-" + FLOW_ID;
        TestHttpResponse approveResponse = exchange(
                HttpMethod.PATCH,
                "/api/v1/backup-recovery/restore-requests/" + restoreId + "/approve",
                bearerHeaders("owner-token", approveRequestId),
                Map.of("reviewComment", "批准数据库流模拟恢复", "confirmText", "APPROVE_SIMULATED_RESTORE", "reason", "审批数据库流恢复", "idempotencyKey", "approve-" + randomKey())
        );

        assertThat(approveResponse.statusCode()).isEqualTo(200);
        JsonNode approveJson = objectMapper.readTree(approveResponse.body());
        assertThat(approveJson.at("/data/status").asText()).isEqualTo("COMPLETED_SIMULATED");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM backup_flow_drills WHERE flow_id = ? AND drill_id = ? AND action = 'RESTORE_DRILL_CREATED'",
                    FLOW_ID, drillId, "PASSED");
            assertSingleValue(connection,
                    "SELECT status FROM backup_flow_restores WHERE flow_id = ? AND restore_request_id = ? AND action = 'RESTORE_REQUEST_CREATED'",
                    FLOW_ID, restoreId, "PENDING_APPROVAL");
            assertSingleValue(connection,
                    "SELECT status FROM backup_flow_restores WHERE flow_id = ? AND restore_request_id = ? AND action = 'RESTORE_REQUEST_APPROVED'",
                    FLOW_ID, restoreId, "COMPLETED_SIMULATED");
            assertSingleValue(connection,
                    "SELECT response_code FROM backup_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, approveRequestId, "/api/v1/backup-recovery/restore-requests/" + restoreId + "/approve", 200);
        }
        System.out.println("SQL evidence: backup-recovery restore drill/request/approval reached backend, wrote drill/restore/audit/request rows, and returned 200.");
    }

    private String createBackupPoint(String purpose) throws Exception {
        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/backup-recovery/jobs",
                bearerHeaders("br-admin-token", "req-backup-setup-" + purpose + "-" + FLOW_ID),
                jobBody(purpose + "-" + randomKey())
        );
        assertThat(response.statusCode()).isEqualTo(201);
        return objectMapper.readTree(response.body()).at("/data/resultSummary/backupPointId").asText();
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("X-Request-Id", requestId);
        headers.set("X-Test-Flow-Id", FLOW_ID);
        return headers;
    }

    private Map<String, Object> policyBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "Database Flow Backup " + idempotencyKey);
        body.put("domains", List.of("DATABASE_AUTH", "RESOURCE_METADATA", "OPS_AUDIT_INDEX"));
        body.put("scheduleSummary", Map.of("mode", "DAILY", "timezone", "Asia/Shanghai", "windowMinutes", 90));
        body.put("retentionDays", 30);
        body.put("minimumCopies", 2);
        body.put("storageRef", Map.of("alias", "backup-vault-main", "regionSummary", "cn-local", "tier", "WARM"));
        body.put("encryptionMode", "MANAGED_KEY");
        body.put("reason", "创建 backup-recovery 数据库流策略");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> jobBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("policyId", "policy-main");
        body.put("trigger", "ADMIN_MANUAL");
        body.put("domains", List.of("DATABASE_AUTH", "RESOURCE_METADATA"));
        body.put("reason", "创建 backup-recovery 数据库流任务");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> drillBody(String backupPointId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("backupPointId", backupPointId);
        body.put("domains", List.of("DATABASE_AUTH", "RESOURCE_METADATA"));
        body.put("validationPlan", Map.of("mode", "SANDBOX_READ", "checks", List.of("CHECKSUM", "SCHEMA")));
        body.put("reason", "创建 backup-recovery 数据库流恢复演练");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> restoreBody(String backupPointId, String drillId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("backupPointId", backupPointId);
        body.put("domains", List.of("DATABASE_AUTH", "RESOURCE_METADATA"));
        body.put("restoreMode", "SANDBOX_RESTORE");
        body.put("drillId", drillId);
        body.put("impactSummary", Map.of("scope", "sandbox", "writesProduction", false));
        body.put("confirmText", "REQUEST_RESTORE_REVIEW");
        body.put("reason", "申请 backup-recovery 数据库流模拟恢复");
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
                CREATE TABLE IF NOT EXISTS backup_flow_policies (
                    flow_id VARCHAR(128) NOT NULL,
                    policy_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS backup_flow_jobs (
                    flow_id VARCHAR(128) NOT NULL,
                    job_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    policy_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS backup_flow_points (
                    flow_id VARCHAR(128) NOT NULL,
                    backup_point_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    policy_id VARCHAR(128) NOT NULL,
                    job_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS backup_flow_verifications (
                    flow_id VARCHAR(128) NOT NULL,
                    verification_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    backup_point_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS backup_flow_drills (
                    flow_id VARCHAR(128) NOT NULL,
                    drill_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    backup_point_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS backup_flow_restores (
                    flow_id VARCHAR(128) NOT NULL,
                    restore_request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    backup_point_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    requested_by VARCHAR(128) NOT NULL,
                    approved_by VARCHAR(128),
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS backup_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS backup_flow_request_log (
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
        BackupRecoveryFlowEvidenceRecorder backupRecoveryFlowEvidenceRecorder() {
            return new JdbcBackupRecoveryFlowEvidenceRecorder();
        }
    }

    static class JdbcBackupRecoveryFlowEvidenceRecorder implements BackupRecoveryFlowEvidenceRecorder {
        @Override
        public void recordPolicyWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO backup_flow_policies(flow_id, policy_id, action, status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("policyId"), action, payload.get("status"), payload.get("createdBy"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("policyId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write backup-recovery policy database evidence", exception);
            }
        }

        @Override
        public void recordJobWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insertJob(connection, flowId, action, payload);
                Object point = payload.get("backupPoint");
                if (point instanceof Map<?, ?> pointMap) {
                    insertPoint(connection, flowId, action, pointMap);
                }
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("jobId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write backup-recovery job database evidence", exception);
            }
        }

        @Override
        public void recordPointVerification(HttpServletRequest request, String action, Map<String, Object> verification, Map<String, Object> point, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO backup_flow_verifications(flow_id, verification_id, action, backup_point_id, status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, verification.get("verificationId"), action, verification.get("backupPointId"), verification.get("status"), verification.get("createdBy"), Timestamp.from(Instant.now()));
                insertPoint(connection, flowId, action, point);
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(verification.get("backupPointId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write backup-recovery verification database evidence", exception);
            }
        }

        @Override
        public void recordDrillWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO backup_flow_drills(flow_id, drill_id, action, backup_point_id, status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("drillId"), action, payload.get("backupPointId"), payload.get("status"), payload.get("createdBy"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("drillId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write backup-recovery drill database evidence", exception);
            }
        }

        @Override
        public void recordRestoreWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO backup_flow_restores(flow_id, restore_request_id, action, backup_point_id, status, requested_by, approved_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("restoreRequestId"), action, payload.get("backupPointId"), payload.get("status"), payload.get("requestedBy"), payload.get("approvedBy"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("restoreRequestId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write backup-recovery restore database evidence", exception);
            }
        }

        private static void insertJob(Connection connection, String flowId, String action, Map<String, Object> payload) throws Exception {
            insert(connection,
                    "INSERT INTO backup_flow_jobs(flow_id, job_id, action, policy_id, status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    flowId, payload.get("jobId"), action, payload.get("policyId"), payload.get("status"), payload.get("createdBy"), Timestamp.from(Instant.now()));
        }

        private static void insertPoint(Connection connection, String flowId, String action, Map<?, ?> point) throws Exception {
            insert(connection,
                    "INSERT INTO backup_flow_points(flow_id, backup_point_id, action, policy_id, job_id, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    flowId, point.get("backupPointId"), action, point.get("policyId"), point.get("jobId"), point.get("status"), Timestamp.from(Instant.now()));
        }

        private static void insertAuditAndRequest(Connection connection, String flowId, String requestId, String action, String targetId, String path, int responseCode) throws Exception {
            insert(connection,
                    "INSERT INTO backup_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, requestId, action, targetId, "SUCCESS", Timestamp.from(Instant.now()));
            insert(connection,
                    "INSERT INTO backup_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
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
