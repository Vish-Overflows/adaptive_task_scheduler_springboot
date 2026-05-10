package com.vishnusinha.orchestrator.scheduler.job;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class JobEntity {

    @Id
    private UUID id;

    @Column(name = "job_type", nullable = false, length = 80)
    private String type;

    @Column(nullable = false)
    private int priority;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Column(name = "estimated_duration_ms", nullable = false)
    private long estimatedDurationMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobStatus status;

    @Column(name = "assigned_worker_id", length = 120)
    private String assignedWorkerId;

    @Column(name = "scheduled_policy", length = 40)
    private String scheduledPolicy;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "queued_at", nullable = false)
    private Instant queuedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected JobEntity() {
    }

    private JobEntity(
            UUID id,
            String type,
            int priority,
            JsonNode payload,
            long estimatedDurationMs,
            String idempotencyKey,
            Instant now
    ) {
        this.id = id;
        this.type = type;
        this.priority = priority;
        this.payload = payload;
        this.estimatedDurationMs = estimatedDurationMs;
        this.idempotencyKey = idempotencyKey;
        this.status = JobStatus.QUEUED;
        this.retryCount = 0;
        this.createdAt = now;
        this.queuedAt = now;
        this.updatedAt = now;
    }

    public static JobEntity queued(
            String type,
            int priority,
            JsonNode payload,
            long estimatedDurationMs,
            String idempotencyKey,
            Clock clock
    ) {
        return new JobEntity(
                UUID.randomUUID(),
                type,
                priority,
                payload,
                estimatedDurationMs,
                idempotencyKey,
                Instant.now(clock)
        );
    }

    public void markScheduled(String workerId, Clock clock) {
        markScheduled(workerId, null, clock);
    }

    public void markScheduled(String workerId, String policy, Clock clock) {
        this.status = JobStatus.SCHEDULED;
        this.assignedWorkerId = workerId;
        this.scheduledPolicy = policy;
        this.updatedAt = Instant.now(clock);
    }

    public void markRunning(Clock clock) {
        this.status = JobStatus.RUNNING;
        Instant now = Instant.now(clock);
        this.startedAt = now;
        this.updatedAt = now;
    }

    public void markCompleted(Clock clock) {
        this.status = JobStatus.COMPLETED;
        Instant now = Instant.now(clock);
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void markFailed(Clock clock) {
        this.status = JobStatus.FAILED;
        Instant now = Instant.now(clock);
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void retryFromFailure(Clock clock) {
        this.status = JobStatus.QUEUED;
        this.assignedWorkerId = null;
        this.scheduledPolicy = null;
        this.retryCount++;
        Instant now = Instant.now(clock);
        this.queuedAt = now;
        this.startedAt = null;
        this.completedAt = null;
        this.updatedAt = now;
    }

    public void returnToQueue(Clock clock) {
        this.status = JobStatus.QUEUED;
        this.assignedWorkerId = null;
        this.scheduledPolicy = null;
        Instant now = Instant.now(clock);
        this.queuedAt = now;
        this.startedAt = null;
        this.updatedAt = now;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (queuedAt == null) {
            queuedAt = createdAt;
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public int getPriority() {
        return priority;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public long getEstimatedDurationMs() {
        return estimatedDurationMs;
    }

    public JobStatus getStatus() {
        return status;
    }

    public String getAssignedWorkerId() {
        return assignedWorkerId;
    }

    public String getScheduledPolicy() {
        return scheduledPolicy;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
