package cn.beiming.opscontrol;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class OpsControlPortConfigTest {
    @Test
    void portIsFixedTo8133() throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(cn.beiming.opscore.OpsCoreServiceApplication.class)
                .properties("spring.main.web-application-type=none")
                .run("--server.port=8133")) {
            assertThat(context.getEnvironment().getProperty("server.port", Integer.class)).isEqualTo(8133);
        }
    }
}
