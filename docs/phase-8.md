# Phase 8: Adaptive and ML-Assisted Scheduling

## Objective

Make "adaptive scheduling" real and defensible.

Phase 8 adds an `ADAPTIVE` policy that extracts workload features and chooses one of the existing scheduling policies. It also adds a Python ML component that trains policy-selection models from benchmark data.

## Java Runtime Adaptive Policy

Enable adaptive scheduling:

```bash
ORCHESTRATOR_SCHEDULING_POLICY=ADAPTIVE
```

The Java scheduler extracts:

- queue depth
- average estimated duration
- duration standard deviation
- average priority
- priority standard deviation
- active worker count
- average worker load
- worker load standard deviation

It then chooses:

```text
high priority variance -> PRIORITY_AWARE
high duration variance -> SHORTEST_JOB_FIRST
high worker load imbalance -> LEAST_LOADED
otherwise -> ROUND_ROBIN
```

This is rule-based adaptive scheduling. It is deterministic, explainable, and safe even before any ML model is trained.

## Python ML Component

Location:

```text
ml/train_policy_selector.py
```

Dependencies:

```text
ml/requirements.txt
```

Install:

```bash
python3 -m pip install -r ml/requirements.txt
```

Train:

```bash
python3 ml/train_policy_selector.py \
  --input artifacts/benchmarks/benchmark-results.csv \
  --output-dir artifacts/ml
```

## Models Used

The trainer compares:

- `LogisticRegression`
- `DecisionTreeClassifier`

The decision tree is especially useful because it can be exported as readable rules. Logistic regression is included as a simple baseline so the ML work is comparative, not arbitrary.

Artifacts:

```text
artifacts/ml/policy-selector.joblib
artifacts/ml/model-metrics.json
artifacts/ml/decision-tree-rules.txt
```

## ML Framing

The ML model does not schedule individual jobs directly.

It predicts which existing scheduling policy is likely best for the current workload:

```text
ROUND_ROBIN
LEAST_LOADED
PRIORITY_AWARE
SHORTEST_JOB_FIRST
```

This keeps the system explainable and avoids fake "AI scheduler" claims.

## Resume-Accurate Claim

Use this phrasing:

```text
Implemented adaptive scheduling by extracting workload features and switching between benchmarked policies, with a Python scikit-learn pipeline comparing logistic regression and decision-tree classifiers for policy recommendation.
```
