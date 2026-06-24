package cn.beiming.attendance;

import cn.beiming.admission.AdmissionCoreServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = AdmissionCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=",
                "spring.flyway.enabled=true",
                "attendance.test-controls.enabled=false"
        }
)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AttendancePostgreSqlPersistenceIntegrationTest {
    private static final String FLOW_ID = "attendance-pg-" + UUID.randomUUID();

    static {
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_attendance_pg")
            .withUsername("beiming")
            .withPassword("beiming");

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    Environment environment;

    @BeforeEach
    void setUp() throws Exception {
        assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("true");
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM attendance_leaderboard_snapshots");
            statement.executeUpdate("DELETE FROM attendance_removal_candidates");
            statement.executeUpdate("DELETE FROM attendance_monthly_runs");
            statement.executeUpdate("DELETE FROM attendance_contributions");
            statement.executeUpdate("DELETE FROM attendance_ledgers");
            statement.executeUpdate("DELETE FROM attendance_accounts");
            statement.executeUpdate("DELETE FROM app_idempotency_records WHERE scope LIKE 'attendance.%'");
            statement.executeUpdate("DELETE FROM app_audit_logs WHERE target_type LIKE 'ATTENDANCE%'");
            statement.executeUpdate("DELETE FROM app_request_logs WHERE path LIKE '/api/v1/attendance%'");
        }
    }

    @Test
    void initializationPersistsAccountInitialLedgerAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String requestId = requestId("initialize");
        JsonNode initialized = exchange(HttpMethod.POST, "/api/v1/attendance/admin/initializations", bearerHeaders("admin-token", requestId), initBody("wl-app-1", "init-" + randomKey()));

        assertThat(initialized.at("/code").asInt()).isZero();
        assertThat(initialized.at("/data/account/status").asText()).isEqualTo("ACTIVE");
        String accountId = initialized.at("/data/account/accountId").asText();
        String ledgerId = initialized.at("/data/ledger/ledgerId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM attendance_accounts WHERE account_id = ?", accountId, "ACTIVE");
            assertSingleValue(connection, "SELECT type FROM attendance_ledgers WHERE ledger_id = ?", ledgerId, "INITIAL_GRANT");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'ATTENDANCE_INITIALIZED' AND target_id = ?", requestId, accountId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'attendance.initialization.create'", 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/attendance/admin/initializations'", requestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL attendance initialization wrote attendance_accounts/attendance_ledgers/app_audit_logs/app_idempotency_records/app_request_logs.");
    }

    @Test
    void adjustmentPersistsLedgerCandidateAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String accountId = initialize("wl-app-1");
        String requestId = requestId("adjust");
        JsonNode adjusted = exchange(HttpMethod.POST, "/api/v1/attendance/admin/accounts/" + accountId + "/adjustments", bearerHeaders("admin-token", requestId), adjustBody(-100, "adjust-" + randomKey()));

        assertThat(adjusted.at("/code").asInt()).isZero();
        assertThat(adjusted.at("/data/account/status").asText()).isEqualTo("REMOVAL_CANDIDATE");
        String ledgerId = adjusted.at("/data/ledger/ledgerId").asText();
        String candidateId = adjusted.at("/data/candidate/candidateId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT score_balance FROM attendance_accounts WHERE account_id = ?", accountId, 0);
            assertSingleValue(connection, "SELECT delta FROM attendance_ledgers WHERE ledger_id = ?", ledgerId, -100);
            assertSingleValue(connection, "SELECT status FROM attendance_removal_candidates WHERE candidate_id = ?", candidateId, "OPEN");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'ATTENDANCE_SCORE_ADJUSTED' AND target_id = ?", requestId, accountId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'attendance.adjustment.create'", 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = ?", requestId, "/api/v1/attendance/admin/accounts/" + accountId + "/adjustments", 200);
        }
        System.out.println("SQL evidence: PostgreSQL attendance adjustment wrote account/ledger/removal-candidate/audit/idempotency/request rows.");
    }

    @Test
    void contributionAndCorrectionPersistContributionLedgerAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String accountId = initialize("wl-app-1");
        String createRequestId = requestId("contribution");
        JsonNode created = exchange(HttpMethod.POST, "/api/v1/attendance/admin/contributions", bearerHeaders("admin-token", createRequestId), contributionBody(accountId, 30, "contrib-" + randomKey()));

        assertThat(created.at("/code").asInt()).isZero();
        String contributionId = created.at("/data/contribution/contributionId").asText();
        String ledgerId = created.at("/data/ledger/ledgerId").asText();

        String correctRequestId = requestId("correct");
        JsonNode corrected = exchange(HttpMethod.PATCH, "/api/v1/attendance/admin/contributions/" + contributionId, bearerHeaders("admin-token", correctRequestId),
                Map.of("idempotencyKey", "correct-" + randomKey(), "title", "修正贡献记录", "publicReason", "修正说明", "reason", "后台修正"));
        assertThat(corrected.at("/data/title").asText()).isEqualTo("修正贡献记录");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT score_delta FROM attendance_contributions WHERE contribution_id = ?", contributionId, 30);
            assertSingleValue(connection, "SELECT type FROM attendance_ledgers WHERE ledger_id = ?", ledgerId, "CONTRIBUTION_REWARD");
            assertSingleValue(connection, "SELECT title FROM attendance_contributions WHERE contribution_id = ?", contributionId, "修正贡献记录");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'ATTENDANCE_CONTRIBUTION_CREATED' AND target_id = ?", createRequestId, contributionId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'ATTENDANCE_CONTRIBUTION_CORRECTED' AND target_id = ?", correctRequestId, contributionId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/attendance/admin/contributions'", createRequestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL attendance contribution and correction wrote contribution/ledger/audit/idempotency/request rows.");
    }

    @Test
    void monthlyRunPersistsRunDeductionLedgerAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String accountId = initialize("wl-app-1");
        String requestId = requestId("monthly");
        JsonNode run = exchange(HttpMethod.POST, "/api/v1/attendance/admin/monthly-runs", bearerHeaders("admin-token", requestId),
                Map.of("cycleKey", "2026-05", "reason", "月度扣分", "confirmText", "RUN_MONTHLY_DEDUCTION", "idempotencyKey", "monthly-" + randomKey()));

        assertThat(run.at("/code").asInt()).isZero();
        String runId = run.at("/data/runId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM attendance_monthly_runs WHERE run_id = ?", runId, "COMPLETED");
            assertSingleValue(connection, "SELECT score_balance FROM attendance_accounts WHERE account_id = ?", accountId, 80);
            assertSingleValue(connection, "SELECT COUNT(*) FROM attendance_ledgers WHERE source_id = ? AND type = 'MONTHLY_DEDUCTION'", runId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'ATTENDANCE_MONTHLY_RUN_EXECUTED' AND target_id = ?", requestId, runId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'attendance.monthly-run.execute'", 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/attendance/admin/monthly-runs'", requestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL attendance monthly run wrote monthly_run/account/ledger/audit/idempotency/request rows.");
    }

    @Test
    void candidateConfirmAndIdempotencyConflictKeepPostgreSqlRowsConsistent() throws Exception {
        String accountId = initialize("wl-app-1");
        JsonNode adjusted = exchange(HttpMethod.POST, "/api/v1/attendance/admin/accounts/" + accountId + "/adjustments", bearerHeaders("admin-token", requestId("candidate-seed")), adjustBody(-100, "candidate-seed-" + randomKey()));
        String candidateId = adjusted.at("/data/candidate/candidateId").asText();

        String requestId = requestId("confirm");
        JsonNode confirmed = exchange(HttpMethod.PATCH, "/api/v1/attendance/admin/removal-candidates/" + candidateId + "/confirm", bearerHeaders("admin-token", requestId),
                Map.of("idempotencyKey", "confirm-" + FLOW_ID, "publicReason", "进入复核", "reason", "积分归零", "confirmText", "CONFIRM_REMOVAL_CANDIDATE"));
        assertThat(confirmed.at("/data/candidate/status").asText()).isEqualTo("CONFIRMED");

        JsonNode conflict = exchange(HttpMethod.PATCH, "/api/v1/attendance/admin/removal-candidates/" + candidateId + "/confirm", bearerHeaders("admin-token", requestId("confirm-conflict")),
                Map.of("idempotencyKey", "confirm-" + FLOW_ID, "publicReason", "不同原因", "reason", "不同指纹", "confirmText", "CONFIRM_REMOVAL_CANDIDATE"));
        assertThat(conflict.at("/code").asInt()).isEqualTo(45017);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM attendance_removal_candidates WHERE candidate_id = ?", candidateId, "CONFIRMED");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'ATTENDANCE_REMOVAL_CANDIDATE_CONFIRMED' AND target_id = ?", requestId, candidateId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'attendance.candidate.confirm'", 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_request_logs WHERE request_id = ?", requestId("confirm-conflict"), 0L);
        }
        System.out.println("SQL evidence: PostgreSQL attendance candidate confirm wrote candidate/audit/idempotency/request rows and idempotency conflict did not add request rows.");
    }

    @Test
    void authenticationPermissionValidationAndMissingResourcesRemainHttpBoundaries() throws Exception {
        assertThat(exchange(HttpMethod.GET, "/api/v1/attendance/me/account", new HttpHeaders(), null).at("/code").asInt()).isEqualTo(41000);
        assertThat(exchange(HttpMethod.GET, "/api/v1/attendance/admin/accounts", bearerHeaders("user-token", requestId("forbidden")), null).at("/code").asInt()).isEqualTo(42001);
        assertThat(exchange(HttpMethod.POST, "/api/v1/attendance/admin/initializations", bearerHeaders("admin-token", requestId("validation")), Map.of("applicationId", "wl-app-1", "idempotencyKey", "short")).at("/code").asInt()).isEqualTo(40001);
        assertThat(exchange(HttpMethod.GET, "/api/v1/attendance/admin/accounts/missing", bearerHeaders("admin-token", requestId("missing")), null).at("/code").asInt()).isEqualTo(45000);
        assertThat(exchange(HttpMethod.POST, "/api/v1/attendance/admin/initializations", bearerHeaders("admin-token", requestId("dependency")), initBody("wl-bad-status", "dep-" + randomKey())).at("/code").asInt()).isEqualTo(45010);
        System.out.println("SQL evidence: PostgreSQL attendance HTTP boundary test covered auth failure, permission denial, validation, missing resource, and dependency status conflict.");
    }

    private String initialize(String applicationId) throws Exception {
        JsonNode response = exchange(HttpMethod.POST, "/api/v1/attendance/admin/initializations", bearerHeaders("admin-token", requestId("setup-" + applicationId)), initBody(applicationId, "setup-" + randomKey()));
        assertThat(response.at("/code").asInt()).isZero();
        return response.at("/data/account/accountId").asText();
    }

    private JsonNode exchange(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).method(method.name(), publisher);
        headers.forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
        HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body());
    }

    private HttpHeaders bearerHeaders(String token, String requestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("X-Request-Id", requestId);
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

    private String requestId(String prefix) {
        return "req-" + prefix + "-" + FLOW_ID;
    }

    private static String randomKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void assertSingleValue(Connection connection, String sql, Object expected) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            assertThat(result.next()).as(sql).isTrue();
            assertThat(result.getObject(1)).isEqualTo(expected);
            assertThat(result.next()).as(sql + " must return one row").isFalse();
        }
    }

    private static void assertSingleValue(Connection connection, String sql, Object first, Object expected) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, first);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).as(sql).isTrue();
                assertThat(result.getObject(1)).isEqualTo(expected);
                assertThat(result.next()).as(sql + " must return one row").isFalse();
            }
        }
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
}
