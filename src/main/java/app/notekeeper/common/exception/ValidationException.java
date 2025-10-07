package app.notekeeper.common.exception;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Validation error type enum
 */
@Getter
@AllArgsConstructor
enum ValidationErrorType {
    INVALID_FORMAT(400, "Invalid format"),
    REQUIRED_FIELD(400, "Missing field"),
    OUT_OF_RANGE(400, "Out of range"),
    DUPLICATE_VALUE(409, "Duplicate value");

    private final Integer code;
    private final String message;
}

@Getter
public class ValidationException extends RuntimeException {

    private final ValidationErrorType errorType;
    private final Map<String, String> errorDetails;

    public ValidationException(ValidationErrorType errorType, Map<String, String> errorDetails) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.errorDetails = errorDetails;
    }

    public ValidationException(ValidationErrorType errorType, String message, Map<String, String> errorDetails) {
        super(message);
        this.errorType = errorType;
        this.errorDetails = errorDetails;
    }

    public ValidationException(ValidationErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
        this.errorDetails = null;
    }

    public static ValidationException missingField(Map<String, String> details) {
        return new ValidationException(ValidationErrorType.REQUIRED_FIELD, details);
    }

    public static ValidationException invalidFormat(Map<String, String> details) {
        return new ValidationException(ValidationErrorType.INVALID_FORMAT, details);
    }

    public static ValidationException outOfRange(Map<String, String> details) {
        return new ValidationException(ValidationErrorType.OUT_OF_RANGE, details);
    }

    public static ValidationException duplicateValue(Map<String, String> details) {
        return new ValidationException(ValidationErrorType.DUPLICATE_VALUE, details);
    }
}
