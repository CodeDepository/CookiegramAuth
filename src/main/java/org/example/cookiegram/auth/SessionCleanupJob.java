package org.example.cookiegram.auth;

import org.example.cookiegram.auth.repository.SessionTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class SessionCleanupJob {

    private final SessionTokenRepository sessions;

    public SessionCleanupJob(SessionTokenRepository sessions) {
        this.sessions = sessions;
    }

    @Scheduled(fixedRate = 60_000) // every 60s
    @Transactional
    public void cleanup() {
        sessions.deleteExpired(Instant.now());
    }
}