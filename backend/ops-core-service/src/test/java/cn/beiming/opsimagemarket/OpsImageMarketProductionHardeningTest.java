package cn.beiming.opsimagemarket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = cn.beiming.opscore.OpsCoreServiceApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OpsImageMarketProductionHardeningTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void testControlHeadersAreIgnoredByDefault() throws Exception {
        JsonNode summary = performJson(get("/api/v1/ops-image-market/admin/ops/summary")
                .header("Authorization", "Bearer oim-viewer-token")
                .header("X-Test-Auth-Mode", "unavailable")
                .header("X-Test-Fail-Store", "true"), 200);
        assertThat(summary.at("/data/testControlsEnabled").asBoolean()).isFalse();
        assertThat(summary.toString()).contains("TEST_CONTROLS_DISABLED_OUTSIDE_TEST");

        JsonNode provider = performJson(post("/api/v1/ops-image-market/admin/providers")
                        .header("Authorization", "Bearer oim-admin-token")
                        .header("X-Test-Fail-Audit", "true"),
                with(providerBody("prod-control-ignored"), "confirmText", "REGISTER_IMAGE_PROVIDER"), 201);
        assertThat(provider.at("/data/providerId").asText()).isNotBlank();
        assertNoSecrets(provider);
    }

    @Test
    void productionSourceDoesNotContainForbiddenBoundaries() throws IOException {
        Path sourceRoot = Path.of("src/main/java/cn/beiming/opsimagemarket");
        StringBuilder source = new StringBuilder();
        try (var paths = Files.walk(sourceRoot)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            source.append(Files.readString(path)).append('\n');
                        } catch (IOException ex) {
                            throw new IllegalStateException(ex);
                        }
                    });
        }
        Pattern forbidden = Pattern.compile("cn\\.beiming\\.(auth|profile|notification|content|serverstatus|resource|admin|onboarding|exam|whitelist|attendance|community|activity|calendar|changelog|opscontrol|nodedaemon|cloudrevesync|backuprecovery|alerting|onlinemap|pluginintegration|crossplatformnotification)\\.|Repository|JdbcTemplate|ProcessBuilder|Runtime\\.getRuntime|DockerClient|containerd|docker\\s|kubectl|helm|skopeo|crane|oras|registryToken|dockerPassword|registryPassword|imageSecret|pullSecret|Authorization|requestHeaders|credential|secretKey|rawToken|manifestPayload|layerUrl|internalUrl|internalPath|resolvedPath|\\.env|authorized_keys|id_rsa|rm -rf|Remove-Item -Recurse|rmdir /s|rd /s|del /s|jdbc:");
        assertThat(forbidden.matcher(source).find()).isFalse();
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, int status) throws Exception {
        MvcResult result = mvc.perform(builder.accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performJson(MockHttpServletRequestBuilder builder, Map<String, Object> body, int status) throws Exception {
        MvcResult result = mvc.perform(builder
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private Map<String, Object> providerBody(String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("displayName", "Prod Image Provider " + idempotencyKey);
        body.put("registryType", "OCI_REGISTRY");
        body.put("endpointSummary", Map.of("url", "https://prod-registry.example.com/" + idempotencyKey + "/v2"));
        body.put("credentialRefSummary", Map.of("alias", "managed-" + idempotencyKey, "managedBy", "vault-summary"));
        body.put("allowedNamespaces", List.of("beiming", "library"));
        body.put("allowedSourceModules", List.of("ops-control", "plugin-integration"));
        body.put("allowedRiskLevels", List.of("LOW", "MEDIUM", "HIGH"));
        body.put("syncPolicySummary", Map.of("mode", "MANUAL", "window", "maintenance"));
        body.put("rateLimitSummary", Map.of("windowSeconds", 60, "capacity", 120));
        body.put("reason", "生产默认忽略测试控制头");
        body.put("idempotencyKey", idempotencyKey);
        return body;
    }

    private Map<String, Object> with(Map<String, Object> source, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put(key, value);
        return copy;
    }

    private void assertNoSecrets(JsonNode json) {
        assertThat(json.toString()).doesNotContain(
                "registryToken", "registryPassword", "dockerPassword", "imageSecret", "pullSecret",
                "rawToken", "secretKey", "Authorization", "requestHeaders",
                "manifestPayload", "layerUrl", "internalUrl", "internalPath", "resolvedPath",
                "fullException", "stackTrace", "databaseUrl", "ProcessBuilder", "Runtime.getRuntime",
                "node-daemon", "/srv/", "C:\\\\", ".env", "authorized_keys", "id_rsa", "token=");
    }
}
