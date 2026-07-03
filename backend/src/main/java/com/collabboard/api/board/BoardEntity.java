package com.collabboard.api.board;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boards")
public class BoardEntity {

    @Id
    private String id;

    private String name;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position asc")
    private List<BoardListEntity> lists = new ArrayList<>();

    protected BoardEntity() {
    }

    public BoardEntity(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<BoardListEntity> getLists() {
        return lists;
    }

    public void addList(BoardListEntity list) {
        lists.add(list);
        list.setBoard(this);
    }
}

