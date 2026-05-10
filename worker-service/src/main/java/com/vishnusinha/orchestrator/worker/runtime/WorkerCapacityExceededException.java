package com.vishnusinha.orchestrator.worker.runtime;

public class WorkerCapacityExceededException extends RuntimeException {

    public WorkerCapacityExceededException(String message) {
        super(message);
    }
}
