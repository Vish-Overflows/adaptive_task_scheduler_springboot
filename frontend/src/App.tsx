import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Activity,
  BarChart3,
  CheckCircle2,
  Clock3,
  Database,
  FlaskConical,
  GitBranch,
  Play,
  RotateCcw,
  Server,
  ShieldCheck,
  SlidersHorizontal,
  Workflow,
  XCircle
} from "lucide-react";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";
import { api, durationMs, formatMs, Job, MetricsSummary, SchedulingPolicy } from "./lib/api";
import { workloads } from "./lib/workloads";

type Snapshot = MetricsSummary & { label: string };

const policyLabels: Record<SchedulingPolicy, string> = {
  ROUND_ROBIN: "Round Robin",
  LEAST_LOADED: "Least Loaded",
  PRIORITY_AWARE: "Priority Aware",
  SHORTEST_JOB_FIRST: "Shortest Job First",
  ADAPTIVE: "Adaptive"
};

export default function App() {
  const queryClient = useQueryClient();
  const [selectedWorkloadType, setSelectedWorkloadType] = useState(workloads[1].type);
  const selectedWorkload = workloads.find((workload) => workload.type === selectedWorkloadType) ?? workloads[0];
  const [priority, setPriority] = useState(100);
  const [estimatedDurationMs, setEstimatedDurationMs] = useState(2000);
  const [payloadText, setPayloadText] = useState(JSON.stringify(selectedWorkload.payload, null, 2));
  const [metricMode, setMetricMode] = useState<"jobs" | "workers">("jobs");
  const [snapshots, setSnapshots] = useState<Snapshot[]>([]);
  const [lastSubmittedJob, setLastSubmittedJob] = useState<Job | null>(null);

  const metricsQuery = useQuery({ queryKey: ["metrics"], queryFn: api.metrics });
  const jobsQuery = useQuery({ queryKey: ["jobs"], queryFn: api.jobs });
  const workersQuery = useQuery({ queryKey: ["workers"], queryFn: api.workers });
  const policiesQuery = useQuery({ queryKey: ["policies"], queryFn: api.policies });

  useEffect(() => {
    setPayloadText(JSON.stringify(selectedWorkload.payload, null, 2));
  }, [selectedWorkload.type]);

  useEffect(() => {
    if (!metricsQuery.data) return;
    const now = new Date();
    setSnapshots((current) => [
      ...current.slice(-23),
      {
        ...metricsQuery.data,
        label: now.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" })
      }
    ]);
  }, [metricsQuery.data]);

  const submitJob = useMutation({
    mutationFn: async () => {
      const payload = JSON.parse(payloadText) as Record<string, number>;
      return api.submitJob({
        type: selectedWorkload.type,
        priority,
        payload,
        estimatedDurationMs,
        idempotencyKey: `${selectedWorkload.type.toLowerCase()}-${Date.now()}`
      });
    },
    onSuccess: (job) => {
      setLastSubmittedJob(job);
      queryClient.invalidateQueries({ queryKey: ["jobs"] });
      queryClient.invalidateQueries({ queryKey: ["metrics"] });
    }
  });

  const updatePolicy = useMutation({
    mutationFn: api.updatePolicy,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["policies"] });
      queryClient.invalidateQueries({ queryKey: ["metrics"] });
    }
  });

  const jobs = jobsQuery.data?.jobs ?? [];
  const workers = workersQuery.data?.workers ?? [];
  const metrics = metricsQuery.data;
  const terminalJobs = jobs.filter((job) => job.status === "COMPLETED" || job.status === "FAILED");
  const averageQueueWait = average(terminalJobs.map((job) => durationMs(job.queuedAt, job.startedAt)).filter(isNumber));
  const averageRuntime = average(terminalJobs.map((job) => durationMs(job.startedAt, job.completedAt)).filter(isNumber));
  const lastSubmittedLive = lastSubmittedJob
    ? jobs.find((job) => job.id === lastSubmittedJob.id) ?? lastSubmittedJob
    : null;

  const jobStatusData = useMemo(
    () => [
      { name: "Queued", value: metrics?.queuedJobs ?? 0 },
      { name: "Scheduled", value: metrics?.scheduledJobs ?? 0 },
      { name: "Running", value: metrics?.runningJobs ?? 0 },
      { name: "Completed", value: metrics?.completedJobs ?? 0 },
      { name: "Failed", value: metrics?.failedJobs ?? 0 }
    ],
    [metrics]
  );

  return (
    <main className="app-shell">
      <aside className="top-bar">
        <div className="brand-lockup">
          <div className="brand-mark">
            <Workflow size={22} />
          </div>
          <div>
            <strong>Workload Orchestrator</strong>
            <span>Scheduler control plane</span>
          </div>
        </div>
        <nav>
          <a href="#control">Control</a>
          <a href="#jobs">Jobs</a>
          <a href="#workers">Workers</a>
          <a href="#statistics">Stats</a>
        </nav>
      </aside>

      <section className="page">
        <header className="hero-panel" id="control">
          <div className="hero-copy">
            <p className="eyebrow">Distributed Workload Orchestrator</p>
            <h1>Submit jobs, choose a policy, and watch the cluster assign work.</h1>
            <p>
              This system accepts computational jobs, stores them durably, selects a scheduling policy,
              assigns work to Spring Boot workers, recovers from failures, and exposes metrics for policy comparison.
            </p>
            <div className="architecture-strip" aria-label="System architecture">
              <span>Client</span>
              <span>Scheduler</span>
              <span>Postgres</span>
              <span>Redis</span>
              <span>Workers</span>
              <span>Metrics</span>
            </div>
          </div>
          <div className="system-card">
            <div className="system-card-row">
              <span>Active policy</span>
              <strong>{policyLabels[metrics?.activePolicy ?? "LEAST_LOADED"]}</strong>
            </div>
            <div className="system-card-row">
              <span>Workers</span>
              <strong>{metrics?.activeWorkers ?? 0}/{metrics?.totalWorkers ?? 0} active</strong>
            </div>
            <div className="system-card-row">
              <span>Average load</span>
              <strong>{percent(metrics?.averageWorkerLoad ?? 0)}</strong>
            </div>
          </div>
        </header>

        <section className="grid two">
          <article className="panel">
            <div className="panel-heading">
              <div>
                <p className="eyebrow">Available Workloads</p>
                <h2>Pick a workload</h2>
              </div>
              <FlaskConical />
            </div>
            <div className="workload-grid">
              {workloads.map((workload) => (
                <button
                  key={workload.type}
                  className={workload.type === selectedWorkload.type ? "workload-tile active" : "workload-tile"}
                  onClick={() => setSelectedWorkloadType(workload.type)}
                >
                  <strong>{workload.name}</strong>
                  <span>{workload.description}</span>
                </button>
              ))}
            </div>
          </article>

          <article className="panel">
            <div className="panel-heading">
              <div>
                <p className="eyebrow">Submission Format</p>
                <h2>{selectedWorkload.name}</h2>
              </div>
              <Database />
            </div>
            <p className="muted">{selectedWorkload.bounds}</p>
            <div className="form-grid">
              <label>
                Priority
                <input type="range" min={0} max={1000} value={priority} onChange={(event) => setPriority(Number(event.target.value))} />
                <span>{priority}</span>
              </label>
              <label>
                Estimated duration
                <input
                  type="number"
                  min={100}
                  max={86400000}
                  value={estimatedDurationMs}
                  onChange={(event) => setEstimatedDurationMs(Number(event.target.value))}
                />
              </label>
            </div>
            <label className="payload-editor">
              Payload JSON
              <textarea value={payloadText} onChange={(event) => setPayloadText(event.target.value)} spellCheck={false} />
            </label>
            <button className="primary-action" onClick={() => submitJob.mutate()} disabled={submitJob.isPending}>
              <Play size={18} />
              {submitJob.isPending ? "Submitting" : "Submit job"}
            </button>
            {submitJob.error ? <p className="error-text">{submitJob.error.message}</p> : null}
          </article>
        </section>

        <section className="grid three">
          <MetricCard icon={<Clock3 />} label="Queued" value={metrics?.queuedJobs ?? 0} />
          <MetricCard icon={<Activity />} label="Running" value={(metrics?.runningJobs ?? 0) + (metrics?.scheduledJobs ?? 0)} />
          <MetricCard icon={<CheckCircle2 />} label="Completed" value={metrics?.completedJobs ?? 0} />
          <MetricCard icon={<XCircle />} label="Failed" value={metrics?.failedJobs ?? 0} />
          <MetricCard icon={<RotateCcw />} label="Avg queue wait" value={formatMs(averageQueueWait)} />
          <MetricCard icon={<ShieldCheck />} label="Avg runtime" value={formatMs(averageRuntime)} />
        </section>

        <section className="grid two">
          <article className="panel">
            <div className="panel-heading">
              <div>
                <p className="eyebrow">Policy Control</p>
                <h2>Set active scheduler policy</h2>
              </div>
              <SlidersHorizontal />
            </div>
            <div className="policy-list">
              {(policiesQuery.data?.supportedPolicies ?? []).map((policy) => (
                <button
                  key={policy}
                  className={policy === policiesQuery.data?.activePolicy ? "policy-button active" : "policy-button"}
                  onClick={() => updatePolicy.mutate(policy)}
                >
                  <strong>{policyLabels[policy]}</strong>
                  <span>{policyDescription(policy)}</span>
                </button>
              ))}
            </div>
          </article>

          <article className="panel">
            <div className="panel-heading">
              <div>
                <p className="eyebrow">Last Submitted Job</p>
                <h2>{lastSubmittedLive ? lastSubmittedLive.type : "No job submitted yet"}</h2>
              </div>
              <GitBranch />
            </div>
            {lastSubmittedLive ? (
              <div className="job-timeline">
                <StatusStep label="Submitted" active />
                <StatusStep label="Queued" active={["QUEUED", "SCHEDULED", "RUNNING", "COMPLETED", "FAILED"].includes(lastSubmittedLive.status)} />
                <StatusStep label={`Worker ${lastSubmittedLive.assignedWorkerId ?? "pending"}`} active={Boolean(lastSubmittedLive.assignedWorkerId)} />
                <StatusStep label={lastSubmittedLive.status} active />
                <dl className="detail-grid">
                  <div><dt>Policy</dt><dd>{lastSubmittedLive.scheduledPolicy ?? metrics?.activePolicy ?? "pending"}</dd></div>
                  <div><dt>Queue wait</dt><dd>{formatMs(durationMs(lastSubmittedLive.queuedAt, lastSubmittedLive.startedAt))}</dd></div>
                  <div><dt>Runtime</dt><dd>{formatMs(durationMs(lastSubmittedLive.startedAt, lastSubmittedLive.completedAt))}</dd></div>
                  <div><dt>Retries</dt><dd>{lastSubmittedLive.retryCount}</dd></div>
                </dl>
              </div>
            ) : (
              <p className="muted">Submit a workload and this panel will track which policy and worker picked it up.</p>
            )}
          </article>
        </section>

        <section className="panel" id="statistics">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">System Metrics</p>
              <h2>Inspect jobs and workers</h2>
            </div>
            <div className="segmented">
              <button className={metricMode === "jobs" ? "active" : ""} onClick={() => setMetricMode("jobs")}>Jobs</button>
              <button className={metricMode === "workers" ? "active" : ""} onClick={() => setMetricMode("workers")}>Workers</button>
            </div>
          </div>
          <div className="chart-grid">
            <div className="chart-box">
              <ResponsiveContainer width="100%" height={260}>
                {metricMode === "jobs" ? (
                  <BarChart data={jobStatusData}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="name" />
                    <YAxis allowDecimals={false} />
                    <Tooltip />
                    <Bar dataKey="value" fill="#1f5fbf" radius={[6, 6, 0, 0]} />
                  </BarChart>
                ) : (
                  <BarChart data={workers.map((worker) => ({ name: worker.workerId, load: Math.round(worker.loadScore * 100), active: worker.activeJobCount }))}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="name" />
                    <YAxis />
                    <Tooltip />
                    <Bar dataKey="load" fill="#0f766e" radius={[6, 6, 0, 0]} />
                  </BarChart>
                )}
              </ResponsiveContainer>
            </div>
            <div className="chart-box">
              <ResponsiveContainer width="100%" height={260}>
                <AreaChart data={snapshots}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="label" minTickGap={24} />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Area type="monotone" dataKey="runningJobs" stackId="1" stroke="#1f5fbf" fill="#d7e7fb" />
                  <Area type="monotone" dataKey="queuedJobs" stackId="1" stroke="#0f766e" fill="#ccfbf1" />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>
        </section>

        <section className="grid two">
          <article className="panel" id="jobs">
            <div className="panel-heading">
              <div>
                <p className="eyebrow">Jobs</p>
                <h2>Recent submissions</h2>
              </div>
              <BarChart3 />
            </div>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Type</th>
                    <th>Status</th>
                    <th>Worker</th>
                    <th>Policy</th>
                    <th>Retries</th>
                  </tr>
                </thead>
                <tbody>
                  {jobs.slice(0, 12).map((job) => (
                    <tr key={job.id}>
                      <td>{job.type}</td>
                      <td><Badge status={job.status} /></td>
                      <td>{job.assignedWorkerId ?? "pending"}</td>
                      <td>{job.scheduledPolicy ?? "pending"}</td>
                      <td>{job.retryCount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </article>

          <article className="panel" id="workers">
            <div className="panel-heading">
              <div>
                <p className="eyebrow">Workers</p>
                <h2>Live cluster</h2>
              </div>
              <Server />
            </div>
            <div className="worker-list">
              {workers.map((worker) => (
                <div className="worker-row" key={worker.workerId}>
                  <div>
                    <strong>{worker.workerId}</strong>
                    <span>{worker.status} · {worker.activeJobCount}/{worker.maxConcurrentJobs} jobs</span>
                  </div>
                  <div className="load-track">
                    <span style={{ width: `${Math.min(worker.loadScore * 100, 100)}%` }} />
                  </div>
                </div>
              ))}
            </div>
          </article>
        </section>
      </section>
    </main>
  );
}

function MetricCard({ icon, label, value }: { icon: React.ReactNode; label: string; value: string | number }) {
  return (
    <article className="metric-card">
      <div>{icon}</div>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function StatusStep({ label, active }: { label: string; active: boolean }) {
  return <span className={active ? "status-step active" : "status-step"}>{label}</span>;
}

function Badge({ status }: { status: Job["status"] }) {
  return <span className={`badge ${status.toLowerCase()}`}>{status}</span>;
}

function average(values: number[]): number | null {
  if (values.length === 0) return null;
  return values.reduce((sum, value) => sum + value, 0) / values.length;
}

function isNumber(value: number | null): value is number {
  return typeof value === "number" && Number.isFinite(value);
}

function percent(value: number) {
  return `${Math.round(value * 100)}%`;
}

function policyDescription(policy: SchedulingPolicy) {
  switch (policy) {
    case "ROUND_ROBIN":
      return "Cycles across available workers.";
    case "LEAST_LOADED":
      return "Picks the lowest-load worker.";
    case "PRIORITY_AWARE":
      return "Prioritizes important jobs first.";
    case "SHORTEST_JOB_FIRST":
      return "Runs shorter estimated jobs first.";
    case "ADAPTIVE":
      return "Chooses a policy from workload features.";
  }
}
