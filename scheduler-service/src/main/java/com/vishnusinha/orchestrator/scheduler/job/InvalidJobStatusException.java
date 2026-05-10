package com.vishnusinha.orchestrator.scheduler.job;

public class InvalidJobStatusException extends RuntimeException {

    public InvalidJobStatusException(String status) {
        super("Unsupported job status: " + status);
    }
}
