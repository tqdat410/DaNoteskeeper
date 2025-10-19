package app.notekeeper.model.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Retrieve note response with AI-generated answer")
public class RetrieveNoteResponse {

    @Schema(description = "AI-generated answer based on retrieved notes", example = "Based on your notes, the meeting was scheduled for...")
    private String answer;

    @Schema(description = "List of relevant notes used to generate the answer")
    private List<NoteResponse> relevantNotes;

    @Schema(description = "Number of notes found", example = "3")
    private int notesFound;

}
