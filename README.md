# Fault-Tolerant Distributed Workload Orchestrator

An adaptive distributed workload scheduler built as a serious backend/systems project. The system will accept computational jobs, schedule them across containerized workers, recover from worker failures, and benchmark scheduling policies under controlled workloads.

This repository is currently at **Phase 9: frontend dashboard and operator control plane**.

## Current Capabilities

- Multi-module Java 21 / Spring Boot 3 codebase
- `scheduler-service` Spring Boot application
- `worker-service` Spring Boot application
- Shared DTO/contracts module
- PostgreSQL and Redis local infrastructure
- Docker Compose cluster with one scheduler and three workers
- Actuator health, readiness, metrics, and Prometheus endpoints
- Service identity APIs for scheduler and workers
- Validated job submission API
- PostgreSQL-backed job records
- Redis-backed pending job queue
- Optional idempotency keys for safe repeated submissions
- Redis-backed worker registry
- Worker auto-registration and heartbeat reporting
- Worker health API with `ACTIVE` / `UNHEALTHY` status
- Scheduler dispatch loop
- Worker-side job execution endpoint
- Seven built-in deterministic worker workloads
- Completion callbacks from workers to scheduler
- End-to-end job lifecycle from `QUEUED` to `COMPLETED`
- Configurable scheduling policies:
  - `ROUND_ROBIN`
  - `LEAST_LOADED`
  - `PRIORITY_AWARE`
  - `SHORTEST_JOB_FIRST`
  - `ADAPTIVE`
- Fault monitor for unhealthy workers and stuck in-flight jobs
- Retry budget with permanent failure after exhaustion
- Duplicate completion callback hardening
- Prometheus metrics and JSON metrics summary API
- Python benchmark runner for policy comparison
- Python ML training component using logistic regression and decision tree classifiers
- React frontend dashboard for job submission, policy control, worker visibility, and metrics exploration

## Target Architecture

```text
                         +---------------------+
                         |      Client/API      |
                         +----------+----------+
                                    |
                                    v
                         +---------------------+
                         |  Scheduler Service  |
                         |  Spring Boot / Java |
                         +----+-----------+----+
                              |           |
                  job queue   |           | historical state
                              v           v
                         +---------+   +-------------+
                         |  Redis  |   | PostgreSQL  |
                         +---------+   +-------------+
                              |
               assigns jobs / tracks workers
                              |
        +---------------------+---------------------+
        v                     v                     v
+---------------+     +---------------+     +---------------+
|   Worker 1    |     |   Worker 2    |     |   Worker 3    |
| Spring Boot   |     | Spring Boot   |     | Spring Boot   |
+---------------+     +---------------+     +---------------+
```

## Repository Layout

```text
.
├── docker-compose.yml
├── pom.xml
├── frontend/
├── shared/
├── scheduler-service/
├── worker-service/
└── docs/
```

## Services

### Scheduler Service

The scheduler is the control plane. It will own job submission, worker state, scheduling policies, benchmark execution, and metrics.

Current endpoint:

```http
GET /api/v1/info
POST /api/v1/jobs
GET /api/v1/jobs
GET /api/v1/jobs/{id}
POST /api/v1/jobs/{id}/completion
GET /api/v1/workers
GET /api/v1/workers/{workerId}
POST /api/v1/workers/register
POST /api/v1/workers/heartbeat
GET /api/v1/policies
PUT /api/v1/policies/active
GET /api/v1/metrics/summary
GET /actuator/health
GET /actuator/health/readiness
GET /actuator/prometheus
```

Default local port: `8080`.

### Worker Service

Workers are the execution plane. They will register with the scheduler, send heartbeats, execute assigned jobs, and report completion metadata.

Current endpoint:

```http
GET /api/v1/info
POST /api/v1/jobs/execute
GET /actuator/health
GET /actuator/health/readiness
GET /actuator/prometheus
```

Default exposed local worker port: `8081` for `worker-1`.

## Running Locally

### With Docker Compose

After Docker is installed:

```bash
docker compose up --build
```

Then open the dashboard:

```text
http://localhost:5173
```

Or check the APIs directly:

```bash
curl http://localhost:8080/api/v1/info
curl http://localhost:8081/api/v1/info
curl http://localhost:8080/actuator/health
```

Submit a queued job:

```bash
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "type": "MATRIX_MULTIPLY",
    "priority": 100,
    "payload": {"matrixSize": 64},
    "estimatedDurationMs": 2000,
    "idempotencyKey": "readme-demo-1"
}'
```

Then watch it execute:

```bash
curl http://localhost:8080/api/v1/jobs
```

The job should move from `QUEUED` to `RUNNING`, then to `COMPLETED`.

Change the active scheduler policy:

```bash
curl -X PUT http://localhost:8080/api/v1/policies/active \
  -H "Content-Type: application/json" \
  -d '{"policy": "LEAST_LOADED"}'
```

Supported workload types:

| Type | Example payload |
| --- | --- |
| `CPU_BENCHMARK` | `{"upperBound": 50000}` |
| `MATRIX_MULTIPLY` | `{"matrixSize": 64}` |
| `HASH_COMPUTE` | `{"iterations": 100000}` |
| `SORT_BENCHMARK` | `{"itemCount": 100000}` |
| `MONTE_CARLO_PI` | `{"samples": 250000}` |
| `JSON_TRANSFORM` | `{"records": 50000}` |
| `GRAPH_TRAVERSAL` | `{"nodes": 2000, "edgesPerNode": 4}` |

Workers execute these bounded built-in workloads. They do not run arbitrary user code.

### Without Docker

Install:

- Java 21
- Maven 3.9+
- PostgreSQL
- Redis

Then run:

```bash
mvn clean verify
mvn -pl scheduler-service spring-boot:run
mvn -pl worker-service spring-boot:run
```

Run the frontend in a separate terminal:

```bash
cd frontend
npm install
npm run dev
```

Default frontend URL: `http://localhost:5173`.

## Environment Variables

Scheduler:

| Variable | Default | Purpose |
| --- | --- | --- |
| `PORT` | unset | Cloud platform HTTP port override, used before `SERVER_PORT` |
| `SERVER_PORT` | `8080` | Scheduler HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/orchestrator` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `orchestrator` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `orchestrator` | Database password |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis host |
| `SPRING_DATA_REDIS_PORT` | `6379` | Redis port |
| `ORCHESTRATOR_ALLOWED_ORIGINS` | local frontend URLs | Comma-separated browser origins allowed to call the scheduler API |
| `ORCHESTRATOR_PENDING_JOBS_KEY` | `orchestrator:jobs:pending` | Redis pending job queue key |
| `ORCHESTRATOR_WORKER_REGISTRY_KEY` | `orchestrator:workers:registry` | Redis worker registry hash key |
| `ORCHESTRATOR_WORKER_HEARTBEAT_TIMEOUT` | `15s` | Age after which a worker is marked unhealthy |
| `ORCHESTRATOR_DISPATCH_ENABLED` | `true` | Enables scheduler job dispatch loop |
| `ORCHESTRATOR_DISPATCH_INTERVAL_MS` | `1000` | Delay between dispatch attempts in milliseconds |
| `ORCHESTRATOR_DISPATCH_MAX_JOBS_PER_TICK` | `1` | Maximum jobs dispatched per scheduler tick |
| `ORCHESTRATOR_SCHEDULING_POLICY` | `LEAST_LOADED` | Active scheduling policy: `ROUND_ROBIN`, `LEAST_LOADED`, `PRIORITY_AWARE`, `SHORTEST_JOB_FIRST`, or `ADAPTIVE` |
| `ORCHESTRATOR_FAULT_TOLERANCE_ENABLED` | `true` | Enables job reclaim and retry monitor |
| `ORCHESTRATOR_IN_FLIGHT_TIMEOUT` | `60s` | Time after which an in-flight job is considered stuck |
| `ORCHESTRATOR_FAULT_SCAN_INTERVAL_MS` | `5000` | Delay between fault-tolerance scans in milliseconds |
| `ORCHESTRATOR_MAX_RETRIES` | `3` | Maximum retry attempts before permanent failure |
| `ORCHESTRATOR_ENVIRONMENT` | `local` | Runtime environment label |

Worker:

| Variable | Default | Purpose |
| --- | --- | --- |
| `PORT` | unset | Cloud platform HTTP port override, used before `SERVER_PORT` |
| `SERVER_PORT` | `8081` | Worker HTTP port |
| `WORKER_ID` | `worker-local-1` | Stable worker identity |
| `SCHEDULER_BASE_URL` | `http://localhost:8080` | Scheduler base URL |
| `WORKER_PUBLIC_BASE_URL` | `http://localhost:8081` | Worker URL stored in scheduler registry |
| `WORKER_HEARTBEAT_INTERVAL_MS` | `5000` | Interval between worker heartbeats in milliseconds |
| `WORKER_MAX_CONCURRENT_JOBS` | `4` | Advertised worker execution capacity |
| `WORKER_REGISTRATION_ENABLED` | `true` | Enables scheduler registration and heartbeat loop |
| `ORCHESTRATOR_ENVIRONMENT` | `local` | Runtime environment label |

Frontend:

| Variable | Default | Purpose |
| --- | --- | --- |
| `VITE_API_BASE_URL` | same-origin `/api` | Public scheduler URL when the frontend is hosted separately |

## Public Deployment

The fastest public demo path is:

- Frontend on Vercel
- Scheduler and workers on Railway
- PostgreSQL and Redis as Railway managed services

Detailed deployment steps are in [docs/deployment.md](docs/deployment.md).

## Phase Roadmap

1. Project skeleton and local infrastructure
2. Job submission and persistence
3. Worker registration and heartbeats
4. Basic scheduling and job execution
5. Pluggable scheduling policies
6. Fault tolerance and retry semantics
7. Metrics and benchmarking
8. Adaptive / ML-assisted policy selection
9. Frontend dashboard and UI polish

## Benchmark and ML Tooling

Run a benchmark against a running scheduler:

```bash
python3 benchmarks/benchmark_runner.py \
  --policy LEAST_LOADED \
  --workload duration_skew \
  --jobs 40
```

Train the policy selector:

```bash
python3 -m pip install -r ml/requirements.txt
python3 ml/train_policy_selector.py
```

## Engineering Principles

- Keep scheduler and worker responsibilities separate.
- Store durable history in PostgreSQL; keep fast-changing queue/state in Redis.
- Make scheduling policies pluggable and benchmarkable.
- Prefer measurable claims over vague feature lists.
- Treat failure behavior as a first-class requirement, not a stretch polish item.
