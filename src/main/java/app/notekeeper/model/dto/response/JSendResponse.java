package app.notekeeper.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * Generic API response wrapper with type safety and detailed error handling
 * Follows JSend specification: https://github.com/omniti-labs/jsend
 * 
 * @param <T> Type of the data payload
 */
@Value
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper following JSend specification")
public class JSendResponse<T> {

    @Schema(description = "Response status", example = "success", allowableValues = { "success", "fail", "error" })
    ResponseStatus status;

    @Schema(description = "Response data payload")
    T data;

    @Schema(description = "Human-readable message", example = "Operation completed successfully")
    String message;

    @Schema(description = "Error code for system errors", example = "500")
    Integer errorCode;

    // Success response
    public static <T> JSendResponse<T> success(String message) {
        return JSendResponse.<T>builder()
                .status(ResponseStatus.SUCCESS)
                .message(message)
                .build();
    }

    public static <T> JSendResponse<T> success(T data, String message) {
        return JSendResponse.<T>builder()
                .status(ResponseStatus.SUCCESS)
                .data(data)
                .message(message)
                .build();
    }

    // Fail response
    public static <T> JSendResponse<T> fail(String message) {
        return JSendResponse.<T>builder()
                .message(message)
                .status(ResponseStatus.FAIL)
                .build();
    }

    public static <T> JSendResponse<T> fail(String message, T errors) {
        return JSendResponse.<T>builder()
                .message(message)
                .data(errors)
                .status(ResponseStatus.FAIL)
                .build();
    }

    // Error response
    public static <T> JSendResponse<T> error(String message, Integer errorCode) {
        return JSendResponse.<T>builder()
                .status(ResponseStatus.ERROR)
                .message(message)
                .errorCode(errorCode)
                .build();
    }

    public static <T> JSendResponse<T> error(String message, T data, Integer errorCode) {
        return JSendResponse.<T>builder()
                .status(ResponseStatus.ERROR)
                .message(message)
                .data(data)
                .errorCode(errorCode)
                .build();
    }

    enum ResponseStatus {
        SUCCESS("success"),
        ERROR("error"),
        FAIL("fail");

        private final String value;

        ResponseStatus(String status) {
            this.value = status;
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }
}
