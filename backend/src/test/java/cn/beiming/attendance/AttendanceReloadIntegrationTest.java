package cn.beiming.attendance;

import cn.beiming.admission.AdmissionCoreServiceApplication;
import cn.beiming.admission.persistence.AttendancePersistence;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 考勤回读契约：写进去的账户、流水、贡献、月度结算，重建模块实例后必须读得回来。
 *
 * <p>锁的是那个不报错的 bug：{@code AttendancePersistence} 只有 7 个 persist 方法、一个 load 都没有，
 * 进程重启后 {@code AttendanceStore} 的内存 Map 全空，第一批玩家初始化出来的考勤直接消失，
 * 接口 200、库里有行、审计也写了，全程无异常无告警。这与 exam、whitelist 是同一个「假持久化」形态。
 *
 * <p>必须在真实 PostgreSQL 上跑：内存模式下 loadXxx 返回空，「读得回来」无从验证
 * （这正是假持久化能一直绿的原因）。验证方式是「重建模块实例」而不是只看接口——
 * 只看接口会被内存响应模型冒充成持久化。
 */
@SpringBootTest(
        classes = AdmissionCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=",
                "spring.flyway.enabled=true",
                "attendance.test-controls.enabled=false",
                "attendance.auth.stub-tokens-enabled=true"
        }
)
@Testcontainers
class AttendanceReloadIntegrationTest {

    static {
        System.setProperty("api.version", System.getProperty("api.version", "1.40"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("beiming_attendance_reload_pg")
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
    AttendancePersistence persistence;

    @Test
    @DisplayName("ATTENDANCE-RELOAD-001 账户/流水/贡献/月度结算写库后，重建模块实例仍读得回且一致")
    void writtenAttendanceStateSurvivesModuleRebuild() throws Exception {
        // ---- 写入：初始化账户（产生账户 + 初始流水） ----
        Map<String, Object> initBody = new LinkedHashMap<>();
        initBody.put("applicationId", "wl-app-1");
        initBody.put("idempotencyKey", "reload-init-" + UUID.randomUUID());
        initBody.put("reason", "符合白名单初始化");
        JsonNode initialized = send(HttpMethod.POST, "/api/v1/attendance/admin/initializations", initBody, 201);
        String accountId = initialized.at("/data/account/accountId").asText();
        assertThat(accountId).isNotBlank();
        int initBalance = initialized.at("/data/account/scoreBalance").asInt();
        assertThat(initBalance).isEqualTo(100);

        // ---- 写入：贡献 +30（产生贡献记录 + CONTRIBUTION_REWARD 流水，余额 100→130） ----
        Map<String, Object> contributionBody = new LinkedHashMap<>();
        contributionBody.put("accountId", accountId);
        contributionBody.put("type", "PROJECT_BUILD");
        contributionBody.put("sourceModule", "manual");
        contributionBody.put("sourceId", "reload-src-" + UUID.randomUUID());
        contributionBody.put("title", "回读探针工程贡献");
        contributionBody.put("description", "重建模块实例后要能读回这条贡献");
        contributionBody.put("occurredAt", "2026-05-23T10:00:00Z");
        contributionBody.put("scoreDelta", 30);
        contributionBody.put("publicReason", "工程贡献加分");
        contributionBody.put("reason", "后台记录贡献");
        contributionBody.put("idempotencyKey", "reload-contrib-" + UUID.randomUUID());
        JsonNode contribution = send(HttpMethod.POST, "/api/v1/attendance/admin/contributions", contributionBody, 201);
        String contributionId = contribution.at("/data/contribution/contributionId").asText();
        String contributionLedgerId = contribution.at("/data/ledger/ledgerId").asText();
        assertThat(contributionId).isNotBlank();

        // ---- 写入：月度结算 2026-06 扣 20（贡献在 5 月、扣分在 6 月，避免被"本周期有贡献"跳过） ----
        Map<String, Object> monthlyBody = new LinkedHashMap<>();
        monthlyBody.put("cycleKey", "2026-06");
        monthlyBody.put("deductionScore", 20);
        monthlyBody.put("reason", "月度扣分");
        monthlyBody.put("confirmText", "RUN_MONTHLY_DEDUCTION");
        monthlyBody.put("idempotencyKey", "reload-monthly-" + UUID.randomUUID());
        JsonNode run = send(HttpMethod.POST, "/api/v1/attendance/admin/monthly-runs", monthlyBody, 201);
        String runId = run.at("/data/runId").asText();
        assertThat(runId).isNotBlank();

        // ---- 先直接查库，确认四张业务表确实有行（真落库基线） ----
        try (Connection connection = openConnection()) {
            assertThat(count(connection, "SELECT COUNT(*) FROM attendance_accounts WHERE account_id = '" + accountId + "'"))
                    .as("账户必须真的落库").isEqualTo(1);
            assertThat(count(connection, "SELECT COUNT(*) FROM attendance_ledgers WHERE account_id = '" + accountId + "'"))
                    .as("初始化流水 + 贡献流水 + 月度扣分流水，至少 3 条").isGreaterThanOrEqualTo(3);
            assertThat(count(connection, "SELECT COUNT(*) FROM attendance_contributions WHERE contribution_id = '" + contributionId + "'"))
                    .as("贡献必须真的落库").isEqualTo(1);
            assertThat(count(connection, "SELECT COUNT(*) FROM attendance_monthly_runs WHERE run_id = '" + runId + "'"))
                    .as("月度结算必须真的落库").isEqualTo(1);
            assertThat(count(connection, "SELECT score_balance FROM attendance_accounts WHERE account_id = '" + accountId + "'"))
                    .as("余额 100+30-20=110").isEqualTo(110);
        }

        // ---- 重建模块实例：模拟进程重启，从空内存开始，只靠库回读 ----
        AttendanceStore rebuilt = new AttendanceStore(new AttendanceTestControls(false), persistence);
        rebuilt.hydrate();

        // ---- 断言：账户读回，余额与状态一致 ----
        AttendanceAccountRecord account = rebuilt.accountsSnapshot().get(accountId);
        assertThat(account).as("重启后账户必须读得回来，否则第一批玩家的考勤全丢").isNotNull();
        assertThat(account.scoreBalance).as("余额必须与写进去的一致（100+30-20）").isEqualTo(110);
        assertThat(account.status).isEqualTo("ACTIVE");
        assertThat(account.userId).isEqualTo("member-user-1");
        assertThat(account.memberId).isEqualTo("member-1");

        // ---- 派生索引必须重建：靠 userId 反查账户（myAccount / me/* 全靠它） ----
        assertThat(rebuilt.accountIdByUser("member-user-1"))
                .as("userId→accountId 索引不重建，玩家登录后 /me/account 直接空").isEqualTo(accountId);
        assertThat(rebuilt.accountIdByMember("member-1"))
                .as("memberId→accountId 索引不重建，重复初始化拦不住").isEqualTo(accountId);
        assertThat(rebuilt.accountIdByApplication("wl-app-1"))
                .as("applicationId 索引不重建，同一申请单会被重复初始化").isEqualTo(accountId);

        // ---- 断言：流水读回，条数与类型对得上 ----
        assertThat(rebuilt.ledgersSnapshot().values().stream()
                .filter(ledger -> accountId.equals(ledger.accountId))
                .count()).as("三条流水（初始/贡献/月度）都要读回来").isGreaterThanOrEqualTo(3);
        assertThat(rebuilt.ledgersSnapshot().containsKey(contributionLedgerId))
                .as("贡献产出的流水要读得回").isTrue();
        assertThat(rebuilt.ledgersSnapshot().get(contributionLedgerId).type).isEqualTo("CONTRIBUTION_REWARD");
        assertThat(rebuilt.ledgersSnapshot().get(contributionLedgerId).delta).isEqualTo(30);
        assertThat(rebuilt.ledgersSnapshot().values().stream()
                .anyMatch(ledger -> "MONTHLY_DEDUCTION".equals(ledger.type) && accountId.equals(ledger.accountId)))
                .as("月度扣分流水要读得回").isTrue();

        // ---- 断言：贡献读回，字段一致 ----
        ContributionRecord storedContribution = rebuilt.contributionsSnapshot().get(contributionId);
        assertThat(storedContribution).as("贡献必须读得回").isNotNull();
        assertThat(storedContribution.scoreDelta).isEqualTo(30);
        assertThat(storedContribution.title).isEqualTo("回读探针工程贡献");

        // ---- 断言：月度结算读回，同一周期不能重复扣 ----
        MonthlyRunRecord storedRun = rebuilt.monthlyRunsSnapshot().get(runId);
        assertThat(storedRun).as("月度结算必须读得回；否则重启后同一周期会被当成没执行过，全服重复扣分").isNotNull();
        assertThat(storedRun.status).isEqualTo("COMPLETED");
        assertThat(storedRun.dryRun).isFalse();
        assertThat(storedRun.cycleKey).isEqualTo("2026-06");

        System.out.println("SQL evidence: PostgreSQL attendance reload reconstructed account/ledger/contribution/monthly-run from attendance_* tables and rebuilt userId/memberId/applicationId indexes.");
    }

    @Test
    @DisplayName("ATTENDANCE-RELOAD-002 覆盖式回读：库里没有的账户不会被内存残留复活")
    void hydrateOverwritesInsteadOfMerging() throws Exception {
        // 先建一个账户并落库
        Map<String, Object> initBody = new LinkedHashMap<>();
        initBody.put("applicationId", "wl-app-2");
        initBody.put("idempotencyKey", "reload2-init-" + UUID.randomUUID());
        initBody.put("reason", "符合白名单初始化");
        JsonNode initialized = send(HttpMethod.POST, "/api/v1/attendance/admin/initializations", initBody, 201);
        String accountId = initialized.at("/data/account/accountId").asText();

        // 造一个「内存里有一堆脏数据」的实例，再 hydrate：库里只有这一个账户，回读后不该保留别的
        AttendanceStore rebuilt = new AttendanceStore(new AttendanceTestControls(false), persistence);
        AttendanceAccountRecord ghost = new AttendanceAccountRecord();
        ghost.accountId = "att-acc-999999";
        ghost.userId = "ghost-user";
        ghost.memberId = "ghost-member";
        ghost.status = "ACTIVE";
        rebuilt.accountsSnapshot().put(ghost.accountId, ghost);

        rebuilt.hydrate();

        assertThat(rebuilt.accountsSnapshot().containsKey(accountId)).as("库里的账户要在").isTrue();
        assertThat(rebuilt.accountsSnapshot().containsKey("att-acc-999999"))
                .as("覆盖式回读：内存里的残留（库里没有的）必须被清掉，不能合并复活").isFalse();

        // 主键序号必须推进到库里已有 ID 之上，重启后新建记录不撞主键
        String newIdProbe = "att-acc-" + (rebuilt.currentIdSeq() + 1);
        assertThat(newIdProbe).as("idSeq 必须推进到已读回 ID 之上，否则新账户撞老主键").isNotEqualTo(accountId);
    }

    private long count(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getLong(1);
        }
    }

    private JsonNode send(HttpMethod method, String path, Map<String, Object> body, int expectedStatus) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        if (body == null) {
            builder.method(method.name(), HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .method(method.name(), HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        }
        HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .as("%s %s 期望 %s，实际 %s，body=%s", method, path, expectedStatus, response.statusCode(), response.body())
                .isEqualTo(expectedStatus);
        return objectMapper.readTree(response.body());
    }

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
