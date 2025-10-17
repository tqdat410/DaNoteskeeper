package app.notekeeper.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TopicResponse {
    private UUID id;
    private String name;
    private String description;
    private String aiSummary;
    private UUID ownerId;
    private String ownerDisplayName;
}
