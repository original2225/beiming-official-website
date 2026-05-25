package cn.beiming.backuprecovery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class BackupRecoveryPortConfigTest {
    @Test
    void applicationPortIsFixedTo8119() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(BackupRecoveryServiceApplication.class)
                .properties("spring.main.web-application-type=none")
                .run()) {
            assertThat(context.getEnvironment().getProperty("server.port")).isEqualTo("8119");
        }
    }
}
