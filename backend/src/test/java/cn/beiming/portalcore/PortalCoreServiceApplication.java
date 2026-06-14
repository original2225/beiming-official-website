package cn.beiming.portalcore;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = {
                "cn.beiming.portalcore",
                "cn.beiming.guide",
                "cn.beiming.material",
                "cn.beiming.onlinemap"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "cn\\.beiming\\.(guide|material|onlinemap)\\..*ServiceApplication"
        )
)
public class PortalCoreServiceApplication {
}
