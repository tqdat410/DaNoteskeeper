package app.notekeeper.model.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "File upload request")
public class FileUploadRequest {

    @Schema(description = "Topic ID - not required", example = "d290f1ee-6c54-4b01-90e6-d701748f0851", required = false)
    private UUID topicId;

    @Schema(description = "File title - not required", example = "My First Image", required = false)
    private String title;

    @Schema(description = "File description - not required", example = "This is an image file", required = false)
    private String description;

    @Schema(description = "File type", example = "IMAGE", required = true)
    private FileType type;

    public enum FileType {
        IMAGE,
        DOCUMENT
    }

    public FileType getType() {
        return type;
    }

}
