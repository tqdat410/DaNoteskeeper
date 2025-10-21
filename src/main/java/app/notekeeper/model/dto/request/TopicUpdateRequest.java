package app.notekeeper.model.dto.request;

import lombok.Data;

@Data
public class TopicUpdateRequest {
    private String name;
    private String description;
}
