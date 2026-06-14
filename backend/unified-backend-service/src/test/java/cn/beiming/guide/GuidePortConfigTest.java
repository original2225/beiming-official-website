package cn.beiming.guide;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = cn.beiming.portalcore.PortalCoreServiceApplication.class, properties = "server.port=8134")
class GuidePortConfigTest {
    @Value("${server.port}")
    int serverPort;

    @Test
    void guideModuleRunsThroughPortalCorePort8134() {
        assertThat(serverPort).isEqualTo(8134);
    }
}
