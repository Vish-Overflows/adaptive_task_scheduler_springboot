package com.vishnusinha.orchestrator.scheduler.job.api;

import com.vishnusinha.orchestrator.scheduler.job.JobMapper;
import com.vishnusinha.orchestrator.scheduler.job.JobCompletionService;
import com.vishnusinha.orchestrator.scheduler.job.JobService;
import com.vishnusinha.orchestrator.scheduler.job.JobStatus;
import com.vishnusinha.orchestrator.scheduler.job.JobSubmissionResult;
import com.vishnusinha.orchestrator.scheduler.job.InvalidJobStatusException;
import com.vishnusinha.orchestrator.shared.job.CompleteJobRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;
    private final JobCompletionService jobCompletionService;
    private final JobMapper jobMapper;

    public JobController(JobService jobService, JobCompletionService jobCompletionService, JobMapper jobMapper) {
        this.jobService = jobService;
        this.jobCompletionService = jobCompletionService;
        this.jobMapper = jobMapper;
    }

    @PostMapping
    public ResponseEntity<JobResponse> submit(
            @Valid @RequestBody SubmitJobRequest request,
            UriComponentsBuilder uriComponentsBuilder
    ) {
        JobSubmissionResult result = jobService.submit(request);
        JobResponse response = jobMapper.toResponse(result.job());
        URI location = uriComponentsBuilder
                .path("/api/v1/jobs/{id}")
                .buildAndExpand(response.id())
                .toUri();

        if (result.created()) {
            return ResponseEntity.created(location).body(response);
        }
        return ResponseEntity.ok()
                .location(location)
                .body(response);
    }

    @GetMapping("/{jobId}")
    public JobResponse get(@PathVariable UUID jobId) {
        return jobMapper.toResponse(jobService.get(jobId));
    }

    @PostMapping("/{jobId}/completion")
    public JobResponse complete(
            @PathVariable UUID jobId,
            @Valid @RequestBody CompleteJobRequest request
    ) {
        CompleteJobRequest normalized = new CompleteJobRequest(
                jobId,
                request.workerId(),
                request.status(),
                request.message(),
                request.runtimeMs()
        );
        return jobMapper.toResponse(jobCompletionService.complete(normalized));
    }

    @GetMapping
    public JobPageResponse list(
            @RequestParam(required = false) Optional<String> status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        return jobService.list(status.map(JobController::parseStatus), page, size);
    }

    private static JobStatus parseStatus(String status) {
        try {
            return JobStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new InvalidJobStatusException(status);
        }
    }
}
