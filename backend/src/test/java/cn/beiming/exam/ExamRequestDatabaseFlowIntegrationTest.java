package cn.beiming.exam;

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
        properties = "exam.test-controls.enabled=true"
)
class ExamRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "exam-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:exam_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
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
                    "exam_flow_request_log",
                    "exam_flow_sessions",
                    "exam_flow_answers",
                    "exam_flow_questions",
                    "exam_flow_audits"
            )) {
                deleteFlowRows(statement, table);
            }
        }
    }

    @Test
    void createSessionRunsThroughBackendAndDatabaseThenReturnsCreatedSession() throws Exception {
        String requestId = "req-create-session-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/exams/me/sessions",
                bearerHeaders("ready-token", requestId),
                Map.of("applicationId", "app-ready", "idempotencyKey", "create-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/userId").asText()).isEqualTo("seed-ready");
        assertThat(json.at("/data/status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(json.at("/data/reviewDirection").asText()).isEqualTo("REDSTONE");
        String sessionId = json.at("/data/sessionId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM exam_flow_sessions WHERE flow_id = ? AND session_id = ? AND action = 'EXAM_SESSION_CREATED'",
                    FLOW_ID, sessionId, "IN_PROGRESS");
            assertSingleValue(connection,
                    "SELECT user_id FROM exam_flow_sessions WHERE flow_id = ? AND session_id = ? AND action = 'EXAM_SESSION_CREATED'",
                    FLOW_ID, sessionId, "seed-ready");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM exam_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'EXAM_SESSION_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM exam_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/exams/me/sessions'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: exam create session reached backend, wrote session/audit/request rows, and returned 201.");
    }

    @Test
    void saveAnswersRunsThroughBackendAndDatabaseThenReturnsDraftAnswers() throws Exception {
        String requestId = "req-save-answers-" + FLOW_ID;
        String sessionId = createReadySession("save");

        TestHttpResponse response = exchange(
                HttpMethod.PUT,
                "/api/v1/exams/me/sessions/" + sessionId + "/answers",
                bearerHeaders("ready-token", requestId),
                Map.of("idempotencyKey", "save-" + randomKey(), "answers", answers())
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/sessionId").asText()).isEqualTo(sessionId);
        assertThat(json.at("/data/draft").asBoolean()).isTrue();
        assertThat(json.at("/data/answers").size()).isEqualTo(3);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT answers_count FROM exam_flow_answers WHERE flow_id = ? AND session_id = ? AND action = 'EXAM_ANSWERS_SAVED'",
                    FLOW_ID, sessionId, 3);
            assertSingleValue(connection,
                    "SELECT status FROM exam_flow_answers WHERE flow_id = ? AND session_id = ? AND action = 'EXAM_ANSWERS_SAVED'",
                    FLOW_ID, sessionId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM exam_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'EXAM_ANSWERS_SAVED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM exam_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, requestId, "/api/v1/exams/me/sessions/" + sessionId + "/answers", 200);
        }
        System.out.println("SQL evidence: exam save answers reached backend, wrote answer/audit/request rows, and returned 200.");
    }

    @Test
    void submitRunsThroughBackendAndDatabaseThenReturnsPendingManualReview() throws Exception {
        String requestId = "req-submit-" + FLOW_ID;
        String sessionId = createReadySession("submit");

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/exams/me/sessions/" + sessionId + "/submit",
                bearerHeaders("ready-token", requestId),
                Map.of("idempotencyKey", "submit-" + randomKey(), "answers", answers())
        );

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/sessionId").asText()).isEqualTo(sessionId);
        assertThat(json.at("/data/status").asText()).isEqualTo("PENDING_MANUAL_REVIEW");
        assertThat(json.at("/data/result").asText()).isEqualTo("PENDING");
        assertThat(json.at("/data/scoreSummary/objectiveScore").asInt()).isEqualTo(20);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM exam_flow_sessions WHERE flow_id = ? AND session_id = ? AND action = 'EXAM_SESSION_SUBMITTED'",
                    FLOW_ID, sessionId, "PENDING_MANUAL_REVIEW");
            assertSingleValue(connection,
                    "SELECT result FROM exam_flow_sessions WHERE flow_id = ? AND session_id = ? AND action = 'EXAM_SESSION_SUBMITTED'",
                    FLOW_ID, sessionId, "PENDING");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM exam_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'EXAM_SESSION_SUBMITTED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM exam_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, requestId, "/api/v1/exams/me/sessions/" + sessionId + "/submit", 200);
        }
        System.out.println("SQL evidence: exam submit reached backend, wrote submitted session/audit/request rows, and returned 200.");
    }

    @Test
    void createQuestionRunsThroughBackendAndDatabaseThenReturnsDraftQuestion() throws Exception {
        String requestId = "req-create-question-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/exams/admin/question-bank/questions",
                bearerHeaders("admin-token", requestId),
                questionBody("SQL evidence question " + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(json.at("/data/type").asText()).isEqualTo("SINGLE_CHOICE");
        assertThat(json.at("/data/reviewDirection").asText()).isEqualTo("REDSTONE");
        String questionId = json.at("/data/questionId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM exam_flow_questions WHERE flow_id = ? AND question_id = ? AND action = 'EXAM_QUESTION_CREATED'",
                    FLOW_ID, questionId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT version FROM exam_flow_questions WHERE flow_id = ? AND question_id = ? AND action = 'EXAM_QUESTION_CREATED'",
                    FLOW_ID, questionId, 1);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM exam_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'EXAM_QUESTION_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM exam_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/exams/admin/question-bank/questions'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: exam create question reached backend, wrote question/audit/request rows, and returned 201.");
    }

    private String createReadySession(String purpose) throws Exception {
        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/exams/me/sessions",
                bearerHeaders("ready-token", "req-setup-" + purpose + "-" + FLOW_ID),
                Map.of("applicationId", "app-ready-" + purpose + "-" + randomKey(), "idempotencyKey", "setup-" + randomKey())
        );
        assertThat(response.statusCode()).isEqualTo(201);
        return objectMapper.readTree(response.body()).at("/data/sessionId").asText();
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
                CREATE TABLE IF NOT EXISTS exam_flow_sessions (
                    flow_id VARCHAR(128) NOT NULL,
                    session_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    user_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    review_direction VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS exam_flow_answers (
                    flow_id VARCHAR(128) NOT NULL,
                    session_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    answers_count INT NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS exam_flow_questions (
                    flow_id VARCHAR(128) NOT NULL,
                    question_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    type VARCHAR(32) NOT NULL,
                    review_direction VARCHAR(32) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    version INT NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS exam_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS exam_flow_request_log (
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
        ExamFlowEvidenceRecorder examFlowEvidenceRecorder() {
            return new JdbcExamFlowEvidenceRecorder();
        }
    }

    static class JdbcExamFlowEvidenceRecorder implements ExamFlowEvidenceRecorder {
        @Override
        public void recordSessionWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String sessionId = String.valueOf(payload.get("sessionId"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO exam_flow_sessions(flow_id, session_id, action, user_id, status, result, review_direction, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId, sessionId, action, payload.get("userId"), payload.get("status"), payload.get("result"), payload.get("reviewDirection"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, sessionId, request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write exam session database evidence", exception);
            }
        }

        @Override
        public void recordAnswerWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String sessionId = String.valueOf(payload.get("sessionId"));
            int answersCount = payload.get("answers") instanceof List<?> answers ? answers.size() : 0;
            String status = Boolean.TRUE.equals(payload.get("draft")) ? "DRAFT" : "SUBMITTED";
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO exam_flow_answers(flow_id, session_id, action, answers_count, status, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        flowId, sessionId, action, answersCount, status, Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, sessionId, request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write exam answer database evidence", exception);
            }
        }

        @Override
        public void recordQuestionWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            String questionId = String.valueOf(payload.get("questionId"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO exam_flow_questions(flow_id, question_id, action, type, review_direction, status, version, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId, questionId, action, payload.get("type"), payload.get("reviewDirection"), payload.get("status"), payload.get("version"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, questionId, request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write exam question database evidence", exception);
            }
        }

        private static void insertAuditAndRequest(Connection connection, String flowId, String requestId, String action, String targetId, String path, int responseCode) throws Exception {
            insert(connection,
                    "INSERT INTO exam_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, requestId, action, targetId, "SUCCESS", Timestamp.from(Instant.now()));
            insert(connection,
                    "INSERT INTO exam_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
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
