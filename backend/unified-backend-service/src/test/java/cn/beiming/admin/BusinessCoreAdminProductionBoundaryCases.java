package cn.beiming.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
abstract class BusinessCoreAdminProductionBoundaryCases {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("ADM-PROD trusted gateway actor context authenticates and trims by permissions")
    void trustedGatewayContextAuthenticatesAndTrimsByPermissions() throws Exception {
        JsonNode overview = performJson(trusted(get("/api/v1/admin/overview"), "gateway-admin", "ADMIN", "NODE_READ"), 200);
        assertThat(values(overview.at("/data/modules"), "moduleKey")).contains("ONLINE_MAP").doesNotContain("OPS_CONTROL", "NODE_DAEMON");

        JsonNode onlineMap = performJson(trusted(get("/api/v1/admin/modules/ONLINE_MAP"), "gateway-admin", "ADMIN", "NODE_READ"), 200);
        assertThat(onlineMap.at("/data/moduleKey").asText()).isEqualTo("ONLINE_MAP");

        performJson(trusted(get("/api/v1/admin/modules/ONLINE_MAP"), "gateway-admin", "ADMIN", ""), 403, 42002);
        performJson(trusted(get("/api/v1/admin/overview").header("Authorization", "Bearer owner-token"), "gateway-user", "USER", "NODE_READ"), 403, 42001);
    }

    @Test
    @DisplayName("ADM-PROD malformed trusted gateway actor context does not fall back to local tokens")
    void malformedTrustedGatewayContextDoesNotFallbackToLocalTokens() throws Exception {
        performJson(get("/api/v1/admin/overview")
                .header("Authorization", "Bearer owner-token")
                .header("X-Beiming-Actor-Roles", "OWNER"), 502, 46703);
        performJson(get("/api/v1/admin/overview")
                .header("Authorization", "Bearer owner-token")
                .header("X-Beiming-Actor-User-Id", "gateway-admin")
                .header("X-Beiming-Actor-Roles", "ROOT"), 502, 46702);
        performJson(get("/api/v1/admin/overview")
                .header("Authorization", "Bearer owner-token")
                .header("X-Beiming-Actor-User-Id", "gateway-admin")
                .header("X-Beiming-Actor-Roles", " "), 502, 46703);
    }

    @Test
    @DisplayName("ADM-PROD production mode rejects X-Test degradation and failure hooks")
    void productionModeRejectsTestHooks() throws Exception {
        performJson(trusted(get("/api/v1/admin/overview")
                .header("X-Test-Module-Mode", "CONTENT:UNAVAILABLE")
                .header("X-Test-Platform-Mode", "API_GATEWAY:UNAVAILABLE"), "gateway-owner", "OWNER", "NODE_READ"), 400, 51735);

        JsonNode rejected = performJson(trusted(patch("/api/v1/admin/settings")
                .header("X-Test-Fail-Audit", "true")
                .header("X-Test-Fail-Settings", "true"), "gateway-admin", "ADMIN", "NODE_READ"), settingsPatchBody("prod-safe-1"), 400);
        assertThat(rejected.at("/code").asInt()).isEqualTo(51735);
    }

    @Test
    @DisplayName("ADM-PROD ops summary reports production test mode boundary and trusted context auth mode")
    void opsSummaryReportsProductionBoundary() throws Exception {
        JsonNode ops = performJson(trusted(get("/api/v1/admin/ops/summary")
                .header("X-Request-Id", "req-prod-boundary")
                .header("X-Gateway-Internal-Request-Id", "gw-internal-1"), "gateway-admin", "ADMIN", "NODE_READ"), 200);
        assertThat(ops.at("/data/testMode").asBoolean()).isFalse();
        assertThat(ops.at("/data/authMode").asText()).isEqualTo("TRUSTED_GATEWAY_CONTEXT");
        assertThat(ops.toString()).doesNotContain("gw-internal-1");

        MvcResult result = mvc.perform(trusted(get("/api/v1/admin/ops/summary")
                        .header("X-Request-Id", "req-prod-boundary-2"), "gateway-admin", "ADMIN", "NODE_READ"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-prod-boundary-2"))
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("X-Gateway-Internal-Request-Id");
    }

    private MockHttpServletRequestBuilder trusted(MockHttpServletRequestBuilder request, String userId, String roles, String permissions) {
        return request
                .header("X-Beiming-Actor-User-Id", userId)
                .header("X-Beiming-Actor-Roles", roles)
                .header("X-Beiming-Actor-Permissions", permissions);
    }

    private JsonNode performJson(MockHttpServletRequestBuilder request, int status) throws Exception {
        MvcResult result = mvc.perform(request.accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performJson(MockHttpServletRequestBuilder request, int status, int code) throws Exception {
        JsonNode json = performJson(request, status);
        assertThat(json.at("/code").asInt()).isEqualTo(code);
        return json;
    }

    private JsonNode performJson(MockHttpServletRequestBuilder request, Map<String, Object> body, int status) throws Exception {
        MvcResult result = mvc.perform(request
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private Map<String, Object> settingsPatchBody(String idempotencyKey) {
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("navigationModuleOrder", List.of("AUTH", "ADMIN", "CONTENT"));
        layout.put("quickActions", List.of(Map.of("key", "content-review", "targetRoute", "/admin/content")));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("layout", layout);
        body.put("items", List.of(Map.of("key", "dashboard.refreshSeconds", "value", 45)));
        body.put("reason", "production boundary test");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private List<String> values(JsonNode array, String field) {
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        if (array.isArray()) {
            for (JsonNode node : array) {
                values.add(node.path(field).asText());
            }
        }
        return values;
    }
}
