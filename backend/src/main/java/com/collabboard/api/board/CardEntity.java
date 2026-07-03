package com.collabboard.api.board;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cards")
public class CardEntity {

    @Id
    private String id;

    private String title;

    @Column(length = 1200)
    private String description;

    private String assignee;

    @Column(name = "sort_order")
    private int position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private BoardListEntity list;

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt asc")
    private List<CardCommentEntity> comments = new ArrayList<>();

    protected CardEntity() {
    }

    public CardEntity(String id, String title, String description, String assignee, int position) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.assignee = assignee;
        this.position = position;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAssignee() {
        return assignee;
    }

    public int getPosition() {
        return position;
    }

    public List<CardCommentEntity> getComments() {
        return comments;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setList(BoardListEntity list) {
        this.list = list;
    }

    public void addComment(CardCommentEntity comment) {
        comments.add(comment);
        comment.setCard(this);
    }
}
