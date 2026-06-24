package cn.beiming.community;

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
                "community.test-controls.enabled=true"
        }
)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CommunityPostgreSqlPersistenceIntegrationTest {
    private static final String FLOW_ID = "community-pg-" + UUID.randomUUID();

    static {
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_community_pg")
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
            statement.executeUpdate("DELETE FROM community_ticket_messages");
            statement.executeUpdate("DELETE FROM community_tickets");
            statement.executeUpdate("DELETE FROM community_reports");
            statement.executeUpdate("DELETE FROM community_poll_votes");
            statement.executeUpdate("DELETE FROM community_polls");
            statement.executeUpdate("DELETE FROM community_favorites");
            statement.executeUpdate("DELETE FROM community_reactions");
            statement.executeUpdate("DELETE FROM community_comments");
            statement.executeUpdate("DELETE FROM community_penalties");
            statement.executeUpdate("DELETE FROM community_posts");
            statement.executeUpdate("DELETE FROM community_boards");
            statement.executeUpdate("DELETE FROM app_idempotency_records WHERE scope LIKE 'community.%'");
            statement.executeUpdate("DELETE FROM app_audit_logs WHERE target_type LIKE 'COMMUNITY%'");
            statement.executeUpdate("DELETE FROM app_request_logs WHERE path LIKE '/api/v1/community%'");
        }
    }

    @Test
    void boardPostCommentReactionAndFavoritePersistBusinessAuditIdempotencyAndRequestLogs() throws Exception {
        JsonNode board = exchange(HttpMethod.POST, "/api/v1/community/admin/boards", bearerHeaders("admin-token", requestId("board")), boardBody("board-" + randomKey()));
        assertThat(board.at("/code").asInt()).isZero();
        String boardId = board.at("/data/boardId").asText();

        JsonNode post = exchange(HttpMethod.POST, "/api/v1/community/me/posts", bearerHeaders("member-user-1-token", requestId("post")), postBody(boardId, "post-" + randomKey()));
        assertThat(post.at("/data/status").asText()).isEqualTo("DRAFT");
        String postId = post.at("/data/postId").asText();

        exchange(HttpMethod.POST, "/api/v1/community/me/posts/" + postId + "/submit", bearerHeaders("member-user-1-token", requestId("submit")), Map.of("idempotencyKey", "submit-" + randomKey()));
        exchange(HttpMethod.PATCH, "/api/v1/community/admin/posts/" + postId + "/approve", bearerHeaders("helper-token", requestId("approve")), reviewBody("approve-" + randomKey()));
        JsonNode comment = exchange(HttpMethod.POST, "/api/v1/community/me/posts/" + postId + "/comments", bearerHeaders("member-user-2-token", requestId("comment")), Map.of("body", "PostgreSQL 评论证据", "idempotencyKey", "comment-" + randomKey()));
        String commentId = comment.at("/data/commentId").asText();
        exchange(HttpMethod.PATCH, "/api/v1/community/admin/comments/" + commentId + "/approve", bearerHeaders("helper-token", requestId("comment-approve")), reviewBody("comment-approve-" + randomKey()));
        exchange(HttpMethod.POST, "/api/v1/community/me/posts/" + postId + "/like", bearerHeaders("member-user-2-token", requestId("like")), Map.of("idempotencyKey", "like-" + randomKey()));
        exchange(HttpMethod.POST, "/api/v1/community/me/posts/" + postId + "/favorite", bearerHeaders("member-user-2-token", requestId("favorite")), Map.of("idempotencyKey", "favorite-" + randomKey()));

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM community_boards WHERE board_id = ?", boardId, "ACTIVE");
            assertSingleValue(connection, "SELECT status FROM community_posts WHERE post_id = ?", postId, "APPROVED");
            assertSingleValue(connection, "SELECT status FROM community_comments WHERE comment_id = ?", commentId, "APPROVED");
            assertSingleValue(connection, "SELECT COUNT(*) FROM community_reactions WHERE target_type = 'POST' AND target_id = ? AND actor_user_id = 'member-user-2' AND active = true", postId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM community_favorites WHERE post_id = ? AND actor_user_id = 'member-user-2' AND active = true", postId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_type = 'COMMUNITY_POST' AND target_id = ? AND action = 'COMMUNITY_POST_APPROVED'", postId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'member-user-1' AND scope = 'community.post.create'", 1L);
            assertSingleValue(connection, "SELECT response_code FROM app_request_logs WHERE request_id = ? AND path = '/api/v1/community/me/posts'", requestId("post"), 201);
        }
        System.out.println("SQL evidence: PostgreSQL community board/post/comment/reaction/favorite wrote business/audit/idempotency/request rows.");
    }

    @Test
    void pollReportTicketPenaltyAndIdempotencyConflictKeepPostgreSqlRowsConsistent() throws Exception {
        String postId = createApprovedPost("setup-" + randomKey());
        JsonNode poll = exchange(HttpMethod.POST, "/api/v1/community/admin/polls", bearerHeaders("admin-token", requestId("poll")), pollBody(postId, "poll-" + randomKey()));
        String pollId = poll.at("/data/pollId").asText();
        String optionId = poll.at("/data/options/0/optionId").asText();
        exchange(HttpMethod.PATCH, "/api/v1/community/admin/polls/" + pollId + "/open", bearerHeaders("admin-token", requestId("poll-open")), Map.of("reason", "开放投票", "idempotencyKey", "poll-open-" + randomKey()));
        exchange(HttpMethod.POST, "/api/v1/community/me/polls/" + pollId + "/votes", bearerHeaders("member-user-2-token", requestId("vote")), Map.of("optionIds", List.of(optionId), "idempotencyKey", "vote-" + randomKey()));

        JsonNode report = exchange(HttpMethod.POST, "/api/v1/community/me/posts/" + postId + "/reports", bearerHeaders("member-user-2-token", requestId("report")), reportBody("report-" + randomKey()));
        String reportId = report.at("/data/reportId").asText();
        exchange(HttpMethod.PATCH, "/api/v1/community/admin/reports/" + reportId + "/assign", bearerHeaders("helper-token", requestId("report-assign")), Map.of("reason", "领取举报", "idempotencyKey", "report-assign-" + randomKey()));

        JsonNode ticket = exchange(HttpMethod.POST, "/api/v1/community/me/tickets", bearerHeaders("pending-profile-token", requestId("ticket")), ticketBody("ticket-" + randomKey()));
        String ticketId = ticket.at("/data/ticketId").asText();
        exchange(HttpMethod.POST, "/api/v1/community/admin/tickets/" + ticketId + "/messages", bearerHeaders("helper-token", requestId("ticket-reply")), Map.of("messageType", "STAFF_REPLY", "body", "已收到", "reason", "回复工单", "idempotencyKey", "ticket-reply-" + randomKey()));

        JsonNode penalty = exchange(HttpMethod.POST, "/api/v1/community/admin/penalties", bearerHeaders("admin-token", requestId("penalty")), penaltyBody("member-user-1", "penalty-" + randomKey()));
        String penaltyId = penalty.at("/data/penaltyId").asText();
        exchange(HttpMethod.PATCH, "/api/v1/community/admin/penalties/" + penaltyId + "/revoke", bearerHeaders("admin-token", requestId("penalty-revoke")), Map.of("publicReason", "解除限制", "reason", "处罚解除", "confirmText", "REVOKE_COMMUNITY_PENALTY", "idempotencyKey", "penalty-revoke-" + FLOW_ID));

        String conflictKey = "board-conflict-" + randomKey().substring(0, 12);
        exchange(HttpMethod.POST, "/api/v1/community/admin/boards", bearerHeaders("admin-token", requestId("conflict-seed")), boardBody(conflictKey));
        JsonNode conflict = exchange(HttpMethod.POST, "/api/v1/community/admin/boards", bearerHeaders("admin-token", requestId("board-conflict")), with(boardBody(conflictKey), "name", "不同板块名"));
        assertThat(conflict.at("/code").asInt()).isEqualTo(49017);

        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT status FROM community_polls WHERE poll_id = ?", pollId, "OPEN");
            assertSingleValue(connection, "SELECT COUNT(*) FROM community_poll_votes WHERE poll_id = ? AND actor_user_id = 'member-user-2'", pollId, 1L);
            assertSingleValue(connection, "SELECT status FROM community_reports WHERE report_id = ?", reportId, "UNDER_REVIEW");
            assertSingleValue(connection, "SELECT status FROM community_tickets WHERE ticket_id = ?", ticketId, "WAITING_USER");
            assertSingleValue(connection, "SELECT COUNT(*) FROM community_ticket_messages WHERE ticket_id = ? AND message_type = 'STAFF_REPLY'", ticketId, 1L);
            assertSingleValue(connection, "SELECT status FROM community_penalties WHERE penalty_id = ?", penaltyId, "REVOKED");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE request_id = ? AND action = 'COMMUNITY_PENALTY_REVOKED' AND target_id = ?", requestId("penalty-revoke"), penaltyId, 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_idempotency_records WHERE actor_user_id = 'admin' AND scope = 'community.penalty.revoke'", 1L);
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_request_logs WHERE request_id = ?", requestId("board-conflict"), 0L);
        }
        System.out.println("SQL evidence: PostgreSQL community poll/report/ticket/penalty wrote rows and idempotency conflict added no successful request log.");
    }

    @Test
    void authenticationPermissionValidationMissingStateAndDependencyFailuresRemainHttpBoundaries() throws Exception {
        assertThat(exchange(HttpMethod.POST, "/api/v1/community/me/posts", jsonHeaders(), postBody("board-general", "unauth-" + randomKey())).at("/code").asInt()).isEqualTo(41000);
        assertThat(exchange(HttpMethod.POST, "/api/v1/community/admin/boards", bearerHeaders("helper-token", requestId("forbidden")), boardBody("forbidden-" + randomKey())).at("/code").asInt()).isEqualTo(42001);
        assertThat(exchange(HttpMethod.POST, "/api/v1/community/admin/boards", bearerHeaders("admin-token", requestId("validation")), Map.of("slug", "bad slug", "idempotencyKey", "short")).at("/code").asInt()).isEqualTo(40001);
        assertThat(exchange(HttpMethod.GET, "/api/v1/community/admin/posts/missing", bearerHeaders("admin-token", requestId("missing")), null).at("/code").asInt()).isEqualTo(49001);
        assertThat(exchange(HttpMethod.POST, "/api/v1/community/me/posts", headersWith("member-user-1-token", requestId("dependency"), "X-Test-Profile-Mode", "unavailable"), postBody("board-general", "dep-" + randomKey())).at("/code").asInt()).isEqualTo(49210);

        String postId = createApprovedPost("offline-" + randomKey());
        exchange(HttpMethod.PATCH, "/api/v1/community/admin/posts/" + postId + "/offline", headersWith("admin-token", requestId("notify"), "X-Test-Notification-Mode", "unavailable"), Map.of("publicReason", "通知失败也要下架", "reason", "治理下架", "idempotencyKey", "notify-" + randomKey()));
        try (Connection connection = openConnection()) {
            assertSingleValue(connection, "SELECT notification_status FROM community_posts WHERE post_id = ?", postId, "FAILED");
            assertSingleValue(connection, "SELECT COUNT(*) FROM app_audit_logs WHERE target_id = ? AND action = 'COMMUNITY_NOTIFICATION_FAILED'", postId, 1L);
        }
        System.out.println("SQL evidence: PostgreSQL community HTTP boundaries covered auth, permission, validation, missing resource, state/dependency failure, and notification degradation.");
    }

    private String createApprovedPost(String purpose) throws Exception {
        String boardId = exchange(HttpMethod.POST, "/api/v1/community/admin/boards", bearerHeaders("admin-token", requestId("setup-board-" + purpose)), boardBody("board-" + purpose)).at("/data/boardId").asText();
        JsonNode post = exchange(HttpMethod.POST, "/api/v1/community/me/posts", bearerHeaders("member-user-1-token", requestId("setup-post-" + purpose)), postBody(boardId, "post-" + purpose));
        String postId = post.at("/data/postId").asText();
        exchange(HttpMethod.POST, "/api/v1/community/me/posts/" + postId + "/submit", bearerHeaders("member-user-1-token", requestId("setup-submit-" + purpose)), Map.of("idempotencyKey", "submit-" + purpose));
        exchange(HttpMethod.PATCH, "/api/v1/community/admin/posts/" + postId + "/approve", bearerHeaders("helper-token", requestId("setup-approve-" + purpose)), reviewBody("approve-" + purpose));
        return postId;
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

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders headersWith(String token, String requestId, String name, String value) {
        HttpHeaders headers = bearerHeaders(token, requestId);
        headers.set(name, value);
        return headers;
    }

    private Map<String, Object> boardBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("slug", ("pg-" + idempotencyKey).toLowerCase());
        body.put("name", "PostgreSQL 社区板块");
        body.put("description", "用于社区 PostgreSQL 持久化测试");
        body.put("visibility", "PUBLIC");
        body.put("status", "ACTIVE");
        body.put("allowedPostTypes", List.of("DISCUSSION", "QUESTION", "SUGGESTION"));
        body.put("tags", List.of("postgresql"));
        body.put("sortOrder", 10);
        body.put("reason", "创建社区板块");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> postBody(String boardId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("boardId", boardId);
        body.put("type", "DISCUSSION");
        body.put("title", "PostgreSQL 社区帖子");
        body.put("summary", "社区帖子摘要");
        body.put("body", "社区帖子正文，覆盖 PostgreSQL 业务表和公共日志。");
        body.put("tags", List.of("postgresql"));
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> reviewBody(String idempotencyKey) {
        return Map.of("reviewComment", "审核通过", "reason", "符合社区规则", "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> pollBody(String postId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("postId", postId);
        body.put("title", "社区投票");
        body.put("description", "选择社区方向");
        body.put("options", List.of(Map.of("label", "方案一"), Map.of("label", "方案二")));
        body.put("multipleChoice", false);
        body.put("minChoices", 1);
        body.put("maxChoices", 1);
        body.put("eligibleVisibility", "PUBLIC");
        body.put("anonymousResult", true);
        body.put("reason", "创建投票");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> reportBody(String idempotencyKey) {
        return Map.of("reasonType", "SPAM", "description", "这个内容疑似灌水，需要处理", "evidenceLinks", List.of("https://example.com/evidence"), "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> ticketBody(String idempotencyKey) {
        return Map.of("type", "ACCOUNT_ISSUE", "title", "账号问题", "body", "账号绑定遇到问题", "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> penaltyBody(String userId, String idempotencyKey) {
        return Map.of("targetUserId", userId, "type", "POST_RESTRICTED", "publicReason", "临时限制发帖", "reason", "社区治理处罚", "confirmText", "CREATE_COMMUNITY_PENALTY", "idempotencyKey", idempotencyKey);
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
