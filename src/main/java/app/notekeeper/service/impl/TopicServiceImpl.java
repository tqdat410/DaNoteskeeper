package app.notekeeper.service.impl;

import app.notekeeper.common.exception.ServiceException;
import app.notekeeper.model.dto.request.TopicCreateRequest;
import app.notekeeper.model.dto.request.TopicUpdateRequest;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.model.dto.response.TopicResponse;
import app.notekeeper.model.entity.Topic;
import app.notekeeper.model.entity.User;
import app.notekeeper.repository.TopicRepository;
import app.notekeeper.repository.UserRepository;
import app.notekeeper.security.SecurityUtils;
import app.notekeeper.service.TopicService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TopicServiceImpl implements TopicService {

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

    @Override
    public JSendResponse<TopicResponse> createTopic(TopicCreateRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> ServiceException.resourceNotFound("Owner not found"));

        Topic topic = Topic.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .build();

        topicRepository.save(topic);

        TopicResponse response = TopicResponse.builder()
                .id(topic.getId())
                .name(topic.getName())
                .description(topic.getDescription())
                .ownerId(owner.getId())
                .ownerDisplayName(owner.getDisplayName())
                .build();

        return JSendResponse.success(response, "Create topic successfully");
    }

    @Override
    public JSendResponse<TopicResponse> getTopicById(UUID topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> ServiceException.resourceNotFound("Topic not found"));

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        if (!topic.getOwner().getId().equals(currentUserId)) {
            throw ServiceException.businessRuleViolation("You are not allowed to view this topic");
        }

        TopicResponse response = TopicResponse.builder()
                .id(topic.getId())
                .name(topic.getName())
                .description(topic.getDescription())
                .aiSummary(topic.getAiSummary())
                .ownerId(topic.getOwner().getId())
                .ownerDisplayName(topic.getOwner().getDisplayName())
                .build();

        return JSendResponse.success(response, "View topic successfully");
    }

    @Override
    public JSendResponse<TopicResponse> updateTopic(UUID topicId, TopicUpdateRequest request) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> ServiceException.resourceNotFound("Topic not found"));

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        if (!topic.getOwner().getId().equals(currentUserId)) {
            throw ServiceException.businessRuleViolation("You are not allowed to update this topic");
        }

        if (request.getName() != null)
            topic.setName(request.getName());
        if (request.getDescription() != null)
            topic.setDescription(request.getDescription());

        topicRepository.save(topic);

        TopicResponse response = TopicResponse.builder()
                .id(topic.getId())
                .name(topic.getName())
                .description(topic.getDescription())
                .ownerId(topic.getOwner().getId())
                .ownerDisplayName(topic.getOwner().getDisplayName())
                .build();

        return JSendResponse.success(response, "Update topic successfully");
    }

    @Override
    public JSendResponse<Void> deleteTopic(UUID topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> ServiceException.resourceNotFound("Topic not found"));

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        if (!topic.getOwner().getId().equals(currentUserId)) {
            throw ServiceException.businessRuleViolation("You are not allowed to delete this topic");
        }

        topicRepository.delete(topic);

        return JSendResponse.success(null, "Delete topic successfully");
    }

    @Override
    public void initDefaultTopic(UUID userId) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> ServiceException.resourceNotFound("User not found"));

        Topic defaultTopic = Topic.builder()
                .name("Default Topic")
                .description("This is the default topic.")
                .owner(owner)
                .isDefault(true)
                .build();

        topicRepository.save(defaultTopic);
    }

    @Override
    public JSendResponse<List<TopicResponse>> getAllTopicsByCurrentUser() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        List<Topic> topics = topicRepository.findByOwnerId(currentUserId);

        List<TopicResponse> responses = topics.stream()
                .map(topic -> TopicResponse.builder()
                        .id(topic.getId())
                        .name(topic.getName())
                        .description(topic.getDescription())
                        .aiSummary(topic.getAiSummary())
                        .ownerId(topic.getOwner().getId())
                        .ownerDisplayName(topic.getOwner().getDisplayName())
                        .build())
                .toList();

        return JSendResponse.success(responses, "View all topics successfully");
    }
}
