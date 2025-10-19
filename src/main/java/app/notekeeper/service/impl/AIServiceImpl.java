package app.notekeeper.service.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import app.notekeeper.common.exception.ServiceException;
import app.notekeeper.common.exception.SystemException;
import app.notekeeper.event.NoteCreatedEvent;
import app.notekeeper.external.ai.OllamaService;
import app.notekeeper.external.ai.OpenAIService;
import app.notekeeper.model.dto.request.RetrieveNoteRequest;
import app.notekeeper.model.dto.response.NoteQueryResponse;
import app.notekeeper.model.dto.response.NoteResponse;
import app.notekeeper.model.dto.response.RetrieveNoteResponse;
import app.notekeeper.model.entity.Note;
import app.notekeeper.model.entity.Topic;
import app.notekeeper.model.entity.User;
import app.notekeeper.model.enums.NoteType;
import app.notekeeper.repository.NoteRepository;
import app.notekeeper.repository.TopicRepository;
import app.notekeeper.repository.UserRepository;
import app.notekeeper.service.AIService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@AllArgsConstructor
public class AIServiceImpl implements AIService {

    private final OpenAIService openAIService;
    private final OllamaService ollamaService;
    private final NoteRepository noteRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

    @Override
    @TransactionalEventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processNote(NoteCreatedEvent event) {
        try {
            log.info("Starting async note processing (classify + embed) for note ID: {}", event.getNoteId());

            // Load note once
            Note note = noteRepository.findById(event.getNoteId())
                    .orElseThrow(() -> new IllegalArgumentException("Note not found with ID: " + event.getNoteId()));

            log.info("Processing note: {} (type: {}, owner: {})",
                    event.getNoteId(), note.getType(), note.getOwner().getId());

            // Step 1: Classify and extract content
            ClassificationResult classificationResult = performClassification(note);

            if (classificationResult == null) {
                log.warn("Classification skipped for note: {}", event.getNoteId());
                return;
            }

            // Step 2: Generate embedding from content
            float[] embedding = performEmbedding(note, classificationResult.extractedContent);

            // Step 3: Save everything in one update
            saveNoteProcessingResult(event.getNoteId(), note.getType(), classificationResult, embedding);

            log.info("Note processing completed successfully for note ID: {}", event.getNoteId());
        } catch (Exception e) {
            log.error("Failed to process note with ID: {}", event.getNoteId(), e);
            // Don't throw - async method, just log error
        }
    }

    /**
     * Helper class to hold classification results
     */
    private static class ClassificationResult {
        Topic selectedTopic;
        String aiSummary;
        String extractedContent; // Only for IMAGE/DOCUMENT types

        ClassificationResult(Topic selectedTopic, String aiSummary, String extractedContent) {
            this.selectedTopic = selectedTopic;
            this.aiSummary = aiSummary;
            this.extractedContent = extractedContent;
        }
    }

    private ClassificationResult performClassification(Note note) {
        try {
            // Get all topics of the note owner
            List<Topic> userTopics = topicRepository.findAll().stream()
                    .filter(topic -> topic.getOwner().getId().equals(note.getOwner().getId()))
                    .toList();

            if (userTopics.isEmpty()) {
                log.warn("No topics found for user: {}, skipping classification", note.getOwner().getId());
                return null;
            }

            log.info("Found {} topics for classification", userTopics.size());

            // Call OpenAI to classify note, get summary, and extract content (for
            // IMAGE/DOCUMENT)
            app.notekeeper.external.ai.dto.response.ClassificationTopicResponse classificationResponse = openAIService
                    .classifyNote(note, userTopics);

            UUID selectedTopicId = classificationResponse.getTopicId();
            String aiSummary = classificationResponse.getAiSummary();
            String extractedContent = classificationResponse.getContent();

            if (selectedTopicId == null) {
                log.warn("OpenAI returned null topic ID, using default topic");
                selectedTopicId = getDefaultTopicId(userTopics);
            }

            log.info("Classification result - Topic ID: {}, Summary: {}, Content extracted: {}",
                    selectedTopicId, aiSummary, extractedContent != null && !extractedContent.isEmpty());

            // Validate selected topic exists and belongs to user
            UUID finalTopicId = selectedTopicId;
            Topic selectedTopic = userTopics.stream()
                    .filter(t -> t.getId().equals(finalTopicId))
                    .findFirst()
                    .orElseGet(() -> {
                        log.warn("Selected topic {} not found in user's topics, using default", finalTopicId);
                        return getDefaultTopic(userTopics);
                    });

            return new ClassificationResult(selectedTopic, aiSummary, extractedContent);

        } catch (Exception e) {
            log.error("Error during classification", e);
            throw e;
        }
    }

    private float[] performEmbedding(Note note, String extractedContent) {
        try {
            // Determine content to embed
            String contentToEmbed = null;

            if (note.getType() == NoteType.TEXT) {
                contentToEmbed = note.getContent();
            } else if (note.getType() == NoteType.IMAGE || note.getType() == NoteType.DOCUMENT) {
                // Use extracted content from classification
                contentToEmbed = extractedContent;
            }

            if (contentToEmbed == null || contentToEmbed.trim().isEmpty()) {
                log.warn("Note {} has no content to embed, skipping embedding generation", note.getId());
                return null;
            }

            log.info("Generating embedding for note {} (content length: {} chars)",
                    note.getId(), contentToEmbed.length());

            // Generate embedding from content
            float[] embedding = ollamaService.generateEmbedding(contentToEmbed);

            if (embedding == null) {
                log.warn("Failed to generate embedding for note: {}, embedding is null", note.getId());
                return null;
            }

            log.info("Generated embedding for note {}: [Vector with {} dimensions]",
                    note.getId(), embedding.length);

            return embedding;

        } catch (Exception e) {
            log.error("Error during embedding generation", e);
            // Don't throw, allow note to be saved without embedding
            return null;
        }
    }

    private void saveNoteProcessingResult(UUID noteId, NoteType noteType,
            ClassificationResult classification, float[] embedding) {
        try {
            // Save based on what we have
            boolean hasContent = classification.extractedContent != null
                    && !classification.extractedContent.trim().isEmpty()
                    && noteType != NoteType.TEXT;
            boolean hasEmbedding = embedding != null;

            if (hasContent && hasEmbedding) {
                // Save all: topic, summary, content, embedding
                noteRepository.updateAll(noteId, classification.selectedTopic,
                        classification.aiSummary, classification.extractedContent, embedding);
                log.info("Updated note {} with topic, summary, content, and embedding", noteId);
            } else if (hasContent) {
                // Save: topic, summary, content (no embedding)
                noteRepository.updateClassificationWithContent(noteId, classification.selectedTopic,
                        classification.aiSummary, classification.extractedContent);
                log.info("Updated note {} with topic, summary, and content", noteId);
            } else if (hasEmbedding) {
                // Save: topic, summary, embedding (TEXT type)
                noteRepository.updateClassificationAndEmbedding(noteId, classification.selectedTopic,
                        classification.aiSummary, embedding);
                log.info("Updated note {} with topic, summary, and embedding", noteId);
            } else {
                // Save: topic, summary only
                noteRepository.updateClassification(noteId, classification.selectedTopic,
                        classification.aiSummary);
                log.info("Updated note {} with topic and summary only", noteId);
            }

        } catch (Exception e) {
            log.error("Error saving note processing result for note: {}", noteId, e);
            throw e;
        }
    }

    private UUID getDefaultTopicId(List<Topic> topics) {
        return getDefaultTopic(topics).getId();
    }

    private Topic getDefaultTopic(List<Topic> topics) {
        return topics.stream()
                .filter(topic -> topic.isDefault())
                .findFirst()
                .orElse(topics.get(0)); // Fallback to first topic if no default found
    }

    @Override
    @Transactional(readOnly = true)
    public RetrieveNoteResponse retrieveNotes(RetrieveNoteRequest request, UUID userId) {
        try {
            log.info("Retrieving notes for user {} with query: '{}'", userId, request.getQuery());

            // Step 1: Generate embedding from query
            float[] queryEmbedding = ollamaService.generateEmbedding(request.getQuery());

            if (queryEmbedding == null) {
                log.warn("Failed to generate embedding for query: {}", request.getQuery());
                throw SystemException.systemError("Failed to process your query. Please try again.");
            }

            // Step 2: Convert embedding to PostgreSQL vector format
            String embeddingString = convertEmbeddingToString(queryEmbedding);

            // Step 3: Search for similar notes using vector similarity
            List<Object[]> similarNotesRaw = noteRepository.findSimilarNotes(
                    userId,
                    request.getTopicId(),
                    embeddingString,
                    5); // Limit to top 5 most similar notes

            log.info("Found {} similar notes", similarNotesRaw.size());

            if (similarNotesRaw.isEmpty()) {
                return RetrieveNoteResponse.builder()
                        .answer("I couldn't find any relevant notes to answer your question. Please try a different query or create more notes on this topic.")
                        .relevantNotes(new ArrayList<>())
                        .notesFound(0)
                        .build();
            }

            // Step 4: Convert raw results to NoteQueryResponse
            List<NoteQueryResponse> relevantNotes = convertToNoteQueryResponses(similarNotesRaw);

            // Step 5: Generate answer using LLM
            String answer = openAIService.generateAnswerFromNotes(request.getQuery(), relevantNotes);

            // Step 6: Convert to NoteResponse for API response
            List<NoteResponse> noteResponses = relevantNotes.stream()
                    .map(this::convertToNoteResponse)
                    .toList();

            log.info("Successfully generated answer for query: '{}'", request.getQuery());

            return RetrieveNoteResponse.builder()
                    .answer(answer)
                    .relevantNotes(noteResponses)
                    .notesFound(noteResponses.size())
                    .build();

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving notes for query: {}", request.getQuery(), e);
            throw SystemException.systemError("Failed to retrieve notes. Please try again.");
        }
    }

    private String convertEmbeddingToString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private List<NoteQueryResponse> convertToNoteQueryResponses(List<Object[]> rawResults) {
        List<NoteQueryResponse> notes = new ArrayList<>();

        for (Object[] row : rawResults) {
            try {
                NoteQueryResponse note = new NoteQueryResponse();

                // Map columns: id, owner_id, topic_id, title, description, content, ai_summary,
                // type, file_url, created_at, updated_at
                note.setId((UUID) row[0]);

                // Load owner entity
                UUID ownerId = (UUID) row[1];
                User owner = userRepository.findById(ownerId).orElse(null);
                note.setOwner(owner);

                // Load topic entity if present
                if (row[2] != null) {
                    UUID topicId = (UUID) row[2];
                    Topic topic = topicRepository.findById(topicId).orElse(null);
                    note.setTopic(topic);
                }

                note.setTitle((String) row[3]);
                note.setDescription((String) row[4]);
                note.setContent((String) row[5]);
                note.setAiSummary((String) row[6]);
                note.setType(NoteType.valueOf((String) row[7]));
                note.setFileUrl((String) row[8]);

                // Convert Instant to ZonedDateTime
                if (row[9] != null) {
                    Instant createdAt = (Instant) row[9];
                    note.setCreatedAt(ZonedDateTime.ofInstant(createdAt, ZoneId.systemDefault()));
                }

                if (row[10] != null) {
                    Instant updatedAt = (Instant) row[10];
                    note.setUpdatedAt(ZonedDateTime.ofInstant(updatedAt, ZoneId.systemDefault()));
                }

                notes.add(note);
            } catch (Exception e) {
                log.error("Error converting raw result to NoteQueryResponse", e);
                // Skip this note and continue with others
            }
        }

        return notes;
    }

    private NoteResponse convertToNoteResponse(NoteQueryResponse note) {
        return NoteResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .description(note.getDescription())
                .type(note.getType())
                .ownerId(note.getOwner() != null ? note.getOwner().getId() : null)
                .ownerDisplayName(note.getOwner() != null ? note.getOwner().getDisplayName() : null)
                .topicId(note.getTopic() != null ? note.getTopic().getId() : null)
                .topicName(note.getTopic() != null ? note.getTopic().getName() : null)
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }

}
