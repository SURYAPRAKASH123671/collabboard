package com.collabboard.api.board;

import jakarta.validation.constraints.NotBlank;

public record BoardCommand(
        @NotBlank String type,
        String actor,
        String listId,
        String cardId,
        String targetListId,
        Integer targetPosition,
        String title,
        String description,
        String assignee,
        String commentBody
) {
    public BoardCommand withActor(String authenticatedActor) {
        return new BoardCommand(
                type,
                authenticatedActor,
                listId,
                cardId,
                targetListId,
                targetPosition,
                title,
                description,
                assignee,
                commentBody
        );
    }
}
