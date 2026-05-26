package cn.beiming.alerting;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class AlertingPortConfigTest {
    @Test
    void applicationPortIsFixedTo8120() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AlertingServiceApplication.class)
                .properties("spring.main.web-application-type=none")
                .run()) {
            assertThat(context.getEnvironment().getProperty("server.port")).isEqualTo("8120");
        }
    }
}
