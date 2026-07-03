package com.collabboard.api.board;

import com.collabboard.api.auth.AppUser;
import com.collabboard.api.auth.AppUserRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class BoardService {

    private static final String DEMO_BOARD_ID = "demo-board";

    private final Map<String, Set<String>> viewersByBoard = new HashMap<>();
    private final BoardRepository boardRepository;
    private final BoardMemberRepository memberRepository;
    private final AppUserRepository userRepository;
    private final BoardListRepository listRepository;
    private final CardRepository cardRepository;
    private final CardCommentRepository commentRepository;
    private final ActivityEventRepository activityRepository;

    public BoardService(
            BoardRepository boardRepository,
            BoardMemberRepository memberRepository,
            AppUserRepository userRepository,
            BoardListRepository listRepository,
            CardRepository cardRepository,
            CardCommentRepository commentRepository,
            ActivityEventRepository activityRepository
    ) {
        this.boardRepository = boardRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.listRepository = listRepository;
        this.cardRepository = cardRepository;
        this.commentRepository = commentRepository;
        this.activityRepository = activityRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDemoBoard() {
        if (boardRepository.existsById(DEMO_BOARD_ID)) {
            return;
        }

        BoardEntity board = new BoardEntity(DEMO_BOARD_ID, "CollabBoard Launch Plan");

        BoardListEntity todo = new BoardListEntity("list-todo", "To do", 0);
        todo.addCard(new CardEntity("card-brief", "Define MVP scope", "Pick Blueprint C and keep the first release focused.", "Surya", 0));
        todo.addCard(new CardEntity("card-auth", "Plan JWT auth", "Reuse known Spring Security JWT patterns from Nexora.", "Surya", 1));

        BoardListEntity doing = new BoardListEntity("list-doing", "In progress", 1);
        doing.addCard(new CardEntity("card-ws", "Wire STOMP channel", "Broadcast board commands to every viewer on the board.", "Surya", 0));

        BoardListEntity done = new BoardListEntity("list-done", "Done", 2);
        done.addCard(new CardEntity("card-idea", "Choose project idea", "Real-time Kanban plus activity feed.", "Surya", 0));

        board.addList(todo);
        board.addList(doing);
        board.addList(done);
        boardRepository.save(board);
        addActivity(board, "System", "created the CollabBoard demo board");
    }

    @Transactional(readOnly = true)
    public synchronized List<BoardSummary> listBoards(AppUser user) {
        return memberRepository.findByUserOrderByBoardNameAsc(user).stream()
                .map(member -> new BoardSummary(member.getBoard().getId(), member.getBoard().getName(), member.getRole()))
                .toList();
    }

    @Transactional
    public synchronized BoardSnapshot createBoard(CreateBoardRequest request, AppUser owner) {
        BoardEntity board = createBoardEntity("board-" + UUID.randomUUID(), request.name().trim());
        boardRepository.save(board);
        memberRepository.save(new BoardMemberEntity("member-" + UUID.randomUUID(), board, owner, BoardMemberRole.OWNER, Instant.now()));
        addActivity(board, owner.getName(), "created the board");
        return toSnapshot(board);
    }

    @Transactional(readOnly = true)
    public synchronized BoardSnapshot snapshot(String boardId, AppUser user) {
        requireMember(boardId, user);
        BoardEntity board = findBoard(boardId);
        return toSnapshot(board);
    }

    @Transactional
    public synchronized BoardSnapshot join(String boardId, AppUser user) {
        requireMember(boardId, user);
        BoardEntity board = findBoard(boardId);
        viewersFor(boardId).add(user.getName());
        addActivity(board, user.getName(), "joined the board");
        return toSnapshot(board);
    }

    @Transactional
    public synchronized BoardSnapshot leave(String boardId, AppUser user) {
        requireMember(boardId, user);
        BoardEntity board = findBoard(boardId);
        viewersFor(boardId).remove(user.getName());
        addActivity(board, user.getName(), "left the board");
        return toSnapshot(board);
    }

    @Transactional
    public synchronized BoardSnapshot apply(String boardId, BoardCommand command, AppUser user) {
        requireMember(boardId, user);
        return switch (command.type()) {
            case "CREATE_LIST" -> createList(boardId, command);
            case "UPDATE_LIST" -> updateList(boardId, command);
            case "DELETE_LIST" -> deleteList(boardId, command);
            case "MOVE_LIST" -> moveList(boardId, command);
            case "CREATE_CARD" -> createCard(boardId, command);
            case "UPDATE_CARD" -> updateCard(boardId, command);
            case "ADD_COMMENT" -> addComment(boardId, command);
            case "MOVE_CARD" -> moveCard(boardId, command);
            case "DELETE_CARD" -> deleteCard(boardId, command);
            default -> snapshot(boardId, user);
        };
    }

    private BoardSnapshot createList(String boardId, BoardCommand command) {
        BoardEntity board = findBoard(boardId);
        String title = valueOrDefault(command.title(), "Untitled list");
        BoardListEntity list = new BoardListEntity(
                "list-" + UUID.randomUUID(),
                title,
                board.getLists().size()
        );
        board.addList(list);
        boardRepository.save(board);

        addActivity(board, command.actor(), "created list \"" + list.getTitle() + "\"");
        return toSnapshot(board);
    }

    private BoardSnapshot updateList(String boardId, BoardCommand command) {
        BoardEntity board = findBoard(boardId);
        BoardListEntity list = findList(command.listId(), boardId);
        String oldTitle = list.getTitle();
        list.setTitle(valueOrDefault(command.title(), list.getTitle()));
        listRepository.save(list);

        addActivity(board, command.actor(), "renamed \"" + oldTitle + "\" to \"" + list.getTitle() + "\"");
        return toSnapshot(board);
    }

    private BoardSnapshot deleteList(String boardId, BoardCommand command) {
        BoardEntity board = findBoard(boardId);
        BoardListEntity list = findList(command.listId(), boardId);
        if (!list.getCards().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only empty lists can be deleted");
        }

        String title = list.getTitle();
        board.getLists().removeIf(candidate -> candidate.getId().equals(list.getId()));
        listRepository.delete(list);
        normalizeListPositions(board);
        boardRepository.save(board);

        addActivity(board, command.actor(), "deleted list \"" + title + "\"");
        return toSnapshot(board);
    }

    private BoardSnapshot moveList(String boardId, BoardCommand command) {
        BoardEntity board = findBoard(boardId);
        BoardListEntity list = findList(command.listId(), boardId);
        board.getLists().sort(Comparator.comparingInt(BoardListEntity::getPosition));
        board.getLists().removeIf(candidate -> candidate.getId().equals(list.getId()));
        int targetPosition = boundedPosition(command.targetPosition(), board.getLists().size());
        board.getLists().add(targetPosition, list);
        normalizeListPositions(board);
        boardRepository.save(board);

        addActivity(board, command.actor(), "moved list \"" + list.getTitle() + "\"");
        return toSnapshot(board);
    }

    @Transactional(readOnly = true)
    public synchronized List<BoardMemberResponse> listMembers(String boardId, AppUser requester) {
        requireMember(boardId, requester);
        return memberRepository.findByBoardIdOrderByUserNameAsc(boardId).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public synchronized BoardMemberResponse inviteMember(String boardId, InviteMemberRequest request, AppUser requester) {
        BoardEntity board = findBoard(boardId);
        BoardMemberEntity requesterMembership = requireMember(boardId, requester);
        if (requesterMembership.getRole() == BoardMemberRole.MEMBER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owners and admins can invite members");
        }

        AppUser invitedUser = userRepository.findByEmailIgnoreCase(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        BoardMemberEntity member = memberRepository.findByBoardIdAndUser(boardId, invitedUser)
                .orElseGet(() -> memberRepository.save(new BoardMemberEntity(
                        "member-" + UUID.randomUUID(),
                        board,
                        invitedUser,
                        BoardMemberRole.MEMBER,
                        Instant.now()
                )));
        addActivity(board, requester.getName(), "invited " + invitedUser.getName() + " to the board");
        return toMemberResponse(member);
    }

    private BoardSnapshot createCard(String boardId, BoardCommand command) {
        BoardEntity board = findBoard(boardId);
        BoardListEntity list = findList(command.listId(), boardId);
        CardEntity card = new CardEntity(
                "card-" + UUID.randomUUID(),
                valueOrDefault(command.title(), "Untitled card"),
                valueOrDefault(command.description(), ""),
                valueOrDefault(command.assignee(), command.actor()),
                list.getCards().size()
        );
        list.addCard(card);
        listRepository.save(list);

        addActivity(board, command.actor(), "added \"" + card.getTitle() + "\" to " + list.getTitle());
        return toSnapshot(board);
    }

    private BoardSnapshot updateCard(String boardId, BoardCommand command) {
        BoardEntity board = findBoard(boardId);
        CardEntity card = findCard(command.cardId(), boardId);
        card.setTitle(valueOrDefault(command.title(), card.getTitle()));
        card.setDescription(valueOrDefault(command.description(), card.getDescription()));
        card.setAssignee(valueOrDefault(command.assignee(), card.getAssignee()));
        cardRepository.save(card);

        addActivity(board, command.actor(), "updated \"" + card.getTitle() + "\"");
        return toSnapshot(board);
    }

    private BoardSnapshot addComment(String boardId, BoardCommand command) {
        BoardEntity board = findBoard(boardId);
        CardEntity card = findCard(command.cardId(), boardId);
        String body = valueOrDefault(command.commentBody(), "").trim();
        if (body.isBlank()) {
            return toSnapshot(board);
        }

        CardCommentEntity comment = new CardCommentEntity(
                "comment-" + UUID.randomUUID(),
                command.actor(),
                body,
                Instant.now()
        );
        card.addComment(comment);
        commentRepository.save(comment);

        addActivity(board, command.actor(), "commented on \"" + card.getTitle() + "\"");
        return toSnapshot(board);
    }

    private BoardSnapshot moveCard(String boardId, BoardCommand command) {
        BoardEntity board = findBoard(boardId);
        BoardListEntity source = findList(command.listId(), boardId);
        BoardListEntity target = findList(command.targetListId(), boardId);
        CardEntity card = findCard(command.cardId(), boardId);

        source.removeCard(card);
        normalizePositions(source);

        if (source.getId().equals(target.getId())) {
            int targetPosition = boundedPosition(command.targetPosition(), source.getCards().size());
            source.getCards().add(targetPosition, card);
            card.setList(source);
            normalizePositions(source);
            listRepository.save(source);
        } else {
            int targetPosition = boundedPosition(command.targetPosition(), target.getCards().size());
            target.getCards().sort(Comparator.comparingInt(CardEntity::getPosition));
            target.getCards().add(targetPosition, card);
            card.setList(target);
            normalizePositions(target);
            listRepository.save(source);
            listRepository.save(target);
        }

        addActivity(board, command.actor(), "moved \"" + card.getTitle() + "\" to " + target.getTitle());
        return toSnapshot(board);
    }

    private BoardSnapshot deleteCard(String boardId, BoardCommand command) {
        BoardEntity board = findBoard(boardId);
        BoardListEntity list = findList(command.listId(), boardId);
        CardEntity card = findCard(command.cardId(), boardId);
        String title = card.getTitle();

        list.removeCard(card);
        cardRepository.delete(card);
        normalizePositions(list);
        listRepository.save(list);

        addActivity(board, command.actor(), "deleted \"" + title + "\"");
        return toSnapshot(board);
    }

    private BoardEntity findBoard(String boardId) {
        return boardRepository.findWithListsById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("Board not found: " + boardId));
    }

    private BoardListEntity findList(String listId, String boardId) {
        BoardListEntity list = listRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("List not found: " + listId));
        if (!belongsToBoard(list, boardId)) {
            throw new IllegalArgumentException("List does not belong to board: " + boardId);
        }
        return list;
    }

    private CardEntity findCard(String cardId, String boardId) {
        CardEntity card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));
        if (!belongsToBoard(card, boardId)) {
            throw new IllegalArgumentException("Card does not belong to board: " + boardId);
        }
        return card;
    }

    private void normalizePositions(BoardListEntity list) {
        List<CardEntity> cards = list.getCards();
        cards.sort(Comparator.comparingInt(CardEntity::getPosition));
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).setPosition(i);
        }
    }

    private int boundedPosition(Integer requestedPosition, int listSize) {
        if (requestedPosition == null) {
            return listSize;
        }
        return Math.max(0, Math.min(requestedPosition, listSize));
    }

    private void normalizeListPositions(BoardEntity board) {
        List<BoardListEntity> lists = board.getLists();
        for (int i = 0; i < lists.size(); i++) {
            lists.get(i).setPosition(i);
        }
    }

    private void addActivity(BoardEntity board, String actor, String message) {
        activityRepository.save(new ActivityEventEntity(
                "evt-" + UUID.randomUUID(),
                board,
                actor,
                message,
                Instant.now()
        ));
    }

    private BoardSnapshot toSnapshot(BoardEntity board) {
        return new BoardSnapshot(
                board.getId(),
                board.getName(),
                board.getLists().stream()
                        .sorted(Comparator.comparingInt(BoardListEntity::getPosition))
                        .map(list -> new BoardList(
                                list.getId(),
                                list.getTitle(),
                                list.getPosition(),
                                list.getCards().stream()
                                        .sorted(Comparator.comparingInt(CardEntity::getPosition))
                                        .map(card -> new CardItem(
                                                card.getId(),
                                                card.getTitle(),
                                                card.getDescription(),
                                                card.getAssignee(),
                                                card.getPosition(),
                                                card.getComments().stream()
                                                        .map(comment -> new CardComment(
                                                                comment.getId(),
                                                                comment.getAuthor(),
                                                                comment.getBody(),
                                                                comment.getCreatedAt()
                                                        ))
                                                        .toList()
                                        ))
                                        .toList()
                        ))
                        .toList(),
                activityRepository.findTop30ByBoard_IdOrderByCreatedAtDesc(board.getId()).stream()
                        .map(event -> new ActivityEvent(
                                event.getId(),
                                event.getBoard().getId(),
                                event.getActor(),
                                event.getMessage(),
                                event.getCreatedAt()
                        ))
                        .toList(),
                List.copyOf(viewersFor(board.getId()))
        );
    }

    private BoardEntity createBoardEntity(String boardId, String name) {
        BoardEntity board = new BoardEntity(boardId, name);
        board.addList(new BoardListEntity("list-" + UUID.randomUUID(), "To do", 0));
        board.addList(new BoardListEntity("list-" + UUID.randomUUID(), "In progress", 1));
        board.addList(new BoardListEntity("list-" + UUID.randomUUID(), "Done", 2));
        return board;
    }

    private Set<String> viewersFor(String boardId) {
        return viewersByBoard.computeIfAbsent(boardId, ignored -> new LinkedHashSet<>());
    }

    private BoardMemberEntity requireMember(String boardId, AppUser user) {
        return memberRepository.findByBoardIdAndUser(boardId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Board membership required"));
    }

    private BoardMemberResponse toMemberResponse(BoardMemberEntity member) {
        AppUser user = member.getUser();
        return new BoardMemberResponse(user.getId(), user.getName(), user.getEmail(), member.getRole());
    }

    private boolean belongsToBoard(BoardListEntity list, String boardId) {
        return findBoard(boardId).getLists().stream().anyMatch(candidate -> candidate.getId().equals(list.getId()));
    }

    private boolean belongsToBoard(CardEntity card, String boardId) {
        return findBoard(boardId).getLists().stream()
                .flatMap(list -> list.getCards().stream())
                .anyMatch(candidate -> candidate.getId().equals(card.getId()));
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
