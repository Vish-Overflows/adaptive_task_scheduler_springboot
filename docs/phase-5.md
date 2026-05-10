# Phase 5: Pluggable Scheduling Policies

## Objective

Make scheduling policy-driven and benchmark-ready.

Phase 5 replaces fixed FIFO dispatch with a configurable scheduling layer. The scheduler can now choose jobs and workers using different policies without changing the dispatch loop.

## Implemented

- `SchedulingPolicy` abstraction
- Runtime policy selection through configuration
- Built-in fallback policy: `LEAST_LOADED`
- Invalid policy values fail startup clearly
- PostgreSQL-backed job selection
- Worker selection helpers for active workers with available capacity
- Four policies:
  - `ROUND_ROBIN`
  - `LEAST_LOADED`
  - `PRIORITY_AWARE`
  - `SHORTEST_JOB_FIRST`
- Unit tests for policy behavior and dispatcher integration

## Policy Control

Set the active policy with:

```text
ORCHESTRATOR_SCHEDULING_POLICY
```

Allowed values:

```text
ROUND_ROBIN
LEAST_LOADED
PRIORITY_AWARE
SHORTEST_JOB_FIRST
```

Default:

```text
LEAST_LOADED
```

If the variable is missing, the scheduler uses `LEAST_LOADED`.

If the variable contains an unsupported value, startup fails. The scheduler should not silently run a different policy than the operator intended.

## Policies

### ROUND_ROBIN

Job choice:

- Oldest queued job first

Worker choice:

- Cycles through active workers with available capacity
- Ignores load except for capacity filtering

Useful as a baseline policy for benchmarks.

### LEAST_LOADED

Job choice:

- Oldest queued job first

Worker choice:

- Active worker with lowest `activeJobCount / maxConcurrentJobs`
- Ties break by active job count, then worker ID

This is the default because it uses heartbeat load data and behaves well for demos.

### PRIORITY_AWARE

Job choice:

- Highest priority first
- Ties break by oldest queued job

Worker choice:

- Least-loaded active worker

This is useful for workloads where important jobs should jump ahead.

### SHORTEST_JOB_FIRST

Job choice:

- Lowest `estimatedDurationMs` first
- Ties break by oldest queued job

Worker choice:

- Least-loaded active worker

This tends to reduce average completion latency, but it can starve longer jobs without aging. Phase 7 benchmarks should make that tradeoff visible.

## Design Note

Phase 4 used Redis FIFO queue order as the dispatch source.

Phase 5 uses PostgreSQL as the source of truth for selecting the next `QUEUED` job. Redis still records job intake, but policy-based scheduling needs durable queries over job metadata such as priority and estimated duration.

## Local Test

Start with a selected policy:

```bash
ORCHESTRATOR_SCHEDULING_POLICY=PRIORITY_AWARE docker-compose up --build -d
```

Submit jobs with different priorities or estimated durations:

```bash
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "type": "cpu_benchmark",
    "priority": 900,
    "payload": {"inputSize": 128},
    "estimatedDurationMs": 3000,
    "idempotencyKey": "phase5-high-priority"
  }'
```

Then inspect jobs:

```bash
curl http://localhost:8080/api/v1/jobs
```

## Phase 6 Entry Criteria

Before Phase 6, this should work:

- Scheduler starts with each supported policy
- Missing policy uses `LEAST_LOADED`
- Invalid policy fails startup
- Priority jobs are selected before lower-priority jobs under `PRIORITY_AWARE`
- Short jobs are selected first under `SHORTEST_JOB_FIRST`
- Round robin cycles across active workers

Phase 6 will add failure detection, job reclaiming, retries, and duplicate-completion hardening.
