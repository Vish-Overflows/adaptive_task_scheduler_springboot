# Benchmark Results

This folder is reserved for benchmark evidence: plots, summaries, and exported comparisons between scheduling policies.

The benchmark runner writes raw data to:

```text
artifacts/benchmarks/
```

Useful result artifacts to add here:

- throughput by policy
- average and p95 queue wait by policy
- end-to-end latency distributions
- worker utilization over time
- failure recovery timing
- policy comparison tables

Do not fabricate results. Add plots here only after running the benchmark harness against a live scheduler and worker cluster.
