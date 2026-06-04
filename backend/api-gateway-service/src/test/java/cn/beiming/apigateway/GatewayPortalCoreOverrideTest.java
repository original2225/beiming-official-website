package cn.beiming.apigateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = ApiGatewayServiceApplication.class,
        properties = "api-gateway.upstreams.portal-core-base-url=http://127.0.0.1:19034"
)
@AutoConfigureMockMvc
class GatewayPortalCoreOverrideTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("GATE-PCORE-011 overrides only portal-core merged upstreams for real HTTP smoke")
    void overridesOnlyPortalCoreMergedUpstreams() throws Exception {
        JsonNode routes = performJson(get("/api/v1/gateway/admin/routes")
                .header("Authorization", "Bearer helper-token")
                .param("pageSize", "100"));

        assertRoute(routes, "material", "MATERIAL", "/api/v1/materials", "http://127.0.0.1:19034", 19034);
        assertRoute(routes, "guide", "GUIDE", "/api/v1/guides", "http://127.0.0.1:19034", 19034);
        assertRoute(routes, "online-map", "ONLINE_MAP", "/api/v1/online-map", "http://127.0.0.1:8121", 8121);
        assertRoute(routes, "cross-platform-notification", "CROSS_PLATFORM_NOTIFICATION", "/api/v1/cross-platform-notification", "http://127.0.0.1:8123", 8123);
        assertRoute(routes, "node-daemon", "NODE_DAEMON", "/api/v1/node-daemon", "http://127.0.0.1:8117", 8117);
        assertThat(routes.at("/data/items").toString()).doesNotContain("\"serviceKey\":\"PORTAL_CORE\"");
    }

    private JsonNode performJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) throws Exception {
        MvcResult result = mvc.perform(builder.accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void assertRoute(JsonNode routes, String routeId, String serviceKey, String pathPrefix, String upstreamBaseUrl, int upstreamPort) {
        JsonNode route = findRoute(routes, routeId);
        assertThat(route.path("serviceKey").asText()).isEqualTo(serviceKey);
        assertThat(route.path("pathPrefix").asText()).isEqualTo(pathPrefix);
        assertThat(route.path("upstreamBaseUrl").asText()).isEqualTo(upstreamBaseUrl);
        assertThat(route.path("upstreamPort").asInt()).isEqualTo(upstreamPort);
    }

    private JsonNode findRoute(JsonNode routes, String routeId) {
        for (JsonNode item : routes.at("/data/items")) {
            if (routeId.equals(item.path("routeId").asText())) {
                return item;
            }
        }
        throw new AssertionError("missing route " + routeId);
    }
}
