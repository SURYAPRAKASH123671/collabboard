package com.collabboard.api.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.collabboard.api.auth.AppUser;
import com.collabboard.api.auth.AppUserRepository;
import com.collabboard.api.auth.UserRole;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class BoardServiceListTests {

    @Autowired
    private BoardService boardService;

    @Autowired
    private AppUserRepository userRepository;

    @Test
    void createsRenamesMovesAndDeletesEmptyLists() {
        AppUser user = userRepository.save(new AppUser(
                "List Owner",
                "list.owner@example.com",
                "hash",
                UserRole.USER,
                Instant.now()
        ));
        BoardSnapshot board = boardService.createBoard(new CreateBoardRequest("List Test Board"), user);

        BoardSnapshot withList = boardService.apply(board.id(), command("CREATE_LIST", null, "QA"), user);
        assertThat(withList.lists()).extracting(BoardList::title).contains("QA");

        String qaListId = withList.lists().stream()
                .filter(list -> list.title().equals("QA"))
                .findFirst()
                .orElseThrow()
                .id();

        BoardSnapshot renamed = boardService.apply(board.id(), command("UPDATE_LIST", qaListId, "Ready for QA"), user);
        assertThat(renamed.lists()).extracting(BoardList::title).contains("Ready for QA");

        BoardSnapshot moved = boardService.apply(
                board.id(),
                new BoardCommand("MOVE_LIST", "List Owner", qaListId, null, null, 0, null, null, null, null),
                user
        );
        assertThat(moved.lists().get(0).id()).isEqualTo(qaListId);

        BoardSnapshot deleted = boardService.apply(board.id(), command("DELETE_LIST", qaListId, null), user);
        assertThat(deleted.lists()).noneMatch(list -> list.id().equals(qaListId));
    }

    @Test
    void rejectsDeletingNonEmptyList() {
        AppUser user = userRepository.save(new AppUser(
                "Card List Owner",
                "card.list.owner@example.com",
                "hash",
                UserRole.USER,
                Instant.now()
        ));
        BoardSnapshot board = boardService.createBoard(new CreateBoardRequest("Non Empty List Board"), user);
        String listId = board.lists().get(0).id();

        boardService.apply(board.id(), new BoardCommand(
                "CREATE_CARD",
                "Card List Owner",
                listId,
                null,
                null,
                null,
                "Keep me",
                "",
                "Card List Owner",
                null
        ), user);

        assertThatThrownBy(() -> boardService.apply(board.id(), command("DELETE_LIST", listId, null), user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only empty lists");
    }

    private BoardCommand command(String type, String listId, String title) {
        return new BoardCommand(type, "List Owner", listId, null, null, null, title, null, null, null);
    }
}

