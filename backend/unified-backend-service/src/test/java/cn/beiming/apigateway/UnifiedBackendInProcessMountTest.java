package cn.beiming.apigateway;

import cn.beiming.unifiedbackend.UnifiedBackendServiceApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {UnifiedBackendServiceApplication.class, UnifiedBackendInProcessMountTest.FailOnPortalProxyConfig.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class UnifiedBackendInProcessMountTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private FailOnPortalProxyHttpClient client;

    @BeforeEach
    void reset() {
        client.reset();
    }

    @Test
    void servesGuideMaterialAndOnlineMapInProcessBeforeGatewayCatchAllProxy() throws Exception {
        mvc.perform(get("/api/v1/guides/categories").header("X-Request-Id", "req-guide-in-process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mvc.perform(get("/api/v1/materials/featured").header("X-Request-Id", "req-material-in-process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mvc.perform(get("/api/v1/online-map/health").header("X-Request-Id", "req-online-map-in-process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        assertThat(client.calls()).doesNotContain("GUIDE", "MATERIAL", "ONLINE_MAP");
    }

    @Test
    void stillServesGatewayAndPortalCoreSelfApisThroughCandidateEntrypoint() throws Exception {
        mvc.perform(get("/api/v1/gateway/health").header("X-Request-Id", "req-gateway-self"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("api-gateway"));

        mvc.perform(get("/api/v1/portal-core/health").header("X-Request-Id", "req-portal-self"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("portal-core"));
    }

    @TestConfiguration
    static class FailOnPortalProxyConfig {
        @Bean
        @Primary
        FailOnPortalProxyHttpClient failOnPortalProxyHttpClient() {
            return new FailOnPortalProxyHttpClient();
        }
    }

    static class FailOnPortalProxyHttpClient implements GatewayHttpClient {
        private final List<String> calls = new java.util.concurrent.CopyOnWriteArrayList<>();

        void reset() {
            calls.clear();
        }

        List<String> calls() {
            return List.copyOf(calls);
        }

        @Override
        public GatewayHttpResponse exchange(GatewayRoute route, GatewayHttpRequest request) {
            calls.add(route.serviceKey());
            if (List.of("GUIDE", "MATERIAL", "ONLINE_MAP").contains(route.serviceKey())) {
                throw new AssertionError(route.serviceKey() + " must be served in-process");
            }
            byte[] body = "{\"code\":0,\"message\":\"success\",\"data\":{\"service\":\"fallback\"}}".getBytes(StandardCharsets.UTF_8);
            return new GatewayHttpResponse(200, MediaType.APPLICATION_JSON_VALUE, body, new LinkedHashMap<>());
        }
    }
}
