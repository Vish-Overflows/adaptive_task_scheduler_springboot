package com.vishnusinha.orchestrator.scheduler.policy;

import com.vishnusinha.orchestrator.scheduler.job.JobEntity;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerState;

import java.util.List;
import java.util.Optional;

public interface SchedulingPolicy {

    SchedulingPolicyType type();

    Optional<JobEntity> selectJob();

    Optional<WorkerState> selectWorker(List<WorkerState> workers);
}
