package cn.beiming.community;

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
class CommunityEngagementCoreProductionHardeningTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void testControlHeadersAreIgnoredByDefault() throws Exception {
        JsonNode created = performJson(post("/api/v1/community/me/posts")
                        .header("Authorization", bearer("member-user-1-token"))
                        .header("X-Test-Profile-Mode", "unavailable")
                        .header("X-Test-Content-Mode", "unavailable"),
                postBody("board-general", "prod-post-1"), 201);
        String postId = created.at("/data/postId").asText();

        performJson(post("/api/v1/community/me/posts/" + postId + "/submit").header("Authorization", bearer("member-user-1-token")),
                Map.of("idempotencyKey", "prod-submit-1"), 200);
        JsonNode approved = performJson(patch("/api/v1/community/admin/posts/" + postId + "/approve")
                        .header("Authorization", bearer("helper-token"))
                        .header("X-Test-Notification-Mode", "unavailable")
                        .header("X-Test-Fail-Audit", "true")
                        .header("X-Test-Fail-Store", "true"),
                Map.of("reviewComment", "审核通过", "reason", "生产控制头关闭", "idempotencyKey", "prod-approve-1"), 200);
        assertThat(approved.at("/data/status").asText()).isEqualTo("APPROVED");
        assertThat(approved.at("/data/notificationFailure").isNull()).isTrue();

        JsonNode ops = performJson(get("/api/v1/community/admin/ops/summary").header("Authorization", bearer("helper-token")), 200);
        assertThat(ops.at("/data/port").asInt()).isEqualTo(8132);
        assertThat(ops.at("/data/legacyPort").asInt()).isEqualTo(8112);
        assertThat(ops.at("/data/testControlsEnabled").asBoolean()).isFalse();
        assertThat(ops.toString()).contains("TEST_CONTROLS_DISABLED_OUTSIDE_TEST");
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

    private Map<String, Object> postBody(String boardId, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("boardId", boardId);
        body.put("type", "DISCUSSION");
        body.put("title", "生产硬化帖子");
        body.put("summary", "生产硬化测试");
        body.put("body", "默认运行环境必须忽略测试控制头。");
        body.put("tags", List.of("hardening"));
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
