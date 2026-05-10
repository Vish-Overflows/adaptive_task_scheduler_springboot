# Phase 2: Job Submission and Persistence

## Objective

Allow clients to submit jobs to the scheduler, persist those jobs in PostgreSQL, and enqueue job IDs in Redis for future scheduling.

Phase 2 does not execute jobs. It establishes the durable job intake path.

## Implemented

- `jobs` PostgreSQL table managed by Flyway
- Job lifecycle status model
- Validated job submission API
- Job lookup API
- Paginated job listing API
- Optional idempotency key support
- Redis pending-job queue insertion
- Consistent RFC 7807-style error responses
- Unit tests for core submission behavior

## API

### Submit Job

```http
POST /api/v1/jobs
Content-Type: application/json
```

```json
{
  "type": "image_render",
  "priority": 100,
  "payload": {
    "inputSize": 42,
    "complexity": "medium"
  },
  "estimatedDurationMs": 1500,
  "idempotencyKey": "demo-job-001"
}
```

Returns `201 Created` for a new job. If the same `idempotencyKey` already exists, returns `200 OK` with the existing job and does not enqueue a duplicate.

### Get Job

```http
GET /api/v1/jobs/{jobId}
```

### List Jobs

```http
GET /api/v1/jobs?page=0&size=50
GET /api/v1/jobs?status=QUEUED&page=0&size=50
```

## Job Statuses

- `QUEUED`
- `SCHEDULED`
- `RUNNING`
- `COMPLETED`
- `FAILED`

Only `QUEUED` is used in Phase 2. Later phases will transition jobs through the full lifecycle.

## Redis Queue

Submitted job IDs are pushed into:

```text
orchestrator:jobs:pending
```

This key can be changed with:

```text
ORCHESTRATOR_PENDING_JOBS_KEY
```

## Validation Rules

- `type` is required and may contain letters, numbers, underscores, and hyphens.
- `priority` defaults to `100` and must be between `0` and `1000`.
- `payload` is required and must be a JSON object.
- `estimatedDurationMs` must be positive and cannot exceed 24 hours.
- `idempotencyKey` is optional and must be at most 120 characters.

## Phase 3 Entry Criteria

Before Phase 3, this should work:

```bash
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "type": "cpu_benchmark",
    "priority": 100,
    "payload": {"inputSize": 128},
    "estimatedDurationMs": 2000,
    "idempotencyKey": "phase2-demo-1"
  }'

curl http://localhost:8080/api/v1/jobs
```

Phase 3 will add worker registration and heartbeat tracking.
