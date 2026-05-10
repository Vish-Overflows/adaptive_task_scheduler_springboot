export type WorkloadTemplate = {
  type: string;
  name: string;
  description: string;
  payload: Record<string, number>;
  bounds: string;
};

export const workloads: WorkloadTemplate[] = [
  {
    type: "CPU_BENCHMARK",
    name: "CPU Benchmark",
    description: "Counts primes up to a bounded upper limit.",
    payload: { upperBound: 50000 },
    bounds: "upperBound: 1,000 - 200,000"
  },
  {
    type: "MATRIX_MULTIPLY",
    name: "Matrix Multiply",
    description: "Multiplies generated dense matrices.",
    payload: { matrixSize: 64 },
    bounds: "matrixSize: 8 - 128"
  },
  {
    type: "HASH_COMPUTE",
    name: "Hash Compute",
    description: "Performs repeated SHA-256 hashing.",
    payload: { iterations: 100000 },
    bounds: "iterations: 1,000 - 500,000"
  },
  {
    type: "SORT_BENCHMARK",
    name: "Sort Benchmark",
    description: "Sorts generated integer arrays.",
    payload: { itemCount: 100000, seed: 42 },
    bounds: "itemCount: 1,000 - 500,000"
  },
  {
    type: "MONTE_CARLO_PI",
    name: "Monte Carlo Pi",
    description: "Estimates pi using random sampling.",
    payload: { samples: 250000, seed: 7 },
    bounds: "samples: 1,000 - 1,000,000"
  },
  {
    type: "JSON_TRANSFORM",
    name: "JSON Transform",
    description: "Transforms synthetic JSON-like records.",
    payload: { records: 50000 },
    bounds: "records: 1,000 - 250,000"
  },
  {
    type: "GRAPH_TRAVERSAL",
    name: "Graph Traversal",
    description: "Builds and traverses a generated graph.",
    payload: { nodes: 2000, edgesPerNode: 4 },
    bounds: "nodes: 100 - 10,000; edgesPerNode: 1 - 16"
  }
];
