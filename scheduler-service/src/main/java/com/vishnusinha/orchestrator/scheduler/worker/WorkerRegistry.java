package com.vishnusinha.orchestrator.scheduler.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vishnusinha.orchestrator.scheduler.config.WorkerRegistryProperties;
import com.vishnusinha.orchestrator.shared.worker.RegisterWorkerRequest;
import com.vishnusinha.orchestrator.shared.worker.WorkerHeartbeatRequest;
import com.vishnusinha.orchestrator.shared.worker.WorkerStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WorkerRegistry {

    private final StringRedisTemplate redisTemplate;
    private final WorkerRegistryProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public WorkerRegistry(
            StringRedisTemplate redisTemplate,
            WorkerRegistryProperties properties,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public WorkerState register(RegisterWorkerRequest request) {
        Instant now = Instant.now(clock);
        WorkerState state = new WorkerState(
                request.workerId(),
                request.serviceName(),
                request.version(),
                request.environment(),
                request.baseUrl(),
                request.maxConcurrentJobs(),
                0,
                0.0,
                WorkerStatus.ACTIVE,
                now,
                now,
                now
        );
        save(state);
        return state;
    }

    public WorkerState heartbeat(WorkerHeartbeatRequest request) {
        Instant now = Instant.now(clock);
        WorkerState current = findRaw(request.workerId())
                .orElseThrow(() -> new WorkerNotFoundException(request.workerId()));
        WorkerState updated = current.withHeartbeat(
                request.activeJobCount(),
                request.maxConcurrentJobs(),
                request.loadScore(),
                now
        );
        save(updated);
        return updated;
    }

    public WorkerState get(String workerId) {
        return findRaw(workerId)
                .map(this::withComputedStatus)
                .orElseThrow(() -> new WorkerNotFoundException(workerId));
    }

    public List<WorkerState> list() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(properties.registryKey());
        return entries.values().stream()
                .map(Object::toString)
                .map(this::deserialize)
                .map(this::withComputedStatus)
                .sorted(Comparator.comparing(WorkerState::workerId))
                .toList();
    }

    private Optional<WorkerState> findRaw(String workerId) {
        Object value = redisTemplate.opsForHash().get(properties.registryKey(), workerId);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(deserialize(value.toString()));
    }

    private WorkerState withComputedStatus(WorkerState worker) {
        WorkerStatus computedStatus = isHeartbeatStale(worker.lastHeartbeatAt())
                ? WorkerStatus.UNHEALTHY
                : WorkerStatus.ACTIVE;
        if (computedStatus == worker.status()) {
            return worker;
        }
        WorkerState updated = worker.withStatus(computedStatus, Instant.now(clock));
        save(updated);
        return updated;
    }

    private boolean isHeartbeatStale(Instant lastHeartbeatAt) {
        Duration age = Duration.between(lastHeartbeatAt, Instant.now(clock));
        return age.compareTo(properties.heartbeatTimeout()) > 0;
    }

    private void save(WorkerState worker) {
        redisTemplate.opsForHash().put(properties.registryKey(), worker.workerId(), serialize(worker));
    }

    private String serialize(WorkerState worker) {
        try {
            return objectMapper.writeValueAsString(worker);
        } catch (JsonProcessingException exception) {
            throw new WorkerStateSerializationException("Failed to serialize worker state", exception);
        }
    }

    private WorkerState deserialize(String json) {
        try {
            return objectMapper.readValue(json, WorkerState.class);
        } catch (JsonProcessingException exception) {
            throw new WorkerStateSerializationException("Failed to deserialize worker state", exception);
        }
    }
}
