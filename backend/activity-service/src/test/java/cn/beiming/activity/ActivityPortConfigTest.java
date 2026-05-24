package cn.beiming.activity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ActivityServiceApplication.class)
class ActivityPortConfigTest {
    @Autowired
    Environment environment;

    @Test
    void activityPortIsFixed() {
        assertThat(environment.getProperty("server.port")).isEqualTo("8113");
    }
}
