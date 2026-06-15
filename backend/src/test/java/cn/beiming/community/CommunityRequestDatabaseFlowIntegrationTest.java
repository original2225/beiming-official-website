package cn.beiming.community;

import cn.beiming.engagement.EngagementCoreServiceApplication;
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
        classes = EngagementCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "community.test-controls.enabled=true"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CommunityRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "community-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:community_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
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
                    "community_flow_request_log",
                    "community_flow_boards",
                    "community_flow_posts",
                    "community_flow_comments",
                    "community_flow_reports",
                    "community_flow_audits"
            )) {
                deleteFlowRows(statement, table);
            }
        }
    }

    @Test
    void createBoardRunsThroughBackendAndDatabaseThenReturnsActiveBoard() throws Exception {
        String requestId = "req-com-board-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/community/admin/boards",
                bearerHeaders("admin-token", requestId),
                boardBody("board-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/status").asText()).isEqualTo("ACTIVE");
        assertThat(json.at("/data/visibility").asText()).isEqualTo("PUBLIC");
        String boardId = json.at("/data/boardId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM community_flow_boards WHERE flow_id = ? AND board_id = ? AND action = 'COMMUNITY_BOARD_CREATED'",
                    FLOW_ID, boardId, "ACTIVE");
            assertSingleValue(connection,
                    "SELECT visibility FROM community_flow_boards WHERE flow_id = ? AND board_id = ? AND action = 'COMMUNITY_BOARD_CREATED'",
                    FLOW_ID, boardId, "PUBLIC");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM community_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'COMMUNITY_BOARD_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM community_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/community/admin/boards'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: community board create reached backend, wrote board/audit/request rows, and returned 201.");
    }

    @Test
    void createPostRunsThroughBackendAndDatabaseThenReturnsDraftPost() throws Exception {
        String boardId = createBoard("setup-post");
        String requestId = "req-com-post-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/community/me/posts",
                bearerHeaders("member-user-1-token", requestId),
                postBody(boardId, "post-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/boardId").asText()).isEqualTo(boardId);
        assertThat(json.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(json.at("/data/author/userId").asText()).isEqualTo("member-user-1");
        String postId = json.at("/data/postId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM community_flow_posts WHERE flow_id = ? AND post_id = ? AND action = 'COMMUNITY_POST_CREATED'",
                    FLOW_ID, postId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT author_user_id FROM community_flow_posts WHERE flow_id = ? AND post_id = ? AND action = 'COMMUNITY_POST_CREATED'",
                    FLOW_ID, postId, "member-user-1");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM community_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'COMMUNITY_POST_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM community_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/community/me/posts'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: community post create reached backend, wrote post/audit/request rows, and returned 201.");
    }

    @Test
    void createCommentRunsThroughBackendAndDatabaseThenReturnsPendingReviewComment() throws Exception {
        String postId = createApprovedPost("setup-comment");
        String requestId = "req-com-comment-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/community/me/posts/" + postId + "/comments",
                bearerHeaders("member-user-2-token", requestId),
                Map.of("body", "数据库流评论证据", "idempotencyKey", "comment-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/postId").asText()).isEqualTo(postId);
        assertThat(json.at("/data/status").asText()).isEqualTo("PENDING_REVIEW");
        assertThat(json.at("/data/author/userId").asText()).isEqualTo("member-user-2");
        String commentId = json.at("/data/commentId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM community_flow_comments WHERE flow_id = ? AND comment_id = ? AND action = 'COMMUNITY_COMMENT_CREATED'",
                    FLOW_ID, commentId, "PENDING_REVIEW");
            assertSingleValue(connection,
                    "SELECT post_id FROM community_flow_comments WHERE flow_id = ? AND comment_id = ? AND action = 'COMMUNITY_COMMENT_CREATED'",
                    FLOW_ID, commentId, postId);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM community_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'COMMUNITY_COMMENT_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM community_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, requestId, "/api/v1/community/me/posts/" + postId + "/comments", 201);
        }
        System.out.println("SQL evidence: community comment create reached backend, wrote comment/audit/request rows, and returned 201.");
    }

    @Test
    void createReportRunsThroughBackendAndDatabaseThenReturnsOpenReport() throws Exception {
        String postId = createApprovedPost("setup-report");
        String requestId = "req-com-report-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/community/me/posts/" + postId + "/reports",
                bearerHeaders("member-user-2-token", requestId),
                reportBody("report-" + randomKey())
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/targetId").asText()).isEqualTo(postId);
        assertThat(json.at("/data/status").asText()).isEqualTo("OPEN");
        assertThat(json.at("/data/targetType").asText()).isEqualTo("POST");
        String reportId = json.at("/data/reportId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM community_flow_reports WHERE flow_id = ? AND report_id = ? AND action = 'COMMUNITY_REPORT_CREATED'",
                    FLOW_ID, reportId, "OPEN");
            assertSingleValue(connection,
                    "SELECT target_id FROM community_flow_reports WHERE flow_id = ? AND report_id = ? AND action = 'COMMUNITY_REPORT_CREATED'",
                    FLOW_ID, reportId, postId);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM community_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'COMMUNITY_REPORT_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM community_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = ?",
                    FLOW_ID, requestId, "/api/v1/community/me/posts/" + postId + "/reports", 201);
        }
        System.out.println("SQL evidence: community report create reached backend, wrote report/audit/request rows, and returned 201.");
    }

    private String createBoard(String purpose) throws Exception {
        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/community/admin/boards",
                bearerHeaders("admin-token", "req-setup-board-" + purpose + "-" + FLOW_ID),
                boardBody(purpose + "-" + randomKey())
        );
        assertThat(response.statusCode()).isEqualTo(201);
        return objectMapper.readTree(response.body()).at("/data/boardId").asText();
    }

    private String createApprovedPost(String purpose) throws Exception {
        String boardId = createBoard(purpose);
        TestHttpResponse created = exchange(
                HttpMethod.POST,
                "/api/v1/community/me/posts",
                bearerHeaders("member-user-1-token", "req-setup-post-" + purpose + "-" + FLOW_ID),
                postBody(boardId, purpose + "-post-" + randomKey())
        );
        assertThat(created.statusCode()).isEqualTo(201);
        String postId = objectMapper.readTree(created.body()).at("/data/postId").asText();
        TestHttpResponse submitted = exchange(
                HttpMethod.POST,
                "/api/v1/community/me/posts/" + postId + "/submit",
                bearerHeaders("member-user-1-token", "req-setup-submit-" + purpose + "-" + FLOW_ID),
                Map.of("idempotencyKey", purpose + "-submit-" + randomKey())
        );
        assertThat(submitted.statusCode()).isEqualTo(200);
        TestHttpResponse approved = exchange(
                HttpMethod.PATCH,
                "/api/v1/community/admin/posts/" + postId + "/approve",
                bearerHeaders("helper-token", "req-setup-approve-" + purpose + "-" + FLOW_ID),
                reviewBody(purpose + "-approve-" + randomKey())
        );
        assertThat(approved.statusCode()).isEqualTo(200);
        return postId;
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

    private Map<String, Object> boardBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("slug", "db-flow-" + idempotencyKey.toLowerCase());
        body.put("name", "数据库流板块");
        body.put("description", "用于社区数据库流测试");
        body.put("visibility", "PUBLIC");
        body.put("status", "ACTIVE");
        body.put("allowedPostTypes", List.of("DISCUSSION", "QUESTION", "SUGGESTION"));
        body.put("tags", List.of("database-flow"));
        body.put("sortOrder", 10);
        body.put("reason", "创建社区板块");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> postBody(String boardId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("boardId", boardId);
        body.put("type", "DISCUSSION");
        body.put("title", "数据库流帖子");
        body.put("summary", "数据库流帖子摘要");
        body.put("body", "数据库流帖子正文，覆盖真实 HTTP 和 SQL 证据。");
        body.put("tags", List.of("database-flow"));
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> reviewBody(String idempotencyKey) {
        return Map.of("reviewComment", "审核通过", "reason", "符合社区规则", "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> reportBody(String idempotencyKey) {
        return Map.of("reasonType", "SPAM", "description", "这个内容疑似灌水，需要处理", "evidenceLinks", List.of("https://example.com/evidence"), "idempotencyKey", idempotencyKey);
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
                CREATE TABLE IF NOT EXISTS community_flow_boards (
                    flow_id VARCHAR(128) NOT NULL,
                    board_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    slug VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    visibility VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS community_flow_posts (
                    flow_id VARCHAR(128) NOT NULL,
                    post_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    board_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    author_user_id VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS community_flow_comments (
                    flow_id VARCHAR(128) NOT NULL,
                    comment_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    post_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    author_user_id VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS community_flow_reports (
                    flow_id VARCHAR(128) NOT NULL,
                    report_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_type VARCHAR(32) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS community_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS community_flow_request_log (
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
        CommunityFlowEvidenceRecorder communityFlowEvidenceRecorder() {
            return new JdbcCommunityFlowEvidenceRecorder();
        }
    }

    static class JdbcCommunityFlowEvidenceRecorder implements CommunityFlowEvidenceRecorder {
        @Override
        public void recordBoardWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO community_flow_boards(flow_id, board_id, action, slug, status, visibility, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("boardId"), action, payload.get("slug"), payload.get("status"), payload.get("visibility"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("boardId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write community board database evidence", exception);
            }
        }

        @Override
        public void recordPostWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            Map<?, ?> author = map(payload.get("author"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO community_flow_posts(flow_id, post_id, action, board_id, status, author_user_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("postId"), action, payload.get("boardId"), payload.get("status"), author.get("userId"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("postId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write community post database evidence", exception);
            }
        }

        @Override
        public void recordCommentWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            Map<?, ?> author = map(payload.get("author"));
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO community_flow_comments(flow_id, comment_id, action, post_id, status, author_user_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("commentId"), action, payload.get("postId"), payload.get("status"), author.get("userId"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("commentId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write community comment database evidence", exception);
            }
        }

        @Override
        public void recordReportWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            if (flowId == null || flowId.isBlank()) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO community_flow_reports(flow_id, report_id, action, target_type, target_id, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("reportId"), action, payload.get("targetType"), payload.get("targetId"), payload.get("status"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request.getHeader("X-Request-Id"), action, String.valueOf(payload.get("reportId")), request.getRequestURI(), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write community report database evidence", exception);
            }
        }

        private static void insertAuditAndRequest(Connection connection, String flowId, String requestId, String action, String targetId, String path, int responseCode) throws Exception {
            insert(connection,
                    "INSERT INTO community_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, requestId, action, targetId, "SUCCESS", Timestamp.from(Instant.now()));
            insert(connection,
                    "INSERT INTO community_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
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
