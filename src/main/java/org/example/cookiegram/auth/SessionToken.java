package org.example.cookiegram.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sessions", indexes = {
        @Index(name="idx_sessions_token", columnList = "token", unique = true)
})
public class SessionToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique = true, length=64)
    private String token;

    @ManyToOne(optional=false, fetch = FetchType.LAZY)
    private User user;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    public SessionToken() {}

    public SessionToken(String token, User user) {
        this.token = token;
        this.user = user;
    }

    public Long getId() { return id; }
    public String getToken() { return token; }
    public User getUser() { return user; }
    public Instant getCreatedAt() { return createdAt; }
}
