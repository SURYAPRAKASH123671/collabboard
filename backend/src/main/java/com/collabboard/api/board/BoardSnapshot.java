package com.collabboard.api.board;

import java.util.List;

public record BoardSnapshot(
        String id,
        String name,
        List<BoardList> lists,
        List<ActivityEvent> activity,
        List<String> viewers
) {
}

