package com.collabboard.api.board;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "card_comments")
public class CardCommentEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false, length = 1600)
    private String body;

    @Column(nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private CardEntity card;

    protected CardCommentEntity() {
    }

    public CardCommentEntity(String id, String author, String body, Instant createdAt) {
        this.id = id;
        this.author = author;
        this.body = body;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCard(CardEntity card) {
        this.card = card;
    }
}

