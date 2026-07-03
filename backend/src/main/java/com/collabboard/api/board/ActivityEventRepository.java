package com.collabboard.api.board;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityEventRepository extends JpaRepository<ActivityEventEntity, String> {

    List<ActivityEventEntity> findTop30ByBoard_IdOrderByCreatedAtDesc(String boardId);
}
