package app.notekeeper.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import app.notekeeper.model.dto.request.FileUploadRequest;
import app.notekeeper.model.dto.request.NoteUpdateRequest;
import app.notekeeper.model.dto.request.RetrieveNoteRequest;
import app.notekeeper.model.dto.request.TextUploadRequest;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.model.dto.response.NoteResponse;
import app.notekeeper.model.dto.response.RetrieveNoteResponse;
import app.notekeeper.model.enums.NoteType;
import app.notekeeper.security.SecurityUtils;
import app.notekeeper.service.AIService;
import app.notekeeper.service.IOService;
import app.notekeeper.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Note Management", description = "APIs for managing notes (upload files and text)")
public class NoteController {

        private final IOService ioService;
        private final NoteService noteService;
        private final AIService aiService;

        @PostMapping(value = "/upload/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Upload file (image or document)", description = "Upload an image or document file and create a note")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid file or validation error", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = JSendResponse.class)))
        })
        public ResponseEntity<JSendResponse<Void>> uploadFile(
                        @Parameter(description = "File to upload (image or document)", required = true) @RequestPart("file") MultipartFile file,

                        @Parameter(description = "File type (IMAGE or DOCUMENT)", required = true) @RequestParam("type") FileUploadRequest.FileType type,

                        @Parameter(description = "Topic ID (optional)") @RequestParam(value = "topicId", required = false) UUID topicId,

                        @Parameter(description = "File title (optional)") @RequestParam(value = "title", required = false) String title,

                        @Parameter(description = "File description (optional)") @RequestParam(value = "description", required = false) String description) {

                log.info("File upload request received for: {}", file.getOriginalFilename());

                // Build FileUploadRequest from parameters
                FileUploadRequest fileUploadRequest = new FileUploadRequest();
                fileUploadRequest.setType(type);
                fileUploadRequest.setTopicId(topicId);
                fileUploadRequest.setTitle(title);
                fileUploadRequest.setDescription(description);

                JSendResponse<Void> response = ioService.uploadFile(file, fileUploadRequest);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/upload/text")
        @Operation(summary = "Upload text note", description = "Create a text-based note")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Text note created successfully"),
                        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = JSendResponse.class)))
        })
        public ResponseEntity<JSendResponse<Void>> uploadText(
                        @RequestBody TextUploadRequest textUploadRequest) {

                log.info("Text upload request received for title: {}", textUploadRequest.getTitle());
                JSendResponse<Void> response = ioService.uploadText(textUploadRequest);
                return ResponseEntity.ok(response);
        }

        @GetMapping
        @Operation(summary = "Get notes list", description = "Get paginated list of notes with optional filters")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Notes retrieved successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = JSendResponse.class)))
        })
        public ResponseEntity<JSendResponse<Page<NoteResponse>>> getNotes(
                        @Parameter(description = "Topic ID filter (optional)") @RequestParam(required = false) UUID topicId,
                        @Parameter(description = "Note type filter (TEXT/IMAGE/DOCUMENT, optional)") @RequestParam(required = false) NoteType type,
                        @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") int size) {

                log.info("Get notes list request - topicId: {}, type: {}, page: {}, size: {}", topicId, type, page,
                                size);

                // Create pageable with sort by updatedAt descending
                Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

                JSendResponse<Page<NoteResponse>> response = noteService.getNotes(topicId, type, pageable);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/{noteId}")
        @Operation(summary = "Get note detail", description = "Retrieve note details by ID. For TEXT notes, returns content. For IMAGE/DOCUMENT notes, returns file URL.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Note retrieved successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Note not found", content = @Content(schema = @Schema(implementation = JSendResponse.class)))
        })
        public ResponseEntity<JSendResponse<NoteResponse>> getNoteDetail(
                        @Parameter(description = "Note ID", required = true) @PathVariable UUID noteId) {

                log.info("Get note detail request for ID: {}", noteId);
                JSendResponse<NoteResponse> response = noteService.getNoteDetail(noteId);
                return ResponseEntity.ok(response);
        }

        @PutMapping("/{noteId}")
        @Operation(summary = "Update text note", description = "Update a TEXT note (title and content)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Note updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid note type or validation error", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Note not found", content = @Content(schema = @Schema(implementation = JSendResponse.class)))
        })
        public ResponseEntity<JSendResponse<NoteResponse>> updateTextNote(
                        @Parameter(description = "Note ID", required = true) @PathVariable UUID noteId,
                        @RequestBody NoteUpdateRequest request) {

                log.info("Update text note request for ID: {}", noteId);
                JSendResponse<NoteResponse> response = noteService.updateTextNote(noteId, request);
                return ResponseEntity.ok(response);
        }

        @DeleteMapping("/{noteId}")
        @Operation(summary = "Delete note", description = "Delete a note (TEXT/IMAGE/DOCUMENT) and its associated file if applicable")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Note deleted successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Note not found", content = @Content(schema = @Schema(implementation = JSendResponse.class)))
        })
        public ResponseEntity<JSendResponse<Void>> deleteNote(
                        @Parameter(description = "Note ID", required = true) @PathVariable UUID noteId) {

                log.info("Delete note request for ID: {}", noteId);
                JSendResponse<Void> response = noteService.deleteNote(noteId);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/retrieve")
        @Operation(summary = "Retrieve notes with AI", description = "Search and retrieve relevant notes using AI-powered semantic search, then generate an answer based on the found notes")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Notes retrieved and answer generated successfully"),
                        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = JSendResponse.class)))
        })
        public ResponseEntity<JSendResponse<RetrieveNoteResponse>> retrieveNotes(
                        @RequestBody RetrieveNoteRequest request) {

                log.info("POST /api/v1/notes/retrieve - Retrieve notes request with query: '{}'", request.getQuery());

                // Get current authenticated user
                UUID currentUserId = SecurityUtils.getCurrentUserId();
                if (currentUserId == null) {
                        log.warn("Retrieve notes failed - no authenticated user");
                        throw app.notekeeper.common.exception.ServiceException
                                        .businessRuleViolation("Authentication required");
                }

                RetrieveNoteResponse response = aiService.retrieveNotes(request, currentUserId);

                log.info("POST /api/v1/notes/retrieve - Retrieved {} relevant notes and generated answer",
                                response.getNotesFound());

                return ResponseEntity.ok(
                                JSendResponse.success(response, "Notes retrieved and answer generated successfully"));
        }

}
