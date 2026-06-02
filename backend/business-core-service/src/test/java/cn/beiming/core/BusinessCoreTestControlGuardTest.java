package cn.beiming.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BusinessCoreServiceApplication.class, properties = {
        "beiming.business-core.test-control-headers.enabled=false",
        "beiming.admin.test-mode=true"
})
@AutoConfigureMockMvc
class BusinessCoreTestControlGuardTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void productionModeRejectsTestControlHeadersBeforeSelfEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/business-core/health")
                        .header("X-Request-Id", "req-test-control-self")
                        .header("X-Test-Fail-Audit", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Request-Id", "req-test-control-self"))
                .andExpect(jsonPath("$.code").value(51735))
                .andExpect(jsonPath("$.message").value("test control headers are disabled"))
                .andExpect(jsonPath("$.requestId").value("req-test-control-self"))
                .andReturn();

        assertSafeRejectBody(result);
    }

    @Test
    void productionModeRejectsTestControlHeadersBeforeBusinessModules() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("X-Test-Auth-Mode", "unavailable"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(51735));

        mockMvc.perform(get("/api/v1/admin/overview")
                        .header("Authorization", "Bearer owner-token")
                        .header("X-Beiming-Actor-User-Id", "gateway-owner")
                        .header("X-Beiming-Actor-Roles", "OWNER")
                        .header("X-Beiming-Actor-Permissions", "NODE_READ")
                        .header("X-Test-Module-Mode", "CONTENT:UNAVAILABLE")
                        .header("X-Test-Platform-Mode", "API_GATEWAY:UNAVAILABLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(51735));

        mockMvc.perform(get("/api/v1/resources/admin/ops/summary")
                        .header("Authorization", "Bearer admin-token")
                        .header("X-Test-Fail-Store", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(51735));
    }

    @Test
    void productionModeRejectBodyEscapesRequestIdAsJson() throws Exception {
        mockMvc.perform(get("/api/v1/business-core/health")
                        .header("X-Request-Id", "req-quote-\"-safe")
                        .header("X-Test-Fail-Audit", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Request-Id", "req-quote-\"-safe"))
                .andExpect(jsonPath("$.code").value(51735))
                .andExpect(jsonPath("$.requestId").value("req-quote-\"-safe"));
    }

    private void assertSafeRejectBody(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        assertThat(body)
                .doesNotContain("Authorization")
                .doesNotContain("Cookie")
                .doesNotContain("X-Test-Fail-Audit")
                .doesNotContain("true")
                .doesNotContain("jdbc:")
                .doesNotContain("Exception")
                .doesNotContain("C:\\")
                .doesNotContain("sharePassword")
                .doesNotContain("nodeSecret");
    }
}
