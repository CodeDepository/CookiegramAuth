package org.example.cookiegram.auth.service;

import org.example.cookiegram.auth.dto.*;
import org.example.cookiegram.auth.entity.PasswordResetToken;
import org.example.cookiegram.auth.repository.PasswordResetTokenRepository;
import org.example.cookiegram.auth.repository.SessionTokenRepository;
import org.example.cookiegram.auth.repository.UserRepository;
import org.example.cookiegram.auth.entity.SessionToken;
import org.example.cookiegram.auth.entity.User;
import org.example.cookiegram.auth.exception.UnauthorizedException;
import org.example.cookiegram.auth.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;


import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private static final Duration SESSION_TTL = Duration.ofHours(24);

    private final UserRepository users;
    private final SessionTokenRepository sessions;
    private final PasswordService passwords;
    private static final Duration RESET_TTL = Duration.ofMinutes(15);
    private final PasswordResetTokenRepository resetTokens;
    private final EmailService emailService;

    @Value("${cookiegram.base-url}")
    private String baseUrl;

    @Value("${cookiegram.mail.from}")
    private String mailFrom;


    public AuthService(
            UserRepository users,
            SessionTokenRepository sessions,
            PasswordService passwords,
            PasswordResetTokenRepository resetTokens,
            EmailService emailService
    ) {
        this.users = users;
        this.sessions = sessions;
        this.passwords = passwords;
        this.resetTokens = resetTokens;
        this.emailService = emailService;
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
    public MessageResponse forgotPassword(ForgotPasswordRequest req) {
        String email = req.email.trim().toLowerCase();

        var userOpt = users.findByEmailIgnoreCase(email);

        // Always return same message (don’t leak whether user exists)
        if (userOpt.isEmpty()) {
            return new MessageResponse("If that email exists, a reset link was sent.");
        }

        User user = userOpt.get();

        String token = UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "");

        Instant expiresAt = Instant.now().plus(RESET_TTL);
        resetTokens.save(new PasswordResetToken(token, user, expiresAt));

        String link = baseUrl + "/reset.html?token=" + token;

        String body = "Cookiegram password reset\n\n" +
                "Click this link to reset your password:\n" + link + "\n\n" +
                "This link expires in 15 minutes.\n";

        emailService.send(mailFrom, user.getEmail(), "Reset your Cookiegram password", body);

        return new MessageResponse("If that email exists, a reset link was sent.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest req) {
        String token = req.token.trim();

        PasswordResetToken prt = resetTokens.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        if (prt.isUsed()) {
            throw new IllegalArgumentException("Reset token already used");
        }
        if (prt.isExpired()) {
            throw new IllegalArgumentException("Reset token expired");
        }

        User user = prt.getUser();
        user.setPassword(passwords.store(req.newPassword)); // still plain for now, can upgrade later
        users.save(user);

        prt.markUsed();
        resetTokens.save(prt);

        return new MessageResponse("Password updated");
    }

    @Transactional
    public void logout(String token) {
        sessions.deleteByToken(token);
    }
}