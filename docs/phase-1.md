# Phase 1: Foundation and Local Infrastructure

## Objective

Create a clean foundation for the distributed workload orchestrator. This phase intentionally avoids job scheduling logic so the infrastructure, boundaries, and local development story stay solid.

## Implemented

- Parent Maven project with Java 21 and Spring Boot 3.3.5
- `shared` module for cross-service contracts
- `scheduler-service` application
- `worker-service` application
- Actuator health/readiness/metrics/prometheus endpoints
- Service identity endpoint for each service
- Docker Compose topology:
  - PostgreSQL
  - Redis
  - Scheduler
  - Worker 1
  - Worker 2
  - Worker 3
- Multi-stage Dockerfiles for scheduler and worker services

## Health Endpoints

Scheduler:

```text
GET http://localhost:8080/api/v1/info
GET http://localhost:8080/actuator/health
```

Worker 1:

```text
GET http://localhost:8081/api/v1/info
GET http://localhost:8081/actuator/health
```

## Phase 2 Entry Criteria

Before starting Phase 2, the following should work on a machine with Java/Maven/Docker installed:

```bash
docker compose up --build
curl http://localhost:8080/api/v1/info
curl http://localhost:8081/api/v1/info
```

## Phase 2 Scope Preview

Next phase adds:

- Job domain model
- Job submission API
- PostgreSQL persistence
- Redis queue insertion
- Job listing and status retrieval APIs
