package cn.beiming.opscontrol;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Path;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

class OpsControlPortConfigTest {
    @Test
    void portIsFixedTo8116() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        Path path = Path.of("backend/ops-control-service/src/main/resources/application.yml");
        if (!Files.exists(path)) {
            path = Path.of("src/main/resources/application.yml");
        }
        loader.load("ops-control", new FileSystemResource(path))
                .forEach(environment.getPropertySources()::addLast);
        int port = Binder.get(environment)
                .bind("server.port", Integer.class)
                .orElseThrow(() -> new IllegalStateException("server.port is missing"));
        assertThat(port).isEqualTo(8116);
        assertThat(ConfigurationPropertySources.get(environment)).isNotNull();
    }
}
