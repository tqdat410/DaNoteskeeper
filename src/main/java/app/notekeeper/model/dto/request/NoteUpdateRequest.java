package app.notekeeper.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Note update request (TEXT only)")
public class NoteUpdateRequest {

    @Schema(description = "Note title", example = "Updated Title")
    private String title;

    @Schema(description = "Note content", example = "Updated content here...")
    private String content;

}
