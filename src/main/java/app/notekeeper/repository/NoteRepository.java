package app.notekeeper.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import app.notekeeper.model.dto.response.NoteQueryResponse;
import app.notekeeper.model.entity.Note;
import app.notekeeper.model.entity.Topic;
import app.notekeeper.model.enums.NoteType;

@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {

        /**
         * Find note by ID without loading embedding field (using projection)
         * This avoids Hibernate issues with NULL embeddings in pgvector
         */
        @Query("SELECT new app.notekeeper.model.dto.response.NoteQueryResponse(" +
                        "n.id, n.owner, n.topic, n.title, n.description, n.content, n.aiSummary, n.type, n.fileUrl, n.createdAt, n.updatedAt) "
                        +
                        "FROM Note n WHERE n.id = :noteId")
        Optional<NoteQueryResponse> findNoteResponseById(@Param("noteId") UUID noteId);

        /**
         * Find notes by owner with filters, without loading embedding field
         */
        @Query("SELECT new app.notekeeper.model.dto.response.NoteQueryResponse(" +
                        "n.id, n.owner, n.topic, n.title, n.description, n.content, n.aiSummary, n.type, n.fileUrl, n.createdAt, n.updatedAt) "
                        +
                        "FROM Note n WHERE n.owner.id = :ownerId " +
                        "AND (:topicId IS NULL OR n.topic.id = :topicId) " +
                        "AND (:type IS NULL OR n.type = :type)")
        Page<NoteQueryResponse> findNotesByOwnerWithFilters(
                        @Param("ownerId") UUID ownerId,
                        @Param("topicId") UUID topicId,
                        @Param("type") NoteType type,
                        Pageable pageable);

        /**
         * Find all notes in a specific topic (paginated, without loading embedding)
         * Used for getting notes in shared topics
         */
        @Query("SELECT new app.notekeeper.model.dto.response.NoteQueryResponse(" +
                        "n.id, n.owner, n.topic, n.title, n.description, n.content, n.aiSummary, n.type, n.fileUrl, n.createdAt, n.updatedAt) "
                        +
                        "FROM Note n WHERE n.topic.id = :topicId " +
                        "ORDER BY n.createdAt DESC")
        Page<NoteQueryResponse> findNotesByTopicId(@Param("topicId") UUID topicId, Pageable pageable);

        /**
         * Find note by ID and topic ID (without loading embedding)
         * Used for validating note belongs to shared topic
         */
        @Query("SELECT new app.notekeeper.model.dto.response.NoteQueryResponse(" +
                        "n.id, n.owner, n.topic, n.title, n.description, n.content, n.aiSummary, n.type, n.fileUrl, n.createdAt, n.updatedAt) "
                        +
                        "FROM Note n WHERE n.id = :noteId AND n.topic.id = :topicId")
        Optional<NoteQueryResponse> findNoteByIdAndTopicId(@Param("noteId") UUID noteId,
                        @Param("topicId") UUID topicId);

        @Modifying
        @Query("UPDATE Note n SET n.embedding = :embedding WHERE n.id = :noteId")
        void updateEmbedding(@Param("noteId") UUID noteId, @Param("embedding") float[] embedding);

        /**
         * Update note title and content (for TEXT notes only)
         * Avoids loading full entity with embedding
         */
        @Modifying
        @Query("UPDATE Note n SET n.title = :title, n.content = :content WHERE n.id = :noteId")
        void updateTitleAndContent(@Param("noteId") UUID noteId, @Param("title") String title,
                        @Param("content") String content);

        /**
         * Delete note by ID without loading entity
         * Avoids embedding field loading issue
         */
        @Modifying
        @Query("DELETE FROM Note n WHERE n.id = :noteId")
        void deleteNoteById(@Param("noteId") UUID noteId);

        /**
         * Update embedding and AI summary for TEXT note after content update
         * Avoids loading full entity with embedding
         */
        @Modifying
        @Query("UPDATE Note n SET n.embedding = :embedding, n.aiSummary = :summary WHERE n.id = :noteId")
        void updateEmbeddingAndSummary(@Param("noteId") UUID noteId, @Param("embedding") float[] embedding,
                        @Param("summary") String summary);

        @Modifying
        @Query("UPDATE Note n SET n.topic = :topic, n.aiSummary = :summary WHERE n.id = :noteId")
        void updateClassification(@Param("noteId") UUID noteId, @Param("topic") Topic topic,
                        @Param("summary") String summary);

        @Modifying
        @Query("UPDATE Note n SET n.topic = :topic, n.aiSummary = :summary, n.content = :content WHERE n.id = :noteId")
        void updateClassificationWithContent(@Param("noteId") UUID noteId, @Param("topic") Topic topic,
                        @Param("summary") String summary, @Param("content") String content);

        @Modifying
        @Query("UPDATE Note n SET n.topic = :topic, n.aiSummary = :summary, n.embedding = :embedding WHERE n.id = :noteId")
        void updateClassificationAndEmbedding(@Param("noteId") UUID noteId, @Param("topic") Topic topic,
                        @Param("summary") String summary, @Param("embedding") float[] embedding);

        @Modifying
        @Query("UPDATE Note n SET n.topic = :topic, n.aiSummary = :summary, n.content = :content, n.embedding = :embedding WHERE n.id = :noteId")
        void updateAll(@Param("noteId") UUID noteId, @Param("topic") Topic topic,
                        @Param("summary") String summary, @Param("content") String content,
                        @Param("embedding") float[] embedding);

        /**
         * Find similar notes using vector similarity search (cosine distance)
         * Returns notes with similarity >= 0.7 (distance <= 0.6), ordered by similarity
         * (most similar first)
         * Note: cosine distance <=> returns 0-2, where 0=identical, 2=opposite
         * Similarity = 1 - (distance/2), so distance <= 0.6 means similarity >= 0.7
         */
        @Query(value = "SELECT n.id, n.owner_id, n.topic_id, n.title, n.description, n.content, n.ai_summary, n.type, n.file_url, n.created_at, n.updated_at "
                        +
                        "FROM notes n " +
                        "WHERE n.owner_id = :ownerId " +
                        "AND (:topicId IS NULL OR n.topic_id = :topicId) " +
                        "AND n.embedding IS NOT NULL " +
                        "AND (n.embedding <=> CAST(:queryEmbedding AS vector)) <= 0.6 " +
                        "ORDER BY n.embedding <=> CAST(:queryEmbedding AS vector) " +
                        "LIMIT :limit", nativeQuery = true)
        List<Object[]> findSimilarNotes(
                        @Param("ownerId") UUID ownerId,
                        @Param("topicId") UUID topicId,
                        @Param("queryEmbedding") String queryEmbedding,
                        @Param("limit") int limit);

}