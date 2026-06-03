package cn.beiming.engagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = "cn.beiming.engagement",
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "cn\\.beiming\\.(community|activity|calendar|changelog)\\..*ServiceApplication"
        )
)
public class EngagementCoreServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EngagementCoreServiceApplication.class, args);
    }
}
