package com.vishnusinha.orchestrator.scheduler.worker.api;

import com.vishnusinha.orchestrator.scheduler.worker.WorkerMapper;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerRegistry;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerState;
import com.vishnusinha.orchestrator.shared.worker.RegisterWorkerRequest;
import com.vishnusinha.orchestrator.shared.worker.WorkerHeartbeatRequest;
import com.vishnusinha.orchestrator.shared.worker.WorkerStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workers")
public class WorkerController {

    private final WorkerRegistry workerRegistry;
    private final WorkerMapper workerMapper;

    public WorkerController(WorkerRegistry workerRegistry, WorkerMapper workerMapper) {
        this.workerRegistry = workerRegistry;
        this.workerMapper = workerMapper;
    }

    @PostMapping("/register")
    public ResponseEntity<WorkerResponse> register(
            @Valid @RequestBody RegisterWorkerRequest request,
            UriComponentsBuilder uriComponentsBuilder
    ) {
        WorkerResponse response = workerMapper.toResponse(workerRegistry.register(request));
        URI location = uriComponentsBuilder
                .path("/api/v1/workers/{workerId}")
                .buildAndExpand(response.workerId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/heartbeat")
    public WorkerResponse heartbeat(@Valid @RequestBody WorkerHeartbeatRequest request) {
        return workerMapper.toResponse(workerRegistry.heartbeat(request));
    }

    @GetMapping("/{workerId}")
    public WorkerResponse get(@PathVariable String workerId) {
        return workerMapper.toResponse(workerRegistry.get(workerId));
    }

    @GetMapping
    public WorkerListResponse list() {
        List<WorkerState> workers = workerRegistry.list();
        int activeWorkers = (int) workers.stream()
                .filter(worker -> worker.status() == WorkerStatus.ACTIVE)
                .count();
        int unhealthyWorkers = workers.size() - activeWorkers;
        return new WorkerListResponse(
                workers.stream().map(workerMapper::toResponse).toList(),
                workers.size(),
                activeWorkers,
                unhealthyWorkers
        );
    }
}
