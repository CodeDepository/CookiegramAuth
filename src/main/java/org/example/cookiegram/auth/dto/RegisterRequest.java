package org.example.cookiegram.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 30)
    public String username;

    @NotBlank
    @Email
    public String email;

    @NotBlank
    @Size(min = 4, max = 100)
    public String password;
}