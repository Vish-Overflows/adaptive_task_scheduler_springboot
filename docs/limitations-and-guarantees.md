# Limitations and Guarantees

This document is intentionally explicit about what the project does and does not guarantee. A scheduler project becomes more credible when its failure modes are named clearly.

## Current Scope

The system has one scheduler service, multiple workers, PostgreSQL for durable job state, and Redis for queue and worker coordination.

This is enough to explore:

- job intake
- worker registration and heartbeats
- scheduling policy selection
- retries and stuck-job recovery
- metrics and benchmark comparisons
- dashboard-based operation

It is not a fully replicated production scheduler.

## Scheduler Availability

The scheduler is currently a single authority.

If the scheduler process dies:

- workers continue to exist, but they stop receiving new assignments
- queued jobs remain in Redis and PostgreSQL
- persisted job records survive in PostgreSQL
- dispatch can continue after the scheduler restarts

What is not implemented:

- leader election
- active-active scheduler replicas
- Raft, ZooKeeper, etcd, or database-backed leasing
- automatic scheduler failover

For a production version, the next reasonable step would be a scheduler leadership lease, probably backed by PostgreSQL advisory locks or Redis `SET NX` with expiry, before considering heavier consensus systems.

## Execution Semantics

The current system should be described as **at-least-once dispatch with idempotent completion handling**.

It does not claim exactly-once execution.

### What is protected

- Job submission persists a record before queueing.
- Optional idempotency keys prevent duplicate client submissions from creating duplicate jobs.
- Every dispatch creates a fresh attempt token.
- Workers must include the attempt token when reporting completion.
- Stale completion callbacks from older attempts are rejected.
- Completion callbacks are guarded so a terminal job is not completed twice.
- Retry count limits prevent infinite retry loops.

### What can still happen

- A worker may finish a job but fail before the scheduler receives the callback.
- A stuck-job scan may eventually requeue that job.
- Another worker may execute the same job attempt.
- The scheduler may receive a late callback from the original worker.
- A late callback from an older attempt will be rejected, but the old work may still have consumed resources.

For the built-in workloads this is acceptable because they are deterministic, bounded computations without external side effects.

For real side-effecting jobs, each job handler would need its own idempotency strategy. Examples:

- write output under a deterministic object key
- use idempotency keys when calling external APIs
- use database uniqueness constraints for side effects
- make completion conditional on an attempt token

## Redis Atomicity

Redis is used for pending-job queueing and worker registry state.

Current implementation scope:

- simple queue push/pop behavior
- worker heartbeat state in Redis
- periodic reclaim of stale in-flight jobs

What is not yet fully hardened:

- atomic multi-key scheduling transactions
- Lua-scripted claim operations
- compare-and-set style worker assignment

The current implementation does use per-dispatch attempt tokens to reject stale completions. A stronger Redis design would also atomically move jobs from pending to in-flight and attach the attempt token as part of the same claim operation.

## Worker Failures

Current behavior:

- workers send periodic heartbeats
- stale workers are treated as unhealthy
- jobs assigned to unhealthy or stuck workers can be retried
- retry budget exhaustion marks jobs permanently failed

Known limitation:

- worker liveness is heartbeat-based, so detection is delayed by timeout settings
- the scheduler cannot know whether a timed-out worker actually stopped executing or merely stopped communicating

## Adaptive Scheduling

The `ADAPTIVE` policy is currently best described as heuristic-assisted policy switching.

It extracts workload features and chooses among existing policies. The Python ML component compares simple classifiers for policy recommendation, but the Java runtime does not yet perform online learning.

Accurate wording:

```text
Adaptive scheduling through workload feature extraction and policy switching.
```

Avoid overstating it as:

```text
AI-powered autonomous scheduler
```

Future directions:

- contextual bandits
- online policy selection
- latency prediction from historical jobs
- reinforcement learning in a simulator
- benchmark-driven policy retraining

## Testing Gaps

Current tests cover core services and policy behavior, but distributed systems need more than unit tests.

Important future tests:

- Docker Compose integration test that submits jobs and waits for completion
- concurrent submissions under multiple policies
- worker crash during execution
- duplicate completion callback simulation
- Redis queue atomicity tests
- benchmark regression checks

## Non-Goals

The current project does not attempt to implement:

- arbitrary user code execution
- Kubernetes scheduling
- multi-region operation
- exactly-once side effects
- replicated scheduler consensus
- production authentication and authorization
- multi-tenant resource isolation

These are valid production concerns, but they are outside the current project boundary.
