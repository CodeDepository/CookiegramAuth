package org.example.cookiegram.auth.controller;

import org.example.cookiegram.auth.security.AuthFilter;
import org.example.cookiegram.auth.service.AuthService;
import org.example.cookiegram.auth.security.AuthenticatedUser;
import org.example.cookiegram.auth.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;

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
        LoginResponse res = auth.login(req);

        ResponseCookie cookie = ResponseCookie.from("CG_SESSION", res.token)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                // .secure(true) // enable later when using HTTPS
                .maxAge(Duration.ofHours(24))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(res);
    }

    // Protected now (filter enforces auth)
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@RequestAttribute(AuthFilter.ATTR_USER) AuthenticatedUser user) {
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getUsername(), user.getEmail()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        return ResponseEntity.ok(auth.forgotPassword(req));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return ResponseEntity.ok(auth.resetPassword(req));
    }

    // Protected now
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = null;
        var cookies = request.getCookies();
        if (cookies != null) {
            for (var c : cookies) {
                if ("CG_SESSION".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }

        if (token != null && !token.isBlank()) {
            auth.logout(token.trim());
        }

        ResponseCookie clear = ResponseCookie.from("CG_SESSION", "")
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clear.toString())
                .body(java.util.Map.of("message", "Logged out"));
    }
}