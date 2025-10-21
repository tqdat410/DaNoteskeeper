package app.notekeeper.model.dto.request;

import lombok.Data;


@Data
public class TopicCreateRequest {
    private String name;
    private String description;
}

