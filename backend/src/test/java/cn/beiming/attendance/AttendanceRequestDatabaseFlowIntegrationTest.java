package cn.beiming.attendance;

import cn.beiming.admission.AdmissionCoreServiceApplication;
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
        classes = AdmissionCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "attendance.test-controls.enabled=true"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AttendanceRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "attendance-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:attendance_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
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
                    "attendance_flow_request_log",
                    "attendance_flow_accounts",
                    "attendance_flow_ledgers",
                    "attendance_flow_contributions",
                    "attendance_flow_monthly_runs",
                    "attendance_flow_candidates",
                    "attendance_flow_audits"
            )) {
                deleteFlowRows(statement, table);
            }
        }
    }

    @Test
    void initializeRunsThroughBackendAndDatabaseThenReturnsActiveAccount() throws Exception {
        String requestId = "req-att-init-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/attendance/admin/initializations",
                bearerHeaders("admin-token", requestId),
                initBody("wl-app-1", "init-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/account/status").asText()).isEqualTo("ACTIVE");
        assertThat(json.at("/data/account/scoreBalance").asInt()).isEqualTo(100);
        assertThat(json.at("/data/ledger/type").asText()).isEqualTo("INITIAL_GRANT");
        String accountId = json.at("/data/account/accountId").asText();
        String ledgerId = json.at("/data/ledger/ledgerId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM attendance_flow_accounts WHERE flow_id = ? AND account_id = ? AND action = 'ATTENDANCE_INITIALIZED'",
                    FLOW_ID, accountId, "ACTIVE");
            assertSingleValue(connection,
                    "SELECT type FROM attendance_flow_ledgers WHERE flow_id = ? AND ledger_id = ? AND action = 'ATTENDANCE_INITIAL_LEDGER_CREATED'",
                    FLOW_ID, ledgerId, "INITIAL_GRANT");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM attendance_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'ATTENDANCE_INITIALIZED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM attendance_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/attendance/admin/initializations'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: attendance initialize reached backend, wrote account/ledger/audit/request rows, and returned 201.");
    }

    @Test
    void adjustmentRunsThroughBackendAndDatabaseThenReturnsCandidateWhenBalanceReachesZero() throws Exception {
        String accountId = initialize("wl-app-1", "setup-adjust");
        String requestId = "req-att-adjust-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/attendance/admin/accounts/" + accountId + "/adjustments",
                bearerHeaders("admin-token", requestId),
                adjustBody(-100, "adjust-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/account/status").asText()).isEqualTo("REMOVAL_CANDIDATE");
        assertThat(json.at("/data/account/scoreBalance").asInt()).isZero();
        assertThat(json.at("/data/ledger/type").asText()).isEqualTo("ADMIN_ADJUSTMENT");
        String ledgerId = json.at("/data/ledger/ledgerId").asText();
        String candidateId = json.at("/data/candidate/candidateId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT score_balance FROM attendance_flow_accounts WHERE flow_id = ? AND account_id = ? AND action = 'ATTENDANCE_SCORE_ADJUSTED'",
                    FLOW_ID, accountId, 0);
            assertSingleValue(connection,
                    "SELECT delta FROM attendance_flow_ledgers WHERE flow_id = ? AND ledger_id = ? AND action = 'ATTENDANCE_ADJUSTMENT_LEDGER_CREATED'",
                    FLOW_ID, ledgerId, -100);
            assertSingleValue(connection,
                    "SELECT status FROM attendance_flow_candidates WHERE flow_id = ? AND candidate_id = ? AND action = 'ATTENDANCE_REMOVAL_CANDIDATE_CREATED'",
                    FLOW_ID, candidateId, "OPEN");
            assertSingleValue(connection,
                    "SELECT response_code FROM attendance_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, requestId, "/api/v1/attendance/admin/accounts/" + accountId + "/adjustments", 200);
        }
        System.out.println("SQL evidence: attendance adjustment reached backend, wrote account/ledger/candidate/request rows, and returned 200.");
    }

    @Test
    void contributionRunsThroughBackendAndDatabaseThenReturnsRewardLedger() throws Exception {
        String accountId = initialize("wl-app-1", "setup-contribution");
        String requestId = "req-att-contribution-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/attendance/admin/contributions",
                bearerHeaders("admin-token", requestId),
                contributionBody(accountId, 30, "contribution-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/contribution/scoreDelta").asInt()).isEqualTo(30);
        assertThat(json.at("/data/ledger/type").asText()).isEqualTo("CONTRIBUTION_REWARD");
        assertThat(json.at("/data/account/scoreBalance").asInt()).isEqualTo(130);
        String contributionId = json.at("/data/contribution/contributionId").asText();
        String ledgerId = json.at("/data/ledger/ledgerId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT score_delta FROM attendance_flow_contributions WHERE flow_id = ? AND contribution_id = ? AND action = 'ATTENDANCE_CONTRIBUTION_CREATED'",
                    FLOW_ID, contributionId, 30);
            assertSingleValue(connection,
                    "SELECT type FROM attendance_flow_ledgers WHERE flow_id = ? AND ledger_id = ? AND action = 'ATTENDANCE_CONTRIBUTION_LEDGER_CREATED'",
                    FLOW_ID, ledgerId, "CONTRIBUTION_REWARD");
            assertSingleValue(connection,
                    "SELECT score_balance FROM attendance_flow_accounts WHERE flow_id = ? AND account_id = ? AND action = 'ATTENDANCE_CONTRIBUTION_APPLIED'",
                    FLOW_ID, accountId, 130);
            assertSingleValue(connection,
                    "SELECT response_code FROM attendance_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/attendance/admin/contributions'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: attendance contribution reached backend, wrote contribution/ledger/account/request rows, and returned 201.");
    }

    @Test
    void monthlyRunRunsThroughBackendAndDatabaseThenReturnsCompletedRun() throws Exception {
        String accountId = initialize("wl-app-1", "setup-monthly");
        String requestId = "req-att-monthly-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/attendance/admin/monthly-runs",
                bearerHeaders("admin-token", requestId),
                Map.of("cycleKey", "2026-05", "reason", "月度扣分", "confirmText", "RUN_MONTHLY_DEDUCTION", "idempotencyKey", "monthly-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/status").asText()).isEqualTo("COMPLETED");
        assertThat(json.at("/data/deductedAccounts").asInt()).isEqualTo(1);
        String runId = json.at("/data/runId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM attendance_flow_monthly_runs WHERE flow_id = ? AND run_id = ? AND action = 'ATTENDANCE_MONTHLY_RUN_EXECUTED'",
                    FLOW_ID, runId, "COMPLETED");
            assertSingleValue(connection,
                    "SELECT score_balance FROM attendance_flow_accounts WHERE flow_id = ? AND account_id = ? AND action = 'ATTENDANCE_MONTHLY_DEDUCTION_APPLIED'",
                    FLOW_ID, accountId, 80);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM attendance_flow_ledgers WHERE flow_id = ? AND source_id = ? AND action = 'ATTENDANCE_MONTHLY_LEDGER_CREATED'",
                    FLOW_ID, runId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM attendance_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/attendance/admin/monthly-runs'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: attendance monthly run reached backend, wrote run/ledger/account/request rows, and returned 201.");
    }

    private String initialize(String applicationId, String purpose) throws Exception {
        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/attendance/admin/initializations",
                bearerHeaders("admin-token", "req-setup-" + purpose + "-" + FLOW_ID),
                initBody(applicationId, purpose + "-" + randomKey())
        );
        assertThat(response.statusCode()).isEqualTo(201);
        return objectMapper.readTree(response.body()).at("/data/account/accountId").asText();
    }

    private TestHttpResponse exchange(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method.name(), HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
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

    private Map<String, Object> initBody(String applicationId, String idempotencyKey) {
        return Map.of("applicationId", applicationId, "idempotencyKey", idempotencyKey, "reason", "符合白名单初始化");
    }

    private Map<String, Object> adjustBody(int delta, String idempotencyKey) {
        return Map.of("delta", delta, "publicReason", delta >= 0 ? "贡献加分" : "考勤扣分", "reason", "后台积分调整", "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> contributionBody(String accountId, int scoreDelta, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accountId", accountId);
        body.put("type", "PROJECT_BUILD");
        body.put("sourceModule", "manual");
        body.put("sourceId", idempotencyKey);
        body.put("title", "工程贡献");
        body.put("description", "参与工程建设");
        body.put("occurredAt", "2026-05-23T10:00:00Z");
        body.put("scoreDelta", scoreDelta);
        body.put("publicReason", "工程贡献加分");
        body.put("reason", "后台记录贡献");
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
                CREATE TABLE IF NOT EXISTS attendance_flow_accounts (
                    flow_id VARCHAR(128) NOT NULL,
                    account_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    user_id VARCHAR(128) NOT NULL,
                    member_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    score_balance INT NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS attendance_flow_ledgers (
                    flow_id VARCHAR(128) NOT NULL,
                    ledger_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    account_id VARCHAR(128) NOT NULL,
                    type VARCHAR(64) NOT NULL,
                    delta INT NOT NULL,
                    balance_after INT NOT NULL,
                    source_id VARCHAR(128),
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS attendance_flow_contributions (
                    flow_id VARCHAR(128) NOT NULL,
                    contribution_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    account_id VARCHAR(128) NOT NULL,
                    score_delta INT NOT NULL,
                    ledger_id VARCHAR(128),
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS attendance_flow_monthly_runs (
                    flow_id VARCHAR(128) NOT NULL,
                    run_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    cycle_key VARCHAR(16) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    deducted_accounts INT NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS attendance_flow_candidates (
                    flow_id VARCHAR(128) NOT NULL,
                    candidate_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    account_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    score_balance INT NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS attendance_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS attendance_flow_request_log (
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
        AttendanceFlowEvidenceRecorder attendanceFlowEvidenceRecorder() {
            return new JdbcAttendanceFlowEvidenceRecorder();
        }
    }

    static class JdbcAttendanceFlowEvidenceRecorder implements AttendanceFlowEvidenceRecorder {
        @Override
        public void recordInitializationWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            Map<?, ?> account = map(payload.get("account"));
            Map<?, ?> ledger = map(payload.get("ledger"));
            try (Connection connection = openConnection()) {
                insertAccount(connection, flowId, action, account);
                insertLedger(connection, flowId, "ATTENDANCE_INITIAL_LEDGER_CREATED", ledger);
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(account.get("accountId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write attendance initialization database evidence", exception);
            }
        }

        @Override
        public void recordAdjustmentWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            Map<?, ?> account = map(payload.get("account"));
            Map<?, ?> ledger = map(payload.get("ledger"));
            try (Connection connection = openConnection()) {
                insertAccount(connection, flowId, action, account);
                insertLedger(connection, flowId, "ATTENDANCE_ADJUSTMENT_LEDGER_CREATED", ledger);
                Object candidateValue = payload.get("candidate");
                if (candidateValue instanceof Map<?, ?> candidate) {
                    insertCandidate(connection, flowId, "ATTENDANCE_REMOVAL_CANDIDATE_CREATED", candidate);
                }
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(account.get("accountId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write attendance adjustment database evidence", exception);
            }
        }

        @Override
        public void recordContributionWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            Map<?, ?> account = map(payload.get("account"));
            Map<?, ?> contribution = map(payload.get("contribution"));
            try (Connection connection = openConnection()) {
                insertContribution(connection, flowId, action, contribution);
                Object ledgerValue = payload.get("ledger");
                if (ledgerValue instanceof Map<?, ?> ledger) {
                    insertLedger(connection, flowId, "ATTENDANCE_CONTRIBUTION_LEDGER_CREATED", ledger);
                }
                insertAccount(connection, flowId, "ATTENDANCE_CONTRIBUTION_APPLIED", account);
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(contribution.get("contributionId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write attendance contribution database evidence", exception);
            }
        }

        @Override
        public void recordMonthlyRunWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String runId = String.valueOf(payload.get("runId"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO attendance_flow_monthly_runs(flow_id, run_id, action, cycle_key, status, deducted_accounts, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, runId, action, payload.get("cycleKey"), payload.get("status"), payload.get("deductedAccounts"), Timestamp.from(Instant.now()));
                Object itemsValue = payload.get("previewItems");
                if (itemsValue instanceof List<?> items) {
                    for (Object itemValue : items) {
                        if (!(itemValue instanceof Map<?, ?> item) || !Boolean.TRUE.equals(item.get("deduct"))) {
                            continue;
                        }
                        insert(connection,
                                "INSERT INTO attendance_flow_accounts(flow_id, account_id, action, user_id, member_id, status, score_balance, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                                flowId, item.get("accountId"), "ATTENDANCE_MONTHLY_DEDUCTION_APPLIED", "UNKNOWN", "UNKNOWN", "ACTIVE", item.get("balanceAfter"), Timestamp.from(Instant.now()));
                        insert(connection,
                                "INSERT INTO attendance_flow_ledgers(flow_id, ledger_id, action, account_id, type, delta, balance_after, source_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                flowId, "monthly-ledger-" + runId + "-" + item.get("accountId"), "ATTENDANCE_MONTHLY_LEDGER_CREATED", item.get("accountId"), "MONTHLY_DEDUCTION", -intValue(payload.get("deductionScore")), item.get("balanceAfter"), runId, Timestamp.from(Instant.now()));
                    }
                }
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, runId, request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write attendance monthly run database evidence", exception);
            }
        }

        private static void insertAccount(Connection connection, String flowId, String action, Map<?, ?> account) throws Exception {
            insert(connection,
                    "INSERT INTO attendance_flow_accounts(flow_id, account_id, action, user_id, member_id, status, score_balance, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    flowId, account.get("accountId"), action, account.get("userId"), account.get("memberId"), account.get("status"), account.get("scoreBalance"), Timestamp.from(Instant.now()));
        }

        private static void insertLedger(Connection connection, String flowId, String action, Map<?, ?> ledger) throws Exception {
            insert(connection,
                    "INSERT INTO attendance_flow_ledgers(flow_id, ledger_id, action, account_id, type, delta, balance_after, source_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    flowId, ledger.get("ledgerId"), action, ledger.get("accountId"), ledger.get("type"), ledger.get("delta"), ledger.get("balanceAfter"), ledger.get("sourceId"), Timestamp.from(Instant.now()));
        }

        private static void insertContribution(Connection connection, String flowId, String action, Map<?, ?> contribution) throws Exception {
            insert(connection,
                    "INSERT INTO attendance_flow_contributions(flow_id, contribution_id, action, account_id, score_delta, ledger_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    flowId, contribution.get("contributionId"), action, contribution.get("accountId"), contribution.get("scoreDelta"), contribution.get("ledgerId"), Timestamp.from(Instant.now()));
        }

        private static void insertCandidate(Connection connection, String flowId, String action, Map<?, ?> candidate) throws Exception {
            insert(connection,
                    "INSERT INTO attendance_flow_candidates(flow_id, candidate_id, action, account_id, status, score_balance, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    flowId, candidate.get("candidateId"), action, candidate.get("accountId"), candidate.get("status"), candidate.get("scoreBalance"), Timestamp.from(Instant.now()));
        }

        private static void insertAuditAndRequest(Connection connection, String flowId, String requestId, String action, String targetId, String path, int responseCode) throws Exception {
            insert(connection,
                    "INSERT INTO attendance_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, requestId, action, targetId, "SUCCESS", Timestamp.from(Instant.now()));
            insert(connection,
                    "INSERT INTO attendance_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
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

        private static Map<?, ?> map(Object value) {
            if (value instanceof Map<?, ?> map) {
                return map;
            }
            throw new IllegalArgumentException("expected map payload");
        }

        private static int intValue(Object value) {
            if (value instanceof Number number) {
                return number.intValue();
            }
            return Integer.parseInt(String.valueOf(value));
        }
    }

    record TestHttpResponse(int statusCode, String body) {
    }
}
