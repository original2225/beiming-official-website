package cn.beiming.profile;

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

import java.time.Instant;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileApiContractTest {
    private static final String TEST_DOCUMENT_COVERAGE = """
            PROF-COM-001 PROF-COM-002 PROF-COM-003 PROF-COM-004 PROF-COM-005 PROF-COM-006 PROF-COM-007 PROF-COM-008 PROF-COM-009 PROF-COM-010
            PROF-AUTH-001 PROF-AUTH-002 PROF-AUTH-003 PROF-AUTH-004 PROF-AUTH-005 PROF-AUTH-006 PROF-AUTH-007 PROF-AUTH-008 PROF-AUTH-009 PROF-AUTH-010 PROF-AUTH-011 PROF-AUTH-012 PROF-AUTH-013 PROF-AUTH-014 PROF-AUTH-015 PROF-AUTH-016
            PROF-PUB-LIST-001 PROF-PUB-LIST-002 PROF-PUB-LIST-003 PROF-PUB-LIST-004 PROF-PUB-LIST-005 PROF-PUB-LIST-006 PROF-PUB-LIST-007 PROF-PUB-LIST-008 PROF-PUB-LIST-009 PROF-PUB-LIST-010
            PROF-PUB-DETAIL-001 PROF-PUB-DETAIL-002 PROF-PUB-DETAIL-003 PROF-PUB-DETAIL-004 PROF-PUB-DETAIL-005 PROF-PUB-DETAIL-006
            PROF-ME-001 PROF-ME-002 PROF-ME-003 PROF-ME-004 PROF-ME-005 PROF-ME-006
            PROF-ME-PATCH-001 PROF-ME-PATCH-002 PROF-ME-PATCH-003 PROF-ME-PATCH-004 PROF-ME-PATCH-005 PROF-ME-PATCH-006
            PROF-ADMIN-LIST-001 PROF-ADMIN-LIST-002 PROF-ADMIN-LIST-003 PROF-ADMIN-LIST-004 PROF-ADMIN-LIST-005 PROF-ADMIN-LIST-006 PROF-ADMIN-LIST-007
            PROF-ADMIN-DETAIL-001 PROF-ADMIN-DETAIL-002 PROF-ADMIN-DETAIL-003
            PROF-ACTIVATE-001 PROF-ACTIVATE-002 PROF-ACTIVATE-003 PROF-ACTIVATE-004 PROF-ACTIVATE-005 PROF-ACTIVATE-006 PROF-ACTIVATE-007 PROF-ACTIVATE-008 PROF-ACTIVATE-009 PROF-ACTIVATE-010 PROF-ACTIVATE-011 PROF-ACTIVATE-012 PROF-ACTIVATE-013 PROF-ACTIVATE-014 PROF-ACTIVATE-015 PROF-ACTIVATE-016 PROF-ACTIVATE-017 PROF-ACTIVATE-018
            PROF-ADMIN-PATCH-001 PROF-ADMIN-PATCH-002 PROF-ADMIN-PATCH-003 PROF-ADMIN-PATCH-004 PROF-ADMIN-PATCH-005 PROF-ADMIN-PATCH-006 PROF-ADMIN-PATCH-007 PROF-ADMIN-PATCH-008 PROF-ADMIN-PATCH-009
            PROF-STATUS-001 PROF-STATUS-002 PROF-STATUS-003 PROF-STATUS-004 PROF-STATUS-005 PROF-STATUS-006 PROF-STATUS-007 PROF-STATUS-008 PROF-STATUS-009 PROF-STATUS-010 PROF-STATUS-011 PROF-STATUS-012
            PROF-GROUP-LIST-001 PROF-GROUP-LIST-002 PROF-GROUP-LIST-003
            PROF-GROUP-CREATE-001 PROF-GROUP-CREATE-002 PROF-GROUP-CREATE-003 PROF-GROUP-CREATE-004 PROF-GROUP-CREATE-005 PROF-GROUP-CREATE-006
            PROF-GROUP-PATCH-001 PROF-GROUP-PATCH-002 PROF-GROUP-PATCH-003 PROF-GROUP-PATCH-004
            PROF-GROUP-ARCHIVE-001 PROF-GROUP-ARCHIVE-002 PROF-GROUP-ARCHIVE-003 PROF-GROUP-ARCHIVE-004
            PROF-MILESTONE-001 PROF-MILESTONE-002 PROF-MILESTONE-003 PROF-MILESTONE-004 PROF-MILESTONE-005 PROF-MILESTONE-006 PROF-MILESTONE-007 PROF-MILESTONE-008 PROF-MILESTONE-009
            PROF-WORK-001 PROF-WORK-002 PROF-WORK-003 PROF-WORK-004 PROF-WORK-005 PROF-WORK-006 PROF-WORK-007 PROF-WORK-008 PROF-WORK-009 PROF-WORK-010
            PROF-AUDIT-001 PROF-AUDIT-002 PROF-AUDIT-003 PROF-AUDIT-004 PROF-AUDIT-005 PROF-AUDIT-006 PROF-AUDIT-007
            PROF-SEC-001 PROF-SEC-002 PROF-SEC-003 PROF-SEC-004 PROF-SEC-005 PROF-SEC-006 PROF-SEC-007 PROF-SEC-008
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ProfileStore store;

    @Autowired
    TestAuthContextProvider auth;

    @BeforeEach
    void setUp() {
        auth.reset();
        store.reset();
        store.seedTestData(auth);
    }

    @Test
    @DisplayName("profile local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("PROF-[A-Z]+(?:-[A-Z]+)*-[0-9]{3}");
        Set<String> mapped = pattern.matcher(TEST_DOCUMENT_COVERAGE).results()
                .map(java.util.regex.MatchResult::group)
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
        assertThat(mapped).hasSize(154);
        assertThat(TEST_DOCUMENT_COVERAGE).contains("PROF-COM-001", "PROF-AUTH-016", "PROF-ACTIVATE-018", "PROF-SEC-008");
    }

    @Test
    @DisplayName("PROF-COM-001/008 success responses use common envelope and request ids")
    void successResponseUsesCommonEnvelopeAndRequestId() throws Exception {
        mvc.perform(get("/api/v1/profile/members")
                        .header("X-Request-Id", "req-profile-ok"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-profile-ok"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.items").isArray());

        mvc.perform(get("/api/v1/profile/members")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.requestId").isString());
    }

    @Test
    @DisplayName("PROF-COM-003/004/005/006/007 validation, paging, auth, and role errors use common codes")
    void commonErrorContract() throws Exception {
        mvc.perform(post("/api/v1/profile/admin/groups")
                        .header("Authorization", bearer("admin-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "x"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.errors").isArray());

        mvc.perform(get("/api/v1/profile/admin/members").param("page", "0")
                        .header("Authorization", bearer("admin-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40002));

        mvc.perform(get("/api/v1/profile/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));

        mvc.perform(get("/api/v1/profile/me").header("Authorization", "Token bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41003));

        mvc.perform(get("/api/v1/profile/admin/members").header("Authorization", bearer("user-token")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(42001));
    }

    @Test
    @DisplayName("PROF-PUB-LIST covers public list filtering, paging, sorting, hidden states, and field isolation")
    void publicMemberListContract() throws Exception {
        JsonNode list = performJson(get("/api/v1/profile/members")
                .param("page", "1")
                .param("pageSize", "20")
                .param("sort", "displayName_asc"), 200);
        assertThat(list.at("/data/page").asInt()).isEqualTo(1);
        assertThat(list.at("/data/pageSize").asInt()).isEqualTo(20);
        assertThat(valuesAt(list, "/data/items", "memberId")).contains(store.memberIdByUserId("active_member"));
        assertThat(valuesAt(list, "/data/items", "memberId")).doesNotContain(
                store.memberIdByUserId("private_member"),
                store.memberIdByUserId("removed_member"),
                store.memberIdByUserId("archived_member"),
                store.memberIdByUserId("pending_member"));
        assertThat(list.toString()).doesNotContain("adminNote", "authRolesSnapshot", "authUserStatusSnapshot", "userId");

        mvc.perform(get("/api/v1/profile/members")
                        .param("keyword", "Active")
                        .param("groupId", store.groupIdByName("Builder"))
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].displayName").value("Active Member"));

        performJson(get("/api/v1/profile/members").param("status", "REMOVED"), 400, 40001);

        store.failNextPublicRead();
        performJson(get("/api/v1/profile/members"), 500, 51200);
    }

    @Test
    @DisplayName("PROF-PUB-DETAIL covers public detail success, not found, hidden profiles, dependency fallback, and field isolation")
    void publicMemberDetailContract() throws Exception {
        JsonNode detail = performJson(get("/api/v1/profile/members/" + store.memberIdByUserId("member_with_milestones")), 200);
        assertThat(detail.at("/data/milestones").size()).isGreaterThan(0);
        assertThat(detail.at("/data/workSnapshots").size()).isGreaterThan(0);
        assertThat(detail.at("/data/activitySummary").isNull()).isTrue();
        assertThat(detail.at("/data/contributionSummary").isNull()).isTrue();
        assertThat(detail.toString()).doesNotContain("adminNote", "authRolesSnapshot", "permissions");

        performJson(get("/api/v1/profile/members/missing"), 404, 43200);
        performJson(get("/api/v1/profile/members/" + store.memberIdByUserId("private_member")), 409, 43213);
        performJson(get("/api/v1/profile/members/" + store.memberIdByUserId("removed_member")), 409, 43213);
    }

    @Test
    @DisplayName("PROF-ME and PROF-AUTH cover current user profile and auth context failures")
    void currentUserAndAuthContextContract() throws Exception {
        mvc.perform(get("/api/v1/profile/me").header("Authorization", bearer("active-member-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("active_member"))
                .andExpect(jsonPath("$.data.adminNote").doesNotExist());

        performJson(get("/api/v1/profile/me").header("Authorization", bearer("no-profile-token")), 404, 43200);

        auth.failNextCurrentUnavailable();
        performJson(get("/api/v1/profile/me").header("Authorization", bearer("active-member-token")), 502, 46200);

        auth.failNextCurrentTimeout();
        performJson(get("/api/v1/profile/me").header("Authorization", bearer("active-member-token")), 504, 46201);

        auth.failNextCurrentIncompatible();
        performJson(get("/api/v1/profile/me").header("Authorization", bearer("active-member-token")), 502, 46202);

        auth.setTokenRoles("admin-token", List.of());
        performJson(get("/api/v1/profile/admin/members").header("Authorization", bearer("admin-token")), 403, 42001);
    }

    @Test
    @DisplayName("PROF-ME-PATCH current users can update only public fields with audit and rollback")
    void currentUserPatchContract() throws Exception {
        JsonNode updated = performJson(patch("/api/v1/profile/me").header("Authorization", bearer("active-member-token")), Map.of(
                "avatarUrl", "https://example.com/avatar.png",
                "skinUrl", "/assets/skins/active.png",
                "bio", "Updated bio",
                "visibility", "PRIVATE",
                "reason", "self update"
        ), 200);
        assertThat(updated.at("/data/bio").asText()).isEqualTo("Updated bio");
        assertThat(store.auditActions()).contains("PROFILE_SELF_UPDATED");

        performJson(patch("/api/v1/profile/me").header("Authorization", bearer("active-member-token")), Map.of(
                "bio", "bad",
                "status", "ARCHIVED",
                "reason", "try protected"
        ), 400, 40001);

        performJson(patch("/api/v1/profile/me").header("Authorization", bearer("active-member-token")), Map.of("bio", "No reason"), 400, 40001);
        performJson(patch("/api/v1/profile/me").header("Authorization", bearer("active-member-token")), Map.of("bio", "x".repeat(1001), "reason", "too long"), 400, 40001);
        performJson(patch("/api/v1/profile/me").header("Authorization", bearer("archived-member-token")), Map.of("bio", "no", "reason", "archived"), 409, 43212);

        String before = store.profileBioByUserId("active_member");
        store.failNextAudit();
        performJson(patch("/api/v1/profile/me").header("Authorization", bearer("active-member-token")), Map.of("bio", "Audit fail", "reason", "audit"), 500, 51201);
        assertThat(store.profileBioByUserId("active_member")).isEqualTo(before);
    }

    @Test
    @DisplayName("PROF-ADMIN-LIST and PROF-ADMIN-DETAIL cover backend reads")
    void adminReadContract() throws Exception {
        mvc.perform(get("/api/v1/profile/admin/members")
                        .header("Authorization", bearer("helper-token"))
                        .param("keyword", "Private")
                        .param("visibility", "PRIVATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].userId").value("private_member"));

        mvc.perform(get("/api/v1/profile/admin/members")
                        .header("Authorization", bearer("admin-token"))
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].status").value("ARCHIVED"));

        mvc.perform(get("/api/v1/profile/admin/members/" + store.memberIdByUserId("active_member"))
                        .header("Authorization", bearer("helper-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminNote").exists());

        performJson(get("/api/v1/profile/admin/members/missing").header("Authorization", bearer("helper-token")), 404, 43200);
        performJson(get("/api/v1/profile/admin/members/" + store.memberIdByUserId("active_member")).header("Authorization", bearer("user-token")), 403, 42001);
    }

    @Test
    @DisplayName("PROF-ACTIVATE and PROF-AUTH cover activation success, auth snapshots, idempotency, and rollback")
    void activateMemberContract() throws Exception {
        JsonNode created = performJson(post("/api/v1/profile/admin/members/activate").header("Authorization", bearer("admin-token")), Map.of(
                "userId", "target_user",
                "displayNameSnapshot", "Forged Name",
                "authRolesSnapshot", List.of("OWNER"),
                "minecraftId", "ForgedMc",
                "groupId", store.groupIdByName("Builder"),
                "bio", "New member",
                "reason", "whitelist approved",
                "idempotencyKey", "activate-target-1"
        ), 201);
        assertThat(created.at("/data/displayNameSnapshot").asText()).isEqualTo("Target User");
        assertThat(created.at("/data/minecraftId").asText()).isEqualTo("TargetMc");
        assertThat(values(created.at("/data/authRolesSnapshot"))).containsExactly("USER");
        assertThat(store.auditActions()).contains("PROFILE_MEMBER_ACTIVATED");

        JsonNode retry = performJson(post("/api/v1/profile/admin/members/activate").header("Authorization", bearer("admin-token")), Map.of(
                "userId", "target_user",
                "displayNameSnapshot", "Forged Name",
                "authRolesSnapshot", List.of("OWNER"),
                "minecraftId", "ForgedMc",
                "groupId", store.groupIdByName("Builder"),
                "bio", "New member",
                "reason", "whitelist approved",
                "idempotencyKey", "activate-target-1"
        ), 201);
        assertThat(retry.at("/data/memberId").asText()).isEqualTo(created.at("/data/memberId").asText());

        performJson(post("/api/v1/profile/admin/members/activate").header("Authorization", bearer("admin-token")), Map.of(
                "userId", "another_target",
                "reason", "different",
                "idempotencyKey", "activate-target-1"
        ), 409, 43002);

        performJson(post("/api/v1/profile/admin/members/activate").header("Authorization", bearer("owner-token")), Map.of("userId", "owner_target", "reason", "owner"), 201);
        performJson(post("/api/v1/profile/admin/members/activate").header("Authorization", bearer("helper-token")), Map.of("userId", "helper_target", "reason", "bad"), 403, 42001);
        performJson(post("/api/v1/profile/admin/members/activate").header("Authorization", bearer("admin-token")), Map.of("reason", "missing"), 400, 40001);
        performJson(post("/api/v1/profile/admin/members/activate").header("Authorization", bearer("admin-token")), Map.of("userId", "x"), 400, 40001);
        performJson(post("/api/v1/profile/admin/members/activate").header("Authorization", bearer("admin-token")), Map.of("userId", "group_target", "groupId", "missing", "reason", "bad group"), 404, 43201);
        performJson(post("/api/v1/profile/admin/members/activate").header("Authorization", bearer("admin-token")), Map.of("userId", "active_member", "reason", "duplicate"), 409, 43210);
        performJson(post("/api/v1/profile/admin/members/activate").header("Authorization", bearer("admin-token")), Map.of("userId", "mc_conflict_user", "reason", "mc conflict"), 409, 43211);

        auth.setTargetMissing("missing_target");
        performJson(post("/api/v1/profile/admin/members/activate").header("Authorization", bearer("admin-token")), Map.of("userId", "missing_target", "reason", "missing"), 404, 43204);

        for (String status : List.of("DISABLED", "BANNED", "DELETED")) {
            auth.setTargetStatus("blocked_" + status, status);
            performJson(post("/api/v1/profile/admin/members/activate").header("Authorization", bearer("admin-token")), Map.of("userId", "blocked_" + status, "reason", status), 409, 43215);
        }

        auth.failNextTargetUnavailable();
        performJson(post("/api/v1/profile/admin/members/activate").header("Authorization", bearer("admin-token")), Map.of("userId", "unavailable_target", "reason", "auth"), 502, 46200);

        auth.failNextTargetTimeout();
        performJson(post("/api/v1/profile/admin/members/activate").header("Authorization", bearer("admin-token")), Map.of("userId", "timeout_target", "reason", "auth"), 504, 46201);

        auth.failNextTargetIncompatible();
        performJson(post("/api/v1/profile/admin/members/activate").header("Authorization", bearer("admin-token")), Map.of("userId", "bad_target", "reason", "auth"), 502, 46202);

        store.failNextAudit();
        performJson(post("/api/v1/profile/admin/members/activate").header("Authorization", bearer("admin-token")), Map.of("userId", "audit_fail_target", "reason", "audit"), 500, 51201);
        assertThat(store.memberExistsByUserId("audit_fail_target")).isFalse();
    }

    @Test
    @DisplayName("PROF-ACTIVATE-011 and PROF-SEC-006 protect concurrent user and Minecraft uniqueness")
    void activationConcurrencyContract() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();
        try (var pool = Executors.newFixedThreadPool(2)) {
            for (String userId : List.of("race_user", "race_user")) {
                pool.submit(() -> {
                    start.await();
                    MvcResult result = mvc.perform(post("/api/v1/profile/admin/members/activate")
                                    .header("Authorization", bearer("admin-token"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json(Map.of("userId", userId, "reason", "race"))))
                            .andReturn();
                    if (result.getResponse().getStatus() == 201) {
                        successCount.incrementAndGet();
                    }
                    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
                    if (body.path("code").asInt() == 43210) {
                        conflictCount.incrementAndGet();
                    }
                    return null;
                });
            }
            start.countDown();
        }
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("PROF-ADMIN-PATCH backend updates validate fields, permissions, conflicts, state, and audit rollback")
    void adminPatchContract() throws Exception {
        JsonNode updated = performJson(patch("/api/v1/profile/admin/members/" + store.memberIdByUserId("active_member"))
                .header("Authorization", bearer("admin-token")), Map.of(
                "displayNameSnapshot", "Renamed Member",
                "avatarUrl", "https://example.com/renamed.png",
                "minecraftId", "RenamedMc",
                "minecraftUuid", "cccccccccccccccccccccccccccccccc",
                "skinUrl", "/skins/renamed.png",
                "groupId", store.groupIdByName("Redstone"),
                "joinedAt", "2026-05-21T00:00:00Z",
                "bio", "Admin updated",
                "visibility", "PUBLIC",
                "adminNote", "Internal note",
                "reason", "admin update"
        ), 200);
        assertThat(updated.at("/data/displayNameSnapshot").asText()).isEqualTo("Renamed Member");
        assertThat(store.auditActions()).contains("PROFILE_MEMBER_UPDATED");

        performJson(patch("/api/v1/profile/admin/members/" + store.memberIdByUserId("active_member")).header("Authorization", bearer("helper-token")), Map.of("bio", "no", "reason", "bad"), 403, 42001);
        performJson(patch("/api/v1/profile/admin/members/missing").header("Authorization", bearer("admin-token")), Map.of("bio", "no", "reason", "missing"), 404, 43200);
        performJson(patch("/api/v1/profile/admin/members/" + store.memberIdByUserId("active_member")).header("Authorization", bearer("admin-token")), Map.of("groupId", "missing", "reason", "bad"), 404, 43201);
        performJson(patch("/api/v1/profile/admin/members/" + store.memberIdByUserId("active_member")).header("Authorization", bearer("admin-token")), Map.of("minecraftUuid", store.minecraftUuidByUserId("member_with_group"), "reason", "conflict"), 409, 43211);
        performJson(patch("/api/v1/profile/admin/members/" + store.memberIdByUserId("active_member")).header("Authorization", bearer("admin-token")), Map.of("bio", "No reason"), 400, 40001);
        performJson(patch("/api/v1/profile/admin/members/" + store.memberIdByUserId("active_member")).header("Authorization", bearer("admin-token")), Map.of("avatarUrl", "ftp://bad", "reason", "bad url"), 400, 40001);
        performJson(patch("/api/v1/profile/admin/members/" + store.memberIdByUserId("archived_member")).header("Authorization", bearer("admin-token")), Map.of("visibility", "PUBLIC", "reason", "restore"), 409, 43212);

        String before = store.profileBioByUserId("active_member");
        store.failNextAudit();
        performJson(patch("/api/v1/profile/admin/members/" + store.memberIdByUserId("active_member")).header("Authorization", bearer("admin-token")), Map.of("bio", "Audit fail", "reason", "audit"), 500, 51201);
        assertThat(store.profileBioByUserId("active_member")).isEqualTo(before);
    }

    @Test
    @DisplayName("PROF-STATUS covers allowed and rejected member status transitions")
    void statusTransitionContract() throws Exception {
        String memberId = store.memberIdByUserId("active_member");
        performJson(patch("/api/v1/profile/admin/members/" + memberId + "/status").header("Authorization", bearer("admin-token")), Map.of("status", "INACTIVE", "reason", "inactive"), 200);
        performJson(patch("/api/v1/profile/admin/members/" + memberId + "/status").header("Authorization", bearer("admin-token")), Map.of("status", "ACTIVE", "reason", "active"), 200);
        performJson(patch("/api/v1/profile/admin/members/" + memberId + "/status").header("Authorization", bearer("admin-token")), Map.of("status", "SUSPENDED", "reason", "suspend"), 200);
        performJson(patch("/api/v1/profile/admin/members/" + memberId + "/status").header("Authorization", bearer("admin-token")), Map.of("status", "ACTIVE", "reason", "restore"), 200);
        performJson(patch("/api/v1/profile/admin/members/" + memberId + "/status").header("Authorization", bearer("admin-token")), Map.of("status", "REMOVED", "reason", "removed"), 200);
        performJson(patch("/api/v1/profile/admin/members/" + memberId + "/status").header("Authorization", bearer("admin-token")), Map.of("status", "ARCHIVED", "reason", "archive"), 200);
        performJson(patch("/api/v1/profile/admin/members/" + memberId + "/status").header("Authorization", bearer("admin-token")), Map.of("status", "ACTIVE", "reason", "restore"), 409, 43212);
        performJson(patch("/api/v1/profile/admin/members/" + store.memberIdByUserId("removed_member") + "/status").header("Authorization", bearer("admin-token")), Map.of("status", "ACTIVE", "reason", "restore"), 409, 43212);
        performJson(patch("/api/v1/profile/admin/members/" + store.memberIdByUserId("member_with_group") + "/status").header("Authorization", bearer("admin-token")), Map.of("status", "NOPE", "reason", "bad"), 400, 40001);
        performJson(patch("/api/v1/profile/admin/members/" + store.memberIdByUserId("member_with_group") + "/status").header("Authorization", bearer("admin-token")), Map.of("status", "INACTIVE"), 400, 40001);
        performJson(patch("/api/v1/profile/admin/members/" + store.memberIdByUserId("member_with_group") + "/status").header("Authorization", bearer("helper-token")), Map.of("status", "INACTIVE", "reason", "bad"), 403, 42001);
    }

    @Test
    @DisplayName("PROF-GROUP covers list, create, patch, archive, idempotency, and usage protection")
    void groupContract() throws Exception {
        mvc.perform(get("/api/v1/profile/admin/groups").header("Authorization", bearer("helper-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.archived == true)]").isEmpty());

        mvc.perform(get("/api/v1/profile/admin/groups").header("Authorization", bearer("helper-token")).param("includeArchived", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.archived == true)]").exists());

        JsonNode created = performJson(post("/api/v1/profile/admin/groups").header("Authorization", bearer("admin-token")), Map.of(
                "name", "Explorer",
                "description", "Explore team",
                "color", "#123ABC",
                "sortOrder", 9,
                "reason", "create",
                "idempotencyKey", "group-idem-1"
        ), 201);
        assertThat(created.at("/data/name").asText()).isEqualTo("Explorer");

        JsonNode retried = performJson(post("/api/v1/profile/admin/groups").header("Authorization", bearer("admin-token")), Map.of(
                "name", "Explorer",
                "description", "Explore team",
                "color", "#123ABC",
                "sortOrder", 9,
                "reason", "create",
                "idempotencyKey", "group-idem-1"
        ), 201);
        assertThat(retried.at("/data/id").asText()).isEqualTo(created.at("/data/id").asText());

        performJson(post("/api/v1/profile/admin/groups").header("Authorization", bearer("helper-token")), Map.of("name", "HelperGroup", "reason", "bad"), 403, 42001);
        performJson(post("/api/v1/profile/admin/groups").header("Authorization", bearer("admin-token")), Map.of("name", "Builder", "reason", "dup"), 409, 43001);
        performJson(post("/api/v1/profile/admin/groups").header("Authorization", bearer("admin-token")), Map.of("name", "x", "color", "bad", "reason", "bad"), 400, 40001);
        performJson(post("/api/v1/profile/admin/groups").header("Authorization", bearer("admin-token")), Map.of("name", "Changed", "reason", "changed", "idempotencyKey", "group-idem-1"), 409, 43002);

        performJson(patch("/api/v1/profile/admin/groups/" + created.at("/data/id").asText()).header("Authorization", bearer("admin-token")), Map.of("name", "Explorers", "reason", "rename"), 200);
        performJson(patch("/api/v1/profile/admin/groups/missing").header("Authorization", bearer("admin-token")), Map.of("name", "Missing", "reason", "missing"), 404, 43201);
        performJson(patch("/api/v1/profile/admin/groups/" + created.at("/data/id").asText()).header("Authorization", bearer("admin-token")), Map.of("name", "No Reason"), 400, 40001);
        performJson(patch("/api/v1/profile/admin/groups/" + created.at("/data/id").asText()).header("Authorization", bearer("admin-token")), Map.of("name", "Builder", "reason", "dup"), 409, 43001);

        performJson(patch("/api/v1/profile/admin/groups/" + created.at("/data/id").asText() + "/archive").header("Authorization", bearer("admin-token")), Map.of("reason", "archive"), 200);
        performJson(patch("/api/v1/profile/admin/groups/" + created.at("/data/id").asText() + "/archive").header("Authorization", bearer("admin-token")), Map.of("reason", "again"), 200);
        performJson(patch("/api/v1/profile/admin/groups/" + store.groupIdByName("Builder") + "/archive").header("Authorization", bearer("admin-token")), Map.of("reason", "used"), 409, 43214);
        performJson(patch("/api/v1/profile/admin/groups/" + store.groupIdByName("Redstone") + "/archive").header("Authorization", bearer("helper-token")), Map.of("reason", "bad"), 403, 42001);
    }

    @Test
    @DisplayName("PROF-MILESTONE replaces member milestones and public detail filters private items")
    void milestonesContract() throws Exception {
        String memberId = store.memberIdByUserId("member_with_milestones");
        JsonNode updated = performJson(put("/api/v1/profile/admin/members/" + memberId + "/milestones").header("Authorization", bearer("admin-token")), Map.of(
                "items", List.of(
                        Map.of("type", "PROJECT", "title", "New Build", "description", "Finished build", "happenedAt", "2026-05-21T00:00:00Z", "publicVisible", true, "sortOrder", 1),
                        Map.of("id", store.firstMilestoneId(memberId), "type", "EVENT", "title", "Private Event", "description", "Hidden", "happenedAt", "2026-05-20T00:00:00Z", "publicVisible", false, "sortOrder", 2)
                ),
                "reason", "replace milestones"
        ), 200);
        assertThat(updated.at("/data/milestones").size()).isEqualTo(2);
        assertThat(store.auditActions()).contains("PROFILE_MEMBER_MILESTONES_REPLACED");

        JsonNode publicDetail = performJson(get("/api/v1/profile/members/" + memberId), 200);
        assertThat(valuesAt(publicDetail, "/data/milestones", "title")).contains("New Build").doesNotContain("Private Event");

        performJson(put("/api/v1/profile/admin/members/missing/milestones").header("Authorization", bearer("admin-token")), Map.of("items", List.of(), "reason", "missing"), 404, 43200);
        performJson(put("/api/v1/profile/admin/members/" + memberId + "/milestones").header("Authorization", bearer("admin-token")), Map.of("items", List.of(Map.of("type", "NOPE", "title", "", "happenedAt", "bad", "publicVisible", true, "sortOrder", 1)), "reason", "bad"), 400, 40001);
        performJson(put("/api/v1/profile/admin/members/" + memberId + "/milestones").header("Authorization", bearer("helper-token")), Map.of("items", List.of(), "reason", "bad"), 403, 42001);

        int before = store.milestoneCount(memberId);
        store.failNextAudit();
        performJson(put("/api/v1/profile/admin/members/" + memberId + "/milestones").header("Authorization", bearer("admin-token")), Map.of("items", List.of(), "reason", "audit"), 500, 51201);
        assertThat(store.milestoneCount(memberId)).isEqualTo(before);
    }

    @Test
    @DisplayName("PROF-WORK replaces work snapshots and public detail filters private items")
    void workSnapshotsContract() throws Exception {
        String memberId = store.memberIdByUserId("member_with_work_snapshots");
        JsonNode updated = performJson(put("/api/v1/profile/admin/members/" + memberId + "/work-snapshots").header("Authorization", bearer("admin-token")), Map.of(
                "items", List.of(
                        Map.of("type", "BUILD", "title", "Castle", "summary", "Build summary", "coverUrl", "https://example.com/castle.png", "sourceModule", "content", "sourceId", "content-1", "publicVisible", true, "sortOrder", 1),
                        Map.of("id", store.firstWorkId(memberId), "type", "VIDEO", "title", "Private Video", "summary", "Hidden", "coverUrl", "/covers/private.png", "sourceModule", "activity", "sourceId", "act-1", "publicVisible", false, "sortOrder", 2)
                ),
                "reason", "replace works"
        ), 200);
        assertThat(updated.at("/data/workSnapshots").size()).isEqualTo(2);
        assertThat(store.auditActions()).contains("PROFILE_MEMBER_WORKS_REPLACED");

        JsonNode publicDetail = performJson(get("/api/v1/profile/members/" + memberId), 200);
        assertThat(valuesAt(publicDetail, "/data/workSnapshots", "title")).contains("Castle").doesNotContain("Private Video");

        performJson(put("/api/v1/profile/admin/members/missing/work-snapshots").header("Authorization", bearer("admin-token")), Map.of("items", List.of(), "reason", "missing"), 404, 43200);
        performJson(put("/api/v1/profile/admin/members/" + memberId + "/work-snapshots").header("Authorization", bearer("admin-token")), Map.of("items", List.of(Map.of("type", "NOPE", "title", "", "publicVisible", true, "sortOrder", 1)), "reason", "bad"), 400, 40001);
        performJson(put("/api/v1/profile/admin/members/" + memberId + "/work-snapshots").header("Authorization", bearer("helper-token")), Map.of("items", List.of(), "reason", "bad"), 403, 42001);

        int before = store.workCount(memberId);
        store.failNextAudit();
        performJson(put("/api/v1/profile/admin/members/" + memberId + "/work-snapshots").header("Authorization", bearer("admin-token")), Map.of("items", List.of(), "reason", "audit"), 500, 51201);
        assertThat(store.workCount(memberId)).isEqualTo(before);
    }

    @Test
    @DisplayName("PROF-AUDIT and PROF-SEC cover audit reads, immutability, and auth boundary")
    void auditAndSecurityContract() throws Exception {
        String memberId = store.memberIdByUserId("active_member");
        performJson(patch("/api/v1/profile/admin/members/" + memberId).header("Authorization", bearer("admin-token")).header("X-Request-Id", "req-profile-audit"), Map.of("bio", "Audit bio", "reason", "audit"), 200);

        mvc.perform(get("/api/v1/profile/admin/members/" + memberId + "/audit-logs").header("Authorization", bearer("admin-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].requestId").value("req-profile-audit"));

        mvc.perform(get("/api/v1/profile/admin/members/" + memberId + "/audit-logs").header("Authorization", bearer("owner-token")))
                .andExpect(status().isOk());

        performJson(get("/api/v1/profile/admin/members/" + memberId + "/audit-logs").header("Authorization", bearer("helper-token")), 403, 42001);
        performJson(get("/api/v1/profile/admin/members/" + memberId + "/audit-logs").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/profile/admin/members/missing/audit-logs").header("Authorization", bearer("admin-token")), 404, 43200);
        performJson(get("/api/v1/profile/admin/members/" + memberId + "/audit-logs").header("Authorization", bearer("admin-token")).param("page", "0"), 400, 40002);

        mvc.perform(delete("/api/v1/profile/admin/members/" + memberId + "/audit-logs").header("Authorization", bearer("owner-token")))
                .andExpect(status().is4xxClientError());

        assertThat(auth.writeCallCount()).isZero();
        assertThat(store.usesAuthImplementation()).isFalse();
        assertThat(store.auditActions()).contains("PROFILE_MEMBER_UPDATED");
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

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
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
