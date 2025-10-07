package app.notekeeper.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import app.notekeeper.model.entity.EmailVerification;

@Repository
public interface EmailVerificationRepository extends CrudRepository<EmailVerification, String> {

    Optional<EmailVerification> findByEmail(String email);

    void deleteByEmail(String email);
}