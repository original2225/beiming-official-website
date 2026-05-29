package cn.beiming.apigateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ApiGatewayServiceApplication.class)
@AutoConfigureMockMvc
class GatewayProductionHardeningTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void productionSourceDoesNotImportBusinessServicesOrDangerousBoundaries() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
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
        Pattern forbidden = Pattern.compile("cn\\.beiming\\.(auth|profile|notification|content|serverstatus|resource|admin|onboarding|exam|whitelist|attendance|community|activity|calendar|changelog|opscontrol|nodedaemon|cloudrevesync|backuprecovery|alerting|onlinemap|pluginintegration|crossplatformnotification|opsimagemarket)\\.|Repository|JdbcTemplate|ProcessBuilder|Runtime\\.getRuntime|DockerClient|containerd|docker\\s|kubectl|helm|registryToken|nodeToken|rawToken|secretKey|credential|jdbc:|rm -rf|Remove-Item -Recurse|rmdir /s|rd /s|del /s");
        assertThat(forbidden.matcher(source).find()).isFalse();
    }

    @Test
    void productionSummaryDeclaresKnownGapsWithoutLeakingSecrets() throws Exception {
        JsonNode summary = performJson(get("/api/v1/gateway/admin/ops/summary")
                .header("Authorization", "Bearer owner-token"), 200);
        assertThat(summary.at("/data/productionGaps").toString()).contains(
                "SERVICE_DISCOVERY_NOT_CONNECTED",
                "DISTRIBUTED_RATE_LIMIT_NOT_CONNECTED",
                "PERSISTENT_AUDIT_NOT_CONNECTED");
        assertThat(summary.toString()).doesNotContain("rawToken", "secretKey", "Authorization", "Cookie", "stackTrace");
    }

    @Test
    void javaHttpClientIsReusedOutsidePerRequestExchange() throws IOException {
        String source = Files.readString(Path.of("src/main/java/cn/beiming/apigateway/GatewayModule.java"));
        Pattern perExchangeClientCreation = Pattern.compile("GatewayHttpResponse exchange\\(GatewayRoute route, GatewayHttpRequest request\\) \\{[\\s\\S]*?HttpClient\\.newBuilder");
        assertThat(perExchangeClientCreation.matcher(source).find()).isFalse();
    }

    private JsonNode performJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder, int status) throws Exception {
        MvcResult result = mvc.perform(builder)
                .andExpect(status().is(status))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
