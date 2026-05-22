package cn.beiming.serverstatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ServerStatusPortConfigTest {
    @Value("${server.port}")
    int serverPort;

    @Test
    @DisplayName("server-status service uses fixed port 8105")
    void serverStatusServiceUsesFixedPort() {
        assertThat(serverPort).isEqualTo(8105);
    }
}
