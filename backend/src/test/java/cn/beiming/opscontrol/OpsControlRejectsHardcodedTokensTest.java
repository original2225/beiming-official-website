package cn.beiming.opscontrol;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 运维接口必须只认真实登录会话，不得接受源码里写死的桩 token。
 *
 * <p>改造前这两类串是硬编码在 {@code OpsAuth} 里的常量，任何读过仓库的人拿
 * {@code owner-token} 就能取得 OWNER 权限。这个测试把那条路堵死，防止以后为了跑通测试
 * 又把 {@code stub-tokens-enabled} 的默认值改回 true。
 *
 * <p>注意：本类**故意不开启** {@code ops-control.auth.stub-tokens-enabled}，
 * 这是被测行为本身，不是配置遗漏。
 */
@SpringBootTest(classes = cn.beiming.opscore.OpsCoreServiceApplication.class,
        properties = {"server.port=8133"})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OpsControlRejectsHardcodedTokensTest {
    private static final String[] HARDCODED_TOKENS = {
            "owner-token",
            "ops-admin-token",
            "ops-viewer-token",
            "ops-node-writer-token",
            "ops-container-token",
            "ops-file-token",
            "ops-terminal-token",
            "ops-approver-token",
            "user-token",
    };

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void hardcodedTokensAreRejectedOnReadEndpoints() throws Exception {
        for (String token : HARDCODED_TOKENS) {
            JsonNode body = perform(get("/api/v1/ops-control/overview"), token);
            assertThat(body.at("/code").asInt())
                    .as("hardcoded token %s must not authenticate", token)
                    .isEqualTo(41003);
        }
    }

    @Test
    void hardcodedTokensAreRejectedOnWriteEndpoints() throws Exception {
        for (String token : HARDCODED_TOKENS) {
            JsonNode body = perform(get("/api/v1/ops-control/nodes"), token);
            assertThat(body.at("/code").asInt())
                    .as("hardcoded token %s must not authenticate", token)
                    .isEqualTo(41003);
        }
    }

    @Test
    void missingAuthorizationHeaderReportsUnauthenticated() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/ops-control/overview").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.at("/code").asInt()).isEqualTo(41000);
    }

    private JsonNode perform(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
                             String token) throws Exception {
        MvcResult result = mvc.perform(builder
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}