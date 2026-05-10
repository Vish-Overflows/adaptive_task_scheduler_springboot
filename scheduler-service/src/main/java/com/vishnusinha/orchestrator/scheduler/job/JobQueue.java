package com.vishnusinha.orchestrator.scheduler.job;

import java.util.UUID;
import java.util.Optional;

public interface JobQueue {

    void enqueue(UUID jobId);

    Optional<UUID> dequeue();

    void requeue(UUID jobId);
}
