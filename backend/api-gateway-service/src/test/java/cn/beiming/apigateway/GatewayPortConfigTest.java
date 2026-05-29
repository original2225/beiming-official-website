package cn.beiming.apigateway;

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

@SpringBootTest(classes = ApiGatewayServiceApplication.class)
@AutoConfigureMockMvc
class GatewayPortConfigTest {
    @Autowired
    Environment environment;

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("api-gateway service uses the fixed local development port")
    void apiGatewayUsesFixedLocalDevelopmentPort() {
        assertThat(environment.getProperty("server.port")).isEqualTo("8125");
    }

    @Test
    @DisplayName("api-gateway allows configured local frontend origins")
    void apiGatewayAllowsLocalFrontendOrigins() throws Exception {
        for (String origin : new String[]{
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:5174",
                "http://127.0.0.1:5174",
                "http://localhost:5182",
                "http://127.0.0.1:5182"
        }) {
            mvc.perform(options("/api/v1/auth/login")
                            .header("Origin", origin)
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "Authorization,Content-Type,X-Request-Id"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin))
                    .andExpect(header().string("Access-Control-Expose-Headers", "X-Request-Id"));
        }
    }
}
