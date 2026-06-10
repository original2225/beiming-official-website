package cn.beiming.unifiedbackend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = UnifiedBackendServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UnifiedBackendRealHttpRehearsalTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void runsRealHttpRehearsalAcrossCandidateEntrypointWithoutLeakingSensitiveFields() throws Exception {
        List<HttpTarget> targets = List.of(
                target("/api/v1/unified-backend/health"),
                target("/api/v1/gateway/health"),
                target("/api/v1/business-core/health"),
                target("/api/v1/admission-core/health"),
                target("/api/v1/engagement-core/health"),
                target("/api/v1/ops-core/health"),
                target("/api/v1/portal-core/health"),
                target("/api/v1/auth/session/verify", 41000, null),
                target("/api/v1/profile/members"),
                target("/api/v1/notifications/me/unread-count", 0, "Bearer user-token"),
                target("/api/v1/content/home"),
                target("/api/v1/server-status/overview"),
                target("/api/v1/resources"),
                target("/api/v1/admin/overview", 0, "Bearer helper-token"),
                target("/api/v1/onboarding/me/progress", 41000, null),
                target("/api/v1/exams/me/sessions/current", 41000, null),
                target("/api/v1/whitelist/me/applications/current", 41000, null),
                target("/api/v1/attendance/leaderboard"),
                target("/api/v1/community/boards"),
                target("/api/v1/activity/events"),
                target("/api/v1/calendar/upcoming"),
                target("/api/v1/changelog/versions/latest"),
                target("/api/v1/ops-control/overview", 0, "Bearer owner-token"),
                target("/api/v1/cloudreve-sync/health"),
                target("/api/v1/backup-recovery/health"),
                target("/api/v1/alerting/health"),
                target("/api/v1/plugin-integration/health"),
                target("/api/v1/cross-platform-notification/health"),
                target("/api/v1/ops-image-market/health"),
                target("/api/v1/guides/categories"),
                target("/api/v1/materials/featured"),
                target("/api/v1/online-map/health")
        );

        for (HttpTarget target : targets) {
            JsonNode response = getMaybeAuthorized(target.path(), target.authorization());
            assertThat(response.at("/code").asInt()).as(target.path()).isEqualTo(target.expectedCode());
            assertNoSensitiveFields(response);
        }

        JsonNode authFailure = get("/api/v1/unified-backend/admin/readiness");
        assertThat(authFailure.at("/code").asInt()).isEqualTo(41000);
        assertNoSensitiveFields(authFailure);
    }

    @Test
    void scansGatewayRoutesAgainstUnifiedMountsWithoutRouteDrift() throws Exception {
        JsonNode gatewayRoutes = getAdmin("/api/v1/gateway/admin/routes?pageSize=100&sort=routeId_asc");
        JsonNode unifiedMounts = getAdmin("/api/v1/unified-backend/admin/mounts");

        Map<String, JsonNode> gatewayById = itemsByRouteId(gatewayRoutes.at("/data/items"));
        Map<String, JsonNode> unifiedById = itemsByRouteId(unifiedMounts.at("/data/items"));
        assertThat(gatewayById).hasSize(25);
        assertThat(gatewayById).doesNotContainKey("node-daemon");

        for (Map.Entry<String, JsonNode> gateway : gatewayById.entrySet()) {
            JsonNode unified = unifiedById.get(gateway.getKey());
            assertThat(unified).as(gateway.getKey()).isNotNull();
            assertThat(unified.path("serviceKey").asText()).isEqualTo(gateway.getValue().path("serviceKey").asText());
            assertThat(unified.path("pathPrefix").asText()).isEqualTo(gateway.getValue().path("pathPrefix").asText());
            assertThat(unified.path("mountDisposition").asText()).isEqualTo("IN_PROCESS");
            assertThat(unified.path("candidatePort").asInt()).isEqualTo(8135);
        }

        assertThat(unifiedMounts.at("/data/items").toString())
                .doesNotContain("HTTP_UPSTREAM_FALLBACK", "node-daemon", "KEEP_EXTERNAL", "8117");
        JsonNode malformedAuth = getWithAuth("/api/v1/unified-backend/admin/mounts", "Token bad");
        assertThat(malformedAuth.at("/code").asInt()).isEqualTo(41003);
        assertNoSensitiveFields(gatewayRoutes);
        assertNoSensitiveFields(unifiedMounts);
    }

    @Test
    void realHttpCutoverTargetsMatchGatewayBusinessSurface() throws Exception {
        JsonNode readiness = getAdmin("/api/v1/unified-backend/admin/readiness");
        assertThat(readiness.at("/data/entrypointCutoverExecutionEvidence/candidateBaseUrl").asText())
                .isEqualTo("http://127.0.0.1:8135");
        assertThat(readiness.at("/data/entrypointCutoverExecutionEvidence/currentGatewayBaseUrl").asText())
                .isEqualTo("http://127.0.0.1:8125");
        assertThat(readiness.at("/data/readyToReplaceGateway").asBoolean()).isFalse();

        JsonNode gatewayRoutes = getAdmin("/api/v1/gateway/admin/routes?pageSize=100&sort=routeId_asc");
        JsonNode unifiedMounts = getAdmin("/api/v1/unified-backend/admin/mounts");

        Map<String, JsonNode> gatewayById = itemsByRouteId(gatewayRoutes.at("/data/items"));
        Map<String, JsonNode> unifiedById = itemsByRouteId(unifiedMounts.at("/data/items"));
        assertThat(gatewayById).hasSize(25);

        for (Map.Entry<String, JsonNode> gateway : gatewayById.entrySet()) {
            JsonNode unified = unifiedById.get(gateway.getKey());
            assertThat(unified).as(gateway.getKey()).isNotNull();
            assertThat(unified.path("pathPrefix").asText()).isEqualTo(gateway.getValue().path("pathPrefix").asText());
            assertThat(unified.path("mountDisposition").asText()).isEqualTo("IN_PROCESS");
            assertThat(unified.path("currentPort").asInt()).isEqualTo(gateway.getValue().path("upstreamPort").asInt());
            assertThat(unified.path("candidatePort").asInt()).isEqualTo(8135);

            JsonNode response = getMaybeAuthorized(gateway.getValue().path("healthCheckPath").asText(), authorizationFor(gateway.getKey()));
            assertThat(response.at("/code").asInt()).as(gateway.getKey()).isIn(0, 41000);
            assertNoSensitiveFields(response);
        }

        assertThat(unifiedMounts.toString())
                .doesNotContain("/api/v1/unified-backend/auth")
                .doesNotContain("HTTP_UPSTREAM_FALLBACK")
                .doesNotContain("node-daemon")
                .doesNotContain("8117");
        assertNoSensitiveFields(gatewayRoutes);
        assertNoSensitiveFields(unifiedMounts);
    }

    private JsonNode get(String path) throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(url(path), String.class);
        assertThat(response.getBody()).isNotBlank();
        return objectMapper.readTree(response.getBody());
    }

    private JsonNode getMaybeAuthorized(String path, String authorization) throws Exception {
        if (authorization == null) {
            return get(path);
        }
        return getWithAuth(path, authorization);
    }

    private JsonNode getAdmin(String path) throws Exception {
        return getWithAuth(path, "Bearer admin-token");
    }

    private JsonNode getWithAuth(String path, String authorization) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        ResponseEntity<String> response = restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getBody()).isNotBlank();
        return objectMapper.readTree(response.getBody());
    }

    private String url(String path) {
        return "http://127.0.0.1:" + port + path;
    }

    private Map<String, JsonNode> itemsByRouteId(JsonNode items) {
        Map<String, JsonNode> byId = new LinkedHashMap<>();
        Iterator<JsonNode> iterator = items.elements();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            byId.put(item.path("routeId").asText(), item);
        }
        return byId;
    }

    private String authorizationFor(String routeId) {
        return switch (routeId) {
            case "notification" -> "Bearer user-token";
            case "admin" -> "Bearer helper-token";
            case "ops-control" -> "Bearer owner-token";
            default -> null;
        };
    }

    private void assertNoSensitiveFields(JsonNode node) {
        String text = node.toString().toLowerCase();
        assertThat(text)
                .doesNotContain("authorization")
                .doesNotContain("cookie")
                .doesNotContain("secret")
                .doesNotContain("stacktrace")
                .doesNotContain("c:\\users\\");
    }

    private HttpTarget target(String path) {
        return target(path, 0, null);
    }

    private HttpTarget target(String path, int expectedCode, String authorization) {
        return new HttpTarget(path, expectedCode, authorization);
    }

    private record HttpTarget(String path, int expectedCode, String authorization) {
    }
}
