package app.notekeeper.model.dto.response;

import java.time.ZonedDateTime;
import java.util.UUID;

import app.notekeeper.model.enums.NotePerm;
import app.notekeeper.model.enums.NoteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SharedNoteResponse {
    private UUID id;
    private UUID noteId;
    private String noteTitle;
    private String noteDescription;
    private NoteType noteType;
    private UUID userId;
    private String userEmail;
    private String userDisplayName;
    private NotePerm permission;
    private ZonedDateTime createdAt;
}
