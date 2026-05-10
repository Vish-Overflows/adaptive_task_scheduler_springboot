package com.vishnusinha.orchestrator.scheduler.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.vishnusinha.orchestrator.scheduler.job.api.JobPageResponse;
import com.vishnusinha.orchestrator.scheduler.job.api.SubmitJobRequest;
import com.vishnusinha.orchestrator.scheduler.metrics.OrchestratorMetrics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobService {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;

    private final JobRepository jobRepository;
    private final JobQueue jobQueue;
    private final JobMapper jobMapper;
    private final OrchestratorMetrics orchestratorMetrics;
    private final Clock clock;

    public JobService(
            JobRepository jobRepository,
            JobQueue jobQueue,
            JobMapper jobMapper,
            OrchestratorMetrics orchestratorMetrics,
            Clock clock
    ) {
        this.jobRepository = jobRepository;
        this.jobQueue = jobQueue;
        this.jobMapper = jobMapper;
        this.orchestratorMetrics = orchestratorMetrics;
        this.clock = clock;
    }

    @Transactional
    public JobSubmissionResult submit(SubmitJobRequest request) {
        validatePayload(request.payload());

        String idempotencyKey = normalizeOptional(request.idempotencyKey());
        if (idempotencyKey != null) {
            Optional<JobEntity> existing = jobRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                orchestratorMetrics.recordJobSubmitted(existing.get().getType(), false);
                return new JobSubmissionResult(existing.get(), false);
            }
        }

        JobEntity job = JobEntity.queued(
                normalizeType(request.type()),
                request.normalizedPriority(),
                request.payload(),
                request.estimatedDurationMs(),
                idempotencyKey,
                clock
        );

        JobEntity saved = jobRepository.saveAndFlush(job);
        jobQueue.enqueue(saved.getId());
        orchestratorMetrics.recordJobSubmitted(saved.getType(), true);
        return new JobSubmissionResult(saved, true);
    }

    @Transactional(readOnly = true)
    public JobEntity get(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));
    }

    @Transactional(readOnly = true)
    public JobPageResponse list(Optional<JobStatus> status, int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = normalizeSize(size);
        PageRequest pageRequest = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<JobEntity> result = status
                .map(jobStatus -> jobRepository.findByStatus(jobStatus, pageRequest))
                .orElseGet(() -> jobRepository.findAll(pageRequest));

        return new JobPageResponse(
                result.stream().map(jobMapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private static int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private static String normalizeType(String type) {
        return type.trim().toUpperCase();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static void validatePayload(JsonNode payload) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidJobPayloadException("payload must be valid JSON");
        }
        if (!payload.isObject()) {
            throw new InvalidJobPayloadException("payload must be a JSON object");
        }
    }
}
