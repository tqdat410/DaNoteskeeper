package app.notekeeper.model.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Retrieve note request")
public class RetrieveNoteRequest {

    @NotBlank(message = "Query is required")
    @Schema(description = "Search query", example = "meeting notes", required = true)
    private String query;

    @Schema(description = "Topic ID - optional", example = "123e4567-e89b-12d3-a456-426614174000", required = false)
    private UUID topicId;

}
