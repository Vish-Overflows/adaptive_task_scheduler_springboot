# Phase 4: Basic Scheduling and Job Execution

## Objective

Move jobs from the pending queue to live workers and close the loop when workers finish execution.

Phase 4 implements a simple but complete execution path. It does not yet compare scheduling policies or retry failed jobs.

## Implemented

- Scheduler-side dispatch loop
- Redis pending queue consumption
- Active-worker selection
- Worker capacity awareness
- Worker execution endpoint
- Built-in worker workload execution
- Worker completion callback to scheduler
- Job lifecycle transitions:
  - `QUEUED`
  - `SCHEDULED`
  - `RUNNING`
  - `COMPLETED`
  - `FAILED`
- Worker heartbeat load now reflects active jobs
- Unit tests for worker selection, dispatch, and completion handling

## Execution Flow

1. Client submits a job with `POST /api/v1/jobs`.
2. Scheduler stores the job in PostgreSQL as `QUEUED`.
3. Scheduler pushes the job ID into Redis.
4. Scheduler dispatch loop pops a job ID from Redis.
5. Scheduler selects an `ACTIVE` worker with open capacity.
6. Scheduler sends the job to the worker.
7. Worker accepts the job and returns `202 Accepted`.
8. Scheduler marks the job `RUNNING`.
9. Worker executes the requested built-in workload.
10. Worker calls scheduler completion endpoint.
11. Scheduler marks the job `COMPLETED` or `FAILED`.

## Supported Worker Job Types

Workers execute bounded, deterministic workloads. They do not execute arbitrary user code.

| Type | Payload fields | What it does |
| --- | --- | --- |
| `CPU_BENCHMARK` | `upperBound` | Counts primes up to a bounded integer |
| `MATRIX_MULTIPLY` | `matrixSize` | Multiplies generated dense matrices |
| `HASH_COMPUTE` | `iterations` | Performs repeated SHA-256 hashing |
| `SORT_BENCHMARK` | `itemCount`, `seed` | Sorts generated integer arrays |
| `MONTE_CARLO_PI` | `samples`, `seed` | Estimates pi with random samples |
| `JSON_TRANSFORM` | `records` | Transforms synthetic JSON-like records |
| `GRAPH_TRAVERSAL` | `nodes`, `edgesPerNode` | Builds and traverses a generated graph |

`estimatedDurationMs` remains scheduling metadata. The actual runtime depends on payload size and machine capacity.

## Scheduler Dispatch

The dispatch loop is controlled by:

```text
ORCHESTRATOR_DISPATCH_ENABLED
ORCHESTRATOR_DISPATCH_INTERVAL_MS
ORCHESTRATOR_DISPATCH_MAX_JOBS_PER_TICK
```

Defaults:

```text
ORCHESTRATOR_DISPATCH_ENABLED=true
ORCHESTRATOR_DISPATCH_INTERVAL_MS=1000
ORCHESTRATOR_DISPATCH_MAX_JOBS_PER_TICK=1
```

The default dispatches one job per tick to avoid over-assigning work before the next worker heartbeat updates load.

## Worker Execution API

Workers receive jobs through:

```http
POST /api/v1/jobs/execute
Content-Type: application/json
```

```json
{
  "jobId": "7e628ab8-88c2-4a1a-a35a-bb1a1b3d77f7",
  "type": "CPU_BENCHMARK",
  "payload": {
    "inputSize": 128
  },
  "estimatedDurationMs": 2000
}
```

Workers respond with:

```http
202 Accepted
```

If the worker is full, it returns `409 Conflict`.

## Scheduler Completion API

Workers report completion through:

```http
POST /api/v1/jobs/{jobId}/completion
Content-Type: application/json
```

```json
{
  "jobId": "7e628ab8-88c2-4a1a-a35a-bb1a1b3d77f7",
  "workerId": "worker-1",
  "status": "COMPLETED",
  "message": "Job completed",
  "runtimeMs": 2003
}
```

The scheduler rejects completion reports from workers that were not assigned to the job.

## Phase 4 Entry Test

Start the stack:

```bash
docker-compose up --build -d
```

Submit a job:

```bash
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "type": "cpu_benchmark",
    "priority": 100,
    "payload": {"inputSize": 128},
    "estimatedDurationMs": 2000,
    "idempotencyKey": "phase4-demo-1"
  }'
```

Watch it transition:

```bash
curl http://localhost:8080/api/v1/jobs
```

You should see the job move from `QUEUED` to `RUNNING`, then to `COMPLETED`.

## Phase 5 Entry Criteria

Before Phase 5, the system should reliably:

- Keep three workers registered as `ACTIVE`
- Dispatch queued jobs to workers
- Mark accepted jobs `RUNNING`
- Mark finished jobs `COMPLETED`
- Reflect active worker load through heartbeats

Phase 5 will make scheduling policies pluggable and benchmarkable.
