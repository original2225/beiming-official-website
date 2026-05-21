package cn.beiming.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NotificationPortConfigTest {
    @Value("${server.port}")
    int serverPort;

    @Test
    @DisplayName("notification service uses fixed port 8103")
    void notificationServiceUsesFixedPort() {
        assertThat(serverPort).isEqualTo(8103);
    }
}
