#!/usr/bin/env python3
"""Train policy-selection models from benchmark summaries.

Expected input is the CSV produced by benchmarks/benchmark_runner.py after
running each workload under each policy. The trainer labels the best policy per
workload by lowest p95 end-to-end latency, then compares Logistic Regression
against a Decision Tree classifier.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

import joblib
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, f1_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.tree import DecisionTreeClassifier, export_text


FEATURE_COLUMNS = [
    "jobCount",
    "avgEstimatedDurationMs",
    "durationStdDevMs",
    "avgPriority",
    "priorityStdDev",
    "completedJobs",
    "failedJobs",
    "avgQueueWaitMs",
]


def load_training_frame(csv_path: Path) -> pd.DataFrame:
    raw = pd.read_csv(csv_path)
    best_rows = raw.sort_values(["workload", "p95EndToEndMs"]).groupby("workload", as_index=False).first()
    feature_rows = raw.groupby("workload", as_index=False)[FEATURE_COLUMNS].mean()
    return feature_rows.merge(best_rows[["workload", "policy"]], on="workload").rename(columns={"policy": "bestPolicy"})


def build_models() -> dict[str, Pipeline]:
    preprocessor = ColumnTransformer(
        transformers=[("numeric", StandardScaler(), FEATURE_COLUMNS)],
        remainder="drop",
    )
    return {
        "logistic_regression": Pipeline(
            [
                ("preprocessor", preprocessor),
                ("classifier", LogisticRegression(max_iter=1000, class_weight="balanced")),
            ]
        ),
        "decision_tree": Pipeline(
            [
                ("preprocessor", preprocessor),
                ("classifier", DecisionTreeClassifier(max_depth=4, min_samples_leaf=1, random_state=42)),
            ]
        ),
    }


def evaluate_models(frame: pd.DataFrame) -> tuple[str, dict[str, dict[str, float]], dict[str, Pipeline]]:
    x = frame[FEATURE_COLUMNS]
    y = frame["bestPolicy"]

    if len(frame) < 6 or y.nunique() < 2:
        raise SystemExit("Need at least 6 benchmark rows and 2 distinct best policies to train meaningfully.")

    x_train, x_test, y_train, y_test = train_test_split(
        x,
        y,
        test_size=0.3,
        random_state=42,
        stratify=y if y.value_counts().min() >= 2 else None,
    )

    models = build_models()
    scores: dict[str, dict[str, float]] = {}
    for name, model in models.items():
        model.fit(x_train, y_train)
        predictions = model.predict(x_test)
        scores[name] = {
            "accuracy": accuracy_score(y_test, predictions),
            "macroF1": f1_score(y_test, predictions, average="macro"),
        }

    best_model_name = max(scores, key=lambda model_name: (scores[model_name]["macroF1"], scores[model_name]["accuracy"]))
    return best_model_name, scores, models


def export_artifacts(best_model_name: str, scores: dict[str, dict[str, float]], models: dict[str, Pipeline], output_dir: Path) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    best_model = models[best_model_name]
    joblib.dump(best_model, output_dir / "policy-selector.joblib")
    (output_dir / "model-metrics.json").write_text(json.dumps({"bestModel": best_model_name, "scores": scores}, indent=2), encoding="utf-8")

    tree_model = models["decision_tree"]
    tree = tree_model.named_steps["classifier"]
    rules = export_text(tree, feature_names=FEATURE_COLUMNS)
    (output_dir / "decision-tree-rules.txt").write_text(rules, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", default="artifacts/benchmarks/benchmark-results.csv")
    parser.add_argument("--output-dir", default="artifacts/ml")
    args = parser.parse_args()

    frame = load_training_frame(Path(args.input))
    best_model_name, scores, models = evaluate_models(frame)
    export_artifacts(best_model_name, scores, models, Path(args.output_dir))
    print(json.dumps({"bestModel": best_model_name, "scores": scores}, indent=2))


if __name__ == "__main__":
    main()
