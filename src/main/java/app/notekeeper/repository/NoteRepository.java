package app.notekeeper.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import app.notekeeper.model.entity.Note;

@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {

}