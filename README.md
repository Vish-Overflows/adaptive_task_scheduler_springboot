# Adaptive Task Scheduler

A small distributed workload scheduler built with Spring Boot, PostgreSQL, Redis, Docker, and React.

The project started from a simple question: if a system receives many jobs, how should it decide which worker should run each one? This repository explores that question by building the moving parts around it: job intake, durable state, worker heartbeats, scheduling policies, retries after failure, metrics, benchmarks, and a browser dashboard to operate the system.

This is not a general-purpose job runner. Workers execute a fixed set of bounded demo workloads so the scheduler can be tested safely and repeatedly.

## What It Does

- Accepts jobs through a scheduler API.
- Stores job history and lifecycle timestamps in PostgreSQL.
- Uses Redis for the pending-job queue and live worker registry.
- Runs multiple Spring Boot worker services.
- Lets workers register themselves and send heartbeats.
- Dispatches queued jobs to healthy workers.
- Tracks each job from `QUEUED` to `SCHEDULED`, `RUNNING`, `COMPLETED`, or `FAILED`.
- Supports retries when workers become unhealthy or jobs get stuck.
- Exposes scheduling policies that can be changed at runtime.
- Provides Prometheus metrics and a JSON metrics summary API.
- Includes benchmark tooling to compare policy behavior.
- Includes a lightweight ML training component for policy-selection experiments.
- Includes a React dashboard for submitting jobs, choosing policies, viewing workers, and inspecting metrics.

## Why This Project Exists

Many student projects stop at CRUD APIs. This one is meant to exercise more realistic backend concerns:

- coordination between services
- durable vs ephemeral state
- failure handling
- scheduling tradeoffs
- observability
- repeatable benchmarking
- a UI that makes the system understandable to someone else

The useful part is not just that jobs run. The useful part is that the project records how they were scheduled, which worker handled them, how long they waited, how long they ran, and what happened when workers failed.

## Architecture

The system has two main roles:

- **Scheduler:** accepts jobs, stores state, chooses workers, tracks failures, exposes APIs and metrics.
- **Workers:** register with the scheduler, execute bounded workloads, and report completion.

PostgreSQL is used for durable job history. Redis is used for fast coordination: pending jobs and worker heartbeat state.

```text
Browser / API client
        |
        v
+--------------------+
| Scheduler Service  |
| Spring Boot        |
+----+----------+----+
     |          |
     |          +------------------+
     |                             |
     v                             v
+----------+                 +------------+
| Redis    |                 | PostgreSQL |
| queues   |                 | job history|
| workers  |                 | timestamps |
+----+-----+                 +------------+
     |
     | dispatches work to healthy workers
     v
+-----------+    +-----------+    +-----------+
| Worker 1  |    | Worker 2  |    | Worker 3  |
| Spring    |    | Spring    |    | Spring    |
+-----------+    +-----------+    +-----------+
```

The React frontend talks to the scheduler API. It does not talk directly to Redis, Postgres, or workers.

## Scheduling Policies

The scheduler currently supports:

| Policy | Behavior |
| --- | --- |
| `ROUND_ROBIN` | Rotates across available workers. |
| `LEAST_LOADED` | Picks the worker with the lowest current load. |
| `PRIORITY_AWARE` | Gives higher-priority jobs earlier treatment. |
| `SHORTEST_JOB_FIRST` | Favors jobs with lower estimated runtime. |
| `ADAPTIVE` | Chooses a scheduling style from workload features. |

The active policy can be changed through the API or the dashboard.

## Built-In Workloads

Workers do real bounded computation, not arbitrary user code.

| Type | Example payload |
| --- | --- |
| `CPU_BENCHMARK` | `{"upperBound": 50000}` |
| `MATRIX_MULTIPLY` | `{"matrixSize": 64}` |
| `HASH_COMPUTE` | `{"iterations": 100000}` |
| `SORT_BENCHMARK` | `{"itemCount": 100000}` |
| `MONTE_CARLO_PI` | `{"samples": 250000}` |
| `JSON_TRANSFORM` | `{"records": 50000}` |
| `GRAPH_TRAVERSAL` | `{"nodes": 2000, "edgesPerNode": 4}` |

The bounds keep the system demo-friendly while still giving the scheduler meaningful work to distribute.

## Repository Layout

```text
.
├── benchmarks/          # Python benchmark runner
├── docs/                # phase notes and deployment guide
├── frontend/            # React dashboard
├── ml/                  # policy-selection training experiment
├── scheduler-service/   # Spring Boot scheduler
├── shared/              # shared DTOs/contracts
├── worker-service/      # Spring Boot worker
├── docker-compose.yml
└── pom.xml
```

## Running Locally

The easiest way to run the full system is Docker Compose.

```bash
docker compose up --build
```

Then open:

```text
http://localhost:5173
```

Useful API checks:

```bash
curl http://localhost:8080/api/v1/info
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/workers
```

Submit a job:

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

List jobs:

```bash
curl http://localhost:8080/api/v1/jobs
```

Change the scheduler policy:

```bash
curl -X PUT http://localhost:8080/api/v1/policies/active \
  -H "Content-Type: application/json" \
  -d '{"policy": "LEAST_LOADED"}'
```

## Running Without Docker

Install:

- Java 21
- Maven 3.9+
- PostgreSQL
- Redis
- Node.js 20+

Run backend services:

```bash
mvn clean verify
mvn -pl scheduler-service spring-boot:run
mvn -pl worker-service spring-boot:run
```

Run the frontend:

```bash
cd frontend
npm install
npm run dev
```

Default frontend URL:

```text
http://localhost:5173
```

## Main API Surface

Scheduler:

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
GET /actuator/prometheus
```

Worker:

```http
GET /api/v1/info
POST /api/v1/jobs/execute
GET /actuator/health
GET /actuator/prometheus
```

## Metrics and Benchmarks

The scheduler exposes a metrics summary API and Prometheus-compatible metrics.

Run a benchmark against a running scheduler:

```bash
python3 benchmarks/benchmark_runner.py \
  --policy LEAST_LOADED \
  --workload duration_skew \
  --jobs 40
```

Train the policy-selection experiment:

```bash
python3 -m pip install -r ml/requirements.txt
python3 ml/train_policy_selector.py
```

The ML component is intentionally small. It is included to explore whether simple workload features can help pick a scheduling policy; it is not presented as a production-grade prediction system.

## Public Deployment

The practical public-demo setup is:

- Frontend on Vercel
- Scheduler and workers on a container host such as Railway, Render, or a VM
- Managed PostgreSQL and Redis

Detailed Railway/Vercel notes are in [docs/deployment.md](docs/deployment.md).

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
| `ORCHESTRATOR_SCHEDULING_POLICY` | `LEAST_LOADED` | Active scheduling policy |
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

## Project Notes

The project was built in phases:

1. Local infrastructure and service skeletons
2. Job submission and persistence
3. Worker registration and heartbeats
4. Scheduling and execution
5. Multiple scheduling policies
6. Fault tolerance and retries
7. Metrics and benchmarking
8. Adaptive policy-selection experiment
9. Frontend dashboard
