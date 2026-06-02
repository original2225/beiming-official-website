package cn.beiming.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BusinessCoreServiceApplication.class, properties = {
        "beiming.business-core.test-control-headers.enabled=true",
        "beiming.admin.test-mode=true"
})
@AutoConfigureMockMvc
class BusinessCoreTestControlLocalModeTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void localTestModeStillAllowsInheritedFailureHooks() throws Exception {
        mockMvc.perform(get("/api/v1/resources/admin/ops/summary")
                        .header("Authorization", "Bearer admin-token")
                        .header("X-Test-Fail-Store", "true"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(51600));
    }
}
