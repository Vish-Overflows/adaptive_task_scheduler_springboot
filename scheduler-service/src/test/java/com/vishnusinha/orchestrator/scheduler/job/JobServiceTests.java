package com.vishnusinha.orchestrator.scheduler.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vishnusinha.orchestrator.scheduler.job.api.SubmitJobRequest;
import com.vishnusinha.orchestrator.scheduler.metrics.OrchestratorMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-05T10:00:00Z"), ZoneOffset.UTC);

    private final JobRepository jobRepository = org.mockito.Mockito.mock(JobRepository.class);
    private final JobQueue jobQueue = org.mockito.Mockito.mock(JobQueue.class);
    private final JobMapper jobMapper = new JobMapper();
    private final OrchestratorMetrics orchestratorMetrics = org.mockito.Mockito.mock(OrchestratorMetrics.class);
    private final JobService jobService = new JobService(jobRepository, jobQueue, jobMapper, orchestratorMetrics, CLOCK);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void submitPersistsQueuedJobAndEnqueuesItsId() {
        SubmitJobRequest request = request("render", "idem-1");
        when(jobRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(jobRepository.saveAndFlush(any(JobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobSubmissionResult result = jobService.submit(request);

        assertThat(result.created()).isTrue();
        assertThat(result.job().getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(result.job().getType()).isEqualTo("RENDER");
        assertThat(result.job().getPriority()).isEqualTo(100);
        assertThat(result.job().getCreatedAt()).isEqualTo(Instant.parse("2026-05-05T10:00:00Z"));

        ArgumentCaptor<UUID> queuedJobId = ArgumentCaptor.forClass(UUID.class);
        verify(jobQueue).enqueue(queuedJobId.capture());
        assertThat(queuedJobId.getValue()).isEqualTo(result.job().getId());
    }

    @Test
    void submitWithExistingIdempotencyKeyReturnsExistingJobWithoutRequeueing() {
        JobEntity existing = JobEntity.queued(
                "EXPORT",
                50,
                objectMapper.createObjectNode().put("rows", 1000),
                5000,
                "idem-existing",
                CLOCK
        );
        when(jobRepository.findByIdempotencyKey("idem-existing")).thenReturn(Optional.of(existing));

        JobSubmissionResult result = jobService.submit(request("export", "idem-existing"));

        assertThat(result.created()).isFalse();
        assertThat(result.job()).isSameAs(existing);
        verify(jobRepository, never()).saveAndFlush(any(JobEntity.class));
        verify(jobQueue, never()).enqueue(any(UUID.class));
    }

    @Test
    void submitRejectsNonObjectPayloads() {
        SubmitJobRequest request = new SubmitJobRequest(
                "render",
                10,
                objectMapper.valueToTree("not-an-object"),
                1000L,
                null
        );

        assertThatThrownBy(() -> jobService.submit(request))
                .isInstanceOf(InvalidJobPayloadException.class)
                .hasMessageContaining("payload must be a JSON object");
    }

    @Test
    void getThrowsWhenJobDoesNotExist() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.get(jobId))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining(jobId.toString());
    }

    private SubmitJobRequest request(String type, String idempotencyKey) {
        return new SubmitJobRequest(
                type,
                null,
                objectMapper.createObjectNode().put("inputSize", 42),
                1500L,
                idempotencyKey
        );
    }
}
