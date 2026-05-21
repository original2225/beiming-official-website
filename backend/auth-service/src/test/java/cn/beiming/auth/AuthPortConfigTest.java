package cn.beiming.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuthPortConfigTest {
    @Autowired
    Environment environment;

    @Test
    @DisplayName("auth service uses the fixed local development port")
    void authServiceUsesFixedLocalDevelopmentPort() {
        assertThat(environment.getProperty("server.port")).isEqualTo("8101");
    }
}
