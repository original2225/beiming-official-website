package cn.beiming.admission.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
class AttendancePersistenceConfiguration {
    @Bean
    AttendancePersistence attendancePersistence(ObjectProvider<JdbcTemplate> jdbcTemplate, ObjectMapper objectMapper) {
        JdbcTemplate available = jdbcTemplate.getIfAvailable();
        if (available == null) {
            return new NoopAttendancePersistence();
        }
        return new AttendancePostgresPersistence(available, objectMapper);
    }
}
