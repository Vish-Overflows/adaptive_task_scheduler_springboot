package com.vishnusinha.orchestrator.scheduler.dispatch;

import com.vishnusinha.orchestrator.scheduler.job.JobDispatchException;
import com.vishnusinha.orchestrator.scheduler.job.JobEntity;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerState;
import com.vishnusinha.orchestrator.shared.job.DispatchJobRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class WorkerDispatchClient {

    private final RestClient.Builder restClientBuilder;

    public WorkerDispatchClient(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    public void dispatch(JobEntity job, WorkerState worker) {
        DispatchJobRequest request = new DispatchJobRequest(
                job.getId(),
                job.getDispatchAttemptId(),
                job.getType(),
                job.getPayload(),
                job.getEstimatedDurationMs()
        );

        try {
            restClientBuilder
                    .baseUrl(worker.baseUrl().toString())
                    .build()
                    .post()
                    .uri("/api/v1/jobs/execute")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException exception) {
            throw new JobDispatchException(
                    "Worker %s did not accept job %s".formatted(worker.workerId(), job.getId()),
                    exception
            );
        }
    }
}
