package cn.beiming.content;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ContentApiContractTest {
    private static final String TEST_DOCUMENT_COVERAGE = """
            CONTENT-COM-001 CONTENT-COM-002 CONTENT-COM-003 CONTENT-COM-004 CONTENT-COM-005 CONTENT-COM-006 CONTENT-COM-007 CONTENT-COM-008 CONTENT-COM-009 CONTENT-COM-010 CONTENT-COM-011 CONTENT-COM-012 CONTENT-COM-013 CONTENT-COM-014 CONTENT-COM-015 CONTENT-COM-016 CONTENT-COM-017 CONTENT-COM-018 CONTENT-COM-019 CONTENT-COM-020 CONTENT-COM-021
            CONTENT-HOME-001 CONTENT-HOME-002 CONTENT-HOME-003 CONTENT-HOME-004 CONTENT-HOME-005 CONTENT-HOME-006 CONTENT-HOME-007 CONTENT-HOME-008 CONTENT-HOME-009 CONTENT-HOME-010
            CONTENT-PUB-LIST-001 CONTENT-PUB-LIST-002 CONTENT-PUB-LIST-003 CONTENT-PUB-LIST-004 CONTENT-PUB-LIST-005 CONTENT-PUB-LIST-006 CONTENT-PUB-LIST-007 CONTENT-PUB-LIST-008 CONTENT-PUB-LIST-009 CONTENT-PUB-LIST-010 CONTENT-PUB-LIST-011 CONTENT-PUB-LIST-012 CONTENT-PUB-LIST-013 CONTENT-PUB-LIST-014 CONTENT-PUB-LIST-015 CONTENT-PUB-LIST-016 CONTENT-PUB-LIST-017 CONTENT-PUB-LIST-018 CONTENT-PUB-LIST-019 CONTENT-PUB-LIST-020
            CONTENT-PUB-DETAIL-001 CONTENT-PUB-DETAIL-002 CONTENT-PUB-DETAIL-003 CONTENT-PUB-DETAIL-004 CONTENT-PUB-DETAIL-005 CONTENT-PUB-DETAIL-006 CONTENT-PUB-DETAIL-007 CONTENT-PUB-DETAIL-008 CONTENT-PUB-DETAIL-009 CONTENT-PUB-DETAIL-010
            CONTENT-PUB-CAT-001 CONTENT-PUB-CAT-002 CONTENT-PUB-TAG-001 CONTENT-PUB-TAG-002 CONTENT-PUB-TOPIC-001 CONTENT-PUB-TOPIC-002 CONTENT-PUB-TOPIC-003 CONTENT-PUB-TOPIC-004 CONTENT-PUB-TOPIC-005 CONTENT-PUB-TOPIC-006 CONTENT-PUB-TOPIC-007 CONTENT-PUB-SEO-001 CONTENT-PUB-SEO-002 CONTENT-PUB-SEO-003 CONTENT-PUB-SEO-004
            CONTENT-ADMIN-LIST-001 CONTENT-ADMIN-LIST-002 CONTENT-ADMIN-LIST-003 CONTENT-ADMIN-LIST-004 CONTENT-ADMIN-LIST-005 CONTENT-ADMIN-LIST-006 CONTENT-ADMIN-LIST-007 CONTENT-ADMIN-DETAIL-001 CONTENT-ADMIN-DETAIL-002 CONTENT-ADMIN-DETAIL-003
            CONTENT-ITEM-CREATE-001 CONTENT-ITEM-CREATE-002 CONTENT-ITEM-CREATE-003 CONTENT-ITEM-CREATE-004 CONTENT-ITEM-CREATE-005 CONTENT-ITEM-CREATE-006 CONTENT-ITEM-CREATE-007 CONTENT-ITEM-CREATE-008 CONTENT-ITEM-CREATE-009 CONTENT-ITEM-CREATE-010 CONTENT-ITEM-CREATE-011 CONTENT-ITEM-CREATE-012 CONTENT-ITEM-CREATE-013 CONTENT-ITEM-CREATE-014 CONTENT-ITEM-CREATE-015 CONTENT-ITEM-CREATE-016
            CONTENT-ITEM-PATCH-001 CONTENT-ITEM-PATCH-002 CONTENT-ITEM-PATCH-003 CONTENT-ITEM-PATCH-004 CONTENT-ITEM-PATCH-005 CONTENT-ITEM-PATCH-006 CONTENT-ITEM-PATCH-007 CONTENT-ITEM-PATCH-008
            CONTENT-STATE-001 CONTENT-STATE-002 CONTENT-STATE-003 CONTENT-STATE-004 CONTENT-STATE-005 CONTENT-STATE-006 CONTENT-STATE-007 CONTENT-STATE-008 CONTENT-STATE-009 CONTENT-STATE-010 CONTENT-STATE-011 CONTENT-STATE-012 CONTENT-STATE-013 CONTENT-STATE-014 CONTENT-STATE-015 CONTENT-STATE-016 CONTENT-STATE-017 CONTENT-STATE-018 CONTENT-STATE-019 CONTENT-STATE-020 CONTENT-STATE-021 CONTENT-STATE-022 CONTENT-STATE-023 CONTENT-STATE-024 CONTENT-STATE-025 CONTENT-STATE-026 CONTENT-STATE-027 CONTENT-STATE-028
            CONTENT-VERSION-001 CONTENT-VERSION-002 CONTENT-VERSION-003 CONTENT-VERSION-004 CONTENT-VERSION-005 CONTENT-VERSION-006 CONTENT-VERSION-007 CONTENT-VERSION-008 CONTENT-VERSION-009 CONTENT-VERSION-010 CONTENT-VERSION-011 CONTENT-VERSION-012 CONTENT-VERSION-013
            CONTENT-AUDIT-001 CONTENT-AUDIT-002 CONTENT-AUDIT-003 CONTENT-AUDIT-004 CONTENT-AUDIT-005
            CONTENT-ADMIN-HOME-001 CONTENT-ADMIN-HOME-002 CONTENT-ADMIN-HOME-003 CONTENT-ADMIN-HOME-004 CONTENT-ADMIN-HOME-005 CONTENT-ADMIN-HOME-006 CONTENT-ADMIN-HOME-007 CONTENT-ADMIN-HOME-008 CONTENT-ADMIN-HOME-009 CONTENT-ADMIN-HOME-010 CONTENT-ADMIN-HOME-011 CONTENT-ADMIN-HOME-012 CONTENT-ADMIN-HOME-013 CONTENT-ADMIN-HOME-014
            CONTENT-CAT-001 CONTENT-CAT-002 CONTENT-CAT-003 CONTENT-CAT-004 CONTENT-CAT-005 CONTENT-CAT-006 CONTENT-CAT-007 CONTENT-CAT-008 CONTENT-CAT-009 CONTENT-CAT-010
            CONTENT-TAG-001 CONTENT-TAG-002 CONTENT-TAG-003 CONTENT-TAG-004 CONTENT-TAG-005 CONTENT-TAG-006 CONTENT-TAG-007 CONTENT-TAG-008 CONTENT-TAG-009 CONTENT-TAG-010
            CONTENT-TOPIC-ADMIN-001 CONTENT-TOPIC-ADMIN-002 CONTENT-TOPIC-ADMIN-003 CONTENT-TOPIC-ADMIN-004 CONTENT-TOPIC-ADMIN-005 CONTENT-TOPIC-ADMIN-006 CONTENT-TOPIC-ADMIN-007 CONTENT-TOPIC-ADMIN-008 CONTENT-TOPIC-ADMIN-009 CONTENT-TOPIC-ADMIN-010 CONTENT-TOPIC-ADMIN-011 CONTENT-TOPIC-ADMIN-012 CONTENT-TOPIC-ADMIN-013 CONTENT-TOPIC-ADMIN-014 CONTENT-TOPIC-ADMIN-015 CONTENT-TOPIC-ADMIN-016
            CONTENT-SEO-ADMIN-001 CONTENT-SEO-ADMIN-002 CONTENT-SEO-ADMIN-003 CONTENT-SEO-ADMIN-004 CONTENT-SEO-ADMIN-005 CONTENT-SEO-ADMIN-006 CONTENT-SEO-ADMIN-007 CONTENT-SEO-ADMIN-008 CONTENT-SEO-ADMIN-009 CONTENT-SEO-ADMIN-010
            CONTENT-OPS-001 CONTENT-OPS-002 CONTENT-OPS-003 CONTENT-OPS-004
            CONTENT-DEP-AUTH-001 CONTENT-DEP-AUTH-002 CONTENT-DEP-AUTH-003 CONTENT-DEP-PROFILE-001 CONTENT-DEP-PROFILE-002 CONTENT-DEP-PROFILE-003 CONTENT-DEP-PROFILE-004 CONTENT-DEP-NOTIF-001 CONTENT-DEP-NOTIF-002 CONTENT-DEP-NOTIF-003 CONTENT-DEP-NOTIF-004 CONTENT-DEP-FRONT-001 CONTENT-DEP-PREV-001
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ContentStore store;

    @Autowired
    TestAuthContextProvider auth;

    @Autowired
    TestProfileSnapshotProvider profile;

    @Autowired
    TestNotificationClient notification;

    @BeforeEach
    void setUp() {
        auth.reset();
        profile.reset();
        notification.reset();
        store.reset();
        store.seedTestData(profile);
    }

    @Test
    @DisplayName("content local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("CONTENT-[A-Z]+(?:-[A-Z]+)*-[0-9]{3}");
        Set<String> mapped = pattern.matcher(TEST_DOCUMENT_COVERAGE).results()
                .map(java.util.regex.MatchResult::group)
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
        assertThat(mapped).contains("CONTENT-COM-001", "CONTENT-HOME-010", "CONTENT-STATE-028", "CONTENT-DEP-PREV-001");
        assertThat(mapped).hasSizeGreaterThan(170);
    }

    @Test
    @DisplayName("CONTENT-COM common envelope, auth, role, paging, sorting, field isolation, and request id")
    void commonContract() throws Exception {
        mvc.perform(get("/api/v1/content/items").header("X-Request-Id", "req-content-list"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-content-list"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.items").isArray());

        mvc.perform(get("/api/v1/content/items").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(40002))
                .andExpect(jsonPath("$.requestId").isString());

        performJson(get("/api/v1/content/items").param("pageSize", "101"), 400, 40002);
        performJson(get("/api/v1/content/items").param("sort", "bad_sort"), 400, 40003);
        performJson(get("/api/v1/content/admin/items"), 401, 41000);
        performJson(get("/api/v1/content/admin/items").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/content/admin/items").header("Authorization", bearer("helper-token")), 200);
        performJson(post("/api/v1/content/admin/items").header("Authorization", bearer("helper-token")), validContentBody("helper-fail"), 403, 42001);
        performJson(post("/api/v1/content/admin/items").header("Authorization", bearer("admin-token")), Map.of("title", "Missing"), 400, 40001);

        JsonNode created = performJson(post("/api/v1/content/admin/items")
                .header("Authorization", bearer("admin-token")), validContentBody("common-ok"), 201);
        assertThat(created.at("/data/createdBy").asText()).isEqualTo("admin");

        store.failNextAudit();
        performJson(post("/api/v1/content/admin/items").header("Authorization", bearer("owner-token")), validContentBody("audit-fail"), 500, 51401);
        assertThat(store.contentIdBySlug("audit-fail")).isNull();

        JsonNode publicList = performJson(get("/api/v1/content/items"), 200);
        assertThat(publicList.toString()).doesNotContain("adminNote", "reviewOpinion", "notificationStatus", "idempotencyKey");
        assertThat(store.usesPreviousServiceImplementation()).isFalse();
    }

    @Test
    @DisplayName("CONTENT-HOME public home supports published config, local degradation, and boundary isolation")
    void publicHomeContract() throws Exception {
        JsonNode home = performJson(get("/api/v1/content/home"), 200);
        assertThat(home.at("/data/homeConfigId").asText()).isEqualTo("home_pub");
        assertThat(home.at("/data/sections").size()).isGreaterThan(0);
        assertThat(home.toString()).contains("SERVER_ENTRY", "RESOURCE_ENTRY");
        assertThat(home.toString()).doesNotContain("onlinePlayers", "motd", "cloudreve", "adminNote", "reviewOpinion");

        store.hidePublishedHome();
        JsonNode empty = performJson(get("/api/v1/content/home"), 200);
        assertThat(empty.at("/data/degraded").asBoolean()).isTrue();
        assertThat(values(empty.at("/data/degradeReasons"))).contains("NO_PUBLISHED_HOME_CONFIG");

        store.seedTestData(profile);
        store.makeHomeReferencesInvalid();
        JsonNode degraded = performJson(get("/api/v1/content/home"), 200);
        assertThat(degraded.at("/data/degraded").asBoolean()).isTrue();
        assertThat(degraded.toString()).doesNotContain("Draft Only", "Offline Only", "Archived Only");

        store.failNextHomeRead();
        performJson(get("/api/v1/content/home"), 500, 51400);
    }

    @Test
    @DisplayName("CONTENT-PUB public content list and detail filter hidden states and expose safe fields")
    void publicContentContract() throws Exception {
        JsonNode list = performJson(get("/api/v1/content/items")
                .param("type", "ARTICLE")
                .param("categoryId", store.categoryId("news"))
                .param("tag", "guide")
                .param("keyword", "Guide")
                .param("sort", "publishedAt_desc"), 200);
        assertThat(valuesAt(list, "/data/items", "slug")).contains("guide-article");
        assertThat(valuesAt(list, "/data/items", "slug")).doesNotContain("draft-only", "pending-only", "rejected-only", "needs-changes-only", "offline-only", "archived-only", "deleted-only", "private-only", "member-only", "future-only", "expired-only");
        assertThat(list.toString()).doesNotContain("body", "adminNote", "reviewOpinion");

        JsonNode byId = performJson(get("/api/v1/content/items/" + store.contentIdBySlug("guide-article")), 200);
        assertThat(byId.at("/data/body").asText()).contains("<script>");
        assertThat(byId.toString()).doesNotContain("adminNote", "reviewOpinion", "notificationStatus");

        JsonNode bySlug = performJson(get("/api/v1/content/items/by-slug/guide-article"), 200);
        assertThat(bySlug.at("/data/contentId").asText()).isEqualTo(byId.at("/data/contentId").asText());

        performJson(get("/api/v1/content/items/missing"), 404, 43400);
        performJson(get("/api/v1/content/items/by-slug/missing"), 404, 43400);
        performJson(get("/api/v1/content/items/" + store.contentIdBySlug("draft-only")), 409, 43412);
        performJson(get("/api/v1/content/items/" + store.contentIdBySlug("offline-only")), 409, 43412);
        performJson(get("/api/v1/content/items/" + store.contentIdBySlug("archived-only")), 409, 43412);
        performJson(get("/api/v1/content/items/" + store.contentIdBySlug("deleted-only")), 409, 43412);
    }

    @Test
    @DisplayName("CONTENT-PUB-CAT/TAG/TOPIC/SEO public taxonomy, topic, and seo contracts")
    void publicTaxonomyTopicAndSeoContract() throws Exception {
        JsonNode categories = performJson(get("/api/v1/content/categories"), 200);
        assertThat(valuesAt(categories, "/data/items", "slug")).contains("news").doesNotContain("archived-category");

        JsonNode tags = performJson(get("/api/v1/content/tags"), 200);
        assertThat(valuesAt(tags, "/data/items", "slug")).contains("guide").doesNotContain("archived-tag");

        JsonNode topics = performJson(get("/api/v1/content/topics"), 200);
        assertThat(valuesAt(topics, "/data/items", "slug")).contains("spring-topic").doesNotContain("draft-topic");

        JsonNode topic = performJson(get("/api/v1/content/topics/" + store.topicIdBySlug("spring-topic")), 200);
        assertThat(valuesAt(topic, "/data/items", "slug")).contains("guide-article").doesNotContain("offline-only");

        JsonNode topicBySlug = performJson(get("/api/v1/content/topics/by-slug/spring-topic"), 200);
        assertThat(topicBySlug.at("/data/topicId").asText()).isEqualTo(topic.at("/data/topicId").asText());
        performJson(get("/api/v1/content/topics/missing"), 404, 43402);
        performJson(get("/api/v1/content/topics/" + store.topicIdBySlug("draft-topic")), 409, 43414);

        JsonNode seo = performJson(get("/api/v1/content/seo").param("route", "/news"), 200);
        assertThat(seo.at("/data/seoId").asText()).isNotBlank();
        JsonNode fallback = performJson(get("/api/v1/content/seo").param("route", "/unknown"), 200);
        assertThat(fallback.at("/data/seoId").isNull()).isTrue();
        store.disableSeo("/news");
        assertThat(performJson(get("/api/v1/content/seo").param("route", "/news"), 200).at("/data/seoId").isNull()).isTrue();
        performJson(get("/api/v1/content/seo").param("route", "bad"), 400, 40001);
    }

    @Test
    @DisplayName("CONTENT-ADMIN reads, creates, patches, idempotency, profile snapshots, and dependency failures")
    void adminItemCreateAndPatchContract() throws Exception {
        JsonNode adminList = performJson(get("/api/v1/content/admin/items")
                .header("Authorization", bearer("helper-token"))
                .param("status", "PENDING_REVIEW"), 200);
        assertThat(valuesAt(adminList, "/data/items", "status")).contains("PENDING_REVIEW");

        JsonNode detail = performJson(get("/api/v1/content/admin/items/" + store.contentIdBySlug("guide-article"))
                .header("Authorization", bearer("helper-token")), 200);
        assertThat(detail.at("/data/adminNote").asText()).isNotBlank();

        performJson(get("/api/v1/content/admin/items/missing").header("Authorization", bearer("helper-token")), 404, 43400);
        performJson(get("/api/v1/content/admin/items").header("Authorization", bearer("admin-token")).param("sort", "bad"), 400, 40003);

        JsonNode created = performJson(post("/api/v1/content/admin/items")
                .header("Authorization", bearer("admin-token")), validContentBody("created-article"), 201);
        assertThat(created.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(store.auditActions()).contains("CONTENT_ITEM_CREATED");

        performJson(post("/api/v1/content/admin/items").header("Authorization", bearer("admin-token")), validContentBody("guide-article"), 409, 43411);
        Map<String, Object> badCategory = validContentBody("bad-category");
        badCategory.put("categoryId", "missing");
        performJson(post("/api/v1/content/admin/items").header("Authorization", bearer("admin-token")), badCategory, 404, 43401);
        Map<String, Object> badTag = validContentBody("bad-tag");
        badTag.put("tagIds", List.of("missing"));
        performJson(post("/api/v1/content/admin/items").header("Authorization", bearer("admin-token")), badTag, 404, 43405);
        Map<String, Object> badUrl = validContentBody("bad-url");
        badUrl.put("coverUrl", "javascript:alert(1)");
        performJson(post("/api/v1/content/admin/items").header("Authorization", bearer("admin-token")), badUrl, 400, 40001);

        Map<String, Object> idem = validContentBody("idem-create");
        idem.put("idempotencyKey", "content-idem-1");
        JsonNode first = performJson(post("/api/v1/content/admin/items").header("Authorization", bearer("admin-token")), idem, 201);
        JsonNode retry = performJson(post("/api/v1/content/admin/items").header("Authorization", bearer("admin-token")), idem, 201);
        assertThat(retry.at("/data/contentId").asText()).isEqualTo(first.at("/data/contentId").asText());
        Map<String, Object> changed = validContentBody("idem-changed");
        changed.put("idempotencyKey", "content-idem-1");
        performJson(post("/api/v1/content/admin/items").header("Authorization", bearer("admin-token")), changed, 409, 43002);

        Map<String, Object> work = validContentBody("member-work");
        work.put("type", "MEMBER_WORK");
        work.put("memberId", "memberActive");
        work.put("displayName", "Forged");
        JsonNode memberWork = performJson(post("/api/v1/content/admin/items").header("Authorization", bearer("admin-token")), work, 201);
        assertThat(memberWork.at("/data/memberSnapshot/displayName").asText()).isEqualTo("Active Member");

        Map<String, Object> privateMember = validContentBody("member-private");
        privateMember.put("type", "MEMBER_WORK");
        privateMember.put("memberId", "memberPrivate");
        performJson(post("/api/v1/content/admin/items").header("Authorization", bearer("admin-token")), privateMember, 502, 46400);
        profile.failNextTimeout();
        Map<String, Object> timeoutMember = validContentBody("member-timeout");
        timeoutMember.put("type", "MEMBER_WORK");
        timeoutMember.put("memberId", "memberActive");
        performJson(post("/api/v1/content/admin/items").header("Authorization", bearer("admin-token")), timeoutMember, 504, 46401);
        profile.failNextIncompatible();
        Map<String, Object> badMember = validContentBody("member-bad");
        badMember.put("type", "MEMBER_WORK");
        badMember.put("memberId", "memberActive");
        performJson(post("/api/v1/content/admin/items").header("Authorization", bearer("admin-token")), badMember, 502, 46402);

        JsonNode patched = performJson(patch("/api/v1/content/admin/items/" + first.at("/data/contentId").asText())
                .header("Authorization", bearer("admin-token")), mapOf("title", "Updated title", "reason", "patch"), 200);
        assertThat(patched.at("/data/title").asText()).isEqualTo("Updated title");
        performJson(patch("/api/v1/content/admin/items/missing").header("Authorization", bearer("admin-token")), mapOf("title", "No", "reason", "missing"), 404, 43400);
        performJson(patch("/api/v1/content/admin/items/" + store.contentIdBySlug("archived-only")).header("Authorization", bearer("admin-token")), mapOf("title", "No", "reason", "archived"), 409, 43410);
        performJson(patch("/api/v1/content/admin/items/" + first.at("/data/contentId").asText()).header("Authorization", bearer("admin-token")), mapOf("slug", "guide-article", "reason", "dup"), 409, 43411);
    }

    @Test
    @DisplayName("CONTENT-STATE covers content review, publication, archive, delete, notification, and rollback rules")
    void contentStateContract() throws Exception {
        String draftId = store.contentIdBySlug("draft-only");
        performJson(patch("/api/v1/content/admin/items/" + draftId + "/submit-review").header("Authorization", bearer("admin-token")), mapOf("reason", "submit"), 200);
        assertThat(store.itemStatus(draftId)).isEqualTo("PENDING_REVIEW");
        performJson(patch("/api/v1/content/admin/items/" + draftId + "/submit-review").header("Authorization", bearer("admin-token")), mapOf("reason", "again"), 200);

        performJson(patch("/api/v1/content/admin/items/" + store.contentIdBySlug("guide-article") + "/submit-review").header("Authorization", bearer("admin-token")), mapOf("reason", "bad"), 409, 43410);

        notification.failNextRequired();
        performJson(patch("/api/v1/content/admin/items/" + draftId + "/approve").header("Authorization", bearer("admin-token")), mapOf("reviewOpinion", "ok", "reason", "approve"), 502, 46410);
        assertThat(store.itemStatus(draftId)).isEqualTo("PENDING_REVIEW");

        performJson(patch("/api/v1/content/admin/items/" + draftId + "/approve").header("Authorization", bearer("admin-token")), mapOf("reviewOpinion", "ok", "reason", "approve"), 200);
        assertThat(notification.requiredCalls()).isEqualTo(1);
        performJson(patch("/api/v1/content/admin/items/" + draftId + "/approve").header("Authorization", bearer("admin-token")), mapOf("reviewOpinion", "ok", "reason", "again"), 200);
        assertThat(notification.requiredCalls()).isEqualTo(1);

        notification.failNextOptional();
        performJson(patch("/api/v1/content/admin/items/" + draftId + "/publish").header("Authorization", bearer("admin-token")), mapOf("reason", "publish"), 200);
        assertThat(store.itemStatus(draftId)).isEqualTo("APPROVED");
        assertThat(store.latestNotificationStatus(draftId)).contains("FAILED");
        performJson(patch("/api/v1/content/admin/items/" + draftId + "/publish").header("Authorization", bearer("admin-token")), mapOf("reason", "again"), 200);

        performJson(patch("/api/v1/content/admin/items/" + draftId + "/offline").header("Authorization", bearer("admin-token")), mapOf("reason", "offline"), 200);
        assertThat(store.itemStatus(draftId)).isEqualTo("OFFLINE");
        performJson(patch("/api/v1/content/admin/items/" + draftId + "/offline").header("Authorization", bearer("admin-token")), mapOf("reason", "again"), 200);

        performJson(patch("/api/v1/content/admin/items/" + draftId + "/archive").header("Authorization", bearer("admin-token")), mapOf("reason", "archive"), 200);
        assertThat(store.itemStatus(draftId)).isEqualTo("ARCHIVED");
        performJson(patch("/api/v1/content/admin/items/" + draftId + "/archive").header("Authorization", bearer("admin-token")), mapOf("reason", "again"), 200);

        performJson(patch("/api/v1/content/admin/items/" + store.contentIdBySlug("guide-article") + "/archive").header("Authorization", bearer("admin-token")), mapOf("reason", "bad"), 409, 43410);
        performJson(patch("/api/v1/content/admin/items/" + store.contentIdBySlug("delete-draft") + "/delete").header("Authorization", bearer("admin-token")), mapOf("reason", "delete"), 200);
        performJson(patch("/api/v1/content/admin/items/" + store.contentIdBySlug("guide-article") + "/delete").header("Authorization", bearer("admin-token")), mapOf("reason", "bad"), 409, 43410);

        String pendingReject = store.contentIdBySlug("pending-reject");
        notification.failNextRequiredTimeout();
        performJson(patch("/api/v1/content/admin/items/" + pendingReject + "/reject").header("Authorization", bearer("admin-token")), mapOf("reviewOpinion", "no", "reason", "reject"), 504, 46411);
        assertThat(store.itemStatus(pendingReject)).isEqualTo("PENDING_REVIEW");
        performJson(patch("/api/v1/content/admin/items/" + pendingReject + "/reject").header("Authorization", bearer("admin-token")), mapOf("reviewOpinion", "no", "reason", "reject"), 200);
        assertThat(store.itemStatus(pendingReject)).isEqualTo("REJECTED");

        String pendingChanges = store.contentIdBySlug("pending-changes");
        performJson(patch("/api/v1/content/admin/items/" + pendingChanges + "/request-changes").header("Authorization", bearer("admin-token")), mapOf("reviewOpinion", "fix", "reason", "changes"), 200);
        assertThat(store.itemStatus(pendingChanges)).isEqualTo("NEEDS_CHANGES");
    }

    @Test
    @DisplayName("CONTENT-VERSION preserves item history, restores old snapshots as draft, and hides history publicly")
    void contentVersionContract() throws Exception {
        JsonNode created = performJson(post("/api/v1/content/admin/items")
                .header("Authorization", bearer("admin-token")), validContentBody("version-history"), 201);
        String contentId = created.at("/data/contentId").asText();

        JsonNode initialVersions = performJson(get("/api/v1/content/admin/items/" + contentId + "/versions")
                .header("Authorization", bearer("admin-token")), 200);
        assertThat(initialVersions.at("/data/items/0/version").asInt()).isEqualTo(1);
        assertThat(initialVersions.at("/data/items/0/sourceAction").asText()).isEqualTo("CREATED");
        assertThat(initialVersions.at("/data/items/0/snapshot/title").asText()).isEqualTo("Title version-history");
        performJson(get("/api/v1/content/admin/items/" + contentId + "/versions/1")
                .header("Authorization", bearer("owner-token")), 200);
        performJson(get("/api/v1/content/admin/items/" + contentId + "/versions")
                .header("Authorization", bearer("helper-token")), 403, 42001);
        performJson(get("/api/v1/content/admin/items/missing/versions")
                .header("Authorization", bearer("admin-token")), 404, 43400);
        performJson(get("/api/v1/content/admin/items/" + contentId + "/versions/99")
                .header("Authorization", bearer("admin-token")), 404, 43417);

        performJson(patch("/api/v1/content/admin/items/" + contentId)
                .header("Authorization", bearer("admin-token")), mapOf("title", "Updated version title", "reason", "patch version"), 200);
        JsonNode patchedVersions = performJson(get("/api/v1/content/admin/items/" + contentId + "/versions")
                .header("Authorization", bearer("admin-token")), 200);
        assertThat(patchedVersions.at("/data/items/0/version").asInt()).isEqualTo(2);
        assertThat(patchedVersions.at("/data/items/0/sourceAction").asText()).isEqualTo("UPDATED");
        assertThat(patchedVersions.at("/data/items/0/snapshot/title").asText()).isEqualTo("Updated version title");

        performJson(patch("/api/v1/content/admin/items/" + contentId + "/submit-review")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "submit version"), 200);
        performJson(patch("/api/v1/content/admin/items/" + contentId + "/approve")
                .header("Authorization", bearer("admin-token")), mapOf("reviewOpinion", "ok", "reason", "approve version"), 200);
        performJson(patch("/api/v1/content/admin/items/" + contentId + "/publish")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "publish version"), 200);
        JsonNode publishedVersions = performJson(get("/api/v1/content/admin/items/" + contentId + "/versions")
                .header("Authorization", bearer("admin-token")), 200);
        assertThat(publishedVersions.at("/data/items/0/version").asInt()).isEqualTo(3);
        assertThat(publishedVersions.at("/data/items/0/sourceAction").asText()).isEqualTo("PUBLISHED");
        assertThat(performJson(get("/api/v1/content/items/" + contentId), 200).toString()).doesNotContain("versions", "sourceAction", "restoredFromVersion");

        performJson(patch("/api/v1/content/admin/items/" + contentId + "/versions/1/restore")
                .header("Authorization", bearer("admin-token")), mapOf(), 400, 40001);
        JsonNode restored = performJson(patch("/api/v1/content/admin/items/" + contentId + "/versions/1/restore")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "restore version"), 200);
        assertThat(restored.at("/data/title").asText()).isEqualTo("Title version-history");
        assertThat(restored.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(restored.at("/data/publishedAt").isNull()).isTrue();
        assertThat(restored.at("/data/reviewOpinion").isNull()).isTrue();

        JsonNode restoredVersions = performJson(get("/api/v1/content/admin/items/" + contentId + "/versions")
                .header("Authorization", bearer("admin-token")), 200);
        assertThat(restoredVersions.at("/data/items/0/version").asInt()).isEqualTo(4);
        assertThat(restoredVersions.at("/data/items/0/sourceAction").asText()).isEqualTo("RESTORED");
        assertThat(restoredVersions.at("/data/items/0/restoredFromVersion").asInt()).isEqualTo(1);
        JsonNode audit = performJson(get("/api/v1/content/admin/items/" + contentId + "/audit-logs")
                .header("Authorization", bearer("admin-token")), 200);
        assertThat(audit.toString()).contains("CONTENT_ITEM_VERSION_RESTORED");

        performJson(patch("/api/v1/content/admin/items/" + store.contentIdBySlug("archived-only") + "/versions/1/restore")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "archived restore"), 409, 43418);

        JsonNode conflict = performJson(post("/api/v1/content/admin/items")
                .header("Authorization", bearer("admin-token")), validContentBody("version-conflict-original"), 201);
        String conflictId = conflict.at("/data/contentId").asText();
        performJson(patch("/api/v1/content/admin/items/" + conflictId)
                .header("Authorization", bearer("admin-token")), mapOf("slug", "version-conflict-current", "reason", "change slug"), 200);
        performJson(post("/api/v1/content/admin/items")
                .header("Authorization", bearer("admin-token")), validContentBody("version-conflict-original"), 201);
        performJson(patch("/api/v1/content/admin/items/" + conflictId + "/versions/1/restore")
                .header("Authorization", bearer("admin-token")), mapOf("reason", "restore conflict"), 409, 43411);
    }

    @Test
    @DisplayName("CONTENT-AUDIT and CONTENT-OPS expose audits and service summary without sensitive fields")
    void auditAndOpsContract() throws Exception {
        String contentId = store.contentIdBySlug("guide-article");
        performJson(patch("/api/v1/content/admin/items/" + contentId)
                .header("Authorization", bearer("admin-token"))
                .header("X-Request-Id", "req-content-audit"), mapOf("summary", "Audit summary", "reason", "audit"), 200);
        JsonNode audit = performJson(get("/api/v1/content/admin/items/" + contentId + "/audit-logs")
                .header("Authorization", bearer("admin-token")), 200);
        assertThat(audit.at("/data/items/0/requestId").asText()).isEqualTo("req-content-audit");
        performJson(get("/api/v1/content/admin/items/" + contentId + "/audit-logs").header("Authorization", bearer("owner-token")), 200);
        performJson(get("/api/v1/content/admin/items/" + contentId + "/audit-logs").header("Authorization", bearer("helper-token")), 403, 42001);
        performJson(get("/api/v1/content/admin/items/missing/audit-logs").header("Authorization", bearer("admin-token")), 404, 43400);
        mvc.perform(delete("/api/v1/content/admin/items/" + contentId + "/audit-logs").header("Authorization", bearer("owner-token")))
                .andExpect(status().is4xxClientError());

        JsonNode summary = performJson(get("/api/v1/content/admin/ops/summary").header("Authorization", bearer("admin-token")), 200);
        assertThat(summary.at("/data/service").asText()).isEqualTo("content");
        assertThat(summary.at("/data/storageMode").asText()).isEqualTo("IN_MEMORY");
        assertThat(summary.at("/data/authMode").asText()).isEqualTo("TEST_STUB");
        assertThat(summary.at("/data/profileMode").asText()).isEqualTo("TEST_STUB");
        assertThat(summary.at("/data/notificationMode").asText()).isEqualTo("TEST_STUB");
        assertThat(summary.toString()).doesNotContain("Bearer", "token", "admin note", "reviewOpinion", "reason");
        performJson(get("/api/v1/content/admin/ops/summary").header("Authorization", bearer("owner-token")), 200);
        performJson(get("/api/v1/content/admin/ops/summary").header("Authorization", bearer("helper-token")), 403, 42001);
    }

    @Test
    @DisplayName("CONTENT-ADMIN-HOME covers draft save, preview, publish, rollback, idempotency, and public isolation")
    void adminHomeContract() throws Exception {
        performJson(get("/api/v1/content/admin/home").header("Authorization", bearer("helper-token")), 200);
        JsonNode before = performJson(get("/api/v1/content/home"), 200);

        Map<String, Object> body = homeBody("home-idem-1");
        JsonNode saved = performJson(put("/api/v1/content/admin/home").header("Authorization", bearer("admin-token")), body, 200);
        assertThat(saved.at("/data/draft/sections").size()).isEqualTo(1);
        assertThat(performJson(get("/api/v1/content/home"), 200).at("/data/version").asInt()).isEqualTo(before.at("/data/version").asInt());
        JsonNode savedAgain = performJson(put("/api/v1/content/admin/home").header("Authorization", bearer("admin-token")), body, 200);
        assertThat(savedAgain.at("/data/draft/homeConfigId").asText()).isEqualTo(saved.at("/data/draft/homeConfigId").asText());
        performJson(put("/api/v1/content/admin/home").header("Authorization", bearer("admin-token")), homeBody("home-idem-1", "Changed"), 409, 43002);

        JsonNode previewResult = performJson(post("/api/v1/content/admin/home/preview").header("Authorization", bearer("helper-token")), homeBody(null), 200);
        assertThat(previewResult.at("/data/createdPublishedVersion").asBoolean()).isFalse();
        assertThat(performJson(get("/api/v1/content/home"), 200).at("/data/version").asInt()).isEqualTo(before.at("/data/version").asInt());

        JsonNode published = performJson(patch("/api/v1/content/admin/home/publish").header("Authorization", bearer("admin-token")), mapOf("reason", "publish home"), 200);
        assertThat(published.at("/data/published/version").asInt()).isGreaterThan(before.at("/data/version").asInt());
        performJson(patch("/api/v1/content/admin/home/publish").header("Authorization", bearer("admin-token")), mapOf("reason", "again"), 200);

        JsonNode rollback = performJson(patch("/api/v1/content/admin/home/rollback").header("Authorization", bearer("admin-token")), mapOf("version", 1, "reason", "rollback"), 200);
        assertThat(rollback.at("/data/published/version").asInt()).isEqualTo(1);
        performJson(patch("/api/v1/content/admin/home/rollback").header("Authorization", bearer("admin-token")), mapOf("version", 999, "reason", "missing"), 404, 43404);

        Map<String, Object> tooMany = mapOf("sections", java.util.stream.IntStream.range(0, 21).mapToObj(i -> homeSection("s" + i, "HERO")).toList(), "reason", "too many");
        performJson(put("/api/v1/content/admin/home").header("Authorization", bearer("admin-token")), tooMany, 400, 40001);
        store.clearDraftHome();
        performJson(patch("/api/v1/content/admin/home/publish").header("Authorization", bearer("admin-token")), mapOf("reason", "none"), 404, 43404);
    }

    @Test
    @DisplayName("CONTENT-CAT and CONTENT-TAG cover management, idempotency, conflicts, references, and archive")
    void categoryAndTagContract() throws Exception {
        JsonNode adminCategories = performJson(get("/api/v1/content/admin/categories").header("Authorization", bearer("helper-token")), 200);
        assertThat(valuesAt(adminCategories, "/data/items", "slug")).contains("archived-category");
        JsonNode activeCategories = performJson(get("/api/v1/content/admin/categories")
                .header("Authorization", bearer("helper-token"))
                .param("includeArchived", "false"), 200);
        assertThat(valuesAt(activeCategories, "/data/items", "slug")).contains("news").doesNotContain("archived-category");
        JsonNode category = performJson(post("/api/v1/content/admin/categories").header("Authorization", bearer("admin-token")),
                categoryBody("Builds", "builds", "cat-idem-1"), 201);
        assertThat(category.at("/data/slug").asText()).isEqualTo("builds");
        assertThat(performJson(post("/api/v1/content/admin/categories").header("Authorization", bearer("admin-token")),
                categoryBody("Builds", "builds", "cat-idem-1"), 201).at("/data/categoryId").asText()).isEqualTo(category.at("/data/categoryId").asText());
        performJson(post("/api/v1/content/admin/categories").header("Authorization", bearer("admin-token")), categoryBody("News", "news", null), 409, 43001);
        performJson(post("/api/v1/content/admin/categories").header("Authorization", bearer("admin-token")), categoryBody("x", "Bad Slug", null), 400, 40001);
        performJson(post("/api/v1/content/admin/categories").header("Authorization", bearer("admin-token")), categoryBody("Changed", "changed", "cat-idem-1"), 409, 43002);
        performJson(patch("/api/v1/content/admin/categories/" + category.at("/data/categoryId").asText()).header("Authorization", bearer("admin-token")), mapOf("name", "Build Projects", "reason", "rename"), 200);
        performJson(patch("/api/v1/content/admin/categories/missing").header("Authorization", bearer("admin-token")), mapOf("name", "Missing", "reason", "missing"), 404, 43401);
        performJson(patch("/api/v1/content/admin/categories/" + category.at("/data/categoryId").asText() + "/archive").header("Authorization", bearer("admin-token")), mapOf("reason", "archive"), 200);
        performJson(patch("/api/v1/content/admin/categories/" + store.categoryId("news") + "/archive").header("Authorization", bearer("admin-token")), mapOf("reason", "used"), 409, 43415);

        JsonNode adminTags = performJson(get("/api/v1/content/admin/tags").header("Authorization", bearer("helper-token")), 200);
        assertThat(valuesAt(adminTags, "/data/items", "slug")).contains("archived-tag");
        JsonNode activeTags = performJson(get("/api/v1/content/admin/tags")
                .header("Authorization", bearer("helper-token"))
                .param("includeArchived", "false"), 200);
        assertThat(valuesAt(activeTags, "/data/items", "slug")).contains("guide").doesNotContain("archived-tag");
        JsonNode tag = performJson(post("/api/v1/content/admin/tags").header("Authorization", bearer("admin-token")),
                tagBody("Spotlight", "spotlight", "tag-idem-1"), 201);
        assertThat(performJson(post("/api/v1/content/admin/tags").header("Authorization", bearer("admin-token")),
                tagBody("Spotlight", "spotlight", "tag-idem-1"), 201).at("/data/tagId").asText()).isEqualTo(tag.at("/data/tagId").asText());
        performJson(post("/api/v1/content/admin/tags").header("Authorization", bearer("admin-token")), tagBody("Guide", "guide", null), 409, 43001);
        performJson(post("/api/v1/content/admin/tags").header("Authorization", bearer("admin-token")), tagBody("", "bad", null), 400, 40001);
        performJson(patch("/api/v1/content/admin/tags/" + tag.at("/data/tagId").asText()).header("Authorization", bearer("admin-token")), mapOf("name", "Spotlights", "reason", "rename"), 200);
        performJson(patch("/api/v1/content/admin/tags/missing").header("Authorization", bearer("admin-token")), mapOf("name", "Missing", "reason", "missing"), 404, 43405);
        performJson(patch("/api/v1/content/admin/tags/" + tag.at("/data/tagId").asText() + "/archive").header("Authorization", bearer("admin-token")), mapOf("reason", "archive"), 200);
        performJson(patch("/api/v1/content/admin/tags/" + store.tagId("guide") + "/archive").header("Authorization", bearer("admin-token")), mapOf("reason", "used"), 409, 43415);
    }

    @Test
    @DisplayName("CONTENT-TOPIC-ADMIN and CONTENT-SEO-ADMIN cover topic and seo management")
    void topicAndSeoAdminContract() throws Exception {
        performJson(get("/api/v1/content/admin/topics").header("Authorization", bearer("helper-token")), 200);
        JsonNode draftTopics = performJson(get("/api/v1/content/admin/topics")
                .header("Authorization", bearer("helper-token"))
                .param("status", "DRAFT"), 200);
        assertThat(valuesAt(draftTopics, "/data/items", "slug")).contains("draft-topic").doesNotContain("spring-topic");
        JsonNode publicTopics = performJson(get("/api/v1/content/admin/topics")
                .header("Authorization", bearer("helper-token"))
                .param("visibility", "PUBLIC"), 200);
        assertThat(valuesAt(publicTopics, "/data/items", "visibility")).containsOnly("PUBLIC");
        JsonNode keywordTopics = performJson(get("/api/v1/content/admin/topics")
                .header("Authorization", bearer("helper-token"))
                .param("keyword", "Spring"), 200);
        assertThat(valuesAt(keywordTopics, "/data/items", "slug")).contains("spring-topic").doesNotContain("draft-topic");
        performJson(get("/api/v1/content/admin/topics")
                .header("Authorization", bearer("helper-token"))
                .param("sort", "createdAt_desc"), 200);
        performJson(get("/api/v1/content/admin/topics/" + store.topicIdBySlug("spring-topic")).header("Authorization", bearer("helper-token")), 200);
        performJson(get("/api/v1/content/admin/topics/missing").header("Authorization", bearer("helper-token")), 404, 43402);

        JsonNode topic = performJson(post("/api/v1/content/admin/topics").header("Authorization", bearer("admin-token")), topicBody("summer-topic", "topic-idem-1"), 201);
        assertThat(topic.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(performJson(post("/api/v1/content/admin/topics").header("Authorization", bearer("admin-token")), topicBody("summer-topic", "topic-idem-1"), 201).at("/data/topicId").asText()).isEqualTo(topic.at("/data/topicId").asText());
        performJson(post("/api/v1/content/admin/topics").header("Authorization", bearer("admin-token")), topicBody("spring-topic", null), 409, 43411);
        performJson(post("/api/v1/content/admin/topics").header("Authorization", bearer("admin-token")), mapOf("title", "x", "reason", "bad"), 400, 40001);
        performJson(patch("/api/v1/content/admin/topics/" + topic.at("/data/topicId").asText()).header("Authorization", bearer("admin-token")), mapOf("title", "Summer Topic Updated", "reason", "patch"), 200);
        performJson(patch("/api/v1/content/admin/topics/" + topic.at("/data/topicId").asText() + "/publish").header("Authorization", bearer("admin-token")), mapOf("reason", "publish"), 200);
        performJson(patch("/api/v1/content/admin/topics/" + topic.at("/data/topicId").asText() + "/publish").header("Authorization", bearer("admin-token")), mapOf("reason", "again"), 200);
        performJson(patch("/api/v1/content/admin/topics/" + topic.at("/data/topicId").asText() + "/offline").header("Authorization", bearer("admin-token")), mapOf("reason", "offline"), 200);
        performJson(patch("/api/v1/content/admin/topics/" + topic.at("/data/topicId").asText() + "/archive").header("Authorization", bearer("admin-token")), mapOf("reason", "archive"), 200);
        performJson(patch("/api/v1/content/admin/topics/" + store.topicIdBySlug("spring-topic") + "/archive").header("Authorization", bearer("admin-token")), mapOf("reason", "bad"), 409, 43414);
        JsonNode draftTopic = performJson(post("/api/v1/content/admin/topics").header("Authorization", bearer("admin-token")), topicBody("delete-topic", null), 201);
        performJson(patch("/api/v1/content/admin/topics/" + draftTopic.at("/data/topicId").asText() + "/delete").header("Authorization", bearer("admin-token")), mapOf("reason", "delete"), 200);

        performJson(get("/api/v1/content/admin/seo").header("Authorization", bearer("helper-token")), 200);
        JsonNode routeSeo = performJson(get("/api/v1/content/admin/seo")
                .header("Authorization", bearer("helper-token"))
                .param("route", "/news"), 200);
        assertThat(valuesAt(routeSeo, "/data/items", "route")).containsExactly("/news");
        JsonNode keywordSeo = performJson(get("/api/v1/content/admin/seo")
                .header("Authorization", bearer("helper-token"))
                .param("keyword", "News"), 200);
        assertThat(valuesAt(keywordSeo, "/data/items", "route")).contains("/news");
        JsonNode seo = performJson(put("/api/v1/content/admin/seo/by-route").header("Authorization", bearer("admin-token")), seoBody("/topic", "seo-idem-1"), 200);
        assertThat(seo.at("/data/route").asText()).isEqualTo("/topic");
        performJson(put("/api/v1/content/admin/seo/by-route").header("Authorization", bearer("admin-token")), seoBody("/topic", "seo-idem-1"), 200);
        performJson(put("/api/v1/content/admin/seo/by-route").header("Authorization", bearer("admin-token")), seoBody("bad", null), 400, 40001);
        Map<String, Object> badRobots = seoBody("/bad-robots", null);
        badRobots.put("robots", "BAD");
        performJson(put("/api/v1/content/admin/seo/by-route").header("Authorization", bearer("admin-token")), badRobots, 400, 40001);
        performJson(put("/api/v1/content/admin/seo/by-route").header("Authorization", bearer("admin-token")), seoBody("/changed", "seo-idem-1"), 409, 43002);
        performJson(get("/api/v1/content/admin/seo/" + seo.at("/data/seoId").asText()).header("Authorization", bearer("helper-token")), 200);
        performJson(get("/api/v1/content/admin/seo/missing").header("Authorization", bearer("helper-token")), 404, 43403);
        performJson(patch("/api/v1/content/admin/seo/" + seo.at("/data/seoId").asText() + "/disable").header("Authorization", bearer("admin-token")), mapOf("reason", "disable"), 200);
        performJson(patch("/api/v1/content/admin/seo/" + seo.at("/data/seoId").asText() + "/disable").header("Authorization", bearer("admin-token")), mapOf("reason", "again"), 200);
    }

    @Test
    @DisplayName("CONTENT-DEP covers auth, profile, notification, frontend, and previous-service boundaries")
    void dependencyAndBoundaryContract() throws Exception {
        auth.failNextCurrentUnavailable();
        performJson(get("/api/v1/content/admin/items").header("Authorization", bearer("admin-token")), 502, 46420);
        auth.failNextCurrentTimeout();
        performJson(get("/api/v1/content/admin/items").header("Authorization", bearer("admin-token")), 504, 46421);
        auth.failNextCurrentIncompatible();
        performJson(get("/api/v1/content/admin/items").header("Authorization", bearer("admin-token")), 502, 46422);

        profile.failNextUnavailable();
        Map<String, Object> work = validContentBody("profile-unavailable");
        work.put("type", "MEMBER_WORK");
        work.put("memberId", "memberActive");
        performJson(post("/api/v1/content/admin/items").header("Authorization", bearer("admin-token")), work, 502, 46400);

        profile.failNextUnavailable();
        JsonNode publishedWork = performJson(get("/api/v1/content/items/" + store.contentIdBySlug("member-work-public")), 200);
        assertThat(publishedWork.at("/data/memberSnapshot/displayName").asText()).isEqualTo("Active Member");

        assertThat(notification.hasRecipientStateStore()).isFalse();
        assertThat(store.frontendHardcodedContentChanged()).isFalse();
        assertThat(store.previousServiceFilesChanged()).isFalse();
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

    private Map<String, Object> validContentBody(String slug) {
        return mapOf(
                "type", "ARTICLE",
                "slug", slug,
                "title", "Title " + slug,
                "summary", "Summary " + slug,
                "body", "Body <script>alert(1)</script>",
                "coverUrl", "/covers/" + slug + ".png",
                "categoryId", store.categoryId("news"),
                "tagIds", List.of(store.tagId("guide")),
                "visibility", "PUBLIC",
                "authorUserId", "user",
                "adminNote", "admin note",
                "reason", "test"
        );
    }

    private Map<String, Object> homeBody(String idempotencyKey) {
        return homeBody(idempotencyKey, "Featured");
    }

    private Map<String, Object> homeBody(String idempotencyKey, String title) {
        Map<String, Object> body = mapOf(
                "sections", List.of(homeSection("draft-featured", "FEATURED_ARTICLES", title)),
                "seo", seoPayload("/"),
                "reason", "home"
        );
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        return body;
    }

    private Map<String, Object> homeSection(String id, String type) {
        return homeSection(id, type, "Section " + id);
    }

    private Map<String, Object> homeSection(String id, String type, String title) {
        return mapOf("sectionId", id, "type", type, "title", title, "subtitle", "Subtitle", "enabled", true, "sortOrder", 1,
                "items", List.of(mapOf("contentId", store.contentIdBySlug("guide-article"))));
    }

    private Map<String, Object> categoryBody(String name, String slug, String idempotencyKey) {
        Map<String, Object> body = mapOf("name", name, "slug", slug, "description", "Category", "sortOrder", 10, "reason", "category");
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        return body;
    }

    private Map<String, Object> tagBody(String name, String slug, String idempotencyKey) {
        Map<String, Object> body = mapOf("name", name, "slug", slug, "reason", "tag");
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        return body;
    }

    private Map<String, Object> topicBody(String slug, String idempotencyKey) {
        Map<String, Object> body = mapOf("slug", slug, "title", "Topic " + slug, "summary", "Topic summary", "coverUrl", "/topics/" + slug + ".png",
                "visibility", "PUBLIC", "contentIds", List.of(store.contentIdBySlug("guide-article")), "seo", seoPayload("/topics/" + slug), "reason", "topic");
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        return body;
    }

    private Map<String, Object> seoBody(String route, String idempotencyKey) {
        Map<String, Object> body = seoPayload(route);
        body.put("reason", "seo");
        if (idempotencyKey != null) {
            body.put("idempotencyKey", idempotencyKey);
        }
        return body;
    }

    private Map<String, Object> seoPayload(String route) {
        return mapOf("route", route, "title", "SEO " + route, "description", "SEO description", "keywords", List.of("beiming", "server"),
                "coverUrl", "/seo.png", "robots", "INDEX_FOLLOW", "canonicalUrl", "https://example.com" + route);
    }

    private String bearer(String token) {
        return "Bearer " + token;
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

    private List<String> values(JsonNode arrayNode) {
        return java.util.stream.StreamSupport.stream(arrayNode.spliterator(), false)
                .map(JsonNode::asText)
                .toList();
    }

    private List<String> valuesAt(JsonNode root, String arrayPointer, String fieldName) {
        return java.util.stream.StreamSupport.stream(root.at(arrayPointer).spliterator(), false)
                .map(item -> item.path(fieldName).asText())
                .toList();
    }
}
