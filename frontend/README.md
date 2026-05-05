# Event Hub Client

React/Vite SPA for the Spring Boot event-management API.

## Requirements

- Node.js LTS
- npm
- Backend running on `http://localhost:8080`

## Run

```bash
npm install
npm run dev
```

Vite serves the client on `http://localhost:5173` and proxies `/api` requests to the Spring Boot backend.

## Run with Docker

Start the full stack with one command from the project root:

```bash
docker compose up --build
```

The backend will be available on `http://localhost:8080`, the frontend on `http://localhost:3000`.
