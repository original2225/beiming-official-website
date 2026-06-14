package cn.beiming.engagement;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = {
                "cn.beiming.engagement",
                "cn.beiming.community",
                "cn.beiming.activity",
                "cn.beiming.calendar",
                "cn.beiming.changelog"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "cn\\.beiming\\.(community|activity|calendar|changelog)\\..*ServiceApplication"
        )
)
public class EngagementCoreServiceApplication {
}
