package org.example.cookiegram.auth.service;


import org.springframework.stereotype.Service;

@Service
public class PasswordService {
    // Sprint 1: no hashing yet
    public String store(String raw) {
        return raw;
    }

    public boolean matches(String raw, String stored) {
        return raw.equals(stored);
    }
}