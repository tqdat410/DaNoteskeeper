package app.notekeeper.service;

import app.notekeeper.model.dto.request.TopicCreateRequest;
import app.notekeeper.model.dto.request.TopicUpdateRequest;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.model.dto.response.TopicResponse;

import java.util.List;
import java.util.UUID;

public interface TopicService {

    JSendResponse<TopicResponse> createTopic(TopicCreateRequest request);

    JSendResponse<TopicResponse> getTopicById(UUID topicId);

    JSendResponse<TopicResponse> updateTopic(UUID topicId, TopicUpdateRequest request);

    JSendResponse<Void> deleteTopic(UUID topicId);

    void initDefaultTopic(UUID userId);

    JSendResponse<List<TopicResponse>> getAllTopicsByCurrentUser();

}
