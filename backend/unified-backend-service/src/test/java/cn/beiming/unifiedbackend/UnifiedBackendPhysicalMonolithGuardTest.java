package cn.beiming.unifiedbackend;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedBackendPhysicalMonolithGuardTest {
    private static final List<String> CORE_DIRECTORIES = List.of(
            "business-core-service",
            "admission-core-service",
            "engagement-core-service",
            "ops-core-service",
            "portal-core-service"
    );

    @Test
    void unifiedBackendPomDoesNotMountLegacyCoreSourceRoots() throws IOException {
        String pom = Files.readString(serviceRoot().resolve("pom.xml"));

        assertThat(pom)
                .doesNotContain("build-helper-maven-plugin")
                .doesNotContain("add-source")
                .doesNotContain("../business-core-service/src/main/java")
                .doesNotContain("../admission-core-service/src/main/java")
                .doesNotContain("../engagement-core-service/src/main/java")
                .doesNotContain("../ops-core-service/src/main/java")
                .doesNotContain("../portal-core-service/src/main/java");
    }

    @Test
    void unifiedBackendIsOnlyBackendMavenEntrypoint() throws IOException {
        Path backendRoot = backendRoot();

        try (Stream<Path> paths = Files.walk(backendRoot)) {
            assertThat(paths
                    .filter(path -> path.getFileName().toString().equals("pom.xml"))
                    .map(backendRoot::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .toList())
                    .containsExactly("unified-backend-service/pom.xml");
        }
    }

    @Test
    void unifiedBackendIsOnlySpringBootEntrypoint() throws IOException {
        Path backendRoot = backendRoot();

        try (Stream<Path> paths = Files.walk(backendRoot)) {
            assertThat(paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("src\\main\\java")
                            || path.toString().contains("src/main/java"))
                    .filter(this::looksLikeSpringBootEntrypoint)
                    .map(backendRoot::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .toList())
                    .containsExactly("unified-backend-service/src/main/java/cn/beiming/unifiedbackend/UnifiedBackendServiceApplication.java");
        }
    }

    @Test
    void legacyCoreDirectoriesNoLongerContainProductionJavaSources() throws IOException {
        Path backendRoot = backendRoot();

        for (String coreDirectory : CORE_DIRECTORIES) {
            Path sourceRoot = backendRoot.resolve(coreDirectory).resolve("src/main/java");
            if (!Files.exists(sourceRoot)) {
                continue;
            }
            try (Stream<Path> paths = Files.walk(sourceRoot)) {
                assertThat(paths
                        .filter(path -> path.toString().endsWith(".java"))
                        .map(backendRoot::relativize)
                        .map(Path::toString)
                        .map(path -> path.replace('\\', '/'))
                        .toList())
                        .as(coreDirectory + " production Java sources")
                        .isEmpty();
            }
        }
    }

    private boolean looksLikeSpringBootEntrypoint(Path path) {
        try {
            String source = Files.readString(path);
            return source.contains("@SpringBootApplication")
                    || source.contains("SpringApplication.run")
                    || source.contains("public static void main(String[] args)");
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read " + path, ex);
        }
    }

    private static Path backendRoot() {
        return serviceRoot().getParent();
    }

    private static Path serviceRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        if (Files.exists(current.resolve("pom.xml"))
                && Files.exists(current.resolve("src/main/java/cn/beiming/unifiedbackend/UnifiedBackendServiceApplication.java"))) {
            return current;
        }
        return current.resolve("backend/unified-backend-service").normalize();
    }
}
