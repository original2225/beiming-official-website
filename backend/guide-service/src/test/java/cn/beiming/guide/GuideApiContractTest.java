package cn.beiming.guide;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class GuideApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("guide local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "GUIDE-COM", 1, 37);
        addRange(mapped, "GUIDE-HOME", 1, 14);
        addRange(mapped, "GUIDE-CAT-PUB", 1, 12);
        addRange(mapped, "GUIDE-PUB-LIST", 1, 32);
        addRange(mapped, "GUIDE-PUB-DETAIL", 1, 24);
        addRange(mapped, "GUIDE-SEARCH", 1, 24);
        addRange(mapped, "GUIDE-CMD", 1, 18);
        addRange(mapped, "GUIDE-CHANNEL-PUB", 1, 16);
        addRange(mapped, "GUIDE-RULE", 1, 24);
        addRange(mapped, "GUIDE-FEEDBACK", 1, 24);
        addRange(mapped, "GUIDE-ADMIN-LIST", 1, 20);
        addRange(mapped, "GUIDE-ADMIN-DETAIL", 1, 12);
        addRange(mapped, "GUIDE-ADMIN-CREATE", 1, 30);
        addRange(mapped, "GUIDE-ADMIN-PATCH", 1, 24);
        addRange(mapped, "GUIDE-STATE", 1, 88);
        addRange(mapped, "GUIDE-VERSION", 1, 30);
        addRange(mapped, "GUIDE-CAT-ADMIN", 1, 52);
        addRange(mapped, "GUIDE-CHANNEL-ADMIN", 1, 70);
        addRange(mapped, "GUIDE-FEEDBACK-ADMIN", 1, 38);
        addRange(mapped, "GUIDE-AUDIT", 1, 16);
        addRange(mapped, "GUIDE-OPS", 1, 14);
        addRange(mapped, "GUIDE-COMPAT", 1, 24);

        assertThat(mapped).hasSize(643);
        assertThat(mapped).contains(
                "GUIDE-COM-001",
                "GUIDE-HOME-014",
                "GUIDE-PUB-LIST-032",
                "GUIDE-STATE-088",
                "GUIDE-CHANNEL-ADMIN-070",
                "GUIDE-COMPAT-024"
        );
    }

    @Test
    @DisplayName("GUIDE-COM covers envelope, request id, auth, roles, dependencies, trusted actor, idempotency, and audit rollback")
    void commonContract() throws Exception {
        mvc.perform(get("/api/v1/guides/home").header("X-Request-Id", "req-guide-home"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-guide-home"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.featuredGuides").isArray());

        JsonNode generated = performJson(get("/api/v1/guides/categories"), 200);
        assertThat(generated.at("/requestId").asText()).isNotBlank();

        performJson(get("/api/v1/guides/articles").param("page", "0"), 400, 40002);
        performJson(get("/api/v1/guides/articles").param("page", "abc"), 400, 40002);
        performJson(get("/api/v1/guides/articles").param("pageSize", "101"), 400, 40002);
        performJson(get("/api/v1/guides/articles").param("sort", "bad"), 400, 40003);
        performJson(post("/api/v1/guides/articles/guide-rules/feedback"), feedback("HELPFUL", "good", "fb-unauth"), 401, 41000);
        performJson(get("/api/v1/guides/admin/articles").header("Authorization", "Token bad"), 401, 41003);
        performJson(get("/api/v1/guides/admin/articles").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/guides/admin/articles").header("Authorization", bearer("helper-token")), 200);
        performJson(post("/api/v1/guides/admin/articles").header("Authorization", bearer("helper-token")), validGuide("helper-create", "helper-create"), 403, 42001);
        performJson(patch("/api/v1/guides/admin/articles/guide-pending/approve").header("Authorization", bearer("helper-token")), review("ok"), 200);
        performJson(post("/api/v1/guides/admin/articles").header("Authorization", bearer("admin-token")), validGuide("created-common", "created-common"), 201);
        performJson(post("/api/v1/guides/admin/articles").header("Authorization", bearer("owner-token")), validGuide("owner-created-common", "owner-created-common"), 201);
        performJson(post("/api/v1/guides/articles/guide-rules/feedback").header("Authorization", bearer("disabled-token")), feedback("HELPFUL", "disabled", "fb-disabled"), 502, 46940);

        performJson(post("/api/v1/guides/articles/guide-rules/feedback").header("Authorization", bearer("auth-unavailable-token")), feedback("HELPFUL", "auth", "fb-auth"), 502, 46940);
        performJson(post("/api/v1/guides/articles/guide-rules/feedback").header("Authorization", bearer("auth-timeout-token")), feedback("HELPFUL", "auth", "fb-auth-timeout"), 504, 46941);
        performJson(post("/api/v1/guides/articles/guide-rules/feedback").header("Authorization", bearer("auth-bad-token")), feedback("HELPFUL", "auth", "fb-auth-bad"), 502, 46942);

        performJson(post("/api/v1/guides/admin/articles")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Profile-Mode", "unavailable"), with(validGuide("profile-down", "profile-down"), "maintainerMemberId", "member-active"), 502, 46950);
        performJson(post("/api/v1/guides/admin/articles")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Profile-Mode", "timeout"), with(validGuide("profile-timeout", "profile-timeout"), "maintainerMemberId", "member-active"), 504, 46951);
        performJson(post("/api/v1/guides/admin/articles")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Profile-Mode", "bad"), with(validGuide("profile-bad", "profile-bad"), "maintainerMemberId", "member-active"), 502, 46952);

        performJson(patch("/api/v1/guides/admin/articles/guide-pending-reject/reject")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Notification-Mode", "unavailable"), review("reject"), 502, 46960);
        performJson(patch("/api/v1/guides/admin/articles/guide-pending-reject/reject")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Notification-Mode", "timeout"), review("reject"), 504, 46961);

        JsonNode degradedResource = performJson(get("/api/v1/guides/articles/guide-resource-ref").header("X-Test-Resource-Mode", "unavailable"), 200);
        assertThat(degradedResource.at("/data/degraded").asBoolean()).isTrue();
        assertThat(degradedResource.toString()).doesNotContain("downloadUrl", "Cloudreve", "sharePassword");

        JsonNode degradedLine = performJson(get("/api/v1/guides/articles/guide-server-address").header("X-Test-Server-Status-Mode", "unavailable"), 200);
        assertThat(degradedLine.at("/data/degraded").asBoolean()).isTrue();
        assertThat(degradedLine.toString()).doesNotContain("onlinePlayers", "motd", "latency");

        JsonNode actorFeedback = performJson(post("/api/v1/guides/articles/guide-rules/feedback")
                .header("Authorization", bearer("user-token"))
                .header("X-Request-Id", "req-guide-actor")
                .header("X-Gateway-Internal-Request-Id", "req-guide-actor")
                .header("X-Beiming-Actor-User-Id", "member"), feedback("HELPFUL", "actor", "fb-actor"), 201);
        assertThat(actorFeedback.at("/data/actorUserId").asText()).isEqualTo("member");

        JsonNode forgedActor = performJson(post("/api/v1/guides/articles/guide-rules/feedback")
                .header("Authorization", bearer("user-token"))
                .header("X-Beiming-Actor-User-Id", "owner"), feedback("HELPFUL", "forged", "fb-forged"), 201);
        assertThat(forgedActor.at("/data/actorUserId").asText()).isEqualTo("user");

        Map<String, Object> create = validGuide("idem-common", "idem-common");
        JsonNode created = performJson(post("/api/v1/guides/admin/articles").header("Authorization", bearer("admin-token")), create, 201);
        JsonNode replay = performJson(post("/api/v1/guides/admin/articles").header("Authorization", bearer("admin-token")), create, 201);
        assertThat(replay.at("/data/guideId").asText()).isEqualTo(created.at("/data/guideId").asText());
        performJson(post("/api/v1/guides/admin/articles").header("Authorization", bearer("admin-token")), validGuide("idem-common-changed", "idem-common"), 409, 43914);

        performJson(post("/api/v1/guides/admin/articles")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Fail-Audit", "true"), validGuide("audit-fail", "audit-fail"), 500, 51901);
    }

    @Test
    @DisplayName("GUIDE-HOME GUIDE-CAT GUIDE-PUB covers public home, categories, lists, details, visibility, degradation, and field isolation")
    void publicReadContract() throws Exception {
        JsonNode home = performJson(get("/api/v1/guides/home"), 200);
        assertThat(home.at("/data/featuredGuides").isArray()).isTrue();
        assertThat(home.at("/data/currentRule/ruleVersion").asText()).isEqualTo("rules-2026-v2");
        assertThat(home.toString()).doesNotContain("adminNote", "reviewOpinion", "notificationStatus", "idempotencyKey", "guide-draft", "guide-offline");

        JsonNode categories = performJson(get("/api/v1/guides/categories")
                .param("type", "JOIN_GUIDE")
                .param("audience", "VISITOR")
                .param("keyword", "join"), 200);
        assertThat(categories.toString()).contains("cat-join").doesNotContain("cat-disabled", "cat-archived");
        performJson(get("/api/v1/guides/categories").param("type", "BAD"), 400, 40001);
        performJson(get("/api/v1/guides/categories").param("audience", "BAD"), 400, 40001);

        JsonNode first = performJson(get("/api/v1/guides/articles").param("page", "1").param("pageSize", "1"), 200);
        JsonNode second = performJson(get("/api/v1/guides/articles").param("page", "2").param("pageSize", "1"), 200);
        assertThat(first.at("/data/items/0/guideId").asText()).isNotEqualTo(second.at("/data/items/0/guideId").asText());
        JsonNode empty = performJson(get("/api/v1/guides/articles").param("page", "99").param("pageSize", "20"), 200);
        assertThat(empty.at("/data/items").size()).isEqualTo(0);

        JsonNode filtered = performJson(get("/api/v1/guides/articles")
                .param("type", "CLIENT_SETUP")
                .param("categoryId", "cat-client")
                .param("tag", "client")
                .param("audience", "VISITOR")
                .param("keyword", "client")
                .param("pinned", "true")
                .param("sort", "pinned_desc"), 200);
        assertThat(filtered.toString()).contains("guide-client").doesNotContain("guide-rules");
        for (String sort : new String[]{"publishedAt_desc", "updatedAt_desc", "title_asc", "verifiedAt_desc"}) {
            performJson(get("/api/v1/guides/articles").param("sort", sort), 200);
        }
        performJson(get("/api/v1/guides/articles").param("type", "BAD"), 400, 40001);
        performJson(get("/api/v1/guides/articles").param("sort", "bad"), 400, 40003);

        String listText = performJson(get("/api/v1/guides/articles"), 200).toString();
        assertThat(listText).doesNotContain("guide-draft", "guide-pending", "guide-approved", "guide-rejected", "guide-needs", "guide-offline", "guide-archived", "guide-deleted", "guide-member", "guide-admin", "guide-future", "guide-expired-hidden", "adminNote", "reviewOpinion");

        JsonNode detail = performJson(get("/api/v1/guides/articles/guide-rules"), 200);
        assertThat(detail.at("/data/body").asText()).contains("rules");
        assertThat(detail.at("/data/toc").isArray()).isTrue();
        assertThat(detail.toString()).doesNotContain("adminNote", "reviewOpinion", "notificationStatus", "downloadUrl", "Cloudreve");
        JsonNode slugDetail = performJson(get("/api/v1/guides/articles/by-slug/server-rules"), 200);
        assertThat(slugDetail.at("/data/guideId").asText()).isEqualTo("guide-rules");
        performJson(get("/api/v1/guides/articles/missing"), 404, 43900);
        performJson(get("/api/v1/guides/articles/by-slug/missing"), 404, 43900);
        for (String id : new String[]{"guide-draft", "guide-pending", "guide-approved", "guide-rejected", "guide-needs", "guide-offline", "guide-archived", "guide-deleted", "guide-member", "guide-admin", "guide-future", "guide-expired-hidden"}) {
            performJson(get("/api/v1/guides/articles/" + id), 404);
        }
    }

    @Test
    @DisplayName("GUIDE-SEARCH GUIDE-CMD GUIDE-CHANNEL GUIDE-RULE covers search facets, command index, external channels, and rules")
    void searchCommandChannelRuleContract() throws Exception {
        JsonNode search = performJson(get("/api/v1/guides/search")
                .param("q", "rules")
                .param("type", "SERVER_RULE")
                .param("categoryId", "cat-rules")
                .param("tag", "rules")
                .param("audience", "VISITOR")
                .param("ruleVersion", "rules-2026-v2"), 200);
        assertThat(search.at("/data/items").toString()).contains("guide-rules").doesNotContain("guide-draft");
        assertThat(search.at("/data/facets").isObject()).isTrue();
        assertThat(search.at("/data/noResultFeedbackEnabled").asBoolean()).isTrue();
        assertThat(search.toString()).doesNotContain("<script", "adminNote", "reviewOpinion");
        performJson(get("/api/v1/guides/search"), 400, 40001);
        performJson(get("/api/v1/guides/search").param("q", ""), 400, 40001);
        performJson(get("/api/v1/guides/search").param("q", "x").param("pageSize", "51"), 400, 40002);
        performJson(get("/api/v1/guides/search").param("q", "x").param("type", "BAD"), 400, 40001);
        JsonNode noResult = performJson(get("/api/v1/guides/search").param("q", "no-such-guide"), 200);
        assertThat(noResult.at("/data/items").size()).isEqualTo(0);

        JsonNode commands = performJson(get("/api/v1/guides/commands")
                .param("keyword", "spawn")
                .param("tag", "basic")
                .param("guideId", "guide-commands")
                .param("sort", "command_asc"), 200);
        assertThat(commands.at("/data/items").toString()).contains("/spawn").doesNotContain("adminNote", "guide-draft");
        performJson(get("/api/v1/guides/commands").param("page", "0"), 400, 40002);
        performJson(get("/api/v1/guides/commands").param("sort", "bad"), 400, 40003);

        JsonNode channels = performJson(get("/api/v1/guides/external-channels")
                .param("type", "QQ_GROUP")
                .param("audience", "VISITOR")
                .param("keyword", "group"), 200);
        assertThat(channels.toString()).contains("channel-qq").doesNotContain("adminNote", "botToken", "chatMessages", "channel-disabled", "channel-archived");
        performJson(get("/api/v1/guides/external-channels").param("type", "BAD"), 400, 40001);

        JsonNode currentRule = performJson(get("/api/v1/guides/rules/current"), 200);
        assertThat(currentRule.at("/data/ruleVersion").asText()).isEqualTo("rules-2026-v2");
        assertThat(currentRule.at("/data/current").asBoolean()).isTrue();
        JsonNode historyRule = performJson(get("/api/v1/guides/rules/versions/rules-2025-v1"), 200);
        assertThat(historyRule.at("/data/current").asBoolean()).isFalse();
        performJson(get("/api/v1/guides/rules/versions/missing"), 404, 43900);
    }

    @Test
    @DisplayName("GUIDE-FEEDBACK and GUIDE-FEEDBACK-ADMIN cover current user feedback, idempotency, isolation, and processing")
    void feedbackContract() throws Exception {
        JsonNode feedback = performJson(post("/api/v1/guides/articles/guide-rules/feedback")
                .header("Authorization", bearer("user-token")), feedback("OUTDATED", "rules need review", "feedback-1"), 201);
        String feedbackId = feedback.at("/data/feedbackId").asText();
        assertThat(feedback.at("/data/guideVersion").asInt()).isGreaterThan(0);
        assertThat(feedback.at("/data/actorUserId").asText()).isEqualTo("user");

        JsonNode replay = performJson(post("/api/v1/guides/articles/guide-rules/feedback")
                .header("Authorization", bearer("user-token")), feedback("OUTDATED", "rules need review", "feedback-1"), 201);
        assertThat(replay.at("/data/feedbackId").asText()).isEqualTo(feedbackId);
        performJson(post("/api/v1/guides/articles/guide-rules/feedback")
                .header("Authorization", bearer("user-token")), feedback("BROKEN_LINK", "changed", "feedback-1"), 409, 43914);
        performJson(post("/api/v1/guides/articles/missing/feedback")
                .header("Authorization", bearer("user-token")), feedback("HELPFUL", "missing", "feedback-missing"), 404, 43900);
        performJson(post("/api/v1/guides/articles/guide-draft/feedback")
                .header("Authorization", bearer("user-token")), feedback("HELPFUL", "draft", "feedback-draft"), 404);
        performJson(post("/api/v1/guides/articles/guide-rules/feedback")
                .header("Authorization", bearer("user-token")), feedback("BAD", "bad", "feedback-bad"), 400, 40001);
        performJson(post("/api/v1/guides/articles/guide-rules/feedback")
                .header("Authorization", bearer("user-token")), feedback("OTHER", "x".repeat(1001), "feedback-long"), 400, 40001);
        performJson(post("/api/v1/guides/articles/guide-rules/feedback")
                .header("Authorization", bearer("user-token"))
                .header("X-Test-Fail-Audit", "true"), feedback("HELPFUL", "audit", "feedback-audit"), 500, 51900);

        JsonNode list = performJson(get("/api/v1/guides/admin/feedback")
                .header("Authorization", bearer("helper-token"))
                .param("guideId", "guide-rules")
                .param("type", "OUTDATED")
                .param("status", "OPEN")
                .param("actorUserId", "user")
                .param("from", "2026-05-01T00:00:00Z")
                .param("to", "2026-06-01T00:00:00Z")
                .param("sort", "createdAt_desc"), 200);
        assertThat(list.at("/data/items").toString()).contains(feedbackId).doesNotContain("token");
        performJson(get("/api/v1/guides/admin/feedback").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/guides/admin/feedback")
                .header("Authorization", bearer("helper-token"))
                .param("from", "2026-06-01T00:00:00Z")
                .param("to", "2026-05-01T00:00:00Z"), 400, 40001);
        performJson(get("/api/v1/guides/admin/feedback")
                .header("Authorization", bearer("helper-token"))
                .param("page", "abc"), 400, 40002);
        performJson(get("/api/v1/guides/admin/feedback")
                .header("Authorization", bearer("helper-token"))
                .param("from", "bad-time")
                .param("to", "2026-06-01T00:00:00Z"), 400, 40001);
        performJson(get("/api/v1/guides/admin/feedback")
                .header("Authorization", bearer("helper-token"))
                .param("type", "BAD"), 400, 40001);

        JsonNode resolved = performJson(patch("/api/v1/guides/admin/feedback/" + feedbackId + "/resolve")
                .header("Authorization", bearer("admin-token")), feedbackResolution("fixed", true, "resolve-1"), 200);
        assertThat(resolved.at("/data/status").asText()).isEqualTo("RESOLVED");
        performJson(patch("/api/v1/guides/admin/feedback/" + feedbackId + "/resolve")
                .header("Authorization", bearer("admin-token")), feedbackResolution("fixed", true, "resolve-1"), 200);
        performJson(patch("/api/v1/guides/admin/feedback/" + feedbackId + "/resolve")
                .header("Authorization", bearer("admin-token")), feedbackResolution("changed", true, "resolve-1"), 409, 43914);
        performJson(patch("/api/v1/guides/admin/feedback/missing/resolve")
                .header("Authorization", bearer("admin-token")), feedbackResolution("missing", false, "resolve-missing"), 404, 43904);
        performJson(patch("/api/v1/guides/admin/feedback/" + feedbackId + "/ignore")
                .header("Authorization", bearer("helper-token")), feedbackResolution("ignore", false, "ignore-helper"), 403, 42001);
    }

    @Test
    @DisplayName("GUIDE-ADMIN GUIDE-STATE GUIDE-VERSION GUIDE-CAT-ADMIN GUIDE-CHANNEL-ADMIN covers administration and state transitions")
    void adminStateVersionCategoryChannelContract() throws Exception {
        JsonNode guide = performJson(post("/api/v1/guides/admin/articles")
                .header("Authorization", bearer("admin-token")), validGuide("admin-flow", "admin-flow"), 201);
        String guideId = guide.at("/data/guideId").asText();
        assertThat(guide.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(guide.at("/data/currentVersion").asInt()).isEqualTo(1);

        performJson(get("/api/v1/guides/admin/articles")
                .header("Authorization", bearer("helper-token"))
                .param("status", "DRAFT")
                .param("visibility", "PUBLIC")
                .param("type", "JOIN_GUIDE")
                .param("categoryId", "cat-join")
                .param("tag", "join")
                .param("maintainerUserId", "maintainer")
                .param("expired", "false")
                .param("keyword", "Flow")
                .param("sort", "updatedAt_desc"), 200);
        JsonNode detail = performJson(get("/api/v1/guides/admin/articles/" + guideId).header("Authorization", bearer("helper-token")), 200);
        assertThat(detail.at("/data/body").asText()).contains("Flow");
        performJson(get("/api/v1/guides/admin/articles/missing").header("Authorization", bearer("helper-token")), 404, 43900);

        performJson(post("/api/v1/guides/admin/articles").header("Authorization", bearer("admin-token")), validGuide("admin-flow", "slug-conflict"), 409, 43911);
        performJson(post("/api/v1/guides/admin/articles").header("Authorization", bearer("admin-token")), with(validGuide("bad-category", "bad-category"), "categoryId", "missing"), 404, 43901);
        performJson(post("/api/v1/guides/admin/articles").header("Authorization", bearer("admin-token")), with(validGuide("bad-rule", "bad-rule"), "type", "SERVER_RULE"), 400, 40001);
        performJson(post("/api/v1/guides/admin/articles").header("Authorization", bearer("admin-token")), with(validGuide("bad-time", "bad-time"), "visibleUntil", "2026-01-01T00:00:00Z"), 400, 40001);
        performJson(post("/api/v1/guides/admin/articles").header("Authorization", bearer("admin-token")), with(validGuide("bad-visible-from", "bad-visible-from"), "visibleFrom", "bad-time"), 400, 40001);
        performJson(post("/api/v1/guides/admin/articles").header("Authorization", bearer("admin-token")), with(validGuide("bad-visible-until", "bad-visible-until"), "visibleUntil", "bad-time"), 400, 40001);

        Map<String, Object> patchBody = new LinkedHashMap<>();
        patchBody.put("title", "Flow Guide Updated");
        patchBody.put("body", "Updated body");
        patchBody.put("reason", "update");
        patchBody.put("idempotencyKey", "patch-flow");
        performJson(patch("/api/v1/guides/admin/articles/" + guideId).header("Authorization", bearer("admin-token")), patchBody, 200);
        performJson(patch("/api/v1/guides/admin/articles/" + guideId).header("Authorization", bearer("admin-token")), patchBody, 200);
        performJson(patch("/api/v1/guides/admin/articles/" + guideId).header("Authorization", bearer("admin-token")), Map.of("title", "Changed", "reason", "update", "idempotencyKey", "patch-flow"), 409, 43914);

        performJson(patch("/api/v1/guides/admin/articles/" + guideId + "/submit-review").header("Authorization", bearer("admin-token")), reason("submit"), 200);
        performJson(patch("/api/v1/guides/admin/articles/" + guideId + "/submit-review").header("Authorization", bearer("admin-token")), reason("submit"), 200);
        performJson(patch("/api/v1/guides/admin/articles/" + guideId + "/approve").header("Authorization", bearer("helper-token")), review("ok"), 200);
        performJson(patch("/api/v1/guides/admin/articles/" + guideId + "/publish").header("Authorization", bearer("helper-token")), reason("publish"), 403, 42001);
        performJson(patch("/api/v1/guides/admin/articles/" + guideId + "/publish").header("Authorization", bearer("admin-token")), reason("publish"), 200);
        performJson(get("/api/v1/guides/articles/" + guideId), 200);
        performJson(patch("/api/v1/guides/admin/articles/" + guideId + "/archive").header("Authorization", bearer("admin-token")), reason("bad archive"), 409, 43910);
        performJson(patch("/api/v1/guides/admin/articles/" + guideId + "/delete").header("Authorization", bearer("admin-token")), reason("bad delete"), 409, 43910);
        performJson(patch("/api/v1/guides/admin/articles/" + guideId + "/offline").header("Authorization", bearer("admin-token")), reason("offline"), 200);
        performJson(get("/api/v1/guides/articles/" + guideId), 404);
        performJson(patch("/api/v1/guides/admin/articles/" + guideId + "/archive").header("Authorization", bearer("admin-token")), reason("archive"), 200);
        performJson(patch("/api/v1/guides/admin/articles/" + guideId).header("Authorization", bearer("admin-token")), Map.of("title", "Archived edit", "reason", "edit"), 409, 43910);

        JsonNode versions = performJson(get("/api/v1/guides/admin/articles/" + guideId + "/versions").header("Authorization", bearer("admin-token")), 200);
        assertThat(versions.at("/data/items").size()).isGreaterThan(0);
        performJson(get("/api/v1/guides/admin/articles/" + guideId + "/versions/1").header("Authorization", bearer("admin-token")), 200);
        performJson(patch("/api/v1/guides/admin/articles/" + guideId + "/versions/1/restore").header("Authorization", bearer("admin-token")), reason("restore archived"), 409, 43910);

        JsonNode category = performJson(post("/api/v1/guides/admin/categories")
                .header("Authorization", bearer("admin-token")), validCategory("new-guide-category"), 201);
        String categoryId = category.at("/data/categoryId").asText();
        performJson(get("/api/v1/guides/admin/categories").header("Authorization", bearer("helper-token")).param("enabled", "true").param("keyword", "new"), 200);
        performJson(post("/api/v1/guides/admin/categories")
                .header("Authorization", bearer("admin-token")), with(validCategory("bad-cat-sort"), "sortOrder", "abc"), 400, 40001);
        Map<String, Object> categoryPatch = Map.of("description", "updated", "reason", "patch", "idempotencyKey", "cat-patch");
        performJson(patch("/api/v1/guides/admin/categories/" + categoryId).header("Authorization", bearer("admin-token")), categoryPatch, 200);
        performJson(patch("/api/v1/guides/admin/categories/" + categoryId).header("Authorization", bearer("admin-token")), categoryPatch, 200);
        performJson(patch("/api/v1/guides/admin/categories/" + categoryId).header("Authorization", bearer("admin-token")), Map.of("description", "changed", "reason", "patch", "idempotencyKey", "cat-patch"), 409, 43914);
        performJson(patch("/api/v1/guides/admin/categories/" + categoryId).header("Authorization", bearer("admin-token")), Map.of("sortOrder", "abc", "reason", "patch", "idempotencyKey", "cat-patch-bad"), 400, 40001);
        performJson(patch("/api/v1/guides/admin/categories/" + categoryId + "/archive").header("Authorization", bearer("admin-token")), reason("archive"), 200);
        performJson(patch("/api/v1/guides/admin/categories/cat-rules/archive").header("Authorization", bearer("admin-token")), reason("used"), 409, 43915);

        JsonNode channel = performJson(post("/api/v1/guides/admin/external-channels")
                .header("Authorization", bearer("admin-token")), validChannel("new-qq"), 201);
        String channelId = channel.at("/data/channelId").asText();
        assertThat(channel.at("/data/sortOrder").asInt()).isEqualTo(20);
        performJson(get("/api/v1/guides/admin/external-channels").header("Authorization", bearer("helper-token")).param("type", "QQ_GROUP").param("status", "ENABLED"), 200);
        performJson(get("/api/v1/guides/admin/external-channels").header("Authorization", bearer("helper-token")).param("type", "BAD"), 400, 40001);
        performJson(get("/api/v1/guides/admin/external-channels").header("Authorization", bearer("helper-token")).param("page", "0"), 400, 40002);
        performJson(post("/api/v1/guides/admin/external-channels").header("Authorization", bearer("admin-token")), with(validChannel("bad-channel-sort"), "sortOrder", "abc"), 400, 40001);
        Map<String, Object> channelPatch = Map.of("purpose", "updated", "reason", "patch", "idempotencyKey", "channel-patch");
        performJson(patch("/api/v1/guides/admin/external-channels/" + channelId).header("Authorization", bearer("admin-token")), channelPatch, 200);
        performJson(patch("/api/v1/guides/admin/external-channels/" + channelId).header("Authorization", bearer("admin-token")), channelPatch, 200);
        performJson(patch("/api/v1/guides/admin/external-channels/" + channelId).header("Authorization", bearer("admin-token")), Map.of("purpose", "changed", "reason", "patch", "idempotencyKey", "channel-patch"), 409, 43914);
        performJson(patch("/api/v1/guides/admin/external-channels/" + channelId).header("Authorization", bearer("admin-token")), Map.of("visibility", "BAD", "reason", "patch", "idempotencyKey", "channel-patch-bad"), 400, 40001);
        performJson(patch("/api/v1/guides/admin/external-channels/" + channelId + "/disable").header("Authorization", bearer("admin-token")), reason("disable"), 200);
        performJson(patch("/api/v1/guides/admin/external-channels/" + channelId + "/enable").header("Authorization", bearer("admin-token")), reason("enable"), 200);
        performJson(patch("/api/v1/guides/admin/external-channels/" + channelId + "/archive").header("Authorization", bearer("admin-token")), reason("archive"), 200);
        performJson(patch("/api/v1/guides/admin/external-channels/channel-qq/archive").header("Authorization", bearer("admin-token")), reason("used"), 409, 43915);
        performJson(post("/api/v1/guides/admin/external-channels").header("Authorization", bearer("admin-token")), with(validChannel("bad-secret"), "entryUrl", "https://example.com/admin?token=secret"), 400, 40001);
    }

    @Test
    @DisplayName("GUIDE-AUDIT GUIDE-OPS GUIDE-COMPAT covers audit, self-check, sensitive fields, and service boundaries")
    void auditOpsCompatibilityContract() throws Exception {
        JsonNode audit = performJson(get("/api/v1/guides/admin/articles/guide-rules/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("action", "GUIDE_PUBLISHED")
                .param("actorUserId", "system")
                .param("result", "SUCCESS")
                .param("from", "2026-05-01T00:00:00Z")
                .param("to", "2026-06-01T00:00:00Z")
                .param("sort", "createdAt_asc"), 200);
        assertThat(audit.at("/data/items").isArray()).isTrue();
        assertThat(audit.toString()).doesNotContain("Authorization", "Cookie", "token", "sharePassword", "notification body", "stackTrace");
        performJson(get("/api/v1/guides/admin/articles/guide-rules/audit-logs").header("Authorization", bearer("helper-token")), 403, 42001);
        performJson(get("/api/v1/guides/admin/articles/missing/audit-logs").header("Authorization", bearer("admin-token")), 404, 43900);
        performJson(get("/api/v1/guides/admin/articles/guide-rules/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("from", "2026-06-01T00:00:00Z")
                .param("to", "2026-05-01T00:00:00Z"), 400, 40001);
        performJson(get("/api/v1/guides/admin/articles/guide-rules/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("sort", "bad"), 400, 40003);
        performJson(get("/api/v1/guides/admin/articles/guide-rules/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("page", "0"), 400, 40002);
        performJson(get("/api/v1/guides/admin/articles/guide-rules/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("from", "bad-time")
                .param("to", "2026-06-01T00:00:00Z"), 400, 40001);

        JsonNode ops = performJson(get("/api/v1/guides/admin/ops/summary").header("Authorization", bearer("admin-token")), 200);
        assertThat(ops.at("/data/service").asText()).isEqualTo("guide");
        assertThat(ops.at("/data/port").asInt()).isEqualTo(8127);
        assertThat(ops.at("/data/storageMode").asText()).isEqualTo("IN_MEMORY");
        assertThat(ops.at("/data/productionGaps").toString()).contains("PERSISTENT_STORAGE_NOT_ENABLED", "REAL_CROSS_SERVICE_HTTP_NOT_ENABLED", "GATEWAY_INTERNAL_SIGNATURE_NOT_ENABLED");
        assertThat(ops.toString()).doesNotContain("token", "secret", "sharePassword", "adminNote", "reviewOpinion", "stackTrace");
        performJson(get("/api/v1/guides/admin/ops/summary").header("Authorization", bearer("helper-token")), 403, 42001);

        String source = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/cn/beiming/guide/GuideServiceApplication.java"));
        assertThat(source).contains("cn.beiming.guide");
        assertThat(source).doesNotContain("cn.beiming.auth", "cn.beiming.profile", "cn.beiming.notification", "cn.beiming.content", "cn.beiming.resource", "cn.beiming.serverstatus");
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, int status) throws Exception {
        MvcResult result = mvc.perform(builder.accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, int status, int code) throws Exception {
        JsonNode json = performJson(builder, status);
        assertThat(json.at("/code").asInt()).isEqualTo(code);
        assertThat(json.at("/requestId").asText()).isNotBlank();
        return json;
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, Map<String, Object> body, int status) throws Exception {
        MvcResult result = mvc.perform(builder
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, Map<String, Object> body, int status, int code) throws Exception {
        JsonNode json = performJson(builder, body, status);
        assertThat(json.at("/code").asInt()).isEqualTo(code);
        assertThat(json.at("/requestId").asText()).isNotBlank();
        return json;
    }

    private Map<String, Object> validGuide(String slug, String key) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "JOIN_GUIDE");
        body.put("slug", slug);
        body.put("title", "Flow Guide " + slug);
        body.put("summary", "A guide summary");
        body.put("body", "Flow guide body");
        body.put("categoryId", "cat-join");
        body.put("tags", java.util.List.of("join", "client"));
        body.put("audience", java.util.List.of("VISITOR"));
        body.put("visibility", "PUBLIC");
        body.put("pinned", false);
        body.put("toc", java.util.List.of(Map.of("title", "Start", "anchor", "start", "level", 1, "sortOrder", 1)));
        body.put("commandEntries", java.util.List.of());
        body.put("references", java.util.List.of());
        body.put("externalChannelIds", java.util.List.of());
        body.put("maintainerMemberId", "member-active");
        body.put("visibleFrom", "2026-05-01T00:00:00Z");
        body.put("visibleUntil", "2026-12-31T00:00:00Z");
        body.put("verifiedAt", "2026-05-20T00:00:00Z");
        body.put("expiresAt", "2026-12-31T00:00:00Z");
        body.put("reason", "create guide");
        body.put("idempotencyKey", key);
        return body;
    }

    private Map<String, Object> validCategory(String slug) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Category " + slug);
        body.put("slug", slug);
        body.put("description", "category");
        body.put("icon", "book");
        body.put("sortOrder", 20);
        body.put("enabled", true);
        body.put("reason", "category");
        return body;
    }

    private Map<String, Object> validChannel(String slug) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "QQ_GROUP");
        body.put("name", "QQ " + slug);
        body.put("slug", slug);
        body.put("purpose", "player chat");
        body.put("joinCondition", "follow rules");
        body.put("rules", java.util.List.of("friendly chat"));
        body.put("entryUrl", "https://example.com/join/" + slug);
        body.put("entryHint", "masked group id");
        body.put("visibility", "PUBLIC");
        body.put("sortOrder", 20);
        body.put("adminNote", "internal note");
        body.put("reason", "channel");
        body.put("idempotencyKey", slug);
        return body;
    }

    private Map<String, Object> feedback(String type, String message, String key) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type);
        body.put("message", message);
        body.put("anchor", "start");
        body.put("idempotencyKey", key);
        return body;
    }

    private Map<String, Object> feedbackResolution(String note, boolean notifyUser, String key) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("resolutionNote", note);
        body.put("notifyUser", notifyUser);
        body.put("reason", note);
        body.put("idempotencyKey", key);
        return body;
    }

    private Map<String, Object> reason(String reason) {
        return Map.of("reason", reason);
    }

    private Map<String, Object> review(String opinion) {
        return Map.of("reviewOpinion", opinion, "publicComment", opinion, "reason", opinion);
    }

    private Map<String, Object> with(Map<String, Object> source, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put(key, value);
        return copy;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void addRange(Set<String> target, String prefix, int start, int end) {
        for (int index = start; index <= end; index++) {
            target.add(prefix + "-" + "%03d".formatted(index));
        }
    }
}
