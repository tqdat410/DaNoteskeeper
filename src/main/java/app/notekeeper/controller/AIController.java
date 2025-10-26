package app.notekeeper.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.notekeeper.external.ai.OllamaService;
import app.notekeeper.model.dto.response.JSendResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Services", description = "APIs for AI-powered features")
public class AIController {

    private final OllamaService ollamaService;

    @PostMapping("/embedding")
    @Operation(summary = "Generate text embedding", description = "Generate embedding vector for text content using Ollama")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Embedding generated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or empty content", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = JSendResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = JSendResponse.class)))
    })
    public ResponseEntity<JSendResponse<EmbeddingResponse>> generateEmbedding(
            @Valid @RequestBody EmbeddingRequest request) {

        log.info("Generate embedding request - content length: {}", request.getContent().length());

        // Generate embedding
        float[] embedding = ollamaService.generateEmbedding(request.getContent());

        if (embedding == null) {
            log.warn("Failed to generate embedding for content");
            return ResponseEntity.ok(JSendResponse.error("Failed to generate embedding", 500));
        }

        // Build response
        EmbeddingResponse response = new EmbeddingResponse();
        response.setEmbedding(embedding);
        response.setDimensions(embedding.length);

        log.info("Successfully generated embedding with {} dimensions", embedding.length);
        return ResponseEntity.ok(JSendResponse.success(response, "Embedding generated successfully"));
    }

    // ========== Inner DTOs ==========

    @Data
    public static class EmbeddingRequest {
        @NotBlank(message = "Content is required")
        private String content;
    }

    @Data
    public static class EmbeddingResponse {
        private float[] embedding;
        private int dimensions;
    }
}
