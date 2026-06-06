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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {UnifiedBackendServiceApplication.class, UnifiedBackendInProcessMountTest.FailOnMountedProxyConfig.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class UnifiedBackendInProcessMountTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private FailOnMountedProxyHttpClient client;

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
    void servesBusinessCoreRoutesInProcessBeforeGatewayCatchAllProxy() throws Exception {
        mvc.perform(get("/api/v1/business-core/health").header("X-Request-Id", "req-business-core-self"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("business-core"));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user\",\"password\":\"Password12345\"}")
                        .header("X-Request-Id", "req-auth-in-process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").exists());

        mvc.perform(get("/api/v1/profile/members").header("X-Request-Id", "req-profile-in-process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mvc.perform(get("/api/v1/notifications/me/unread-count")
                        .header("Authorization", "Bearer user-token")
                        .header("X-Request-Id", "req-notification-in-process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mvc.perform(get("/api/v1/content/home").header("X-Request-Id", "req-content-in-process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mvc.perform(get("/api/v1/server-status/overview").header("X-Request-Id", "req-status-in-process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mvc.perform(get("/api/v1/resources").header("X-Request-Id", "req-resource-in-process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mvc.perform(get("/api/v1/admin/overview")
                        .header("Authorization", "Bearer helper-token")
                        .header("X-Request-Id", "req-admin-in-process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        assertThat(client.calls()).doesNotContain(
                "AUTH", "PROFILE", "NOTIFICATION", "CONTENT", "SERVER_STATUS", "RESOURCE", "ADMIN");
    }

    @Test
    void servesAdmissionCoreRoutesInProcessBeforeGatewayCatchAllProxy() throws Exception {
        mvc.perform(get("/api/v1/admission-core/health").header("X-Request-Id", "req-admission-core-self"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("admission-core"));

        mvc.perform(get("/api/v1/onboarding/me/progress").header("X-Request-Id", "req-onboarding-in-process"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));

        mvc.perform(get("/api/v1/exams/me/sessions/current").header("X-Request-Id", "req-exam-in-process"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));

        mvc.perform(get("/api/v1/whitelist/me/applications/current").header("X-Request-Id", "req-whitelist-in-process"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));

        mvc.perform(get("/api/v1/attendance/leaderboard").header("X-Request-Id", "req-attendance-in-process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        assertThat(client.calls()).doesNotContain("ONBOARDING", "EXAM", "WHITELIST", "ATTENDANCE");
    }

    @Test
    void stillServesGatewayBusinessCoreAndPortalCoreSelfApisThroughCandidateEntrypoint() throws Exception {
        mvc.perform(get("/api/v1/gateway/health").header("X-Request-Id", "req-gateway-self"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("api-gateway"));

        mvc.perform(get("/api/v1/business-core/health").header("X-Request-Id", "req-business-self"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("business-core"));

        mvc.perform(get("/api/v1/admission-core/health").header("X-Request-Id", "req-admission-self"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("admission-core"));

        mvc.perform(get("/api/v1/portal-core/health").header("X-Request-Id", "req-portal-self"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("portal-core"));
    }

    @TestConfiguration
    static class FailOnMountedProxyConfig {
        @Bean
        @Primary
        FailOnMountedProxyHttpClient failOnMountedProxyHttpClient() {
            return new FailOnMountedProxyHttpClient();
        }
    }

    static class FailOnMountedProxyHttpClient implements GatewayHttpClient {
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
            if (List.of(
                    "AUTH", "PROFILE", "NOTIFICATION", "CONTENT", "SERVER_STATUS", "RESOURCE", "ADMIN",
                    "ONBOARDING", "EXAM", "WHITELIST", "ATTENDANCE",
                    "GUIDE", "MATERIAL", "ONLINE_MAP"
            ).contains(route.serviceKey())) {
                throw new AssertionError(route.serviceKey() + " must be served in-process");
            }
            byte[] body = "{\"code\":0,\"message\":\"success\",\"data\":{\"service\":\"fallback\"}}".getBytes(StandardCharsets.UTF_8);
            return new GatewayHttpResponse(200, MediaType.APPLICATION_JSON_VALUE, body, new LinkedHashMap<>());
        }
    }
}
