package app.notekeeper.external.ai;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import app.notekeeper.external.ai.dto.response.ClassificationTopicResponse;
import app.notekeeper.model.dto.response.NoteQueryResponse;
import app.notekeeper.model.entity.Note;
import app.notekeeper.model.entity.Topic;
import app.notekeeper.model.enums.NoteType;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OpenAIService {

    private final ChatClient mainChatClient;
    @SuppressWarnings("unused") // Reserved for future advanced classification features
    private final ChatClient secondaryChatClient;

    @Value("${app.storage.upload-dir}")
    private String uploadDir;

    public OpenAIService(ChatClient mainChatClient, @Qualifier("powerfulChatClient") ChatClient secondaryChatClient) {
        this.mainChatClient = mainChatClient;
        this.secondaryChatClient = secondaryChatClient;
    }

    public ClassificationTopicResponse classifyNote(Note note, List<Topic> topics) {
        try {
            log.info("Classifying note type: {} with {} available topics", note.getType(), topics.size());

            String topicsInfo = prepareTopicsInfo(topics);
            String noteMetadata = prepareNoteMetadata(note);

            ClassificationTopicResponse response;

            // For IMAGE and DOCUMENT notes, use vision/document model with file data
            if ((note.getType() == NoteType.IMAGE || note.getType() == NoteType.DOCUMENT)
                    && note.getFileUrl() != null) {
                response = classifyWithFile(note, noteMetadata, topicsInfo);
            } else {
                // For TEXT notes, use text-only classification
                response = classifyWithText(note, noteMetadata, topicsInfo);
            }

            log.info("Note classified to topic ID: {} with summary: {}",
                    response.getTopicId(), response.getAiSummary());
            return response;

        } catch (Exception e) {
            log.error("Error during note classification", e);
            // Return default topic with error message as summary
            UUID defaultTopicId = topics.stream()
                    .filter(topic -> topic.isDefault())
                    .findFirst()
                    .map(Topic::getId)
                    .orElse(topics.get(0).getId());

            ClassificationTopicResponse errorResponse = new ClassificationTopicResponse();
            errorResponse.setTopicId(defaultTopicId);
            errorResponse.setAiSummary("Error during classification: " + e.getMessage());
            return errorResponse;
        }
    }

    private ClassificationTopicResponse classifyWithFile(Note note, String noteMetadata, String topicsInfo) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(note.getFileUrl()).normalize();

            if (!Files.exists(filePath)) {
                log.warn("File not found: {}, falling back to text classification", note.getFileUrl());
                return classifyWithText(note, noteMetadata, topicsInfo);
            }

            // Check file size (limit to 20MB for vision/document models)
            long fileSize = Files.size(filePath);
            long maxSize = note.getType() == NoteType.IMAGE ? 5 * 1024 * 1024 : 20 * 1024 * 1024;

            if (fileSize > maxSize) {
                log.warn("File too large: {} bytes (max: {} bytes), falling back to text classification",
                        fileSize, maxSize);
                return classifyWithText(note, noteMetadata, topicsInfo);
            }

            log.info("Classifying {} note with file analysis, file size: {} bytes",
                    note.getType(), fileSize);

            // Use Spring Resource to read file
            org.springframework.core.io.Resource fileResource = new org.springframework.core.io.FileSystemResource(
                    filePath);

            // Determine MIME type from file extension
            String mimeType = determineMimeType(note.getFileUrl());

            // Prepare system prompt based on file type
            String systemPrompt = note.getType() == NoteType.IMAGE
                    ? prepareImageSystemPrompt()
                    : prepareDocumentSystemPrompt();

            ClassificationTopicResponse response = mainChatClient.prompt()
                    .system(systemPrompt)
                    .user(u -> u
                            .text(String.format(
                                    """
                                            Analyze this %s and classify it into the most appropriate topic.
                                            Also create a brief summary and extract the content.

                                            %s

                                            %s

                                            IMPORTANT: Return ONLY a valid JSON object without any markdown formatting or code blocks.
                                            Do not wrap the JSON in ```json or ``` tags.
                                            The response must start with { and end with }.
                                            """,
                                    note.getType().toString().toLowerCase(), noteMetadata, topicsInfo))
                            .media(MimeTypeUtils.parseMimeType(mimeType), fileResource))
                    .options(OpenAiChatOptions.builder()
                            .maxTokens(2000)
                            .temperature(0.3)
                            .build())
                    .call()
                    .entity(ClassificationTopicResponse.class);

            return response;

        } catch (Exception e) {
            log.error("Error classifying with file, falling back to text classification", e);
            return classifyWithText(note, noteMetadata, topicsInfo);
        }
    }

    private ClassificationTopicResponse classifyWithText(Note note, String noteMetadata, String topicsInfo) {
        String contentInfo = prepareContentInfo(note);

        ClassificationTopicResponse response = mainChatClient.prompt()
                .system("""
                        You are an intelligent note classification assistant specialized in categorizing user notes.
                        Your task is to analyze note content and classify it into the most appropriate topic from a provided list.

                        CLASSIFICATION RULES:
                        1. Carefully analyze the note content and understand its main theme
                        2. Match the note to ONE topic that best fits its content
                        3. Consider topic name, description, and AI summary when making your decision
                        4. If no topic clearly matches the content, choose the DEFAULT topic
                        5. ALWAYS return a valid topic ID from the provided list
                        6. For TEXT notes: analyze the full text content
                        7. For DOCUMENT notes: rely on filename, title, and description
                        8. Prioritize exact matches over partial matches
                        9. When uncertain between two topics, choose the more general one

                        SUMMARY REQUIREMENTS:
                        Create a concise summary (2-3 sentences, max 200 characters) describing:
                        - The main idea or theme of the note
                        - Key points or information
                        - Context or purpose

                        OUTPUT FORMAT:
                        You must respond with a JSON object containing topicId and aiSummary fields.
                        CRITICAL: Return ONLY the raw JSON object without any markdown code blocks or formatting.
                        Do NOT wrap the response in ```json or ``` tags.
                        The response must start directly with { and end with }.

                        Example format (return exactly like this without any extra characters):
                        {
                          "topicId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                          "aiSummary": "Meeting notes discussing Q4 project deliverables and team assignments."
                        }
                        """)
                .user("""
                        Analyze the following note information and classify it into one of the available topics.
                        Also create a brief summary of the note content.

                        IMPORTANT: Return ONLY a valid JSON object without markdown formatting.
                        Do not use ```json or ``` code blocks.
                        Start your response with { and end with }.

                        {noteMetadata}

                        {contentInfo}

                        {topicsInfo}
                        """)
                .user(u -> u
                        .param("noteMetadata", noteMetadata)
                        .param("contentInfo", contentInfo)
                        .param("topicsInfo", topicsInfo))
                .options(OpenAiChatOptions.builder()
                        .maxTokens(2000)
                        .temperature(0.3)
                        .build())
                .call()
                .entity(ClassificationTopicResponse.class);

        return response;
    }

    private String prepareNoteMetadata(Note note) {
        StringBuilder metadata = new StringBuilder();
        metadata.append("NOTE METADATA:\n");
        metadata.append("=============\n");
        metadata.append(String.format("Type: %s\n", note.getType()));

        if (note.getTitle() != null && !note.getTitle().isEmpty()) {
            metadata.append(String.format("Title: %s\n", note.getTitle()));
        }

        if (note.getDescription() != null && !note.getDescription().isEmpty()) {
            metadata.append(String.format("Description: %s\n", note.getDescription()));
        }

        if (note.getFileUrl() != null) {
            metadata.append(String.format("Filename: %s\n", note.getFileUrl()));
        }

        return metadata.toString();
    }

    private String prepareContentInfo(Note note) {
        StringBuilder content = new StringBuilder();

        if (note.getType() == NoteType.TEXT && note.getContent() != null) {
            content.append("\nCONTENT:\n");
            content.append("========\n");
            content.append(note.getContent());
        }
        // For DOCUMENT and IMAGE, content will be analyzed from the actual file

        return content.toString();
    }

    private String prepareTopicsInfo(List<Topic> topics) {
        StringBuilder topicsInfo = new StringBuilder();
        topicsInfo.append("\nAVAILABLE TOPICS:\n");
        topicsInfo.append("=================\n\n");

        for (Topic topic : topics) {
            topicsInfo.append(String.format("ID: %s\n", topic.getId()));
            topicsInfo.append(String.format("Name: %s\n", topic.getName()));

            if (topic.getDescription() != null && !topic.getDescription().isEmpty()) {
                topicsInfo.append(String.format("Description: %s\n", topic.getDescription()));
            }

            if (topic.getAiSummary() != null && !topic.getAiSummary().isEmpty()) {
                topicsInfo.append(String.format("AI Summary: %s\n", topic.getAiSummary()));
            }

            topicsInfo.append(String.format("Default: %s\n", topic.isDefault() ? "YES" : "NO"));
            topicsInfo.append("\n");
        }

        topicsInfo.append("TASK:\n");
        topicsInfo.append("Select the most appropriate topic ID from the list above.\n");
        topicsInfo.append("If no topic clearly matches, choose the DEFAULT topic.\n");

        return topicsInfo.toString();
    }

    private String determineMimeType(String filename) {
        if (filename == null) {
            return "image/jpeg";
        }

        String lowerFilename = filename.toLowerCase();

        // Image types
        if (lowerFilename.endsWith(".png")) {
            return "image/png";
        } else if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFilename.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFilename.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerFilename.endsWith(".bmp")) {
            return "image/bmp";
        }
        // Document types
        else if (lowerFilename.endsWith(".pdf")) {
            return "application/pdf";
        }

        // Default to JPEG for images
        return "image/jpeg";
    }

    private String prepareImageSystemPrompt() {
        return """
                You are an intelligent note classification assistant with vision capabilities.
                Analyze both the image content and metadata to classify notes accurately.

                CLASSIFICATION RULES:
                1. Carefully analyze the image visual content to understand its main theme
                2. Consider the image context along with title and description
                3. Match the note to ONE topic that best fits its content
                4. Consider topic name, description, and AI summary when making your decision
                5. If no topic clearly matches, choose the DEFAULT topic
                6. ALWAYS return a valid topic ID from the provided list
                7. Prioritize visual content over text metadata when there's a clear match
                8. For ambiguous images, use metadata to help decide

                SUMMARY REQUIREMENTS:
                Create a concise summary (2-3 sentences, max 200 characters) describing:
                - What you see in the image
                - The main subject or theme
                - Key visual elements

                CONTENT EXTRACTION:
                Provide a detailed description of the image (3-5 sentences) that captures:
                - All visible elements and objects in the image
                - Colors, composition, and layout
                - Text or labels if present
                - Context and setting
                - Any notable details or patterns

                OUTPUT FORMAT:
                You must respond with a JSON object containing topicId, aiSummary, and content fields.
                CRITICAL: Return ONLY the raw JSON object without any markdown code blocks or formatting.
                Do NOT wrap the response in ```json or ``` tags.
                The response must start directly with { and end with }.

                Example format (return exactly like this without any extra characters):
                {
                  "topicId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                  "aiSummary": "An image showing a sunset over mountains with vibrant orange and purple colors.",
                  "content": "The image depicts a breathtaking sunset scene over a mountain range. The sky features vibrant gradients of orange, purple, and pink hues. Dark silhouettes of mountain peaks are visible in the foreground. A few wispy clouds are scattered across the sky, catching the warm light. The overall composition creates a serene and dramatic atmosphere."
                }
                """;
    }

    private String prepareDocumentSystemPrompt() {
        return """
                You are an intelligent note classification assistant with document analysis capabilities.
                Analyze both the document content and metadata to classify notes accurately.

                CLASSIFICATION RULES:
                1. Carefully read and analyze the document content to understand its main theme and purpose
                2. Consider document structure, headings, key terms, and overall context
                3. Match the note to ONE topic that best fits its content
                4. Consider topic name, description, and AI summary when making your decision
                5. If no topic clearly matches, choose the DEFAULT topic
                6. ALWAYS return a valid topic ID from the provided list
                7. Prioritize document content over filename and metadata
                8. For technical documents, look for specific terminology and domain-specific language
                9. Consider the document type (report, presentation, form, etc.) in your analysis

                SUMMARY REQUIREMENTS:
                Create a concise summary (2-3 sentences, max 200 characters) describing:
                - The main topic or purpose of the document
                - Key points or findings
                - Document type and context

                CONTENT EXTRACTION:
                Extract and rewrite the entire document content in a clear, structured format:
                - Preserve all headings, sections, and structure
                - Include all key information and data points
                - Maintain the logical flow and organization
                - Convert visual elements (tables, charts) to text descriptions
                - Keep important details, numbers, and terminology

                OUTPUT FORMAT:
                You must respond with a JSON object containing topicId, aiSummary, and content fields.
                CRITICAL: Return ONLY the raw JSON object without any markdown code blocks or formatting.
                Do NOT wrap the response in ```json or ``` tags.
                The response must start directly with { and end with }.

                Example format (return exactly like this without any extra characters):
                {
                  "topicId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                  "aiSummary": "Technical report on cloud migration strategies, covering cost analysis and implementation timeline.",
                  "content": "# Cloud Migration Strategy Report\\n\\n## Executive Summary\\nThis report outlines the proposed cloud migration approach...\\n\\n## Cost Analysis\\n- Infrastructure costs: $50,000\\n- Migration effort: 6 months\\n..."
                }
                """;
    }

    /**
     * Generate answer based on retrieved notes context
     * 
     * @param query         User's query
     * @param relevantNotes List of relevant notes found by vector search
     * @return AI-generated answer
     */
    public String generateAnswerFromNotes(String query, List<NoteQueryResponse> relevantNotes) {
        try {
            log.info("Generating answer for query: '{}' with {} relevant notes", query, relevantNotes.size());

            if (relevantNotes.isEmpty()) {
                return "I couldn't find any relevant notes to answer your question. Please try a different query or create more notes on this topic.";
            }

            // Prepare context from relevant notes
            String notesContext = prepareNotesContext(relevantNotes);

            String answer = mainChatClient.prompt()
                    .system("""
                            You are an intelligent assistant helping users find information from their personal notes.
                            Your task is to answer the user's question based ONLY on the provided notes context.

                            IMPORTANT RULES:
                            1. Use ONLY information from the provided notes
                            2. If the notes don't contain enough information to answer, say so clearly
                            3. Be concise and direct in your answer (2-4 sentences)
                            4. Cite specific notes when referencing information
                            5. If multiple notes have relevant info, synthesize them into a coherent answer
                            6. Do NOT make up or infer information not present in the notes
                            7. Maintain a helpful and professional tone

                            RESPONSE FORMAT:
                            - Start with a direct answer to the question
                            - Support with specific details from the notes
                            - If information is incomplete, mention what's missing
                            """)
                    .user("""
                            User Question: {query}

                            {notesContext}

                            Please answer the user's question based on the notes provided above.
                            Return ONLY the answer text without any additional formatting.
                            """)
                    .user(u -> u
                            .param("query", query)
                            .param("notesContext", notesContext))
                    .options(OpenAiChatOptions.builder()
                            .maxTokens(500)
                            .temperature(0.3)
                            .build())
                    .call()
                    .content();

            log.info("Successfully generated answer (length: {} chars)", answer != null ? answer.length() : 0);
            return answer;

        } catch (Exception e) {
            log.error("Error generating answer from notes", e);
            return "I encountered an error while processing your question. Please try again later.";
        }
    }

    private String prepareNotesContext(List<NoteQueryResponse> notes) {
        StringBuilder context = new StringBuilder();
        context.append("RELEVANT NOTES:\n");
        context.append("===============\n\n");

        for (int i = 0; i < notes.size(); i++) {
            NoteQueryResponse note = notes.get(i);
            context.append(String.format("Note %d:\n", i + 1));
            context.append(String.format("Title: %s\n", note.getTitle()));

            if (note.getTopic() != null) {
                context.append(String.format("Topic: %s\n", note.getTopic().getName()));
            }

            if (note.getAiSummary() != null && !note.getAiSummary().isEmpty()) {
                context.append(String.format("Summary: %s\n", note.getAiSummary()));
            }

            if (note.getContent() != null && !note.getContent().isEmpty()) {
                // Limit content length to avoid token overflow
                String content = note.getContent();
                context.append(String.format("Content: %s\n", content));
            }

            context.append("\n");
        }

        return context.toString();
    }

}
