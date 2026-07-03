package com.collabboard.api.board;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardRepository extends JpaRepository<BoardEntity, String> {

    Optional<BoardEntity> findWithListsById(String id);
}
