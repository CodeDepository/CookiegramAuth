package org.example.cookiegram.auth.dto;

public class ForgotPasswordResponse {
    public String message;
    public String resetToken; // DEV: show token instead of email

    public ForgotPasswordResponse(String message, String resetToken) {
        this.message = message;
        this.resetToken = resetToken;
    }
}