package cn.beiming.crossplatformnotification;

import cn.beiming.opscore.OpsCoreServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class CrossPlatformNotificationPortConfigTest {
    @Test
    void applicationPortIsFixedToOpsCorePortAndKeepsLegacyPortInSummary() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(OpsCoreServiceApplication.class)
                .properties("spring.main.web-application-type=none")
                .run()) {
            assertThat(context.getEnvironment().getProperty("server.port")).isEqualTo("8133");
        }
    }
}
