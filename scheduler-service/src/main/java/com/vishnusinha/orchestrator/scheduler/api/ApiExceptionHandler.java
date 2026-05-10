package com.vishnusinha.orchestrator.scheduler.api;

import com.vishnusinha.orchestrator.scheduler.job.InvalidJobPayloadException;
import com.vishnusinha.orchestrator.scheduler.job.InvalidJobStatusException;
import com.vishnusinha.orchestrator.scheduler.job.JobCompletionRejectedException;
import com.vishnusinha.orchestrator.scheduler.job.JobNotFoundException;
import com.vishnusinha.orchestrator.scheduler.job.QueueUnavailableException;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerNotFoundException;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerStateSerializationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "One or more fields failed validation.",
                request
        );

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request parameters",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleMalformedJson(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON",
                "Request body must be valid JSON.",
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request parameter",
                "Parameter '%s' has an unsupported value.".formatted(exception.getName()),
                request
        );
    }

    @ExceptionHandler(InvalidJobPayloadException.class)
    ProblemDetail handleInvalidPayload(InvalidJobPayloadException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid job payload", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidJobStatusException.class)
    ProblemDetail handleInvalidStatus(InvalidJobStatusException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid job status", exception.getMessage(), request);
    }

    @ExceptionHandler(JobNotFoundException.class)
    ProblemDetail handleJobNotFound(JobNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Job not found", exception.getMessage(), request);
    }

    @ExceptionHandler(JobCompletionRejectedException.class)
    ProblemDetail handleJobCompletionRejected(JobCompletionRejectedException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Job completion rejected", exception.getMessage(), request);
    }

    @ExceptionHandler(WorkerNotFoundException.class)
    ProblemDetail handleWorkerNotFound(WorkerNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Worker not found", exception.getMessage(), request);
    }

    @ExceptionHandler({QueueUnavailableException.class, DataAccessResourceFailureException.class, WorkerStateSerializationException.class})
    ProblemDetail handleDependencyUnavailable(RuntimeException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Dependency unavailable",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT,
                "Conflicting job submission",
                "A job with the same unique submission metadata already exists.",
                request
        );
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
