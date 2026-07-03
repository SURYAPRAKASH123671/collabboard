package com.collabboard.api.board;

public record BoardSummary(
        String id,
        String name,
        BoardMemberRole role
) {
}
