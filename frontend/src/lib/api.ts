export type JobStatus = "QUEUED" | "SCHEDULED" | "RUNNING" | "COMPLETED" | "FAILED";

export type Job = {
  id: string;
  type: string;
  priority: number;
  payload: Record<string, unknown>;
  estimatedDurationMs: number;
  status: JobStatus;
  assignedWorkerId: string | null;
  scheduledPolicy: string | null;
  retryCount: number;
  idempotencyKey: string | null;
  createdAt: string;
  queuedAt: string;
  startedAt: string | null;
  completedAt: string | null;
  updatedAt: string;
};

export type JobsResponse = {
  jobs: Job[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type Worker = {
  workerId: string;
  serviceName: string;
  version: string;
  environment: string;
  baseUrl: string;
  maxConcurrentJobs: number;
  activeJobCount: number;
  loadScore: number;
  status: "ACTIVE" | "UNHEALTHY";
  registeredAt: string;
  lastHeartbeatAt: string;
  updatedAt: string;
};

export type WorkersResponse = {
  workers: Worker[];
  totalWorkers: number;
  activeWorkers: number;
  unhealthyWorkers: number;
};

export type MetricsSummary = {
  activePolicy: SchedulingPolicy;
  queuedJobs: number;
  scheduledJobs: number;
  runningJobs: number;
  completedJobs: number;
  failedJobs: number;
  totalWorkers: number;
  activeWorkers: number;
  unhealthyWorkers: number;
  averageWorkerLoad: number;
};

export type SchedulingPolicy =
  | "ROUND_ROBIN"
  | "LEAST_LOADED"
  | "PRIORITY_AWARE"
  | "SHORTEST_JOB_FIRST"
  | "ADAPTIVE";

export type PoliciesResponse = {
  activePolicy: SchedulingPolicy;
  supportedPolicies: SchedulingPolicy[];
};

export type SubmitJobRequest = {
  type: string;
  priority: number;
  payload: Record<string, number>;
  estimatedDurationMs: number;
  idempotencyKey?: string;
};

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    headers: { "Content-Type": "application/json", ...(options?.headers ?? {}) },
    ...options
  });

  if (!response.ok) {
    const problem = await response.text();
    throw new Error(problem || `Request failed with status ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export const api = {
  metrics: () => request<MetricsSummary>("/api/v1/metrics/summary"),
  jobs: () => request<JobsResponse>("/api/v1/jobs?size=50"),
  workers: () => request<WorkersResponse>("/api/v1/workers"),
  policies: () => request<PoliciesResponse>("/api/v1/policies"),
  updatePolicy: (policy: SchedulingPolicy) =>
    request<PoliciesResponse>("/api/v1/policies/active", {
      method: "PUT",
      body: JSON.stringify({ policy })
    }),
  submitJob: (job: SubmitJobRequest) =>
    request<Job>("/api/v1/jobs", {
      method: "POST",
      body: JSON.stringify(job)
    })
};

export function durationMs(start: string | null, end: string | null): number | null {
  if (!start || !end) return null;
  return Math.max(0, new Date(end).getTime() - new Date(start).getTime());
}

export function formatMs(value: number | null): string {
  if (value === null) return "pending";
  if (value < 1000) return `${Math.round(value)} ms`;
  return `${(value / 1000).toFixed(2)} s`;
}
