package org.example.cookiegram.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public String store(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String stored) {
        if (stored == null) return false;

        // BCrypt hashes start with $2a$, $2b$, or $2y$
        boolean looksLikeBcrypt =
                stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$");

        if (looksLikeBcrypt) {
            return encoder.matches(rawPassword, stored);
        }

        // Backwards compatibility for old plain-text users created earlier
        return rawPassword.equals(stored);
    }
}