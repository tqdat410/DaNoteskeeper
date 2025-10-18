package app.notekeeper.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import app.notekeeper.model.entity.Note;
import app.notekeeper.model.enums.NoteType;

@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {

    @Query("SELECT n FROM Note n WHERE n.owner.id = :ownerId " +
            "AND (:topicId IS NULL OR n.topic.id = :topicId) " +
            "AND (:type IS NULL OR n.type = :type)")
    Page<Note> findNotesByOwnerWithFilters(
            @Param("ownerId") UUID ownerId,
            @Param("topicId") UUID topicId,
            @Param("type") NoteType type,
            Pageable pageable);

}