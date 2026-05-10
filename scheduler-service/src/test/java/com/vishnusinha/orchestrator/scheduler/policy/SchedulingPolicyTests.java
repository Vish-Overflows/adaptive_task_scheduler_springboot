package com.vishnusinha.orchestrator.scheduler.policy;

import com.vishnusinha.orchestrator.scheduler.job.JobRepository;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerState;
import com.vishnusinha.orchestrator.shared.worker.WorkerStatus;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SchedulingPolicyTests {

    private final JobRepository jobRepository = mock(JobRepository.class);

    @Test
    void leastLoadedUsesFifoJobSelectionAndLowestLoadWorker() {
        LeastLoadedSchedulingPolicy policy = new LeastLoadedSchedulingPolicy(jobRepository);

        policy.selectJob();
        assertThat(policy.selectWorker(List.of(
                worker("worker-1", WorkerStatus.ACTIVE, 3, 4),
                worker("worker-2", WorkerStatus.ACTIVE, 1, 4),
                worker("worker-3", WorkerStatus.UNHEALTHY, 0, 4)
        ))).hasValueSatisfying(worker -> assertThat(worker.workerId()).isEqualTo("worker-2"));

        verify(jobRepository).findNextQueuedFifo();
    }

    @Test
    void priorityAwareUsesPriorityJobSelectionAndLowestLoadWorker() {
        PriorityAwareSchedulingPolicy policy = new PriorityAwareSchedulingPolicy(jobRepository);

        policy.selectJob();
        assertThat(policy.selectWorker(List.of(
                worker("worker-1", WorkerStatus.ACTIVE, 2, 4),
                worker("worker-2", WorkerStatus.ACTIVE, 0, 4)
        ))).hasValueSatisfying(worker -> assertThat(worker.workerId()).isEqualTo("worker-2"));

        verify(jobRepository).findNextQueuedByPriority();
    }

    @Test
    void shortestJobFirstUsesDurationJobSelectionAndLowestLoadWorker() {
        ShortestJobFirstSchedulingPolicy policy = new ShortestJobFirstSchedulingPolicy(jobRepository);

        policy.selectJob();

        verify(jobRepository).findNextQueuedByShortestDuration();
    }

    @Test
    void roundRobinCyclesAcrossAvailableWorkers() {
        RoundRobinSchedulingPolicy policy = new RoundRobinSchedulingPolicy(jobRepository);
        List<WorkerState> workers = List.of(
                worker("worker-2", WorkerStatus.ACTIVE, 0, 4),
                worker("worker-1", WorkerStatus.ACTIVE, 0, 4)
        );

        assertThat(policy.selectWorker(workers)).hasValueSatisfying(worker -> assertThat(worker.workerId()).isEqualTo("worker-1"));
        assertThat(policy.selectWorker(workers)).hasValueSatisfying(worker -> assertThat(worker.workerId()).isEqualTo("worker-2"));
        assertThat(policy.selectWorker(workers)).hasValueSatisfying(worker -> assertThat(worker.workerId()).isEqualTo("worker-1"));
    }

    @Test
    void adaptiveRecommendsPriorityWhenPriorityVarianceIsHigh() {
        AdaptiveSchedulingPolicy policy = adaptivePolicy();

        SchedulingPolicyType recommendation = policy.recommend(new WorkloadFeatures(
                50,
                1000,
                100,
                500,
                300,
                3,
                0.2,
                0.1
        ));

        assertThat(recommendation).isEqualTo(SchedulingPolicyType.PRIORITY_AWARE);
    }

    @Test
    void adaptiveRecommendsShortestJobFirstWhenDurationVarianceIsHigh() {
        AdaptiveSchedulingPolicy policy = adaptivePolicy();

        SchedulingPolicyType recommendation = policy.recommend(new WorkloadFeatures(
                50,
                1000,
                900,
                100,
                20,
                3,
                0.2,
                0.1
        ));

        assertThat(recommendation).isEqualTo(SchedulingPolicyType.SHORTEST_JOB_FIRST);
    }

    private AdaptiveSchedulingPolicy adaptivePolicy() {
        return new AdaptiveSchedulingPolicy(
                mock(WorkloadFeatureExtractor.class),
                new RoundRobinSchedulingPolicy(jobRepository),
                new LeastLoadedSchedulingPolicy(jobRepository),
                new PriorityAwareSchedulingPolicy(jobRepository),
                new ShortestJobFirstSchedulingPolicy(jobRepository)
        );
    }

    private static WorkerState worker(String workerId, WorkerStatus status, int activeJobs, int maxJobs) {
        Instant now = Instant.parse("2026-05-05T10:00:00Z");
        return new WorkerState(
                workerId,
                "worker-service",
                "0.1.0",
                "test",
                URI.create("http://" + workerId + ":8081"),
                maxJobs,
                activeJobs,
                activeJobs / (double) maxJobs,
                status,
                now,
                now,
                now
        );
    }
}
