package app.notekeeper.event;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class NoteSharedEvent {

    private final UUID noteId;
    private final UUID sharedWithUserId;
    private final UUID sharedByUserId;

}
