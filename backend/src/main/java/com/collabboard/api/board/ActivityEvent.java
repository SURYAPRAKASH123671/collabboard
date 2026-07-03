package com.collabboard.api.board;

import java.time.Instant;

public record ActivityEvent(
        String id,
        String boardId,
        String actor,
        String message,
        Instant createdAt
) {
}

