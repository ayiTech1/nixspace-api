package com.nixspace.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthRequests {

    public record RegisterRequest(
            @NotBlank @Email(message = "Invalid email format")
            String email,

            @NotBlank @Size(min = 8, max = 72, message = "Password must be 8–72 characters")
            String password,

            @NotBlank @Size(min = 2, max = 100, message = "Display name must be 2–100 characters")
            String displayName,

            String timezone
    ) {}

    public record LoginRequest(
            @NotBlank @Email
            String email,

            @NotBlank
            String password
    ) {}

    public record RefreshTokenRequest(
            @NotBlank
            String refreshToken
    ) {}

    public record ChangePasswordRequest(
            @NotBlank
            String currentPassword,

            @NotBlank @Size(min = 8, max = 72)
            String newPassword
    ) {}
}
