package cn.beiming.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessCoreLegacyBaselineTest {
    @Test
    void keepsLegacyServicesAsRegressionBaselinesWithoutBuildHelperDependency() {
        Path repositoryRoot = repositoryRoot();
        List<LegacyServiceBaseline> legacyServices = List.of(
                new LegacyServiceBaseline("auth-service", "auth", "Auth",
                        List.of("AuthApiContractTest.java", "AuthPortConfigTest.java")),
                new LegacyServiceBaseline("profile-service", "profile", "Profile",
                        List.of("ProfileApiContractTest.java", "ProfilePortConfigTest.java")),
                new LegacyServiceBaseline("notification-service", "notification", "Notification",
                        List.of("NotificationApiContractTest.java", "NotificationPortConfigTest.java")),
                new LegacyServiceBaseline("content-service", "content", "Content",
                        List.of("ContentApiContractTest.java", "ContentPortConfigTest.java")),
                new LegacyServiceBaseline("server-status-service", "serverstatus", "ServerStatus",
                        List.of("ServerStatusApiContractTest.java", "ServerStatusPortConfigTest.java")),
                new LegacyServiceBaseline("resource-service", "resource", "Resource",
                        List.of("ResourceApiContractTest.java", "ResourcePortConfigTest.java")),
                new LegacyServiceBaseline("admin-service", "admin", "Admin",
                        List.of("AdminApiContractTest.java", "AdminPortConfigTest.java", "AdminProductionBoundaryTest.java"))
        );

        for (LegacyServiceBaseline service : legacyServices) {
            Path serviceRoot = repositoryRoot.resolve(Path.of("backend", service.serviceName()));
            Path packageRoot = Path.of("cn", "beiming", service.packageName());
            assertPathExists(serviceRoot.resolve("pom.xml"));
            assertPathExists(serviceRoot.resolve(Path.of("src", "main", "resources", "application.yml")));
            assertPathExists(serviceRoot.resolve(Path.of("src", "main", "java")).resolve(packageRoot)
                    .resolve(service.classPrefix() + "Module.java"));
            assertPathExists(serviceRoot.resolve(Path.of("src", "main", "java")).resolve(packageRoot)
                    .resolve(service.classPrefix() + "ServiceApplication.java"));
            for (String testFile : service.testFiles()) {
                assertPathExists(serviceRoot.resolve(Path.of("src", "test", "java")).resolve(packageRoot)
                        .resolve(testFile));
            }
        }
        assertPathExists(repositoryRoot.resolve(Path.of("docs", "contracts-auth.md")));
        assertPathExists(repositoryRoot.resolve(Path.of("docs", "contracts-profile.md")));
        assertPathExists(repositoryRoot.resolve(Path.of("docs", "contracts-notification.md")));
        assertPathExists(repositoryRoot.resolve(Path.of("docs", "contracts-content.md")));
        assertPathExists(repositoryRoot.resolve(Path.of("docs", "contracts-server-status.md")));
        assertPathExists(repositoryRoot.resolve(Path.of("docs", "contracts-resource.md")));
        assertPathExists(repositoryRoot.resolve(Path.of("docs", "contracts-admin.md")));
        assertThat(assertPathExists(repositoryRoot.resolve(Path.of("backend", "business-core-service", "pom.xml")))).content().doesNotContain(
                "../auth-service",
                "../profile-service",
                "../notification-service",
                "../content-service",
                "../server-status-service",
                "../resource-service",
                "../admin-service"
        );
    }

    private Path repositoryRoot() {
        Path path = Path.of("").toAbsolutePath();
        while (path != null) {
            if (Files.exists(path.resolve(Path.of("docs", "contracts-business-core.md")))
                    && Files.exists(path.resolve(Path.of("backend", "business-core-service", "pom.xml")))) {
                return path;
            }
            path = path.getParent();
        }
        throw new AssertionError("repository root not found from " + Path.of("").toAbsolutePath());
    }

    private Path assertPathExists(Path path) {
        for (int attempt = 0; attempt < 75; attempt++) {
            if (Files.exists(path)) {
                return path;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertThat(path).exists();
        return path;
    }

    private record LegacyServiceBaseline(String serviceName, String packageName, String classPrefix, List<String> testFiles) {
    }
}
