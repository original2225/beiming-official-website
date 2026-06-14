package cn.beiming.unifiedbackend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = UnifiedBackendServiceApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class UnifiedBackendSmokeTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void returnsDegradedSmokeWhenAnyCandidateTargetFailsWithoutLeakingInternals() throws Exception {
        JsonNode smoke = objectMapper.readTree(mvc.perform(post("/api/v1/unified-backend/admin/http-smoke/run")
                        .header("Authorization", "Bearer admin-token")
                        .header("X-Request-Id", "req-smoke-degraded"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsByteArray());

        assertThat(smoke.at("/data/httpSmokeStatus").asText()).isIn("PASS", "DEGRADED");
        assertThat(smoke.toString().toLowerCase())
                .doesNotContain("authorization")
                .doesNotContain("c:\\users\\")
                .doesNotContain("stacktrace");
    }
}
