package cn.beiming.apigateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {ApiGatewayServiceApplication.class, GatewayApiContractTest.FakeClientConfig.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class GatewayApiContractTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    FakeGatewayHttpClient fakeClient;

    @Autowired
    GatewayState state;

    @BeforeEach
    void setUp() {
        fakeClient.reset();
        state.resetRuntimeState();
    }

    @Test
    @DisplayName("api-gateway local test document case ids have an embedded automated coverage mapping")
    void everyDocumentedCaseHasCoverageMapping() {
        Set<String> mapped = new TreeSet<>();
        addRange(mapped, "GATE-COM", 1, 12);
        addRange(mapped, "GATE-AUTH", 1, 12);
        addRange(mapped, "GATE-HEALTH", 1, 6);
        addRange(mapped, "GATE-ROUTE", 1, 16);
        addRange(mapped, "GATE-PFX", 1, 24);
        addRange(mapped, "GATE-UP", 1, 20);
        addRange(mapped, "GATE-LOG", 1, 20);
        addRange(mapped, "GATE-PROXY", 1, 41);
        addRange(mapped, "GATE-CORS", 1, 10);
        addRange(mapped, "GATE-SEC", 1, 10);

        assertThat(mapped).hasSize(171);
        assertThat(mapped).contains("GATE-COM-001", "GATE-PFX-024", "GATE-UP-020", "GATE-PROXY-041", "GATE-SEC-010");
    }

    @Test
    @DisplayName("GATE-COM, GATE-AUTH, and GATE-HEALTH cover envelopes, request ids, auth, and summary")
    void commonAuthHealthAndSummary() throws Exception {
        mvc.perform(get("/api/v1/gateway/health").header("X-Request-Id", "req-gateway-health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-gateway-health"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.service").value("api-gateway"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.port").value(8125))
                .andExpect(jsonPath("$.data.routesTotal").value(24))
                .andExpect(jsonPath("$.requestId").value("req-gateway-health"));

        JsonNode generated = performJson(get("/api/v1/gateway/health"), 200);
        assertThat(generated.at("/requestId").asText()).startsWith("req_");

        performJson(get("/api/v1/gateway/admin/ops/summary"), 401, 41000);
        performJson(get("/api/v1/gateway/admin/ops/summary").header("Authorization", "Token bad"), 401, 41003);
        performJson(get("/api/v1/gateway/admin/ops/summary").header("Authorization", bearer("user-token")), 403, 42001);

        for (String token : List.of("helper-token", "admin-token", "owner-token")) {
            JsonNode summary = performJson(get("/api/v1/gateway/admin/ops/summary").header("Authorization", bearer(token)), 200);
            assertThat(summary.at("/data/service").asText()).isEqualTo("api-gateway");
            assertThat(summary.at("/data/port").asInt()).isEqualTo(8125);
            assertThat(summary.at("/data/routesTotal").asInt()).isEqualTo(24);
            assertThat(summary.at("/data/productionGaps").toString()).contains("SERVICE_DISCOVERY_NOT_CONNECTED", "WEBSOCKET_PROXY_NOT_ENABLED");
            assertNoSecrets(summary);
        }

        performJson(get("/api/v1/gateway/admin/request-logs").header("Authorization", bearer("helper-token")), 403, 42001);
        performJson(get("/api/v1/gateway/admin/request-logs").header("Authorization", bearer("admin-token")), 200);
        performJson(get("/api/v1/gateway/admin/request-logs").header("Authorization", bearer("owner-token")), 200);

        performJson(get("/api/v1/gateway/admin/routes")
                .header("Authorization", bearer("helper-token"))
                .header("X-Request-Id", "req-admin-log"), 200);
        JsonNode adminLogs = performJson(get("/api/v1/gateway/admin/request-logs")
                .header("Authorization", bearer("admin-token"))
                .param("pageSize", "100"), 200);
        assertThat(adminLogs.at("/data/items").toString())
                .contains("\"requestId\":\"req-admin-log\"")
                .contains("\"method\":\"GET\"")
                .contains("\"path\":\"/api/v1/gateway/admin/routes\"")
                .contains("\"gatewayStatus\":200")
                .contains("\"result\":\"SUCCESS\"");
        assertNoSecrets(adminLogs);
    }

    @Test
    @DisplayName("GATE-ROUTE and GATE-PFX cover the read-only route registry")
    void routesMatchExistingServiceContracts() throws Exception {
        JsonNode routes = performJson(get("/api/v1/gateway/admin/routes")
                .header("Authorization", bearer("helper-token"))
                .param("pageSize", "100")
                .param("sort", "upstreamPort_asc"), 200);
        assertThat(routes.at("/data/total").asInt()).isEqualTo(24);
        assertRoute(routes, "auth", "AUTH", "/api/v1/auth", 8101);
        assertRoute(routes, "profile", "PROFILE", "/api/v1/profile", 8102);
        assertRoute(routes, "notification", "NOTIFICATION", "/api/v1/notifications", 8103);
        assertRoute(routes, "content", "CONTENT", "/api/v1/content", 8104);
        assertRoute(routes, "server-status", "SERVER_STATUS", "/api/v1/server-status", 8105);
        assertRoute(routes, "resource", "RESOURCE", "/api/v1/resources", 8106);
        assertRoute(routes, "admin", "ADMIN", "/api/v1/admin", 8107);
        assertRoute(routes, "onboarding", "ONBOARDING", "/api/v1/onboarding", 8108);
        assertRoute(routes, "exam", "EXAM", "/api/v1/exams", 8109);
        assertRoute(routes, "whitelist", "WHITELIST", "/api/v1/whitelist", 8110);
        assertRoute(routes, "attendance", "ATTENDANCE", "/api/v1/attendance", 8111);
        assertRoute(routes, "community", "COMMUNITY", "/api/v1/community", 8112);
        assertRoute(routes, "activity", "ACTIVITY", "/api/v1/activity", 8113);
        assertRoute(routes, "calendar", "CALENDAR", "/api/v1/calendar", 8114);
        assertRoute(routes, "changelog", "CHANGELOG", "/api/v1/changelog", 8115);
        assertRoute(routes, "ops-control", "OPS_CONTROL", "/api/v1/ops-control", 8116);
        assertRoute(routes, "node-daemon", "NODE_DAEMON", "/api/v1/node-daemon", 8117);
        assertRoute(routes, "cloudreve-sync", "CLOUDREVE_SYNC", "/api/v1/cloudreve-sync", 8118);
        assertRoute(routes, "backup-recovery", "BACKUP_RECOVERY", "/api/v1/backup-recovery", 8119);
        assertRoute(routes, "alerting", "ALERTING", "/api/v1/alerting", 8120);
        assertRoute(routes, "online-map", "ONLINE_MAP", "/api/v1/online-map", 8121);
        assertRoute(routes, "plugin-integration", "PLUGIN_INTEGRATION", "/api/v1/plugin-integration", 8122);
        assertRoute(routes, "cross-platform-notification", "CROSS_PLATFORM_NOTIFICATION", "/api/v1/cross-platform-notification", 8123);
        assertRoute(routes, "ops-image-market", "OPS_IMAGE_MARKET", "/api/v1/ops-image-market", 8124);

        JsonNode filtered = performJson(get("/api/v1/gateway/admin/routes")
                .header("Authorization", bearer("helper-token"))
                .param("keyword", "resources")
                .param("enabled", "true")
                .param("sort", "routeId_asc"), 200);
        assertThat(filtered.at("/data/items").toString()).contains("\"routeId\":\"resource\"");

        JsonNode detail = performJson(get("/api/v1/gateway/admin/routes/exam").header("Authorization", bearer("helper-token")), 200);
        assertThat(detail.at("/data/pathPrefix").asText()).isEqualTo("/api/v1/exams");
        performJson(get("/api/v1/gateway/admin/routes/BAD!").header("Authorization", bearer("helper-token")), 400, 40001);
        performJson(get("/api/v1/gateway/admin/routes/missing").header("Authorization", bearer("helper-token")), 404, 43000);
        performJson(get("/api/v1/gateway/admin/routes").header("Authorization", bearer("helper-token")).param("serviceKey", "NOPE"), 400, 46203);
        performJson(get("/api/v1/gateway/admin/routes").header("Authorization", bearer("helper-token")).param("enabled", "maybe"), 400, 46203);
        performJson(get("/api/v1/gateway/admin/routes").header("Authorization", bearer("helper-token")).param("sort", "bad"), 400, 46203);
        performJson(get("/api/v1/gateway/admin/routes").header("Authorization", bearer("helper-token")).param("page", "0"), 400, 46203);
    }

    @Test
    @DisplayName("GATE-UP covers upstream health snapshots and refresh state transitions")
    void upstreamHealthSnapshotsAndRefresh() throws Exception {
        JsonNode initial = performJson(get("/api/v1/gateway/admin/upstreams")
                .header("Authorization", bearer("helper-token"))
                .param("pageSize", "100"), 200);
        assertThat(initial.at("/data/total").asInt()).isEqualTo(24);
        assertThat(initial.at("/data/items/0/status").asText()).isEqualTo("UNKNOWN");

        fakeClient.respond("AUTH", 401, body(41000, "not logged in", null));
        JsonNode auth = performJson(post("/api/v1/gateway/admin/upstreams/AUTH/health-refresh")
                .header("Authorization", bearer("helper-token"))
                .header("X-Request-Id", "req-up-auth"), 200);
        assertThat(auth.at("/data/status").asText()).isEqualTo("UP");
        assertThat(fakeClient.lastRequest().headers().get("X-Request-Id")).containsExactly("req-up-auth");

        fakeClient.respond("CONTENT", 500, body(51600, "internal", null));
        JsonNode content = performJson(post("/api/v1/gateway/admin/upstreams/CONTENT/health-refresh")
                .header("Authorization", bearer("helper-token")), 200);
        assertThat(content.at("/data/status").asText()).isEqualTo("DEGRADED");

        fakeClient.failConnection("RESOURCE");
        JsonNode down = performJson(post("/api/v1/gateway/admin/upstreams/RESOURCE/health-refresh")
                .header("Authorization", bearer("helper-token")), 200);
        assertThat(down.at("/data/status").asText()).isEqualTo("DOWN");
        assertThat(down.at("/data/lastErrorCode").asInt()).isEqualTo(46210);

        fakeClient.failTimeout("SERVER_STATUS");
        JsonNode timeout = performJson(post("/api/v1/gateway/admin/upstreams/SERVER_STATUS/health-refresh")
                .header("Authorization", bearer("helper-token")), 200);
        assertThat(timeout.at("/data/status").asText()).isEqualTo("TIMEOUT");
        assertThat(timeout.at("/data/lastErrorCode").asInt()).isEqualTo(46211);

        JsonNode filtered = performJson(get("/api/v1/gateway/admin/upstreams")
                .header("Authorization", bearer("helper-token"))
                .param("status", "TIMEOUT")
                .param("serviceKey", "SERVER_STATUS")
                .param("sort", "lastCheckedAt_desc"), 200);
        assertThat(filtered.at("/data/items/0/serviceKey").asText()).isEqualTo("SERVER_STATUS");

        performJson(get("/api/v1/gateway/admin/upstreams").header("Authorization", bearer("helper-token")).param("status", "BAD"), 400, 46203);
        performJson(get("/api/v1/gateway/admin/upstreams").header("Authorization", bearer("helper-token")).param("serviceKey", "BAD"), 400, 46203);
        performJson(get("/api/v1/gateway/admin/upstreams").header("Authorization", bearer("helper-token")).param("sort", "bad"), 400, 46203);
        performJson(post("/api/v1/gateway/admin/upstreams/BAD/health-refresh").header("Authorization", bearer("helper-token")), 404, 43000);
    }

    @Test
    @DisplayName("GATE-PROXY covers forwarding, degradation, CORS, and sanitized request logs")
    void proxyForwardingDegradationCorsAndLogs() throws Exception {
        fakeClient.respond("AUTH", 200, Map.of("code", 0, "message", "success", "data", Map.of("ok", true)));
        MvcResult proxied = mvc.perform(post("/api/v1/auth/login?next=/profile&token=secret-query")
                        .header("Authorization", bearer("secret-upstream-token"))
                        .header("X-Request-Id", "req-proxy")
                        .header("Accept-Language", "zh-CN")
                        .header("Connection", "keep-alive")
                        .header("X-Beiming-Actor-User-Id", "forged-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"u\",\"password\":\"secret-body\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-proxy"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        assertThat(proxied.getResponse().getHeaders("X-Request-Id")).containsExactly("req-proxy");
        GatewayHttpRequest outbound = fakeClient.lastRequest();
        assertThat(outbound.method()).isEqualTo("POST");
        assertThat(outbound.path()).isEqualTo("/api/v1/auth/login");
        assertThat(outbound.query()).isEqualTo("next=/profile&token=secret-query");
        assertThat(outbound.body()).contains("secret-body");
        assertThat(outbound.headers().get("Authorization")).containsExactly(bearer("secret-upstream-token"));
        assertThat(outbound.headers()).doesNotContainKey("Connection");
        assertThat(outbound.headers()).doesNotContainKey("X-Beiming-Actor-User-Id");

        for (HttpMethod method : List.of(HttpMethod.GET, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)) {
            fakeClient.respond("AUTH", 200, Map.of("code", 0, "message", "success", "data", Map.of("method", method.name())));
            mvc.perform(request(method, "/api/v1/auth/method-check")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ok\":true}"))
                    .andExpect(status().isOk());
            assertThat(fakeClient.lastRequest().method()).isEqualTo(method.name());
        }

        fakeClient.respond("AUTH", 400, body(40001, "invalid request", null));
        performJson(get("/api/v1/auth/bad"), 400, 40001);
        fakeClient.respond("AUTH", 500, body(51100, "upstream failed", null));
        performJson(get("/api/v1/auth/fail"), 500, 51100);
        fakeClient.respond("AUTH", 599, body(59900, "non standard upstream failure", null));
        performJson(get("/api/v1/auth/non-standard"), 599, 59900);
        fakeClient.respondText("AUTH", 200, "text/plain", "plain-upstream");
        MvcResult text = mvc.perform(get("/api/v1/auth/plain")).andExpect(status().isOk()).andReturn();
        assertThat(text.getResponse().getContentAsString()).isEqualTo("plain-upstream");
        assertThat(text.getResponse().getContentType()).contains("text/plain");

        fakeClient.failConnection("AUTH");
        performJson(get("/api/v1/auth/down"), 502, 46210);
        fakeClient.failTimeout("AUTH");
        performJson(get("/api/v1/auth/slow"), 504, 46211);
        fakeClient.failInvalidResponse("AUTH");
        performJson(get("/api/v1/auth/broken-response"), 502, 46212);
        fakeClient.failInvalidUpstream("AUTH");
        performJson(get("/api/v1/auth/invalid-upstream"), 502, 46213);

        performJson(get("/api/v1/unknown/path"), 404, 46200);
        performJson(request(HttpMethod.TRACE, "/api/v1/auth/trace"), 405, 46201);
        performJson(get("/api/v1/resourceful"), 404, 46200);

        mvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type,X-Request-Id"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"))
                .andExpect(header().string("Access-Control-Expose-Headers", "X-Request-Id"));
        mvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://example.invalid")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

        JsonNode logs = performJson(get("/api/v1/gateway/admin/request-logs")
                .header("Authorization", bearer("admin-token"))
                .param("pageSize", "100")
                .param("routeId", "auth")
                .param("serviceKey", "AUTH")
                .param("sort", "createdAt_desc"), 200);
        assertThat(logs.at("/data/items").toString()).contains("\"routeId\":\"auth\"");
        assertNoSecrets(logs);
        assertThat(logs.toString()).doesNotContain("secret-upstream-token", "secret-query", "secret-body", "forged-user");

        performJson(get("/api/v1/gateway/admin/request-logs").header("Authorization", bearer("admin-token")).param("result", "BAD"), 400, 46203);
        performJson(get("/api/v1/gateway/admin/request-logs").header("Authorization", bearer("admin-token")).param("from", "bad-time"), 400, 46203);
        performJson(get("/api/v1/gateway/admin/request-logs").header("Authorization", bearer("admin-token"))
                .param("from", "2026-05-30T00:00:00Z").param("to", "2026-05-29T00:00:00Z"), 400, 46203);
        performJson(get("/api/v1/gateway/admin/request-logs").header("Authorization", bearer("admin-token")).param("sort", "bad"), 400, 46203);
    }

    @Test
    @DisplayName("GATE-PROXY maps invalid upstream route configuration to the contract error code")
    void invalidUpstreamConfigurationUsesDedicatedFailureType() {
        GatewayRoute invalidRoute = new GatewayRoute(
                "invalid",
                "INVALID",
                "invalid",
                "/api/v1/invalid",
                "http:// bad-host",
                8199,
                "/api/v1/invalid/health",
                50,
                true,
                List.of("GET"),
                true,
                Instant.parse("2026-05-29T00:00:00Z"),
                Instant.parse("2026-05-29T00:00:00Z")
        );
        GatewayHttpRequest request = new GatewayHttpRequest("GET", "/api/v1/invalid", null, Map.of(), "");

        try {
            new JavaGatewayHttpClient().exchange(invalidRoute, request);
            throw new AssertionError("expected invalid upstream failure");
        } catch (GatewayUpstreamException ex) {
            assertThat(ex.type()).isEqualTo(GatewayFailureType.INVALID_UPSTREAM);
        }
    }

    @TestConfiguration
    static class FakeClientConfig {
        @Bean
        @Primary
        FakeGatewayHttpClient fakeGatewayHttpClient(ObjectMapper objectMapper) {
            return new FakeGatewayHttpClient(objectMapper);
        }
    }

    static final class FakeGatewayHttpClient implements GatewayHttpClient {
        private final ObjectMapper objectMapper;
        private final Map<String, FakeResponse> responses = new LinkedHashMap<>();
        private final List<GatewayHttpRequest> requests = new ArrayList<>();

        FakeGatewayHttpClient(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        void reset() {
            responses.clear();
            requests.clear();
        }

        void respond(String serviceKey, int status, Object body) throws Exception {
            responses.put(serviceKey, new FakeResponse(status, "application/json", objectMapper.writeValueAsBytes(body), null));
        }

        void respondText(String serviceKey, int status, String contentType, String body) {
            responses.put(serviceKey, new FakeResponse(status, contentType, body.getBytes(StandardCharsets.UTF_8), null));
        }

        void failConnection(String serviceKey) {
            responses.put(serviceKey, new FakeResponse(0, null, new byte[0], GatewayFailureType.CONNECTION));
        }

        void failTimeout(String serviceKey) {
            responses.put(serviceKey, new FakeResponse(0, null, new byte[0], GatewayFailureType.TIMEOUT));
        }

        void failInvalidResponse(String serviceKey) {
            responses.put(serviceKey, new FakeResponse(0, null, new byte[0], GatewayFailureType.INVALID_RESPONSE));
        }

        void failInvalidUpstream(String serviceKey) {
            responses.put(serviceKey, new FakeResponse(0, null, new byte[0], GatewayFailureType.INVALID_UPSTREAM));
        }

        GatewayHttpRequest lastRequest() {
            return requests.get(requests.size() - 1);
        }

        @Override
        public GatewayHttpResponse exchange(GatewayRoute route, GatewayHttpRequest request) {
            requests.add(request);
            FakeResponse response = responses.getOrDefault(route.serviceKey(), new FakeResponse(200, "application/json",
                    "{\"code\":0,\"message\":\"success\",\"data\":{\"default\":true}}".getBytes(StandardCharsets.UTF_8), null));
            if (response.failureType == GatewayFailureType.CONNECTION) {
                throw GatewayUpstreamException.connection("connection failed");
            }
            if (response.failureType == GatewayFailureType.TIMEOUT) {
                throw GatewayUpstreamException.timeout("timeout");
            }
            if (response.failureType == GatewayFailureType.INVALID_RESPONSE) {
                throw GatewayUpstreamException.invalidResponse("invalid upstream response");
            }
            if (response.failureType == GatewayFailureType.INVALID_UPSTREAM) {
                throw GatewayUpstreamException.invalidUpstream("upstream address invalid");
            }
            return new GatewayHttpResponse(response.status, response.contentType, response.body, Map.of());
        }

        private record FakeResponse(int status, String contentType, byte[] body, GatewayFailureType failureType) {
        }
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, int status) throws Exception {
        MvcResult result = mvc.perform(builder.accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, int status, int code) throws Exception {
        JsonNode json = performJson(builder, status);
        assertThat(json.path("code").asInt()).isEqualTo(code);
        if (json.has("requestId")) {
            assertThat(json.path("requestId").asText()).isNotBlank();
        }
        return json;
    }

    private void assertRoute(JsonNode routes, String routeId, String serviceKey, String prefix, int port) {
        JsonNode match = findRoute(routes, routeId);
        assertThat(match.path("serviceKey").asText()).isEqualTo(serviceKey);
        assertThat(match.path("pathPrefix").asText()).isEqualTo(prefix);
        assertThat(match.path("upstreamPort").asInt()).isEqualTo(port);
        assertThat(match.path("authDelegated").asBoolean()).isTrue();
    }

    private JsonNode findRoute(JsonNode routes, String routeId) {
        for (JsonNode item : routes.at("/data/items")) {
            if (routeId.equals(item.path("routeId").asText())) {
                return item;
            }
        }
        throw new AssertionError("missing route " + routeId);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void addRange(Set<String> ids, String prefix, int first, int last) {
        for (int i = first; i <= last; i++) {
            ids.add(prefix + "-" + String.format("%03d", i));
        }
    }

    private Map<String, Object> body(int code, String message, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", data);
        return body;
    }

    private void assertNoSecrets(JsonNode json) {
        assertThat(json.toString()).doesNotContain(
                "Authorization", "Cookie", "secret-upstream-token", "secret-query", "secret-body",
                "password", "rawToken", "nodeToken", "registryToken", "Cloudreve", "webhook",
                "stackTrace", "Exception", "X-Beiming-Actor", "X-Gateway-Internal", "id_rsa", ".env");
    }
}
