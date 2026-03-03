package org.example.cookiegram.auth.repository;

import org.example.cookiegram.auth.entity.SessionToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.Optional;

public interface SessionTokenRepository extends JpaRepository<SessionToken, Long> {
    Optional<SessionToken> findByToken(String token);
    void deleteByToken(String token);

    @Modifying
    @Query("delete from SessionToken s where s.expiresAt < ?1")
    int deleteExpired(Instant now);
}