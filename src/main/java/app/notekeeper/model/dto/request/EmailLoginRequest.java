package app.notekeeper.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "User email login request")
public class EmailLoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "User email address", example = "example@gmail.com", required = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "User password", example = "password123", required = true)
    private String password;
}
