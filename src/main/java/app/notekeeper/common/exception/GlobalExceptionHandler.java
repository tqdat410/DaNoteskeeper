package app.notekeeper.common.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import app.notekeeper.model.dto.response.JSendResponse;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<JSendResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        log.warn("Validation failed: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                errors.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                errors.put("global", error.getDefaultMessage());
            }
        });

        JSendResponse<Map<String, String>> response = JSendResponse.fail("Validation failed", errors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Removed IllegalArgumentException handler - replaced by ValidationException
    // and AuthenticationException

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<JSendResponse<Map<String, String>>> handleValidationException(
            ValidationException ex, WebRequest request) {

        log.warn("Validation error [{}]: {}", ex.getErrorType(), ex.getMessage());

        JSendResponse<Map<String, String>> response = JSendResponse.fail(ex.getMessage(), ex.getErrorDetails());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<JSendResponse<Void>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        log.error("Runtime exception occurred", ex);

        JSendResponse<Void> response = JSendResponse.error("An unexpected error occurred", 500);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<JSendResponse<Void>> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("Unexpected exception occurred", ex);

        JSendResponse<Void> response = JSendResponse.error("An unexpected error occurred", 500);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<JSendResponse<Map<String, String>>> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {

        log.warn("Authentication error [{}]: {}", ex.getErrorType(), ex.getMessage());

        // Determine HTTP status based on error type
        HttpStatus httpStatus = switch (ex.getErrorType()) {
            case INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED;
            case RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
            case SESSION_EXPIRED -> HttpStatus.UNAUTHORIZED;
        };

        JSendResponse<Map<String, String>> response = JSendResponse.fail(ex.getMessage(), ex.getErrorDetails());
        return new ResponseEntity<>(response, httpStatus);
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<JSendResponse<Map<String, String>>> handleServiceException(
            ServiceException ex, WebRequest request) {

        log.error("Service error [{}]: {}", ex.getErrorType(), ex.getMessage(), ex);

        // Determine HTTP status based on error type
        HttpStatus httpStatus = switch (ex.getErrorType()) {
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case RESOURCE_CONFLICT -> HttpStatus.CONFLICT;
            case BUSINESS_RULE_VIOLATION -> HttpStatus.BAD_REQUEST;
        };

        JSendResponse<Map<String, String>> response = JSendResponse.fail(ex.getMessage(), ex.getErrorDetails());
        return new ResponseEntity<>(response, httpStatus);
    }

    @ExceptionHandler(SystemException.class)
    public ResponseEntity<JSendResponse<Map<String, String>>> handleSystemException(
            SystemException ex, WebRequest request) {

        log.error("System error [{}]: {}", ex.getErrorType(), ex.getMessage(), ex);

        // Determine HTTP status based on error type
        HttpStatus httpStatus = switch (ex.getErrorType()) {
            case EXTERNAL_SERVICE_ERROR -> HttpStatus.SERVICE_UNAVAILABLE;
            case SYSTEM_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        JSendResponse<Map<String, String>> response = JSendResponse.error(ex.getMessage(), ex.getErrorDetails(),
                httpStatus.value());
        return new ResponseEntity<>(response, httpStatus);
    }
}
