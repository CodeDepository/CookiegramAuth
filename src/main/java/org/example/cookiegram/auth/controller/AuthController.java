package org.example.cookiegram.auth.controller;

import org.example.cookiegram.auth.security.AuthFilter;
import org.example.cookiegram.auth.service.AuthService;
import org.example.cookiegram.auth.security.AuthenticatedUser;
import org.example.cookiegram.auth.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(auth.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(auth.login(req));
    }

    // Protected now (filter enforces auth)
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@RequestAttribute(AuthFilter.ATTR_USER) AuthenticatedUser user) {
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getUsername(), user.getEmail()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        return ResponseEntity.ok(auth.forgotPassword(req));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return ResponseEntity.ok(auth.resetPassword(req));
    }

    // Protected now
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("X-Auth-Token") String token) {
        auth.logout(token.trim());
        return ResponseEntity.ok().body(java.util.Map.of("message", "Logged out"));
    }
}