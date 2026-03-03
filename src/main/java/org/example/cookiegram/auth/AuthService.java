package org.example.cookiegram.auth;

import org.example.cookiegram.auth.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

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
        sessions.save(new SessionToken(token, user));

        return new LoginResponse(token, new UserResponse(user.getId(), user.getUsername(), user.getEmail()));
    }

    @Transactional(readOnly = true)
    public UserResponse me(String token) {
        SessionToken session = sessions.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Not authenticated"));

        User user = session.getUser();
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail());
    }
}
