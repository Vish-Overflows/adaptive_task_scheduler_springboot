package com.vishnusinha.orchestrator.worker.api;

import com.vishnusinha.orchestrator.worker.runtime.WorkerCapacityExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class WorkerApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", "One or more fields failed validation.", request);
    }

    @ExceptionHandler(WorkerCapacityExceededException.class)
    ProblemDetail handleCapacityExceeded(WorkerCapacityExceededException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Worker capacity exceeded", exception.getMessage(), request);
    }

    private static ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://errors.orchestrator.local/" + status.value()));
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
