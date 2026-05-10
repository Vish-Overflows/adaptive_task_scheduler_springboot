# Phase 7: Metrics and Benchmarking

## Objective

Make scheduler behavior measurable.

Phase 7 adds observability and a benchmark harness so scheduling policies can be compared with data instead of claims.

## Implemented

- Micrometer metrics for:
  - submitted jobs
  - dispatched jobs
  - completed jobs
  - failed jobs
  - retried jobs
  - reclaimed jobs
  - queue wait time
  - execution time
  - end-to-end latency
- Prometheus-compatible metrics through:

```http
GET /actuator/prometheus
```

- JSON summary endpoint for dashboard and demos:

```http
GET /api/v1/metrics/summary
```

- Python benchmark runner:

```text
benchmarks/benchmark_runner.py
```

The runner submits controlled workloads, waits for terminal job states, and exports JSON/CSV results.

## Metrics Summary

```json
{
  "activePolicy": "LEAST_LOADED",
  "queuedJobs": 0,
  "scheduledJobs": 0,
  "runningJobs": 1,
  "completedJobs": 20,
  "failedJobs": 0,
  "totalWorkers": 3,
  "activeWorkers": 3,
  "unhealthyWorkers": 0,
  "averageWorkerLoad": 0.25
}
```

## Benchmark Runner

Run the Java stack with a selected policy:

```bash
ORCHESTRATOR_SCHEDULING_POLICY=LEAST_LOADED docker-compose up --build -d
```

Run a benchmark:

```bash
python3 benchmarks/benchmark_runner.py \
  --policy LEAST_LOADED \
  --workload duration_skew \
  --jobs 40
```

Supported workloads:

```text
uniform
priority_skew
duration_skew
```

Output:

```text
artifacts/benchmarks/benchmark-results.csv
artifacts/benchmarks/{policy}-{workload}.json
```

## Benchmark Fields

The benchmark CSV includes:

- policy
- workload
- job count
- average estimated duration
- duration standard deviation
- average priority
- priority standard deviation
- completed jobs
- failed jobs
- average queue wait
- p95 end-to-end latency

These fields become the training data for Phase 8.

## Why This Matters

This phase makes policy comparison concrete:

- `SHORTEST_JOB_FIRST` should reduce latency on duration-skewed workloads.
- `PRIORITY_AWARE` should favor priority-skewed workloads.
- `LEAST_LOADED` should improve load balance when worker utilization varies.
- `ROUND_ROBIN` remains the baseline.
