package app.notekeeper.common.exception;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Service error type enum
 */
@Getter
@AllArgsConstructor
enum ServiceErrorType {
    RESOURCE_NOT_FOUND(404, "Resource not found"),
    RESOURCE_CONFLICT(409, "Resource conflict"),
    BUSINESS_RULE_VIOLATION(400, "Business rule violation");

    private final Integer code;
    private final String message;
}

@Getter
public class ServiceException extends RuntimeException {

    private final ServiceErrorType errorType;
    private final Map<String, String> errorDetails;

    public ServiceException(ServiceErrorType errorType, Map<String, String> errorDetails) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.errorDetails = errorDetails;
    }

    public ServiceException(ServiceErrorType errorType, String message, Map<String, String> errorDetails) {
        super(message);
        this.errorType = errorType;
        this.errorDetails = errorDetails;
    }

    public ServiceException(ServiceErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
        this.errorDetails = null;
    }

    public static ServiceException resourceNotFound(String message) {
        return new ServiceException(ServiceErrorType.RESOURCE_NOT_FOUND, message);
    }

    public static ServiceException resourceConflict(String message) {
        return new ServiceException(ServiceErrorType.RESOURCE_CONFLICT, message);
    }

    public static ServiceException businessRuleViolation(String message) {
        return new ServiceException(ServiceErrorType.BUSINESS_RULE_VIOLATION, message);
    }
}
