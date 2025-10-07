package app.notekeeper.config;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration for date and time handling in JPA entities
 * Ensures all audit timestamps are stored in UTC
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class DateTimeConfig {

    /**
     * Provides UTC timezone for JPA auditing
     */
    @Bean
    DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(ZonedDateTime.now(ZoneOffset.UTC));
    }
}