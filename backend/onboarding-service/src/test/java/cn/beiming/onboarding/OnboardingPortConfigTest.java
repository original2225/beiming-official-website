package cn.beiming.onboarding;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OnboardingPortConfigTest {
    @Autowired
    Environment environment;

    @Test
    void onboardingPortIsFixed() {
        assertThat(environment.getProperty("server.port")).isEqualTo("8108");
    }
}
