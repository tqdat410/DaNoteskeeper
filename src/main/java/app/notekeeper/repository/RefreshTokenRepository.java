package app.notekeeper.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import app.notekeeper.model.entity.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId")
    Optional<RefreshToken> findByUserId(@Param("userId") UUID userId);

    void deleteByUserId(UUID userId);
}