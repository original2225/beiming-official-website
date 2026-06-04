package cn.beiming.portalcore;

import cn.beiming.apigateway.ApiGatewayServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PortalCoreRealHttpSmokeIntegrationTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void runsPortalCoreSmokeThroughRealGatewayAndRealPortalCoreProcesses() throws Exception {
        int portalPort = freePort();
        int gatewayPort = freePort();

        try (ConfigurableApplicationContext portal = startPortalCore(portalPort, gatewayPort);
             ConfigurableApplicationContext gateway = startGateway(gatewayPort, portalPort)) {
            waitUntilOk("http://127.0.0.1:" + portalPort + "/api/v1/portal-core/health");
            waitUntilOk("http://127.0.0.1:" + gatewayPort + "/api/v1/gateway/health");

            JsonNode report = postJson(
                    "http://127.0.0.1:" + portalPort + "/api/v1/portal-core/admin/http-smoke/run",
                    Map.of("Authorization", "Bearer admin-token", "X-Request-Id", "req-real-http-smoke")
            );

            assertThat(report.at("/code").asInt()).isEqualTo(0);
            assertThat(report.at("/data/httpSmokeStatus").asText()).isEqualTo("PASS");
            assertThat(report.at("/data/results")).hasSize(2);
            assertThat(report.at("/data/results").toString())
                    .contains("\"targetKey\":\"GATEWAY_GUIDE_CATEGORIES\"")
                    .contains("\"targetKey\":\"GATEWAY_MATERIAL_FEATURED\"")
                    .contains("\"status\":\"PASS\"")
                    .contains("\"businessCode\":0");

            JsonNode readiness = getJson(
                    "http://127.0.0.1:" + portalPort + "/api/v1/portal-core/admin/readiness",
                    Map.of("Authorization", "Bearer admin-token")
            );
            assertThat(readiness.at("/data/httpSmokeStatus").asText()).isEqualTo("PASS");
            assertThat(readiness.at("/data/readyForProduction").asBoolean()).isFalse();
            assertThat(readiness.at("/data/checks").toString())
                    .contains("\"checkKey\":\"REAL_HTTP_SMOKE\",\"status\":\"PASS\"")
                    .contains("\"checkKey\":\"REAL_PERSISTENCE\",\"status\":\"BLOCKED\"")
                    .contains("\"checkKey\":\"REAL_AUDIT_PERSISTENCE\",\"status\":\"BLOCKED\"")
                    .contains("\"checkKey\":\"REAL_OBJECT_STORAGE\",\"status\":\"BLOCKED\"")
                    .contains("\"checkKey\":\"REAL_FILE_SECURITY_SCANNER\",\"status\":\"BLOCKED\"")
                    .contains("\"checkKey\":\"REAL_FULLTEXT_SEARCH\",\"status\":\"BLOCKED\"")
                    .contains("\"checkKey\":\"REAL_NOTIFICATION_DELIVERY\",\"status\":\"BLOCKED\"");
        }
    }

    private ConfigurableApplicationContext startPortalCore(int portalPort, int gatewayPort) {
        return new SpringApplicationBuilder(PortalCoreServiceApplication.class)
                .web(WebApplicationType.SERVLET)
                .run(
                        "--server.port=" + portalPort,
                        "--portal-core.http-smoke.gateway-base-url=http://127.0.0.1:" + gatewayPort,
                        "--portal-core.http-smoke.timeout-ms=2000"
                );
    }

    private ConfigurableApplicationContext startGateway(int gatewayPort, int portalPort) {
        return new SpringApplicationBuilder(ApiGatewayServiceApplication.class)
                .web(WebApplicationType.SERVLET)
                .run(
                        "--server.port=" + gatewayPort,
                        "--api-gateway.upstreams.portal-core-base-url=http://127.0.0.1:" + portalPort
                );
    }

    private JsonNode getJson(String url, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET();
        headers.forEach(builder::header);
        HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).isLessThan(500);
        return OBJECT_MAPPER.readTree(response.body());
    }

    private JsonNode postJson(String url, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .POST(HttpRequest.BodyPublishers.noBody());
        headers.forEach(builder::header);
        HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).isEqualTo(200);
        return OBJECT_MAPPER.readTree(response.body());
    }

    private void waitUntilOk(String url) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        Exception last = null;
        while (System.nanoTime() < deadline) {
            try {
                JsonNode body = getJson(url, Map.of());
                if (body.at("/code").asInt(-1) == 0) {
                    return;
                }
            } catch (Exception ex) {
                last = ex;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("service did not become ready: " + url, last);
    }

    private int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

}
