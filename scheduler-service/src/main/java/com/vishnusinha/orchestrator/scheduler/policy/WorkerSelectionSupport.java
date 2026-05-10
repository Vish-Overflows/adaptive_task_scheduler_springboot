package com.vishnusinha.orchestrator.scheduler.policy;

import com.vishnusinha.orchestrator.scheduler.worker.WorkerState;
import com.vishnusinha.orchestrator.shared.worker.WorkerStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

final class WorkerSelectionSupport {

    private WorkerSelectionSupport() {
    }

    static Optional<WorkerState> leastLoaded(List<WorkerState> workers) {
        return workers.stream()
                .filter(WorkerSelectionSupport::canAcceptJob)
                .min(Comparator
                        .comparingDouble(WorkerState::loadScore)
                        .thenComparingInt(WorkerState::activeJobCount)
                        .thenComparing(WorkerState::workerId));
    }

    static Optional<WorkerState> roundRobin(List<WorkerState> workers, AtomicInteger cursor) {
        List<WorkerState> candidates = workers.stream()
                .filter(WorkerSelectionSupport::canAcceptJob)
                .sorted(Comparator.comparing(WorkerState::workerId))
                .toList();

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        int selectedIndex = Math.floorMod(cursor.getAndIncrement(), candidates.size());
        return Optional.of(candidates.get(selectedIndex));
    }

    private static boolean canAcceptJob(WorkerState worker) {
        return worker.status() == WorkerStatus.ACTIVE
                && worker.activeJobCount() < worker.maxConcurrentJobs();
    }
}
