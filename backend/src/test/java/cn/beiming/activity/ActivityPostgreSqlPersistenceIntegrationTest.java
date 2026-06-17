package cn.beiming.activity;

import cn.beiming.engagement.EngagementCoreServiceApplication;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = EngagementCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=",
                "spring.flyway.enabled=true",
                "activity.test-controls.enabled=true"
        }
)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ActivityPostgreSqlPersistenceIntegrationTest {
    private static final String FLOW_ID = "activity-pg-" + UUID.randomUUID();

    static {
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_activity_pg")
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
            statement.executeUpdate("DELETE FROM activity_contribution_candidates");
            statement.executeUpdate("DELETE FROM activity_rewards");
            statement.executeUpdate("DELETE FROM activity_results");
            statement.executeUpdate("DELETE FROM activity_registrations");
            statement.executeUpdate("DELETE FROM activity_events");
            statement.executeUpdate("DELETE FROM app_idempotency_records WHERE scope LIKE 'activity.%'");
            statement.executeUpdate("DELETE FROM app_audit_logs WHERE target_type LIKE 'ACTIVITY%'");
            statement.executeUpdate("DELETE FROM app_request_logs WHERE path LIKE '/api/v1/activity%'");
        }
    }

    @Test
    void eventLifecyclePersistsEventAuditIdempotencyAndRequestLogs() throws Exception {
        String createRequestId = requestId("event-create");
        JsonNode created = exchange(HttpMethod.POST, "/api/v1/activity/admin/events", bearerHeaders("admin-token", createRequestId), eventBody("event-" + randomKey()));
        assertThat(created.at("/code").asInt()).isZero();
        String activityId = created.at("/data/activityId").asText();

        exchange(HttpMethod.POST, "/api/v1/activity/admin/events/" + activityId + "/submit", bearerHeaders("helper-token", requestId("event-submit")), Map.of("reason", "提交审核", "idempotencyKey", "submit-" + randomKey()));
        exchange(HttpMethod.PATCH, "/api/v1/activity/admin/events/" + activityId + "/approve", bearerHeaders("helper-token", requestId("event-approve")), reviewBody("approve-" + randomKey()));
        exchange(HttpMethod.PATCH, "/api/v1/activity/admin/events/" + activityId + "/publish", bearerHeaders("admin-token", requestId("event-publish")), Map.of("reason", "发布活动", "idempotencyKey", "publish-" + randomKey()));

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM activity_events WHERE activity_id = ?", activityId, "REGISTRATION_OPEN");
            assertSingleValue(connection, "SELECT created_by FROM activity_events WHERE activity_id = ?", activityId, "admin-user");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_type = 'ACTIVITY_EVENT' AND target_id = ? AND action = 'ACTIVITY_PUBLISHED'", activityId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin-user' AND scope = 'activity.event.create'", 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/activity/admin/events'", createRequestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL activity event lifecycle wrote event/audit/idempotency/request rows.");
    }

    @Test
    void registrationCheckInResultRewardAndCandidatePersistBusinessRows() throws Exception {
        String activityId = createPublishedActivity("flow-" + randomKey());
        String registrationRequestId = requestId("registration");
        JsonNode registration = exchange(HttpMethod.POST, "/api/v1/activity/me/events/" + activityId + "/registrations",
                bearerHeaders("member-user-1-token", registrationRequestId, Map.of("X-Test-Now", "2026-05-25T12:00:00Z")), registerBody("registration-" + randomKey()));
        String registrationId = registration.at("/data/registrationId").asText();

        exchange(HttpMethod.PATCH, "/api/v1/activity/admin/events/" + activityId + "/start", bearerHeaders("helper-token", requestId("start")), Map.of("reason", "活动开始", "idempotencyKey", "start-" + randomKey()));
        exchange(HttpMethod.PATCH, "/api/v1/activity/admin/registrations/" + registrationId + "/check-in", bearerHeaders("helper-token", requestId("checkin"), Map.of("X-Test-Now", "2026-06-01T12:30:00Z")), Map.of("method", "MANUAL", "reason", "现场签到", "idempotencyKey", "checkin-" + randomKey()));
        exchange(HttpMethod.PATCH, "/api/v1/activity/admin/events/" + activityId + "/complete", bearerHeaders("helper-token", requestId("complete")), Map.of("reason", "活动完成", "idempotencyKey", "complete-" + randomKey()));

        JsonNode result = exchange(HttpMethod.PUT, "/api/v1/activity/admin/events/" + activityId + "/result", bearerHeaders("helper-token", requestId("result")), Map.of("title", "活动结果", "summary", "活动完成", "details", "流程证据完整", "reason", "录入结果", "idempotencyKey", "result-" + randomKey()));
        String resultId = result.at("/data/resultId").asText();
        exchange(HttpMethod.PATCH, "/api/v1/activity/admin/events/" + activityId + "/result/publish", bearerHeaders("admin-token", requestId("result-publish")), Map.of("reason", "发布结果", "idempotencyKey", "result-publish-" + randomKey()));

        JsonNode reward = exchange(HttpMethod.POST, "/api/v1/activity/admin/events/" + activityId + "/rewards", bearerHeaders("helper-token", requestId("reward")), rewardBody(registrationId, "reward-" + randomKey()));
        String rewardId = reward.at("/data/rewardId").asText();
        exchange(HttpMethod.PATCH, "/api/v1/activity/admin/rewards/" + rewardId + "/issue", bearerHeaders("helper-token", requestId("reward-issue")), Map.of("publicComment", "奖励已登记", "reason", "发放奖励", "idempotencyKey", "issue-" + randomKey()));
        JsonNode candidates = exchange(HttpMethod.POST, "/api/v1/activity/admin/events/" + activityId + "/contribution-candidates", bearerHeaders("admin-token", requestId("candidate")), Map.of("reason", "生成贡献候选", "idempotencyKey", "candidate-" + randomKey()));
        String candidateId = candidates.at("/data/items/0/candidateId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM activity_registrations WHERE registration_id = ?", registrationId, "CHECKED_IN");
            assertSingleValue(connection, "SELECT status FROM activity_results WHERE result_id = ?", resultId, "PUBLISHED");
            assertSingleValue(connection, "SELECT status FROM activity_rewards WHERE reward_id = ?", rewardId, "ISSUED");
            assertSingleValue(connection, "SELECT reward_id FROM activity_contribution_candidates WHERE candidate_id = ?", candidateId, rewardId);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'ACTIVITY_REGISTERED' AND target_id = ?", registrationRequestId, registrationId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'member-user-1' AND scope = 'activity.registration.create'", 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = ?", registrationRequestId, "/api/v1/activity/me/events/" + activityId + "/registrations", 201);
        }
        System.out.println("SQL evidence: PostgreSQL activity registration/check-in/result/reward/candidate wrote business/audit/idempotency/request rows.");
    }

    @Test
    void httpBoundariesAndIdempotencyConflictRemainConsistent() throws Exception {
        assertThat(exchange(HttpMethod.GET, "/api/v1/activity/me/registrations", jsonHeaders(), null).at("/code").asInt()).isEqualTo(41000);
        assertThat(exchange(HttpMethod.POST, "/api/v1/activity/admin/events", bearerHeaders("user-token", requestId("forbidden")), eventBody("forbidden-" + randomKey())).at("/code").asInt()).isEqualTo(42001);
        assertThat(exchange(HttpMethod.POST, "/api/v1/activity/admin/events", bearerHeaders("admin-token", requestId("validation")), with(eventBody("validation-" + randomKey()), "idempotencyKey", "short")).at("/code").asInt()).isEqualTo(40001);
        assertThat(exchange(HttpMethod.GET, "/api/v1/activity/admin/events/missing", bearerHeaders("admin-token", requestId("missing")), null).at("/code").asInt()).isEqualTo(49600);
        assertThat(exchange(HttpMethod.POST, "/api/v1/activity/admin/events", headersWith("admin-token", requestId("dependency"), "X-Test-Content-Mode", "unavailable"), with(eventBody("dependency-" + randomKey()), "linkedContentId", "content-public-1")).at("/code").asInt()).isEqualTo(49440);

        String idempotencyKey = "idem-" + randomKey();
        exchange(HttpMethod.POST, "/api/v1/activity/admin/events", bearerHeaders("admin-token", requestId("idem-seed")), eventBody(idempotencyKey));
        JsonNode conflict = exchange(HttpMethod.POST, "/api/v1/activity/admin/events", bearerHeaders("admin-token", requestId("idem-conflict")), with(eventBody(idempotencyKey), "title", "不同标题"));
        assertThat(conflict.at("/code").asInt()).isEqualTo(49617);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin-user' AND scope = 'activity.event.create' AND idempotency_key = ?", idempotencyKey, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_request_logs WHERE request_id = ?", requestId("idem-conflict"), 0L);
        }
        System.out.println("SQL evidence: PostgreSQL activity boundary test covered auth, permission, validation, missing resource, dependency failure, and idempotency conflict rollback.");
    }

    private String createPublishedActivity(String purpose) throws Exception {
        JsonNode created = exchange(HttpMethod.POST, "/api/v1/activity/admin/events", bearerHeaders("admin-token", requestId("setup-create-" + purpose)), eventBody("event-" + purpose));
        String activityId = created.at("/data/activityId").asText();
        exchange(HttpMethod.POST, "/api/v1/activity/admin/events/" + activityId + "/submit", bearerHeaders("helper-token", requestId("setup-submit-" + purpose)), Map.of("reason", "提交审核", "idempotencyKey", "submit-" + purpose));
        exchange(HttpMethod.PATCH, "/api/v1/activity/admin/events/" + activityId + "/approve", bearerHeaders("helper-token", requestId("setup-approve-" + purpose)), reviewBody("approve-" + purpose));
        exchange(HttpMethod.PATCH, "/api/v1/activity/admin/events/" + activityId + "/publish", bearerHeaders("admin-token", requestId("setup-publish-" + purpose)), Map.of("reason", "发布活动", "idempotencyKey", "publish-" + purpose));
        return activityId;
    }

    private JsonNode exchange(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).method(method.name(), publisher);
        headers.forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
        HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body());
    }

    private HttpHeaders bearerHeaders(String token, String requestId) {
        return bearerHeaders(token, requestId, Map.of());
    }

    private HttpHeaders bearerHeaders(String token, String requestId, Map<String, String> extraHeaders) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Request-Id", requestId);
        extraHeaders.forEach(headers::set);
        return headers;
    }

    private HttpHeaders headersWith(String token, String requestId, String name, String value) {
        HttpHeaders headers = bearerHeaders(token, requestId);
        headers.set(name, value);
        return headers;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Map<String, Object> eventBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("slug", "activity-pg-" + idempotencyKey.toLowerCase());
        body.put("title", "PostgreSQL 活动");
        body.put("summary", "PostgreSQL 活动摘要");
        body.put("description", "活动描述覆盖真实 HTTP、PostgreSQL 业务表和公共日志。");
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
        body.put("tags", List.of("activity", "postgresql"));
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
        return Map.of("registrationId", registrationId, "type", "POINTS_CANDIDATE", "title", "活动贡献奖励", "description", "参与活动获得贡献候选", "quantity", 1, "scoreCandidateDelta", 10, "reason", "创建奖励", "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> with(Map<String, Object> source, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put(key, value);
        return copy;
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
