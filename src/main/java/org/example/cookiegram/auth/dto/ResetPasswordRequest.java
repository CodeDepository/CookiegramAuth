package org.example.cookiegram.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {
    @NotBlank
    public String token;

    @NotBlank
    @Size(min = 4, max = 100)
    public String newPassword;
}