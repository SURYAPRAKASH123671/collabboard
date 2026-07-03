package com.collabboard.api.board;

import com.collabboard.api.auth.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardMemberRepository extends JpaRepository<BoardMemberEntity, String> {

    List<BoardMemberEntity> findByUserOrderByBoardNameAsc(AppUser user);

    List<BoardMemberEntity> findByBoardIdOrderByUserNameAsc(String boardId);

    Optional<BoardMemberEntity> findByBoardIdAndUser(String boardId, AppUser user);

    boolean existsByBoardIdAndUser(String boardId, AppUser user);
}

