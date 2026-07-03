package com.collabboard.api.board;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
@Table(name = "board_lists")
public class BoardListEntity {

    @Id
    private String id;

    private String title;

    @Column(name = "sort_order")
    private int position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardEntity board;

    @OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position asc")
    private List<CardEntity> cards = new ArrayList<>();

    protected BoardListEntity() {
    }

    public BoardListEntity(String id, String title, int position) {
        this.id = id;
        this.title = title;
        this.position = position;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getPosition() {
        return position;
    }

    public List<CardEntity> getCards() {
        return cards;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setBoard(BoardEntity board) {
        this.board = board;
    }

    public void addCard(CardEntity card) {
        cards.add(card);
        card.setList(this);
    }

    public void removeCard(CardEntity card) {
        cards.remove(card);
        card.setList(null);
    }
}
