package cn.beiming.changelog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

class ChangelogPortConfigTest {
    @Test
    void servicePortIsFixedTo8115() {
        SpringApplication application = new SpringApplication(ChangelogServiceApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        try (ConfigurableApplicationContext context = application.run()) {
            Environment environment = context.getEnvironment();
            assertThat(environment.getProperty("server.port")).isEqualTo("8115");
        }
    }
}
