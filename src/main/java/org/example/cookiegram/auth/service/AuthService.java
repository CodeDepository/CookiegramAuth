package org.example.cookiegram.auth.service;

import org.example.cookiegram.auth.dto.*;
import org.example.cookiegram.auth.repository.SessionTokenRepository;
import org.example.cookiegram.auth.repository.UserRepository;
import org.example.cookiegram.auth.entity.SessionToken;
import org.example.cookiegram.auth.entity.User;
import org.example.cookiegram.auth.exception.UnauthorizedException;
import org.example.cookiegram.auth.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private static final Duration SESSION_TTL = Duration.ofHours(24);

    private final UserRepository users;
    private final SessionTokenRepository sessions;
    private final PasswordService passwords;

    public AuthService(UserRepository users, SessionTokenRepository sessions, PasswordService passwords) {
        this.users = users;
        this.sessions = sessions;
        this.passwords = passwords;
    }

    @Transactional
    public UserResponse register(RegisterRequest req) {
        String username = req.username.trim();
        String email = req.email.trim();

        if (users.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email already taken");
        }

        User user = new User(username, email, passwords.store(req.password));
        users.save(user);

        return new UserResponse(user.getId(), user.getUsername(), user.getEmail());
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        String key = req.usernameOrEmail.trim();

        User user = users.findByUsernameIgnoreCase(key)
                .or(() -> users.findByEmailIgnoreCase(key))
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwords.matches(req.password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plus(SESSION_TTL);

        sessions.save(new SessionToken(token, user, expiresAt));

        return new LoginResponse(token, new UserResponse(user.getId(), user.getUsername(), user.getEmail()));
    }

    @Transactional(readOnly = true)
    public AuthenticatedUser requireUserByToken(String token) {
        SessionToken session = sessions.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));

        if (session.isExpired()) {
            throw new UnauthorizedException("Session expired");
        }

        // IMPORTANT: read needed fields while still inside the transaction
        User u = session.getUser();
        return new AuthenticatedUser(u.getId(), u.getUsername(), u.getEmail());
    }

    @Transactional
    public void logout(String token) {
        sessions.deleteByToken(token);
    }
}