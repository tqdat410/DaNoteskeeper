package app.notekeeper.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication token response")
public class AuthTokenResponse {

    @Schema(description = "JWT access token for API authorization", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Refresh token for obtaining new access tokens", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer", defaultValue = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Access token expiration time in seconds", example = "3600")
    private Long expiresIn;

    @Schema(description = "User email address", example = "example@danotekeeper.app")
    private String email;

    @Schema(description = "User display name", example = "Name")
    private String displayName;

    @Schema(description = "User avatar URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    @Schema(description = "User role (only for admin login)", example = "USER")
    @Builder.Default
    private String role = "USER";

}