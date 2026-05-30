package cn.beiming.material;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MaterialPortConfigTest {
    @Value("${server.port}")
    int serverPort;

    @Test
    void materialServicePortIsFixedAt8126() {
        assertThat(serverPort).isEqualTo(8126);
    }
}
