package com.collabboard.api.board;

import com.collabboard.api.auth.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "board_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_board_member_user", columnNames = {"board_id", "user_id"})
)
public class BoardMemberEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardEntity board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardMemberRole role;

    @Column(nullable = false)
    private Instant createdAt;

    protected BoardMemberEntity() {
    }

    public BoardMemberEntity(String id, BoardEntity board, AppUser user, BoardMemberRole role, Instant createdAt) {
        this.id = id;
        this.board = board;
        this.user = user;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public BoardEntity getBoard() {
        return board;
    }

    public AppUser getUser() {
        return user;
    }

    public BoardMemberRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

