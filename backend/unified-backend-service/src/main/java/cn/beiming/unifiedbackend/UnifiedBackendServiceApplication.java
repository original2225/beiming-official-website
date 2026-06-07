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
                "cn.beiming.core",
                "cn.beiming.auth",
                "cn.beiming.profile",
                "cn.beiming.notification",
                "cn.beiming.content",
                "cn.beiming.serverstatus",
                "cn.beiming.resource",
                "cn.beiming.admin",
                "cn.beiming.admission",
                "cn.beiming.engagement",
                "cn.beiming.community",
                "cn.beiming.activity",
                "cn.beiming.calendar",
                "cn.beiming.changelog",
                "cn.beiming.opscore",
                "cn.beiming.opscontrol",
                "cn.beiming.cloudrevesync",
                "cn.beiming.backuprecovery",
                "cn.beiming.alerting",
                "cn.beiming.pluginintegration",
                "cn.beiming.crossplatformnotification",
                "cn.beiming.opsimagemarket",
                "cn.beiming.onboarding",
                "cn.beiming.exam",
                "cn.beiming.whitelist",
                "cn.beiming.attendance",
                "cn.beiming.portalcore",
                "cn.beiming.guide",
                "cn.beiming.material",
                "cn.beiming.onlinemap"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "cn\\.beiming\\.(apigateway|core|admission|engagement|community|activity|calendar|changelog|opscore|opscontrol|cloudrevesync|backuprecovery|alerting|pluginintegration|crossplatformnotification|opsimagemarket|onboarding|exam|whitelist|attendance|portalcore|guide|material|onlinemap)\\..*ServiceApplication"
        )
)
public class UnifiedBackendServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UnifiedBackendServiceApplication.class, args);
    }
}
