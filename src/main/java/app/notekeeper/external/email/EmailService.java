package app.notekeeper.external.email;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Service
@Slf4j
public class EmailService {

    private final Resend resend;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.storage.upload-dir}")
    private String uploadDir;

    public EmailService(@Value("${app.email.api-key}") String apiKey) {
        this.resend = new Resend(apiKey);
    }

    public boolean sendVerificationEmail(String toEmail, String name, String verificationToken) {
        log.info("Sending email verification to: {}", toEmail);

        try {
            String verificationUrl = baseUrl + "/api/v1/auth/verify-email?token=" + verificationToken;
            String htmlContent = loadEmailTemplate("templates/email-send-template.html",
                    name, verificationUrl, "Email Confirmation", "Confirm Email",
                    "Thank you for registering with DaNoteskeeper. Please click the button below to confirm your email address and complete your registration.",
                    "Note: The verification link will expire in 5 minutes and can only be used once.");

            CreateEmailOptions emailOptions = CreateEmailOptions.builder()
                    .from("DaNoteskeeper <no-reply@dattq.click>")
                    .to(toEmail)
                    .subject("Email Confirmation - DaNoteskeeper")
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
                    "We received a request to reset your password for your DaNoteskeeper account. Click the button below to reset your password.",
                    "Note: This password reset link will expire in 5 minutes and can only be used once.");

            CreateEmailOptions emailOptions = CreateEmailOptions.builder()
                    .from("DaNoteskeeper <no-reply@dattq.click>")
                    .to(toEmail)
                    .subject("Password Reset - DaNoteskeeper")
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

    /**
     * Send topic shared notification email
     * 
     * @param toEmail          Recipient email address
     * @param userName         Recipient display name
     * @param sharerName       Person who shared the topic
     * @param topicName        Name of the shared topic
     * @param topicDescription Description of the topic
     * @param topicUrl         URL to view the topic
     * @return true if email sent successfully, false otherwise
     */
    public boolean sendTopicSharedEmail(String toEmail, String userName, String sharerName,
            String topicName, String topicDescription, String topicUrl) {
        log.info("Sending topic shared notification to: {}", toEmail);

        try {
            String message = String.format("<strong>%s</strong> has shared a topic with you!", sharerName);
            String sharedInfo = buildTopicInfo(topicName, topicDescription);

            String htmlContent = loadSharingTemplate(
                    userName,
                    "Topic Shared With You",
                    message,
                    sharedInfo,
                    topicUrl,
                    "View Topic",
                    "You can now view and access all notes in this shared topic.");

            CreateEmailOptions emailOptions = CreateEmailOptions.builder()
                    .from("DaNoteskeeper <no-reply@dattq.click>")
                    .to(toEmail)
                    .subject(String.format("Topic Shared: %s - DaNoteskeeper", topicName))
                    .html(htmlContent)
                    .build();

            CreateEmailResponse response = resend.emails().send(emailOptions);
            log.info("Topic shared email sent successfully to: {} with ID: {}", toEmail, response.getId());

            return true;

        } catch (ResendException e) {
            log.error("Failed to send topic shared email to: {} - Resend Error: {}", toEmail, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error sending topic shared email to: {}", toEmail, e);
            return false;
        }
    }

    /**
     * Send note shared notification email with optional attachment
     * 
     * @param toEmail         Recipient email address
     * @param userName        Recipient display name
     * @param sharerName      Person who shared the note
     * @param noteTitle       Title of the shared note
     * @param noteType        Type of the note (TEXT, IMAGE, DOCUMENT)
     * @param noteDescription Description of the note
     * @param noteContent     Content of the note (for TEXT notes)
     * @param fileUrl         File URL (for IMAGE/DOCUMENT notes, relative path in
     *                        upload dir)
     * @param noteUrl         URL to view the note
     * @return true if email sent successfully, false otherwise
     */
    public boolean sendNoteSharedEmail(String toEmail, String userName, String sharerName,
            String noteTitle, String noteType, String noteDescription,
            String noteContent, String fileUrl, String noteUrl) {
        log.info("Sending note shared notification to: {}", toEmail);

        try {
            String message = String.format("<strong>%s</strong> has shared a note with you!", sharerName);
            String sharedInfo = buildNoteInfo(noteTitle, noteType, noteDescription);
            String additionalNote = "TEXT".equals(noteType) || "IMAGE".equals(noteType) || "DOCUMENT".equals(noteType)
                    ? "The note file is attached to this email for your convenience."
                    : "You can view this note by clicking the button above.";

            String htmlContent = loadSharingTemplate(
                    userName,
                    "Note Shared With You",
                    message,
                    sharedInfo,
                    noteUrl,
                    "View Note",
                    additionalNote);

            CreateEmailOptions.Builder emailBuilder = CreateEmailOptions.builder()
                    .from("DaNoteskeeper <no-reply@dattq.click>")
                    .to(toEmail)
                    .subject(String.format("Note Shared: %s - DaNoteskeeper", noteTitle))
                    .html(htmlContent);

            // Attach file based on note type
            Attachment attachment = null;

            if ("TEXT".equals(noteType) && noteContent != null && !noteContent.isEmpty()) {
                // TEXT: Attach content as .txt file
                String filename = sanitizeFilename(noteTitle) + ".txt";
                String base64Content = Base64.getEncoder()
                        .encodeToString(noteContent.getBytes(StandardCharsets.UTF_8));

                attachment = Attachment.builder()
                        .fileName(filename)
                        .content(base64Content)
                        .build();

                log.info("Attached TEXT note content as file: {}", filename);

            } else if (("IMAGE".equals(noteType) || "DOCUMENT".equals(noteType))
                    && fileUrl != null && !fileUrl.isEmpty()) {
                // IMAGE/DOCUMENT: Attach actual file from upload directory
                try {
                    Path filePath = Paths.get(uploadDir, fileUrl);

                    if (Files.exists(filePath)) {
                        byte[] fileBytes = Files.readAllBytes(filePath);
                        String base64Content = Base64.getEncoder().encodeToString(fileBytes);
                        String filename = filePath.getFileName().toString();

                        attachment = Attachment.builder()
                                .fileName(filename)
                                .content(base64Content)
                                .build();

                        log.info("Attached {} note file: {}", noteType, filename);
                    } else {
                        log.warn("File not found for attachment: {}", filePath);
                    }
                } catch (IOException e) {
                    log.error("Failed to read file for attachment: {}", fileUrl, e);
                }
            }

            // Add attachment if present
            if (attachment != null) {
                emailBuilder.attachments(attachment);
            }

            CreateEmailResponse response = resend.emails().send(emailBuilder.build());
            log.info("Note shared email sent successfully to: {} with ID: {}", toEmail, response.getId());

            return true;

        } catch (ResendException e) {
            log.error("Failed to send note shared email to: {} - Resend Error: {}", toEmail, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error sending note shared email to: {}", toEmail, e);
            return false;
        }
    }

    private String buildTopicInfo(String topicName, String topicDescription) {
        return String.format(
                "<div class=\"info-box\">" +
                        "<p><strong>Topic Name:</strong> %s</p>" +
                        "<p><strong>Description:</strong> %s</p>" +
                        "</div>",
                topicName,
                topicDescription != null ? topicDescription : "No description");
    }

    private String buildNoteInfo(String noteTitle, String noteType, String noteDescription) {
        return String.format(
                "<div class=\"info-box\">" +
                        "<p><strong>Note Title:</strong> %s</p>" +
                        "<p><strong>Type:</strong> %s</p>" +
                        "<p><strong>Description:</strong> %s</p>" +
                        "</div>",
                noteTitle,
                noteType,
                noteDescription != null ? noteDescription : "No description");
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "note";
        }
        // Remove special characters and limit length
        return filename.replaceAll("[^a-zA-Z0-9-_\\s]", "")
                .replaceAll("\\s+", "_")
                .substring(0, Math.min(filename.length(), 50));
    }

    private String loadSharingTemplate(String userName, String title, String message,
            String sharedInfo, String actionUrl, String buttonText,
            String additionalNote) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/sharing-notification.html");
            String template = resource.getContentAsString(StandardCharsets.UTF_8);

            // Replace placeholders
            template = template.replace("{{USER_NAME}}", userName);
            template = template.replace("{{TITLE}}", title);
            template = template.replace("{{MESSAGE}}", message);
            template = template.replace("{{SHARED_INFO}}", sharedInfo);
            template = template.replace("{{ACTION_URL}}", actionUrl);
            template = template.replace("{{BUTTON_TEXT}}", buttonText);
            template = template.replace("{{ADDITIONAL_NOTE}}", additionalNote);

            return template;

        } catch (IOException e) {
            log.error("Failed to load sharing notification template", e);
            throw new RuntimeException("Email template not found: templates/sharing-notification.html", e);
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
