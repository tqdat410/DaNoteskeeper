package app.notekeeper.external.ai;

import java.util.List;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OllamaService {

    private final EmbeddingModel embeddingModel;

    public OllamaService(@Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Generate embedding vector for text content
     * 
     * @param content Text content to embed
     * @return Embedding vector as Float array (nullable)
     */
    public float[] generateEmbedding(String content) {
        try {
            if (content == null || content.trim().isEmpty()) {
                log.warn("Empty content provided for embedding, returning null");
                return null;
            }

            log.info("Generating embedding for content (length: {} chars)", content.length());

            // Create embedding request
            EmbeddingRequest request = new EmbeddingRequest(List.of(content), null);

            // Call Ollama embedding model
            EmbeddingResponse response = embeddingModel.call(request);

            // Extract embedding vector from first result
            if (response.getResults() != null && !response.getResults().isEmpty()) {
                float[] primitiveArray = response.getResults().get(0).getOutput();

                log.info("Successfully generated embedding vector with {} dimensions", primitiveArray.length);
                return primitiveArray;
            }

            log.warn("No embedding results returned from Ollama");
            return null;

        } catch (Exception e) {
            log.error("Error generating embedding for content", e);
            return null;
        }
    }

}
