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

Start the Spring Boot backend on `http://localhost:8080`, then run:

```bash
docker compose -f docker-compose.client.yml up --build
```

The client will be available on `http://localhost:3000`.
