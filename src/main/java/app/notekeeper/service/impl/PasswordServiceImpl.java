package app.notekeeper.service.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import app.notekeeper.common.exception.ServiceException;
import app.notekeeper.common.exception.SystemException;
import app.notekeeper.external.email.EmailService;
import app.notekeeper.model.dto.request.ChangePasswordRequest;
import app.notekeeper.model.dto.request.ForgotPasswordRequest;
import app.notekeeper.model.dto.request.ResetPasswordRequest;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.model.entity.PasswordReset;
import app.notekeeper.model.entity.User;
import app.notekeeper.repository.PasswordResetRepository;
import app.notekeeper.repository.UserRepository;
import app.notekeeper.security.SecurityUtils;
import app.notekeeper.service.PasswordService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    public JSendResponse<Void> changePassword(ChangePasswordRequest request) {
        try {
            // Get current user from security context
            String currentUserEmail = SecurityUtils.getCurrentUserEmail();
            if (currentUserEmail == null) {
                log.warn("Change password failed - no authenticated user");
                throw ServiceException.businessRuleViolation("Authentication required");
            }

            log.info("Change password attempt for: {}", currentUserEmail);

            // Validate password confirmation
            if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
                throw ServiceException.businessRuleViolation("New password confirmation does not match");
            }

            // Find user
            User user = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> ServiceException.businessRuleViolation("User not found"));

            // Verify current password
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                log.warn("Change password failed - invalid current password for: {}", currentUserEmail);
                throw ServiceException.businessRuleViolation("Current password is incorrect");
            }

            // Update password
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            log.info("Password changed successfully for: {}", currentUserEmail);
            return JSendResponse.success(null, "Password changed successfully");

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Change password failed", e);
            throw SystemException.systemError("Change password failed due to system error");
        }
    }

    @Override
    public JSendResponse<Void> forgotPassword(ForgotPasswordRequest request) {
        try {
            log.info("Forgot password request for: {}", request.getEmail());

            // Check if user exists
            Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
            if (userOpt.isEmpty()) {
                // Don't reveal if email exists or not for security
                log.warn("Forgot password request for non-existent email: {}", request.getEmail());
                return JSendResponse.success(null, "If the email exists, a password reset link has been sent");
            }

            User user = userOpt.get();

            // Check if there's a pending reset request (cooldown check)
            Optional<PasswordReset> existingReset = passwordResetRepository.findByEmail(request.getEmail());
            if (existingReset.isPresent()) {
                PasswordReset passwordReset = existingReset.get();
                long currentTime = System.currentTimeMillis();
                long cooldownPeriod = 60000; // 1 minute cooldown

                if (currentTime - passwordReset.getLastResendTime() < cooldownPeriod) {
                    log.warn("Forgot password blocked due to cooldown for: {}", request.getEmail());
                    throw ServiceException
                            .businessRuleViolation("Please wait before requesting another password reset");
                }

                // Update existing reset request
                passwordReset.setLastResendTime(currentTime);
                passwordResetRepository.save(passwordReset);

                // Send email with existing token
                boolean emailSent = emailService.sendPasswordResetEmail(
                        request.getEmail(),
                        user.getDisplayName(),
                        passwordReset.getToken());

                if (!emailSent) {
                    log.error("Failed to send password reset email to: {}", request.getEmail());
                    throw SystemException.externalServiceError("Failed to send password reset email");
                }
            } else {
                // Create new reset token
                String resetToken = UUID.randomUUID().toString();

                PasswordReset passwordReset = PasswordReset.builder()
                        .token(resetToken)
                        .email(request.getEmail())
                        .lastResendTime(System.currentTimeMillis())
                        .build();

                passwordResetRepository.save(passwordReset);

                // Send password reset email
                boolean emailSent = emailService.sendPasswordResetEmail(
                        request.getEmail(),
                        user.getDisplayName(),
                        resetToken);

                if (!emailSent) {
                    log.error("Failed to send password reset email to: {}", request.getEmail());
                    passwordResetRepository.deleteById(resetToken);
                    throw SystemException.externalServiceError("Failed to send password reset email");
                }
            }

            log.info("Password reset email sent successfully to: {}", request.getEmail());
            return JSendResponse.success(null, "If the email exists, a password reset link has been sent");

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Forgot password failed for: {}", request.getEmail(), e);
            throw SystemException.systemError("Forgot password failed due to system error");
        }
    }

    @Override
    public String resetPassword(String token, ResetPasswordRequest request) {
        try {
            log.info("Password reset attempt with token: {}", token);

            // Validate password confirmation
            if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
                return "PASSWORD_MISMATCH";
            }

            // Get password reset data from token
            Optional<PasswordReset> passwordResetOpt = passwordResetRepository.findById(token);
            if (passwordResetOpt.isEmpty()) {
                log.warn("Invalid or expired password reset token: {}", token);
                return "EXPIRED";
            }

            PasswordReset passwordReset = passwordResetOpt.get();
            String email = passwordReset.getEmail();

            // Find user
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                log.warn("User not found for password reset: {}", email);
                passwordResetRepository.deleteById(token);
                return "USER_NOT_FOUND";
            }

            User user = userOpt.get();

            // Update password
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            // Cleanup reset token
            passwordResetRepository.deleteById(token);

            log.info("Password reset completed successfully for: {}", email);
            return "SUCCESS";

        } catch (Exception e) {
            log.error("Password reset failed for token: {}", token, e);
            return "ERROR";
        }
    }
}
