package app.notekeeper.service.impl;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.notekeeper.common.exception.ServiceException;
import app.notekeeper.common.exception.SystemException;
import app.notekeeper.common.exception.ValidationException;
import app.notekeeper.event.NoteContentUpdatedEvent;
import app.notekeeper.model.dto.request.NoteUpdateRequest;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.model.dto.response.NoteQueryResponse;
import app.notekeeper.model.dto.response.NoteResponse;
import app.notekeeper.model.enums.NoteType;
import app.notekeeper.repository.NoteRepository;
import app.notekeeper.security.SecurityUtils;
import app.notekeeper.service.IOService;
import app.notekeeper.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NoteServiceImpl implements NoteService {

    private final NoteRepository noteRepository;
    private final IOService ioService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.deployment-url}")
    private String deploymentUrl;

    @Override
    public JSendResponse<NoteResponse> getNoteDetail(UUID noteId) {
        try {
            log.info("Getting note detail with ID: {}", noteId);

            NoteQueryResponse note = noteRepository.findNoteResponseById(noteId)
                    .orElseThrow(() -> ServiceException.resourceNotFound("Note not found with ID: " + noteId));

            // Verify ownership
            UUID currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId == null) {
                throw ServiceException.businessRuleViolation("Authentication required");
            }

            if (!note.getOwner().getId().equals(currentUserId)) {
                throw ServiceException.businessRuleViolation("You are not allowed to view this note");
            }

            NoteResponse response = buildNoteResponseFromQuery(note);
            log.info("Note detail retrieved successfully: {}", noteId);

            return JSendResponse.success(response, "Note retrieved successfully");

        } catch (ServiceException | ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get note detail: {}", noteId, e);
            throw SystemException.systemError("Failed to retrieve note");
        }
    }

    @Override
    public JSendResponse<NoteResponse> updateTextNote(UUID noteId, NoteUpdateRequest request) {
        try {
            log.info("Updating text note with ID: {}", noteId);

            // Use projection to avoid loading embedding field
            NoteQueryResponse noteQuery = noteRepository.findNoteResponseById(noteId)
                    .orElseThrow(() -> ServiceException.resourceNotFound("Note not found with ID: " + noteId));

            // Verify ownership
            UUID currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId == null) {
                throw ServiceException.businessRuleViolation("Authentication required");
            }

            if (!noteQuery.getOwner().getId().equals(currentUserId)) {
                throw ServiceException.businessRuleViolation("You are not allowed to update this note");
            }

            // Only TEXT type can be updated
            if (noteQuery.getType() != NoteType.TEXT) {
                throw ValidationException.invalidFormat(
                        java.util.Map.of("type", "Only TEXT notes can be updated"));
            }

            // Update fields using update query (avoids loading entity)
            String newTitle = request.getTitle() != null ? request.getTitle() : noteQuery.getTitle();
            String newContent = request.getContent() != null ? request.getContent() : noteQuery.getContent();

            // Check if content has actually changed
            boolean contentChanged = request.getContent() != null
                    && !request.getContent().equals(noteQuery.getContent());

            noteRepository.updateTitleAndContent(noteId, newTitle, newContent);
            log.info("Text note updated successfully: {}", noteId);

            // Publish event to regenerate embedding if content changed
            if (contentChanged) {
                log.info("Content changed for note {}, triggering embedding update", noteId);
                eventPublisher.publishEvent(new NoteContentUpdatedEvent(noteId, newContent));
            }

            // Fetch updated note to return response
            NoteQueryResponse updatedNote = noteRepository.findNoteResponseById(noteId)
                    .orElseThrow(() -> ServiceException.resourceNotFound("Note not found after update"));

            NoteResponse response = buildNoteResponseFromQuery(updatedNote);
            return JSendResponse.success(response, "Note updated successfully");

        } catch (ServiceException | ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update text note: {}", noteId, e);
            throw SystemException.systemError("Failed to update note");
        }
    }

    @Override
    public JSendResponse<Void> deleteNote(UUID noteId) {
        try {
            log.info("Deleting note with ID: {}", noteId);

            // Use projection to avoid loading embedding field
            NoteQueryResponse noteQuery = noteRepository.findNoteResponseById(noteId)
                    .orElseThrow(() -> ServiceException.resourceNotFound("Note not found with ID: " + noteId));

            // Verify ownership
            UUID currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId == null) {
                throw ServiceException.businessRuleViolation("Authentication required");
            }

            if (!noteQuery.getOwner().getId().equals(currentUserId)) {
                throw ServiceException.businessRuleViolation("You are not allowed to delete this note");
            }

            // Delete file if it's IMAGE or DOCUMENT type
            if ((noteQuery.getType() == NoteType.IMAGE || noteQuery.getType() == NoteType.DOCUMENT)
                    && noteQuery.getFileUrl() != null) {
                ioService.deleteFile(noteQuery.getFileUrl());
                log.info("Associated file deleted: {}", noteQuery.getFileUrl());
            }

            // Delete note from database using custom query (no entity loading needed)
            noteRepository.deleteNoteById(noteId);
            log.info("Note deleted successfully: {}", noteId);

            return JSendResponse.success(null, "Note deleted successfully");

        } catch (ServiceException | ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete note: {}", noteId, e);
            throw SystemException.systemError("Failed to delete note");
        }
    }

    @Override
    public JSendResponse<Page<NoteResponse>> getNotes(UUID topicId, NoteType type, Pageable pageable) {
        try {
            log.info("Getting notes list with filters - topicId: {}, type: {}, page: {}, size: {}",
                    topicId, type, pageable.getPageNumber(), pageable.getPageSize());

            // Get current authenticated user
            UUID currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId == null) {
                throw ServiceException.businessRuleViolation("Authentication required");
            }

            // Fetch notes with filters and pagination
            Page<NoteQueryResponse> notesPage = noteRepository.findNotesByOwnerWithFilters(
                    currentUserId, topicId, type, pageable);

            // Convert to response DTOs
            Page<NoteResponse> responsePage = notesPage.map(this::buildNoteResponseFromQuery);

            log.info("Notes retrieved successfully - total elements: {}, total pages: {}",
                    responsePage.getTotalElements(), responsePage.getTotalPages());

            return JSendResponse.success(responsePage, "Notes retrieved successfully");

        } catch (ServiceException | ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get notes list", e);
            throw SystemException.systemError("Failed to retrieve notes");
        }
    }

    /**
     * Build NoteResponse from NoteQueryResponse (projection without embedding)
     */
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
        if (note.getType() == NoteType.TEXT) {
            builder.content(note.getContent());
        }

        // Add fileUrl only for IMAGE/DOCUMENT type with deployment URL prefix
        if (note.getType() == NoteType.IMAGE || note.getType() == NoteType.DOCUMENT) {
            if (note.getFileUrl() != null) {
                builder.fileUrl(deploymentUrl + "/file/" + note.getFileUrl());
            }
        }

        return builder.build();
    }

}
