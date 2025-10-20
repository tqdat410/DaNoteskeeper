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

import app.notekeeper.model.dto.response.SharedNoteResponse;
import app.notekeeper.model.entity.SharedNote;

@Repository
public interface SharedNoteRepository extends JpaRepository<SharedNote, UUID> {

    /**
     * Find all shared notes for a specific note
     */
    @Query("SELECT sn FROM SharedNote sn WHERE sn.note.id = :noteId")
    List<SharedNote> findByNoteId(@Param("noteId") UUID noteId);

    /**
     * Check if a note is already shared with a specific user
     */
    @Query("SELECT sn FROM SharedNote sn WHERE sn.note.id = :noteId AND sn.user.id = :userId")
    Optional<SharedNote> findByNoteIdAndUserId(@Param("noteId") UUID noteId, @Param("userId") UUID userId);

    /**
     * Check if a note is shared with a user without loading entities
     * Used for validation to avoid loading Note.embedding field
     */
    @Query("SELECT CASE WHEN COUNT(sn) > 0 THEN true ELSE false END FROM SharedNote sn WHERE sn.note.id = :noteId AND sn.user.id = :userId")
    boolean existsByNoteIdAndUserId(@Param("noteId") UUID noteId, @Param("userId") UUID userId);

    /**
     * Find all notes shared with a specific user (paginated)
     */
    @Query("SELECT sn FROM SharedNote sn WHERE sn.user.id = :userId ORDER BY sn.createdAt DESC")
    Page<SharedNote> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find shared note responses for owner (shows recipient info)
     * Avoids loading Note.embedding field
     */
    @Query("""
            SELECT new app.notekeeper.model.dto.response.SharedNoteResponse(
                sn.id,
                sn.note.id,
                sn.note.title,
                sn.note.description,
                sn.note.type,
                sn.user.id,
                sn.user.email,
                sn.user.displayName,
                sn.permission,
                sn.createdAt
            )
            FROM SharedNote sn
            WHERE sn.note.id = :noteId
            """)
    List<SharedNoteResponse> findSharedNoteResponsesByNoteId(@Param("noteId") UUID noteId);

    /**
     * Find shared note responses for recipient (shows owner info)
     * Avoids loading Note.embedding field
     */
    @Query("""
            SELECT new app.notekeeper.model.dto.response.SharedNoteResponse(
                sn.id,
                sn.note.id,
                sn.note.title,
                sn.note.description,
                sn.note.type,
                sn.note.owner.id,
                sn.note.owner.email,
                sn.note.owner.displayName,
                sn.permission,
                sn.createdAt
            )
            FROM SharedNote sn
            WHERE sn.user.id = :userId
            ORDER BY sn.createdAt DESC
            """)
    Page<SharedNoteResponse> findSharedNoteResponsesByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Get note ID for a SharedNote without loading full Note entity
     * Used for validation during unshare operation
     */
    @Query("SELECT sn.note.id FROM SharedNote sn WHERE sn.id = :sharedNoteId")
    Optional<UUID> findNoteIdBySharedNoteId(@Param("sharedNoteId") UUID sharedNoteId);

    /**
     * Delete SharedNote by ID without loading the entity
     * Avoids loading Note.embedding field
     */
    @Modifying
    @Query("DELETE FROM SharedNote sn WHERE sn.id = :sharedNoteId")
    void deleteByIdWithoutLoading(@Param("sharedNoteId") UUID sharedNoteId);
}