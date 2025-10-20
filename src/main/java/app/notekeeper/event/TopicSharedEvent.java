package app.notekeeper.event;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TopicSharedEvent {

    private final UUID topicId;
    private final UUID sharedWithUserId;
    private final UUID sharedByUserId;

}
