package cn.beiming.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthPortConfigTest {
    @Autowired
    Environment environment;

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("auth service uses the fixed local development port")
    void authServiceUsesFixedLocalDevelopmentPort() {
        assertThat(environment.getProperty("server.port")).isEqualTo("8101");
    }

    @Test
    @DisplayName("auth service allows the local auth test console origin")
    void authServiceAllowsLocalAuthTestConsoleOrigin() throws Exception {
        mvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"));
    }
}
