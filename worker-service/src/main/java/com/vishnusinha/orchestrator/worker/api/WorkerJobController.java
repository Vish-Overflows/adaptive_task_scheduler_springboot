package com.vishnusinha.orchestrator.worker.api;

import com.vishnusinha.orchestrator.shared.job.DispatchJobRequest;
import com.vishnusinha.orchestrator.worker.runtime.WorkerExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
public class WorkerJobController {

    private final WorkerExecutionService workerExecutionService;

    public WorkerJobController(WorkerExecutionService workerExecutionService) {
        this.workerExecutionService = workerExecutionService;
    }

    @PostMapping("/execute")
    public ResponseEntity<Void> execute(@Valid @RequestBody DispatchJobRequest request) {
        workerExecutionService.accept(request);
        return ResponseEntity.accepted().build();
    }
}
