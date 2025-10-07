package app.notekeeper.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import app.notekeeper.model.entity.PasswordReset;

@Repository
public interface PasswordResetRepository extends CrudRepository<PasswordReset, String> {

    Optional<PasswordReset> findByEmail(String email);

    void deleteByEmail(String email);
}