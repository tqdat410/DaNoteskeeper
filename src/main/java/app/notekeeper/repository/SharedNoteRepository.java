package app.notekeeper.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import app.notekeeper.model.entity.SharedNote;

@Repository
public interface SharedNoteRepository extends JpaRepository<SharedNote, UUID> {

}