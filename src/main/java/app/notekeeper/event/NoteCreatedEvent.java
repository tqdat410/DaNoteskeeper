package app.notekeeper.event;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class NoteCreatedEvent {

    private final UUID noteId;

}
