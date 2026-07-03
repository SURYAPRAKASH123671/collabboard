package com.collabboard.api.board;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CardCommentRepository extends JpaRepository<CardCommentEntity, String> {
}

