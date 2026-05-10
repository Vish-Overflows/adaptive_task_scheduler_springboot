package com.vishnusinha.orchestrator.scheduler.job;

public class JobCompletionRejectedException extends RuntimeException {

    public JobCompletionRejectedException(String message) {
        super(message);
    }
}
