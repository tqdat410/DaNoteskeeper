package app.notekeeper.model.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Text upload request")
public class TextUploadRequest {

    @Schema(description = "Topic ID - not required", example = "d290f1ee-6c54-4b01-90e6-d701748f0851", required = false)
    private UUID topicId;

    @NotBlank(message = "Title is required")
    @Schema(description = "Note title", example = "My First Note", required = true)
    private String title;

    @NotBlank(message = "Content is required")
    @Schema(description = "Note content", example = "This is the content of my first note\nContent.", required = true)
    private String content;

}
