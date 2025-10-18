package app.notekeeper.model.dto.response;

import java.time.ZonedDateTime;
import java.util.UUID;

import app.notekeeper.model.enums.NoteType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Note response")
public class NoteResponse {

    @Schema(description = "Note ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    private UUID id;

    @Schema(description = "Note title", example = "My First Note")
    private String title;

    @Schema(description = "Note content (TEXT type only)", example = "This is the content of my note")
    private String content;

    @Schema(description = "Note description", example = "A brief description")
    private String description;

    @Schema(description = "Note type", example = "TEXT")
    private NoteType type;

    @Schema(description = "Owner ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    private UUID ownerId;

    @Schema(description = "Owner display name", example = "John Doe")
    private String ownerDisplayName;

    @Schema(description = "Topic ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    private UUID topicId;

    @Schema(description = "Topic name", example = "Personal Notes")
    private String topicName;

    @Schema(description = "Created timestamp")
    private ZonedDateTime createdAt;

    @Schema(description = "Updated timestamp")
    private ZonedDateTime updatedAt;

}
