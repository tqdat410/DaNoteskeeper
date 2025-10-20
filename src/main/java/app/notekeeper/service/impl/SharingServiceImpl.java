package app.notekeeper.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.notekeeper.common.exception.ServiceException;
import app.notekeeper.model.dto.request.ShareNoteRequest;
import app.notekeeper.model.dto.request.ShareTopicRequest;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.model.dto.response.NoteQueryResponse;
import app.notekeeper.model.dto.response.NoteResponse;
import app.notekeeper.model.dto.response.SharedNoteResponse;
import app.notekeeper.model.dto.response.SharedTopicResponse;
import app.notekeeper.model.entity.Note;
import app.notekeeper.model.entity.SharedNote;
import app.notekeeper.model.entity.SharedTopic;
import app.notekeeper.model.entity.Topic;
import app.notekeeper.model.entity.User;
import app.notekeeper.model.enums.NotePerm;
import app.notekeeper.model.enums.TopicPerm;
import app.notekeeper.repository.NoteRepository;
import app.notekeeper.repository.SharedNoteRepository;
import app.notekeeper.repository.SharedTopicRepository;
import app.notekeeper.repository.TopicRepository;
import app.notekeeper.repository.UserRepository;
import app.notekeeper.security.SecurityUtils;
import app.notekeeper.service.SharingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SharingServiceImpl implements SharingService {

    private final SharedTopicRepository sharedTopicRepository;
    private final SharedNoteRepository sharedNoteRepository;
    private final TopicRepository topicRepository;
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Value("${app.deployment-url}")
    private String deploymentUrl;

    // ==================== TOPIC SHARING (Owner perspective) ====================

    @Override
    public JSendResponse<SharedTopicResponse> shareTopic(ShareTopicRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        Topic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> ServiceException.resourceNotFound("Topic not found"));

        if (!topic.getOwner().getId().equals(currentUserId)) {
            throw ServiceException.businessRuleViolation("You are not the owner of this topic");
        }

        User targetUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> ServiceException
                        .resourceNotFound("User with email '" + request.getEmail() + "' not found"));

        if (targetUser.getId().equals(currentUserId)) {
            throw ServiceException.businessRuleViolation("You cannot share a topic with yourself");
        }

        if (sharedTopicRepository.findByTopicIdAndUserId(topic.getId(), targetUser.getId()).isPresent()) {
            throw ServiceException.resourceConflict("Topic is already shared with this user");
        }

        SharedTopic sharedTopic = SharedTopic.builder()
                .topic(topic)
                .user(targetUser)
                .permission(TopicPerm.READ)
                .build();

        sharedTopicRepository.save(sharedTopic);

        log.info("Topic '{}' shared with user '{}' by owner '{}'",
                topic.getId(), targetUser.getEmail(), currentUserId);

        SharedTopicResponse response = buildSharedTopicResponse(sharedTopic);
        return JSendResponse.success(response, "Topic shared successfully");
    }

    @Override
    public JSendResponse<Void> unshareTopic(UUID sharedTopicId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        SharedTopic sharedTopic = sharedTopicRepository.findById(sharedTopicId)
                .orElseThrow(() -> ServiceException.resourceNotFound("Shared topic not found"));

        if (!sharedTopic.getTopic().getOwner().getId().equals(currentUserId)) {
            throw ServiceException.businessRuleViolation("You are not authorized to unshare this topic");
        }

        sharedTopicRepository.delete(sharedTopic);

        log.info("Unshared topic '{}' from user '{}' by owner '{}'",
                sharedTopic.getTopic().getId(), sharedTopic.getUser().getId(), currentUserId);

        return JSendResponse.success(null, "Topic unshared successfully");
    }

    @Override
    public JSendResponse<List<SharedTopicResponse>> getSharedUsers(UUID topicId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> ServiceException.resourceNotFound("Topic not found"));

        if (!topic.getOwner().getId().equals(currentUserId)) {
            throw ServiceException.businessRuleViolation("You are not the owner of this topic");
        }

        List<SharedTopic> sharedTopics = sharedTopicRepository.findByTopicId(topicId);

        List<SharedTopicResponse> responses = sharedTopics.stream()
                .map(this::buildSharedTopicResponse)
                .collect(Collectors.toList());

        log.info("Retrieved {} shared users for topic '{}'", responses.size(), topicId);

        return JSendResponse.success(responses, "Retrieved shared users successfully");
    }

    // ==================== NOTE SHARING (Owner perspective) ====================

    @Override
    public JSendResponse<SharedNoteResponse> shareNote(ShareNoteRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        // Use projection to avoid loading embedding field
        app.notekeeper.model.dto.response.NoteQueryResponse noteQuery = noteRepository
                .findNoteResponseById(request.getNoteId())
                .orElseThrow(() -> ServiceException.resourceNotFound("Note not found"));

        if (!noteQuery.getOwner().getId().equals(currentUserId)) {
            throw ServiceException.businessRuleViolation("You are not the owner of this note");
        }

        User targetUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> ServiceException
                        .resourceNotFound("User with email '" + request.getEmail() + "' not found"));

        if (targetUser.getId().equals(currentUserId)) {
            throw ServiceException.businessRuleViolation("You cannot share a note with yourself");
        }

        if (sharedNoteRepository.findByNoteIdAndUserId(noteQuery.getId(), targetUser.getId()).isPresent()) {
            throw ServiceException.resourceConflict("Note is already shared with this user");
        }

        // Use getReferenceById to create a proxy without loading embedding field
        // This is safe because we already validated the note exists via projection
        // query above
        Note note = noteRepository.getReferenceById(request.getNoteId());

        SharedNote sharedNote = SharedNote.builder()
                .note(note)
                .user(targetUser)
                .permission(NotePerm.READ)
                .build();

        sharedNoteRepository.save(sharedNote);

        log.info("Note '{}' shared with user '{}' by owner '{}'",
                noteQuery.getId(), targetUser.getEmail(), currentUserId);

        // Build response from projection data to avoid loading Note entity
        SharedNoteResponse response = SharedNoteResponse.builder()
                .id(sharedNote.getId())
                .noteId(noteQuery.getId())
                .noteTitle(noteQuery.getTitle())
                .noteDescription(noteQuery.getDescription())
                .noteType(noteQuery.getType())
                .userId(targetUser.getId())
                .userEmail(targetUser.getEmail())
                .userDisplayName(targetUser.getDisplayName())
                .permission(NotePerm.READ)
                .createdAt(sharedNote.getCreatedAt())
                .build();

        return JSendResponse.success(response, "Note shared successfully");
    }

    @Override
    public JSendResponse<Void> unshareNote(UUID sharedNoteId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        // Get note ID without loading full Note entity with embedding
        UUID noteId = sharedNoteRepository.findNoteIdBySharedNoteId(sharedNoteId)
                .orElseThrow(() -> ServiceException.resourceNotFound("Shared note not found"));

        // Validate ownership using projection
        app.notekeeper.model.dto.response.NoteQueryResponse noteQuery = noteRepository
                .findNoteResponseById(noteId)
                .orElseThrow(() -> ServiceException.resourceNotFound("Note not found"));

        if (!noteQuery.getOwner().getId().equals(currentUserId)) {
            throw ServiceException.businessRuleViolation("You are not authorized to unshare this note");
        }

        // Use custom delete method to avoid loading SharedNote entity with
        // Note.embedding
        sharedNoteRepository.deleteByIdWithoutLoading(sharedNoteId);

        log.info("Unshared note '{}' by owner '{}'", noteId, currentUserId);

        return JSendResponse.success(null, "Note unshared successfully");
    }

    @Override
    public JSendResponse<List<SharedNoteResponse>> getSharedUsersForNote(UUID noteId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        // Use projection to avoid loading embedding field
        app.notekeeper.model.dto.response.NoteQueryResponse noteQuery = noteRepository
                .findNoteResponseById(noteId)
                .orElseThrow(() -> ServiceException.resourceNotFound("Note not found"));

        if (!noteQuery.getOwner().getId().equals(currentUserId)) {
            throw ServiceException.businessRuleViolation("You are not the owner of this note");
        }

        // Use projection query to avoid loading Note entity with embedding
        List<SharedNoteResponse> responses = sharedNoteRepository.findSharedNoteResponsesByNoteId(noteId);

        log.info("Retrieved {} shared users for note '{}'", responses.size(), noteId);

        return JSendResponse.success(responses, "Retrieved shared users successfully");
    }

    // ==================== SHARED WITH ME (Recipient perspective)
    // ====================

    @Override
    public JSendResponse<Page<SharedTopicResponse>> getTopicsSharedWithMe(Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        Page<SharedTopic> sharedTopics = sharedTopicRepository.findByUserId(currentUserId, pageable);

        // Use recipient-specific builder to show owner info
        Page<SharedTopicResponse> responses = sharedTopics.map(this::buildSharedTopicResponseForRecipient);

        log.info("Retrieved {} topics shared with user '{}'", responses.getTotalElements(), currentUserId);

        return JSendResponse.success(responses, "Retrieved shared topics successfully");
    }

    @Override
    public JSendResponse<SharedTopicResponse> getSharedTopicDetail(UUID topicId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        // Check if topic is shared with current user
        SharedTopic sharedTopic = sharedTopicRepository.findByTopicIdAndUserId(topicId, currentUserId)
                .orElseThrow(() -> ServiceException.businessRuleViolation("This topic is not shared with you"));

        // Build response showing owner info (recipient perspective)
        SharedTopicResponse response = buildSharedTopicResponseForRecipient(sharedTopic);

        log.info("Retrieved shared topic detail '{}' for user '{}'", topicId, currentUserId);

        return JSendResponse.success(response, "Retrieved topic detail successfully");
    }

    @Override
    public JSendResponse<Page<SharedNoteResponse>> getNotesSharedWithMe(Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        // Use projection query to avoid loading Note entity with embedding
        Page<SharedNoteResponse> responses = sharedNoteRepository.findSharedNoteResponsesByUserId(currentUserId,
                pageable);

        log.info("Retrieved {} notes shared with user '{}'", responses.getTotalElements(), currentUserId);

        return JSendResponse.success(responses, "Retrieved shared notes successfully");
    }

    @Override
    public JSendResponse<NoteResponse> getSharedNoteDetail(UUID noteId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        // Check if note is shared with current user (validation only, no entity
        // loading)
        if (!sharedNoteRepository.existsByNoteIdAndUserId(noteId, currentUserId)) {
            throw ServiceException.businessRuleViolation("This note is not shared with you");
        }

        // Use projection to avoid loading embedding field
        NoteQueryResponse noteQuery = noteRepository.findNoteResponseById(noteId)
                .orElseThrow(() -> ServiceException.resourceNotFound("Note not found"));

        NoteResponse response = buildNoteResponseFromQuery(noteQuery);
        log.info("Retrieved shared note detail '{}' for user '{}'", noteId, currentUserId);

        return JSendResponse.success(response, "Retrieved shared note successfully");
    }

    @Override
    public JSendResponse<Page<NoteResponse>> getNotesInSharedTopic(UUID topicId, Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        // Check if topic is shared with current user (validation only, no entity
        // loading)
        if (!sharedTopicRepository.existsByTopicIdAndUserId(topicId, currentUserId)) {
            throw ServiceException.businessRuleViolation("This topic is not shared with you");
        }

        // Get all notes in the topic using projection
        Page<NoteQueryResponse> noteQueries = noteRepository.findNotesByTopicId(topicId, pageable);
        Page<NoteResponse> noteResponses = noteQueries.map(this::buildNoteResponseFromQuery);

        log.info("Retrieved {} notes from shared topic '{}' for user '{}'", noteResponses.getNumberOfElements(),
                topicId, currentUserId);

        return JSendResponse.success(noteResponses, "Retrieved notes in shared topic successfully");
    }

    @Override
    public JSendResponse<NoteResponse> getNoteDetailInSharedTopic(UUID topicId, UUID noteId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw ServiceException.businessRuleViolation("User not authenticated");
        }

        // Check if topic is shared with current user (validation only, no entity
        // loading)
        if (!sharedTopicRepository.existsByTopicIdAndUserId(topicId, currentUserId)) {
            throw ServiceException.businessRuleViolation("This topic is not shared with you");
        }

        // Get note detail and validate it belongs to the topic
        NoteQueryResponse noteQuery = noteRepository.findNoteByIdAndTopicId(noteId, topicId)
                .orElseThrow(() -> ServiceException.resourceNotFound("Note not found in this topic"));

        NoteResponse response = buildNoteResponseFromQuery(noteQuery);
        log.info("Retrieved note '{}' from shared topic '{}' for user '{}'", noteId, topicId, currentUserId);

        return JSendResponse.success(response, "Retrieved note in shared topic successfully");
    }

    // ==================== HELPER METHODS ====================

    private NoteResponse buildNoteResponseFromQuery(NoteQueryResponse note) {
        NoteResponse.NoteResponseBuilder builder = NoteResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .description(note.getDescription())
                .type(note.getType())
                .ownerId(note.getOwner().getId())
                .ownerDisplayName(note.getOwner().getDisplayName())
                .topicId(note.getTopic() != null ? note.getTopic().getId() : null)
                .topicName(note.getTopic() != null ? note.getTopic().getName() : null)
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt());

        // Add content only for TEXT type
        if (note.getType() == app.notekeeper.model.enums.NoteType.TEXT) {
            builder.content(note.getContent());
        }

        // Add fileUrl only for IMAGE/DOCUMENT type with deployment URL prefix
        if (note.getType() == app.notekeeper.model.enums.NoteType.IMAGE ||
                note.getType() == app.notekeeper.model.enums.NoteType.DOCUMENT) {
            if (note.getFileUrl() != null) {
                builder.fileUrl(deploymentUrl + "/file/" + note.getFileUrl());
            }
        }

        return builder.build();
    }

    // ==================== HELPER METHODS ====================

    private SharedTopicResponse buildSharedTopicResponse(SharedTopic sharedTopic) {
        return SharedTopicResponse.builder()
                .id(sharedTopic.getId())
                .topicId(sharedTopic.getTopic().getId())
                .topicName(sharedTopic.getTopic().getName())
                .userId(sharedTopic.getUser().getId())
                .userEmail(sharedTopic.getUser().getEmail())
                .userDisplayName(sharedTopic.getUser().getDisplayName())
                .permission(sharedTopic.getPermission())
                .createdAt(sharedTopic.getCreatedAt())
                .build();
    }

    /**
     * Build SharedTopicResponse for "shared with me" views
     * Shows owner information instead of recipient
     */
    private SharedTopicResponse buildSharedTopicResponseForRecipient(SharedTopic sharedTopic) {
        return SharedTopicResponse.builder()
                .id(sharedTopic.getId())
                .topicId(sharedTopic.getTopic().getId())
                .topicName(sharedTopic.getTopic().getName())
                .userId(sharedTopic.getTopic().getOwner().getId())
                .userEmail(sharedTopic.getTopic().getOwner().getEmail())
                .userDisplayName(sharedTopic.getTopic().getOwner().getDisplayName())
                .permission(sharedTopic.getPermission())
                .createdAt(sharedTopic.getCreatedAt())
                .build();
    }
}
