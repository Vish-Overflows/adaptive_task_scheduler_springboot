package com.vishnusinha.orchestrator.scheduler.job;

public class InvalidJobPayloadException extends RuntimeException {

    public InvalidJobPayloadException(String message) {
        super(message);
    }
}
