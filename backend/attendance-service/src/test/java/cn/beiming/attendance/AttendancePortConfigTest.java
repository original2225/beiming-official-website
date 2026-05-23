package cn.beiming.attendance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AttendanceServiceApplication.class)
class AttendancePortConfigTest {
    @Autowired
    Environment environment;

    @Test
    void attendancePortIsFixed() {
        assertThat(environment.getProperty("server.port")).isEqualTo("8111");
    }
}
