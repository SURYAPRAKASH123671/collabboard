package com.collabboard.api.board;

public record BoardMemberResponse(
        Long userId,
        String name,
        String email,
        BoardMemberRole role
) {
}

