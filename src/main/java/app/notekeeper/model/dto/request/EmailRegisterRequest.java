package app.notekeeper.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "User email registration request")
public class EmailRegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "User email address", example = "example@gmail.com", required = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "User password", example = "password123", required = true)
    private String password;

    @NotBlank(message = "Confirm password is required")
    @Schema(description = "Confirm password", example = "password123", required = true)
    private String confirmPassword;

    // Convenience method for fullName (default to email)
    public String getFullName() {
        return this.email != null ? this.email.split("@")[0] : "";
    }

    // Convenience method for gender (default null)
    public String getGender() {
        return null;
    }
}