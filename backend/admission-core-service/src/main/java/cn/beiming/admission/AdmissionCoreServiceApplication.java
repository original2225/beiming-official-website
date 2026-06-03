package cn.beiming.admission;

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
                "cn.beiming.admission",
                "cn.beiming.onboarding",
                "cn.beiming.exam",
                "cn.beiming.whitelist",
                "cn.beiming.attendance"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "cn\\.beiming\\.(onboarding|exam|whitelist|attendance)\\..*ServiceApplication"
        )
)
public class AdmissionCoreServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdmissionCoreServiceApplication.class, args);
    }
}
