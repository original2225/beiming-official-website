package cn.beiming.unifiedbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@SpringBootConfiguration
@EnableAutoConfiguration
public class UnifiedBackendServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UnifiedBackendServiceApplication.class, args);
    }
}
