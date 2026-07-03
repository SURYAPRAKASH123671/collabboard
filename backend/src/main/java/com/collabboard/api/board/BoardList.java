package com.collabboard.api.board;

import java.util.List;

public record BoardList(
        String id,
        String title,
        int position,
        List<CardItem> cards
) {
}

