package app.notekeeper.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import app.notekeeper.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    // Map userId -> WebSocketSession
    private final Map<UUID, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final JwtProvider jwtProvider;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Extract access token from query params: /ws/notifications?token=xxx
        String query = session.getUri().getQuery();
        if (query != null && query.startsWith("token=")) {
            String token = query.substring(6);
            try {
                // Validate token
                if (!jwtProvider.validateToken(token)) {
                    log.error("Invalid or expired token");
                    session.close();
                    return;
                }

                // Extract userId from token
                UUID userId = jwtProvider.getUserIdFromToken(token);
                sessions.put(userId, session);
                log.info("WebSocket connection established for user: {}", userId);
            } catch (Exception e) {
                log.error("Failed to process access token: {}", e.getMessage());
                session.close();
            }
        } else {
            log.warn("WebSocket connection without token, closing");
            session.close();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // Remove session from map
        sessions.entrySet().removeIf(entry -> entry.getValue().getId().equals(session.getId()));
        log.info("WebSocket connection closed: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Handle incoming messages if needed (e.g., ping/pong)
        log.debug("Received message: {}", message.getPayload());
    }

    /**
     * Send notification to specific user
     */
    public void sendNotificationToUser(UUID userId, Object notification) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(notification);
                session.sendMessage(new TextMessage(json));
                log.info("Notification sent to user: {}", userId);
            } catch (IOException e) {
                log.error("Failed to send notification to user: {}", userId, e);
            }
        } else {
            log.debug("User {} is not connected via WebSocket", userId);
        }
    }

    /**
     * Check if user is connected
     */
    public boolean isUserConnected(UUID userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }
}
