package app.notekeeper.external.ai.dto.response;

import java.util.UUID;

import lombok.Data;

@Data
public class ClassificationTopicResponse {

    private UUID topicId;

    private String aiSummary;

    private String content;

}
