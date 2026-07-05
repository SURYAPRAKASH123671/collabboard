# CollabBoard

A real-time Trello-style collaboration board built with React and Spring Boot WebSockets.

## Blueprint

CollabBoard follows the recommended middle-ground blueprint:

- Multiple Kanban boards with lists and cards
- List create, rename, delete-empty, and reorder controls
- Live updates over STOMP WebSockets
- Presence indicators for users viewing a board
- Activity feed for card/list events
- Card detail modal with editable fields
- Persisted card comments broadcast live to board viewers
- Board membership with owner/member roles and invite-by-email access control
- Optimistic React UI with server reconciliation

## Project Layout

```text
collabboard/
  backend/   Spring Boot REST + STOMP WebSocket API
  frontend/  React + Vite client
```

## Run Locally

Backend:

```bash
cd backend
mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

The frontend expects the backend at `http://localhost:8081`.

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

The frontend API base URL is controlled by `VITE_API_URL`.

For a frontend-only portfolio demo on Vercel, set `VITE_DEMO_MODE=true` to load an interactive seeded board without requiring the Spring Boot API. Full local/production collaboration still uses the backend, MySQL, JWT, and STOMP WebSockets.

## Deployment

Docker assets are included for the backend, frontend, and local MySQL deployment:

```bash
docker compose up --build
```

Compose exposes:

```text
Frontend: http://localhost:5173
Backend:  http://localhost:8081
MySQL:    localhost:3307
```

Use `.env.example` as the deployment checklist for database, JWT, CORS, and port settings. In production, replace `COLLABBOARD_JWT_SECRET`, set `COLLABBOARD_ALLOWED_ORIGINS` to the real frontend URL, and prefer managed MySQL plus migration tooling before public release.

## Production Demo Deployment

The public Vercel demo can run in frontend-only demo mode with `VITE_DEMO_MODE=true`. For a fully live real-time demo, deploy the Spring Boot backend and MySQL separately, then point Vercel at the hosted API.

Recommended Railway backend setup:

1. Create a Railway project from this GitHub repository.
2. Add a MySQL service to the same Railway project.
3. Deploy the backend service with root directory `/backend`.
4. Set Railway config file path to `/backend/railway.toml`.
5. Set backend environment variables:

```text
COLLABBOARD_DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
COLLABBOARD_DB_USERNAME=${{MySQL.MYSQLUSER}}
COLLABBOARD_DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
COLLABBOARD_JWT_SECRET=<64+ character random secret>
COLLABBOARD_DDL_AUTO=update
COLLABBOARD_ALLOWED_ORIGINS=https://collabboard-silk.vercel.app
```

After Railway gives the backend a public domain, verify:

```text
https://<railway-backend-domain>/actuator/health
```

Then update Vercel frontend variables:

```text
VITE_DEMO_MODE=false
VITE_API_URL=https://<railway-backend-domain>
```

## MySQL Persistence

The backend now persists boards, lists, cards, and activity events with Spring Data JPA. Presence stays in memory because it represents active viewers, not long-term project data.

Default local connection:

```properties
COLLABBOARD_DB_URL=jdbc:mysql://localhost:3306/collabboard?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
COLLABBOARD_DB_USERNAME=root
COLLABBOARD_DB_PASSWORD=
```

The demo board is seeded automatically only when the `demo-board` record does not exist.

## JWT Auth

REST board APIs are protected with stateless JWT authentication.

Auth endpoints:

```text
POST /api/auth/signup
POST /api/auth/login
GET  /api/auth/me
```

Send the token on protected REST calls:

```http
Authorization: Bearer <token>
```

Local token config:

```properties
COLLABBOARD_JWT_SECRET=collabboard-local-dev-secret-change-me-please-32chars
COLLABBOARD_JWT_ACCESS_TOKEN_SECONDS=3600
```

## Boards API

```text
GET  /api/boards
POST /api/boards
GET  /api/boards/{boardId}
GET  /api/boards/{boardId}/members
POST /api/boards/{boardId}/members
```

WebSocket clients subscribe and publish per board:

```text
/topic/boards/{boardId}
/app/boards/{boardId}/commands
/app/boards/{boardId}/presence/join
/app/boards/{boardId}/presence/leave
```

Supported board command types include:

```text
CREATE_LIST, UPDATE_LIST, DELETE_LIST, MOVE_LIST
CREATE_CARD, UPDATE_CARD, MOVE_CARD, DELETE_CARD
ADD_COMMENT
```

STOMP clients must send the JWT during `CONNECT`:

```text
Authorization: Bearer <token>
```

The backend derives the activity/presence actor from the authenticated WebSocket principal instead of trusting a client-sent name.

Board data is member-scoped. Users only see boards they own or have been invited to, and WebSocket joins/commands validate membership before returning or mutating board state.

## Week-by-week Build Plan

1. Week 1: Scaffold backend/frontend, model boards/lists/cards, create the first live board screen.
2. Week 2: Add REST CRUD and STOMP broadcasts for card create/update/move/delete.
3. Week 3: Add drag-and-drop ordering and optimistic UI reconciliation.
4. Week 4: Add presence tracking per board and a real-time activity feed.
5. Week 5: Add JWT auth, user-owned boards, and invite-ready board membership.
6. Week 6: Persist boards/lists/cards/users in MySQL with migrations.
7. Week 7: Add comments, card details, validation, and useful error states.
8. Week 8: Add concurrency polish: idempotent commands, versions, and stale update handling.
9. Week 9: Add tests for services, WebSocket flows, and critical React interactions.
10. Week 10: Improve UI, responsive behavior, and demo seed data.
11. Week 11: Add observability-friendly activity history and deployment config.
12. Week 12: Final demo script, README screenshots, architecture diagram, and interview talking points.
