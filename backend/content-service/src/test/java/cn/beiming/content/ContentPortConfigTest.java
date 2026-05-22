package cn.beiming.content;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ContentPortConfigTest {
    @Value("${server.port}")
    int serverPort;

    @Test
    @DisplayName("content service uses fixed port 8104")
    void contentServiceUsesFixedPort() {
        assertThat(serverPort).isEqualTo(8104);
    }
}
