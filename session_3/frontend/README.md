# Example Company AI Assistant — Frontend

React chat UI for the Spring Boot chatbot backend.

## Prerequisites

- Node.js 20+
- Backend running on `http://localhost:8080`

## Run

```bash
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173).

API requests are proxied to the backend via Vite (`/api` → `http://localhost:8080`).

## Environment

Optional `.env` file:

```
VITE_API_BASE_URL=
```

Leave empty to use the Vite dev proxy. Set to the backend URL in production (e.g. `http://localhost:8080`).

## Build

```bash
npm run build
npm run preview
```
