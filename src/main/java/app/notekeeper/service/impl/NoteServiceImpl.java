package app.notekeeper.service.impl;

import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.notekeeper.common.exception.ServiceException;
import app.notekeeper.common.exception.SystemException;
import app.notekeeper.common.exception.ValidationException;
import app.notekeeper.model.dto.request.NoteUpdateRequest;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.model.dto.response.NoteResponse;
import app.notekeeper.model.entity.Note;
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

    @Override
    public JSendResponse<NoteResponse> getTextNote(UUID noteId) {
        try {
            log.info("Getting text note with ID: {}", noteId);

            Note note = noteRepository.findById(noteId)
                    .orElseThrow(() -> ServiceException.resourceNotFound("Note not found with ID: " + noteId));

            // Verify ownership
            UUID currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId == null) {
                throw ServiceException.businessRuleViolation("Authentication required");
            }

            if (!note.getOwner().getId().equals(currentUserId)) {
                throw ServiceException.businessRuleViolation("You are not allowed to view this note");
            }

            // Only TEXT type can be retrieved this way
            if (note.getType() != NoteType.TEXT) {
                throw ValidationException.invalidFormat(
                        java.util.Map.of("type",
                                "This endpoint only supports TEXT notes. Use /stream for IMAGE/DOCUMENT"));
            }

            NoteResponse response = buildNoteResponse(note);
            log.info("Text note retrieved successfully: {}", noteId);

            return JSendResponse.success(response, "Note retrieved successfully");

        } catch (ServiceException | ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get text note: {}", noteId, e);
            throw SystemException.systemError("Failed to retrieve note");
        }
    }

    @Override
    public JSendResponse<NoteResponse> updateTextNote(UUID noteId, NoteUpdateRequest request) {
        try {
            log.info("Updating text note with ID: {}", noteId);

            Note note = noteRepository.findById(noteId)
                    .orElseThrow(() -> ServiceException.resourceNotFound("Note not found with ID: " + noteId));

            // Verify ownership
            UUID currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId == null) {
                throw ServiceException.businessRuleViolation("Authentication required");
            }

            if (!note.getOwner().getId().equals(currentUserId)) {
                throw ServiceException.businessRuleViolation("You are not allowed to update this note");
            }

            // Only TEXT type can be updated
            if (note.getType() != NoteType.TEXT) {
                throw ValidationException.invalidFormat(
                        java.util.Map.of("type", "Only TEXT notes can be updated"));
            }

            // Update fields
            if (request.getTitle() != null) {
                note.setTitle(request.getTitle());
            }
            if (request.getContent() != null) {
                note.setContent(request.getContent());
            }

            noteRepository.save(note);
            log.info("Text note updated successfully: {}", noteId);

            NoteResponse response = buildNoteResponse(note);
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

            Note note = noteRepository.findById(noteId)
                    .orElseThrow(() -> ServiceException.resourceNotFound("Note not found with ID: " + noteId));

            // Verify ownership
            UUID currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId == null) {
                throw ServiceException.businessRuleViolation("Authentication required");
            }

            if (!note.getOwner().getId().equals(currentUserId)) {
                throw ServiceException.businessRuleViolation("You are not allowed to delete this note");
            }

            // Delete file if it's IMAGE or DOCUMENT type
            if ((note.getType() == NoteType.IMAGE || note.getType() == NoteType.DOCUMENT)
                    && note.getFileUrl() != null) {
                ioService.deleteFile(note.getFileUrl());
                log.info("Associated file deleted: {}", note.getFileUrl());
            }

            // Delete note from database
            noteRepository.delete(note);
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
    public Resource streamFile(UUID noteId) {
        try {
            log.info("Streaming file for note ID: {}", noteId);

            Note note = noteRepository.findById(noteId)
                    .orElseThrow(() -> ServiceException.resourceNotFound("Note not found with ID: " + noteId));

            // Verify ownership
            UUID currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId == null) {
                throw ServiceException.businessRuleViolation("Authentication required");
            }

            if (!note.getOwner().getId().equals(currentUserId)) {
                throw ServiceException.businessRuleViolation("You are not allowed to view this note");
            }

            // Only IMAGE or DOCUMENT type can be streamed
            if (note.getType() != NoteType.IMAGE && note.getType() != NoteType.DOCUMENT) {
                throw ValidationException.invalidFormat(
                        java.util.Map.of("type", "Only IMAGE/DOCUMENT notes can be streamed"));
            }

            if (note.getFileUrl() == null) {
                throw ServiceException.resourceNotFound("File URL not found for note: " + noteId);
            }

            Resource resource = ioService.loadFileAsResource(note.getFileUrl());
            log.info("File streamed successfully for note: {}", noteId);

            return resource;

        } catch (ServiceException | ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to stream file for note: {}", noteId, e);
            throw SystemException.systemError("Failed to stream file");
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
            Page<Note> notesPage = noteRepository.findNotesByOwnerWithFilters(
                    currentUserId, topicId, type, pageable);

            // Convert to response DTOs
            Page<NoteResponse> responsePage = notesPage.map(this::buildNoteResponse);

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

    private NoteResponse buildNoteResponse(Note note) {
        return NoteResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .description(note.getDescription())
                .type(note.getType())
                .ownerId(note.getOwner().getId())
                .ownerDisplayName(note.getOwner().getDisplayName())
                .topicId(note.getTopic() != null ? note.getTopic().getId() : null)
                .topicName(note.getTopic() != null ? note.getTopic().getName() : null)
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }

}
