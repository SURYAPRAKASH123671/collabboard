package com.collabboard.api.board;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "activity_events")
public class ActivityEventEntity {

    @Id
    private String id;

    private String actor;

    private String message;

    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardEntity board;

    protected ActivityEventEntity() {
    }

    public ActivityEventEntity(String id, BoardEntity board, String actor, String message, Instant createdAt) {
        this.id = id;
        this.board = board;
        this.actor = actor;
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getActor() {
        return actor;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public BoardEntity getBoard() {
        return board;
    }
}

