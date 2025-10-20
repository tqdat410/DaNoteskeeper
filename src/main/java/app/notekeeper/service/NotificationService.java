package app.notekeeper.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import app.notekeeper.event.NoteSharedEvent;
import app.notekeeper.event.TopicSharedEvent;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.model.dto.response.NotificationResponse;

public interface NotificationService {

    /**
     * Handle topic shared event and send notification email
     * 
     * @param event The topic shared event containing topic and user IDs
     */
    void onTopicShared(TopicSharedEvent event);

    /**
     * Handle note shared event and send notification email with attachment
     * 
     * @param event The note shared event containing note and user IDs
     */
    void onNoteShared(NoteSharedEvent event);

    /**
     * Get all notifications for current user (paginated)
     */
    JSendResponse<Page<NotificationResponse>> getNotifications(Pageable pageable);

    /**
     * Get unread notification count for current user
     */
    JSendResponse<Long> getUnreadCount();

    /**
     * Mark notification as read
     */
    JSendResponse<Void> markAsRead(UUID notificationId);

    /**
     * Mark all notifications as read
     */
    JSendResponse<Void> markAllAsRead();
}
