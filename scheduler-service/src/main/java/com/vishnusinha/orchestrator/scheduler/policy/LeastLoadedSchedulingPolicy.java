package com.vishnusinha.orchestrator.scheduler.policy;

import com.vishnusinha.orchestrator.scheduler.job.JobEntity;
import com.vishnusinha.orchestrator.scheduler.job.JobRepository;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class LeastLoadedSchedulingPolicy implements SchedulingPolicy {

    private final JobRepository jobRepository;

    public LeastLoadedSchedulingPolicy(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public SchedulingPolicyType type() {
        return SchedulingPolicyType.LEAST_LOADED;
    }

    @Override
    public Optional<JobEntity> selectJob() {
        return jobRepository.findNextQueuedFifo();
    }

    @Override
    public Optional<WorkerState> selectWorker(List<WorkerState> workers) {
        return WorkerSelectionSupport.leastLoaded(workers);
    }
}
