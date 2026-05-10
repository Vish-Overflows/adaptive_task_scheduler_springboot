# Phase 3: Worker Registration and Heartbeats

## Objective

Make the scheduler aware of worker nodes and continuously track worker health.

Phase 3 does not execute jobs yet. It establishes cluster membership, worker metadata, and heartbeat-based health tracking.

## Implemented

- Worker registration endpoint in scheduler
- Worker heartbeat endpoint in scheduler
- Redis-backed worker registry
- Worker-side automatic registration on startup
- Worker-side periodic heartbeats
- Stale heartbeat detection
- Worker status model:
  - `ACTIVE`
  - `UNHEALTHY`
- Worker lookup API
- Worker listing API with active/unhealthy counts
- Unit tests for worker registry behavior

## API

### Register Worker

```http
POST /api/v1/workers/register
Content-Type: application/json
```

```json
{
  "workerId": "worker-1",
  "serviceName": "worker-service",
  "version": "0.1.0",
  "environment": "docker",
  "baseUrl": "http://worker-1:8081",
  "maxConcurrentJobs": 4
}
```

Workers call this automatically on startup.

### Send Heartbeat

```http
POST /api/v1/workers/heartbeat
Content-Type: application/json
```

```json
{
  "workerId": "worker-1",
  "activeJobCount": 0,
  "maxConcurrentJobs": 4,
  "loadScore": 0.0
}
```

Workers send this automatically every few seconds.

### List Workers

```http
GET /api/v1/workers
```

Example response:

```json
{
  "workers": [
    {
      "workerId": "worker-1",
      "serviceName": "worker-service",
      "version": "0.1.0",
      "environment": "docker",
      "baseUrl": "http://worker-1:8081",
      "maxConcurrentJobs": 4,
      "activeJobCount": 0,
      "loadScore": 0.0,
      "status": "ACTIVE",
      "registeredAt": "2026-05-05T01:30:00Z",
      "lastHeartbeatAt": "2026-05-05T01:30:05Z",
      "updatedAt": "2026-05-05T01:30:05Z"
    }
  ],
  "totalWorkers": 1,
  "activeWorkers": 1,
  "unhealthyWorkers": 0
}
```

### Get Worker

```http
GET /api/v1/workers/{workerId}
```

## Redis Registry

Worker states are stored in a Redis hash:

```text
orchestrator:workers:registry
```

This key can be changed with:

```text
ORCHESTRATOR_WORKER_REGISTRY_KEY
```

## Health Rules

The scheduler marks a worker `UNHEALTHY` if its last heartbeat is older than:

```text
ORCHESTRATOR_WORKER_HEARTBEAT_TIMEOUT
```

Default:

```text
15s
```

Workers send heartbeats every:

```text
WORKER_HEARTBEAT_INTERVAL_MS
```

Default:

```text
5000
```

Worker auto-registration can be disabled with:

```text
WORKER_REGISTRATION_ENABLED=false
```

This is mainly useful for isolated tests.

## Phase 4 Entry Criteria

Before Phase 4, this should work:

```bash
curl http://localhost:8080/api/v1/workers
```

You should see `worker-1`, `worker-2`, and `worker-3` as `ACTIVE`.

Phase 4 will use this registry to assign queued jobs to active workers.
