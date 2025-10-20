package app.notekeeper.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import app.notekeeper.model.entity.SharedTopic;

@Repository
public interface SharedTopicRepository extends JpaRepository<SharedTopic, UUID> {

    /**
     * Find all shared topics for a specific topic
     */
    @Query("SELECT st FROM SharedTopic st WHERE st.topic.id = :topicId")
    List<SharedTopic> findByTopicId(@Param("topicId") UUID topicId);

    /**
     * Check if a topic is already shared with a specific user
     */
    @Query("SELECT st FROM SharedTopic st WHERE st.topic.id = :topicId AND st.user.id = :userId")
    Optional<SharedTopic> findByTopicIdAndUserId(@Param("topicId") UUID topicId, @Param("userId") UUID userId);

    /**
     * Find all topics shared with a specific user (paginated)
     */
    @Query("SELECT st FROM SharedTopic st WHERE st.user.id = :userId ORDER BY st.createdAt DESC")
    Page<SharedTopic> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Check if a topic is shared with a specific user (without loading entity)
     */
    @Query("SELECT COUNT(st) > 0 FROM SharedTopic st WHERE st.topic.id = :topicId AND st.user.id = :userId")
    boolean existsByTopicIdAndUserId(@Param("topicId") UUID topicId, @Param("userId") UUID userId);
}