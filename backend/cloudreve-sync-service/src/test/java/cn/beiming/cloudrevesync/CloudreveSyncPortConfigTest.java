package cn.beiming.cloudrevesync;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class CloudreveSyncPortConfigTest {
    @Test
    void applicationPortIsFixedTo8118() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(CloudreveSyncServiceApplication.class)
                .properties("spring.main.web-application-type=none")
                .run()) {
            assertThat(context.getEnvironment().getProperty("server.port")).isEqualTo("8118");
        }
    }
}
