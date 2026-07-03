package com.collabboard.api.board;

import java.util.List;

public record CardItem(
        String id,
        String title,
        String description,
        String assignee,
        int position,
        List<CardComment> comments
) {
}
