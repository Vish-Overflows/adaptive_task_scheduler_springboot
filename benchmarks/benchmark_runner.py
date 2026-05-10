#!/usr/bin/env python3
"""Run controlled scheduler benchmarks and export results.

This runner assumes the Java stack is already running with the desired
ORCHESTRATOR_SCHEDULING_POLICY. It submits synthetic jobs, waits for terminal
states, and writes a compact JSON + CSV summary suitable for ML training.
"""

from __future__ import annotations

import argparse
import csv
import json
import random
import statistics
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any


TERMINAL_STATUSES = {"COMPLETED", "FAILED"}


@dataclass(frozen=True)
class WorkloadConfig:
    name: str
    jobs: int
    min_duration_ms: int
    max_duration_ms: int
    high_priority_ratio: float


def post_json(base_url: str, path: str, body: dict[str, Any]) -> dict[str, Any]:
    request = urllib.request.Request(
        base_url + path,
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=10) as response:
        return json.loads(response.read().decode("utf-8"))


def get_json(base_url: str, path: str) -> dict[str, Any]:
    with urllib.request.urlopen(base_url + path, timeout=10) as response:
        return json.loads(response.read().decode("utf-8"))


def generate_jobs(config: WorkloadConfig, seed: int) -> list[dict[str, Any]]:
    random.seed(seed)
    workload_types = [
        "CPU_BENCHMARK",
        "MATRIX_MULTIPLY",
        "HASH_COMPUTE",
        "SORT_BENCHMARK",
        "MONTE_CARLO_PI",
        "JSON_TRANSFORM",
        "GRAPH_TRAVERSAL",
    ]
    jobs: list[dict[str, Any]] = []
    for index in range(config.jobs):
        high_priority = random.random() < config.high_priority_ratio
        priority = random.randint(700, 1000) if high_priority else random.randint(0, 300)
        duration = random.randint(config.min_duration_ms, config.max_duration_ms)
        workload_type = workload_types[index % len(workload_types)]
        jobs.append(
            {
                "type": workload_type,
                "priority": priority,
                "payload": payload_for(workload_type, random),
                "estimatedDurationMs": duration,
                "idempotencyKey": f"benchmark-{config.name}-{seed}-{index}",
            }
        )
    return jobs


def payload_for(workload_type: str, random_source: random.Random) -> dict[str, int]:
    if workload_type == "CPU_BENCHMARK":
        return {"upperBound": random_source.randint(10_000, 60_000)}
    if workload_type == "MATRIX_MULTIPLY":
        return {"matrixSize": random_source.randint(24, 72)}
    if workload_type == "HASH_COMPUTE":
        return {"iterations": random_source.randint(20_000, 120_000)}
    if workload_type == "SORT_BENCHMARK":
        return {"itemCount": random_source.randint(20_000, 150_000)}
    if workload_type == "MONTE_CARLO_PI":
        return {"samples": random_source.randint(50_000, 250_000)}
    if workload_type == "JSON_TRANSFORM":
        return {"records": random_source.randint(20_000, 120_000)}
    if workload_type == "GRAPH_TRAVERSAL":
        return {"nodes": random_source.randint(1_000, 5_000), "edgesPerNode": random_source.randint(2, 8)}
    raise ValueError(f"Unsupported workload type: {workload_type}")


def submit_jobs(base_url: str, jobs: list[dict[str, Any]]) -> list[str]:
    job_ids: list[str] = []
    for job in jobs:
        response = post_json(base_url, "/api/v1/jobs", job)
        job_ids.append(response["id"])
    return job_ids


def wait_for_terminal_jobs(base_url: str, job_ids: list[str], timeout_seconds: int) -> list[dict[str, Any]]:
    deadline = time.monotonic() + timeout_seconds
    remaining = set(job_ids)
    terminal_jobs: dict[str, dict[str, Any]] = {}

    while remaining and time.monotonic() < deadline:
        for job_id in list(remaining):
            job = get_json(base_url, f"/api/v1/jobs/{job_id}")
            if job["status"] in TERMINAL_STATUSES:
                terminal_jobs[job_id] = job
                remaining.remove(job_id)
        time.sleep(0.5)

    for job_id in list(remaining):
        terminal_jobs[job_id] = get_json(base_url, f"/api/v1/jobs/{job_id}")

    return list(terminal_jobs.values())


def milliseconds_between(start: str | None, end: str | None) -> float | None:
    if not start or not end:
        return None
    from datetime import datetime

    normalized_start = normalize_iso_timestamp(start)
    normalized_end = normalize_iso_timestamp(end)
    return (datetime.fromisoformat(normalized_end) - datetime.fromisoformat(normalized_start)).total_seconds() * 1000


def normalize_iso_timestamp(value: str) -> str:
    normalized = value.replace("Z", "+00:00")
    if "." not in normalized:
        return normalized
    prefix, suffix = normalized.split(".", 1)
    if "+" in suffix:
        fractional, timezone = suffix.split("+", 1)
        return f"{prefix}.{fractional[:6]}+{timezone}"
    return normalized


def summarize(policy: str, config: WorkloadConfig, submitted_jobs: list[dict[str, Any]], completed_jobs: list[dict[str, Any]]) -> dict[str, Any]:
    durations = [job["estimatedDurationMs"] for job in submitted_jobs]
    priorities = [job["priority"] for job in submitted_jobs]
    terminal_statuses = [job["status"] for job in completed_jobs]
    queue_waits = [
        value for value in (milliseconds_between(job.get("queuedAt"), job.get("startedAt")) for job in completed_jobs) if value is not None
    ]
    end_to_end = [
        value for value in (milliseconds_between(job.get("createdAt"), job.get("completedAt")) for job in completed_jobs) if value is not None
    ]

    return {
        "policy": policy,
        "workload": config.name,
        "jobCount": config.jobs,
        "avgEstimatedDurationMs": statistics.fmean(durations),
        "durationStdDevMs": statistics.pstdev(durations) if len(durations) > 1 else 0.0,
        "avgPriority": statistics.fmean(priorities),
        "priorityStdDev": statistics.pstdev(priorities) if len(priorities) > 1 else 0.0,
        "completedJobs": terminal_statuses.count("COMPLETED"),
        "failedJobs": terminal_statuses.count("FAILED"),
        "avgQueueWaitMs": statistics.fmean(queue_waits) if queue_waits else 0.0,
        "p95EndToEndMs": percentile(end_to_end, 95),
    }


def percentile(values: list[float], percentile_value: int) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    index = int(round((percentile_value / 100) * (len(ordered) - 1)))
    return ordered[index]


def write_outputs(summary: dict[str, Any], output_dir: Path) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    json_path = output_dir / f"{summary['policy']}-{summary['workload']}.json"
    csv_path = output_dir / "benchmark-results.csv"

    json_path.write_text(json.dumps(summary, indent=2), encoding="utf-8")

    write_header = not csv_path.exists()
    with csv_path.open("a", newline="", encoding="utf-8") as file:
        writer = csv.DictWriter(file, fieldnames=list(summary.keys()))
        if write_header:
            writer.writeheader()
        writer.writerow(summary)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--policy", required=True)
    parser.add_argument("--workload", choices=["uniform", "priority_skew", "duration_skew"], default="uniform")
    parser.add_argument("--jobs", type=int, default=40)
    parser.add_argument("--seed", type=int, default=7)
    parser.add_argument("--timeout-seconds", type=int, default=180)
    parser.add_argument("--output-dir", default="artifacts/benchmarks")
    args = parser.parse_args()

    workloads = {
        "uniform": WorkloadConfig("uniform", args.jobs, 500, 1500, 0.5),
        "priority_skew": WorkloadConfig("priority_skew", args.jobs, 500, 2500, 0.8),
        "duration_skew": WorkloadConfig("duration_skew", args.jobs, 100, 8000, 0.5),
    }
    config = workloads[args.workload]

    jobs = generate_jobs(config, args.seed)
    job_ids = submit_jobs(args.base_url, jobs)
    terminal_jobs = wait_for_terminal_jobs(args.base_url, job_ids, args.timeout_seconds)
    summary = summarize(args.policy, config, jobs, terminal_jobs)
    write_outputs(summary, Path(args.output_dir))
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    try:
        main()
    except urllib.error.URLError as exception:
        raise SystemExit(f"Benchmark failed to reach scheduler: {exception}") from exception
