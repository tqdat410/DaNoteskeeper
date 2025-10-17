package app.notekeeper.controller;

import app.notekeeper.model.dto.request.TopicCreateRequest;
import app.notekeeper.model.dto.request.TopicUpdateRequest;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.model.dto.response.TopicResponse;
import app.notekeeper.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
@Tag(name = "Topic Management", description = "APIs for creating, viewing and updating topics")
public class TopicController {

    private final TopicService topicService;

    /**
     * 🟢 Create Topic
     * POST /api/v1/topics
     */
    @PostMapping
    @Operation(summary = "Create a new topic", description = "Create topic for the currently logged in user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Topic created successfully",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class)))
    })
    public ResponseEntity<JSendResponse<TopicResponse>> createTopic(@RequestBody TopicCreateRequest request) {
        JSendResponse<TopicResponse> response = topicService.createTopic(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 🟡 View Topic by ID
     * GET /api/v1/topics/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get topic by ID", description = "Retrieve topic details if user is the owner")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Topic retrieved successfully",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class))),
            @ApiResponse(responseCode = "404", description = "Topic not found",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class)))
    })
    public ResponseEntity<JSendResponse<TopicResponse>> getTopicById(@PathVariable("id") UUID topicId) {
        JSendResponse<TopicResponse> response = topicService.getTopicById(topicId);
        return ResponseEntity.ok(response);
    }

    /**
     * 🟠 Update Topic
     * PUT /api/v1/topics/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update topic", description = "Update topic if user is the owner")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Topic updated successfully",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class))),
            @ApiResponse(responseCode = "404", description = "Topic not found",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class)))
    })
    public ResponseEntity<JSendResponse<TopicResponse>> updateTopic(
            @PathVariable("id") UUID topicId,
            @RequestBody TopicUpdateRequest request) {
        JSendResponse<TopicResponse> response = topicService.updateTopic(topicId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete topic", description = "Delete topic if user is the owner")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Topic deleted successfully",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class))),
            @ApiResponse(responseCode = "404", description = "Topic not found",
                    content = @Content(schema = @Schema(implementation = JSendResponse.class)))
    })
    public ResponseEntity<JSendResponse<Void>> deleteTopic(@PathVariable("id") UUID topicId) {
        JSendResponse<Void> response = topicService.deleteTopic(topicId);
        return ResponseEntity.ok(response);
    }
}
