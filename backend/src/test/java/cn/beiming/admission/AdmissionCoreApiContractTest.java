package cn.beiming.admission;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdmissionCoreServiceApplication.class, properties = "server.port=8131")
@AutoConfigureMockMvc
class AdmissionCoreApiContractTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Value("${server.port}")
    private String port;

    @Test
    void usesAdmissionCoreContractPort() {
        assertThat(port).isEqualTo("8131");
    }

    @Test
    void exposesAdmissionCoreHealthSummary() throws Exception {
        mockMvc.perform(get("/api/v1/admission-core/health").header("X-Request-Id", "req-admission-health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-admission-health"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.service").value("admission-core"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.port").value(8131))
                .andExpect(jsonPath("$.data.modulesTotal").value(4))
                .andExpect(jsonPath("$.data.modulesMounted").value(4))
                .andExpect(jsonPath("$.data.admissionRoutesTotal").value(84))
                .andExpect(jsonPath("$.data.selfRoutesTotal").value(2))
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'ONBOARDING' && @.pathPrefix == '/api/v1/onboarding' && @.routesTotal == 15 && @.status == 'READY')]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'EXAM' && @.pathPrefix == '/api/v1/exams' && @.routesTotal == 28 && @.status == 'READY')]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'WHITELIST' && @.pathPrefix == '/api/v1/whitelist' && @.routesTotal == 20 && @.status == 'READY')]").exists())
                .andExpect(jsonPath("$.data.moduleRoutes[?(@.moduleKey == 'ATTENDANCE' && @.pathPrefix == '/api/v1/attendance' && @.routesTotal == 21 && @.status == 'READY')]").exists())
                .andExpect(jsonPath("$.data.generatedAt").isNotEmpty());
    }

    @Test
    void exposesAdmissionCoreAdminOpsSummaryWithAdminOnlyAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admission-core/admin/ops/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));

        mockMvc.perform(get("/api/v1/admission-core/admin/ops/summary")
                        .header("Authorization", "Basic admin-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41003));

        mockMvc.perform(get("/api/v1/admission-core/admin/ops/summary")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(42001));

        mockMvc.perform(get("/api/v1/admission-core/admin/ops/summary")
                        .header("Authorization", "Bearer helper-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(42001));

        mockMvc.perform(get("/api/v1/admission-core/admin/ops/summary")
                        .header("Authorization", "Bearer owner-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.service").value("admission-core"));

        mockMvc.perform(get("/api/v1/admission-core/admin/ops/summary")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.service").value("admission-core"))
                .andExpect(jsonPath("$.data.port").value(8131))
                .andExpect(jsonPath("$.data.modulesTotal").value(4))
                .andExpect(jsonPath("$.data.modulesMounted").value(4))
                .andExpect(jsonPath("$.data.routesTotal").value(86))
                .andExpect(jsonPath("$.data.admissionRoutesTotal").value(84))
                .andExpect(jsonPath("$.data.selfRoutesTotal").value(2))
                .andExpect(jsonPath("$.data.gatewaySwitchReady").value(true))
                .andExpect(jsonPath("$.data.gatewaySwitchStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.businessCoreDependency.service").value("business-core"))
                .andExpect(jsonPath("$.data.businessCoreDependency.port").value(8130))
                .andExpect(jsonPath("$.data.businessCoreDependency.status").value("REQUIRED_BASELINE"))
                .andExpect(jsonPath("$.data.handoffChain[?(@.from == 'onboarding' && @.to == 'exam' && @.handoff == 'OnboardingExamHandoffSnapshot' && @.mutable == false)]").exists())
                .andExpect(jsonPath("$.data.handoffChain[?(@.from == 'exam' && @.to == 'whitelist' && @.handoff == 'ExamWhitelistHandoffSnapshot' && @.mutable == false)]").exists())
                .andExpect(jsonPath("$.data.handoffChain[?(@.from == 'whitelist' && @.to == 'attendance' && @.handoff == 'WhitelistAttendanceHandoffSnapshot' && @.mutable == false)]").exists())
                .andExpect(jsonPath("$.data.legacyBaselines[?(@.service == 'onboarding-service')]").doesNotExist())
                .andExpect(jsonPath("$.data.legacyBaselines[?(@.service == 'exam-service')]").doesNotExist())
                .andExpect(jsonPath("$.data.legacyBaselines[?(@.service == 'whitelist-service')]").doesNotExist())
                .andExpect(jsonPath("$.data.legacyBaselines[?(@.service == 'attendance-service')]").doesNotExist())
                .andExpect(jsonPath("$.data.legacyBaselines[?(@.service == 'business-core-service' && @.port == 8130)]").exists())
                .andExpect(jsonPath("$.data.legacyBaselines[?(@.service == 'unified-backend-service' && @.port == 8135)]").exists())
                .andExpect(jsonPath("$.data.productionGaps[?(@ == 'gateway route switch is not complete')]").doesNotExist())
                .andExpect(jsonPath("$.data.generatedAt").isNotEmpty());
    }

    @Test
    void mountsRepresentativeSecondBatchRoutesWithoutPathRewrite() throws Exception {
        mockMvc.perform(get("/api/v1/onboarding/me/progress"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));

        mockMvc.perform(get("/api/v1/exams/me/sessions/current"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));

        mockMvc.perform(get("/api/v1/whitelist/me/applications/current"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));

        mockMvc.perform(get("/api/v1/attendance/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/examiner")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/whitelisted")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/attend")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/admission")).andExpect(status().isNotFound());
    }

    @Test
    void consumesTrustedGatewayActorContextOnlyWhenInternalRequestIdExists() throws Exception {
        mockMvc.perform(get("/api/v1/onboarding/me/progress")
                        .header("X-Gateway-Internal-Request-Id", "req-gateway-actor")
                        .header("X-Beiming-Actor-User-Id", "gw-user")
                        .header("X-Beiming-Actor-Roles", "USER")
                        .header("X-Beiming-Actor-Minecraft-Id", "GwSteve")
                        .header("X-Beiming-Actor-Minecraft-Uuid", "uuid-gateway"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value("gw-user"))
                .andExpect(jsonPath("$.data.displayNameSnapshot").value("Actor gw-user"))
                .andExpect(jsonPath("$.data.minecraftBindingSnapshot.minecraftId").value("GwSteve"));

        mockMvc.perform(get("/api/v1/onboarding/me/progress")
                        .header("X-Beiming-Actor-User-Id", "forged-user")
                        .header("X-Beiming-Actor-Roles", "OWNER"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(41000));

        mockMvc.perform(get("/api/v1/onboarding/me/progress")
                        .header("X-Gateway-Internal-Request-Id", "req-missing-actor")
                        .header("X-Beiming-Actor-Roles", "USER"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(46802));
    }

    @Test
    void registersExactlySecondBatchAndSelfApiRoutes() {
        long apiRouteMappings = handlerMapping.getHandlerMethods().keySet().stream()
                .filter(mapping -> mapping.getPatternValues().stream().anyMatch(pattern -> pattern.startsWith("/api/v1/")))
                .count();
        Set<String> apiRoutes = handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream())
                .filter(pattern -> pattern.startsWith("/api/v1/"))
                .collect(Collectors.toCollection(java.util.TreeSet::new));

        assertThat(apiRouteMappings).isEqualTo(86);
        assertThat(apiRoutes).contains(
                "/api/v1/admission-core/health",
                "/api/v1/admission-core/admin/ops/summary",
                "/api/v1/onboarding/me/progress",
                "/api/v1/exams/me/sessions/current",
                "/api/v1/whitelist/me/applications/current",
                "/api/v1/attendance/leaderboard"
        );
    }

    @Test
    void excludesLegacyServiceApplicationClassesFromMergedComponentScan() {
        ComponentScan componentScan = AdmissionCoreServiceApplication.class.getAnnotation(ComponentScan.class);

        assertThat(componentScan).isNotNull();
        assertThat(componentScan.excludeFilters()).anySatisfy(filter -> {
            assertThat(filter.type()).isEqualTo(FilterType.REGEX);
            assertThat(filter.pattern()).contains("cn\\.beiming\\.(onboarding|exam|whitelist|attendance)\\..*ServiceApplication");
        });
    }

    @Test
    void productionSourceDoesNotCrossAdmissionCoreBoundaries() throws Exception {
        List<Path> moduleSourceRoots = List.of(
                Path.of("src/main/java/cn/beiming/admission"),
                Path.of("src/main/java/cn/beiming/onboarding"),
                Path.of("src/main/java/cn/beiming/exam"),
                Path.of("src/main/java/cn/beiming/whitelist"),
                Path.of("src/main/java/cn/beiming/attendance")
        );
        String source = moduleSourceRoots.stream()
                .filter(Files::exists)
                .flatMap(root -> {
                    try {
                        return Files.walk(root);
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                })
                .filter(Files::isRegularFile)
                .map(path -> {
                    try {
                        return Files.readString(path);
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                })
                .collect(Collectors.joining("\n"));

        assertThat(source).doesNotContain(
                "ProcessBuilder",
                "Runtime.getRuntime",
                "node-daemon",
                "cloudreveToken",
                "terminal",
                "container",
                "backupRestore",
                "file-manager",
                "server.properties",
                "enforce-whitelist",
                "whitelist add",
                "whitelist remove",
                "Repository",
                "JdbcTemplate",
                "cn.beiming.auth.",
                "cn.beiming.profile.",
                "cn.beiming.notification.",
                "cn.beiming.content.",
                "cn.beiming.serverstatus.",
                "cn.beiming.resource.",
                "cn.beiming.admin.");

        assertThat(List.of(
                Path.of("../onboarding-service/src/main/java/cn/beiming/onboarding/OnboardingServiceApplication.java"),
                Path.of("../exam-service/src/main/java/cn/beiming/exam/ExamServiceApplication.java"),
                Path.of("../whitelist-service/src/main/java/cn/beiming/whitelist/WhitelistServiceApplication.java"),
                Path.of("../attendance-service/src/main/java/cn/beiming/attendance/AttendanceServiceApplication.java")
        )).allSatisfy(path -> assertThat(Files.exists(path)).isFalse());
        assertThat(List.of(
                Path.of("../onboarding-service"),
                Path.of("../exam-service"),
                Path.of("../whitelist-service"),
                Path.of("../attendance-service")
        )).allSatisfy(path -> assertThat(Files.exists(path)).isFalse());
    }
}
