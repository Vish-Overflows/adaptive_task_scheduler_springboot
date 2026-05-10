# Deployment Guide: Vercel Frontend + Railway Backend

## Goal

Create a public dashboard URL while keeping the scheduler, workers, PostgreSQL, and Redis running as backend services.

Recommended first deployment:

- Vercel for the React frontend
- Railway for the Spring Boot scheduler, Spring Boot workers, PostgreSQL, and Redis

## What Changed for Deployment

- The frontend can call an external scheduler using `VITE_API_BASE_URL`.
- The scheduler has configurable CORS using `ORCHESTRATOR_ALLOWED_ORIGINS`.
- Scheduler and worker services accept cloud platform `PORT`.
- The frontend has a `vercel.json` SPA fallback.

## Railway Backend

Create a Railway project from the GitHub repository.

Add managed services:

1. PostgreSQL
2. Redis

Add a scheduler service:

- Source: this GitHub repository
- Builder: Dockerfile
- Dockerfile path: `scheduler-service/Dockerfile`

Set scheduler variables:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://<postgres-host>:<postgres-port>/<postgres-database>
SPRING_DATASOURCE_USERNAME=<postgres-user>
SPRING_DATASOURCE_PASSWORD=<postgres-password>
SPRING_DATA_REDIS_HOST=<redis-host>
SPRING_DATA_REDIS_PORT=<redis-port>
ORCHESTRATOR_ENVIRONMENT=railway
ORCHESTRATOR_ALLOWED_ORIGINS=https://<your-vercel-app>.vercel.app,http://localhost:5173,http://127.0.0.1:5173
```

After deploy, open the scheduler public URL and verify:

```text
https://<scheduler-service>.up.railway.app/actuator/health
https://<scheduler-service>.up.railway.app/api/v1/info
```

Add a worker service:

- Source: this GitHub repository
- Builder: Dockerfile
- Dockerfile path: `worker-service/Dockerfile`

Set worker variables:

```text
WORKER_ID=worker-1
SCHEDULER_BASE_URL=https://<scheduler-service>.up.railway.app
WORKER_PUBLIC_BASE_URL=https://<worker-service>.up.railway.app
ORCHESTRATOR_ENVIRONMENT=railway
WORKER_MAX_CONCURRENT_JOBS=4
```

For more workers, duplicate the worker service and change:

```text
WORKER_ID=worker-2
WORKER_ID=worker-3
```

Verify the scheduler sees workers:

```text
https://<scheduler-service>.up.railway.app/api/v1/workers
```

## Vercel Frontend

Create a Vercel project from the same GitHub repository.

Use these settings:

```text
Framework Preset: Vite
Root Directory: frontend
Build Command: npm run build
Output Directory: dist
Install Command: npm install
```

Set Vercel environment variable:

```text
VITE_API_BASE_URL=https://<scheduler-service>.up.railway.app
```

Deploy and open:

```text
https://<your-vercel-app>.vercel.app
```

## Final CORS Update

After Vercel gives the final frontend URL, return to Railway scheduler variables and set:

```text
ORCHESTRATOR_ALLOWED_ORIGINS=https://<your-vercel-app>.vercel.app,http://localhost:5173,http://127.0.0.1:5173
```

Redeploy the scheduler service.

## Smoke Test

From the public dashboard:

1. Confirm workers are visible.
2. Select `MATRIX_MULTIPLY`.
3. Submit the job.
4. Confirm the job moves from `QUEUED` to `RUNNING` to `COMPLETED`.
5. Confirm the latest job panel shows the selected policy and assigned worker.

Direct API test:

```bash
curl -X POST https://<scheduler-service>.up.railway.app/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "type": "MATRIX_MULTIPLY",
    "priority": 100,
    "payload": {"matrixSize": 64},
    "estimatedDurationMs": 2000,
    "idempotencyKey": "public-demo-1"
  }'
```

## Notes

- Vercel hosts only the browser frontend.
- Railway hosts the long-running backend services.
- Workers must be reachable by the scheduler using `WORKER_PUBLIC_BASE_URL`.
- The scheduler must be reachable by the frontend using `VITE_API_BASE_URL`.
