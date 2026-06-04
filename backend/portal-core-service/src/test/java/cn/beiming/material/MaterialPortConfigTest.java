package cn.beiming.material;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = cn.beiming.portalcore.PortalCoreServiceApplication.class)
class MaterialPortConfigTest {
    @Value("${server.port}")
    int serverPort;

    @Test
    void materialModuleRunsThroughPortalCorePort8134() {
        assertThat(serverPort).isEqualTo(8134);
    }
}
