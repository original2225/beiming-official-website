package cn.beiming.exam;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = AdmissionCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=",
                "spring.flyway.enabled=true",
                "exam.test-controls.enabled=false"
        }
)
@Testcontainers
class ExamPostgreSqlPersistenceIntegrationTest {
    private static final String FLOW_ID = "exam-pg-" + UUID.randomUUID();

    static {
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_exam_pg")
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
    void setUp() {
        assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("true");
    }

    @Test
    void createSessionPersistsFrozenPaperAuditIdempotencyAndRequestLogInPostgreSql() throws Exception {
        String requestId = requestId("create");
        String userId = uniqueUser("create");
        JsonNode response = exchange(
                HttpMethod.POST,
                "/api/v1/exams/me/sessions",
                trustedUserHeaders(userId, "Exam Create", requestId),
                Map.of("applicationId", "app-ready", "idempotencyKey", "create-" + randomKey())
        );

        assertThat(response.at("/code").asInt()).isZero();
        String sessionId = response.at("/data/sessionId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM exam_sessions WHERE session_id = ?", sessionId, "IN_PROGRESS");
            assertSingleValue(connection, "SELECT jsonb_array_length(paper_snapshot -> 'questions') FROM exam_sessions WHERE session_id = ?", sessionId, 3);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'EXAM_SESSION_CREATED' AND target_id = ?", requestId, sessionId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = ? AND scope = 'exam.create-session'", userId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/exams/me/sessions'", requestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL exam create session wrote exam_sessions/app_audit_logs/app_idempotency_records/app_request_logs and returned 201.");
    }

    @Test
    void saveAnswersAndSubmitPersistAnswersScoringAuditAndRequestLogInPostgreSql() throws Exception {
        String userId = uniqueUser("submit");
        String sessionId = createSession(userId, "Exam Submit");
        String saveRequestId = requestId("save");
        JsonNode saved = exchange(
                HttpMethod.PUT,
                "/api/v1/exams/me/sessions/" + sessionId + "/answers",
                trustedUserHeaders(userId, "Exam Submit", saveRequestId),
                Map.of("idempotencyKey", "save-" + randomKey(), "answers", answers())
        );
        assertThat(saved.at("/code").asInt()).isZero();

        String submitRequestId = requestId("submit");
        JsonNode submitted = exchange(
                HttpMethod.POST,
                "/api/v1/exams/me/sessions/" + sessionId + "/submit",
                trustedUserHeaders(userId, "Exam Submit", submitRequestId),
                Map.of("idempotencyKey", "submit-" + randomKey(), "answers", answers())
        );
        assertThat(submitted.at("/code").asInt()).isZero();
        assertThat(submitted.at("/data/status").asText()).isEqualTo("PENDING_MANUAL_REVIEW");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT draft FROM exam_answer_sheets WHERE session_id = ? AND request_id = ?", sessionId, saveRequestId, true);
            assertSingleValue(connection, "SELECT status FROM exam_sessions WHERE session_id = ?", sessionId, "PENDING_MANUAL_REVIEW");
            assertSingleValue(connection, "SELECT (score_summary ->> 'objectiveScore')::int FROM exam_sessions WHERE session_id = ?", sessionId, 20);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'EXAM_ANSWERS_SAVED' AND target_id = ?", saveRequestId, sessionId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'EXAM_SUBMITTED' AND target_id = ?", submitRequestId, sessionId, 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = ?", submitRequestId, "/api/v1/exams/me/sessions/" + sessionId + "/submit", 200);
        }
        System.out.println("SQL evidence: PostgreSQL exam answers and submit wrote exam_answer_sheets/exam_sessions/app_audit_logs/app_request_logs.");
    }

    @Test
    void manualReviewQuestionAndTemplateWritesPersistPostgreSqlEvidence() throws Exception {
        String userId = uniqueUser("review");
        String sessionId = createSubmittedSession(userId, "Exam Review");
        String reviewRequestId = requestId("review");
        JsonNode reviewed = exchange(
                HttpMethod.PATCH,
                "/api/v1/exams/admin/sessions/" + sessionId + "/manual-review",
                bearerHeaders("admin-token", reviewRequestId),
                Map.of(
                        "idempotencyKey", "review-" + randomKey(),
                        "manualScores", List.of(Map.of("questionId", "q-redstone-short", "score", 30, "comment", "ok")),
                        "result", "PASSED",
                        "publicComment", "passed",
                        "reason", "manual review"
                )
        );
        assertThat(reviewed.at("/code").asInt()).isZero();

        String questionRequestId = requestId("question");
        JsonNode question = exchange(HttpMethod.POST, "/api/v1/exams/admin/question-bank/questions", bearerHeaders("admin-token", questionRequestId), questionBody("pg question"));
        String questionId = question.at("/data/questionId").asText();

        String templateRequestId = requestId("template");
        JsonNode template = exchange(HttpMethod.POST, "/api/v1/exams/admin/paper-templates", bearerHeaders("admin-token", templateRequestId), templateBody("PG template"));
        String templateId = template.at("/data/templateId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT result FROM exam_reviews WHERE session_id = ? AND request_id = ?", sessionId, reviewRequestId, "PASSED");
            assertSingleValue(connection, "SELECT status FROM exam_sessions WHERE session_id = ?", sessionId, "MANUAL_PASSED");
            assertSingleValue(connection, "SELECT status FROM exam_questions WHERE question_id = ?", questionId, "DRAFT");
            assertSingleValue(connection, "SELECT COUNT(*) FROM exam_question_versions WHERE question_id = ?", questionId, 1L);
            assertSingleValue(connection, "SELECT status FROM exam_paper_templates WHERE template_id = ?", templateId, "DRAFT");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'EXAM_MANUAL_PASSED' AND target_id = ?", reviewRequestId, sessionId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'exam.create-question'", 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/exams/admin/paper-templates'", templateRequestId, 201);
        }
        System.out.println("SQL evidence: PostgreSQL exam manual review, question bank, and paper template writes persisted business/audit/idempotency/request rows.");
    }

    private String createSession(String userId, String displayName) throws Exception {
        JsonNode response = exchange(HttpMethod.POST, "/api/v1/exams/me/sessions", trustedUserHeaders(userId, displayName, requestId("setup-" + userId)),
                Map.of("applicationId", "app-ready", "idempotencyKey", "setup-" + randomKey()));
        assertThat(response.at("/code").asInt()).isZero();
        return response.at("/data/sessionId").asText();
    }

    private String createSubmittedSession(String userId, String displayName) throws Exception {
        String sessionId = createSession(userId, displayName);
        JsonNode submitted = exchange(HttpMethod.POST, "/api/v1/exams/me/sessions/" + sessionId + "/submit", trustedUserHeaders(userId, displayName, requestId("setup-submit-" + userId)),
                Map.of("idempotencyKey", "setup-submit-" + randomKey(), "answers", answers()));
        assertThat(submitted.at("/code").asInt()).isZero();
        return sessionId;
    }

    private JsonNode exchange(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method.name(), HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
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

    private HttpHeaders trustedUserHeaders(String userId, String displayName, String requestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Request-Id", requestId);
        headers.set("X-Gateway-Internal-Request-Id", "gw-" + requestId);
        headers.set("X-Beiming-Actor-User-Id", userId);
        headers.set("X-Beiming-Actor-Display-Name", displayName);
        headers.set("X-Beiming-Actor-Roles", "USER");
        headers.set("X-Beiming-Actor-Minecraft-Id", userId + "Mc");
        headers.set("X-Beiming-Actor-Minecraft-Uuid", "uuid-" + userId);
        return headers;
    }

    private List<Map<String, Object>> answers() {
        return List.of(
                Map.of("questionId", "q-redstone-single", "selectedOptionIds", List.of("A")),
                Map.of("questionId", "q-redstone-multiple", "selectedOptionIds", List.of("A", "C")),
                Map.of("questionId", "q-redstone-short", "textAnswer", "redstone timing must be reproducible")
        );
    }

    private Map<String, Object> questionBody(String reason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "SINGLE_CHOICE");
        body.put("reviewDirection", "REDSTONE");
        body.put("difficulty", "NORMAL");
        body.put("stem", "redstone machine launch precheck");
        body.put("options", List.of(Map.of("optionId", "A", "label", "A", "text", "test and document"), Map.of("optionId", "B", "label", "B", "text", "start directly")));
        body.put("correctOptionIds", List.of("A"));
        body.put("score", 10);
        body.put("tags", List.of("redstone"));
        body.put("reason", reason);
        body.put("idempotencyKey", "question-" + randomKey());
        return body;
    }

    private Map<String, Object> templateBody(String name) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("reviewDirection", "REDSTONE");
        body.put("difficulty", "NORMAL");
        body.put("timeLimitMinutes", 45);
        body.put("passScore", 10);
        body.put("objectivePassScore", 10);
        body.put("questionRules", List.of(Map.of("type", "SINGLE_CHOICE", "count", 1, "scoreEach", 10, "tags", List.of("redstone"))));
        body.put("contentRuleVersion", "2026-05-22");
        body.put("retakeCooldownHours", 24);
        body.put("reason", "create template");
        body.put("idempotencyKey", "template-" + randomKey());
        return body;
    }

    private String requestId(String prefix) {
        return "req-" + prefix + "-" + FLOW_ID;
    }

    private String uniqueUser(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private static String randomKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void assertSingleValue(Connection connection, String sql, Object expected) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).as(sql).isTrue();
                assertThat(result.getObject(1)).isEqualTo(expected);
                assertThat(result.next()).as(sql + " must return one row").isFalse();
            }
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
