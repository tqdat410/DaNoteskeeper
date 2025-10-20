package app.notekeeper.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.notekeeper.model.dto.request.ShareNoteRequest;
import app.notekeeper.model.dto.request.ShareTopicRequest;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.model.dto.response.NoteResponse;
import app.notekeeper.model.dto.response.SharedNoteResponse;
import app.notekeeper.model.dto.response.SharedTopicResponse;
import app.notekeeper.service.SharingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/sharing")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sharing Management", description = "APIs for managing topic and note sharing between users")
public class SharingController {

    private final SharingService sharingService;

    // ==================== TOPIC SHARING (Owner perspective) ====================

    @PostMapping("/topics")
    @Operation(summary = "Share a topic with another user", description = "Topic owner can share their topic with another user by email. Default permission is READ.")
    public ResponseEntity<JSendResponse<SharedTopicResponse>> shareTopic(
            @Valid @RequestBody ShareTopicRequest request) {

        log.info("POST /api/v1/sharing/topics - Share topic {} with email {}",
                request.getTopicId(), request.getEmail());

        JSendResponse<SharedTopicResponse> response = sharingService.shareTopic(request);

        log.info("POST /api/v1/sharing/topics - Topic shared successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/topics/{sharedTopicId}")
    @Operation(summary = "Unshare a topic", description = "Remove shared access to a topic. Only topic owner can unshare.")
    public ResponseEntity<JSendResponse<Void>> unshareTopic(
            @Parameter(description = "ID of the SharedTopic record to delete") @PathVariable UUID sharedTopicId) {

        log.info("DELETE /api/v1/sharing/topics/{} - Unshare topic", sharedTopicId);

        JSendResponse<Void> response = sharingService.unshareTopic(sharedTopicId);

        log.info("DELETE /api/v1/sharing/topics/{} - Topic unshared successfully", sharedTopicId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/topics/{topicId}/users")
    @Operation(summary = "Get all users who have access to a topic", description = "Retrieve list of users that a topic has been shared with. Only topic owner can view this.")
    public ResponseEntity<JSendResponse<List<SharedTopicResponse>>> getSharedUsers(
            @Parameter(description = "ID of the topic to get shared users") @PathVariable UUID topicId) {

        log.info("GET /api/v1/sharing/topics/{}/users - Get shared users", topicId);

        JSendResponse<List<SharedTopicResponse>> response = sharingService.getSharedUsers(topicId);

        log.info("GET /api/v1/sharing/topics/{}/users - Retrieved {} shared users",
                topicId, response.getData().size());
        return ResponseEntity.ok(response);
    }

    // ==================== NOTE SHARING (Owner perspective) ====================

    @PostMapping("/notes")
    @Operation(summary = "Share a note with another user", description = "Note owner can share their note with another user by email. Default permission is READ.")
    public ResponseEntity<JSendResponse<SharedNoteResponse>> shareNote(
            @Valid @RequestBody ShareNoteRequest request) {

        log.info("POST /api/v1/sharing/notes - Share note {} with email {}",
                request.getNoteId(), request.getEmail());

        JSendResponse<SharedNoteResponse> response = sharingService.shareNote(request);

        log.info("POST /api/v1/sharing/notes - Note shared successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/notes/{sharedNoteId}")
    @Operation(summary = "Unshare a note", description = "Remove shared access to a note. Only note owner can unshare.")
    public ResponseEntity<JSendResponse<Void>> unshareNote(
            @Parameter(description = "ID of the SharedNote record to delete") @PathVariable UUID sharedNoteId) {

        log.info("DELETE /api/v1/sharing/notes/{} - Unshare note", sharedNoteId);

        JSendResponse<Void> response = sharingService.unshareNote(sharedNoteId);

        log.info("DELETE /api/v1/sharing/notes/{} - Note unshared successfully", sharedNoteId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/notes/{noteId}/users")
    @Operation(summary = "Get all users who have access to a note", description = "Retrieve list of users that a note has been shared with. Only note owner can view this.")
    public ResponseEntity<JSendResponse<List<SharedNoteResponse>>> getSharedUsersForNote(
            @Parameter(description = "ID of the note to get shared users") @PathVariable UUID noteId) {

        log.info("GET /api/v1/sharing/notes/{}/users - Get shared users", noteId);

        JSendResponse<List<SharedNoteResponse>> response = sharingService.getSharedUsersForNote(noteId);

        log.info("GET /api/v1/sharing/notes/{}/users - Retrieved {} shared users",
                noteId, response.getData().size());
        return ResponseEntity.ok(response);
    }

    // ==================== SHARED WITH ME (Recipient perspective)
    // ====================

    @GetMapping("/topics/shared-with-me")
    @Operation(summary = "Get topics shared with me", description = "Retrieve paginated list of topics that have been shared with the current user.")
    public ResponseEntity<JSendResponse<Page<SharedTopicResponse>>> getTopicsSharedWithMe(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/v1/sharing/topics/shared-with-me - Page: {}, Size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        JSendResponse<Page<SharedTopicResponse>> response = sharingService.getTopicsSharedWithMe(pageable);

        log.info("GET /api/v1/sharing/topics/shared-with-me - Retrieved {} topics",
                response.getData().getTotalElements());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/topics/shared-with-me/{topicId}")
    @Operation(summary = "Get shared topic detail", description = "Retrieve details of a specific topic that has been shared with the current user.")
    public ResponseEntity<JSendResponse<SharedTopicResponse>> getSharedTopicDetail(
            @Parameter(description = "ID of the shared topic") @PathVariable UUID topicId) {

        log.info("GET /api/v1/sharing/topics/shared-with-me/{} - Get topic detail", topicId);

        JSendResponse<SharedTopicResponse> response = sharingService.getSharedTopicDetail(topicId);

        log.info("GET /api/v1/sharing/topics/shared-with-me/{} - Retrieved topic detail", topicId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/notes/shared-with-me")
    @Operation(summary = "Get notes shared with me", description = "Retrieve paginated list of notes that have been shared with the current user.")
    public ResponseEntity<JSendResponse<Page<SharedNoteResponse>>> getNotesSharedWithMe(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/v1/sharing/notes/shared-with-me - Page: {}, Size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        JSendResponse<Page<SharedNoteResponse>> response = sharingService.getNotesSharedWithMe(pageable);

        log.info("GET /api/v1/sharing/notes/shared-with-me - Retrieved {} notes",
                response.getData().getTotalElements());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/notes/shared-with-me/{noteId}")
    @Operation(summary = "Get shared note detail", description = "Retrieve details of a note that has been shared with the current user. Returns content for TEXT notes, file URL for IMAGE/DOCUMENT notes.")
    public ResponseEntity<JSendResponse<NoteResponse>> getSharedNoteDetail(
            @Parameter(description = "ID of the shared note") @PathVariable UUID noteId) {

        log.info("GET /api/v1/sharing/notes/shared-with-me/{} - Get note detail", noteId);

        JSendResponse<NoteResponse> response = sharingService.getSharedNoteDetail(noteId);

        log.info("GET /api/v1/sharing/notes/shared-with-me/{} - Retrieved note detail", noteId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/topics/shared-with-me/{topicId}/notes")
    @Operation(summary = "Get notes in shared topic", description = "Retrieve paginated list of all notes in a topic that has been shared with the current user.")
    public ResponseEntity<JSendResponse<Page<NoteResponse>>> getNotesInSharedTopic(
            @Parameter(description = "ID of the shared topic") @PathVariable UUID topicId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/v1/sharing/topics/shared-with-me/{}/notes - Page: {}, Size: {}", topicId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        JSendResponse<Page<NoteResponse>> response = sharingService.getNotesInSharedTopic(topicId, pageable);

        log.info("GET /api/v1/sharing/topics/shared-with-me/{}/notes - Retrieved {} notes",
                topicId, response.getData().getTotalElements());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/topics/shared-with-me/{topicId}/notes/{noteId}")
    @Operation(summary = "Get note detail in shared topic", description = "Retrieve details of a specific note in a topic that has been shared with the current user. Returns content for TEXT notes, file URL for IMAGE/DOCUMENT notes.")
    public ResponseEntity<JSendResponse<NoteResponse>> getNoteDetailInSharedTopic(
            @Parameter(description = "ID of the shared topic") @PathVariable UUID topicId,
            @Parameter(description = "ID of the note") @PathVariable UUID noteId) {

        log.info("GET /api/v1/sharing/topics/shared-with-me/{}/notes/{} - Get note detail in shared topic", topicId,
                noteId);

        JSendResponse<NoteResponse> response = sharingService.getNoteDetailInSharedTopic(topicId, noteId);

        log.info("GET /api/v1/sharing/topics/shared-with-me/{}/notes/{} - Retrieved note detail", topicId, noteId);
        return ResponseEntity.ok(response);
    }
}
