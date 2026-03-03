package org.example.cookiegram.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length = 30)
    private String username;

    @Column(nullable=false, length = 254)
    private String email;

    // Sprint 1: plain text (we’ll swap later via PasswordService)
    @Column(nullable=false, length = 255)
    private String password;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    public User() {}

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Instant getCreatedAt() { return createdAt; }

    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
}