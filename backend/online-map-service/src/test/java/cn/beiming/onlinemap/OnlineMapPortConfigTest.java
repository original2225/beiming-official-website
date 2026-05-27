package cn.beiming.onlinemap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class OnlineMapPortConfigTest {
    @Test
    void applicationPortIsFixedTo8121() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(OnlineMapServiceApplication.class)
                .properties("spring.main.web-application-type=none")
                .run()) {
            assertThat(context.getEnvironment().getProperty("server.port")).isEqualTo("8121");
        }
    }
}
