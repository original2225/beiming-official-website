package cn.beiming.crossplatformnotification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class CrossPlatformNotificationPortConfigTest {
    @Test
    void applicationPortIsFixedTo8123() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(CrossPlatformNotificationServiceApplication.class)
                .properties("spring.main.web-application-type=none")
                .run()) {
            assertThat(context.getEnvironment().getProperty("server.port")).isEqualTo("8123");
        }
    }
}
