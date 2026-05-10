package com.vishnusinha.orchestrator.scheduler.job;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    Optional<JobEntity> findByIdempotencyKey(String idempotencyKey);

    Page<JobEntity> findByStatus(JobStatus status, Pageable pageable);

    long countByStatus(JobStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<JobEntity> findByStatusIn(Collection<JobStatus> statuses);

    List<JobEntity> findTop100ByStatusOrderByCreatedAtAsc(JobStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<JobEntity> findFirstByStatusOrderByCreatedAtAsc(JobStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<JobEntity> findFirstByStatusOrderByPriorityDescCreatedAtAsc(JobStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<JobEntity> findFirstByStatusOrderByEstimatedDurationMsAscCreatedAtAsc(JobStatus status);

    default Optional<JobEntity> findNextQueuedFifo() {
        return findFirstByStatusOrderByCreatedAtAsc(JobStatus.QUEUED);
    }

    default Optional<JobEntity> findNextQueuedByPriority() {
        return findFirstByStatusOrderByPriorityDescCreatedAtAsc(JobStatus.QUEUED);
    }

    default Optional<JobEntity> findNextQueuedByShortestDuration() {
        return findFirstByStatusOrderByEstimatedDurationMsAscCreatedAtAsc(JobStatus.QUEUED);
    }
}
