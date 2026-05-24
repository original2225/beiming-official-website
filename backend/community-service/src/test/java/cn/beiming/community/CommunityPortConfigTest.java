package cn.beiming.community;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CommunityServiceApplication.class)
class CommunityPortConfigTest {
    @Autowired
    Environment environment;

    @Test
    void communityPortIsFixed() {
        assertThat(environment.getProperty("server.port")).isEqualTo("8112");
    }
}
