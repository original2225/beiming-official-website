package cn.beiming.opscore;

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
                "cn.beiming.opscore",
                "cn.beiming.opscontrol",
                "cn.beiming.cloudrevesync",
                "cn.beiming.backuprecovery",
                "cn.beiming.alerting",
                "cn.beiming.pluginintegration",
                "cn.beiming.opsimagemarket"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "cn\\.beiming\\.(opscontrol|cloudrevesync|backuprecovery|alerting|pluginintegration|opsimagemarket)\\..*ServiceApplication"
        )
)
public class OpsCoreServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpsCoreServiceApplication.class, args);
    }
}
