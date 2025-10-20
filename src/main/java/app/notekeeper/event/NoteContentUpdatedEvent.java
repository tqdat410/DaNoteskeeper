package app.notekeeper.event;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class NoteContentUpdatedEvent {

    private final UUID noteId;
    private final String newContent;

}
