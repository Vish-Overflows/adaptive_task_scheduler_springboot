package com.vishnusinha.orchestrator.scheduler.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vishnusinha.orchestrator.scheduler.config.WorkerRegistryProperties;
import com.vishnusinha.orchestrator.shared.worker.RegisterWorkerRequest;
import com.vishnusinha.orchestrator.shared.worker.WorkerHeartbeatRequest;
import com.vishnusinha.orchestrator.shared.worker.WorkerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkerRegistryTests {

    private static final String REGISTRY_KEY = "test:workers";
    private static final Instant NOW = Instant.parse("2026-05-05T10:00:00Z");

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private WorkerRegistry workerRegistry;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        workerRegistry = new WorkerRegistry(
                redisTemplate,
                new WorkerRegistryProperties(REGISTRY_KEY, Duration.ofSeconds(15)),
                objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void registerStoresActiveWorkerState() {
        RegisterWorkerRequest request = new RegisterWorkerRequest(
                "worker-1",
                "worker-service",
                "0.1.0",
                "test",
                URI.create("http://worker-1:8081"),
                4
        );

        WorkerState state = workerRegistry.register(request);

        assertThat(state.workerId()).isEqualTo("worker-1");
        assertThat(state.status()).isEqualTo(WorkerStatus.ACTIVE);
        assertThat(state.registeredAt()).isEqualTo(NOW);
        verify(hashOperations).put(eq(REGISTRY_KEY), eq("worker-1"), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void heartbeatUpdatesExistingWorkerLoadAndTimestamp() {
        WorkerState existing = activeWorker("worker-1", NOW.minusSeconds(5));
        when(hashOperations.get(REGISTRY_KEY, "worker-1")).thenReturn(toJson(existing));

        WorkerState updated = workerRegistry.heartbeat(new WorkerHeartbeatRequest("worker-1", 2, 4, 0.5));

        assertThat(updated.activeJobCount()).isEqualTo(2);
        assertThat(updated.maxConcurrentJobs()).isEqualTo(4);
        assertThat(updated.loadScore()).isEqualTo(0.5);
        assertThat(updated.lastHeartbeatAt()).isEqualTo(NOW);
        assertThat(updated.status()).isEqualTo(WorkerStatus.ACTIVE);
    }

    @Test
    void heartbeatRejectsUnknownWorker() {
        when(hashOperations.get(REGISTRY_KEY, "worker-missing")).thenReturn(null);

        assertThatThrownBy(() -> workerRegistry.heartbeat(new WorkerHeartbeatRequest("worker-missing", 0, 4, 0.0)))
                .isInstanceOf(WorkerNotFoundException.class);
    }

    @Test
    void listMarksStaleWorkersUnhealthy() {
        Map<Object, Object> entries = new LinkedHashMap<>();
        entries.put("worker-1", toJson(activeWorker("worker-1", NOW.minusSeconds(5))));
        entries.put("worker-2", toJson(activeWorker("worker-2", NOW.minusSeconds(60))));
        when(hashOperations.entries(REGISTRY_KEY)).thenReturn(entries);

        var workers = workerRegistry.list();

        assertThat(workers).extracting(WorkerState::workerId).containsExactly("worker-1", "worker-2");
        assertThat(workers.get(0).status()).isEqualTo(WorkerStatus.ACTIVE);
        assertThat(workers.get(1).status()).isEqualTo(WorkerStatus.UNHEALTHY);
    }

    private WorkerState activeWorker(String workerId, Instant lastHeartbeatAt) {
        return new WorkerState(
                workerId,
                "worker-service",
                "0.1.0",
                "test",
                URI.create("http://" + workerId + ":8081"),
                4,
                0,
                0.0,
                WorkerStatus.ACTIVE,
                NOW.minusSeconds(120),
                lastHeartbeatAt,
                lastHeartbeatAt
        );
    }

    private String toJson(WorkerState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
