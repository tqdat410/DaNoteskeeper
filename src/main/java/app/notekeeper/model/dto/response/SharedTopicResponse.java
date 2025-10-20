package app.notekeeper.model.dto.response;

import java.time.ZonedDateTime;
import java.util.UUID;

import app.notekeeper.model.enums.TopicPerm;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SharedTopicResponse {
    private UUID id;
    private UUID topicId;
    private String topicName;
    private UUID userId;
    private String userEmail;
    private String userDisplayName;
    private TopicPerm permission;
    private ZonedDateTime createdAt;
}
