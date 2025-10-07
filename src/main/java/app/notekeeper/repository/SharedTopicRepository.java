package app.notekeeper.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import app.notekeeper.model.entity.SharedTopic;

@Repository
public interface SharedTopicRepository extends JpaRepository<SharedTopic, UUID> {

}