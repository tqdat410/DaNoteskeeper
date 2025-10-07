package app.notekeeper.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT configuration properties
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
@Data
public class JwtProperties {

    private String secret;
    private Long accessTokenExpiration = 3600000L; // 1 hour in milliseconds
    private Long refreshTokenExpiration = 2592000000L; // 30 days in milliseconds

}
