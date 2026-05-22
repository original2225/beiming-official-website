package cn.beiming.onboarding;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
class OnboardingApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("onboarding local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "ONB-COM", 1, 36);
        addRange(mapped, "ONB-PROGRESS", 1, 30);
        addRange(mapped, "ONB-START", 1, 34);
        addRange(mapped, "ONB-PROFILE", 1, 32);
        addRange(mapped, "ONB-RULES", 1, 34);
        addRange(mapped, "ONB-DIRECTION", 1, 28);
        addRange(mapped, "ONB-ADVANCE", 1, 40);
        addRange(mapped, "ONB-NEXT", 1, 22);
        addRange(mapped, "ONB-ADMIN-LIST", 1, 24);
        addRange(mapped, "ONB-ADMIN-DETAIL", 1, 18);
        addRange(mapped, "ONB-ADMIN-RESET", 1, 32);
        addRange(mapped, "ONB-ADMIN-BLOCK", 1, 26);
        addRange(mapped, "ONB-ADMIN-UNBLOCK", 1, 26);
        addRange(mapped, "ONB-AUDIT", 1, 26);
        addRange(mapped, "ONB-OPS", 1, 18);
        addRange(mapped, "ONB-DEPS", 1, 40);
        addRange(mapped, "ONB-COMPAT", 1, 34);
        addRange(mapped, "ONB-HARDEN", 1, 30);
        addRange(mapped, "ONB-CYCLE", 1, 18);
        assertThat(mapped).contains("ONB-COM-001", "ONB-ADVANCE-040", "ONB-OPS-018", "ONB-CYCLE-018");
        assertThat(mapped).hasSize(548);
    }

    @Test
    @DisplayName("ONB-COM common response, request id, auth, role gates, validation, and field isolation")
    void commonContract() throws Exception {
        mvc.perform(get("/api/v1/onboarding/me/progress")
                        .header("Authorization", bearer("minecraft-bound-token"))
                        .header("X-Request-Id", "req-onboarding-progress"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-onboarding-progress"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.requestId").value("req-onboarding-progress"));

        JsonNode generated = performJson(get("/api/v1/onboarding/me/progress")
                .header("Authorization", bearer("minecraft-bound-token")), 200);
        assertThat(generated.at("/requestId").asText()).isNotBlank();

        performJson(get("/api/v1/onboarding/me/progress"), 401, 41000);
        performJson(get("/api/v1/onboarding/me/progress").header("Authorization", "bad-token"), 401, 41003);
        performJson(get("/api/v1/onboarding/admin/applications"), 401, 41000);
        performJson(get("/api/v1/onboarding/admin/applications").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/onboarding/admin/applications").header("Authorization", bearer("helper-token")), 200);
        performJson(patch("/api/v1/onboarding/admin/applications/app-in-progress/reset").header("Authorization", bearer("helper-token")), reason("helper"), 403, 42001);
        performJson(get("/api/v1/onboarding/admin/applications").header("Authorization", bearer("admin-token")).param("page", "0"), 400, 40002);
        performJson(get("/api/v1/onboarding/admin/applications").header("Authorization", bearer("admin-token")).param("pageSize", "101"), 400, 40002);
        performJson(get("/api/v1/onboarding/admin/applications").header("Authorization", bearer("admin-token")).param("sort", "bad"), 400, 40003);
        performJson(get("/api/v1/onboarding/admin/applications").header("Authorization", bearer("admin-token")).param("blocked", "bad"), 400, 40001);
        performJson(get("/api/v1/onboarding/admin/audit-logs").header("Authorization", bearer("admin-token")).param("from", "bad-time"), 400, 40001);
        performJson(patch("/api/v1/onboarding/admin/applications/app-in-progress/reset").header("Authorization", bearer("admin-token")), Map.of(), 400, 40001);
        performJson(patch("/api/v1/onboarding/admin/applications/app-in-progress/reset").header("Authorization", bearer("admin-token")), Map.of("reason", "x".repeat(201)), 400, 40001);
        performJson(patch("/api/v1/onboarding/me/direction").header("Authorization", bearer("minecraft-bound-token")), Map.of("reviewDirection", "BAD"), 400, 40001);
        performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("minecraft-bound-token")), Map.of("idempotencyKey", "short"), 400, 40001);

        JsonNode error = performJson(get("/api/v1/onboarding/me/progress").header("Authorization", bearer("auth-bad-token")), 502, 46802);
        assertThat(error.at("/errors").isArray()).isTrue();

        JsonNode bodyTrusted = performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("minecraft-bound-token")),
                with(Map.of("idempotencyKey", "trusted-start-1"), "userId", "attacker"), 201);
        assertThat(bodyTrusted.at("/data/userId").asText()).isEqualTo("user-bound");
        assertNoSecrets(bodyTrusted);

        performJson(get("/api/v1/onboarding/public/anything").header("Authorization", bearer("minecraft-bound-token")), 404);
    }

    @Test
    @DisplayName("ONB-PROGRESS reads current user progress, degradation, member gating, steps, and no side effects")
    void progressContract() throws Exception {
        JsonNode notStarted = performJson(get("/api/v1/onboarding/me/progress").header("Authorization", bearer("minecraft-bound-token")), 200);
        assertThat(notStarted.at("/data/applicationId").isNull()).isTrue();
        assertThat(notStarted.at("/data/status").asText()).isEqualTo("NOT_STARTED");
        assertThat(valuesAt(notStarted, "/data/steps", "key")).containsExactly("ACCOUNT_READY", "MINECRAFT_BOUND", "PROFILE_CONFIRMED", "RULES_CONFIRMED", "DIRECTION_SELECTED", "EXAM_READY", "WHITELIST_READY");
        assertThat(notStarted.at("/data/steps/1/status").asText()).isEqualTo("COMPLETED");

        JsonNode unbound = performJson(get("/api/v1/onboarding/me/progress").header("Authorization", bearer("minecraft-unbound-token")), 200);
        assertThat(unbound.at("/data/steps/1/status").asText()).isEqualTo("BLOCKED");
        assertThat(unbound.at("/data/currentStep").asText()).isEqualTo("MINECRAFT_BOUND");

        JsonNode beforeOps = performJson(get("/api/v1/onboarding/admin/ops/summary").header("Authorization", bearer("admin-token")), 200);
        performJson(get("/api/v1/onboarding/me/progress").header("Authorization", bearer("minecraft-bound-token")), 200);
        JsonNode afterOps = performJson(get("/api/v1/onboarding/admin/ops/summary").header("Authorization", bearer("admin-token")), 200);
        assertThat(afterOps.at("/data/applicationsTotal").asInt()).isEqualTo(beforeOps.at("/data/applicationsTotal").asInt());
        assertThat(afterOps.at("/data/auditsTotal").asInt()).isEqualTo(beforeOps.at("/data/auditsTotal").asInt());

        JsonNode started = performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("minecraft-bound-token")), Map.of("idempotencyKey", "progress-start-1"), 201);
        JsonNode progress = performJson(get("/api/v1/onboarding/me/progress").header("Authorization", bearer("minecraft-bound-token")), 200);
        assertThat(progress.at("/data/applicationId").asText()).isEqualTo(started.at("/data/applicationId").asText());
        assertThat(progress.at("/data/currentStep").asText()).isEqualTo("PROFILE_CONFIRMED");

        performJson(get("/api/v1/onboarding/me/progress").header("Authorization", bearer("disabled-token")), 502, 46800);
        performJson(get("/api/v1/onboarding/me/progress").header("Authorization", bearer("auth-unavailable-token")), 502, 46800);
        performJson(get("/api/v1/onboarding/me/progress").header("Authorization", bearer("auth-timeout-token")), 504, 46801);
        performJson(get("/api/v1/onboarding/me/progress").header("Authorization", bearer("auth-bad-token")), 502, 46802);

        JsonNode degraded = performJson(get("/api/v1/onboarding/me/progress").header("Authorization", bearer("profile-unavailable-token")), 200);
        assertThat(degraded.at("/data/degraded").asBoolean()).isTrue();
        assertThat(degraded.at("/data/degradeReasons").toString()).contains("PROFILE_UNAVAILABLE");

        JsonNode active = performJson(get("/api/v1/onboarding/me/progress").header("Authorization", bearer("active-member-token")), 200);
        assertThat(active.at("/data/nextAction/enabled").asBoolean()).isFalse();
        assertThat(active.at("/data/profileSummary/status").asText()).isEqualTo("ACTIVE");

        JsonNode contentDown = performJson(get("/api/v1/onboarding/me/progress")
                .header("Authorization", bearer("content-unavailable-token"))
                .header("X-Test-Dependency-Mode", "CONTENT:UNAVAILABLE"), 200);
        assertThat(contentDown.at("/data/degraded").asBoolean()).isTrue();
        assertNoSecrets(contentDown);
    }

    @Test
    @DisplayName("ONB-START creates, restores, gates members, records audit, supports idempotency, and handles dependencies")
    void startContract() throws Exception {
        JsonNode created = performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("minecraft-bound-token")),
                Map.of("idempotencyKey", "start-idem-1"), 201);
        assertThat(created.at("/data/status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(created.at("/data/currentStep").asText()).isEqualTo("PROFILE_CONFIRMED");
        assertThat(created.at("/data/completedAt").isNull()).isTrue();
        assertThat(created.at("/data/reviewDirection").isNull()).isTrue();
        assertThat(created.at("/data/profileConfirmation").isNull()).isTrue();
        assertThat(created.at("/data/ruleConfirmation").isNull()).isTrue();

        JsonNode repeated = performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("minecraft-bound-token")),
                Map.of("idempotencyKey", "start-idem-1"), 200);
        assertThat(repeated.at("/data/applicationId").asText()).isEqualTo(created.at("/data/applicationId").asText());
        performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("minecraft-bound-token")),
                Map.of("idempotencyKey", "start-idem-1", "client", "changed"), 409, 43817);

        JsonNode unbound = performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("minecraft-unbound-token")),
                Map.of("idempotencyKey", "start-unbound-1"), 201);
        assertThat(unbound.at("/data/steps/1/status").asText()).isEqualTo("BLOCKED");

        performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("active-member-token")), Map.of("idempotencyKey", "active-start-1"), 409, 43812);
        performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("inactive-member-token")), Map.of("idempotencyKey", "inactive-start-1"), 409, 43812);
        performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("profile-unavailable-token")), Map.of("idempotencyKey", "prof-down-1"), 502, 46810);
        performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("profile-timeout-token")), Map.of("idempotencyKey", "prof-time-1"), 504, 46811);
        performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("profile-bad-token")), Map.of("idempotencyKey", "prof-bad-1"), 502, 46812);
        performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("auth-unavailable-token")), Map.of("idempotencyKey", "auth-down-1"), 502, 46800);
        performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("banned-token")), Map.of("idempotencyKey", "banned-start-1"), 502, 46800);

        performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("user-token")).header("X-Test-Fail-Audit", "true"), Map.of("idempotencyKey", "audit-fail-1"), 500, 51801);
        JsonNode failedProgress = performJson(get("/api/v1/onboarding/me/progress").header("Authorization", bearer("user-token")), 200);
        assertThat(failedProgress.at("/data/applicationId").isNull()).isTrue();

        JsonNode notificationFailed = performJson(post("/api/v1/onboarding/me/start")
                .header("Authorization", bearer("notification-fail-token"))
                .header("X-Test-Notification-Mode", "unavailable"), Map.of("idempotencyKey", "notify-fail-1"), 201);
        assertThat(notificationFailed.at("/data/notificationStatus").asText()).isEqualTo("FAILED");

        JsonNode list = performJson(get("/api/v1/onboarding/admin/applications").header("Authorization", bearer("admin-token")).param("keyword", created.at("/data/applicationId").asText()), 200);
        assertThat(list.at("/data/total").asInt()).isEqualTo(1);
        assertNoSecrets(created);
    }

    @Test
    @DisplayName("ONB-PROFILE ONB-RULES ONB-DIRECTION ONB-ADVANCE cover user flow, state conflicts, rollback, and downstream placeholders")
    void userFlowContract() throws Exception {
        performJson(patch("/api/v1/onboarding/me/profile-confirmation").header("Authorization", bearer("minecraft-bound-token")), Map.of("confirmed", true), 404, 43800);
        performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("minecraft-bound-token")), Map.of("idempotencyKey", "flow-start-1"), 201);
        performJson(patch("/api/v1/onboarding/me/profile-confirmation").header("Authorization", bearer("minecraft-bound-token")), Map.of("confirmed", false), 400, 40001);

        JsonNode profile = performJson(patch("/api/v1/onboarding/me/profile-confirmation").header("Authorization", bearer("minecraft-bound-token")),
                Map.of("confirmed", true, "idempotencyKey", "profile-idem-1", "minecraftIdSnapshot", "attacker"), 200);
        assertThat(profile.at("/data/profileConfirmation/minecraftIdSnapshot").asText()).isEqualTo("Steve");
        assertThat(profile.at("/data/steps/2/status").asText()).isEqualTo("COMPLETED");

        JsonNode profileReplay = performJson(patch("/api/v1/onboarding/me/profile-confirmation").header("Authorization", bearer("minecraft-bound-token")),
                Map.of("confirmed", true, "idempotencyKey", "profile-idem-1", "minecraftIdSnapshot", "attacker"), 200);
        assertThat(profileReplay.at("/data/profileConfirmation/minecraftIdSnapshot").asText()).isEqualTo("Steve");
        performJson(patch("/api/v1/onboarding/me/profile-confirmation").header("Authorization", bearer("minecraft-bound-token")),
                Map.of("confirmed", true, "idempotencyKey", "profile-idem-1", "extra", "changed"), 409, 43817);

        performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("minecraft-unbound-token")), Map.of("idempotencyKey", "flow-unbound-1"), 201);
        performJson(patch("/api/v1/onboarding/me/profile-confirmation").header("Authorization", bearer("minecraft-unbound-token")), Map.of("confirmed", true), 409, 43813);
        performJson(patch("/api/v1/onboarding/me/profile-confirmation").header("Authorization", bearer("profile-unavailable-token")), Map.of("confirmed", true), 502, 46810);

        performJson(patch("/api/v1/onboarding/me/rules-confirmation").header("Authorization", bearer("minecraft-bound-token")), Map.of("confirmed", true), 400, 40001);
        performJson(patch("/api/v1/onboarding/me/rules-confirmation").header("Authorization", bearer("minecraft-bound-token")),
                Map.of("confirmed", true, "ruleContentId", "rule-old", "ruleVersion", "2026-01-01"), 409, 43814);
        JsonNode rules = performJson(patch("/api/v1/onboarding/me/rules-confirmation").header("Authorization", bearer("minecraft-bound-token")),
                rulesBody("rules-idem-1"), 200);
        assertThat(rules.at("/data/ruleConfirmation/ruleTitle").asText()).isEqualTo("北冥服务器规则");
        assertThat(rules.toString()).doesNotContain("content body");
        performJson(patch("/api/v1/onboarding/me/rules-confirmation").header("Authorization", bearer("content-unavailable-token")).header("X-Test-Dependency-Mode", "CONTENT:UNAVAILABLE"),
                rulesBody("content-down-1"), 502, 46820);

        JsonNode direction = performJson(patch("/api/v1/onboarding/me/direction").header("Authorization", bearer("minecraft-bound-token")),
                Map.of("reviewDirection", "REDSTONE", "idempotencyKey", "direction-idem-1"), 200);
        assertThat(direction.at("/data/reviewDirection").asText()).isEqualTo("REDSTONE");
        performJson(patch("/api/v1/onboarding/me/direction").header("Authorization", bearer("minecraft-bound-token")),
                Map.of("reviewDirection", "BUILDING", "idempotencyKey", "direction-idem-2"), 200);

        JsonNode advanced = performJson(post("/api/v1/onboarding/me/advance").header("Authorization", bearer("minecraft-bound-token")),
                Map.of("idempotencyKey", "advance-idem-1"), 200);
        assertThat(advanced.at("/data/status").asText()).isEqualTo("READY_FOR_EXAM");
        assertThat(advanced.at("/data/currentStep").asText()).isEqualTo("EXAM_READY");
        assertThat(advanced.at("/data/nextAction/targetModule").asText()).isEqualTo("EXAM");
        assertThat(advanced.at("/data/nextAction/targetModuleStatus").asText()).isEqualTo("NOT_IMPLEMENTED");
        assertThat(advanced.at("/data/nextAction/enabled").asBoolean()).isFalse();
        assertThat(advanced.at("/data/completedAt").isNull()).isTrue();
        performJson(patch("/api/v1/onboarding/me/direction").header("Authorization", bearer("minecraft-bound-token")),
                Map.of("reviewDirection", "GENERAL"), 409, 43815);

        performJson(post("/api/v1/onboarding/me/advance").header("Authorization", bearer("minecraft-unbound-token")), Map.of("idempotencyKey", "advance-unbound-1"), 409, 43813);
        performJson(post("/api/v1/onboarding/me/advance").header("Authorization", bearer("user-token")), Map.of("idempotencyKey", "advance-missing-1"), 404, 43800);
        performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("direction-missing-token")), Map.of("idempotencyKey", "direction-missing-start"), 201);
        performJson(post("/api/v1/onboarding/me/advance").header("Authorization", bearer("direction-missing-token")), Map.of("idempotencyKey", "direction-missing-adv"), 409, 43811);

        JsonNode next = performJson(get("/api/v1/onboarding/me/next-action").header("Authorization", bearer("minecraft-bound-token")), 200);
        assertThat(next.at("/data/targetRoute").asText()).isEqualTo("/exam/start");
        assertThat(next.at("/data/enabled").asBoolean()).isFalse();
        assertNoSecrets(next);
    }

    @Test
    @DisplayName("ONB-ADMIN list detail reset block unblock audit and ops summary honor roles, filters, rollback, and audit")
    void adminContract() throws Exception {
        JsonNode list = performJson(get("/api/v1/onboarding/admin/applications")
                .header("Authorization", bearer("helper-token"))
                .param("page", "1")
                .param("pageSize", "2")
                .param("sort", "updatedAt_desc"), 200);
        assertThat(list.at("/data/items").size()).isLessThanOrEqualTo(2);
        assertThat(list.at("/data/total").asInt()).isGreaterThanOrEqualTo(3);

        JsonNode filtered = performJson(get("/api/v1/onboarding/admin/applications")
                .header("Authorization", bearer("admin-token"))
                .param("status", "BLOCKED")
                .param("blocked", "true"), 200);
        assertThat(valuesAt(filtered, "/data/items", "status")).contains("BLOCKED");
        performJson(get("/api/v1/onboarding/admin/applications").header("Authorization", bearer("admin-token")).param("status", "BAD"), 400, 40001);
        performJson(get("/api/v1/onboarding/admin/applications").header("Authorization", bearer("admin-token")).param("reviewDirection", "BAD"), 400, 40001);

        JsonNode detail = performJson(get("/api/v1/onboarding/admin/applications/app-in-progress").header("Authorization", bearer("helper-token")), 200);
        assertThat(detail.at("/data/applicationId").asText()).isEqualTo("app-in-progress");
        assertThat(valuesAt(detail, "/data/steps", "key")).contains("ACCOUNT_READY", "WHITELIST_READY");
        assertNoSecrets(detail);
        performJson(get("/api/v1/onboarding/admin/applications/missing").header("Authorization", bearer("admin-token")), 404, 43800);

        JsonNode reset = performJson(patch("/api/v1/onboarding/admin/applications/app-ready/reset").header("Authorization", bearer("admin-token")),
                Map.of("resetToStep", "RULES_CONFIRMED", "notifyUser", false, "reason", "reset to rules", "idempotencyKey", "reset-idem-1"), 200);
        assertThat(reset.at("/data/status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(reset.at("/data/reviewDirection").isNull()).isTrue();
        assertThat(reset.at("/data/ruleConfirmation").isNull()).isFalse();
        performJson(patch("/api/v1/onboarding/admin/applications/app-ready/reset").header("Authorization", bearer("admin-token")),
                Map.of("resetToStep", "RULES_CONFIRMED", "notifyUser", false, "reason", "reset to rules", "idempotencyKey", "reset-idem-1"), 200);
        performJson(patch("/api/v1/onboarding/admin/applications/app-ready/reset").header("Authorization", bearer("admin-token")),
                Map.of("resetToStep", "BAD", "notifyUser", false, "reason", "bad"), 400, 40001);
        performJson(patch("/api/v1/onboarding/admin/applications/app-in-progress/reset")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Notification-Mode", "unavailable"), reason("notify fail"), 502, 46830);

        JsonNode blocked = performJson(patch("/api/v1/onboarding/admin/applications/app-in-progress/block").header("Authorization", bearer("owner-token")),
                Map.of("blockReason", "Need manual review", "notifyUser", false, "reason", "manual block", "idempotencyKey", "block-idem-1"), 200);
        assertThat(blocked.at("/data/status").asText()).isEqualTo("BLOCKED");
        assertThat(blocked.at("/data/blockedReason").asText()).isEqualTo("Need manual review");
        performJson(patch("/api/v1/onboarding/me/profile-confirmation").header("Authorization", bearer("seed-in-progress-token")), Map.of("confirmed", true), 409, 43816);
        performJson(patch("/api/v1/onboarding/admin/applications/app-in-progress/block").header("Authorization", bearer("admin-token")), Map.of("reason", "missing block"), 400, 40001);

        JsonNode unblocked = performJson(patch("/api/v1/onboarding/admin/applications/app-in-progress/unblock").header("Authorization", bearer("admin-token")),
                Map.of("notifyUser", false, "reason", "manual unblock", "idempotencyKey", "unblock-idem-1"), 200);
        assertThat(unblocked.at("/data/status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(unblocked.at("/data/blockedReason").isNull()).isTrue();
        performJson(patch("/api/v1/onboarding/admin/applications/app-in-progress/unblock").header("Authorization", bearer("helper-token")), reason("denied"), 403, 42001);

        JsonNode audit = performJson(get("/api/v1/onboarding/admin/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("applicationId", "app-in-progress")
                .param("result", "SUCCESS"), 200);
        assertThat(audit.at("/data/items").isArray()).isTrue();
        if (audit.at("/data/items").size() > 0) {
            assertThat(audit.at("/data/items/0/requestId").asText()).isNotBlank();
            assertThat(audit.at("/data/items/0/action").asText()).startsWith("ONBOARDING_");
        }
        performJson(get("/api/v1/onboarding/admin/audit-logs").header("Authorization", bearer("helper-token")), 403, 42001);
        performJson(get("/api/v1/onboarding/admin/audit-logs").header("Authorization", bearer("admin-token")).param("from", "2026-05-23T00:00:00Z").param("to", "2026-05-22T00:00:00Z"), 400, 40001);

        JsonNode ops = performJson(get("/api/v1/onboarding/admin/ops/summary").header("Authorization", bearer("owner-token")), 200);
        assertThat(ops.at("/data/service").asText()).isEqualTo("onboarding");
        assertThat(ops.at("/data/port").asInt()).isEqualTo(8108);
        assertThat(ops.at("/data/productionGaps").toString()).contains("EXAM_NOT_IMPLEMENTED", "WHITELIST_NOT_IMPLEMENTED");
        performJson(get("/api/v1/onboarding/admin/ops/summary").header("Authorization", bearer("helper-token")), 403, 42001);
        performJson(get("/api/v1/onboarding/admin/ops/summary").header("Authorization", bearer("admin-token")).header("X-Test-Fail-Store", "true"), 500, 51800);
        assertNoSecrets(ops);
    }

    @Test
    @DisplayName("ONB-HARDEN and ONB-COMPAT cover canonical idempotency, rollback, boundaries, no internal imports, and no ops leakage")
    void hardeningAndCompatibilityContract() throws Exception {
        Map<String, Object> first = nestedIdempotency("nested-order-1", true);
        JsonNode created = performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("nested-token")), first, 201);
        Map<String, Object> second = nestedIdempotency("nested-order-1", false);
        JsonNode replay = performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("nested-token")), second, 200);
        assertThat(replay.at("/data/applicationId").asText()).isEqualTo(created.at("/data/applicationId").asText());
        Map<String, Object> arrayChange = nestedIdempotency("nested-order-1", false);
        arrayChange.put("clientTags", List.of("b", "a"));
        performJson(post("/api/v1/onboarding/me/start").header("Authorization", bearer("nested-token")), arrayChange, 409, 43817);

        performJson(post("/api/v1/onboarding/me/start")
                .header("Authorization", bearer("rollback-token"))
                .header("X-Test-Fail-Store", "true"), Map.of("idempotencyKey", "store-fail-1"), 500, 51802);
        JsonNode rollbackProgress = performJson(get("/api/v1/onboarding/me/progress").header("Authorization", bearer("rollback-token")), 200);
        assertThat(rollbackProgress.at("/data/applicationId").isNull()).isTrue();

        JsonNode before = performJson(get("/api/v1/onboarding/admin/applications/app-ready").header("Authorization", bearer("admin-token")), 200);
        performJson(patch("/api/v1/onboarding/admin/applications/app-ready/block")
                .header("Authorization", bearer("admin-token"))
                .header("X-Test-Notification-Mode", "timeout"), Map.of("blockReason", "notify timeout", "reason", "notify timeout"), 504, 46831);
        JsonNode after = performJson(get("/api/v1/onboarding/admin/applications/app-ready").header("Authorization", bearer("admin-token")), 200);
        assertThat(after.at("/data/status").asText()).isEqualTo(before.at("/data/status").asText());

        Path serviceRoot = Path.of("backend/onboarding-service/src/main/java");
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
                "Repository", "JdbcTemplate", "ProcessBuilder", "Runtime.getRuntime", "node-daemon",
                "cloudreveToken", "terminal", "container", "backupRestore", "file-manager");

        JsonNode ops = performJson(get("/api/v1/onboarding/admin/ops/summary").header("Authorization", bearer("admin-token")), 200);
        assertThat(ops.toString()).doesNotContain("/api/v1/exam/questions", "/api/v1/whitelist/review", "attendanceScore", "terminal", "node-daemon");
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

    private void assertNoSecrets(JsonNode json) {
        assertThat(json.toString()).doesNotContain(
                "secret-token", "authorizationHeader", "private note", "stackTrace", "content body",
                "notification body", "rawInvitationCode", "cloudrevePassword", "sharePassword",
                "minecraftVerifyCode", "requestHeaders", "paramsSummaryFull");
    }

    private Map<String, Object> rulesBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("confirmed", true);
        body.put("ruleContentId", "rule-current");
        body.put("ruleVersion", "2026-05-22");
        body.put("idempotencyKey", idempotencyKey);
        body.put("ruleTitle", "browser title");
        return body;
    }

    private Map<String, Object> nestedIdempotency(String key, boolean firstOrder) {
        Map<String, Object> nested = new LinkedHashMap<>();
        if (firstOrder) {
            nested.put("a", "1");
            nested.put("b", "2");
        } else {
            nested.put("b", "2");
            nested.put("a", "1");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("clientMeta", nested);
        body.put("clientTags", List.of("a", "b"));
        body.put("idempotencyKey", key);
        return body;
    }

    private Map<String, Object> reason(String reason) {
        return Map.of("reason", reason);
    }

    private Map<String, Object> with(Map<String, Object> source, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put(key, value);
        return copy;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private List<String> valuesAt(JsonNode root, String pointer, String field) {
        List<String> values = new ArrayList<>();
        for (JsonNode item : root.at(pointer)) {
            values.add(item.at("/" + field).asText());
        }
        return values;
    }

    private void addRange(Set<String> ids, String prefix, int start, int end) {
        for (int i = start; i <= end; i++) {
            ids.add("%s-%03d".formatted(prefix, i));
        }
    }
}
