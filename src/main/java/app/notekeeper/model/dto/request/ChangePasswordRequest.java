package app.notekeeper.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Change password request")
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    @Schema(description = "Current password", example = "oldpassword123", required = true)
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "New password must be at least 6 characters")
    @Schema(description = "New password", example = "newpassword123", required = true)
    private String newPassword;

    @NotBlank(message = "Confirm new password is required")
    @Schema(description = "Confirm new password", example = "newpassword123", required = true)
    private String confirmNewPassword;
}