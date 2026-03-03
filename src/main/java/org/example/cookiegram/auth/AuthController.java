package org.example.cookiegram.auth;
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

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@RequestHeader(name="X-Auth-Token", required=false) String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Not authenticated");
        }
        return ResponseEntity.ok(auth.me(token.trim()));
    }
}
