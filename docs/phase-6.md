# Phase 6: Fault Tolerance and Retry Semantics

## Objective

Prevent jobs from getting lost when workers fail, stall, or report failures.

Phase 6 adds a minimal production-style recovery loop: the scheduler periodically scans in-flight jobs, detects unsafe assignments, and either retries or permanently fails jobs based on a retry budget.

## Implemented

- Fault-tolerance configuration
- Scheduled job reclaim monitor
- Retry budget with permanent failure after exhaustion
- Reclaiming jobs assigned to unhealthy workers
- Reclaiming jobs that exceed an in-flight timeout
- Worker-reported failures now use retry semantics
- Duplicate terminal completion callbacks are idempotent
- Wrong-worker completion callbacks are rejected
- Focused unit tests for:
  - retry vs permanent failure
  - reclaiming unhealthy-worker jobs
  - reclaiming timed-out jobs
  - duplicate completion hardening

## Failure Handling Rules

The scheduler considers a job unsafe when:

- It is `SCHEDULED` or `RUNNING`, and
- its assigned worker is `UNHEALTHY`, or
- it has been in flight longer than `ORCHESTRATOR_IN_FLIGHT_TIMEOUT`.

When a job is unsafe:

```text
retryCount < maxRetries -> return job to QUEUED and increment retryCount
retryCount >= maxRetries -> mark job FAILED
```

Worker-reported failures use the same retry budget.

## Completion Hardening

Completion callbacks are accepted only from the assigned worker.

Duplicate callbacks for jobs already in a terminal state are treated as idempotent if they come from the assigned worker. This prevents harmless duplicate network retries from creating false errors.

Callbacks from the wrong worker are rejected.

Callbacks from an old worker after a job has been reclaimed/requeued are rejected because the worker is no longer assigned to that job.

## Configuration

Scheduler:

```text
ORCHESTRATOR_FAULT_TOLERANCE_ENABLED=true
ORCHESTRATOR_IN_FLIGHT_TIMEOUT=60s
ORCHESTRATOR_FAULT_SCAN_INTERVAL_MS=5000
ORCHESTRATOR_MAX_RETRIES=3
```

## Local Failure Test

Start the stack:

```bash
docker-compose up --build -d
```

Submit a long job:

```bash
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "type": "cpu_benchmark",
    "priority": 100,
    "payload": {"inputSize": 128},
    "estimatedDurationMs": 30000,
    "idempotencyKey": "phase6-reclaim-demo"
  }'
```

Stop one worker:

```bash
docker-compose stop worker-1
```

Then inspect jobs:

```bash
curl http://localhost:8080/api/v1/jobs
```

If the job had been assigned to `worker-1`, the scheduler will eventually return it to `QUEUED` and increment `retryCount`, or permanently fail it after the retry budget is exhausted.

## Phase 7 Entry Criteria

Before Phase 7, this should work:

- Worker heartbeats mark stale workers `UNHEALTHY`
- Jobs assigned to unhealthy workers are reclaimed
- Stuck in-flight jobs are reclaimed
- Failed jobs retry up to a cap
- Exhausted jobs end as `FAILED`
- Duplicate terminal completions do not corrupt state

Phase 7 will add metrics, controlled benchmark workloads, policy comparison, and a dashboard/report.
