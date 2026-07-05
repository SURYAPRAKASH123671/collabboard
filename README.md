# CollabBoard

Real-time Trello-style collaboration board built with React, Spring Boot, JWT authentication, MySQL, and STOMP WebSockets.

[Live Demo](https://collabboard-silk.vercel.app) | [GitHub](https://github.com/SURYAPRAKASH123671/collabboard)

> The public Vercel deployment runs in frontend demo mode so visitors can open the board instantly. The full backend stack is included in this repository and runs locally with Spring Boot, MySQL, JWT, and authenticated WebSockets.

## Highlights

- Multi-board Kanban workspace with lists, cards, drag-and-drop ordering, and board switching.
- JWT-secured REST APIs for signup, login, board access, and membership workflows.
- Authenticated STOMP WebSocket handshake for live board commands and presence events.
- Real-time card/list updates, comments, activity feed entries, and viewer presence indicators.
- MySQL persistence for users, boards, members, lists, cards, comments, and activity events.
- Optimistic React UI with server reconciliation for smoother collaboration behavior.
- Docker and Docker Compose setup for local full-stack execution.
- Backend test coverage plus frontend production build verification.

## Tech Stack

| Layer | Tools |
| --- | --- |
| Frontend | React, Vite, JavaScript, CSS |
| Backend | Java, Spring Boot, Spring Security, Spring Data JPA |
| Realtime | STOMP over WebSocket |
| Auth | JWT, BCrypt |
| Database | MySQL |
| DevOps | Docker, Docker Compose, Vercel, Railway-ready backend config |
| Testing | JUnit, Spring Boot Test, Maven, Vite build |

## Architecture

```text
React Client
  |-- REST: auth, boards, members
  |-- STOMP WebSocket: board commands, presence
        |
Spring Boot API
  |-- AuthController
  |-- BoardRestController
  |-- BoardWebSocketController
  |-- Service layer
  |-- JPA repositories
        |
MySQL
```

## Project Structure

```text
collabboard/
  backend/          Spring Boot REST + STOMP WebSocket API
  frontend/         React + Vite client
  docker-compose.yml
  .env.example
```

## Run Locally

Start the backend:

```bash
cd backend
mvn spring-boot:run
```

Start the frontend:

```bash
cd frontend
npm install
npm run dev
```

The frontend expects the backend at:

```text
http://localhost:8081
```

## Run With Docker

```bash
docker compose up --build
```

Local services:

```text
Frontend: http://localhost:5173
Backend:  http://localhost:8081
MySQL:    localhost:3307
```

Use `.env.example` as the environment checklist for database credentials, JWT secret, CORS origins, and ports.

## Verification

Backend tests:

```bash
cd backend
mvn test
```

Frontend install, audit, and production build:

```bash
cd frontend
npm install
npm audit
npm run build
```

## API Overview

Auth endpoints:

```text
POST /api/auth/signup
POST /api/auth/login
GET  /api/auth/me
```

Board endpoints:

```text
GET  /api/boards
POST /api/boards
GET  /api/boards/{boardId}
GET  /api/boards/{boardId}/members
POST /api/boards/{boardId}/members
```

WebSocket destinations:

```text
/topic/boards/{boardId}
/app/boards/{boardId}/commands
/app/boards/{boardId}/presence/join
/app/boards/{boardId}/presence/leave
```

Supported board command types:

```text
CREATE_LIST, UPDATE_LIST, DELETE_LIST, MOVE_LIST
CREATE_CARD, UPDATE_CARD, MOVE_CARD, DELETE_CARD
ADD_COMMENT
```

REST and WebSocket clients must send:

```http
Authorization: Bearer <token>
```

## Production Notes

The public Vercel link is configured with:

```text
VITE_DEMO_MODE=true
```

That mode loads a seeded interactive board without requiring a hosted backend. To connect the public frontend to the real backend, deploy the Spring Boot API and MySQL, then set:

```text
VITE_DEMO_MODE=false
VITE_API_URL=https://<hosted-backend-domain>
```

Railway backend configuration is included in `backend/railway.toml`, including an actuator health check at:

```text
/actuator/health
```

Required backend environment variables:

```text
COLLABBOARD_DB_URL=jdbc:mysql://<host>:<port>/<database>?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
COLLABBOARD_DB_USERNAME=<username>
COLLABBOARD_DB_PASSWORD=<password>
COLLABBOARD_JWT_SECRET=<64+ character random secret>
COLLABBOARD_DDL_AUTO=update
COLLABBOARD_ALLOWED_ORIGINS=https://collabboard-silk.vercel.app
```

## Interview Talking Points

- Authenticated WebSocket handshake instead of trusting client-sent user names.
- Board-level authorization for REST and WebSocket actions.
- Optimistic UI updates reconciled with server broadcasts.
- Real-time activity feed as an event-history style collaboration trail.
- Separate frontend demo mode for portfolio access while preserving a complete backend implementation.
