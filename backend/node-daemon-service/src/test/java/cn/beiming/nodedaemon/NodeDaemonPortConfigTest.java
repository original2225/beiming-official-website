package cn.beiming.nodedaemon;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class NodeDaemonPortConfigTest {
    @Test
    void applicationPortIsFixedTo8117() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        loader.load("node-daemon", new FileSystemResource("backend/node-daemon-service/src/main/resources/application.yml"))
                .forEach(environment.getPropertySources()::addFirst);

        Integer port = Binder.get(environment).bind("server.port", Integer.class).orElse(null);
        String service = Binder.get(environment).bind("node-daemon.service", String.class).orElse(null);

        assertThat(port).isEqualTo(8117);
        assertThat(Objects.requireNonNull(service)).isEqualTo("node-daemon");
    }
}
