package com.vishnusinha.orchestrator.scheduler.worker;

public class WorkerNotFoundException extends RuntimeException {

    public WorkerNotFoundException(String workerId) {
        super("Worker not found: " + workerId);
    }
}
