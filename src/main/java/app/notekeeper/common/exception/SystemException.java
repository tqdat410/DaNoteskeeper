package app.notekeeper.common.exception;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * System error type enum
 */
@Getter
@AllArgsConstructor
enum SystemErrorType {
    EXTERNAL_SERVICE_ERROR(502, "External service error"),
    SYSTEM_ERROR(500, "System error");

    private final Integer code;
    private final String message;
}

@Getter
public class SystemException extends RuntimeException {

    private final SystemErrorType errorType;
    private final Map<String, String> errorDetails;

    public SystemException(SystemErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.errorDetails = null;
    }

    public SystemException(SystemErrorType errorType, Map<String, String> errorDetails) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.errorDetails = errorDetails;
    }

    public SystemException(SystemErrorType errorType, String message, Map<String, String> errorDetails) {
        super(message);
        this.errorType = errorType;
        this.errorDetails = errorDetails;
    }

    public SystemException(SystemErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
        this.errorDetails = null;
    }

    public static SystemException externalServiceError(String message) {
        return new SystemException(SystemErrorType.EXTERNAL_SERVICE_ERROR, message);
    }

    public static SystemException systemError(String message) {
        return new SystemException(SystemErrorType.SYSTEM_ERROR, message);
    }
}
