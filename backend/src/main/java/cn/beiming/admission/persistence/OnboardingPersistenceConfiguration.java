package cn.beiming.admission.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
class OnboardingPersistenceConfiguration {
    @Bean
    OnboardingPersistence onboardingPersistence(ObjectProvider<JdbcTemplate> jdbcTemplate, ObjectMapper objectMapper) {
        JdbcTemplate available = jdbcTemplate.getIfAvailable();
        if (available == null) {
            return new NoopOnboardingPersistence();
        }
        return new OnboardingPostgresPersistence(available, objectMapper);
    }
}
