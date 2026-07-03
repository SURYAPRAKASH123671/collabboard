package com.collabboard.api.board;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardListRepository extends JpaRepository<BoardListEntity, String> {
}

