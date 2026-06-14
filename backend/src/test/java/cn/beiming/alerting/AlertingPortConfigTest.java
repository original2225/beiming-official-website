package cn.beiming.alerting;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class AlertingPortConfigTest {
    @Test
    void applicationPortIsFixedTo8133() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(cn.beiming.opscore.OpsCoreServiceApplication.class)
                .properties("spring.main.web-application-type=none")
                .run("--server.port=8133")) {
            assertThat(context.getEnvironment().getProperty("server.port")).isEqualTo("8133");
        }
    }
}
