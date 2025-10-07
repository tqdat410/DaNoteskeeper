package app.notekeeper.common.exception;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Authentication error type enum
 */
@Getter
@AllArgsConstructor
enum AuthenticationErrorType {
    INVALID_CREDENTIALS(401, "Invalid credentials"),
    SESSION_EXPIRED(401, "Session expired"),
    RATE_LIMIT_EXCEEDED(429, "Rate limit exceeded");

    private final Integer code;
    private final String message;
}

@Getter
public class AuthenticationException extends RuntimeException {

    private final AuthenticationErrorType errorType;
    private final Map<String, String> errorDetails;

    public AuthenticationException(AuthenticationErrorType errorType, Map<String, String> errorDetails) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.errorDetails = errorDetails;
    }

    public AuthenticationException(AuthenticationErrorType errorType, String message,
            Map<String, String> errorDetails) {
        super(message);
        this.errorType = errorType;
        this.errorDetails = errorDetails;
    }

    public AuthenticationException(AuthenticationErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
        this.errorDetails = null;
    }

    public static AuthenticationException invalidCredentials(String message) {
        return new AuthenticationException(AuthenticationErrorType.INVALID_CREDENTIALS, message);
    }

    public static AuthenticationException sessionExpired(String message) {
        return new AuthenticationException(AuthenticationErrorType.SESSION_EXPIRED, message);
    }

    public static AuthenticationException rateLimitExceeded(String message) {
        return new AuthenticationException(AuthenticationErrorType.RATE_LIMIT_EXCEEDED, message);
    }
}
