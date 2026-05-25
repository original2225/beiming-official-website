package cn.beiming.calendar;

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

@SpringBootTest(classes = CalendarServiceApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CalendarProductionHardeningTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void testControlHeadersAreIgnoredByDefault() throws Exception {
        JsonNode created = performJson(post("/api/v1/calendar/admin/events")
                        .header("Authorization", bearer("admin-token"))
                        .header("X-Test-Fail-Audit", "true"),
                eventBody("prod-calendar-1"), 201);
        String eventId = created.at("/data/eventId").asText();

        performJson(post("/api/v1/calendar/admin/events/" + eventId + "/submit")
                        .header("Authorization", bearer("helper-token")),
                Map.of("reason", "提交审核", "idempotencyKey", "prod-calendar-submit"), 200);
        JsonNode approved = performJson(patch("/api/v1/calendar/admin/events/" + eventId + "/approve")
                        .header("Authorization", bearer("helper-token"))
                        .header("X-Test-Notification-Mode", "unavailable")
                        .header("X-Test-Fail-Store", "true"),
                Map.of("reviewComment", "审核通过", "reason", "生产控制头关闭", "idempotencyKey", "prod-calendar-approve"), 200);
        assertThat(approved.at("/data/status").asText()).isEqualTo("APPROVED");
        assertThat(approved.at("/data/reminderPolicy/failure").isNull()).isTrue();

        JsonNode ops = performJson(get("/api/v1/calendar/admin/ops/summary").header("Authorization", bearer("helper-token")), 200);
        assertThat(ops.at("/data/testControlsEnabled").asBoolean()).isFalse();
        assertThat(ops.toString()).contains("TEST_CONTROLS_DISABLED_OUTSIDE_TEST", "CHANGELOG_NOT_CONNECTED");
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

    private Map<String, Object> eventBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "生产硬化日程");
        body.put("summary", "默认运行环境必须忽略测试控制头");
        body.put("description", "calendar 生产硬化测试。");
        body.put("type", "MAINTENANCE");
        body.put("visibility", "PUBLIC");
        body.put("startAt", "2026-06-01T12:00:00Z");
        body.put("endAt", "2026-06-01T14:00:00Z");
        body.put("timezone", "Asia/Shanghai");
        body.put("allDay", false);
        body.put("location", "北冥服务器");
        body.put("relatedUrl", "/maintenance");
        body.put("labels", List.of("maintenance"));
        body.put("priority", 50);
        body.put("reason", "创建生产硬化日程");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
