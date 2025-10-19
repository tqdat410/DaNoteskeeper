package app.notekeeper.service.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import app.notekeeper.common.exception.ServiceException;
import app.notekeeper.common.exception.SystemException;
import app.notekeeper.common.exception.ValidationException;
import app.notekeeper.event.NoteCreatedEvent;
import app.notekeeper.model.dto.request.FileUploadRequest;
import app.notekeeper.model.dto.request.TextUploadRequest;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.model.entity.Note;
import app.notekeeper.model.entity.Topic;
import app.notekeeper.model.entity.User;
import app.notekeeper.model.enums.NoteType;
import app.notekeeper.repository.NoteRepository;
import app.notekeeper.repository.TopicRepository;
import app.notekeeper.repository.UserRepository;
import app.notekeeper.security.SecurityUtils;
import app.notekeeper.service.IOService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class IOServiceImpl implements IOService {

    private final NoteRepository noteRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.storage.upload-dir}")
    private String uploadDir;

    @Value("${app.storage.max-file-size}")
    private long maxFileSize;

    @Value("${app.storage.allowed-image-types}")
    private String allowedImageTypes;

    @Value("${app.storage.allowed-document-types}")
    private String allowedDocumentTypes;

    @Override
    @Transactional
    public JSendResponse<Void> uploadFile(MultipartFile file, FileUploadRequest fileUploadRequest) {
        try {
            log.info("Starting file upload process for file: {}", file.getOriginalFilename());
            log.info("FileUploadRequest - Type: {}, TopicId: {}, Title: {}",
                    fileUploadRequest.getType(), fileUploadRequest.getTopicId(), fileUploadRequest.getTitle());

            // Validate request
            if (fileUploadRequest.getType() == null) {
                throw ValidationException.missingField(Map.of("type", "File type is required (IMAGE or DOCUMENT)"));
            }

            // Validate file
            validateFile(file, fileUploadRequest.getType());

            // Get current authenticated user
            UUID userId = SecurityUtils.getCurrentUserId();
            if (userId == null) {
                log.warn("File upload failed - no authenticated user");
                throw ServiceException.businessRuleViolation("Authentication required");
            }

            User currentUser = userRepository.findById(userId)
                    .orElseThrow(() -> ServiceException.resourceNotFound("User not found"));

            // Get or validate topic if provided
            Topic topic = null;
            if (fileUploadRequest.getTopicId() != null) {
                topic = topicRepository.findById(fileUploadRequest.getTopicId())
                        .orElseThrow(() -> ServiceException.resourceNotFound(
                                "Topic not found with ID: " + fileUploadRequest.getTopicId()));
                log.info("Using existing topic: {}", topic.getId());
            } else {
                // Use default topic
                topic = topicRepository.findByIsDefaultTrueAndOwnerId(currentUser.getId())
                        .orElseThrow(() -> ServiceException.resourceNotFound(
                                "Default topic not found for user ID: " + currentUser.getId()));
                log.info("Using default topic: {}", topic.getId());
            }

            // Store file to disk
            String fileUrl = storeFile(file, currentUser.getId());
            log.info("File stored successfully at: {}", fileUrl);

            // Determine note type based on file type
            NoteType noteType = fileUploadRequest.getType() == FileUploadRequest.FileType.IMAGE
                    ? NoteType.IMAGE
                    : NoteType.DOCUMENT;

            // Create note entity
            Note note = Note.builder()
                    .owner(currentUser)
                    .topic(topic)
                    .title(StringUtils.hasText(fileUploadRequest.getTitle())
                            ? fileUploadRequest.getTitle()
                            : file.getOriginalFilename())
                    .description(fileUploadRequest.getDescription())
                    .type(noteType)
                    .fileUrl(fileUrl)
                    .build();

            noteRepository.save(note);
            log.info("Note created successfully with ID: {}", note.getId());

            // Publish note created event
            eventPublisher.publishEvent(new NoteCreatedEvent(note.getId()));

            return JSendResponse.success("File uploaded successfully");

        } catch (ServiceException | ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("File upload failed for: {}", file.getOriginalFilename(), e);
            throw SystemException.systemError("File upload failed due to system error");
        }
    }

    @Override
    @Transactional
    public JSendResponse<Void> uploadText(TextUploadRequest textUploadRequest) {
        try {
            log.info("Starting text upload process for title: {}", textUploadRequest.getTitle());

            // Get current authenticated user
            UUID userId = SecurityUtils.getCurrentUserId();
            if (userId == null) {
                log.warn("Text upload failed - no authenticated user");
                throw ServiceException.businessRuleViolation("Authentication required");
            }

            User currentUser = userRepository.findById(userId)
                    .orElseThrow(() -> ServiceException.resourceNotFound("User not found"));

            // Get or validate topic if provided
            Topic topic = null;
            if (textUploadRequest.getTopicId() != null) {
                topic = topicRepository.findById(textUploadRequest.getTopicId())
                        .orElseThrow(() -> ServiceException.resourceNotFound(
                                "Topic not found with ID: " + textUploadRequest.getTopicId()));
                log.info("Using existing topic: {}", topic.getId());
            } else {
                // Use default topic
                topic = topicRepository.findByIsDefaultTrueAndOwnerId(currentUser.getId())
                        .orElseThrow(() -> ServiceException.resourceNotFound(
                                "Default topic not found for user ID: " + currentUser.getId()));
                log.info("Using default topic: {}", topic.getId());
            }

            // Create note entity
            Note note = Note.builder()
                    .owner(currentUser)
                    .topic(topic)
                    .title(textUploadRequest.getTitle())
                    .content(textUploadRequest.getContent())
                    .type(NoteType.TEXT)
                    .build();

            noteRepository.save(note);
            log.info("Text note created successfully with ID: {}", note.getId());

            // Publish note created event
            eventPublisher.publishEvent(new NoteCreatedEvent(note.getId()));

            return JSendResponse.success("Text note created successfully");

        } catch (ServiceException | ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Text upload failed for title: {}", textUploadRequest.getTitle(), e);
            throw SystemException.systemError("Text upload failed due to system error");
        }
    }

    private void validateFile(MultipartFile file, FileUploadRequest.FileType fileType) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw ValidationException.missingField(Map.of("file", "File cannot be empty"));
        }

        // Check file size
        if (file.getSize() > maxFileSize) {
            throw ValidationException.outOfRange(
                    Map.of("fileSize",
                            "File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + "MB"));
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null) {
            throw ValidationException.invalidFormat(Map.of("contentType", "Cannot determine file type"));
        }

        // Determine allowed types based on fileType parameter
        List<String> allowedTypes;
        if (fileType == FileUploadRequest.FileType.IMAGE) {
            // Trim whitespace from each allowed type
            allowedTypes = Arrays.stream(allowedImageTypes.split(","))
                    .map(String::trim)
                    .toList();
            log.info("Validating IMAGE file. Content-Type: {}, Allowed types: {}", contentType, allowedTypes);
        } else {
            // Trim whitespace from each allowed type
            allowedTypes = Arrays.stream(allowedDocumentTypes.split(","))
                    .map(String::trim)
                    .toList();
            log.info("Validating DOCUMENT file. Content-Type: {}, Allowed types: {}", contentType, allowedTypes);
        }

        if (!allowedTypes.contains(contentType)) {
            log.warn("File type validation failed. Content-Type: '{}', FileType parameter: {}, Allowed types: {}",
                    contentType, fileType, allowedTypes);
            throw ValidationException.invalidFormat(
                    Map.of("contentType", "File type not allowed. Allowed types: " + String.join(", ", allowedTypes)));
        }

        log.info("File validation passed for: {} (Content-Type: {}, FileType: {})",
                file.getOriginalFilename(), contentType, fileType);
    }

    private String storeFile(MultipartFile file, UUID userId) {
        try {
            // Create upload directory if not exists
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath);
            }

            // Create user-specific subdirectory
            Path userUploadPath = uploadPath.resolve(userId.toString());
            if (!Files.exists(userUploadPath)) {
                Files.createDirectories(userUploadPath);
                log.info("Created user upload directory: {}", userUploadPath);
            }

            // Generate unique filename
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = "";
            if (originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

            // Store file
            Path targetLocation = userUploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Return relative file path
            return userId.toString() + "/" + uniqueFilename;

        } catch (IOException ex) {
            log.error("Failed to store file: {}", file.getOriginalFilename(), ex);
            throw SystemException.systemError("Could not store file. Please try again!");
        }
    }

    @Override
    public Resource loadFileAsResource(String fileUrl) {
        try {
            log.info("Loading file as resource: {}", fileUrl);

            Path filePath = Paths.get(uploadDir).resolve(fileUrl).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.info("File loaded successfully: {}", fileUrl);
                return resource;
            } else {
                log.error("File not found or not readable: {}", fileUrl);
                throw ServiceException.resourceNotFound("File not found: " + fileUrl);
            }

        } catch (MalformedURLException ex) {
            log.error("Failed to load file: {}", fileUrl, ex);
            throw ServiceException.resourceNotFound("File not found: " + fileUrl);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            log.info("Deleting file: {}", fileUrl);

            Path filePath = Paths.get(uploadDir).resolve(fileUrl).normalize();

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("File deleted successfully: {}", fileUrl);
            } else {
                log.warn("File not found for deletion: {}", fileUrl);
            }

        } catch (IOException ex) {
            log.error("Failed to delete file: {}", fileUrl, ex);
            throw SystemException.systemError("Could not delete file: " + fileUrl);
        }
    }
}
