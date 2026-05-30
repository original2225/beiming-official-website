package cn.beiming.guide;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GuidePortConfigTest {
    @Value("${server.port}")
    int serverPort;

    @Test
    void guideServicePortIsFixedAt8127() {
        assertThat(serverPort).isEqualTo(8127);
    }
}
