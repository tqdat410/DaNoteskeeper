package app.notekeeper.service.impl;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import app.notekeeper.common.exception.ServiceException;
import app.notekeeper.event.NoteSharedEvent;
import app.notekeeper.event.TopicSharedEvent;
import app.notekeeper.external.email.EmailService;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.model.dto.response.NoteQueryResponse;
import app.notekeeper.model.dto.response.NotificationResponse;
import app.notekeeper.model.entity.Notification;
import app.notekeeper.model.entity.Topic;
import app.notekeeper.model.entity.User;
import app.notekeeper.model.enums.NotiResourceType;
import app.notekeeper.model.enums.NotiType;
import app.notekeeper.repository.NoteRepository;
import app.notekeeper.repository.NotificationRepository;
import app.notekeeper.repository.TopicRepository;
import app.notekeeper.repository.UserRepository;
import app.notekeeper.security.SecurityUtils;
import app.notekeeper.service.NotificationService;
import app.notekeeper.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final NoteRepository noteRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final NotificationWebSocketHandler webSocketHandler;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    @TransactionalEventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTopicShared(TopicSharedEvent event) {
        log.info("Processing topic shared event: topicId={}, sharedWith={}",
                event.getTopicId(), event.getSharedWithUserId());

        try {
            // Load topic and users
            Topic topic = topicRepository.findById(event.getTopicId())
                    .orElseThrow(() -> new RuntimeException("Topic not found: " + event.getTopicId()));

            User recipient = userRepository.findById(event.getSharedWithUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + event.getSharedWithUserId()));

            User sharer = userRepository.findById(event.getSharedByUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + event.getSharedByUserId()));

            // Send notification email via EmailService
            String topicUrl = baseUrl + "/topics/" + topic.getId();
            boolean sent = emailService.sendTopicSharedEmail(
                    recipient.getEmail(),
                    recipient.getDisplayName(),
                    sharer.getDisplayName(),
                    topic.getName(),
                    topic.getDescription(),
                    topicUrl);

            if (sent) {
                log.info("Topic shared notification sent successfully to {}", recipient.getEmail());
            } else {
                log.warn("Failed to send topic shared notification to {}", recipient.getEmail());
            }

            // Create in-app notification
            createAndSendInAppNotification(
                    recipient,
                    NotiType.TOPIC_SHARED,
                    String.format("%s shared a topic \"%s\" with you", sharer.getDisplayName(), topic.getName()),
                    NotiResourceType.TOPIC,
                    topic.getId());

        } catch (Exception e) {
            log.error("Failed to send topic shared notification for event: {}", event, e);
        }
    }

    @Override
    @TransactionalEventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onNoteShared(NoteSharedEvent event) {
        log.info("Processing note shared event: noteId={}, sharedWith={}",
                event.getNoteId(), event.getSharedWithUserId());

        try {
            // Load note and users
            NoteQueryResponse note = noteRepository.findNoteResponseById(event.getNoteId())
                    .orElseThrow(() -> new RuntimeException("Note not found: " + event.getNoteId()));

            User recipient = userRepository.findById(event.getSharedWithUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + event.getSharedWithUserId()));

            User sharer = userRepository.findById(event.getSharedByUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + event.getSharedByUserId()));

            // Send notification email via EmailService
            String noteUrl = baseUrl + "/notes/" + note.getId();
            boolean sent = emailService.sendNoteSharedEmail(
                    recipient.getEmail(),
                    recipient.getDisplayName(),
                    sharer.getDisplayName(),
                    note.getTitle(),
                    note.getType().name(),
                    note.getDescription(),
                    note.getContent(),
                    note.getFileUrl(), // Pass fileUrl for IMAGE/DOCUMENT attachments
                    noteUrl);

            if (sent) {
                log.info("Note shared notification sent successfully to {}", recipient.getEmail());
            } else {
                log.warn("Failed to send note shared notification to {}", recipient.getEmail());
            }

            // Create in-app notification
            createAndSendInAppNotification(
                    recipient,
                    NotiType.NOTE_SHARED,
                    String.format("%s shared a note \"%s\" with you", sharer.getDisplayName(), note.getTitle()),
                    NotiResourceType.NOTE,
                    note.getId());

        } catch (Exception e) {
            log.error("Failed to send note shared notification for event: {}", event, e);
        }
    }

    // ==================== IN-APP NOTIFICATION METHODS ====================

    @Override
    @Transactional(readOnly = true)
    public JSendResponse<Page<NotificationResponse>> getNotifications(Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUserId,
                pageable);
        Page<NotificationResponse> responses = notifications.map(this::toNotificationResponse);

        return JSendResponse.success(responses, "Notifications retrieved successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public JSendResponse<Long> getUnreadCount() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        long count = notificationRepository.countByUserIdAndIsReadFalse(currentUserId);
        return JSendResponse.success(count, "Unread count retrieved successfully");
    }

    @Override
    @Transactional
    public JSendResponse<Void> markAsRead(UUID notificationId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        int updated = notificationRepository.markAsRead(notificationId, currentUserId);
        if (updated == 0) {
            throw ServiceException.resourceNotFound("Notification not found or does not belong to current user");
        }

        return JSendResponse.success(null, "Notification marked as read");
    }

    @Override
    @Transactional
    public JSendResponse<Void> markAllAsRead() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        notificationRepository.markAllAsRead(currentUserId);
        return JSendResponse.success(null, "All notifications marked as read");
    }

    // ==================== HELPER METHODS ====================

    /**
     * Create and send in-app notification via WebSocket
     */
    private void createAndSendInAppNotification(User user, NotiType type, String message,
            NotiResourceType resourceType, UUID resourceId) {
        try {
            // Save notification to database
            Notification notification = Notification.builder()
                    .user(user)
                    .type(type)
                    .message(message)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .isRead(false)
                    .build();

            notification = notificationRepository.save(notification);
            log.info("In-app notification created for user: {}", user.getId());

            // Send via WebSocket if user is connected
            NotificationResponse response = toNotificationResponse(notification);
            webSocketHandler.sendNotificationToUser(user.getId(), response);

        } catch (Exception e) {
            log.error("Failed to create in-app notification", e);
        }
    }

    /**
     * Convert Notification entity to NotificationResponse DTO
     */
    private NotificationResponse toNotificationResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .message(notification.getMessage())
                .resourceType(notification.getResourceType())
                .resourceId(notification.getResourceId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
