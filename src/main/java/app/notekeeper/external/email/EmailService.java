package app.notekeeper.external.email;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class EmailService {

    private final Resend resend;

    @Value("${app.base-url}")
    private String baseUrl;

    public EmailService(@Value("${app.email.api-key}") String apiKey) {
        this.resend = new Resend(apiKey);
    }

    public boolean sendVerificationEmail(String toEmail, String name, String verificationToken) {
        log.info("Sending email verification to: {}", toEmail);

        try {
            String verificationUrl = baseUrl + "/api/v1/auth/verify-email?token=" + verificationToken;
            String htmlContent = loadEmailTemplate("templates/email-send-template.html",
                    name, verificationUrl, "Email Confirmation", "Confirm Email",
                    "Thank you for registering with NoteKeeper. Please click the button below to confirm your email address and complete your registration.",
                    "Note: The verification link will expire in 5 minutes and can only be used once.");

            CreateEmailOptions emailOptions = CreateEmailOptions.builder()
                    .from("DaNotekeeper <no-reply@dattq.click>")
                    .to(toEmail)
                    .subject("Email Confirmation - NoteKeeper")
                    .html(htmlContent)
                    .build();

            CreateEmailResponse response = resend.emails().send(emailOptions);
            log.info("Verification email sent successfully to: {} with ID: {}", toEmail, response.getId());

            return true;

        } catch (ResendException e) {
            log.error("Failed to send verification email to: {} - Resend Error: {}", toEmail, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error sending verification email to: {}", toEmail, e);
            return false;
        }
    }

    public boolean sendPasswordResetEmail(String toEmail, String name, String resetToken) {
        log.info("Sending password reset email to: {}", toEmail);

        try {
            String resetUrl = baseUrl + "/api/v1/auth/reset-password?token=" + resetToken;
            String htmlContent = loadEmailTemplate("templates/email-send-template.html",
                    name, resetUrl, "Password Reset", "Reset Password",
                    "We received a request to reset your password for your NoteKeeper account. Click the button below to reset your password.",
                    "Note: This password reset link will expire in 5 minutes and can only be used once.");

            CreateEmailOptions emailOptions = CreateEmailOptions.builder()
                    .from("DaNoteskeeper <no-reply@dattq.click>")
                    .to(toEmail)
                    .subject("Password Reset - NoteKeeper")
                    .html(htmlContent)
                    .build();

            CreateEmailResponse response = resend.emails().send(emailOptions);
            log.info("Password reset email sent successfully to: {} with ID: {}", toEmail, response.getId());

            return true;

        } catch (ResendException e) {
            log.error("Failed to send password reset email to: {} - Resend Error: {}", toEmail, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error sending password reset email to: {}", toEmail, e);
            return false;
        }
    }

    private String loadEmailTemplate(String templatePath, String name, String actionUrl,
            String title, String buttonText, String description, String expiryNote) {
        try {
            ClassPathResource resource = new ClassPathResource(templatePath);
            String template = resource.getContentAsString(StandardCharsets.UTF_8);

            // Replace placeholders in template
            template = template.replace("{{USER_NAME}}", name);
            template = template.replace("{{VERIFICATION_URL}}", actionUrl);
            template = template.replace("{{TITLE}}", title);
            template = template.replace("{{BUTTON_TEXT}}", buttonText);
            template = template.replace("{{DESCRIPTION}}", description);
            template = template.replace("{{EXPIRY_NOTE}}", expiryNote);

            return template;

        } catch (IOException e) {
            log.error("Failed to load email template from: {}", templatePath, e);
            throw new RuntimeException("Email template not found: " + templatePath, e);
        }
    }
}
