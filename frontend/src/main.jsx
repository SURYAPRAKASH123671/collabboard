import React, { useEffect, useMemo, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  DndContext,
  KeyboardSensor,
  PointerSensor,
  closestCorners,
  useDroppable,
  useSensor,
  useSensors
} from '@dnd-kit/core';
import {
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import './styles.css';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8081';
const DEMO_MODE = import.meta.env.VITE_DEMO_MODE === 'true';

const createDemoBoard = () => ({
  id: 'demo-board',
  name: 'Product Launch Board',
  viewers: ['Surya', 'Ravi'],
  lists: [
    {
      id: 'todo',
      title: 'Todo',
      position: 0,
      cards: [
        {
          id: 'card-auth',
          title: 'JWT auth flow',
          description: 'Protect REST APIs and WebSocket handshakes with authenticated user context.',
          assignee: 'Surya',
          position: 0,
          comments: [
            {
              id: 'comment-auth',
              author: 'Ravi',
              body: 'Handshake validation is ready for demo.',
              createdAt: new Date().toISOString()
            }
          ]
        },
        {
          id: 'card-deploy',
          title: 'Deployment polish',
          description: 'Verify production build, environment variables, and README instructions.',
          assignee: 'Priya',
          position: 1,
          comments: []
        }
      ]
    },
    {
      id: 'doing',
      title: 'Doing',
      position: 1,
      cards: [
        {
          id: 'card-realtime',
          title: 'Real-time board sync',
          description: 'Broadcast card movement, edits, comments, and activity updates through STOMP.',
          assignee: 'Surya',
          position: 0,
          comments: []
        }
      ]
    },
    {
      id: 'done',
      title: 'Done',
      position: 2,
      cards: [
        {
          id: 'card-mysql',
          title: 'MySQL persistence',
          description: 'Persist users, boards, members, lists, cards, comments, and activity events.',
          assignee: 'Surya',
          position: 0,
          comments: []
        }
      ]
    }
  ],
  activity: [
    {
      id: 'activity-1',
      actor: 'Surya',
      message: 'moved Real-time board sync to Doing',
      createdAt: new Date().toISOString()
    },
    {
      id: 'activity-2',
      actor: 'Priya',
      message: 'commented on JWT auth flow',
      createdAt: new Date().toISOString()
    }
  ]
});

function SortableCard({ card, list, lists, onOpenCard, onMoveCard, onDeleteCard }) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging
  } = useSortable({
    id: card.id,
    data: { type: 'card', cardId: card.id, listId: list.id }
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition
  };

  return (
    <article className={isDragging ? 'card dragging' : 'card'} ref={setNodeRef} style={style}>
      <div className="card-topline">
        <button className="drag-handle" type="button" title="Drag card" {...attributes} {...listeners}>
          ::
        </button>
        <button className="card-title" type="button" onClick={() => onOpenCard(card.id)}>
          {card.title}
        </button>
      </div>
      <p>{card.description}</p>
      <div className="card-meta">
        <span>{card.comments?.length || 0} comments</span>
      </div>
      <footer>
        <span>{card.assignee}</span>
        <select
          aria-label={`Move ${card.title}`}
          value={list.id}
          onChange={(event) => onMoveCard(list.id, card.id, event.target.value, lists.find((target) => target.id === event.target.value)?.cards.length || 0)}
        >
          {lists.map((target) => (
            <option key={target.id} value={target.id}>
              {target.title}
            </option>
          ))}
        </select>
        <button type="button" onClick={() => onDeleteCard(list.id, card.id)} title="Delete card">
          Delete
        </button>
      </footer>
    </article>
  );
}

function CardDetailModal({ card, actor, onClose, onSave, onAddComment }) {
  const [draft, setDraft] = useState({
    title: card.title,
    description: card.description,
    assignee: card.assignee
  });
  const [commentBody, setCommentBody] = useState('');

  useEffect(() => {
    setDraft({
      title: card.title,
      description: card.description,
      assignee: card.assignee
    });
  }, [card.id, card.title, card.description, card.assignee]);

  function submitDetails(event) {
    event.preventDefault();
    onSave(card.id, draft);
  }

  function submitComment(event) {
    event.preventDefault();
    const body = commentBody.trim();
    if (!body) return;
    onAddComment(card.id, body);
    setCommentBody('');
  }

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="card-modal" role="dialog" aria-modal="true" aria-label={`${card.title} details`} onMouseDown={(event) => event.stopPropagation()}>
        <header className="modal-header">
          <div>
            <p className="eyebrow">Card details</p>
            <h2>{card.title}</h2>
          </div>
          <button type="button" onClick={onClose} title="Close card details">
            X
          </button>
        </header>

        <form className="detail-form" onSubmit={submitDetails}>
          <label>
            Title
            <input
              value={draft.title}
              onChange={(event) => setDraft({ ...draft, title: event.target.value })}
            />
          </label>
          <label>
            Description
            <textarea
              value={draft.description}
              onChange={(event) => setDraft({ ...draft, description: event.target.value })}
            />
          </label>
          <label>
            Assignee
            <input
              value={draft.assignee}
              onChange={(event) => setDraft({ ...draft, assignee: event.target.value })}
            />
          </label>
          <button type="submit">Save details</button>
        </form>

        <section className="comments-panel">
          <h3>Comments</h3>
          <form className="comment-form" onSubmit={submitComment}>
            <textarea
              aria-label="Add comment"
              placeholder={`Comment as ${actor}`}
              value={commentBody}
              onChange={(event) => setCommentBody(event.target.value)}
            />
            <button type="submit">Add comment</button>
          </form>
          <ol className="comments-list">
            {(card.comments || []).length === 0 ? <li className="empty-comment">No comments yet</li> : null}
            {(card.comments || []).map((comment) => (
              <li key={comment.id}>
                <div>
                  <strong>{comment.author}</strong>
                  <time>{new Date(comment.createdAt).toLocaleString()}</time>
                </div>
                <p>{comment.body}</p>
              </li>
            ))}
          </ol>
        </section>
      </section>
    </div>
  );
}

function DroppableList({ list, listIndex, lists, children, onCreateCard, onRenameList, onDeleteList, onMoveList }) {
  const { setNodeRef, isOver } = useDroppable({
    id: list.id,
    data: { type: 'list', listId: list.id }
  });

  return (
    <section className={isOver ? 'list over' : 'list'} ref={setNodeRef}>
      <header className="list-header">
        <h2>{list.title}</h2>
        <div className="list-actions">
          <button type="button" onClick={() => onMoveList(list.id, listIndex - 1)} disabled={listIndex === 0} title="Move list left">
            &lt;
          </button>
          <button type="button" onClick={() => onMoveList(list.id, listIndex + 1)} disabled={listIndex === lists.length - 1} title="Move list right">
            &gt;
          </button>
          <button type="button" onClick={() => onRenameList(list)} title="Rename list">
            Edit
          </button>
          <button type="button" onClick={() => onDeleteList(list)} disabled={list.cards.length > 0} title="Delete empty list">
            Del
          </button>
          <button type="button" onClick={() => onCreateCard(list.id)} title="Add card">
            +
          </button>
        </div>
      </header>
      <SortableContext items={list.cards.map((card) => card.id)} strategy={verticalListSortingStrategy}>
        <div className="cards">{children}</div>
      </SortableContext>
      {list.cards.length === 0 ? <div className="drop-hint">Drop a card here</div> : null}
    </section>
  );
}

function App() {
  const [token, setToken] = useState(DEMO_MODE ? 'demo-token' : localStorage.getItem('collabboard.token') || '');
  const [authMode, setAuthMode] = useState('login');
  const [authForm, setAuthForm] = useState({
    name: 'Surya',
    email: 'surya@example.com',
    password: 'password123'
  });
  const [authError, setAuthError] = useState('');
  const [actor, setActor] = useState(localStorage.getItem('collabboard.actor') || 'Surya');
  const [boards, setBoards] = useState(DEMO_MODE ? [{ id: 'demo-board', name: 'Product Launch Board' }] : []);
  const [selectedBoardId, setSelectedBoardId] = useState(DEMO_MODE ? 'demo-board' : localStorage.getItem('collabboard.boardId') || '');
  const [board, setBoard] = useState(DEMO_MODE ? createDemoBoard() : null);
  const [members, setMembers] = useState(DEMO_MODE ? [
    { name: 'Surya', email: 'surya@example.com', role: 'OWNER' },
    { name: 'Ravi', email: 'ravi@example.com', role: 'MEMBER' },
    { name: 'Priya', email: 'priya@example.com', role: 'MEMBER' }
  ] : []);
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteError, setInviteError] = useState('');
  const [query, setQuery] = useState('');
  const [connected, setConnected] = useState(DEMO_MODE);
  const [selectedCardId, setSelectedCardId] = useState('');
  const stompRef = useRef(null);
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates })
  );

  useEffect(() => {
    localStorage.setItem('collabboard.actor', actor);
  }, [actor]);

  useEffect(() => {
    if (DEMO_MODE) return;
    if (!token) return;

    fetch(`${API_URL}/api/boards`, {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then((response) => {
        if (!response.ok) throw new Error('Could not load boards');
        return response.json();
      })
      .then((payload) => {
        setBoards(payload);
        if (payload.length > 0 && (!selectedBoardId || !payload.some((summary) => summary.id === selectedBoardId))) {
          setSelectedBoardId(payload[0].id);
        }
      })
      .catch(() => {
        localStorage.removeItem('collabboard.token');
        setToken('');
        setBoard(null);
      });
  }, [selectedBoardId, token]);

  useEffect(() => {
    if (DEMO_MODE) return;
    if (!token || !selectedBoardId) return;

    localStorage.setItem('collabboard.boardId', selectedBoardId);
    setBoard(null);
    setMembers([]);
    setSelectedCardId('');

    fetch(`${API_URL}/api/boards/${selectedBoardId}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then((response) => {
        if (!response.ok) throw new Error('Could not load board');
        return response.json();
      })
      .then(setBoard)
      .catch(() => setBoard(null));

    fetch(`${API_URL}/api/boards/${selectedBoardId}/members`, {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then((response) => {
        if (!response.ok) throw new Error('Could not load members');
        return response.json();
      })
      .then(setMembers)
      .catch(() => setMembers([]));
  }, [selectedBoardId, token]);

  useEffect(() => {
    if (DEMO_MODE) return undefined;
    if (!token || !selectedBoardId) return undefined;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_URL}/ws`),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      reconnectDelay: 2500,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/boards/${selectedBoardId}`, (message) => {
          setBoard(JSON.parse(message.body));
        });
        client.publish({
          destination: `/app/boards/${selectedBoardId}/presence/join`
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false)
    });

    stompRef.current = client;
    client.activate();

    return () => {
      if (client.connected) {
        client.publish({
          destination: `/app/boards/${selectedBoardId}/presence/leave`
        });
      }
      client.deactivate();
    };
  }, [actor, selectedBoardId, token]);

  const filteredLists = useMemo(() => {
    if (!board) return [];
    const needle = query.trim().toLowerCase();
    if (!needle) return board.lists;

    return board.lists.map((list) => ({
      ...list,
      cards: list.cards.filter((card) =>
        [card.title, card.description, card.assignee].some((value) =>
          value.toLowerCase().includes(needle)
        )
      )
    }));
  }, [board, query]);

  const selectedCard = useMemo(() => {
    if (!board || !selectedCardId) return null;
    for (const list of board.lists) {
      const card = list.cards.find((candidate) => candidate.id === selectedCardId);
      if (card) return card;
    }
    return null;
  }, [board, selectedCardId]);

  async function submitAuth(event) {
    event.preventDefault();
    setAuthError('');
    if (DEMO_MODE) return;
    const path = authMode === 'signup' ? 'signup' : 'login';
    const body = authMode === 'signup' ? authForm : { email: authForm.email, password: authForm.password };

    try {
      const response = await fetch(`${API_URL}/api/auth/${path}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });
      if (!response.ok) throw new Error('Authentication failed');
      const payload = await response.json();
      localStorage.setItem('collabboard.token', payload.token);
      localStorage.setItem('collabboard.actor', payload.user.name);
      setToken(payload.token);
      setActor(payload.user.name);
    } catch (error) {
      setAuthError(error.message);
    }
  }

  function logout() {
    if (DEMO_MODE) return;
    localStorage.removeItem('collabboard.token');
    localStorage.removeItem('collabboard.boardId');
    setToken('');
    setBoards([]);
    setSelectedBoardId('');
    setBoard(null);
    setMembers([]);
    setConnected(false);
    stompRef.current?.deactivate();
  }

  async function createBoard() {
    const name = window.prompt('Board name');
    if (!name?.trim()) return;

    if (DEMO_MODE) {
      const createdBoard = {
        id: `board-${Date.now()}`,
        name: name.trim(),
        viewers: [actor],
        lists: [],
        activity: []
      };
      setBoards((currentBoards) => [...currentBoards, { id: createdBoard.id, name: createdBoard.name }]);
      setSelectedBoardId(createdBoard.id);
      setBoard(createdBoard);
      return;
    }

    const response = await fetch(`${API_URL}/api/boards`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ name: name.trim() })
    });
    if (!response.ok) return;
    const createdBoard = await response.json();
    setBoards((currentBoards) => [...currentBoards, { id: createdBoard.id, name: createdBoard.name }]);
    setSelectedBoardId(createdBoard.id);
    setBoard(createdBoard);
  }

  async function inviteMember(event) {
    event.preventDefault();
    setInviteError('');
    const email = inviteEmail.trim();
    if (!email || !selectedBoardId) return;

    if (DEMO_MODE) {
      const member = { name: email.split('@')[0], email, role: 'MEMBER' };
      setMembers((currentMembers) => [...currentMembers, member]);
      setInviteEmail('');
      return;
    }

    const response = await fetch(`${API_URL}/api/boards/${selectedBoardId}/members`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ email })
    });
    if (!response.ok) {
      setInviteError(response.status === 404 ? 'User not found' : 'Only owners/admins can invite members');
      return;
    }
    const member = await response.json();
    setMembers((currentMembers) => {
      if (currentMembers.some((candidate) => candidate.email === member.email)) return currentMembers;
      return [...currentMembers, member].sort((a, b) => a.name.localeCompare(b.name));
    });
    setInviteEmail('');
  }

  function publish(command) {
    if (DEMO_MODE) {
      applyDemoCommand(command);
      return;
    }
    const client = stompRef.current;
    if (!client?.connected || !selectedBoardId) return;
    client.publish({
      destination: `/app/boards/${selectedBoardId}/commands`,
      body: JSON.stringify(command)
    });
  }

  function createCard(listId) {
    const title = window.prompt('Card title');
    if (!title) return;
    publish({
      type: 'CREATE_CARD',
      listId,
      title,
      description: 'New task ready for refinement.',
      assignee: actor
    });
  }

  function createList() {
    const title = window.prompt('List title');
    if (!title?.trim()) return;
    publish({ type: 'CREATE_LIST', title: title.trim() });
  }

  function renameList(list) {
    const title = window.prompt('Rename list', list.title);
    if (!title?.trim()) return;
    publish({ type: 'UPDATE_LIST', listId: list.id, title: title.trim() });
  }

  function deleteList(list) {
    if (list.cards.length > 0) return;
    publish({ type: 'DELETE_LIST', listId: list.id });
  }

  function moveList(listId, targetPosition) {
    publish({ type: 'MOVE_LIST', listId, targetPosition });
  }

  function updateCard(cardId, draft) {
    publish({
      type: 'UPDATE_CARD',
      cardId,
      title: draft.title,
      description: draft.description,
      assignee: draft.assignee
    });
  }

  function addComment(cardId, commentBody) {
    publish({ type: 'ADD_COMMENT', cardId, commentBody });
  }

  function moveCard(listId, cardId, targetListId, targetPosition = 0) {
    publish({ type: 'MOVE_CARD', listId, cardId, targetListId, targetPosition });
  }

  function deleteCard(listId, cardId) {
    publish({ type: 'DELETE_CARD', listId, cardId });
  }

  function appendDemoActivity(sourceBoard, message) {
    return {
      ...sourceBoard,
      activity: [
        {
          id: `activity-${Date.now()}`,
          actor,
          message,
          createdAt: new Date().toISOString()
        },
        ...sourceBoard.activity
      ].slice(0, 20)
    };
  }

  function applyDemoCommand(command) {
    if (command.type === 'MOVE_CARD') return;

    setBoard((currentBoard) => {
      if (!currentBoard) return currentBoard;

      if (command.type === 'CREATE_LIST') {
        const nextBoard = {
          ...currentBoard,
          lists: [
            ...currentBoard.lists,
            {
              id: `list-${Date.now()}`,
              title: command.title,
              position: currentBoard.lists.length,
              cards: []
            }
          ]
        };
        return appendDemoActivity(nextBoard, `created list ${command.title}`);
      }

      if (command.type === 'UPDATE_LIST') {
        const nextBoard = {
          ...currentBoard,
          lists: currentBoard.lists.map((list) =>
            list.id === command.listId ? { ...list, title: command.title } : list
          )
        };
        return appendDemoActivity(nextBoard, `renamed a list to ${command.title}`);
      }

      if (command.type === 'DELETE_LIST') {
        const list = currentBoard.lists.find((candidate) => candidate.id === command.listId);
        const nextBoard = {
          ...currentBoard,
          lists: currentBoard.lists.filter((candidate) => candidate.id !== command.listId)
        };
        return appendDemoActivity(nextBoard, `deleted list ${list?.title || 'Untitled'}`);
      }

      if (command.type === 'MOVE_LIST') {
        const lists = [...currentBoard.lists];
        const currentIndex = lists.findIndex((list) => list.id === command.listId);
        if (currentIndex < 0) return currentBoard;
        const targetIndex = Math.max(0, Math.min(command.targetPosition, lists.length - 1));
        const [movingList] = lists.splice(currentIndex, 1);
        lists.splice(targetIndex, 0, movingList);
        return appendDemoActivity({ ...currentBoard, lists }, `moved list ${movingList.title}`);
      }

      if (command.type === 'CREATE_CARD') {
        const nextCard = {
          id: `card-${Date.now()}`,
          title: command.title,
          description: command.description,
          assignee: command.assignee,
          position: 0,
          comments: []
        };
        const nextBoard = {
          ...currentBoard,
          lists: currentBoard.lists.map((list) =>
            list.id === command.listId ? { ...list, cards: [...list.cards, nextCard] } : list
          )
        };
        return appendDemoActivity(nextBoard, `created card ${command.title}`);
      }

      if (command.type === 'UPDATE_CARD') {
        const nextBoard = {
          ...currentBoard,
          lists: currentBoard.lists.map((list) => ({
            ...list,
            cards: list.cards.map((card) =>
              card.id === command.cardId
                ? { ...card, title: command.title, description: command.description, assignee: command.assignee }
                : card
            )
          }))
        };
        return appendDemoActivity(nextBoard, `updated card ${command.title}`);
      }

      if (command.type === 'ADD_COMMENT') {
        const nextBoard = {
          ...currentBoard,
          lists: currentBoard.lists.map((list) => ({
            ...list,
            cards: list.cards.map((card) =>
              card.id === command.cardId
                ? {
                    ...card,
                    comments: [
                      ...(card.comments || []),
                      {
                        id: `comment-${Date.now()}`,
                        author: actor,
                        body: command.commentBody,
                        createdAt: new Date().toISOString()
                      }
                    ]
                  }
                : card
            )
          }))
        };
        return appendDemoActivity(nextBoard, 'added a card comment');
      }

      if (command.type === 'DELETE_CARD') {
        const nextBoard = {
          ...currentBoard,
          lists: currentBoard.lists.map((list) => ({
            ...list,
            cards: list.cards.filter((card) => card.id !== command.cardId)
          }))
        };
        return appendDemoActivity(nextBoard, 'deleted a card');
      }

      return currentBoard;
    });
  }

  function findCardLocation(cardId, sourceBoard = board) {
    for (const list of sourceBoard.lists) {
      const cardIndex = list.cards.findIndex((card) => card.id === cardId);
      if (cardIndex >= 0) {
        return { list, cardIndex, card: list.cards[cardIndex] };
      }
    }
    return null;
  }

  function resolveDropTarget(over) {
    if (!over) return null;
    if (over.data.current?.type === 'list') {
      const list = board.lists.find((candidate) => candidate.id === over.id);
      return list ? { list, cardIndex: list.cards.length } : null;
    }
    return findCardLocation(over.id);
  }

  function reorderBoard(cardId, sourceListId, targetListId, targetPosition) {
    setBoard((currentBoard) => {
      if (!currentBoard) return currentBoard;

      const sourceList = currentBoard.lists.find((list) => list.id === sourceListId);
      const movingCard = sourceList?.cards.find((card) => card.id === cardId);
      if (!sourceList || !movingCard) return currentBoard;

      return {
        ...currentBoard,
        lists: currentBoard.lists.map((list) => {
          const withoutMovingCard = list.cards.filter((card) => card.id !== cardId);
          if (list.id !== targetListId) {
            return list.id === sourceListId ? { ...list, cards: withoutMovingCard } : list;
          }

          const nextCards = [...withoutMovingCard];
          const safePosition = Math.max(0, Math.min(targetPosition, nextCards.length));
          nextCards.splice(safePosition, 0, movingCard);
          return {
            ...list,
            cards: nextCards.map((card, index) => ({ ...card, position: index }))
          };
        })
      };
    });
  }

  function handleDragEnd(event) {
    const { active, over } = event;
    if (!board || !over || active.id === over.id) return;

    const source = findCardLocation(active.id);
    const target = resolveDropTarget(over);
    if (!source || !target) return;

    const targetPosition = target.cardIndex;
    reorderBoard(active.id, source.list.id, target.list.id, targetPosition);
    moveCard(source.list.id, active.id, target.list.id, targetPosition);
  }

  if (!token) {
    return (
      <main className="shell auth-shell">
        <section className="auth-panel">
          <p className="eyebrow">CollabBoard</p>
          <h1>{authMode === 'signup' ? 'Create your account' : 'Welcome back'}</h1>
          <form onSubmit={submitAuth}>
            {authMode === 'signup' ? (
              <label>
                Name
                <input
                  value={authForm.name}
                  onChange={(event) => setAuthForm({ ...authForm, name: event.target.value })}
                />
              </label>
            ) : null}
            <label>
              Email
              <input
                type="email"
                value={authForm.email}
                onChange={(event) => setAuthForm({ ...authForm, email: event.target.value })}
              />
            </label>
            <label>
              Password
              <input
                type="password"
                value={authForm.password}
                onChange={(event) => setAuthForm({ ...authForm, password: event.target.value })}
              />
            </label>
            {authError ? <p className="auth-error">{authError}</p> : null}
            <button type="submit">{authMode === 'signup' ? 'Sign up' : 'Log in'}</button>
          </form>
          <button
            className="link-button"
            type="button"
            onClick={() => setAuthMode(authMode === 'signup' ? 'login' : 'signup')}
          >
            {authMode === 'signup' ? 'Use an existing account' : 'Create a new account'}
          </button>
        </section>
      </main>
    );
  }

  if (!board) {
    return (
      <main className="shell">
        <section className="empty-state">
          <h1>CollabBoard</h1>
          <p>{boards.length === 0 ? 'Create your first board to start collaborating.' : 'Loading board...'}</p>
          <button type="button" onClick={createBoard}>
            New board
          </button>
        </section>
      </main>
    );
  }

  return (
    <main className="shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Real-time collaboration</p>
          <h1>{board.name}</h1>
        </div>
        <div className="toolbar">
          <select
            aria-label="Select board"
            value={selectedBoardId}
            onChange={(event) => setSelectedBoardId(event.target.value)}
          >
            {boards.map((summary) => (
              <option key={summary.id} value={summary.id}>
                {summary.name}
              </option>
            ))}
          </select>
          <button className="new-board-button" type="button" onClick={createBoard}>
            New board
          </button>
          <button className="new-board-button" type="button" onClick={createList}>
            New list
          </button>
          <input
            aria-label="Viewer name"
            value={actor}
            readOnly
            title="Viewer name comes from your JWT session"
          />
          <input
            aria-label="Search cards"
            placeholder="Search cards"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
          <span className={connected ? 'status online' : 'status'}>{connected ? 'Live' : 'Offline'}</span>
          <button className="logout-button" type="button" onClick={logout}>
            Logout
          </button>
        </div>
      </header>

      <section className="workspace">
        <DndContext sensors={sensors} collisionDetection={closestCorners} onDragEnd={handleDragEnd}>
          <div className="board">
            {filteredLists.map((list, listIndex) => (
              <DroppableList
                key={list.id}
                list={list}
                listIndex={listIndex}
                lists={board.lists}
                onCreateCard={createCard}
                onRenameList={renameList}
                onDeleteList={deleteList}
                onMoveList={moveList}
              >
                {list.cards.map((card) => (
                  <SortableCard
                    key={card.id}
                    card={card}
                    list={list}
                    lists={board.lists}
                    onOpenCard={setSelectedCardId}
                    onMoveCard={moveCard}
                    onDeleteCard={deleteCard}
                  />
                ))}
              </DroppableList>
            ))}
          </div>
        </DndContext>

        <aside className="side-panel">
          <section>
            <h2>Viewing</h2>
            <div className="presence">
              {board.viewers.length === 0 ? <span>No one yet</span> : null}
              {board.viewers.map((viewer) => (
                <span key={viewer}>{viewer}</span>
              ))}
            </div>
          </section>

          <section>
            <h2>Members</h2>
            <form className="member-form" onSubmit={inviteMember}>
              <input
                type="email"
                placeholder="Invite by email"
                value={inviteEmail}
                onChange={(event) => setInviteEmail(event.target.value)}
              />
              <button type="submit">Invite</button>
            </form>
            {inviteError ? <p className="invite-error">{inviteError}</p> : null}
            <div className="members-list">
              {members.map((member) => (
                <div key={member.email}>
                  <span>{member.name}</span>
                  <strong>{member.role}</strong>
                </div>
              ))}
            </div>
          </section>

          <section>
            <h2>Activity</h2>
            <ol className="activity">
              {board.activity.map((event) => (
                <li key={event.id}>
                  <strong>{event.actor}</strong> {event.message}
                  <time>{new Date(event.createdAt).toLocaleTimeString()}</time>
                </li>
              ))}
            </ol>
          </section>
        </aside>
      </section>

      {selectedCard ? (
        <CardDetailModal
          card={selectedCard}
          actor={actor}
          onClose={() => setSelectedCardId('')}
          onSave={updateCard}
          onAddComment={addComment}
        />
      ) : null}
    </main>
  );
}

createRoot(document.getElementById('root')).render(<App />);
