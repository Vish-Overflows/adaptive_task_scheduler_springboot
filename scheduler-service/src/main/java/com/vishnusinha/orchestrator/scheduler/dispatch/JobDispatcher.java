package com.vishnusinha.orchestrator.scheduler.dispatch;

import com.vishnusinha.orchestrator.scheduler.config.DispatchProperties;
import com.vishnusinha.orchestrator.scheduler.job.JobDispatchException;
import com.vishnusinha.orchestrator.scheduler.job.JobEntity;
import com.vishnusinha.orchestrator.scheduler.job.JobRepository;
import com.vishnusinha.orchestrator.scheduler.metrics.OrchestratorMetrics;
import com.vishnusinha.orchestrator.scheduler.policy.SchedulingPolicy;
import com.vishnusinha.orchestrator.scheduler.policy.SchedulingPolicyResolver;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerRegistry;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Component
public class JobDispatcher {

    private static final Logger log = LoggerFactory.getLogger(JobDispatcher.class);

    private final DispatchProperties properties;
    private final JobRepository jobRepository;
    private final WorkerRegistry workerRegistry;
    private final SchedulingPolicyResolver schedulingPolicyResolver;
    private final WorkerDispatchClient workerDispatchClient;
    private final OrchestratorMetrics orchestratorMetrics;
    private final Clock clock;

    public JobDispatcher(
            DispatchProperties properties,
            JobRepository jobRepository,
            WorkerRegistry workerRegistry,
            SchedulingPolicyResolver schedulingPolicyResolver,
            WorkerDispatchClient workerDispatchClient,
            OrchestratorMetrics orchestratorMetrics,
            Clock clock
    ) {
        this.properties = properties;
        this.jobRepository = jobRepository;
        this.workerRegistry = workerRegistry;
        this.schedulingPolicyResolver = schedulingPolicyResolver;
        this.workerDispatchClient = workerDispatchClient;
        this.orchestratorMetrics = orchestratorMetrics;
        this.clock = clock;
    }

    @Scheduled(
            initialDelayString = "${orchestrator.dispatch.interval-ms:1000}",
            fixedDelayString = "${orchestrator.dispatch.interval-ms:1000}"
    )
    public void dispatchTick() {
        if (!properties.enabled()) {
            return;
        }

        for (int index = 0; index < properties.maxJobsPerTick(); index++) {
            boolean dispatched = dispatchOne();
            if (!dispatched) {
                return;
            }
        }
    }

    @Transactional
    public boolean dispatchOne() {
        SchedulingPolicy policy = schedulingPolicyResolver.activePolicy();
        Optional<JobEntity> maybeJob = policy.selectJob();
        if (maybeJob.isEmpty()) {
            return false;
        }

        JobEntity job = maybeJob.get();
        Optional<WorkerState> maybeWorker = policy.selectWorker(workerRegistry.list());
        if (maybeWorker.isEmpty()) {
            return false;
        }

        WorkerState worker = maybeWorker.get();
        job.markScheduled(worker.workerId(), policy.type().name(), clock);
        jobRepository.saveAndFlush(job);

        try {
            workerDispatchClient.dispatch(job, worker);
            job.markRunning(clock);
            jobRepository.saveAndFlush(job);
            orchestratorMetrics.recordJobDispatched(job, policy.type());
            log.info("Dispatched job {} to worker {} using {} policy", job.getId(), worker.workerId(), policy.type());
            return true;
        } catch (JobDispatchException exception) {
            job.returnToQueue(clock);
            jobRepository.saveAndFlush(job);
            log.warn("Could not dispatch job {} to worker {}. Job returned to queue.", job.getId(), worker.workerId());
            return false;
        }
    }
}
