package cn.beiming.activity;

import cn.beiming.engagement.EngagementCoreServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
        classes = EngagementCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "activity.test-controls.enabled=true"
)
@Import(ActivityRequestDatabaseFlowIntegrationTest.EvidenceConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ActivityRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "activity-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:activity_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
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
                    "activity_flow_request_log",
                    "activity_flow_events",
                    "activity_flow_registrations",
                    "activity_flow_results",
                    "activity_flow_rewards",
                    "activity_flow_candidates",
                    "activity_flow_audits"
            )) {
                deleteFlowRows(statement, table);
            }
        }
    }

    @Test
    void createEventRunsThroughBackendAndDatabaseThenReturnsDraftEvent() throws Exception {
        String requestId = "req-act-event-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/activity/admin/events",
                bearerHeaders("admin-token", requestId),
                eventBody("event-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(json.at("/data/createdBy").asText()).isEqualTo("admin-user");
        String activityId = json.at("/data/activityId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM activity_flow_events WHERE flow_id = ? AND activity_id = ? AND action = 'ACTIVITY_CREATED'",
                    FLOW_ID, activityId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT created_by FROM activity_flow_events WHERE flow_id = ? AND activity_id = ? AND action = 'ACTIVITY_CREATED'",
                    FLOW_ID, activityId, "admin-user");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM activity_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'ACTIVITY_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM activity_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/activity/admin/events'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: activity event create reached backend, wrote event/audit/request rows, and returned 201.");
    }

    @Test
    void registrationRunsThroughBackendAndDatabaseThenReturnsConfirmedRegistration() throws Exception {
        String activityId = createApprovedPublishedEvent("setup-registration");
        String requestId = "req-act-registration-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/activity/me/events/" + activityId + "/registrations",
                bearerHeaders("member-user-1-token", requestId, Map.of("X-Test-Now", "2026-05-25T12:00:00Z")),
                registerBody("registration-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/activityId").asText()).isEqualTo(activityId);
        assertThat(json.at("/data/status").asText()).isEqualTo("CONFIRMED");
        assertThat(json.at("/data/participant/userId").asText()).isEqualTo("member-user-1");
        String registrationId = json.at("/data/registrationId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM activity_flow_registrations WHERE flow_id = ? AND registration_id = ? AND action = 'ACTIVITY_REGISTERED'",
                    FLOW_ID, registrationId, "CONFIRMED");
            assertSingleValue(connection,
                    "SELECT activity_id FROM activity_flow_registrations WHERE flow_id = ? AND registration_id = ? AND action = 'ACTIVITY_REGISTERED'",
                    FLOW_ID, registrationId, activityId);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM activity_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'ACTIVITY_REGISTERED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM activity_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, requestId, "/api/v1/activity/me/events/" + activityId + "/registrations", 201);
        }
        System.out.println("SQL evidence: activity registration reached backend, wrote registration/audit/request rows, and returned 201.");
    }

    @Test
    void checkInAndResultRunThroughBackendAndDatabaseThenReturnPublishedResult() throws Exception {
        String activityId = createApprovedPublishedEvent("setup-result");
        String registrationId = register(activityId, "setup-result-registration");
        startEvent(activityId);
        String checkInRequestId = "req-act-checkin-" + FLOW_ID;

        TestHttpResponse checkIn = exchange(
                HttpMethod.PATCH,
                "/api/v1/activity/admin/registrations/" + registrationId + "/check-in",
                bearerHeaders("helper-token", checkInRequestId, Map.of("X-Test-Now", "2026-06-01T12:30:00Z")),
                Map.of("method", "MANUAL", "reason", "现场签到", "idempotencyKey", "checkin-" + randomKey())
        );

        assertThat(checkIn.statusCode()).isEqualTo(200);
        JsonNode checkInJson = objectMapper.readTree(checkIn.body());
        assertThat(checkInJson.at("/data/status").asText()).isEqualTo("CHECKED_IN");

        completeEvent(activityId);
        String resultRequestId = "req-act-result-" + FLOW_ID;
        TestHttpResponse result = exchange(
                HttpMethod.PUT,
                "/api/v1/activity/admin/events/" + activityId + "/result",
                bearerHeaders("helper-token", resultRequestId),
                Map.of("title", "数据库流活动结果", "summary", "活动完成", "details", "流程证据完整", "reason", "录入结果", "idempotencyKey", "result-" + randomKey())
        );

        assertThat(result.statusCode()).isEqualTo(200);
        JsonNode resultJson = objectMapper.readTree(result.body());
        assertThat(resultJson.at("/data/status").asText()).isEqualTo("DRAFT");
        String resultId = resultJson.at("/data/resultId").asText();

        String publishRequestId = "req-act-result-publish-" + FLOW_ID;
        TestHttpResponse published = exchange(
                HttpMethod.PATCH,
                "/api/v1/activity/admin/events/" + activityId + "/result/publish",
                bearerHeaders("admin-token", publishRequestId),
                Map.of("reason", "发布结果", "idempotencyKey", "publish-result-" + randomKey())
        );

        assertThat(published.statusCode()).isEqualTo(200);
        JsonNode publishedJson = objectMapper.readTree(published.body());
        assertThat(publishedJson.at("/data/status").asText()).isEqualTo("PUBLISHED");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM activity_flow_registrations WHERE flow_id = ? AND registration_id = ? AND action = 'ACTIVITY_REGISTRATION_CHECKED_IN'",
                    FLOW_ID, registrationId, "CHECKED_IN");
            assertSingleValue(connection,
                    "SELECT status FROM activity_flow_results WHERE flow_id = ? AND result_id = ? AND action = 'ACTIVITY_RESULT_UPSERTED'",
                    FLOW_ID, resultId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT status FROM activity_flow_results WHERE flow_id = ? AND result_id = ? AND action = 'ACTIVITY_RESULT_PUBLISHED'",
                    FLOW_ID, resultId, "PUBLISHED");
            assertSingleValue(connection,
                    "SELECT response_code FROM activity_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, publishRequestId, "/api/v1/activity/admin/events/" + activityId + "/result/publish", 200);
        }
        System.out.println("SQL evidence: activity check-in and result reached backend, wrote registration/result/request rows, and returned 200.");
    }

    @Test
    void rewardAndContributionCandidateRunThroughBackendAndDatabaseThenReturnCandidate() throws Exception {
        String activityId = createActivityWithCheckedInRegistration("setup-reward");
        publishResult(activityId, "setup-reward-result");
        String registrationId = firstRegistration(activityId);
        String rewardRequestId = "req-act-reward-" + FLOW_ID;

        TestHttpResponse reward = exchange(
                HttpMethod.POST,
                "/api/v1/activity/admin/events/" + activityId + "/rewards",
                bearerHeaders("helper-token", rewardRequestId),
                rewardBody(registrationId, "reward-" + randomKey())
        );

        assertThat(reward.statusCode()).isEqualTo(201);
        JsonNode rewardJson = objectMapper.readTree(reward.body());
        assertThat(rewardJson.at("/data/status").asText()).isEqualTo("PENDING_ISSUE");
        assertThat(rewardJson.at("/data/registrationId").asText()).isEqualTo(registrationId);
        String rewardId = rewardJson.at("/data/rewardId").asText();

        String issueRequestId = "req-act-reward-issue-" + FLOW_ID;
        TestHttpResponse issued = exchange(
                HttpMethod.PATCH,
                "/api/v1/activity/admin/rewards/" + rewardId + "/issue",
                bearerHeaders("helper-token", issueRequestId),
                Map.of("publicComment", "奖励已登记", "reason", "发放奖励", "idempotencyKey", "issue-" + randomKey())
        );
        assertThat(issued.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(issued.body()).at("/data/status").asText()).isEqualTo("ISSUED");

        String candidateRequestId = "req-act-candidate-" + FLOW_ID;
        TestHttpResponse candidate = exchange(
                HttpMethod.POST,
                "/api/v1/activity/admin/events/" + activityId + "/contribution-candidates",
                bearerHeaders("admin-token", candidateRequestId),
                Map.of("reason", "生成贡献候选", "idempotencyKey", "candidate-" + randomKey())
        );
        assertThat(candidate.statusCode()).isEqualTo(201);
        JsonNode candidateJson = objectMapper.readTree(candidate.body());
        assertThat(candidateJson.at("/data/total").asInt()).isEqualTo(1);
        String candidateId = candidateJson.at("/data/items/0/candidateId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM activity_flow_rewards WHERE flow_id = ? AND reward_id = ? AND action = 'ACTIVITY_REWARD_CREATED'",
                    FLOW_ID, rewardId, "PENDING_ISSUE");
            assertSingleValue(connection,
                    "SELECT status FROM activity_flow_rewards WHERE flow_id = ? AND reward_id = ? AND action = 'ACTIVITY_REWARD_ISSUED'",
                    FLOW_ID, rewardId, "ISSUED");
            assertSingleValue(connection,
                    "SELECT reward_id FROM activity_flow_candidates WHERE flow_id = ? AND candidate_id = ? AND action = 'ACTIVITY_CONTRIBUTION_CANDIDATES_CREATED'",
                    FLOW_ID, candidateId, rewardId);
            assertSingleValue(connection,
                    "SELECT response_code FROM activity_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, candidateRequestId, "/api/v1/activity/admin/events/" + activityId + "/contribution-candidates", 201);
        }
        System.out.println("SQL evidence: activity reward and contribution candidate reached backend, wrote reward/candidate/request rows, and returned 201.");
    }

    private String createApprovedPublishedEvent(String purpose) throws Exception {
        TestHttpResponse created = exchange(
                HttpMethod.POST,
                "/api/v1/activity/admin/events",
                bearerHeaders("admin-token", "req-setup-event-" + purpose + "-" + FLOW_ID),
                eventBody(purpose + "-" + randomKey())
        );
        assertThat(created.statusCode()).isEqualTo(201);
        String activityId = objectMapper.readTree(created.body()).at("/data/activityId").asText();
        exchangeOk(HttpMethod.POST, "/api/v1/activity/admin/events/" + activityId + "/submit", "helper-token", "req-setup-submit-" + purpose, Map.of("reason", "提交审核", "idempotencyKey", purpose + "-submit-" + randomKey()));
        exchangeOk(HttpMethod.PATCH, "/api/v1/activity/admin/events/" + activityId + "/approve", "helper-token", "req-setup-approve-" + purpose, reviewBody(purpose + "-approve-" + randomKey()));
        exchangeOk(HttpMethod.PATCH, "/api/v1/activity/admin/events/" + activityId + "/publish", "admin-token", "req-setup-publish-" + purpose, Map.of("reason", "发布活动", "idempotencyKey", purpose + "-publish-" + randomKey()));
        return activityId;
    }

    private String createActivityWithCheckedInRegistration(String purpose) throws Exception {
        String activityId = createApprovedPublishedEvent(purpose);
        String registrationId = register(activityId, purpose + "-registration");
        startEvent(activityId);
        TestHttpResponse checkIn = exchange(
                HttpMethod.PATCH,
                "/api/v1/activity/admin/registrations/" + registrationId + "/check-in",
                bearerHeaders("helper-token", "req-setup-checkin-" + purpose + "-" + FLOW_ID, Map.of("X-Test-Now", "2026-06-01T12:30:00Z")),
                Map.of("method", "MANUAL", "reason", "现场签到", "idempotencyKey", purpose + "-checkin-" + randomKey())
        );
        assertThat(checkIn.statusCode()).isEqualTo(200);
        completeEvent(activityId);
        return activityId;
    }

    private String register(String activityId, String purpose) throws Exception {
        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/activity/me/events/" + activityId + "/registrations",
                bearerHeaders("member-user-1-token", "req-setup-register-" + purpose + "-" + FLOW_ID, Map.of("X-Test-Now", "2026-05-25T12:00:00Z")),
                registerBody(purpose + "-" + randomKey())
        );
        assertThat(response.statusCode()).isEqualTo(201);
        return objectMapper.readTree(response.body()).at("/data/registrationId").asText();
    }

    private String firstRegistration(String activityId) throws Exception {
        TestHttpResponse response = exchange(
                HttpMethod.GET,
                "/api/v1/activity/admin/events/" + activityId + "/registrations",
                bearerHeaders("helper-token", "req-setup-first-registration-" + FLOW_ID),
                null
        );
        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readTree(response.body()).at("/data/items/0/registrationId").asText();
    }

    private void startEvent(String activityId) throws Exception {
        exchangeOk(HttpMethod.PATCH, "/api/v1/activity/admin/events/" + activityId + "/start", "helper-token", "req-setup-start-" + activityId, Map.of("reason", "活动开始", "idempotencyKey", activityId + "-start-" + randomKey()));
    }

    private void completeEvent(String activityId) throws Exception {
        exchangeOk(HttpMethod.PATCH, "/api/v1/activity/admin/events/" + activityId + "/complete", "helper-token", "req-setup-complete-" + activityId, Map.of("reason", "活动完成", "idempotencyKey", activityId + "-complete-" + randomKey()));
    }

    private void publishResult(String activityId, String purpose) throws Exception {
        exchangeOk(HttpMethod.PUT, "/api/v1/activity/admin/events/" + activityId + "/result", "helper-token", "req-setup-result-" + purpose, Map.of("title", "活动结果", "summary", "活动完成", "details", "结果", "reason", "录入结果", "idempotencyKey", purpose + "-upsert-" + randomKey()));
        exchangeOk(HttpMethod.PATCH, "/api/v1/activity/admin/events/" + activityId + "/result/publish", "admin-token", "req-setup-result-publish-" + purpose, Map.of("reason", "发布结果", "idempotencyKey", purpose + "-publish-" + randomKey()));
    }

    private void exchangeOk(HttpMethod method, String path, String token, String requestId, Map<String, Object> body) throws Exception {
        TestHttpResponse response = exchange(method, path, bearerHeaders(token, requestId + "-" + FLOW_ID), body);
        assertThat(response.statusCode()).isEqualTo(200);
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

    private Map<String, Object> eventBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("slug", "activity-db-flow-" + idempotencyKey.toLowerCase());
        body.put("title", "数据库流活动");
        body.put("summary", "数据库流活动摘要");
        body.put("description", "活动描述覆盖真实 HTTP、后端处理和 SQL 证据。");
        body.put("type", "BUILD");
        body.put("visibility", "PUBLIC");
        body.put("registrationPolicy", "OPEN");
        body.put("startAt", "2026-06-01T12:00:00Z");
        body.put("endAt", "2026-06-01T14:00:00Z");
        body.put("registrationOpenAt", "2026-05-25T00:00:00Z");
        body.put("registrationCloseAt", "2026-06-01T11:00:00Z");
        body.put("capacity", 10);
        body.put("waitlistCapacity", 5);
        body.put("locationText", "北冥服务器");
        body.put("coverImageUrl", "/assets/activity-cover.png");
        body.put("tags", List.of("activity", "database-flow"));
        body.put("reason", "创建活动");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> registerBody(String idempotencyKey) {
        return Map.of("answers", Map.of("note", "我会准时参加"), "guestCount", 0, "note", "报名活动", "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> reviewBody(String idempotencyKey) {
        return Map.of("reviewComment", "审核通过", "reason", "符合活动规则", "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> rewardBody(String registrationId, String idempotencyKey) {
        return Map.of(
                "registrationId", registrationId,
                "type", "POINTS_CANDIDATE",
                "title", "活动贡献奖励",
                "description", "参与活动获得贡献候选",
                "quantity", 1,
                "scoreCandidateDelta", 10,
                "reason", "创建奖励",
                "idempotencyKey", idempotencyKey
        );
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
                CREATE TABLE IF NOT EXISTS activity_flow_events (
                    flow_id VARCHAR(128) NOT NULL,
                    activity_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    slug VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS activity_flow_registrations (
                    flow_id VARCHAR(128) NOT NULL,
                    registration_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    activity_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    user_id VARCHAR(128) NOT NULL,
                    member_id VARCHAR(128),
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS activity_flow_results (
                    flow_id VARCHAR(128) NOT NULL,
                    result_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    activity_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    title VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS activity_flow_rewards (
                    flow_id VARCHAR(128) NOT NULL,
                    reward_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    activity_id VARCHAR(128) NOT NULL,
                    registration_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    user_id VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS activity_flow_candidates (
                    flow_id VARCHAR(128) NOT NULL,
                    candidate_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    activity_id VARCHAR(128) NOT NULL,
                    reward_id VARCHAR(128) NOT NULL,
                    user_id VARCHAR(128) NOT NULL,
                    score_delta INT NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS activity_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS activity_flow_request_log (
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

    static class EvidenceConfiguration {
        @Bean
        ActivityFlowEvidenceRecorder activityFlowEvidenceRecorder() {
            return new JdbcActivityFlowEvidenceRecorder();
        }
    }

    static class JdbcActivityFlowEvidenceRecorder implements ActivityFlowEvidenceRecorder {
        @Override
        public void recordEventWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO activity_flow_events(flow_id, activity_id, action, slug, status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("activityId"), action, payload.get("slug"), payload.get("status"), payload.get("createdBy"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("activityId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write activity event database evidence", exception);
            }
        }

        @Override
        public void recordRegistrationWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            Map<?, ?> participant = map(payload.get("participant"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO activity_flow_registrations(flow_id, registration_id, action, activity_id, status, user_id, member_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("registrationId"), action, payload.get("activityId"), payload.get("status"), participant.get("userId"), participant.get("memberId"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("registrationId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write activity registration database evidence", exception);
            }
        }

        @Override
        public void recordResultWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO activity_flow_results(flow_id, result_id, action, activity_id, status, title, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("resultId"), action, payload.get("activityId"), payload.get("status"), payload.get("title"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("resultId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write activity result database evidence", exception);
            }
        }

        @Override
        public void recordRewardWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            Map<?, ?> recipient = map(payload.get("recipient"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO activity_flow_rewards(flow_id, reward_id, action, activity_id, registration_id, status, user_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("rewardId"), action, payload.get("activityId"), payload.get("registrationId"), payload.get("status"), recipient.get("userId"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("rewardId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write activity reward database evidence", exception);
            }
        }

        @Override
        public void recordCandidateWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            Object itemsValue = payload.get("items");
            try (Connection connection = openConnection()) {
                if (itemsValue instanceof List<?> items) {
                    for (Object itemValue : items) {
                        if (itemValue instanceof Map<?, ?> item) {
                            insert(connection,
                                    "INSERT INTO activity_flow_candidates(flow_id, candidate_id, action, activity_id, reward_id, user_id, score_delta, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                                    flowId, item.get("candidateId"), action, item.get("activityId"), item.get("rewardId"), item.get("userId"), item.get("scoreDelta"), Timestamp.from(Instant.now()));
                        }
                    }
                }
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("activityId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write activity candidate database evidence", exception);
            }
        }

        private static void insertAuditAndRequest(Connection connection, String flowId, String requestId, String action, String targetId, String path, int responseCode) throws Exception {
            insert(connection,
                    "INSERT INTO activity_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, requestId, action, targetId, "SUCCESS", Timestamp.from(Instant.now()));
            insert(connection,
                    "INSERT INTO activity_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
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
    }

    record TestHttpResponse(int statusCode, String body) {
    }
}
