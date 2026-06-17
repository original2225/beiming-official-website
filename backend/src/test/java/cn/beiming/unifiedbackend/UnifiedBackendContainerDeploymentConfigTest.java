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

    @Test
    void databaseDevelopmentGuideRequiresRealContainerVerification() throws Exception {
        String agents = Files.readString(root.resolve("AGENTS.md"));
        String apiReference = Files.readString(root.resolve("docs/api-reference.md"));
        String systemDesign = Files.readString(root.resolve("docs/system-design.md"));

        assertThat(agents)
                .contains("之后所有数据库开发、数据库结构调整、持久化适配和相关测试，都必须在 Docker Desktop WSL 或服务器同等真实容器环境中完成")
                .contains("不得只依赖本机进程、H2、MockMvc 或未容器化数据库作为完成依据")
                .contains("容器环境至少包含 `beiming-backend` 和 `beiming-postgres`，并且两个容器必须处于 `healthy` 状态");

        assertThat(apiReference)
                .contains("后续所有数据库开发、数据库结构调整、持久化适配和正式验收都必须使用真实容器链路")
                .contains("容器链路至少包含 `beiming-backend` 和 `beiming-postgres`")
                .contains("不得只用本机进程、H2、MockMvc 或未容器化数据库作为完成依据");

        assertThat(systemDesign)
                .contains("从容器化部署基线建立后，数据库相关开发默认运行在真实容器链路上")
                .contains("本地使用 Docker Desktop WSL，服务器使用 Arch Linux 容器环境")
                .contains("`beiming-backend` 与 `beiming-postgres` 均为 `healthy` 后才能开始数据库验收");
    }

    private static Path projectRoot() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        return cwd.getFileName() != null && "backend".equals(cwd.getFileName().toString()) ? cwd.getParent() : cwd;
    }
}
