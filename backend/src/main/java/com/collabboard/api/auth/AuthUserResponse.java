package com.collabboard.api.auth;

public record AuthUserResponse(
        Long id,
        String name,
        String email,
        UserRole role
) {
    public static AuthUserResponse from(AppUser user) {
        return new AuthUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}

