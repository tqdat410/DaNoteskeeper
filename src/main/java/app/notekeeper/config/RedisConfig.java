package app.notekeeper.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories
public class RedisConfig {
    // Spring Boot auto-configuration handles Redis connection based on
    // application.properties
}