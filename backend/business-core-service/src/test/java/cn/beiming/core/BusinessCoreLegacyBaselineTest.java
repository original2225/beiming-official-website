package cn.beiming.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessCoreLegacyBaselineTest {
    @Test
    void removesRetiredLegacyServiceSourcesAfterBusinessCoreReplacement() {
        Path repositoryRoot = repositoryRoot();
        List<Path> retiredLegacyFiles = List.of(
                Path.of("backend", "admin-service", "pom.xml"),
                Path.of("backend", "admin-service", "src", "main", "java", "cn", "beiming", "admin", "AdminModule.java"),
                Path.of("backend", "admin-service", "src", "main", "java", "cn", "beiming", "admin", "AdminServiceApplication.java"),
                Path.of("backend", "admin-service", "src", "main", "resources", "application.yml"),
                Path.of("backend", "admin-service", "src", "test", "java", "cn", "beiming", "admin", "AdminApiContractTest.java"),
                Path.of("backend", "admin-service", "src", "test", "java", "cn", "beiming", "admin", "AdminPortConfigTest.java"),
                Path.of("backend", "admin-service", "src", "test", "java", "cn", "beiming", "admin", "AdminProductionBoundaryTest.java"),
                Path.of("backend", "auth-service", "pom.xml"),
                Path.of("backend", "auth-service", "src", "main", "java", "cn", "beiming", "auth", "AuthModule.java"),
                Path.of("backend", "auth-service", "src", "main", "java", "cn", "beiming", "auth", "AuthServiceApplication.java"),
                Path.of("backend", "auth-service", "src", "main", "resources", "application.yml"),
                Path.of("backend", "auth-service", "src", "test", "java", "cn", "beiming", "auth", "AuthApiContractTest.java"),
                Path.of("backend", "auth-service", "src", "test", "java", "cn", "beiming", "auth", "AuthPortConfigTest.java"),
                Path.of("backend", "content-service", "pom.xml"),
                Path.of("backend", "content-service", "src", "main", "java", "cn", "beiming", "content", "ContentModule.java"),
                Path.of("backend", "content-service", "src", "main", "java", "cn", "beiming", "content", "ContentServiceApplication.java"),
                Path.of("backend", "content-service", "src", "main", "resources", "application.yml"),
                Path.of("backend", "content-service", "src", "test", "java", "cn", "beiming", "content", "ContentApiContractTest.java"),
                Path.of("backend", "content-service", "src", "test", "java", "cn", "beiming", "content", "ContentPortConfigTest.java"),
                Path.of("backend", "notification-service", "pom.xml"),
                Path.of("backend", "notification-service", "src", "main", "java", "cn", "beiming", "notification", "NotificationModule.java"),
                Path.of("backend", "notification-service", "src", "main", "java", "cn", "beiming", "notification", "NotificationServiceApplication.java"),
                Path.of("backend", "notification-service", "src", "main", "resources", "application.yml"),
                Path.of("backend", "notification-service", "src", "test", "java", "cn", "beiming", "notification", "NotificationApiContractTest.java"),
                Path.of("backend", "notification-service", "src", "test", "java", "cn", "beiming", "notification", "NotificationPortConfigTest.java"),
                Path.of("backend", "profile-service", "pom.xml"),
                Path.of("backend", "profile-service", "src", "main", "java", "cn", "beiming", "profile", "ProfileModule.java"),
                Path.of("backend", "profile-service", "src", "main", "java", "cn", "beiming", "profile", "ProfileServiceApplication.java"),
                Path.of("backend", "profile-service", "src", "main", "resources", "application.yml"),
                Path.of("backend", "profile-service", "src", "test", "java", "cn", "beiming", "profile", "ProfileApiContractTest.java"),
                Path.of("backend", "profile-service", "src", "test", "java", "cn", "beiming", "profile", "ProfilePortConfigTest.java"),
                Path.of("backend", "resource-service", "pom.xml"),
                Path.of("backend", "resource-service", "src", "main", "java", "cn", "beiming", "resource", "ResourceModule.java"),
                Path.of("backend", "resource-service", "src", "main", "java", "cn", "beiming", "resource", "ResourceServiceApplication.java"),
                Path.of("backend", "resource-service", "src", "main", "resources", "application.yml"),
                Path.of("backend", "resource-service", "src", "test", "java", "cn", "beiming", "resource", "ResourceApiContractTest.java"),
                Path.of("backend", "resource-service", "src", "test", "java", "cn", "beiming", "resource", "ResourcePortConfigTest.java"),
                Path.of("backend", "server-status-service", "pom.xml"),
                Path.of("backend", "server-status-service", "src", "main", "java", "cn", "beiming", "serverstatus", "ServerStatusModule.java"),
                Path.of("backend", "server-status-service", "src", "main", "java", "cn", "beiming", "serverstatus", "ServerStatusServiceApplication.java"),
                Path.of("backend", "server-status-service", "src", "main", "resources", "application.yml"),
                Path.of("backend", "server-status-service", "src", "test", "java", "cn", "beiming", "serverstatus", "ServerStatusApiContractTest.java"),
                Path.of("backend", "server-status-service", "src", "test", "java", "cn", "beiming", "serverstatus", "ServerStatusPortConfigTest.java")
        );

        for (Path retiredFile : retiredLegacyFiles) {
            assertThat(repositoryRoot.resolve(retiredFile)).doesNotExist();
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
}
