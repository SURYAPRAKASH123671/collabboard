package com.collabboard.api.auth;

public record AuthResponse(
        String token,
        long expiresInSeconds,
        AuthUserResponse user
) {
}

