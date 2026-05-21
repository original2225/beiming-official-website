package cn.beiming.profile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProfilePortConfigTest {
    @Value("${server.port}")
    int serverPort;

    @Test
    @DisplayName("profile service uses fixed port 8102")
    void profileServiceUsesFixedPort() {
        assertThat(serverPort).isEqualTo(8102);
    }
}
