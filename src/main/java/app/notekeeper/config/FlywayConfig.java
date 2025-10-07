package app.notekeeper.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

/**
 * Flyway Configuration for Auth Service
 * Handles database migration automatically on startup
 */
@Configuration
@Slf4j
public class FlywayConfig {

    /**
     * Custom Flyway migration strategy
     * Ensures proper migration execution and logging
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return new FlywayMigrationStrategy() {
            @Override
            public void migrate(Flyway flyway) {
                log.info("Starting Flyway database migration for auth-service");

                try {
                    // Run migration
                    var result = flyway.migrate();

                    log.info("Flyway migration completed successfully. Applied {} migrations",
                            result.migrationsExecuted);

                    // Log migration info
                    var info = flyway.info();
                    if (info.all().length > 0) {
                        log.info("Current database version: {}", info.current().getVersion());
                    }

                } catch (Exception e) {
                    log.error("Flyway migration failed", e);
                    throw e;
                }
            }
        };
    }
}
