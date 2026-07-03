package com.collabboard.api.board;

import com.collabboard.api.auth.UserPrincipal;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class BoardWebSocketController {

    private final BoardService boardService;

    public BoardWebSocketController(BoardService boardService) {
        this.boardService = boardService;
    }

    @MessageMapping("/boards/{boardId}/commands")
    @SendTo("/topic/boards/{boardId}")
    public BoardSnapshot command(@DestinationVariable String boardId, @Valid BoardCommand command, Principal principal) {
        UserPrincipal user = userPrincipal(principal);
        return boardService.apply(boardId, command.withActor(user.user().getName()), user.user());
    }

    @MessageMapping("/boards/{boardId}/presence/join")
    @SendTo("/topic/boards/{boardId}")
    public BoardSnapshot join(@DestinationVariable String boardId, Principal principal) {
        UserPrincipal user = userPrincipal(principal);
        return boardService.join(boardId, user.user());
    }

    @MessageMapping("/boards/{boardId}/presence/leave")
    @SendTo("/topic/boards/{boardId}")
    public BoardSnapshot leave(@DestinationVariable String boardId, Principal principal) {
        UserPrincipal user = userPrincipal(principal);
        return boardService.leave(boardId, user.user());
    }

    private UserPrincipal userPrincipal(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }
        throw new IllegalArgumentException("Authenticated WebSocket user required");
    }
}
