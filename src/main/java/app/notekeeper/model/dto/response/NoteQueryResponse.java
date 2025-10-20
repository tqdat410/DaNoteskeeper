package app.notekeeper.model.dto.response;

import java.time.ZonedDateTime;
import java.util.UUID;

import app.notekeeper.model.entity.Topic;
import app.notekeeper.model.entity.User;
import app.notekeeper.model.enums.NoteType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteQueryResponse {

    private UUID id;

    private User owner;

    private Topic topic;

    private String title;

    private String description;

    private String content;

    private String aiSummary;

    private NoteType type;

    private String fileUrl;

    private ZonedDateTime createdAt;

    private ZonedDateTime updatedAt;

}
