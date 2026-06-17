package cn.beiming.unifiedbackend;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedBackendContainerDeploymentConfigTest {
    private final Path root = projectRoot();

    @Test
    void containerDeploymentFilesUseUnifiedBackendEntrypointAndExternalizedDatabaseSecrets() throws Exception {
        String dockerfile = Files.readString(root.resolve("backend/Dockerfile"));
        String compose = Files.readString(root.resolve("docker-compose.yml"));
        String envExample = Files.readString(root.resolve("compose.local.env.example"));

        assertThat(dockerfile)
                .contains("COPY target/unified-backend-service-0.1.0-SNAPSHOT.jar")
                .contains("ENTRYPOINT [\"java\", \"-jar\", \"/app/unified-backend-service.jar\"]")
                .contains("EXPOSE 8135")
                .contains("/api/v1/unified-backend/health");

        assertThat(compose)
                .contains("beiming-backend")
                .contains("beiming-postgres")
                .contains("8135:8135")
                .contains("context: ./backend")
                .contains("dockerfile: Dockerfile")
                .contains("SPRING_AUTOCONFIGURE_EXCLUDE: \"\"")
                .contains("SPRING_FLYWAY_ENABLED: \"true\"")
                .contains("jdbc:postgresql://postgres:5432/beiming")
                .contains("SPRING_DATASOURCE_USERNAME: $${POSTGRES_USER}")
                .contains("SPRING_DATASOURCE_PASSWORD: $${POSTGRES_PASSWORD}")
                .contains("/api/v1/unified-backend/health")
                .doesNotContain("5432:5432")
                .doesNotContain("8125")
                .doesNotContain("8130")
                .doesNotContain("8131")
                .doesNotContain("8132")
                .doesNotContain("8133")
                .doesNotContain("8134");

        assertThat(envExample)
                .contains("POSTGRES_DB=beiming")
                .contains("POSTGRES_USER=beiming")
                .contains("POSTGRES_PASSWORD=")
                .doesNotContain("SPRING_DATASOURCE_PASSWORD=");
    }

    private static Path projectRoot() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        return cwd.getFileName() != null && "backend".equals(cwd.getFileName().toString()) ? cwd.getParent() : cwd;
    }
}
