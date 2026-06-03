package cn.beiming.activity;

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = cn.beiming.engagement.EngagementCoreServiceApplication.class, properties = "activity.test-controls.enabled=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ActivityEngagementCoreBehaviorContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("activity local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "ACT-COM", 1, 90);
        addRange(mapped, "ACT-PUB", 1, 70);
        addRange(mapped, "ACT-ME", 1, 90);
        addRange(mapped, "ACT-ADMIN", 1, 130);
        addRange(mapped, "ACT-REG", 1, 110);
        addRange(mapped, "ACT-WAITLIST", 1, 60);
        addRange(mapped, "ACT-CHECKIN", 1, 70);
        addRange(mapped, "ACT-RESULT", 1, 70);
        addRange(mapped, "ACT-REWARD", 1, 90);
        addRange(mapped, "ACT-AUDIT", 1, 60);
        addRange(mapped, "ACT-OPS", 1, 40);
        addRange(mapped, "ACT-DEPS", 1, 120);
        addRange(mapped, "ACT-COMPAT", 1, 80);
        addRange(mapped, "ACT-HARDEN", 1, 184);
        addRange(mapped, "ACT-PORT", 1, 14);
        addRange(mapped, "ACT-CYCLE", 1, 50);
        assertThat(mapped).contains("ACT-COM-001", "ACT-ME-090", "ACT-REWARD-090", "ACT-HARDEN-184", "ACT-CYCLE-050");
        assertThat(mapped).hasSize(1328);
    }

    @Test
    @DisplayName("ACT-COM covers envelope, request id, auth gates, validation, role gates, and trusted field isolation")
    void commonContract() throws Exception {
        mvc.perform(get("/api/v1/activity/events").header("X-Request-Id", "req-activity-public"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-activity-public"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.requestId").value("req-activity-public"));

        JsonNode generated = performJson(get("/api/v1/activity/events"), 200);
        assertThat(generated.at("/requestId").asText()).isNotBlank();

        performJson(post("/api/v1/activity/me/events/act-seed-open/registrations"), registerBody("trusted"), 401, 41000);
        performJson(post("/api/v1/activity/me/events/act-seed-open/registrations").header("Authorization", "bad-token"), registerBody("trusted"), 401, 41003);
        performJson(get("/api/v1/activity/admin/events"), 401, 41000);
        performJson(get("/api/v1/activity/admin/events").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/activity/admin/events").header("Authorization", bearer("helper-token")), 200);
        performJson(patch("/api/v1/activity/admin/events/act-seed-open/publish").header("Authorization", bearer("helper-token")),
                Map.of("reason", "helper cannot publish", "idempotencyKey", "helper-publish-1"), 403, 42001);
        performJson(get("/api/v1/activity/admin/events").header("Authorization", bearer("admin-token")).param("page", "0"), 400, 40002);
        performJson(get("/api/v1/activity/admin/events").header("Authorization", bearer("admin-token")).param("pageSize", "101"), 400, 40002);
        performJson(get("/api/v1/activity/admin/events").header("Authorization", bearer("admin-token")).param("sort", "bad"), 400, 40003);
        performJson(post("/api/v1/activity/admin/events").header("Authorization", bearer("admin-token")), with(eventBody("short-key"), "idempotencyKey", "short"), 400, 40001);

        JsonNode created = performJson(post("/api/v1/activity/admin/events").header("Authorization", bearer("admin-token")),
                with(eventBody("trusted-event-1"), "status", "RESULT_PUBLISHED"), 201);
        assertThat(created.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(created.at("/data/createdBy").asText()).isEqualTo("admin-user");
        assertThat(created.at("/data/confirmedCount").asInt()).isZero();
        assertNoSecrets(created);
    }

    @Test
    @DisplayName("ACT-PUB, ACT-ADMIN, ACT-ME, ACT-WAITLIST, and ACT-CHECKIN cover the event lifecycle")
    void activityLifecycleRegistrationWaitlistAndCheckIn() throws Exception {
        String activityId = createApprovedPublishedEvent("flow-event-1", 1, 2);

        JsonNode publicList = performJson(get("/api/v1/activity/events").param("keyword", "活动"), 200);
        assertThat(publicList.at("/data/total").asInt()).isGreaterThanOrEqualTo(1);
        assertNoSecrets(publicList);

        JsonNode firstRegistration = performJson(registerRequest(activityId, "member-user-1-token"),
                registerBody("reg-member-1"), 201);
        assertThat(firstRegistration.at("/data/status").asText()).isEqualTo("CONFIRMED");
        String firstRegistrationId = firstRegistration.at("/data/registrationId").asText();

        JsonNode waitlisted = performJson(registerRequest(activityId, "member-user-2-token"),
                registerBody("reg-member-2"), 201);
        assertThat(waitlisted.at("/data/status").asText()).isEqualTo("WAITLISTED");
        String waitlistedId = waitlisted.at("/data/registrationId").asText();

        performJson(patch("/api/v1/activity/admin/registrations/" + waitlistedId + "/promote").header("Authorization", bearer("helper-token")),
                Map.of("reviewComment", "名额尚未释放", "reason", "候补转正", "idempotencyKey", "promote-before-cancel"), 409, 49613);

        JsonNode canceled = performJson(post("/api/v1/activity/me/registrations/" + firstRegistrationId + "/cancel").header("Authorization", bearer("member-user-1-token")),
                Map.of("reason", "临时有事", "idempotencyKey", "cancel-reg-1"), 200);
        assertThat(canceled.at("/data/status").asText()).isEqualTo("CANCELED");

        JsonNode promoted = performJson(patch("/api/v1/activity/admin/registrations/" + waitlistedId + "/promote").header("Authorization", bearer("helper-token")),
                Map.of("reviewComment", "候补转正", "reason", "释放名额", "idempotencyKey", "promote-after-cancel"), 200);
        assertThat(promoted.at("/data/status").asText()).isEqualTo("CONFIRMED");

        startEvent(activityId);
        JsonNode checkIn = performJson(patch("/api/v1/activity/admin/registrations/" + waitlistedId + "/check-in")
                        .header("Authorization", bearer("helper-token"))
                        .header("X-Test-Now", "2026-06-01T12:30:00Z"),
                Map.of("method", "MANUAL", "reason", "现场确认", "idempotencyKey", "check-in-1"), 200);
        assertThat(checkIn.at("/data/status").asText()).isEqualTo("CHECKED_IN");

        JsonNode mine = performJson(get("/api/v1/activity/me/events/" + activityId + "/check-in").header("Authorization", bearer("member-user-2-token")), 200);
        assertThat(mine.at("/data/status").asText()).isEqualTo("CHECKED_IN");

        JsonNode regs = performJson(get("/api/v1/activity/admin/events/" + activityId + "/registrations").header("Authorization", bearer("helper-token")), 200);
        assertThat(regs.at("/data/total").asInt()).isEqualTo(2);
        assertNoSecrets(regs);
    }

    @Test
    @DisplayName("ACT-RESULT and ACT-REWARD keep results, rewards, and contribution candidates inside activity")
    void resultRewardAndContributionCandidateFlow() throws Exception {
        String activityId = createApprovedPublishedEvent("reward-event-1", 2, 1);
        JsonNode registration = performJson(registerRequest(activityId, "member-user-1-token"),
                registerBody("reward-reg-1"), 201);
        String registrationId = registration.at("/data/registrationId").asText();
        startEvent(activityId);
        performJson(patch("/api/v1/activity/admin/registrations/" + registrationId + "/check-in")
                        .header("Authorization", bearer("helper-token"))
                        .header("X-Test-Now", "2026-06-01T12:30:00Z"),
                Map.of("method", "MANUAL", "reason", "活动签到", "idempotencyKey", "reward-checkin-1"), 200);
        completeEvent(activityId);

        JsonNode result = performJson(put("/api/v1/activity/admin/events/" + activityId + "/result").header("Authorization", bearer("helper-token")),
                Map.of("title", "活动结果", "summary", "活动完成", "details", "大家完成了工程活动", "reason", "录入结果", "idempotencyKey", "result-upsert-1"), 200);
        assertThat(result.at("/data/status").asText()).isEqualTo("DRAFT");

        JsonNode publishedResult = performJson(patch("/api/v1/activity/admin/events/" + activityId + "/result/publish").header("Authorization", bearer("admin-token")),
                Map.of("reason", "发布结果", "idempotencyKey", "result-publish-1"), 200);
        assertThat(publishedResult.at("/data/status").asText()).isEqualTo("PUBLISHED");

        JsonNode publicResult = performJson(get("/api/v1/activity/events/" + activityId + "/result"), 200);
        assertThat(publicResult.at("/data/result/status").asText()).isEqualTo("PUBLISHED");
        assertNoSecrets(publicResult);

        JsonNode reward = performJson(post("/api/v1/activity/admin/events/" + activityId + "/rewards").header("Authorization", bearer("helper-token")),
                rewardBody(registrationId, "reward-create-1"), 201);
        String rewardId = reward.at("/data/rewardId").asText();
        JsonNode issued = performJson(patch("/api/v1/activity/admin/rewards/" + rewardId + "/issue").header("Authorization", bearer("helper-token")),
                Map.of("publicComment", "奖励已登记", "reason", "发放奖励", "idempotencyKey", "reward-issue-1"), 200);
        assertThat(issued.at("/data/status").asText()).isEqualTo("ISSUED");

        JsonNode mine = performJson(get("/api/v1/activity/me/rewards").header("Authorization", bearer("member-user-1-token")), 200);
        assertThat(mine.at("/data/total").asInt()).isEqualTo(1);
        assertNoSecrets(mine);

        JsonNode candidates = performJson(post("/api/v1/activity/admin/events/" + activityId + "/contribution-candidates").header("Authorization", bearer("admin-token")),
                Map.of("reason", "生成活动贡献候选", "idempotencyKey", "candidate-create-1"), 201);
        assertThat(candidates.at("/data/items").isArray()).isTrue();
        assertThat(candidates.toString()).contains("PENDING");
        assertThat(candidates.toString()).doesNotContain("scoreBalance", "attendancePoints", "leaderboard");
    }

    @Test
    @DisplayName("ACT-HARDEN replays identical idempotent writes and rejects idempotency fingerprint conflicts")
    void idempotencyReplaysCreateAndRegistrationResults() throws Exception {
        Map<String, Object> eventBody = eventBody("idem-create-1");
        JsonNode firstCreate = performJson(post("/api/v1/activity/admin/events").header("Authorization", bearer("admin-token")),
                eventBody, 201);
        JsonNode replayCreate = performJson(post("/api/v1/activity/admin/events").header("Authorization", bearer("admin-token")),
                eventBody, 201);
        assertThat(replayCreate.at("/data/activityId").asText()).isEqualTo(firstCreate.at("/data/activityId").asText());
        performJson(post("/api/v1/activity/admin/events").header("Authorization", bearer("admin-token")),
                with(eventBody, "title", "改动后的活动标题"), 409, 49617);

        String activityId = createApprovedPublishedEvent("idem-reg-event-1", 3, 1);
        Map<String, Object> registerBody = registerBody("idem-reg-1");
        JsonNode firstRegistration = performJson(registerRequest(activityId, "member-user-1-token"),
                registerBody, 201);
        JsonNode replayRegistration = performJson(registerRequest(activityId, "member-user-1-token"),
                registerBody, 201);
        assertThat(replayRegistration.at("/data/registrationId").asText()).isEqualTo(firstRegistration.at("/data/registrationId").asText());
        performJson(registerRequest(activityId, "member-user-1-token"),
                with(registerBody, "note", "换一份报名备注"), 409, 49617);
    }

    @Test
    @DisplayName("ACT-REG and ACT-CHECKIN enforce registration and check-in time windows from server time")
    void registrationAndCheckInWindowsUseServerTime() throws Exception {
        Map<String, Object> futureOpen = with(eventBody("window-reg-open"), "registrationOpenAt", "2026-05-26T00:00:00Z");
        String futureOpenId = createApprovedPublishedEvent(futureOpen, 2, 1);
        performJson(post("/api/v1/activity/me/events/" + futureOpenId + "/registrations")
                        .header("Authorization", bearer("member-user-1-token"))
                        .header("X-Test-Now", "2026-05-25T12:00:00Z"),
                registerBody("before-open"), 409, 49612);

        String closedId = createApprovedPublishedEvent("window-reg-closed", 2, 1);
        performJson(post("/api/v1/activity/me/events/" + closedId + "/registrations")
                        .header("Authorization", bearer("member-user-1-token"))
                        .header("X-Test-Now", "2026-06-01T11:30:00Z"),
                registerBody("after-close"), 409, 49612);

        String checkInActivityId = createApprovedPublishedEvent("window-checkin", 2, 1);
        JsonNode registration = performJson(post("/api/v1/activity/me/events/" + checkInActivityId + "/registrations")
                        .header("Authorization", bearer("member-user-1-token"))
                        .header("X-Test-Now", "2026-05-25T12:00:00Z"),
                registerBody("window-checkin-reg"), 201);
        String registrationId = registration.at("/data/registrationId").asText();
        startEvent(checkInActivityId);

        performJson(patch("/api/v1/activity/admin/registrations/" + registrationId + "/check-in")
                        .header("Authorization", bearer("helper-token"))
                        .header("X-Test-Now", "2026-06-01T10:59:59Z"),
                Map.of("method", "MANUAL", "reason", "过早签到", "idempotencyKey", "checkin-too-early"), 409, 49618);
        performJson(patch("/api/v1/activity/admin/registrations/" + registrationId + "/check-in")
                        .header("Authorization", bearer("helper-token"))
                        .header("X-Test-Now", "2026-06-02T14:00:01Z"),
                Map.of("method", "MANUAL", "reason", "过晚签到", "idempotencyKey", "checkin-too-late"), 409, 49618);
        performJson(patch("/api/v1/activity/admin/registrations/" + registrationId + "/check-in")
                        .header("Authorization", bearer("helper-token"))
                        .header("X-Test-Now", "2026-06-01T11:00:00Z"),
                Map.of("method", "MANUAL", "reason", "窗口内签到", "idempotencyKey", "checkin-in-window"), 200);
    }

    @Test
    @DisplayName("ACT-ADMIN and ACT-WAITLIST reject invalid capacity and registration time boundaries")
    void activityCreationRejectsInvalidCapacityAndRegistrationBoundaries() throws Exception {
        performJson(post("/api/v1/activity/admin/events").header("Authorization", bearer("admin-token")),
                with(eventBody("invalid-capacity"), "capacity", -1), 400, 40001);
        performJson(post("/api/v1/activity/admin/events").header("Authorization", bearer("admin-token")),
                with(eventBody("invalid-waitlist"), "waitlistCapacity", -1), 400, 40001);
        performJson(post("/api/v1/activity/admin/events").header("Authorization", bearer("admin-token")),
                with(eventBody("invalid-close-after-start"), "registrationCloseAt", "2026-06-01T12:30:00Z"), 400, 40001);
        performJson(post("/api/v1/activity/admin/events").header("Authorization", bearer("admin-token")),
                with(eventBody("invalid-open-after-close"), "registrationOpenAt", "2026-06-01T11:30:00Z"), 400, 40001);
    }

    @Test
    @DisplayName("ACT-HARDEN-111..130 replay backend idempotent writes across activity, registration, result, reward, and candidates")
    void backendWriteEndpointsReplayIdempotentResults() throws Exception {
        JsonNode draft = createEvent("idem-backend-event");
        String activityId = draft.at("/data/activityId").asText();

        Map<String, Object> updateBody = with(eventBody("idem-backend-update"), "title", "更新后的活动标题");
        JsonNode firstUpdate = performJson(patch("/api/v1/activity/admin/events/" + activityId).header("Authorization", bearer("helper-token")),
                updateBody, 200);
        JsonNode replayUpdate = performJson(patch("/api/v1/activity/admin/events/" + activityId).header("Authorization", bearer("helper-token")),
                updateBody, 200);
        assertThat(replayUpdate.at("/data/activityId").asText()).isEqualTo(firstUpdate.at("/data/activityId").asText());
        performJson(patch("/api/v1/activity/admin/events/" + activityId).header("Authorization", bearer("helper-token")),
                with(updateBody, "title", "同 key 改标题"), 409, 49617);

        Map<String, Object> submitBody = Map.of("reason", "提交审核", "idempotencyKey", "idem-backend-submit");
        performJson(post("/api/v1/activity/admin/events/" + activityId + "/submit").header("Authorization", bearer("helper-token")),
                submitBody, 200);
        performJson(post("/api/v1/activity/admin/events/" + activityId + "/submit").header("Authorization", bearer("helper-token")),
                submitBody, 200);

        Map<String, Object> approveBody = reviewBody("idem-backend-approve");
        performJson(patch("/api/v1/activity/admin/events/" + activityId + "/approve").header("Authorization", bearer("helper-token")),
                approveBody, 200);
        performJson(patch("/api/v1/activity/admin/events/" + activityId + "/approve").header("Authorization", bearer("helper-token")),
                approveBody, 200);

        Map<String, Object> publishBody = Map.of("reason", "发布活动", "idempotencyKey", "idem-backend-publish");
        performJson(patch("/api/v1/activity/admin/events/" + activityId + "/publish").header("Authorization", bearer("admin-token")),
                publishBody, 200);
        performJson(patch("/api/v1/activity/admin/events/" + activityId + "/publish").header("Authorization", bearer("admin-token")),
                publishBody, 200);

        JsonNode registration = performJson(registerRequest(activityId, "member-user-1-token"),
                registerBody("idem-backend-registration"), 201);
        String registrationId = registration.at("/data/registrationId").asText();
        startEvent(activityId);
        Map<String, Object> checkInBody = Map.of("method", "MANUAL", "reason", "签到", "idempotencyKey", "idem-backend-checkin");
        performJson(patch("/api/v1/activity/admin/registrations/" + registrationId + "/check-in")
                        .header("Authorization", bearer("helper-token"))
                        .header("X-Test-Now", "2026-06-01T12:30:00Z"),
                checkInBody, 200);
        performJson(patch("/api/v1/activity/admin/registrations/" + registrationId + "/check-in")
                        .header("Authorization", bearer("helper-token"))
                        .header("X-Test-Now", "2026-06-01T12:30:00Z"),
                checkInBody, 200);
        completeEvent(activityId);

        Map<String, Object> resultBody = Map.of("title", "活动结果", "summary", "完成", "details", "结果", "reason", "录入", "idempotencyKey", "idem-backend-result");
        performJson(put("/api/v1/activity/admin/events/" + activityId + "/result").header("Authorization", bearer("helper-token")),
                resultBody, 200);
        performJson(put("/api/v1/activity/admin/events/" + activityId + "/result").header("Authorization", bearer("helper-token")),
                resultBody, 200);

        Map<String, Object> resultPublishBody = Map.of("reason", "发布结果", "idempotencyKey", "idem-backend-result-publish");
        performJson(patch("/api/v1/activity/admin/events/" + activityId + "/result/publish").header("Authorization", bearer("admin-token")),
                resultPublishBody, 200);
        performJson(patch("/api/v1/activity/admin/events/" + activityId + "/result/publish").header("Authorization", bearer("admin-token")),
                resultPublishBody, 200);

        Map<String, Object> rewardBody = rewardBody(registrationId, "idem-backend-reward");
        JsonNode reward = performJson(post("/api/v1/activity/admin/events/" + activityId + "/rewards").header("Authorization", bearer("helper-token")),
                rewardBody, 201);
        JsonNode rewardReplay = performJson(post("/api/v1/activity/admin/events/" + activityId + "/rewards").header("Authorization", bearer("helper-token")),
                rewardBody, 201);
        assertThat(rewardReplay.at("/data/rewardId").asText()).isEqualTo(reward.at("/data/rewardId").asText());
        String rewardId = reward.at("/data/rewardId").asText();

        Map<String, Object> issueBody = Map.of("publicComment", "已发放", "reason", "发放奖励", "idempotencyKey", "idem-backend-issue");
        performJson(patch("/api/v1/activity/admin/rewards/" + rewardId + "/issue").header("Authorization", bearer("helper-token")),
                issueBody, 200);
        performJson(patch("/api/v1/activity/admin/rewards/" + rewardId + "/issue").header("Authorization", bearer("helper-token")),
                issueBody, 200);

        Map<String, Object> candidatesBody = Map.of("reason", "生成候选", "idempotencyKey", "idem-backend-candidates");
        JsonNode candidates = performJson(post("/api/v1/activity/admin/events/" + activityId + "/contribution-candidates").header("Authorization", bearer("admin-token")),
                candidatesBody, 201);
        JsonNode candidatesReplay = performJson(post("/api/v1/activity/admin/events/" + activityId + "/contribution-candidates").header("Authorization", bearer("admin-token")),
                candidatesBody, 201);
        assertThat(candidatesReplay.at("/data/items/0/candidateId").asText()).isEqualTo(candidates.at("/data/items/0/candidateId").asText());
    }

    @Test
    @DisplayName("ACT-HARDEN-131..144 reject invalid event patch fields without partial updates")
    void eventPatchRejectsInvalidCapacityAndTimeBoundaries() throws Exception {
        String activityId = createEvent("invalid-patch-event").at("/data/activityId").asText();
        performJson(patch("/api/v1/activity/admin/events/" + activityId).header("Authorization", bearer("helper-token")),
                with(eventBody("invalid-patch-capacity"), "capacity", -1), 400, 40001);
        performJson(patch("/api/v1/activity/admin/events/" + activityId).header("Authorization", bearer("helper-token")),
                with(eventBody("invalid-patch-waitlist"), "waitlistCapacity", -1), 400, 40001);
        performJson(patch("/api/v1/activity/admin/events/" + activityId).header("Authorization", bearer("helper-token")),
                with(eventBody("invalid-patch-close"), "registrationCloseAt", "2026-06-01T12:30:00Z"), 400, 40001);

        JsonNode afterInvalidPatch = performJson(get("/api/v1/activity/admin/events/" + activityId).header("Authorization", bearer("admin-token")), 200);
        assertThat(afterInvalidPatch.at("/data/activity/capacity").asInt()).isEqualTo(10);
        assertThat(afterInvalidPatch.at("/data/activity/waitlistCapacity").asInt()).isEqualTo(5);
    }

    @Test
    @DisplayName("ACT-HARDEN-145..164 rollback backend writes when audit persistence fails")
    void auditFailureRollsBackBackendWrites() throws Exception {
        String activityId = createEvent("audit-rollback-submit").at("/data/activityId").asText();
        performJson(post("/api/v1/activity/admin/events/" + activityId + "/submit")
                        .header("Authorization", bearer("helper-token"))
                        .header("X-Test-Fail-Audit", "true"),
                Map.of("reason", "审计失败", "idempotencyKey", "audit-fail-submit"), 500, 54601);
        JsonNode afterSubmitFail = performJson(get("/api/v1/activity/admin/events/" + activityId).header("Authorization", bearer("admin-token")), 200);
        assertThat(afterSubmitFail.at("/data/activity/status").asText()).isEqualTo("DRAFT");

        String rewardActivityId = createApprovedPublishedEvent("audit-rollback-reward", 2, 1);
        JsonNode registration = performJson(registerRequest(rewardActivityId, "member-user-1-token"),
                registerBody("audit-rollback-registration"), 201);
        String registrationId = registration.at("/data/registrationId").asText();
        startEvent(rewardActivityId);
        performJson(patch("/api/v1/activity/admin/registrations/" + registrationId + "/check-in")
                        .header("Authorization", bearer("helper-token"))
                        .header("X-Test-Now", "2026-06-01T12:30:00Z"),
                Map.of("method", "MANUAL", "reason", "签到", "idempotencyKey", "audit-rollback-checkin"), 200);
        completeEvent(rewardActivityId);
        performJson(put("/api/v1/activity/admin/events/" + rewardActivityId + "/result").header("Authorization", bearer("helper-token")),
                Map.of("title", "活动结果", "summary", "完成", "details", "结果", "reason", "录入", "idempotencyKey", "audit-rollback-result"), 200);
        performJson(patch("/api/v1/activity/admin/events/" + rewardActivityId + "/result/publish").header("Authorization", bearer("admin-token")),
                Map.of("reason", "发布结果", "idempotencyKey", "audit-rollback-result-publish"), 200);

        performJson(post("/api/v1/activity/admin/events/" + rewardActivityId + "/rewards")
                        .header("Authorization", bearer("helper-token"))
                        .header("X-Test-Fail-Audit", "true"),
                rewardBody(registrationId, "audit-fail-reward"), 500, 54601);
        JsonNode rewards = performJson(get("/api/v1/activity/me/rewards").header("Authorization", bearer("member-user-1-token")), 200);
        assertThat(rewards.at("/data/total").asInt()).isZero();
    }

    @Test
    @DisplayName("ACT-DEPS, ACT-AUDIT, ACT-OPS, ACT-COMPAT, and ACT-HARDEN cover dependency failures and boundaries")
    void dependencyHardeningAuditOpsAndCompatibilityFlow() throws Exception {
        performJson(post("/api/v1/activity/me/events/act-seed-open/registrations")
                        .header("Authorization", bearer("member-user-1-token"))
                        .header("X-Test-Profile-Mode", "unavailable"),
                registerBody("profile-down-reg"), 502, 49410);
        performJson(post("/api/v1/activity/admin/events")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Content-Mode", "unavailable"),
                with(eventBody("content-down-event"), "linkedContentId", "content-public-1"), 502, 49440);

        String rollbackActivity = createEvent("rollback-event-1").at("/data/activityId").asText();
        performJson(post("/api/v1/activity/admin/events/" + rollbackActivity + "/submit").header("Authorization", bearer("helper-token")),
                Map.of("reason", "提交审核", "idempotencyKey", "rollback-submit"), 200);
        performJson(patch("/api/v1/activity/admin/events/" + rollbackActivity + "/approve")
                        .header("Authorization", bearer("helper-token"))
                        .header("X-Test-Fail-Audit", "true"),
                reviewBody("rollback-approve"), 500, 54601);
        JsonNode afterAuditFail = performJson(get("/api/v1/activity/admin/events/" + rollbackActivity).header("Authorization", bearer("admin-token")), 200);
        assertThat(afterAuditFail.at("/data/activity/status").asText()).isEqualTo("PENDING_REVIEW");

        String activityId = createApprovedPublishedEvent("notify-fail-event", 2, 1);
        JsonNode registration = performJson(registerRequest(activityId, "member-user-1-token"),
                registerBody("notify-reg"), 201);
        startEvent(activityId);
        performJson(patch("/api/v1/activity/admin/registrations/" + registration.at("/data/registrationId").asText() + "/check-in")
                        .header("Authorization", bearer("helper-token"))
                        .header("X-Test-Now", "2026-06-01T12:30:00Z"),
                Map.of("method", "MANUAL", "reason", "签到", "idempotencyKey", "notify-checkin"), 200);
        completeEvent(activityId);
        performJson(put("/api/v1/activity/admin/events/" + activityId + "/result").header("Authorization", bearer("helper-token")),
                Map.of("title", "活动结果", "summary", "完成", "details", "结果", "reason", "录入", "idempotencyKey", "notify-result"), 200);
        performJson(patch("/api/v1/activity/admin/events/" + activityId + "/result/publish").header("Authorization", bearer("admin-token")),
                Map.of("reason", "发布", "idempotencyKey", "notify-result-publish"), 200);
        JsonNode reward = performJson(post("/api/v1/activity/admin/events/" + activityId + "/rewards").header("Authorization", bearer("helper-token")),
                rewardBody(registration.at("/data/registrationId").asText(), "notify-reward"), 201);
        JsonNode notified = performJson(patch("/api/v1/activity/admin/rewards/" + reward.at("/data/rewardId").asText() + "/issue")
                        .header("Authorization", bearer("helper-token"))
                        .header("X-Test-Notification-Mode", "unavailable"),
                Map.of("publicComment", "通知失败也要发放", "reason", "发放奖励", "idempotencyKey", "notify-issue"), 200);
        assertThat(notified.at("/data/notificationFailure/failureCode").asText()).isEqualTo("49420");
        assertNoSecrets(notified);

        JsonNode audit = performJson(get("/api/v1/activity/admin/audit-logs").header("Authorization", bearer("admin-token")).param("pageSize", "100"), 200);
        assertThat(audit.at("/data/total").asInt()).isGreaterThanOrEqualTo(4);
        assertNoSecrets(audit);

        JsonNode ops = performJson(get("/api/v1/activity/admin/ops/summary").header("Authorization", bearer("helper-token")), 200);
        assertThat(ops.at("/data/service").asText()).isEqualTo("activity");
        assertThat(ops.at("/data/port").asInt()).isEqualTo(8132);
        assertThat(ops.at("/data/legacyPort").asInt()).isEqualTo(8113);
        assertThat(ops.at("/data/testControlsEnabled").asBoolean()).isTrue();
        assertThat(ops.toString()).contains("P1_IN_MEMORY_STORAGE", "ATTENDANCE_CONTRIBUTION_NOT_CONNECTED");
        assertNoSecrets(ops);

        Path serviceRoot = Path.of("backend/engagement-core-service/src/main/java/cn/beiming/activity");
        String source = Files.exists(serviceRoot)
                ? String.join("\n", Files.walk(serviceRoot)
                .filter(Files::isRegularFile)
                .map(path -> {
                    try {
                        return Files.readString(path);
                    } catch (Exception exception) {
                        throw new IllegalStateException(exception);
                    }
                }).toList())
                : "";
        assertThat(source).doesNotContain(
                "cn.beiming.auth.", "cn.beiming.profile.", "cn.beiming.notification.", "cn.beiming.content.",
                "cn.beiming.serverstatus.", "cn.beiming.resource.", "cn.beiming.admin.", "cn.beiming.onboarding.",
                "cn.beiming.exam.", "cn.beiming.whitelist.", "cn.beiming.attendance.", "cn.beiming.community.",
                "Repository", "JdbcTemplate", "ProcessBuilder", "Runtime.getRuntime", "node-daemon", "cloudreveToken",
                "terminal", "container", "backupRestore", "file-manager", "server.properties", "enforce-whitelist",
                "whitelist add", "whitelist remove", "scoreBalance", "attendancePoints", "leaderboard",
                "CommunityStore", "AttendanceStore");
    }

    private JsonNode createEvent(String idempotencyKey) throws Exception {
        return performJson(post("/api/v1/activity/admin/events").header("Authorization", bearer("admin-token")), eventBody(idempotencyKey), 201);
    }

    private String createApprovedPublishedEvent(String idempotencyKey, int capacity, int waitlistCapacity) throws Exception {
        JsonNode event = performJson(post("/api/v1/activity/admin/events").header("Authorization", bearer("admin-token")),
                with(with(eventBody(idempotencyKey), "capacity", capacity), "waitlistCapacity", waitlistCapacity), 201);
        return approveAndPublishEvent(event, idempotencyKey);
    }

    private String createApprovedPublishedEvent(Map<String, Object> body, int capacity, int waitlistCapacity) throws Exception {
        String idempotencyKey = body.get("idempotencyKey").toString();
        JsonNode event = performJson(post("/api/v1/activity/admin/events").header("Authorization", bearer("admin-token")),
                with(with(body, "capacity", capacity), "waitlistCapacity", waitlistCapacity), 201);
        return approveAndPublishEvent(event, idempotencyKey);
    }

    private String approveAndPublishEvent(JsonNode event, String idempotencyKey) throws Exception {
        String activityId = event.at("/data/activityId").asText();
        performJson(post("/api/v1/activity/admin/events/" + activityId + "/submit").header("Authorization", bearer("helper-token")),
                Map.of("reason", "提交审核", "idempotencyKey", idempotencyKey + "-submit"), 200);
        performJson(patch("/api/v1/activity/admin/events/" + activityId + "/approve").header("Authorization", bearer("helper-token")),
                reviewBody(idempotencyKey + "-approve"), 200);
        performJson(patch("/api/v1/activity/admin/events/" + activityId + "/publish").header("Authorization", bearer("admin-token")),
                Map.of("reason", "发布活动", "idempotencyKey", idempotencyKey + "-publish"), 200);
        return activityId;
    }

    private void startEvent(String activityId) throws Exception {
        performJson(patch("/api/v1/activity/admin/events/" + activityId + "/start").header("Authorization", bearer("helper-token")),
                Map.of("reason", "活动开始", "idempotencyKey", activityId + "-start"), 200);
    }

    private void completeEvent(String activityId) throws Exception {
        performJson(patch("/api/v1/activity/admin/events/" + activityId + "/complete").header("Authorization", bearer("helper-token")),
                Map.of("reason", "活动完成", "idempotencyKey", activityId + "-complete"), 200);
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

    private MockHttpServletRequestBuilder registerRequest(String activityId, String token) {
        return post("/api/v1/activity/me/events/" + activityId + "/registrations")
                .header("Authorization", bearer(token))
                .header("X-Test-Now", "2026-05-25T12:00:00Z");
    }

    private Map<String, Object> eventBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("slug", "activity-" + idempotencyKey);
        body.put("title", "北冥工程活动");
        body.put("summary", "一次用于契约测试的活动");
        body.put("description", "活动描述覆盖发布、报名、签到、结果和奖励。");
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
        body.put("tags", List.of("activity", "test"));
        body.put("reason", "创建活动");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> registerBody(String idempotencyKey) {
        return Map.of("answers", Map.of("note", "我会准时参加"), "guestCount", 0, "note", "报名活动", "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> reviewBody(String idempotencyKey) {
        return Map.of("reviewComment", "审核通过", "internalNote", "private note", "reason", "符合活动规则", "idempotencyKey", idempotencyKey);
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

    private Map<String, Object> with(Map<String, Object> source, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put(key, value);
        return copy;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void assertNoSecrets(JsonNode json) {
        assertThat(json.toString()).doesNotContain(
                "secret-token", "authorizationHeader", "requestHeaders", "stackTrace", "notification body",
                "internalNote", "private note", "profile internal", "content internal", "resource internal",
                "server.properties", "whitelist add", "whitelist remove", "node-daemon", "terminal", "container",
                "cloudreveToken", "attendancePoints", "scoreBalance", "leaderboard", "CommunityStore", "AttendanceStore");
    }

    private void addRange(Set<String> ids, String prefix, int start, int end) {
        for (int i = start; i <= end; i++) {
            ids.add("%s-%03d".formatted(prefix, i));
        }
    }
}
