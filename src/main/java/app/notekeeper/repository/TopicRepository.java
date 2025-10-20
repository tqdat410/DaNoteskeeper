package app.notekeeper.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import app.notekeeper.model.entity.Topic;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {

    @Query("SELECT t FROM Topic t WHERE t.isDefault = true AND t.owner.id = :ownerId")
    Optional<Topic> findByIsDefaultTrueAndOwnerId(UUID ownerId);

    List<Topic> findByOwnerId(UUID ownerId);

}