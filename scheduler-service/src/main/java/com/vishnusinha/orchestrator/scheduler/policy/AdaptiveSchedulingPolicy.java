package com.vishnusinha.orchestrator.scheduler.policy;

import com.vishnusinha.orchestrator.scheduler.job.JobEntity;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AdaptiveSchedulingPolicy implements SchedulingPolicy {

    private static final double HIGH_PRIORITY_STDDEV = 250.0;
    private static final double HIGH_DURATION_COEFFICIENT_OF_VARIATION = 0.75;
    private static final double HIGH_WORKER_LOAD_STDDEV = 0.25;

    private final WorkloadFeatureExtractor featureExtractor;
    private final RoundRobinSchedulingPolicy roundRobin;
    private final LeastLoadedSchedulingPolicy leastLoaded;
    private final PriorityAwareSchedulingPolicy priorityAware;
    private final ShortestJobFirstSchedulingPolicy shortestJobFirst;

    public AdaptiveSchedulingPolicy(
            WorkloadFeatureExtractor featureExtractor,
            RoundRobinSchedulingPolicy roundRobin,
            LeastLoadedSchedulingPolicy leastLoaded,
            PriorityAwareSchedulingPolicy priorityAware,
            ShortestJobFirstSchedulingPolicy shortestJobFirst
    ) {
        this.featureExtractor = featureExtractor;
        this.roundRobin = roundRobin;
        this.leastLoaded = leastLoaded;
        this.priorityAware = priorityAware;
        this.shortestJobFirst = shortestJobFirst;
    }

    @Override
    public SchedulingPolicyType type() {
        return SchedulingPolicyType.ADAPTIVE;
    }

    @Override
    public Optional<JobEntity> selectJob() {
        return chooseDelegate(featureExtractor.extract()).selectJob();
    }

    @Override
    public Optional<WorkerState> selectWorker(List<WorkerState> workers) {
        return chooseDelegate(featureExtractor.extract()).selectWorker(workers);
    }

    SchedulingPolicyType recommend(WorkloadFeatures features) {
        return chooseDelegate(features).type();
    }

    private SchedulingPolicy chooseDelegate(WorkloadFeatures features) {
        if (features.queueDepth() == 0) {
            return leastLoaded;
        }

        if (features.priorityStdDev() >= HIGH_PRIORITY_STDDEV) {
            return priorityAware;
        }

        double durationCoefficientOfVariation = features.averageEstimatedDurationMs() == 0.0
                ? 0.0
                : features.durationStdDevMs() / features.averageEstimatedDurationMs();
        if (durationCoefficientOfVariation >= HIGH_DURATION_COEFFICIENT_OF_VARIATION) {
            return shortestJobFirst;
        }

        if (features.workerLoadStdDev() >= HIGH_WORKER_LOAD_STDDEV) {
            return leastLoaded;
        }

        return roundRobin;
    }
}
