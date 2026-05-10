package com.vishnusinha.orchestrator.scheduler.policy;

import com.vishnusinha.orchestrator.scheduler.job.JobEntity;
import com.vishnusinha.orchestrator.scheduler.job.JobRepository;
import com.vishnusinha.orchestrator.scheduler.job.JobStatus;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerRegistry;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerState;
import com.vishnusinha.orchestrator.shared.worker.WorkerStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkloadFeatureExtractor {

    private final JobRepository jobRepository;
    private final WorkerRegistry workerRegistry;

    public WorkloadFeatureExtractor(JobRepository jobRepository, WorkerRegistry workerRegistry) {
        this.jobRepository = jobRepository;
        this.workerRegistry = workerRegistry;
    }

    public WorkloadFeatures extract() {
        List<JobEntity> queuedJobs = jobRepository.findTop100ByStatusOrderByCreatedAtAsc(JobStatus.QUEUED);
        List<WorkerState> activeWorkers = workerRegistry.list().stream()
                .filter(worker -> worker.status() == WorkerStatus.ACTIVE)
                .toList();

        double[] durations = queuedJobs.stream()
                .mapToDouble(JobEntity::getEstimatedDurationMs)
                .toArray();
        double[] priorities = queuedJobs.stream()
                .mapToDouble(JobEntity::getPriority)
                .toArray();
        double[] loads = activeWorkers.stream()
                .mapToDouble(WorkerState::loadScore)
                .toArray();

        return new WorkloadFeatures(
                queuedJobs.size(),
                average(durations),
                standardDeviation(durations),
                average(priorities),
                standardDeviation(priorities),
                activeWorkers.size(),
                average(loads),
                standardDeviation(loads)
        );
    }

    private static double average(double[] values) {
        if (values.length == 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private static double standardDeviation(double[] values) {
        if (values.length <= 1) {
            return 0.0;
        }
        double average = average(values);
        double variance = 0.0;
        for (double value : values) {
            double delta = value - average;
            variance += delta * delta;
        }
        return Math.sqrt(variance / values.length);
    }
}
