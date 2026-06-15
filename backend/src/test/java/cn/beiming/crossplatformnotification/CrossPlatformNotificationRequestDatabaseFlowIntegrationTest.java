package cn.beiming.crossplatformnotification;

import cn.beiming.opscore.OpsCoreServiceApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = OpsCoreServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "cross-platform-notification.test-controls.enabled=true"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CrossPlatformNotificationRequestDatabaseFlowIntegrationTest {
    private static final String FLOW_ID = "cross-platform-notification-flow-" + UUID.randomUUID();
    private static final String DB_URL = "jdbc:h2:mem:cross_platform_notification_flow_evidence;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            createEvidenceTables(statement);
            for (String table : List.of(
                    "cpn_flow_request_log",
                    "cpn_flow_providers",
                    "cpn_flow_capabilities",
                    "cpn_flow_mappings",
                    "cpn_flow_routes",
                    "cpn_flow_deliveries",
                    "cpn_flow_attempts",
                    "cpn_flow_receivers",
                    "cpn_flow_audits"
            )) {
                deleteFlowRows(statement, table);
            }
        }
    }

    @Test
    void createProviderRunsThroughBackendAndDatabaseThenReturnsDraftProvider() throws Exception {
        String requestId = "req-cpn-provider-" + FLOW_ID;

        TestHttpResponse response = exchange(
                HttpMethod.POST,
                "/api/v1/cross-platform-notification/admin/providers",
                bearerHeaders("cpn-admin-token", requestId),
                with(providerBody("provider-" + randomKey()), "confirmText", "REGISTER_EXTERNAL_PROVIDER")
        );

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.at("/code").asInt()).isZero();
        assertThat(json.at("/data/status").asText()).isEqualTo("DRAFT");
        assertThat(json.at("/data/createdBy").asText()).isEqualTo("user-cpn-admin");
        String providerId = json.at("/data/providerId").asText();

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM cpn_flow_providers WHERE flow_id = ? AND provider_id = ? AND action = 'EXTERNAL_PROVIDER_CREATED'",
                    FLOW_ID, providerId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT created_by FROM cpn_flow_providers WHERE flow_id = ? AND provider_id = ? AND action = 'EXTERNAL_PROVIDER_CREATED'",
                    FLOW_ID, providerId, "user-cpn-admin");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM cpn_flow_capabilities WHERE flow_id = ? AND provider_id = ?",
                    FLOW_ID, providerId, 1L);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM cpn_flow_audits WHERE flow_id = ? AND request_id = ? AND action = 'EXTERNAL_PROVIDER_CREATED'",
                    FLOW_ID, requestId, 1L);
            assertSingleValue(connection,
                    "SELECT response_code FROM cpn_flow_request_log WHERE flow_id = ? AND request_id = ? AND path = '/api/v1/cross-platform-notification/admin/providers'",
                    FLOW_ID, requestId, 201);
        }
        System.out.println("SQL evidence: cross-platform-notification provider create reached backend, wrote provider/capability/audit/request rows, and returned 201.");
    }

    @Test
    void templateRouteTestDeliveryAndCancelRunThroughBackendAndDatabaseThenReturnSimulatedDelivery() throws Exception {
        JsonNode mapping = requestJson(HttpMethod.POST,
                "/api/v1/cross-platform-notification/admin/template-mappings",
                bearerHeaders("cpn-admin-token", "req-cpn-template-" + FLOW_ID),
                templateBody("template-" + randomKey()),
                201);
        String mappingId = mapping.at("/data/mappingId").asText();

        JsonNode mappingEnabled = requestJson(HttpMethod.PATCH,
                "/api/v1/cross-platform-notification/admin/template-mappings/" + mappingId + "/enable",
                bearerHeaders("cpn-admin-token", "req-cpn-template-enable-" + FLOW_ID),
                Map.of("reason", "启用模板映射", "idempotencyKey", "enable-template-" + randomKey()),
                200);
        assertThat(mappingEnabled.at("/data/status").asText()).isEqualTo("ENABLED");

        JsonNode route = requestJson(HttpMethod.POST,
                "/api/v1/cross-platform-notification/admin/routes",
                bearerHeaders("cpn-admin-token", "req-cpn-route-" + FLOW_ID),
                with(routeBody("route-" + randomKey(), mappingId), "confirmText", "CONFIGURE_EXTERNAL_ROUTE"),
                201);
        String routeId = route.at("/data/routeId").asText();

        JsonNode routeEnabled = requestJson(HttpMethod.PATCH,
                "/api/v1/cross-platform-notification/admin/routes/" + routeId + "/enable",
                bearerHeaders("cpn-admin-token", "req-cpn-route-enable-" + FLOW_ID),
                Map.of("confirmText", "ENABLE_EXTERNAL_ROUTE", "reason", "启用路由", "idempotencyKey", "enable-route-" + randomKey()),
                200);
        assertThat(routeEnabled.at("/data/status").asText()).isEqualTo("ENABLED");

        JsonNode routeTest = requestJson(HttpMethod.POST,
                "/api/v1/cross-platform-notification/admin/routes/" + routeId + "/test",
                bearerHeaders("cpn-admin-token", "req-cpn-route-test-" + FLOW_ID),
                Map.of("samplePayloadSummary", Map.of("title", "Alert", "body", "Server degraded"),
                        "sampleReceiverSummary", Map.of("receiverType", "CHANNEL", "targetRefSummary", "#ops"),
                        "dryRun", false, "confirmText", "TEST_EXTERNAL_ROUTE", "reason", "测试路由", "idempotencyKey", "test-" + randomKey()),
                201);
        String testDeliveryId = routeTest.at("/data/delivery/deliveryId").asText();

        HttpHeaders failedHeaders = bearerHeaders("cpn-admin-token", "req-cpn-delivery-failed-" + FLOW_ID);
        failedHeaders.set("X-Test-Provider-Mode", "failed");
        JsonNode failedDelivery = requestJson(HttpMethod.POST,
                "/api/v1/cross-platform-notification/admin/deliveries",
                failedHeaders,
                deliveryBody("delivery-failed-" + randomKey(), routeId, mappingId),
                201);
        String failedDeliveryId = failedDelivery.at("/data/deliveryId").asText();
        assertThat(failedDelivery.at("/data/status").asText()).isEqualTo("SIMULATED_FAILED");

        JsonNode retried = requestJson(HttpMethod.PATCH,
                "/api/v1/cross-platform-notification/admin/deliveries/" + failedDeliveryId + "/retry",
                bearerHeaders("cpn-admin-token", "req-cpn-delivery-retry-" + FLOW_ID),
                Map.of("confirmText", "RETRY_EXTERNAL_DELIVERY", "reason", "重试投递", "idempotencyKey", "retry-" + randomKey()),
                200);
        assertThat(retried.at("/data/status").asText()).isEqualTo("SIMULATED_SENT");

        HttpHeaders scheduledHeaders = bearerHeaders("cpn-admin-token", "req-cpn-delivery-scheduled-" + FLOW_ID);
        scheduledHeaders.set("X-Test-Provider-Mode", "rate-limited");
        JsonNode scheduledDelivery = requestJson(HttpMethod.POST,
                "/api/v1/cross-platform-notification/admin/deliveries",
                scheduledHeaders,
                deliveryBody("delivery-scheduled-" + randomKey(), routeId, mappingId),
                201);
        String scheduledDeliveryId = scheduledDelivery.at("/data/deliveryId").asText();
        assertThat(scheduledDelivery.at("/data/status").asText()).isEqualTo("RETRY_SCHEDULED");

        JsonNode cancelled = requestJson(HttpMethod.PATCH,
                "/api/v1/cross-platform-notification/admin/deliveries/" + scheduledDeliveryId + "/cancel",
                bearerHeaders("cpn-admin-token", "req-cpn-delivery-cancel-" + FLOW_ID),
                Map.of("reason", "取消排队投递", "idempotencyKey", "cancel-" + randomKey()),
                200);
        assertThat(cancelled.at("/data/status").asText()).isEqualTo("CANCELED");

        try (Connection connection = openConnection()) {
            assertSingleValue(connection,
                    "SELECT status FROM cpn_flow_mappings WHERE flow_id = ? AND mapping_id = ? AND action = 'EXTERNAL_TEMPLATE_MAPPING_CREATED'",
                    FLOW_ID, mappingId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT status FROM cpn_flow_routes WHERE flow_id = ? AND route_id = ? AND action = 'EXTERNAL_ROUTE_CREATED'",
                    FLOW_ID, routeId, "DRAFT");
            assertSingleValue(connection,
                    "SELECT status FROM cpn_flow_deliveries WHERE flow_id = ? AND delivery_id = ? AND action = 'EXTERNAL_ROUTE_TESTED'",
                    FLOW_ID, testDeliveryId, "SIMULATED_SENT");
            assertSingleValue(connection,
                    "SELECT status FROM cpn_flow_deliveries WHERE flow_id = ? AND delivery_id = ? AND action = 'EXTERNAL_DELIVERY_RETRIED'",
                    FLOW_ID, failedDeliveryId, "SIMULATED_SENT");
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM cpn_flow_attempts WHERE flow_id = ? AND delivery_id = ?",
                    FLOW_ID, failedDeliveryId, 2L);
            assertSingleValue(connection,
                    "SELECT COUNT(*) >= 3 FROM cpn_flow_receivers WHERE flow_id = ? AND provider_id = ?",
                    FLOW_ID, "provider-discord-main", true);
            assertSingleValue(connection,
                    "SELECT COUNT(*) FROM cpn_flow_audits WHERE flow_id = ? AND target_id = ? AND action = 'EXTERNAL_DELIVERY_CANCELED'",
                    FLOW_ID, scheduledDeliveryId, 1L);
        }
        System.out.println("SQL evidence: cross-platform-notification template, route, delivery, attempt, receiver, and audit writes reached backend, and returned 200/201.");
    }

    @Test
    void readModelsAndHardeningRemainIntactAfterDatabaseWrites() throws Exception {
        requestJson(HttpMethod.GET,
                "/api/v1/cross-platform-notification/admin/ops/summary",
                bearerHeaders("cpn-viewer-token", "req-cpn-summary-" + FLOW_ID),
                200);
        requestJson(HttpMethod.GET,
                "/api/v1/cross-platform-notification/admin/providers",
                bearerHeaders("cpn-viewer-token", "req-cpn-providers-" + FLOW_ID),
                200);
        requestJson(HttpMethod.GET,
                "/api/v1/cross-platform-notification/admin/capabilities",
                bearerHeaders("cpn-viewer-token", "req-cpn-capabilities-" + FLOW_ID),
                200);
        requestJson(HttpMethod.GET,
                "/api/v1/cross-platform-notification/admin/audit-logs",
                bearerHeaders("cpn-admin-token", "req-cpn-audit-" + FLOW_ID),
                200);
        HttpHeaders failureHeaders = bearerHeaders("cpn-admin-token", "req-cpn-hardening-" + FLOW_ID);
        failureHeaders.set("X-Test-Fail-Delivery", "true");
        JsonNode failure = requestJson(HttpMethod.POST,
                "/api/v1/cross-platform-notification/admin/deliveries",
                failureHeaders,
                deliveryBody("delivery-hardening-" + randomKey(), "route-alerting-discord-main", "mapping-notification-discord-main"),
                500);
        assertThat(failure.at("/code").asInt()).isIn(55800, 55803);
    }

    private TestHttpResponse exchange(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method.name(), publisher);
        headers.forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
        HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new TestHttpResponse(response.statusCode(), response.body());
    }

    private JsonNode requestJson(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body, int status) throws Exception {
        TestHttpResponse response = exchange(method, path, headers, body);
        assertThat(response.statusCode()).isEqualTo(status);
        return objectMapper.readTree(response.body());
    }

    private JsonNode requestJson(HttpMethod method, String path, HttpHeaders headers, int status) throws Exception {
        return requestJson(method, path, headers, null, status);
    }

    private JsonNode requestJson(HttpMethod method, String path, HttpHeaders headers, Map<String, Object> body, int status, int code) throws Exception {
        JsonNode json = requestJson(method, path, headers, body, status);
        assertThat(json.at("/code").asInt()).isEqualTo(code);
        assertThat(json.at("/requestId").asText()).isNotBlank();
        return json;
    }

    private HttpHeaders bearerHeaders(String token, String requestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("X-Request-Id", requestId);
        headers.set("X-Test-Flow-Id", FLOW_ID);
        return headers;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Map<String, Object> providerBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("channel", "DISCORD");
        body.put("displayName", "Database Flow Provider " + idempotencyKey);
        body.put("endpointSummary", Map.of("url", "https://hooks.example.com/" + idempotencyKey));
        body.put("credentialRefSummary", Map.of("alias", "managed-" + idempotencyKey, "managedBy", "vault-summary"));
        body.put("receiverPolicy", Map.of("allowedReceiverTypes", List.of("CHANNEL", "EMAIL_ADDRESS"), "maxReceivers", 10));
        body.put("allowedSourceModules", List.of("notification", "alerting", "plugin-integration", "community"));
        body.put("allowedRiskLevels", List.of("LOW", "MEDIUM", "HIGH"));
        body.put("rateLimitSummary", Map.of("windowSeconds", 60, "capacity", 100));
        body.put("reason", "创建 cross-platform-notification 数据库流 provider");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> templateBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourceModule", "alerting");
        body.put("sourceTemplateRef", Map.of("code", "template-" + idempotencyKey));
        body.put("providerId", "provider-discord-main");
        body.put("externalTemplateKey", "external-template-" + idempotencyKey);
        body.put("allowedVariables", List.of("title", "body", "player"));
        body.put("renderMode", "MARKDOWN");
        body.put("fallbackTitleTemplate", "{{title}}");
        body.put("fallbackBodyTemplate", "{{body}} for {{player}}");
        body.put("reason", "创建 cross-platform-notification 数据库流模板映射");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> routeBody(String idempotencyKey, String mappingId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "Database Flow Route " + idempotencyKey);
        body.put("sourceModule", "alerting");
        body.put("eventType", "alert.firing");
        body.put("riskLevel", "HIGH");
        body.put("matchers", Map.of("sourceModule", "alerting", "eventType", "alert.firing", "riskLevel", "HIGH"));
        body.put("providerId", "provider-discord-main");
        body.put("templateMappingId", mappingId);
        body.put("receiverSummary", Map.of("receiverType", "CHANNEL", "targetRefSummary", "#ops-" + idempotencyKey));
        body.put("groupingPolicy", Map.of("groupBy", List.of("sourceModule", "eventType"), "groupWaitSeconds", 10, "groupIntervalSeconds", 60));
        body.put("retryPolicySummary", Map.of("maxAttempts", 3, "backoffSeconds", 30, "expireAfterSeconds", 3600));
        body.put("reason", "创建 cross-platform-notification 数据库流路由");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> deliveryBody(String idempotencyKey, String routeId, String mappingId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourceModule", "community");
        body.put("sourceId", "delivery-" + idempotencyKey);
        body.put("eventType", "community.post.featured");
        body.put("riskLevel", "LOW");
        body.put("routeId", routeId);
        body.put("providerId", "provider-discord-main");
        body.put("templateMappingId", mappingId);
        body.put("receiverSummary", Map.of("receiverType", "CHANNEL", "targetRefSummary", "#community"));
        body.put("payloadSummary", Map.of("title", "Community digest", "body", "Server degraded", "player", "Alex"));
        body.put("expiresAt", "2026-06-30T00:00:00Z");
        body.put("confirmText", "CREATE_EXTERNAL_DELIVERY");
        body.put("reason", "创建 cross-platform-notification 数据库流投递");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> with(Map<String, Object> source, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put(key, value);
        return copy;
    }

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private static void assertSingleValue(Connection connection, String sql, Object first, Object second, Object expected) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, first);
            statement.setObject(2, second);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).as(sql).isTrue();
                assertThat(result.getObject(1)).isEqualTo(expected);
                assertThat(result.next()).as(sql + " must return one row").isFalse();
            }
        }
    }

    private static void assertSingleValue(Connection connection, String sql, Object first, Object second, Object third, Object expected) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, first);
            statement.setObject(2, second);
            statement.setObject(3, third);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).as(sql).isTrue();
                assertThat(result.getObject(1)).isEqualTo(expected);
                assertThat(result.next()).as(sql + " must return one row").isFalse();
            }
        }
    }

    private static void createEvidenceTables(Statement statement) throws Exception {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS cpn_flow_providers (
                    flow_id VARCHAR(128) NOT NULL,
                    provider_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS cpn_flow_capabilities (
                    flow_id VARCHAR(128) NOT NULL,
                    provider_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    capability_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS cpn_flow_mappings (
                    flow_id VARCHAR(128) NOT NULL,
                    mapping_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    provider_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    version INT NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS cpn_flow_routes (
                    flow_id VARCHAR(128) NOT NULL,
                    route_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    provider_id VARCHAR(128) NOT NULL,
                    template_mapping_id VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_by VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS cpn_flow_deliveries (
                    flow_id VARCHAR(128) NOT NULL,
                    delivery_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    source_module VARCHAR(64) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    route_id VARCHAR(128),
                    provider_id VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS cpn_flow_attempts (
                    flow_id VARCHAR(128) NOT NULL,
                    attempt_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    delivery_id VARCHAR(128) NOT NULL,
                    attempt_no INT NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS cpn_flow_receivers (
                    flow_id VARCHAR(128) NOT NULL,
                    receiver_id VARCHAR(128) NOT NULL,
                    provider_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    receiver_type VARCHAR(64) NOT NULL,
                    source_module VARCHAR(64) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS cpn_flow_audits (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    action VARCHAR(128) NOT NULL,
                    target_id VARCHAR(128) NOT NULL,
                    result VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        statement.execute("""
                CREATE TABLE IF NOT EXISTS cpn_flow_request_log (
                    flow_id VARCHAR(128) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    path VARCHAR(256) NOT NULL,
                    response_code INT NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
    }

    private static void deleteFlowRows(Statement statement, String table) throws Exception {
        statement.executeUpdate("DELETE FROM " + table + " WHERE flow_id = '" + FLOW_ID + "'");
    }

    private static String randomKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @TestConfiguration
    static class EvidenceConfiguration {
        @Bean
        CrossPlatformNotificationFlowEvidenceRecorder crossPlatformNotificationFlowEvidenceRecorder() {
            return new JdbcCrossPlatformNotificationFlowEvidenceRecorder();
        }
    }

    static class JdbcCrossPlatformNotificationFlowEvidenceRecorder implements CrossPlatformNotificationFlowEvidenceRecorder {
        @Override
        public void recordProviderWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = flowId(request);
            if (flowId == null) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO cpn_flow_providers(flow_id, provider_id, action, status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("providerId"), action, payload.get("status"), payload.get("createdBy"), Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO cpn_flow_capabilities(flow_id, provider_id, action, capability_id, status, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("providerId"), action, "cap-" + payload.get("providerId"), payload.get("healthStatus"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request, action, String.valueOf(payload.get("providerId")), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write cross-platform-notification provider database evidence", exception);
            }
        }

        @Override
        public void recordTemplateMappingWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = flowId(request);
            if (flowId == null) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO cpn_flow_mappings(flow_id, mapping_id, action, provider_id, status, version, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("mappingId"), action, payload.get("providerId"), payload.get("status"), payload.get("version"), payload.get("createdBy"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request, action, String.valueOf(payload.get("mappingId")), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write cross-platform-notification mapping database evidence", exception);
            }
        }

        @Override
        public void recordRouteWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = flowId(request);
            if (flowId == null) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO cpn_flow_routes(flow_id, route_id, action, provider_id, template_mapping_id, status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("routeId"), action, payload.get("providerId"), payload.get("templateMappingId"), payload.get("status"), payload.get("createdBy"), Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO cpn_flow_receivers(flow_id, receiver_id, provider_id, action, receiver_type, source_module, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("routeId") + "-receiver", payload.get("providerId"), action, value(payload.get("receiverSummary"), "receiverType"), payload.get("sourceModule"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request, action, String.valueOf(payload.get("routeId")), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write cross-platform-notification route database evidence", exception);
            }
        }

        @Override
        public void recordDeliveryWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
            String flowId = flowId(request);
            if (flowId == null) {
                return;
            }
            try (Connection connection = openConnection()) {
                insert(connection,
                        "INSERT INTO cpn_flow_deliveries(flow_id, delivery_id, action, source_module, status, route_id, provider_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("deliveryId"), action, payload.get("sourceModule"), payload.get("status"), payload.get("routeId"), payload.get("providerId"), Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO cpn_flow_attempts(flow_id, attempt_id, action, delivery_id, attempt_no, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("deliveryId") + "-attempt-" + payload.get("attempts"), action, payload.get("deliveryId"),
                        payload.get("attempts"), attemptStatus(payload), Timestamp.from(Instant.now()));
                insert(connection,
                        "INSERT INTO cpn_flow_receivers(flow_id, receiver_id, provider_id, action, receiver_type, source_module, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        flowId, payload.get("receiverId"), payload.get("providerId"), action, value(payload.get("receiverSummary"), "receiverType"), payload.get("sourceModule"), Timestamp.from(Instant.now()));
                insertAuditAndRequest(connection, flowId, request, action, String.valueOf(payload.get("deliveryId")), responseCode);
            } catch (Exception exception) {
                throw new IllegalStateException("failed to write cross-platform-notification delivery database evidence", exception);
            }
        }

        private static String flowId(HttpServletRequest request) {
            String flowId = request.getHeader("X-Test-Flow-Id");
            return flowId == null || flowId.isBlank() ? null : flowId;
        }

        private static String value(Object value, String key) {
            if (value instanceof Map<?, ?> map) {
                Object result = map.get(key);
                return result == null ? null : String.valueOf(result);
            }
            return null;
        }

        private static String attemptStatus(Map<String, Object> payload) {
            String status = value(payload.get("attemptSummary"), "status");
            return status == null ? String.valueOf(payload.get("status")) : status;
        }

        private static void insertAuditAndRequest(Connection connection, String flowId, HttpServletRequest request, String action, String targetId, int responseCode) throws Exception {
            insert(connection,
                    "INSERT INTO cpn_flow_audits(flow_id, request_id, action, target_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    flowId, request.getHeader("X-Request-Id"), action, targetId, "SUCCESS", Timestamp.from(Instant.now()));
            insert(connection,
                    "INSERT INTO cpn_flow_request_log(flow_id, request_id, path, response_code, created_at) VALUES (?, ?, ?, ?, ?)",
                    flowId, request.getHeader("X-Request-Id"), request.getRequestURI(), responseCode, Timestamp.from(Instant.now()));
        }

        private static void insert(Connection connection, String sql, Object... values) throws Exception {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int index = 0; index < values.length; index++) {
                    statement.setObject(index + 1, values[index]);
                }
                statement.executeUpdate();
            }
        }
    }

    record TestHttpResponse(int statusCode, String body) {
    }
}
