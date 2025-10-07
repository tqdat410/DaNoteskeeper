package app.notekeeper.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Google login request")
public class GoogleLoginRequest {

    @NotBlank(message = "Google ID token is required")
    @Schema(description = "Google ID token received from frontend OAuth flow", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImY1NzJjNGI4...", required = true)
    private String idToken;

}
