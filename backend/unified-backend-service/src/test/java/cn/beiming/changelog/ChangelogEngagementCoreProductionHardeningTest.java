package cn.beiming.changelog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = cn.beiming.engagement.EngagementCoreServiceApplication.class, properties = "server.port=8132")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ChangelogEngagementCoreProductionHardeningTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void testControlHeadersAreIgnoredByDefault() throws Exception {
        JsonNode created = performJson(post("/api/v1/changelog/admin/releases")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Fail-Audit", "true")
                        .header("X-Test-Resource-Mode", "unavailable"),
                releaseBody("prod-changelog-1"), 201);
        String releaseId = created.at("/data/releaseId").asText();

        performJson(post("/api/v1/changelog/admin/releases/" + releaseId + "/submit")
                        .header("Authorization", bearer("helper-token")),
                Map.of("reason", "提交审核", "idempotencyKey", "prod-changelog-submit"), 200);
        JsonNode approved = performJson(patch("/api/v1/changelog/admin/releases/" + releaseId + "/approve")
                        .header("Authorization", bearer("helper-token"))
                        .header("X-Test-Notification-Mode", "unavailable")
                        .header("X-Test-Fail-Store", "true"),
                Map.of("reviewComment", "审核通过", "reason", "生产控制头关闭", "idempotencyKey", "prod-changelog-approve"), 200);
        assertThat(approved.at("/data/status").asText()).isEqualTo("APPROVED");
        assertThat(approved.at("/data/notificationSummary/failure").isNull()).isTrue();

        JsonNode ops = performJson(get("/api/v1/changelog/admin/ops/summary").header("Authorization", bearer("helper-token")), 200);
        assertThat(ops.at("/data/port").asInt()).isEqualTo(8132);
        assertThat(ops.at("/data/legacyPort").asInt()).isEqualTo(8115);
        assertThat(ops.at("/data/testControlsEnabled").asBoolean()).isFalse();
        assertThat(ops.toString()).contains("TEST_CONTROLS_DISABLED_OUTSIDE_TEST", "CALENDAR_WRITE_NOT_CONNECTED");
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, int status) throws Exception {
        MvcResult result = mvc.perform(builder.accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
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

    private Map<String, Object> releaseBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("slug", idempotencyKey);
        body.put("versionName", "v1.20.4-" + idempotencyKey);
        body.put("title", "生产硬化更新日志");
        body.put("summary", "默认运行环境必须忽略测试控制头");
        body.put("body", "changelog 生产硬化测试。");
        body.put("type", "SERVER_VERSION");
        body.put("visibility", "PUBLIC");
        body.put("impactLevel", "MEDIUM");
        body.put("releasedAt", "2026-06-01T12:00:00Z");
        body.put("effectiveAt", "2026-06-01T13:00:00Z");
        body.put("minecraftVersion", "1.20.4");
        body.put("groups", List.of(group("ADDED", "新增内容")));
        body.put("relatedResourceIds", List.of("resource-pack-1"));
        body.put("relatedServerInstanceIds", List.of("survival-main"));
        body.put("relatedContentId", "content-release-note");
        body.put("reason", "创建生产硬化更新日志");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> group(String type, String title) {
        return Map.of(
                "type", type,
                "title", title,
                "description", "分组说明",
                "items", List.of(Map.of(
                        "title", "变更项",
                        "description", "公开安全的变更说明",
                        "severity", "INFO",
                        "component", "server",
                        "publicSafe", true,
                        "sortOrder", 10
                )),
                "sortOrder", 10
        );
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
