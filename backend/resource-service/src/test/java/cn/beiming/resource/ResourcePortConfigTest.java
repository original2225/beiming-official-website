package cn.beiming.resource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ResourcePortConfigTest {
    @Value("${server.port}")
    int serverPort;

    @Test
    void resourceServicePortIsFixedAt8106() {
        assertThat(serverPort).isEqualTo(8106);
    }
}
