package org.example.cookiegram.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank
    public String usernameOrEmail;

    @NotBlank
    public String password;
}
