# Phase 9: Frontend Dashboard and Operator Control Plane

## Objective

Turn the backend scheduler into a usable system that someone can explore from a browser: understand the architecture, submit real bounded workloads, switch scheduler policies, watch worker assignment, and inspect hard metrics.

## Implemented

- React + TypeScript + Vite frontend
- Clean white/blue dashboard UI
- Workload catalog for the seven supported worker jobs
- Guided job submission form with editable JSON payloads
- Active scheduling policy selector
- Job status timeline with selected policy and assigned worker
- Worker health and load overview
- Metrics playground with job and worker charts
- Recent jobs table for auditability
- Dockerized frontend served by nginx
- nginx proxy for `/api` and `/actuator` calls to the scheduler

## Backend Additions

- `GET /api/v1/policies`
- `PUT /api/v1/policies/active`
- `scheduledPolicy` persisted on jobs once dispatched
- Job responses now expose the scheduler policy used for assignment

## Frontend Stack

- React
- TypeScript
- Vite
- TanStack Query
- Recharts
- lucide-react

## Running

Docker Compose:

```bash
docker compose up --build
```

Open:

```text
http://localhost:5173
```

Local frontend development:

```bash
cd frontend
npm install
npm run dev
```

The local Vite server proxies API calls to the scheduler at `http://localhost:8080`.

## What Users Can See

- What the system does and how jobs move through it
- Which workload they are submitting
- Which scheduling policy is active
- Which worker picked up the job
- Whether the job is queued, running, completed, or failed
- Queue wait time, runtime, retry count, throughput, worker load, and completion mix

## Verification

```bash
cd frontend
npm run build

cd ..
mvn clean test
```
