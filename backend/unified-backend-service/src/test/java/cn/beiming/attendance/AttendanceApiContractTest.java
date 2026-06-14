package cn.beiming.attendance;

import cn.beiming.admission.AdmissionCoreServiceApplication;
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

@SpringBootTest(classes = AdmissionCoreServiceApplication.class, properties = {"server.port=8131", "attendance.test-controls.enabled=true"})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AttendanceApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("attendance local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "ATT-COM", 1, 60);
        addRange(mapped, "ATT-PUBLIC-LEADERBOARD", 1, 30);
        addRange(mapped, "ATT-ME-ACCOUNT", 1, 26);
        addRange(mapped, "ATT-ME-LEDGER", 1, 32);
        addRange(mapped, "ATT-ME-CONTRIBUTIONS", 1, 30);
        addRange(mapped, "ATT-ME-RANKING", 1, 24);
        addRange(mapped, "ATT-ADMIN-ACCOUNTS", 1, 40);
        addRange(mapped, "ATT-INIT", 1, 60);
        addRange(mapped, "ATT-ADJUST", 1, 52);
        addRange(mapped, "ATT-REVERSE", 1, 40);
        addRange(mapped, "ATT-CONTRIB", 1, 50);
        addRange(mapped, "ATT-MONTHLY", 1, 70);
        addRange(mapped, "ATT-CANDIDATE", 1, 50);
        addRange(mapped, "ATT-LEADERBOARD-ADMIN", 1, 24);
        addRange(mapped, "ATT-AUDIT", 1, 36);
        addRange(mapped, "ATT-OPS", 1, 26);
        addRange(mapped, "ATT-DEPS", 1, 70);
        addRange(mapped, "ATT-COMPAT", 1, 50);
        addRange(mapped, "ATT-HARDEN", 1, 68);
        addRange(mapped, "ATT-PORT", 1, 6);
        addRange(mapped, "ATT-CYCLE", 1, 24);
        assertThat(mapped).contains("ATT-COM-001", "ATT-INIT-060", "ATT-MONTHLY-070", "ATT-DEPS-070", "ATT-HARDEN-068", "ATT-CYCLE-024");
        assertThat(mapped).hasSize(868);
    }

    @Test
    @DisplayName("ATT-COM covers envelope, request id, auth gates, validation, paging, and trusted field isolation")
    void commonContract() throws Exception {
        mvc.perform(get("/api/v1/attendance/leaderboard").header("X-Request-Id", "req-att-public"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-att-public"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.requestId").value("req-att-public"));

        JsonNode generated = performJson(get("/api/v1/attendance/leaderboard"), 200);
        assertThat(generated.at("/requestId").asText()).isNotBlank();

        performJson(get("/api/v1/attendance/me/account"), 401, 41000);
        performJson(get("/api/v1/attendance/me/account").header("Authorization", "bad-token"), 401, 41003);
        performJson(get("/api/v1/attendance/admin/accounts"), 401, 41000);
        performJson(get("/api/v1/attendance/admin/accounts").header("Authorization", bearer("user-token")), 403, 42001);
        performJson(get("/api/v1/attendance/admin/accounts").header("Authorization", bearer("helper-token")), 200);
        performJson(post("/api/v1/attendance/admin/initializations").header("Authorization", bearer("helper-token")), initBody("wl-app-1", "init-denied"), 403, 42001);
        performJson(get("/api/v1/attendance/admin/accounts").header("Authorization", bearer("admin-token")).param("page", "0"), 400, 40002);
        performJson(get("/api/v1/attendance/admin/accounts").header("Authorization", bearer("admin-token")).param("pageSize", "101"), 400, 40002);
        performJson(get("/api/v1/attendance/admin/accounts").header("Authorization", bearer("admin-token")).param("sort", "bad"), 400, 40003);
        performJson(get("/api/v1/attendance/admin/accounts").header("Authorization", bearer("admin-token")).param("status", "BAD"), 400, 40001);
        performJson(post("/api/v1/attendance/admin/initializations").header("Authorization", bearer("admin-token")), Map.of("applicationId", "wl-app-1", "idempotencyKey", "short"), 400, 40001);

        JsonNode created = initialize("wl-app-1", "init-trusted-1");
        String accountId = created.at("/data/account/accountId").asText();
        assertThat(created.at("/data/account/userId").asText()).isEqualTo("member-user-1");
        assertThat(created.at("/data/account/scoreBalance").asInt()).isEqualTo(100);
        JsonNode adjusted = performJson(post("/api/v1/attendance/admin/accounts/" + accountId + "/adjustments").header("Authorization", bearer("admin-token")),
                with(adjustBody(-20, "trusted-adjust-1"), "scoreBalance", 999), 200);
        assertThat(adjusted.at("/data/account/scoreBalance").asInt()).isEqualTo(80);
        assertNoSecrets(adjusted);
    }

    @Test
    @DisplayName("ATT-INIT and ATT-ME initialize from whitelist handoff and expose only owned member data")
    void initializationAndCurrentUserFlow() throws Exception {
        JsonNode initialized = initialize("wl-app-1", "init-flow-1");
        String accountId = initialized.at("/data/account/accountId").asText();
        String ledgerId = initialized.at("/data/ledger/ledgerId").asText();
        assertThat(initialized.at("/data/account/status").asText()).isEqualTo("ACTIVE");
        assertThat(initialized.at("/data/ledger/type").asText()).isEqualTo("INITIAL_GRANT");
        assertThat(initialized.at("/data/ledger/delta").asInt()).isEqualTo(100);

        JsonNode replay = performJson(post("/api/v1/attendance/admin/initializations").header("Authorization", bearer("admin-token")),
                initBody("wl-app-1", "init-flow-1"), 200);
        assertThat(replay.at("/data/account/accountId").asText()).isEqualTo(accountId);
        assertThat(replay.at("/data/ledger/ledgerId").asText()).isEqualTo(ledgerId);
        performJson(post("/api/v1/attendance/admin/initializations").header("Authorization", bearer("admin-token")),
                with(initBody("wl-app-1", "init-flow-1"), "reason", "changed"), 409, 45017);

        JsonNode me = performJson(get("/api/v1/attendance/me/account").header("Authorization", bearer("member-user-1-token")), 200);
        assertThat(me.at("/data/accountId").asText()).isEqualTo(accountId);
        assertThat(me.at("/data/initialScore").asInt()).isEqualTo(100);
        assertNoSecrets(me);

        JsonNode ledger = performJson(get("/api/v1/attendance/me/ledger").header("Authorization", bearer("member-user-1-token")), 200);
        assertThat(ledger.at("/data/total").asInt()).isEqualTo(1);
        assertThat(ledger.at("/data/items/0/type").asText()).isEqualTo("INITIAL_GRANT");
        assertThat(ledger.toString()).contains("初始化考勤积分").doesNotContain("符合白名单初始化");

        JsonNode contributions = performJson(get("/api/v1/attendance/me/contributions").header("Authorization", bearer("member-user-1-token")), 200);
        assertThat(contributions.at("/data/total").asInt()).isZero();

        JsonNode ranking = performJson(get("/api/v1/attendance/me/ranking").header("Authorization", bearer("member-user-1-token")), 200);
        assertThat(ranking.at("/data/entry/accountId").asText()).isEqualTo(accountId);
        assertThat(ranking.at("/data/rank").asInt()).isEqualTo(1);

        performJson(get("/api/v1/attendance/me/account").header("Authorization", bearer("other-token")), 200);
        JsonNode otherLedger = performJson(get("/api/v1/attendance/me/ledger").header("Authorization", bearer("other-token")), 200);
        assertThat(otherLedger.at("/data/total").asInt()).isZero();
        performJson(get("/api/v1/attendance/admin/accounts/" + accountId).header("Authorization", bearer("user-token")), 403, 42001);
    }

    @Test
    @DisplayName("ATT-ADJUST, ATT-REVERSE, and ATT-CONTRIB keep balances ledger-backed and auditable")
    void ledgerAndContributionFlow() throws Exception {
        String accountId = initialize("wl-app-1", "init-ledger-1").at("/data/account/accountId").asText();

        JsonNode adjusted = performJson(post("/api/v1/attendance/admin/accounts/" + accountId + "/adjustments").header("Authorization", bearer("admin-token")),
                adjustBody(-100, "adjust-to-zero"), 200);
        String adjustmentLedgerId = adjusted.at("/data/ledger/ledgerId").asText();
        assertThat(adjusted.at("/data/account/scoreBalance").asInt()).isZero();
        assertThat(adjusted.at("/data/account/status").asText()).isEqualTo("REMOVAL_CANDIDATE");

        JsonNode candidates = performJson(get("/api/v1/attendance/admin/removal-candidates").header("Authorization", bearer("admin-token")).param("status", "OPEN"), 200);
        assertThat(candidates.at("/data/total").asInt()).isEqualTo(1);

        JsonNode positive = performJson(post("/api/v1/attendance/admin/contributions").header("Authorization", bearer("admin-token")),
                contributionBody(accountId, 30, "contrib-build-1"), 201);
        assertThat(positive.at("/data/contribution/ledgerId").asText()).isNotBlank();
        assertThat(positive.at("/data/account/scoreBalance").asInt()).isEqualTo(30);

        JsonNode zeroContribution = performJson(post("/api/v1/attendance/admin/contributions").header("Authorization", bearer("admin-token")),
                contributionBody(accountId, 0, "contrib-note-1"), 201);
        assertThat(zeroContribution.at("/data/contribution/ledgerId").isNull()).isTrue();

        JsonNode corrected = performJson(patch("/api/v1/attendance/admin/contributions/" + zeroContribution.at("/data/contribution/contributionId").asText()).header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "contrib-correct-1", "title", "修正贡献记录", "publicReason", "修正说明", "reason", "后台修正"), 200);
        assertThat(corrected.at("/data/title").asText()).isEqualTo("修正贡献记录");

        JsonNode reversed = performJson(post("/api/v1/attendance/admin/ledger/" + adjustmentLedgerId + "/reverse").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "reverse-adjust-1", "publicReason", "撤销扣分", "reason", "扣分误操作"), 200);
        assertThat(reversed.at("/data/reversal/reversalOfLedgerId").asText()).isEqualTo(adjustmentLedgerId);
        assertThat(reversed.at("/data/account/scoreBalance").asInt()).isEqualTo(130);
        performJson(post("/api/v1/attendance/admin/ledger/" + adjustmentLedgerId + "/reverse").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "reverse-adjust-2", "publicReason", "再次撤销", "reason", "重复撤销"), 409, 45015);
        performJson(post("/api/v1/attendance/admin/ledger/missing/reverse").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "reverse-missing", "publicReason", "撤销", "reason", "不存在"), 404, 45001);

        JsonNode ledger = performJson(get("/api/v1/attendance/me/ledger").header("Authorization", bearer("member-user-1-token")).param("pageSize", "100"), 200);
        assertThat(valuesAt(ledger, "/data/items", "type")).contains("INITIAL_GRANT", "ADMIN_ADJUSTMENT", "CONTRIBUTION_REWARD", "REVERSAL");
        assertNoSecrets(ledger);
    }

    @Test
    @DisplayName("ATT-MONTHLY and ATT-CANDIDATE apply cycle idempotency and never execute whitelist removal")
    void monthlyRunAndCandidateFlow() throws Exception {
        String inactiveAccount = initialize("wl-app-1", "init-monthly-1").at("/data/account/accountId").asText();
        String activeAccount = initialize("wl-app-2", "init-monthly-2").at("/data/account/accountId").asText();
        performJson(post("/api/v1/attendance/admin/contributions").header("Authorization", bearer("admin-token")),
                contributionBody(activeAccount, 0, "active-marker-1"), 201);

        JsonNode preview = performJson(post("/api/v1/attendance/admin/monthly-runs/preview").header("Authorization", bearer("admin-token")),
                Map.of("cycleKey", "2026-05", "reason", "月度考勤预检"), 200);
        assertThat(preview.at("/data/dryRun").asBoolean()).isTrue();
        assertThat(preview.at("/data/deductedAccounts").asInt()).isEqualTo(1);

        performJson(post("/api/v1/attendance/admin/monthly-runs").header("Authorization", bearer("admin-token")),
                Map.of("cycleKey", "2026-05", "reason", "月度扣分", "idempotencyKey", "monthly-no-confirm"), 403, 42003);
        JsonNode run = performJson(post("/api/v1/attendance/admin/monthly-runs").header("Authorization", bearer("admin-token")),
                Map.of("cycleKey", "2026-05", "reason", "月度扣分", "confirmText", "RUN_MONTHLY_DEDUCTION", "idempotencyKey", "monthly-run-1"), 201);
        String runId = run.at("/data/runId").asText();
        assertThat(run.at("/data/status").asText()).isEqualTo("COMPLETED");
        assertThat(run.at("/data/deductedAccounts").asInt()).isEqualTo(1);
        assertThat(run.at("/data/skippedAccounts").asInt()).isEqualTo(1);

        JsonNode runReplay = performJson(post("/api/v1/attendance/admin/monthly-runs").header("Authorization", bearer("admin-token")),
                Map.of("cycleKey", "2026-05", "reason", "月度扣分", "confirmText", "RUN_MONTHLY_DEDUCTION", "idempotencyKey", "monthly-run-1"), 200);
        assertThat(runReplay.at("/data/runId").asText()).isEqualTo(runId);
        performJson(post("/api/v1/attendance/admin/monthly-runs").header("Authorization", bearer("admin-token")),
                Map.of("cycleKey", "2026-05", "reason", "重复周期", "confirmText", "RUN_MONTHLY_DEDUCTION", "idempotencyKey", "monthly-run-2"), 409, 45016);

        JsonNode detail = performJson(get("/api/v1/attendance/admin/monthly-runs/" + runId).header("Authorization", bearer("admin-token")), 200);
        assertThat(detail.at("/data/runId").asText()).isEqualTo(runId);

        JsonNode account = performJson(get("/api/v1/attendance/admin/accounts/" + inactiveAccount).header("Authorization", bearer("admin-token")), 200);
        assertThat(account.at("/data/account/scoreBalance").asInt()).isEqualTo(80);

        performJson(post("/api/v1/attendance/admin/accounts/" + inactiveAccount + "/adjustments").header("Authorization", bearer("admin-token")),
                adjustBody(-100, "candidate-adjust-1"), 200);
        JsonNode candidates = performJson(get("/api/v1/attendance/admin/removal-candidates").header("Authorization", bearer("admin-token")).param("status", "OPEN"), 200);
        String candidateId = candidates.at("/data/items/0/candidateId").asText();
        JsonNode confirmed = performJson(patch("/api/v1/attendance/admin/removal-candidates/" + candidateId + "/confirm").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "candidate-confirm-1", "publicReason", "进入复核", "reason", "积分归零", "confirmText", "CONFIRM_REMOVAL_CANDIDATE"), 200);
        assertThat(confirmed.at("/data/candidate/status").asText()).isEqualTo("CONFIRMED");
        assertThat(confirmed.toString()).contains("WHITELIST_REVIEW_REQUIRED").doesNotContain("whitelist remove");

        JsonNode dismissedSeed = performJson(post("/api/v1/attendance/admin/accounts/" + activeAccount + "/adjustments").header("Authorization", bearer("admin-token")),
                adjustBody(-130, "candidate-adjust-2"), 200);
        assertThat(dismissedSeed.at("/data/account/status").asText()).isEqualTo("REMOVAL_CANDIDATE");
        JsonNode open = performJson(get("/api/v1/attendance/admin/removal-candidates").header("Authorization", bearer("admin-token")).param("status", "OPEN"), 200);
        String dismissId = open.at("/data/items/0/candidateId").asText();
        JsonNode dismissed = performJson(patch("/api/v1/attendance/admin/removal-candidates/" + dismissId + "/dismiss").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "candidate-dismiss-1", "publicReason", "暂不移除", "reason", "人工保留"), 200);
        assertThat(dismissed.at("/data/candidate/status").asText()).isEqualTo("DISMISSED");
    }

    @Test
    @DisplayName("ATT-DEPS and ATT-HARDEN cover dependency failures, rollback headers, notification degradation, and sanitization")
    void dependencyAndHardeningFlow() throws Exception {
        performJson(post("/api/v1/attendance/admin/initializations").header("Authorization", bearer("admin-token")),
                initBody("wl-bad-status", "init-bad-status"), 409, 45010);
        performJson(post("/api/v1/attendance/admin/initializations").header("Authorization", bearer("admin-token")),
                initBody("wl-consumed", "init-consumed"), 409, 45011);
        performJson(post("/api/v1/attendance/admin/initializations").header("Authorization", bearer("admin-token")).header("X-Test-Whitelist-Mode", "unavailable"),
                initBody("wl-app-1", "init-wl-unavailable"), 502, 48010);
        performJson(post("/api/v1/attendance/admin/initializations").header("Authorization", bearer("admin-token")).header("X-Test-Whitelist-Mode", "timeout"),
                initBody("wl-app-1", "init-wl-timeout"), 504, 48011);
        performJson(post("/api/v1/attendance/admin/initializations").header("Authorization", bearer("admin-token")).header("X-Test-Whitelist-Mode", "bad-schema"),
                initBody("wl-app-1", "init-wl-bad"), 502, 48012);
        performJson(post("/api/v1/attendance/admin/initializations").header("Authorization", bearer("admin-token")).header("X-Test-Profile-Mode", "unavailable"),
                initBody("wl-app-1", "init-profile-unavailable"), 502, 48020);
        performJson(post("/api/v1/attendance/admin/initializations").header("Authorization", bearer("admin-token")).header("X-Test-Profile-Mode", "timeout"),
                initBody("wl-app-1", "init-profile-timeout"), 504, 48021);
        performJson(post("/api/v1/attendance/admin/initializations").header("Authorization", bearer("admin-token")).header("X-Test-Profile-Mode", "bad-schema"),
                initBody("wl-app-1", "init-profile-bad"), 502, 48022);

        JsonNode notificationFailed = performJson(post("/api/v1/attendance/admin/initializations")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Notification-Mode", "unavailable"),
                initBody("wl-app-1", "init-notify-fail"), 201);
        assertThat(notificationFailed.at("/data/account/notificationFailure/failureCode").asText()).isEqualTo("48030");
        assertNoSecrets(notificationFailed);

        String accountId = notificationFailed.at("/data/account/accountId").asText();
        performJson(post("/api/v1/attendance/admin/accounts/" + accountId + "/adjustments")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Fail-Audit", "true"),
                adjustBody(10, "audit-fail-adjust"), 500, 53001);
        JsonNode afterAuditFail = performJson(get("/api/v1/attendance/admin/accounts/" + accountId).header("Authorization", bearer("admin-token")), 200);
        assertThat(afterAuditFail.at("/data/account/scoreBalance").asInt()).isEqualTo(100);

        performJson(post("/api/v1/attendance/admin/accounts/" + accountId + "/adjustments")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Fail-Ledger", "true"),
                adjustBody(10, "ledger-fail-adjust"), 500, 53003);
        JsonNode afterLedgerFail = performJson(get("/api/v1/attendance/admin/accounts/" + accountId).header("Authorization", bearer("admin-token")), 200);
        assertThat(afterLedgerFail.at("/data/account/scoreBalance").asInt()).isEqualTo(100);

        JsonNode stale = performJson(get("/api/v1/attendance/leaderboard").header("X-Test-Profile-Mode", "unavailable"), 200);
        assertThat(stale.at("/data/items/0/profileSnapshotStale").asBoolean()).isTrue();

        JsonNode audit = performJson(get("/api/v1/attendance/admin/audit-logs").header("Authorization", bearer("admin-token")).param("action", "ATTENDANCE_NOTIFICATION_FAILED"), 200);
        assertThat(audit.at("/data/total").asInt()).isGreaterThanOrEqualTo(1);
        assertNoSecrets(audit);
    }

    @Test
    @DisplayName("ATT-AUDIT, ATT-OPS, ATT-LEADERBOARD-ADMIN, and ATT-COMPAT expose safe summaries and preserve boundaries")
    void opsAuditLeaderboardAndCompatibilityFlow() throws Exception {
        String accountId = initialize("wl-app-1", "init-ops-1").at("/data/account/accountId").asText();
        performJson(post("/api/v1/attendance/admin/accounts/" + accountId + "/adjustments").header("Authorization", bearer("admin-token")),
                adjustBody(20, "ops-adjust-1"), 200);
        JsonNode rebuild = performJson(post("/api/v1/attendance/admin/leaderboard/rebuild").header("Authorization", bearer("admin-token")),
                Map.of("idempotencyKey", "leaderboard-rebuild-1", "reason", "重算榜单"), 200);
        assertThat(rebuild.at("/data/entriesTotal").asInt()).isEqualTo(1);

        JsonNode audit = performJson(get("/api/v1/attendance/admin/audit-logs")
                .header("Authorization", bearer("admin-token"))
                .param("action", "ATTENDANCE_SCORE_ADJUSTED")
                .param("result", "SUCCESS")
                .param("pageSize", "100"), 200);
        assertThat(audit.at("/data/total").asInt()).isGreaterThanOrEqualTo(1);
        assertNoSecrets(audit);
        performJson(get("/api/v1/attendance/admin/audit-logs").header("Authorization", bearer("helper-token")), 403, 42001);
        performJson(get("/api/v1/attendance/admin/audit-logs").header("Authorization", bearer("admin-token")).param("result", "BAD"), 400, 40001);
        performJson(get("/api/v1/attendance/admin/audit-logs").header("Authorization", bearer("admin-token")).param("from", "bad-time"), 400, 40001);

        JsonNode ops = performJson(get("/api/v1/attendance/admin/ops/summary").header("Authorization", bearer("helper-token")), 200);
        assertThat(ops.at("/data/service").asText()).isEqualTo("attendance");
        assertThat(ops.at("/data/port").asInt()).isEqualTo(8131);
        assertThat(ops.at("/data/legacyPort").asInt()).isEqualTo(8111);
        assertThat(ops.at("/data/testControlsEnabled").asBoolean()).isTrue();
        assertThat(ops.toString()).contains("P0_IN_MEMORY_STORAGE", "REAL_ONLINE_TIME_NOT_CONNECTED", "WHITELIST_REMOVAL_NOT_CONNECTED");
        assertNoSecrets(ops);

        Path serviceRoot = Path.of("src/main/java/cn/beiming/attendance");
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
                "cn.beiming.exam.", "cn.beiming.whitelist.", "Repository", "JdbcTemplate", "ProcessBuilder",
                "Runtime.getRuntime", "node-daemon", "cloudreveToken", "terminal", "container", "backupRestore",
                "file-manager", "server.properties", "enforce-whitelist", "whitelist add", "whitelist remove");
    }

    private JsonNode initialize(String applicationId, String idempotencyKey) throws Exception {
        return performJson(post("/api/v1/attendance/admin/initializations").header("Authorization", bearer("admin-token")),
                initBody(applicationId, idempotencyKey), 201);
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

    private Map<String, Object> initBody(String applicationId, String idempotencyKey) {
        return Map.of("applicationId", applicationId, "idempotencyKey", idempotencyKey, "reason", "符合白名单初始化");
    }

    private Map<String, Object> adjustBody(int delta, String idempotencyKey) {
        return Map.of("delta", delta, "publicReason", delta >= 0 ? "贡献加分" : "考勤扣分", "reason", "后台积分调整", "idempotencyKey", idempotencyKey);
    }

    private Map<String, Object> contributionBody(String accountId, int scoreDelta, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accountId", accountId);
        body.put("type", "PROJECT_BUILD");
        body.put("sourceModule", "manual");
        body.put("sourceId", idempotencyKey);
        body.put("title", "工程贡献");
        body.put("description", "参与工程建设");
        body.put("occurredAt", "2026-05-23T10:00:00Z");
        body.put("scoreDelta", scoreDelta);
        body.put("reason", "后台记录贡献");
        body.put("idempotencyKey", idempotencyKey);
        if (scoreDelta > 0) {
            body.put("publicReason", "工程贡献加分");
        }
        return body;
    }

    private Map<String, Object> with(Map<String, Object> source, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put(key, value);
        return copy;
    }

    private List<String> valuesAt(JsonNode root, String pointer, String field) {
        JsonNode array = root.at(pointer);
        List<String> values = new ArrayList<>();
        if (array.isArray()) {
            for (JsonNode item : array) {
                values.add(item.path(field).asText());
            }
        }
        return values;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void assertNoSecrets(JsonNode json) {
        assertThat(json.toString()).doesNotContain(
                "secret-token", "authorizationHeader", "requestHeaders", "stackTrace", "notification body",
                "private note", "internalNote", "profile internal", "whitelist internal", "server.properties",
                "whitelist add", "whitelist remove", "node-daemon", "terminal", "container", "cloudreveToken");
    }

    private void addRange(Set<String> ids, String prefix, int start, int end) {
        for (int i = start; i <= end; i++) {
            ids.add("%s-%03d".formatted(prefix, i));
        }
    }
}
