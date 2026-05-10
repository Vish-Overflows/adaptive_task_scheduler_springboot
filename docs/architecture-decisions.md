# Architecture Decisions

## ADR 001: Use Spring Boot Services for Scheduler and Workers

The project uses separate Spring Boot applications for the scheduler and worker roles. This keeps the control plane and execution plane independently deployable and makes the Docker Compose cluster realistic from the first phase.

## ADR 002: Use PostgreSQL for Durable History

PostgreSQL will store job records, lifecycle timestamps, scheduling decisions, benchmark runs, and metrics snapshots. Durable state belongs in Postgres because it must survive service restarts and support later analysis.

## ADR 003: Use Redis for Fast Queue and Ephemeral Cluster State

Redis will hold pending job queues, worker heartbeat state, and fast-changing scheduling metadata. This avoids overloading PostgreSQL with high-frequency coordination writes.

## ADR 004: Keep Scheduling Policies Pluggable

Scheduling algorithms will be implemented behind a strategy interface. This allows experiments to compare policies without rewriting the scheduling loop.

## ADR 005: Prefer Measured Benchmarks Over Feature Count

The resume value comes from defensible results: p95 latency, throughput, queue wait time, worker utilization, and retry behavior under failure. The architecture is designed to make those measurements easy to collect.
