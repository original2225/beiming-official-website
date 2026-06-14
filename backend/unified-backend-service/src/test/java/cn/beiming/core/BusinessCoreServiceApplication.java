package cn.beiming.core;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = {
                "cn.beiming.core",
                "cn.beiming.auth",
                "cn.beiming.profile",
                "cn.beiming.notification",
                "cn.beiming.content",
                "cn.beiming.serverstatus",
                "cn.beiming.resource",
                "cn.beiming.admin"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "cn\\.beiming\\.(auth|profile|notification|content|serverstatus|resource|admin)\\..*ServiceApplication"
        )
)
public class BusinessCoreServiceApplication {
}
