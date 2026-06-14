package cn.beiming.onlinemap;

import cn.beiming.portalcore.PortalCoreServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class OnlineMapPortConfigTest {
    @Test
    void applicationPortIsFixedTo8134ThroughPortalCore() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(PortalCoreServiceApplication.class)
                .properties("spring.main.web-application-type=none")
                .run("--server.port=8134")) {
            assertThat(context.getEnvironment().getProperty("server.port")).isEqualTo("8134");
        }
    }
}
