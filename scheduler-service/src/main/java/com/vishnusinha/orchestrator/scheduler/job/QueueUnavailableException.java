package com.vishnusinha.orchestrator.scheduler.job;

public class QueueUnavailableException extends RuntimeException {

    public QueueUnavailableException(String message) {
        super(message);
    }
}
