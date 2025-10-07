package app.notekeeper.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import app.notekeeper.model.dto.request.ChangePasswordRequest;
import app.notekeeper.model.dto.request.EmailLoginRequest;
import app.notekeeper.model.dto.request.EmailRegisterRequest;
import app.notekeeper.model.dto.request.ForgotPasswordRequest;
import app.notekeeper.model.dto.request.GoogleLoginRequest;
import app.notekeeper.model.dto.request.RefreshTokenRequest;
import app.notekeeper.model.dto.request.ResendEmailRequest;
import app.notekeeper.model.dto.request.ResetPasswordRequest;
import app.notekeeper.model.dto.response.AuthTokenResponse;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.service.AuthService;
import app.notekeeper.service.PasswordService;
import app.notekeeper.service.SessionService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Authentication", description = "APIs for USER authentication")
public class AuthController {

        private final AuthService authService;
        private final PasswordService passwordService;
        private final SessionService sessionService;

        @PostMapping("/login/email")
        @Operation(summary = "Login with email and password", description = "Authenticate user with email and password")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Login successful"),
                        @ApiResponse(responseCode = "400", description = "Invalid credentials or validation error", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = JSendResponse.class)))
        })
        public ResponseEntity<JSendResponse<AuthTokenResponse>> loginWithEmail(
                        @Valid @RequestBody EmailLoginRequest request) {
                log.info("Email login request received for: {}", request.getEmail());
                JSendResponse<AuthTokenResponse> response = authService.loginWithEmail(request);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/login/google")
        @Operation(summary = "Login with Google", description = "Authenticate user with Google ID token")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Google login successful"),
                        @ApiResponse(responseCode = "400", description = "Invalid Google ID token or validation error", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "502", description = "Google service error", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = JSendResponse.class)))
        })
        public ResponseEntity<JSendResponse<AuthTokenResponse>> loginWithGoogle(
                        @Valid @RequestBody GoogleLoginRequest request) {
                log.info("Google login request received");
                JSendResponse<AuthTokenResponse> response = authService.loginWithGoogle(request);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/register/email")
        @Operation(summary = "Register with email", description = "Register new user account with email verification")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Registration email sent successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input or email already exists", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "502", description = "Email service error", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = JSendResponse.class)))
        })
        public ResponseEntity<JSendResponse<Void>> registerWithEmail(
                        @Valid @RequestBody EmailRegisterRequest request) {
                log.info("Email registration request received for: {}", request.getEmail());

                // Validate password confirmation
                if (!request.getPassword().equals(request.getConfirmPassword())) {
                        return ResponseEntity.badRequest().body(
                                        JSendResponse.fail("Password confirmation does not match"));
                }

                JSendResponse<Void> response = authService.registerWithEmail(request);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/register/email/resend")
        @Operation(summary = "Resend verification email", description = "Resend email verification for pending registration")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Verification email sent successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid email or no pending registration", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "429", description = "Too many requests - cooldown active", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "502", description = "Email service error", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = JSendResponse.class)))
        })
        public ResponseEntity<JSendResponse<Void>> resendVerificationEmail(
                        @Valid @RequestBody ResendEmailRequest request) {
                log.info("Resend verification email request for: {}", request.getEmail());
                JSendResponse<Void> response = authService.resendVerificationEmail(request);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/verify-email")
        @Operation(summary = "Verify email registration", description = "Complete user registration by verifying email token")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Email verification page"),
                        @ApiResponse(responseCode = "400", description = "Invalid or expired token")
        })
        @Hidden
        public ModelAndView verifyEmail(@RequestParam("token") String token) {
                log.info("Email verification request with token: {}", token);

                String result = authService.verifyEmailRegistration(token);

                ModelAndView modelAndView = new ModelAndView("email-verification");
                modelAndView.addObject("result", result);

                return modelAndView;
        }

        @PostMapping("/password/change")
        @Operation(summary = "Change password", description = "Change user password (requires authentication)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Password changed successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid current password or validation error", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = JSendResponse.class)))
        })
        public ResponseEntity<JSendResponse<Void>> changePassword(
                        @Valid @RequestBody ChangePasswordRequest request) {
                log.info("Change password request");

                // Validate password confirmation
                if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
                        return ResponseEntity.badRequest().body(
                                        JSendResponse.fail("New password confirmation does not match"));
                }

                JSendResponse<Void> response = passwordService.changePassword(request);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/password/forgot")
        @Operation(summary = "Forgot password", description = "Send password reset email to user")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Password reset email sent (if email exists)"),
                        @ApiResponse(responseCode = "400", description = "Validation error or too many requests", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "502", description = "Email service error", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = JSendResponse.class)))
        })
        public ResponseEntity<JSendResponse<Void>> forgotPassword(
                        @Valid @RequestBody ForgotPasswordRequest request) {
                log.info("Forgot password request for: {}", request.getEmail());
                JSendResponse<Void> response = passwordService.forgotPassword(request);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/reset-password")
        @Operation(summary = "Reset password page", description = "Display password reset form")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Password reset page"),
                        @ApiResponse(responseCode = "400", description = "Invalid or expired token")
        })
        @Hidden
        public ModelAndView resetPasswordPage(@RequestParam("token") String token) {
                log.info("Reset password page request with token: {}", token);

                ModelAndView modelAndView = new ModelAndView("password-reset");
                modelAndView.addObject("token", token);

                return modelAndView;
        }

        @PostMapping("/reset-password")
        @Operation(summary = "Reset password", description = "Reset user password with token")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Password reset page with result"),
                        @ApiResponse(responseCode = "400", description = "Invalid token or validation error")
        })
        @Hidden
        public ModelAndView resetPassword(
                        @RequestParam("token") String token,
                        @Valid ResetPasswordRequest request) {
                log.info("Reset password request with token: {}", token);

                String result = passwordService.resetPassword(token, request);

                ModelAndView modelAndView = new ModelAndView("password-reset");
                modelAndView.addObject("result", result);
                modelAndView.addObject("token", token);

                return modelAndView;
        }

        @PostMapping("/session/logout")
        @Operation(summary = "Logout", description = "Logout user and invalidate refresh token")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Logout successful"),
                        @ApiResponse(responseCode = "400", description = "Invalid refresh token", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = JSendResponse.class)))
        })
        public ResponseEntity<JSendResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
                log.info("Logout request");
                JSendResponse<Void> response = sessionService.logout(request);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/session/refresh")
        @Operation(summary = "Refresh access token", description = "Generate new access token using refresh token")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = JSendResponse.class)))
        })
        public ResponseEntity<JSendResponse<AuthTokenResponse>> refreshToken(
                        @Valid @RequestBody RefreshTokenRequest request) {
                log.info("Refresh token request");
                JSendResponse<AuthTokenResponse> response = sessionService.refreshToken(request);
                return ResponseEntity.ok(response);
        }
}
