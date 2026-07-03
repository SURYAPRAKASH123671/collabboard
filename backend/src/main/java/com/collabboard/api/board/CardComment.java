package com.collabboard.api.board;

import java.time.Instant;

public record CardComment(
        String id,
        String author,
        String body,
        Instant createdAt
) {
}

