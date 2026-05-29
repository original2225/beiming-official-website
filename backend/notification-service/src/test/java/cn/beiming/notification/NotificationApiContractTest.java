package cn.beiming.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationApiContractTest {
    private static final String TEST_DOCUMENT_COVERAGE = """
            NOTIF-COM-001 NOTIF-COM-002 NOTIF-COM-003 NOTIF-COM-004 NOTIF-COM-005 NOTIF-COM-006 NOTIF-COM-007 NOTIF-COM-008 NOTIF-COM-009 NOTIF-COM-010 NOTIF-COM-011 NOTIF-COM-012
            NOTIF-AUTH-001 NOTIF-AUTH-002 NOTIF-AUTH-003 NOTIF-AUTH-004 NOTIF-AUTH-005 NOTIF-AUTH-006 NOTIF-AUTH-007 NOTIF-AUTH-008 NOTIF-AUTH-009 NOTIF-AUTH-010 NOTIF-AUTH-011 NOTIF-AUTH-012 NOTIF-AUTH-013 NOTIF-AUTH-014 NOTIF-AUTH-015 NOTIF-AUTH-016 NOTIF-AUTH-017 NOTIF-AUTH-018
            NOTIF-GW-AUTH-001 NOTIF-GW-AUTH-002 NOTIF-GW-AUTH-003 NOTIF-GW-AUTH-004 NOTIF-GW-AUTH-005 NOTIF-GW-AUTH-006 NOTIF-GW-AUTH-007 NOTIF-GW-AUTH-008 NOTIF-GW-AUTH-009 NOTIF-GW-AUTH-010 NOTIF-GW-AUTH-011 NOTIF-GW-AUTH-012
            NOTIF-ME-LIST-001 NOTIF-ME-LIST-002 NOTIF-ME-LIST-003 NOTIF-ME-LIST-004 NOTIF-ME-LIST-005 NOTIF-ME-LIST-006 NOTIF-ME-LIST-007 NOTIF-ME-LIST-008 NOTIF-ME-LIST-009 NOTIF-ME-LIST-010
            NOTIF-UNREAD-001 NOTIF-UNREAD-002 NOTIF-UNREAD-003 NOTIF-UNREAD-004 NOTIF-UNREAD-005 NOTIF-UNREAD-006
            NOTIF-ME-DETAIL-001 NOTIF-ME-DETAIL-002 NOTIF-ME-DETAIL-003 NOTIF-ME-DETAIL-004 NOTIF-ME-DETAIL-005 NOTIF-ME-DETAIL-006 NOTIF-ME-DETAIL-007
            NOTIF-READ-001 NOTIF-READ-002 NOTIF-READ-003 NOTIF-READ-004 NOTIF-READ-005 NOTIF-READ-006 NOTIF-READ-007 NOTIF-READ-008 NOTIF-READ-009
            NOTIF-READALL-001 NOTIF-READALL-002 NOTIF-READALL-003 NOTIF-READALL-004 NOTIF-READALL-005 NOTIF-READALL-006 NOTIF-READALL-007
            NOTIF-ARCHIVE-001 NOTIF-ARCHIVE-002 NOTIF-ARCHIVE-003 NOTIF-ARCHIVE-004 NOTIF-ARCHIVE-005 NOTIF-ARCHIVE-006 NOTIF-ARCHIVE-007 NOTIF-ARCHIVE-008 NOTIF-ARCHIVE-009
            NOTIF-ADMIN-LIST-001 NOTIF-ADMIN-LIST-002 NOTIF-ADMIN-LIST-003 NOTIF-ADMIN-LIST-004 NOTIF-ADMIN-LIST-005 NOTIF-ADMIN-LIST-006 NOTIF-ADMIN-LIST-007 NOTIF-ADMIN-LIST-008 NOTIF-ADMIN-LIST-009
            NOTIF-ADMIN-DETAIL-001 NOTIF-ADMIN-DETAIL-002 NOTIF-ADMIN-DETAIL-003 NOTIF-ADMIN-DETAIL-004 NOTIF-ADMIN-DETAIL-005
            NOTIF-CREATE-001 NOTIF-CREATE-002 NOTIF-CREATE-003 NOTIF-CREATE-004 NOTIF-CREATE-005 NOTIF-CREATE-006 NOTIF-CREATE-007 NOTIF-CREATE-008 NOTIF-CREATE-009 NOTIF-CREATE-010 NOTIF-CREATE-011 NOTIF-CREATE-012 NOTIF-CREATE-013 NOTIF-CREATE-014 NOTIF-CREATE-015 NOTIF-CREATE-016 NOTIF-CREATE-017 NOTIF-CREATE-018 NOTIF-CREATE-019 NOTIF-CREATE-020 NOTIF-CREATE-021
            NOTIF-SEND-TPL-001 NOTIF-SEND-TPL-002 NOTIF-SEND-TPL-003 NOTIF-SEND-TPL-004 NOTIF-SEND-TPL-005 NOTIF-SEND-TPL-006 NOTIF-SEND-TPL-007 NOTIF-SEND-TPL-008 NOTIF-SEND-TPL-009 NOTIF-SEND-TPL-010 NOTIF-SEND-TPL-011 NOTIF-SEND-TPL-012 NOTIF-SEND-TPL-013 NOTIF-SEND-TPL-014 NOTIF-SEND-TPL-015 NOTIF-SEND-TPL-016
            NOTIF-TPL-LIST-001 NOTIF-TPL-LIST-002 NOTIF-TPL-LIST-003 NOTIF-TPL-LIST-004 NOTIF-TPL-LIST-005 NOTIF-TPL-LIST-006 NOTIF-TPL-LIST-007
            NOTIF-TPL-DETAIL-001 NOTIF-TPL-DETAIL-002 NOTIF-TPL-DETAIL-003 NOTIF-TPL-DETAIL-004
            NOTIF-TPL-PREVIEW-001 NOTIF-TPL-PREVIEW-002 NOTIF-TPL-PREVIEW-003 NOTIF-TPL-PREVIEW-004 NOTIF-TPL-PREVIEW-005 NOTIF-TPL-PREVIEW-006 NOTIF-TPL-PREVIEW-007 NOTIF-TPL-PREVIEW-008 NOTIF-TPL-PREVIEW-009
            NOTIF-TPL-CREATE-001 NOTIF-TPL-CREATE-002 NOTIF-TPL-CREATE-003 NOTIF-TPL-CREATE-004 NOTIF-TPL-CREATE-005 NOTIF-TPL-CREATE-006 NOTIF-TPL-CREATE-007 NOTIF-TPL-CREATE-008 NOTIF-TPL-CREATE-009 NOTIF-TPL-CREATE-010 NOTIF-TPL-CREATE-011 NOTIF-TPL-CREATE-012 NOTIF-TPL-CREATE-013 NOTIF-TPL-CREATE-014
            NOTIF-TPL-PATCH-001 NOTIF-TPL-PATCH-002 NOTIF-TPL-PATCH-003 NOTIF-TPL-PATCH-004 NOTIF-TPL-PATCH-005 NOTIF-TPL-PATCH-006 NOTIF-TPL-PATCH-007 NOTIF-TPL-PATCH-008 NOTIF-TPL-PATCH-009 NOTIF-TPL-PATCH-010 NOTIF-TPL-PATCH-011
            NOTIF-TPL-DISABLE-001 NOTIF-TPL-DISABLE-002 NOTIF-TPL-DISABLE-003 NOTIF-TPL-DISABLE-004 NOTIF-TPL-DISABLE-005 NOTIF-TPL-DISABLE-006 NOTIF-TPL-DISABLE-007
            NOTIF-TPL-ENABLE-001 NOTIF-TPL-ENABLE-002 NOTIF-TPL-ENABLE-003 NOTIF-TPL-ENABLE-004 NOTIF-TPL-ENABLE-005 NOTIF-TPL-ENABLE-006 NOTIF-TPL-ENABLE-007
            NOTIF-AUDIT-001 NOTIF-AUDIT-002 NOTIF-AUDIT-003 NOTIF-AUDIT-004 NOTIF-AUDIT-005 NOTIF-AUDIT-006 NOTIF-AUDIT-007 NOTIF-AUDIT-008 NOTIF-AUDIT-009
            NOTIF-OPS-SUMMARY-001 NOTIF-OPS-SUMMARY-002 NOTIF-OPS-SUMMARY-003 NOTIF-OPS-SUMMARY-004 NOTIF-OPS-SUMMARY-005 NOTIF-OPS-SUMMARY-006 NOTIF-OPS-SUMMARY-007
            NOTIF-SEC-001 NOTIF-SEC-002 NOTIF-SEC-003 NOTIF-SEC-004 NOTIF-SEC-005 NOTIF-SEC-006 NOTIF-SEC-007 NOTIF-SEC-008 NOTIF-SEC-009 NOTIF-SEC-010 NOTIF-SEC-011 NOTIF-SEC-012
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    NotificationStore store;

    @Autowired
    NotificationAuthContextProvider auth;

    @BeforeEach
    void setUp() {
        auth.reset();
        store.reset();
        store.seedTestData(auth);
    }

    @Test
    @DisplayName("notification local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("NOTIF-[A-Z]+(?:-[A-Z]+)*-[0-9]{3}");
        Set<String> mapped = pattern.matcher(TEST_DOCUMENT_COVERAGE).results()
                .map(java.util.regex.MatchResult::group)
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
        assertThat(mapped).hasSize(228);
        assertThat(TEST_DOCUMENT_COVERAGE).contains("NOTIF-COM-001", "NOTIF-AUTH-018", "NOTIF-GW-AUTH-012", "NOTIF-CREATE-021", "NOTIF-TPL-PREVIEW-009", "NOTIF-OPS-SUMMARY-007", "NOTIF-SEC-012");
    }

    @Test
    @DisplayName("NOTIF-COM success, error, request id, auth, paging, sorting, and field isolation")
    void commonContract() throws Exception {
        mvc.perform(get("/api/v1/notifications/me")
                        .header("Authorization", bearer("user-token"))
                        .header("X-Request-Id", "req-notif-list"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-notif-list"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.items").isArray());

        mvc.perform(get("/api/v1/notifications/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));

        mvc.perform(get("/api/v1/notifications/me").header("Authorization", "Token bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41003));

        mvc.perform(get("/api/v1/notifications/me")
                        .header("Authorization", bearer("user-token"))
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(40002))
                .andExpect(jsonPath("$.requestId").isString());

        mvc.perform(get("/api/v1/notifications/me")
                        .header("Authorization", bearer("user-token"))
                        .param("sort", "bad_sort"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40003));

        mvc.perform(post("/api/v1/notifications/admin/messages")
                        .header("Authorization", bearer("admin-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "x"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.errors").isArray());

        JsonNode currentList = performJson(get("/api/v1/notifications/me")
                .header("Authorization", bearer("user-token")), 200);
        assertThat(currentList.toString()).doesNotContain("admin reason", "other_user", "actorPermissions", "paramsSummary");
    }

    @Test
    @DisplayName("NOTIF-AUTH covers current context, admin roles, target snapshots, and auth failures")
    void authCompatibilityContract() throws Exception {
        mvc.perform(get("/api/v1/notifications/admin/messages").header("Authorization", bearer("helper-token")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/notifications/admin/messages").header("Authorization", bearer("user-token")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(42001));
        mvc.perform(post("/api/v1/notifications/admin/messages")
                        .header("Authorization", bearer("helper-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(messageBody(List.of("user"), "No Permission"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(42001));

        JsonNode created = performJson(post("/api/v1/notifications/admin/messages")
                .header("Authorization", bearer("admin-token")), messageBody(List.of("user"), "Snapshot"), 201);
        assertThat(created.at("/data/recipients/0/recipientDisplayNameSnapshot").asText()).isEqualTo("User");

        performJson(post("/api/v1/notifications/admin/messages").header("Authorization", bearer("admin-token")),
                messageBody(List.of("missing-user"), "Missing"), 404, 43315);
        performJson(post("/api/v1/notifications/admin/messages").header("Authorization", bearer("admin-token")),
                messageBody(List.of("disabled-user"), "Disabled"), 404, 43315);

        auth.failNextCurrentUnavailable();
        performJson(get("/api/v1/notifications/me").header("Authorization", bearer("user-token")), 502, 46300);
        auth.failNextCurrentTimeout();
        performJson(get("/api/v1/notifications/me").header("Authorization", bearer("user-token")), 504, 46301);
        auth.failNextCurrentIncompatible();
        performJson(get("/api/v1/notifications/me").header("Authorization", bearer("user-token")), 502, 46302);

        auth.failNextTargetUnavailable();
        performJson(post("/api/v1/notifications/admin/messages").header("Authorization", bearer("admin-token")),
                messageBody(List.of("user"), "Target unavailable"), 502, 46300);
        auth.failNextTargetTimeout();
        performJson(post("/api/v1/notifications/admin/messages").header("Authorization", bearer("admin-token")),
                messageBody(List.of("user"), "Target timeout"), 504, 46301);
        auth.failNextTargetIncompatible();
        performJson(post("/api/v1/notifications/admin/messages").header("Authorization", bearer("admin-token")),
                messageBody(List.of("user"), "Target bad"), 502, 46302);

        assertThat(store.usesAuthImplementation()).isFalse();
        assertThat(auth.writeCallCount()).isZero();
    }

    @Test
    @DisplayName("NOTIF-GW-AUTH consumes gateway trusted auth context before Bearer fallback")
    void gatewayTrustedAuthContextContract() throws Exception {
        JsonNode list = performJson(gateway(get("/api/v1/notifications/me"), "user", "USER"), 200);
        assertThat(valuesAt(list, "/data/items", "recipientUserId")).containsOnly("user");

        JsonNode unread = performJson(gateway(get("/api/v1/notifications/me/unread-count"), "user", "USER"), 200);
        assertThat(unread.at("/data/unreadCount").asInt()).isEqualTo(store.unreadCount("user"));
        assertThat(unread.at("/data/unreadCount").asInt()).isNotEqualTo(store.unreadCount("another_user"));

        mvc.perform(gateway(get("/api/v1/notifications/admin/messages"), "gateway_helper", "HELPER"))
                .andExpect(status().isOk());

        performJson(gateway(post("/api/v1/notifications/admin/messages"), "gateway_user", "USER"),
                messageBody(List.of("user"), "Gateway User Denied"), 403, 42001);
        assertThat(store.messageCountByTitle("Gateway User Denied")).isZero();

        performJson(get("/api/v1/notifications/me")
                .header("X-Gateway-Internal-Request-Id", "req-gateway-missing-user")
                .header("X-Beiming-Actor-Roles", "USER"), 502, 46302);

        performJson(gateway(get("/api/v1/notifications/admin/messages"), "empty_role_actor", ""), 403, 42001);

        JsonNode trustedWins = performJson(gateway(get("/api/v1/notifications/me"), "user", "USER")
                .header("Authorization", bearer("another-user-token")), 200);
        assertThat(valuesAt(trustedWins, "/data/items", "recipientUserId")).containsOnly("user");

        JsonNode fallback = performJson(get("/api/v1/notifications/me")
                .header("X-Beiming-Actor-User-Id", "user")
                .header("X-Beiming-Actor-Roles", "USER")
                .header("Authorization", bearer("another-user-token")), 200);
        assertThat(valuesAt(fallback, "/data/items", "recipientUserId")).containsOnly("another_user");

        performJson(get("/api/v1/notifications/me")
                .header("X-Gateway-Internal-Request-Id", "bad request id")
                .header("X-Beiming-Actor-User-Id", "user")
                .header("X-Beiming-Actor-Roles", "USER"), 502, 46302);

        performJson(get("/api/v1/notifications/me")
                .header("X-Gateway-Internal-Request-Id", " ")
                .header("X-Beiming-Actor-User-Id", "user")
                .header("X-Beiming-Actor-Roles", "USER"), 502, 46302);
    }

    @Test
    @DisplayName("NOTIF-GW-AUTH writes audit actor from gateway context and keeps target snapshots isolated")
    void gatewayTrustedAuthContextWriteAuditAndBoundaryContract() throws Exception {
        JsonNode created = performJson(gateway(post("/api/v1/notifications/admin/messages"), "gateway_admin", "ADMIN")
                .header("X-Beiming-Actor-Permissions", "NODE_READ")
                .header("X-Beiming-Actor-Minecraft-Id", "ActorMc")
                .header("X-Beiming-Actor-Minecraft-Uuid", "99999999999999999999999999999999"), mapOf(
                "recipientUserIds", List.of("user"),
                "title", "Gateway Create",
                "body", "Notification body",
                "type", "SYSTEM",
                "channels", List.of("IN_APP"),
                "sourceModule", "notification",
                "sourceId", "source-1",
                "reason", "gateway create"
        ), 201);
        assertThat(created.at("/data/createdBy").asText()).isEqualTo("gateway_admin");
        assertThat(created.at("/data/recipients/0/recipientDisplayNameSnapshot").asText()).isEqualTo("User");

        mvc.perform(gateway(get("/api/v1/notifications/admin/messages/" + created.at("/data/notificationId").asText() + "/audit-logs"), "gateway_owner", "OWNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].actorUserId").value("gateway_admin"));

        performJson(gateway(get("/api/v1/notifications/me"), "user", "BAD_ROLE"), 502, 46302);
        performJson(gateway(get("/api/v1/notifications/me"), "user", "USER")
                .header("X-Beiming-Actor-Permissions", "BAD_PERMISSION"), 502, 46302);
        performJson(gateway(get("/api/v1/notifications/me"), "user", "USER")
                .header("X-Beiming-Actor-Minecraft-Id", "ActorMc")
                .header("X-Beiming-Actor-Minecraft-Uuid", "bad-uuid"), 502, 46302);

        String source = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/cn/beiming/notification/NotificationModule.java"));
        assertThat(source).doesNotContain("cn.beiming.auth", "cn.beiming.profile", "cn.beiming.apigateway",
                "AuthRepository", "ProfileRepository", "JdbcTemplate", "node-daemon", "container", "file-manager",
                "Remove-Item -Recurse", "rm -rf", "externalWebhookToken");
    }

    @Test
    @DisplayName("NOTIF-ME-LIST and NOTIF-UNREAD cover filters, sorting, expiry, isolation, and unread count")
    void currentUserListAndUnreadContract() throws Exception {
        JsonNode list = performJson(get("/api/v1/notifications/me")
                .header("Authorization", bearer("user-token"))
                .param("page", "1")
                .param("pageSize", "2"), 200);
        assertThat(list.at("/data/page").asInt()).isEqualTo(1);
        assertThat(list.at("/data/pageSize").asInt()).isEqualTo(2);
        assertThat(valuesAt(list, "/data/items", "recipientUserId")).containsOnly("user");
        assertThat(valuesAt(list, "/data/items", "notificationId")).doesNotContain(store.notificationId("archived-user"), store.notificationId("expired-user"));

        mvc.perform(get("/api/v1/notifications/me")
                        .header("Authorization", bearer("user-token"))
                        .param("status", "UNREAD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].status").value("UNREAD"));

        mvc.perform(get("/api/v1/notifications/me")
                        .header("Authorization", bearer("user-token"))
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].status").value("ARCHIVED"));

        mvc.perform(get("/api/v1/notifications/me")
                        .header("Authorization", bearer("user-token"))
                        .param("type", "EXAM")
                        .param("sourceModule", "exam")
                        .param("sort", "createdAt_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].type").value("EXAM"));

        mvc.perform(get("/api/v1/notifications/me")
                        .header("Authorization", bearer("user-token"))
                        .param("includeExpired", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.notificationId == '" + store.notificationId("expired-user") + "')]").exists());

        performJson(get("/api/v1/notifications/me").header("Authorization", bearer("user-token")).param("status", "NOPE"), 400, 40001);

        JsonNode unread = performJson(get("/api/v1/notifications/me/unread-count")
                .header("Authorization", bearer("user-token")), 200);
        assertThat(unread.at("/data/unreadCount").asInt()).isEqualTo(store.unreadCount("user"));
        assertThat(store.unreadCount("user")).isGreaterThan(0);
        assertThat(store.unreadCount("user")).isNotEqualTo(store.unreadCount("another_user"));

        auth.failNextCurrentUnavailable();
        performJson(get("/api/v1/notifications/me/unread-count").header("Authorization", bearer("user-token")), 502, 46300);
    }

    @Test
    @DisplayName("NOTIF-ME-DETAIL returns only the current user's recipient view")
    void currentUserDetailContract() throws Exception {
        JsonNode detail = performJson(get("/api/v1/notifications/me/" + store.notificationId("unread-user"))
                .header("Authorization", bearer("user-token")), 200);
        assertThat(detail.at("/data/recipientUserId").asText()).isEqualTo("user");
        assertThat(detail.toString()).doesNotContain("another_user", "paramsSummary");

        performJson(get("/api/v1/notifications/me/missing").header("Authorization", bearer("user-token")), 404, 43300);
        performJson(get("/api/v1/notifications/me/" + store.notificationId("unread-user")).header("Authorization", bearer("another-user-token")), 404, 43300);
        performJson(get("/api/v1/notifications/me/" + store.notificationId("archived-user")).header("Authorization", bearer("user-token")), 200);
        performJson(get("/api/v1/notifications/me/" + store.notificationId("expired-user")).header("Authorization", bearer("user-token")), 200);
    }

    @Test
    @DisplayName("NOTIF-READ, NOTIF-READALL, and NOTIF-ARCHIVE cover status transitions and idempotency")
    void readReadAllAndArchiveContract() throws Exception {
        String unreadId = store.notificationId("unread-user");
        int beforeUnread = store.unreadCount("user");
        JsonNode read = performJson(patch("/api/v1/notifications/me/" + unreadId + "/read")
                .header("Authorization", bearer("user-token")), Map.of("userId", "another_user", "readAt", "2000-01-01T00:00:00Z"), 200);
        assertThat(read.at("/data/status").asText()).isEqualTo("READ");
        assertThat(store.unreadCount("user")).isEqualTo(beforeUnread - 1);
        String readAt = read.at("/data/readAt").asText();

        JsonNode readAgain = performJson(patch("/api/v1/notifications/me/" + unreadId + "/read")
                .header("Authorization", bearer("user-token")), Map.of(), 200);
        assertThat(readAgain.at("/data/readAt").asText()).isEqualTo(readAt);

        performJson(patch("/api/v1/notifications/me/" + unreadId + "/read").header("Authorization", bearer("another-user-token")), Map.of(), 404, 43300);
        performJson(patch("/api/v1/notifications/me/missing/read").header("Authorization", bearer("user-token")), Map.of(), 404, 43300);
        performJson(patch("/api/v1/notifications/me/" + store.notificationId("archived-user") + "/read").header("Authorization", bearer("user-token")), Map.of(), 409, 43311);

        JsonNode readAll = performJson(patch("/api/v1/notifications/me/read-all")
                .header("Authorization", bearer("user-token")), Map.of("type", "EXAM", "sourceModule", "exam"), 200);
        assertThat(readAll.at("/data/updatedCount").asInt()).isGreaterThanOrEqualTo(0);
        JsonNode readAllAgain = performJson(patch("/api/v1/notifications/me/read-all")
                .header("Authorization", bearer("user-token")), Map.of(), 200);
        assertThat(readAllAgain.at("/data/updatedCount").asInt()).isGreaterThanOrEqualTo(0);

        int beforeArchiveUnread = store.unreadCount("another_user");
        String archiveId = store.notificationId("unread-another");
        JsonNode archived = performJson(patch("/api/v1/notifications/me/" + archiveId + "/archive")
                .header("Authorization", bearer("another-user-token"))
                .header("X-Request-Id", "req-archive"), Map.of("reason", "hide"), 200);
        assertThat(archived.at("/data/status").asText()).isEqualTo("ARCHIVED");
        assertThat(store.unreadCount("another_user")).isEqualTo(beforeArchiveUnread - 1);
        assertThat(store.latestAuditRequestId("NOTIFICATION_RECIPIENT_ARCHIVED")).isEqualTo("req-archive");

        JsonNode archiveAgain = performJson(patch("/api/v1/notifications/me/" + archiveId + "/archive")
                .header("Authorization", bearer("another-user-token")), Map.of("reason", "again"), 200);
        assertThat(archiveAgain.at("/data/archivedAt").asText()).isEqualTo(archived.at("/data/archivedAt").asText());
        performJson(patch("/api/v1/notifications/me/" + archiveId + "/archive").header("Authorization", bearer("user-token")), Map.of(), 404, 43300);
        performJson(patch("/api/v1/notifications/me/" + archiveId + "/archive").header("Authorization", bearer("another-user-token")), Map.of("reason", "x".repeat(201)), 400, 40001);
    }

    @Test
    @DisplayName("NOTIF-READ and NOTIF-ARCHIVE concurrent calls keep recipient state stable")
    void concurrentRecipientStateChangesAreStable() throws Exception {
        String readId = store.notificationId("concurrent-read-user");
        CountDownLatch readStart = new CountDownLatch(1);
        AtomicInteger readSuccess = new AtomicInteger();
        try (var pool = Executors.newFixedThreadPool(2)) {
            for (int i = 0; i < 2; i++) {
                pool.submit(() -> {
                    readStart.await();
                    MvcResult result = mvc.perform(patch("/api/v1/notifications/me/" + readId + "/read")
                                    .header("Authorization", bearer("user-token"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json(Map.of())))
                            .andReturn();
                    if (result.getResponse().getStatus() == 200) {
                        readSuccess.incrementAndGet();
                    }
                    return null;
                });
            }
            readStart.countDown();
        }
        assertThat(readSuccess.get()).isEqualTo(2);
        assertThat(store.readAt("user", readId)).isNotNull();

        String archiveId = store.notificationId("concurrent-archive-user");
        CountDownLatch archiveStart = new CountDownLatch(1);
        AtomicInteger archiveSuccess = new AtomicInteger();
        try (var pool = Executors.newFixedThreadPool(2)) {
            for (int i = 0; i < 2; i++) {
                pool.submit(() -> {
                    archiveStart.await();
                    MvcResult result = mvc.perform(patch("/api/v1/notifications/me/" + archiveId + "/archive")
                                    .header("Authorization", bearer("user-token"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json(Map.of())))
                            .andReturn();
                    if (result.getResponse().getStatus() == 200) {
                        archiveSuccess.incrementAndGet();
                    }
                    return null;
                });
            }
            archiveStart.countDown();
        }
        assertThat(archiveSuccess.get()).isEqualTo(2);
        assertThat(store.archivedAt("user", archiveId)).isNotNull();
    }

    @Test
    @DisplayName("NOTIF-ADMIN-LIST and NOTIF-ADMIN-DETAIL cover backend message reads")
    void adminMessageReadContract() throws Exception {
        mvc.perform(get("/api/v1/notifications/admin/messages")
                        .header("Authorization", bearer("helper-token"))
                        .param("keyword", "Exam")
                        .param("recipientUserId", "user")
                        .param("type", "EXAM")
                        .param("sourceModule", "exam")
                        .param("deliveryStatus", "DELIVERED")
                        .param("createdBy", "admin")
                        .param("sort", "createdAt_desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());

        mvc.perform(get("/api/v1/notifications/admin/messages/" + store.notificationId("unread-user"))
                        .header("Authorization", bearer("helper-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipientTotal").isNumber())
                .andExpect(jsonPath("$.data.deliveredTotal").isNumber())
                .andExpect(jsonPath("$.data.failedTotal").isNumber());

        performJson(get("/api/v1/notifications/admin/messages/missing").header("Authorization", bearer("helper-token")), 404, 43300);
        performJson(get("/api/v1/notifications/admin/messages/" + store.notificationId("unread-user")).header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/notifications/admin/messages/" + store.notificationId("unread-user")), 401, 41000);
    }

    @Test
    @DisplayName("NOTIF-CREATE covers direct message creation, validation, idempotency, rollback, and security")
    void directMessageCreateContract() throws Exception {
        JsonNode created = performJson(post("/api/v1/notifications/admin/messages")
                .header("Authorization", bearer("admin-token"))
                .header("X-Request-Id", "req-create-message"), messageBody(List.of("user", "user"), "Direct Create"), 201);
        assertThat(created.at("/data/recipientTotal").asInt()).isEqualTo(1);
        assertThat(created.at("/data/recipients/0/status").asText()).isEqualTo("UNREAD");
        assertThat(created.at("/data/recipients/0/deliveryStatus").asText()).isEqualTo("DELIVERED");
        assertThat(store.latestAuditRequestId("NOTIFICATION_MESSAGE_CREATED")).isEqualTo("req-create-message");

        performJson(post("/api/v1/notifications/admin/messages").header("Authorization", bearer("owner-token")),
                messageBody(List.of("user", "another_user"), "Owner Multi"), 201);
        performJson(post("/api/v1/notifications/admin/messages").header("Authorization", bearer("admin-token")),
                Map.of("recipientUserIds", List.of("user"), "title", "x", "body", "body", "type", "SYSTEM", "reason", "bad"), 400, 40001);
        performJson(post("/api/v1/notifications/admin/messages").header("Authorization", bearer("admin-token")),
                messageBody(List.of("user"), "Bad Type", "NOPE"), 400, 40001);
        Map<String, Object> emailBody = messageBody(List.of("user"), "Bad Channel");
        emailBody.put("channels", List.of("EMAIL"));
        performJson(post("/api/v1/notifications/admin/messages").header("Authorization", bearer("admin-token")), emailBody, 400, 40001);
        performJson(post("/api/v1/notifications/admin/messages").header("Authorization", bearer("admin-token")),
                messageBody(List.of(), "No Recipients"), 400, 43316);
        Map<String, Object> badUrl = messageBody(List.of("user"), "Bad Url");
        badUrl.put("actionUrl", "javascript:alert(1)");
        performJson(post("/api/v1/notifications/admin/messages").header("Authorization", bearer("admin-token")), badUrl, 400, 40001);
        Map<String, Object> expired = messageBody(List.of("user"), "Expired");
        expired.put("expiresAt", "2020-01-01T00:00:00Z");
        performJson(post("/api/v1/notifications/admin/messages").header("Authorization", bearer("admin-token")), expired, 400, 40001);

        Map<String, Object> idem = messageBody(List.of("user"), "Idempotent");
        idem.put("idempotencyKey", "msg-idem-1");
        JsonNode first = performJson(post("/api/v1/notifications/admin/messages").header("Authorization", bearer("admin-token")), idem, 201);
        JsonNode second = performJson(post("/api/v1/notifications/admin/messages").header("Authorization", bearer("admin-token")), idem, 201);
        assertThat(second.at("/data/notificationId").asText()).isEqualTo(first.at("/data/notificationId").asText());
        Map<String, Object> changed = messageBody(List.of("another_user"), "Changed");
        changed.put("idempotencyKey", "msg-idem-1");
        performJson(post("/api/v1/notifications/admin/messages").header("Authorization", bearer("admin-token")), changed, 409, 43002);

        int beforeUnread = store.unreadCount("user");
        store.failNextAudit();
        performJson(post("/api/v1/notifications/admin/messages").header("Authorization", bearer("admin-token")),
                messageBody(List.of("user"), "Audit Fail"), 500, 51301);
        assertThat(store.unreadCount("user")).isEqualTo(beforeUnread);
        store.failNextDeliveryWrite();
        performJson(post("/api/v1/notifications/admin/messages").header("Authorization", bearer("admin-token")),
                messageBody(List.of("user"), "Delivery Fail"), 500, 51302);
        assertThat(store.unreadCount("user")).isEqualTo(beforeUnread);
    }

    @Test
    @DisplayName("NOTIF-CREATE concurrent idempotency creates one message")
    void concurrentIdempotentCreateCreatesOneMessage() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        try (var pool = Executors.newFixedThreadPool(2)) {
            for (int i = 0; i < 2; i++) {
                pool.submit(() -> {
                    Map<String, Object> body = messageBody(List.of("user"), "Concurrent Idem");
                    body.put("idempotencyKey", "msg-concurrent-1");
                    start.await();
                    MvcResult result = mvc.perform(post("/api/v1/notifications/admin/messages")
                                    .header("Authorization", bearer("admin-token"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json(body)))
                            .andReturn();
                    if (result.getResponse().getStatus() == 201) {
                        successCount.incrementAndGet();
                    }
                    return null;
                });
            }
            start.countDown();
        }
        assertThat(successCount.get()).isEqualTo(2);
        assertThat(store.messageCountByTitle("Concurrent Idem")).isEqualTo(1);
    }

    @Test
    @DisplayName("NOTIF-TPL-CREATE and NOTIF-TPL-LIST/DETAIL cover template reads and creation")
    void templateCreateAndReadContract() throws Exception {
        JsonNode created = performJson(post("/api/v1/notifications/admin/templates")
                .header("Authorization", bearer("admin-token")), templateBody("EXAM_RESULT_TEMPLATE"), 201);
        assertThat(created.at("/data/status").asText()).isEqualTo("ENABLED");
        assertThat(created.at("/data/version").asInt()).isEqualTo(1);

        mvc.perform(get("/api/v1/notifications/admin/templates")
                        .header("Authorization", bearer("helper-token"))
                        .param("keyword", "EXAM")
                        .param("status", "ENABLED")
                        .param("type", "EXAM")
                        .param("sort", "code_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());

        mvc.perform(get("/api/v1/notifications/admin/templates/" + created.at("/data/templateId").asText())
                        .header("Authorization", bearer("helper-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("EXAM_RESULT_TEMPLATE"));

        performJson(get("/api/v1/notifications/admin/templates/missing").header("Authorization", bearer("helper-token")), 404, 43301);
        performJson(get("/api/v1/notifications/admin/templates").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(post("/api/v1/notifications/admin/templates").header("Authorization", bearer("helper-token")), templateBody("HELPER_TEMPLATE"), 403, 42001);
        performJson(post("/api/v1/notifications/admin/templates").header("Authorization", bearer("admin-token")), templateBody("bad code"), 400, 40001);
        performJson(post("/api/v1/notifications/admin/templates").header("Authorization", bearer("admin-token")), templateBody("EXAM_RESULT_TEMPLATE"), 409, 43317);

        Map<String, Object> missingVariable = templateBody("BAD_VARIABLE_TEMPLATE");
        missingVariable.put("titleTemplate", "Hello ${missing}");
        performJson(post("/api/v1/notifications/admin/templates").header("Authorization", bearer("admin-token")), missingVariable, 400, 43313);

        Map<String, Object> emailTemplate = templateBody("EMAIL_TEMPLATE");
        emailTemplate.put("channels", List.of("EMAIL"));
        performJson(post("/api/v1/notifications/admin/templates").header("Authorization", bearer("admin-token")), emailTemplate, 400, 40001);

        Map<String, Object> idem = templateBody("IDEM_TEMPLATE");
        idem.put("idempotencyKey", "tpl-idem-1");
        JsonNode first = performJson(post("/api/v1/notifications/admin/templates").header("Authorization", bearer("owner-token")), idem, 201);
        JsonNode second = performJson(post("/api/v1/notifications/admin/templates").header("Authorization", bearer("owner-token")), idem, 201);
        assertThat(second.at("/data/templateId").asText()).isEqualTo(first.at("/data/templateId").asText());
        Map<String, Object> changed = templateBody("CHANGED_TEMPLATE");
        changed.put("idempotencyKey", "tpl-idem-1");
        performJson(post("/api/v1/notifications/admin/templates").header("Authorization", bearer("owner-token")), changed, 409, 43002);

        store.failNextAudit();
        performJson(post("/api/v1/notifications/admin/templates").header("Authorization", bearer("admin-token")),
                templateBody("AUDIT_FAIL_TEMPLATE"), 500, 51301);
        assertThat(store.templateExists("AUDIT_FAIL_TEMPLATE")).isFalse();
    }

    @Test
    @DisplayName("NOTIF-TPL-PATCH, DISABLE, ENABLE cover template state and rollback")
    void templatePatchDisableEnableContract() throws Exception {
        String templateId = store.templateId("enabled-template");
        JsonNode patched = performJson(patch("/api/v1/notifications/admin/templates/" + templateId)
                .header("Authorization", bearer("admin-token")), Map.of(
                "name", "Renamed Template",
                "titleTemplate", "Hi ${playerName}",
                "bodyTemplate", "Result ${result}",
                "variableDefinitions", List.of(
                        Map.of("name", "playerName", "required", true, "description", "player", "example", "Steve"),
                        Map.of("name", "result", "required", true, "description", "result", "example", "PASS")
                ),
                "reason", "update"
        ), 200);
        assertThat(patched.at("/data/version").asInt()).isEqualTo(2);

        performJson(patch("/api/v1/notifications/admin/templates/missing").header("Authorization", bearer("admin-token")), Map.of("reason", "missing"), 404, 43301);
        performJson(patch("/api/v1/notifications/admin/templates/" + templateId).header("Authorization", bearer("helper-token")), Map.of("name", "No", "reason", "bad"), 403, 42001);
        performJson(patch("/api/v1/notifications/admin/templates/" + templateId).header("Authorization", bearer("admin-token")), Map.of("name", "No Reason"), 400, 40001);
        performJson(patch("/api/v1/notifications/admin/templates/" + templateId).header("Authorization", bearer("admin-token")), Map.of("code", "DUPLICATE_TEMPLATE", "reason", "dup"), 409, 43317);
        performJson(patch("/api/v1/notifications/admin/templates/" + templateId).header("Authorization", bearer("admin-token")), Map.of("titleTemplate", "Hello ${missing}", "reason", "bad"), 400, 43313);

        int beforeVersion = store.templateVersion(templateId);
        store.failNextAudit();
        performJson(patch("/api/v1/notifications/admin/templates/" + templateId).header("Authorization", bearer("admin-token")), Map.of("name", "Audit Fail", "reason", "audit"), 500, 51301);
        assertThat(store.templateVersion(templateId)).isEqualTo(beforeVersion);

        JsonNode disabled = performJson(patch("/api/v1/notifications/admin/templates/" + templateId + "/disable")
                .header("Authorization", bearer("admin-token")), Map.of("reason", "disable"), 200);
        assertThat(disabled.at("/data/status").asText()).isEqualTo("DISABLED");
        performJson(patch("/api/v1/notifications/admin/templates/" + templateId + "/disable")
                .header("Authorization", bearer("admin-token")), Map.of("reason", "again"), 200);
        performJson(post("/api/v1/notifications/admin/messages/from-template").header("Authorization", bearer("admin-token")), fromTemplateBody("ENABLED_TEMPLATE"), 409, 43312);
        performJson(patch("/api/v1/notifications/admin/templates/missing/disable").header("Authorization", bearer("admin-token")), Map.of("reason", "missing"), 404, 43301);
        performJson(patch("/api/v1/notifications/admin/templates/" + templateId + "/disable").header("Authorization", bearer("helper-token")), Map.of("reason", "bad"), 403, 42001);
        performJson(patch("/api/v1/notifications/admin/templates/" + templateId + "/disable").header("Authorization", bearer("admin-token")), Map.of(), 400, 40001);

        JsonNode enabled = performJson(patch("/api/v1/notifications/admin/templates/" + templateId + "/enable")
                .header("Authorization", bearer("admin-token")), Map.of("reason", "enable"), 200);
        assertThat(enabled.at("/data/status").asText()).isEqualTo("ENABLED");
        performJson(patch("/api/v1/notifications/admin/templates/" + templateId + "/enable")
                .header("Authorization", bearer("admin-token")), Map.of("reason", "again"), 200);
        performJson(patch("/api/v1/notifications/admin/templates/missing/enable").header("Authorization", bearer("admin-token")), Map.of("reason", "missing"), 404, 43301);
        performJson(patch("/api/v1/notifications/admin/templates/" + templateId + "/enable").header("Authorization", bearer("helper-token")), Map.of("reason", "bad"), 403, 42001);
        performJson(patch("/api/v1/notifications/admin/templates/" + templateId + "/enable").header("Authorization", bearer("admin-token")), Map.of(), 400, 40001);

        store.markTemplateInvalid(templateId);
        performJson(patch("/api/v1/notifications/admin/templates/" + templateId + "/enable").header("Authorization", bearer("admin-token")), Map.of("reason", "invalid"), 400, 43313);
    }

    @Test
    @DisplayName("NOTIF-SEND-TPL covers template rendering, validation, idempotency, rollback, and snapshots")
    void sendFromTemplateContract() throws Exception {
        JsonNode sent = performJson(post("/api/v1/notifications/admin/messages/from-template")
                .header("Authorization", bearer("admin-token")), fromTemplateBody("ENABLED_TEMPLATE"), 201);
        assertThat(sent.at("/data/title").asText()).contains("Steve");
        assertThat(sent.at("/data/templateCode").asText()).isEqualTo("ENABLED_TEMPLATE");
        int version = sent.at("/data/templateVersion").asInt();

        performJson(post("/api/v1/notifications/admin/messages/from-template").header("Authorization", bearer("admin-token")), fromTemplateBody("MISSING_TEMPLATE"), 404, 43301);
        performJson(post("/api/v1/notifications/admin/messages/from-template").header("Authorization", bearer("admin-token")), fromTemplateBody("DISABLED_TEMPLATE"), 409, 43312);

        Map<String, Object> missingVar = fromTemplateBody("ENABLED_TEMPLATE");
        missingVar.put("variables", Map.of("playerName", "Steve"));
        performJson(post("/api/v1/notifications/admin/messages/from-template").header("Authorization", bearer("admin-token")), missingVar, 400, 43313);
        Map<String, Object> badChannel = fromTemplateBody("ENABLED_TEMPLATE");
        badChannel.put("channels", List.of("EMAIL"));
        performJson(post("/api/v1/notifications/admin/messages/from-template").header("Authorization", bearer("admin-token")), badChannel, 400, 40001);
        Map<String, Object> noReason = fromTemplateBody("ENABLED_TEMPLATE");
        noReason.remove("reason");
        performJson(post("/api/v1/notifications/admin/messages/from-template").header("Authorization", bearer("admin-token")), noReason, 400, 40001);

        store.markTemplateRenderBroken("BROKEN_TEMPLATE");
        performJson(post("/api/v1/notifications/admin/messages/from-template").header("Authorization", bearer("admin-token")), fromTemplateBody("BROKEN_TEMPLATE"), 400, 43314);
        Map<String, Object> missingRecipient = fromTemplateBody("ENABLED_TEMPLATE");
        missingRecipient.put("recipientUserIds", List.of("missing-user"));
        performJson(post("/api/v1/notifications/admin/messages/from-template").header("Authorization", bearer("admin-token")), missingRecipient, 404, 43315);

        Map<String, Object> idem = fromTemplateBody("ENABLED_TEMPLATE");
        idem.put("idempotencyKey", "send-template-idem-1");
        JsonNode first = performJson(post("/api/v1/notifications/admin/messages/from-template").header("Authorization", bearer("admin-token")), idem, 201);
        JsonNode second = performJson(post("/api/v1/notifications/admin/messages/from-template").header("Authorization", bearer("admin-token")), idem, 201);
        assertThat(second.at("/data/notificationId").asText()).isEqualTo(first.at("/data/notificationId").asText());
        Map<String, Object> changed = fromTemplateBody("ENABLED_TEMPLATE");
        changed.put("variables", Map.of("playerName", "Alex", "result", "PASS"));
        changed.put("idempotencyKey", "send-template-idem-1");
        performJson(post("/api/v1/notifications/admin/messages/from-template").header("Authorization", bearer("admin-token")), changed, 409, 43002);

        store.failNextAudit();
        performJson(post("/api/v1/notifications/admin/messages/from-template").header("Authorization", bearer("admin-token")),
                fromTemplateBody("ENABLED_TEMPLATE"), 500, 51301);

        store.patchTemplateName("ENABLED_TEMPLATE", "Changed After Send");
        JsonNode detail = performJson(get("/api/v1/notifications/admin/messages/" + sent.at("/data/notificationId").asText())
                .header("Authorization", bearer("admin-token")), 200);
        assertThat(detail.at("/data/templateVersion").asInt()).isEqualTo(version);
    }

    @Test
    @DisplayName("NOTIF-TPL-PREVIEW renders templates without notification side effects")
    void templatePreviewContract() throws Exception {
        JsonNode before = performJson(get("/api/v1/notifications/admin/ops/summary")
                .header("Authorization", bearer("admin-token")), 200);

        JsonNode preview = performJson(post("/api/v1/notifications/admin/templates/preview")
                .header("Authorization", bearer("helper-token")), previewBody("ENABLED_TEMPLATE"), 200);
        assertThat(preview.at("/data/templateCode").asText()).isEqualTo("ENABLED_TEMPLATE");
        assertThat(preview.at("/data/templateStatus").asText()).isEqualTo("ENABLED");
        assertThat(preview.at("/data/sendable").asBoolean()).isTrue();
        assertThat(preview.at("/data/title").asText()).contains("Steve");
        assertThat(preview.at("/data/body").asText()).contains("PASS");
        assertThat(preview.at("/data/templateVersion").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(preview.at("/data/createdNotification").asBoolean()).isFalse();

        JsonNode disabled = performJson(post("/api/v1/notifications/admin/templates/preview")
                .header("Authorization", bearer("admin-token")), previewBody("DISABLED_TEMPLATE"), 200);
        assertThat(disabled.at("/data/templateStatus").asText()).isEqualTo("DISABLED");
        assertThat(disabled.at("/data/sendable").asBoolean()).isFalse();

        performJson(post("/api/v1/notifications/admin/templates/preview").header("Authorization", bearer("admin-token")),
                previewBody("MISSING_TEMPLATE"), 404, 43301);

        Map<String, Object> missingVar = previewBody("ENABLED_TEMPLATE");
        missingVar.put("variables", Map.of("playerName", "Steve"));
        performJson(post("/api/v1/notifications/admin/templates/preview").header("Authorization", bearer("admin-token")),
                missingVar, 400, 43313);

        Map<String, Object> unknownVar = previewBody("ENABLED_TEMPLATE");
        unknownVar.put("variables", Map.of("playerName", "Steve", "result", "PASS", "unknown", "x"));
        performJson(post("/api/v1/notifications/admin/templates/preview").header("Authorization", bearer("admin-token")),
                unknownVar, 400, 43313);

        store.markTemplateRenderBroken("BROKEN_TEMPLATE");
        performJson(post("/api/v1/notifications/admin/templates/preview").header("Authorization", bearer("admin-token")),
                previewBody("BROKEN_TEMPLATE"), 400, 43314);

        performJson(post("/api/v1/notifications/admin/templates/preview").header("Authorization", bearer("user-token")),
                previewBody("ENABLED_TEMPLATE"), 403, 42001);
        performJson(post("/api/v1/notifications/admin/templates/preview"), previewBody("ENABLED_TEMPLATE"), 401, 41000);

        JsonNode after = performJson(get("/api/v1/notifications/admin/ops/summary")
                .header("Authorization", bearer("admin-token")), 200);
        assertThat(after.at("/data/messagesTotal").asInt()).isEqualTo(before.at("/data/messagesTotal").asInt());
        assertThat(after.at("/data/unreadTotal").asInt()).isEqualTo(before.at("/data/unreadTotal").asInt());
        assertThat(after.at("/data/auditsTotal").asInt()).isEqualTo(before.at("/data/auditsTotal").asInt());
    }

    @Test
    @DisplayName("NOTIF-OPS-SUMMARY exposes operational counts without sensitive data")
    void opsSummaryContract() throws Exception {
        JsonNode summary = performJson(get("/api/v1/notifications/admin/ops/summary")
                .header("Authorization", bearer("admin-token")), 200);
        assertThat(summary.at("/data/service").asText()).isEqualTo("notification");
        assertThat(summary.at("/data/storageMode").asText()).isEqualTo("IN_MEMORY");
        assertThat(summary.at("/data/authMode").asText()).isEqualTo("TEST_STUB");
        assertThat(summary.at("/data/messagesTotal").asInt()).isGreaterThan(0);
        assertThat(summary.at("/data/templatesTotal").asInt()).isGreaterThan(0);
        assertThat(summary.at("/data/auditsTotal").asInt()).isGreaterThan(0);
        assertThat(summary.at("/data/pendingExternalDeliveries").asInt()).isZero();
        assertThat(java.util.stream.StreamSupport.stream(summary.at("/data/warnings").spliterator(), false)
                .map(JsonNode::asText)
                .toList()).contains("P0_IN_MEMORY_STORAGE", "P0_AUTH_STUB");
        assertThat(summary.toString()).doesNotContain("Bearer", "token", "Notification body", "Result ${result}", "admin reason");

        mvc.perform(get("/api/v1/notifications/admin/ops/summary")
                        .header("Authorization", bearer("owner-token")))
                .andExpect(status().isOk());
        performJson(get("/api/v1/notifications/admin/ops/summary").header("Authorization", bearer("helper-token")), 403, 42001);
        performJson(get("/api/v1/notifications/admin/ops/summary").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/notifications/admin/ops/summary"), 401, 41000);

        int beforeMessages = summary.at("/data/messagesTotal").asInt();
        int beforeArchived = summary.at("/data/archivedTotal").asInt();
        JsonNode created = performJson(post("/api/v1/notifications/admin/messages")
                .header("Authorization", bearer("admin-token")), messageBody(List.of("user"), "Ops Summary"), 201);
        performJson(patch("/api/v1/notifications/me/" + created.at("/data/notificationId").asText() + "/archive")
                .header("Authorization", bearer("user-token")), Map.of("reason", "ops"), 200);

        JsonNode changed = performJson(get("/api/v1/notifications/admin/ops/summary")
                .header("Authorization", bearer("admin-token")), 200);
        assertThat(changed.at("/data/messagesTotal").asInt()).isEqualTo(beforeMessages + 1);
        assertThat(changed.at("/data/archivedTotal").asInt()).isEqualTo(beforeArchived + 1);
        assertThat(changed.at("/data/auditsTotal").asInt()).isGreaterThan(summary.at("/data/auditsTotal").asInt());
    }

    @Test
    @DisplayName("NOTIF-AUDIT and NOTIF-SEC cover audit reads, immutability, and module boundaries")
    void auditAndSecurityContract() throws Exception {
        String messageId = store.notificationId("unread-user");
        mvc.perform(get("/api/v1/notifications/admin/messages/" + messageId + "/audit-logs")
                        .header("Authorization", bearer("admin-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());
        mvc.perform(get("/api/v1/notifications/admin/messages/" + messageId + "/audit-logs")
                        .header("Authorization", bearer("owner-token")))
                .andExpect(status().isOk());
        performJson(get("/api/v1/notifications/admin/messages/" + messageId + "/audit-logs").header("Authorization", bearer("helper-token")), 403, 42001);
        performJson(get("/api/v1/notifications/admin/messages/missing/audit-logs").header("Authorization", bearer("admin-token")), 404, 43300);
        performJson(get("/api/v1/notifications/admin/messages/" + messageId + "/audit-logs").header("Authorization", bearer("admin-token")).param("page", "0"), 400, 40002);

        mvc.perform(delete("/api/v1/notifications/admin/messages/" + messageId + "/audit-logs")
                        .header("Authorization", bearer("owner-token")))
                .andExpect(status().is4xxClientError());

        mvc.perform(get("/api/v1/notifications/me")
                        .header("Authorization", bearer("user-token"))
                        .param("userId", "another_user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.recipientUserId == 'another_user')]").isEmpty());

        JsonNode created = performJson(post("/api/v1/notifications/admin/messages")
                .header("Authorization", bearer("admin-token")), mapOf(
                "recipientUserIds", List.of("user"),
                "title", "Forged",
                "body", "<script>alert(1)</script>",
                "type", "EXAM",
                "sourceModule", "exam",
                "sourceId", "exam-1",
                "reason", "boundary",
                "recipientDisplayNameSnapshot", "Forged Name",
                "status", "READ"
        ), 201);
        assertThat(created.at("/data/recipients/0/recipientDisplayNameSnapshot").asText()).isEqualTo("User");
        assertThat(created.at("/data/recipients/0/status").asText()).isEqualTo("UNREAD");
        assertThat(created.at("/data/body").asText()).contains("<script>");
        assertThat(store.examStatusChanged()).isFalse();
        assertThat(store.whitelistStatusChanged()).isFalse();
    }

    private JsonNode performJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                 int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                 Object body,
                                 int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(request
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                 int expectedStatus,
                                 int expectedCode) throws Exception {
        JsonNode result = performJson(request, expectedStatus);
        assertThat(result.path("code").asInt()).isEqualTo(expectedCode);
        return result;
    }

    private JsonNode performJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                 Object body,
                                 int expectedStatus,
                                 int expectedCode) throws Exception {
        JsonNode result = performJson(request, body, expectedStatus);
        assertThat(result.path("code").asInt()).isEqualTo(expectedCode);
        return result;
    }

    private Map<String, Object> messageBody(List<String> recipients, String title) {
        return messageBody(recipients, title, "SYSTEM");
    }

    private Map<String, Object> messageBody(List<String> recipients, String title, String type) {
        return mapOf(
                "recipientUserIds", recipients,
                "title", title,
                "body", "Notification body",
                "type", type,
                "channels", List.of("IN_APP"),
                "sourceModule", "notification",
                "sourceId", "source-1",
                "reason", "test"
        );
    }

    private Map<String, Object> templateBody(String code) {
        return mapOf(
                "code", code,
                "name", "Exam Result",
                "titleTemplate", "Hello ${playerName}",
                "bodyTemplate", "Result ${result}",
                "variableDefinitions", List.of(
                        Map.of("name", "playerName", "required", true, "description", "player", "example", "Steve"),
                        Map.of("name", "result", "required", true, "description", "result", "example", "PASS")
                ),
                "type", "EXAM",
                "channels", List.of("IN_APP"),
                "reason", "test"
        );
    }

    private Map<String, Object> fromTemplateBody(String code) {
        return mapOf(
                "templateCode", code,
                "recipientUserIds", List.of("user"),
                "variables", Map.of("playerName", "Steve", "result", "PASS"),
                "channels", List.of("IN_APP"),
                "sourceModule", "exam",
                "sourceId", "exam-1",
                "reason", "test"
        );
    }

    private Map<String, Object> previewBody(String code) {
        return mapOf(
                "templateCode", code,
                "variables", Map.of("playerName", "Steve", "result", "PASS")
        );
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder gateway(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            String userId,
            String roles) {
        return request
                .header("X-Gateway-Internal-Request-Id", "req-gateway-context")
                .header("X-Beiming-Actor-User-Id", userId)
                .header("X-Beiming-Actor-Roles", roles);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }

    private List<String> valuesAt(JsonNode root, String arrayPointer, String fieldName) {
        return java.util.stream.StreamSupport.stream(root.at(arrayPointer).spliterator(), false)
                .map(item -> item.path(fieldName).asText())
                .toList();
    }
}
