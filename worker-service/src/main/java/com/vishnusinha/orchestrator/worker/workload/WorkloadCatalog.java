package com.vishnusinha.orchestrator.worker.workload;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class WorkloadCatalog {

    private final Map<String, WorkloadExecutor> executors;

    public WorkloadCatalog(List<WorkloadExecutor> executors) {
        this.executors = executors.stream()
                .collect(Collectors.toUnmodifiableMap(
                        executor -> normalize(executor.type()),
                        Function.identity()
                ));
    }

    public WorkloadResult execute(String type, com.fasterxml.jackson.databind.JsonNode payload) {
        WorkloadExecutor executor = executors.get(normalize(type));
        if (executor == null) {
            throw new WorkloadExecutionException(
                    "Unsupported workload type '%s'. Supported types: %s".formatted(type, executors.keySet())
            );
        }
        return executor.execute(payload);
    }

    private static String normalize(String type) {
        return type.trim().toUpperCase(Locale.ROOT);
    }
}
