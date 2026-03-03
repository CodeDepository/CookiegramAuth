package org.example.cookiegram.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SessionTokenRepository extends JpaRepository<SessionToken, Long> {
    Optional<SessionToken> findByToken(String token);
    void deleteByToken(String token);
}