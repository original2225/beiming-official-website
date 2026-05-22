package cn.beiming.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AdminPortConfigTest {
    @Value("${server.port}")
    int serverPort;

    @Test
    void adminServicePortIsFixedAt8107() {
        assertThat(serverPort).isEqualTo(8107);
    }
}
