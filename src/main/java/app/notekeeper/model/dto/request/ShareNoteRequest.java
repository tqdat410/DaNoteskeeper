package app.notekeeper.model.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShareNoteRequest {

    @NotNull(message = "Note ID is required")
    private UUID noteId;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}
