package com.collabboard.api.board;

import com.collabboard.api.auth.UserPrincipal;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/boards")
@CrossOrigin(origins = "http://localhost:5173")
public class BoardRestController {

    private final BoardService boardService;

    public BoardRestController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping
    public List<BoardSummary> listBoards(@AuthenticationPrincipal UserPrincipal principal) {
        return boardService.listBoards(principal.user());
    }

    @PostMapping
    public ResponseEntity<BoardSnapshot> createBoard(
            @Valid @RequestBody CreateBoardRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        BoardSnapshot board = boardService.createBoard(request, principal.user());
        return ResponseEntity.created(URI.create("/api/boards/" + board.id())).body(board);
    }

    @GetMapping("/{boardId}")
    public BoardSnapshot getBoard(@PathVariable String boardId, @AuthenticationPrincipal UserPrincipal principal) {
        return boardService.snapshot(boardId, principal.user());
    }

    @GetMapping("/{boardId}/members")
    public List<BoardMemberResponse> listMembers(
            @PathVariable String boardId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return boardService.listMembers(boardId, principal.user());
    }

    @PostMapping("/{boardId}/members")
    public ResponseEntity<BoardMemberResponse> inviteMember(
            @PathVariable String boardId,
            @Valid @RequestBody InviteMemberRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        BoardMemberResponse member = boardService.inviteMember(boardId, request, principal.user());
        return ResponseEntity.created(URI.create("/api/boards/" + boardId + "/members/" + member.userId())).body(member);
    }
}
