package cn.beiming.unifiedbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = {
                "cn.beiming.unifiedbackend",
                "cn.beiming.apigateway",
                "cn.beiming.portalcore",
                "cn.beiming.guide",
                "cn.beiming.material",
                "cn.beiming.onlinemap"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "cn\\.beiming\\.(apigateway|portalcore|guide|material|onlinemap)\\..*ServiceApplication"
        )
)
public class UnifiedBackendServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UnifiedBackendServiceApplication.class, args);
    }
}
