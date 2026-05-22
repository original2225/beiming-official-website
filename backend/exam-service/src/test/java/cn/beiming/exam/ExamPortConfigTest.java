package cn.beiming.exam;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ExamPortConfigTest {
    @Autowired
    Environment environment;

    @Test
    void examPortIsFixed() {
        assertThat(environment.getProperty("server.port")).isEqualTo("8109");
    }
}
