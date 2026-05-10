package com.vishnusinha.orchestrator.scheduler.metrics.api;

import com.vishnusinha.orchestrator.scheduler.job.JobRepository;
import com.vishnusinha.orchestrator.scheduler.job.JobStatus;
import com.vishnusinha.orchestrator.scheduler.policy.SchedulingPolicyResolver;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerRegistry;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerState;
import com.vishnusinha.orchestrator.shared.worker.WorkerStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsSummaryController {

    private final JobRepository jobRepository;
    private final WorkerRegistry workerRegistry;
    private final SchedulingPolicyResolver schedulingPolicyResolver;

    public MetricsSummaryController(
            JobRepository jobRepository,
            WorkerRegistry workerRegistry,
            SchedulingPolicyResolver schedulingPolicyResolver
    ) {
        this.jobRepository = jobRepository;
        this.workerRegistry = workerRegistry;
        this.schedulingPolicyResolver = schedulingPolicyResolver;
    }

    @GetMapping("/summary")
    public MetricsSummaryResponse summary() {
        List<WorkerState> workers = workerRegistry.list();
        int activeWorkers = (int) workers.stream()
                .filter(worker -> worker.status() == WorkerStatus.ACTIVE)
                .count();
        int unhealthyWorkers = (int) workers.stream()
                .filter(worker -> worker.status() == WorkerStatus.UNHEALTHY)
                .count();
        double averageWorkerLoad = workers.stream()
                .mapToDouble(WorkerState::loadScore)
                .average()
                .orElse(0.0);

        return new MetricsSummaryResponse(
                schedulingPolicyResolver.activePolicy().type(),
                jobRepository.countByStatus(JobStatus.QUEUED),
                jobRepository.countByStatus(JobStatus.SCHEDULED),
                jobRepository.countByStatus(JobStatus.RUNNING),
                jobRepository.countByStatus(JobStatus.COMPLETED),
                jobRepository.countByStatus(JobStatus.FAILED),
                workers.size(),
                activeWorkers,
                unhealthyWorkers,
                averageWorkerLoad
        );
    }
}
