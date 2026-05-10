package com.vishnusinha.orchestrator.scheduler.policy;

import com.vishnusinha.orchestrator.scheduler.job.JobEntity;
import com.vishnusinha.orchestrator.scheduler.job.JobRepository;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoundRobinSchedulingPolicy implements SchedulingPolicy {

    private final JobRepository jobRepository;
    private final AtomicInteger cursor = new AtomicInteger(0);

    public RoundRobinSchedulingPolicy(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public SchedulingPolicyType type() {
        return SchedulingPolicyType.ROUND_ROBIN;
    }

    @Override
    public Optional<JobEntity> selectJob() {
        return jobRepository.findNextQueuedFifo();
    }

    @Override
    public Optional<WorkerState> selectWorker(List<WorkerState> workers) {
        return WorkerSelectionSupport.roundRobin(workers, cursor);
    }
}
