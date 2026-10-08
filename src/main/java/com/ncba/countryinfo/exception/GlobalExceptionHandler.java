package com.ncba.countryinfo.exception;

import com.ncba.countryinfo.config.CorrelationIdFilter;
import com.ncba.countryinfo.dto.ApiError;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps every exception to a consistent ApiError with the right HTTP status.
 * Client errors (4xx) are logged at WARN, upstream/server errors (5xx) at ERROR.
 * Internal details are never returned; the correlation id links the response to the logs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ----- 400 Bad Request -----

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        log.warn("validation_failed path={} fields={}", req.getRequestURI(), fieldErrors.keySet());
        return build(HttpStatus.BAD_REQUEST, "Request validation failed", req, fieldErrors);
    }

    @ExceptionHandler({HandlerMethodValidationException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiError> handleParamValidation(Exception ex, HttpServletRequest req) {
        log.warn("parameter_validation_failed path={}", req.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "One or more request parameters are invalid", req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Parameter '%s' has an invalid value".formatted(ex.getName()), req, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON", req, null);
    }

    // ----- 404 Not Found -----

    @ExceptionHandler({ResourceNotFoundException.class, CountryNotFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex, HttpServletRequest req) {
        log.warn("not_found path={} reason=\"{}\"", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "No endpoint at this path", req, null);
    }

    // ----- 405 / 409 -----

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), req, null);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleConflict(ObjectOptimisticLockingFailureException ex, HttpServletRequest req) {
        log.warn("concurrent_update_conflict path={}", req.getRequestURI());
        return build(HttpStatus.CONFLICT, "The record was modified by another request; reload and retry", req, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("data_integrity_violation path={}", req.getRequestURI());
        return build(HttpStatus.CONFLICT, "The request conflicts with existing data", req, null);
    }

    // ----- 5xx: upstream SOAP problems -----

    @ExceptionHandler({ExternalServiceUnavailableException.class, CallNotPermittedException.class})
    public ResponseEntity<ApiError> handleUnavailable(RuntimeException ex, HttpServletRequest req) {
        log.error("upstream_unavailable path={} reason=\"{}\"", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "30")
                .body(error(HttpStatus.SERVICE_UNAVAILABLE,
                        "The country information service is temporarily unavailable. Please try again shortly.",
                        req, null));
    }

    @ExceptionHandler(SoapServiceException.class)
    public ResponseEntity<ApiError> handleUpstream(SoapServiceException ex, HttpServletRequest req) {
        log.error("upstream_error path={} reason=\"{}\"", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, "The country information service returned an error", req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("unhandled_exception path={}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", req, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req,
                                           Map<String, String> fieldErrors) {
        return ResponseEntity.status(status).body(error(status, message, req, fieldErrors));
    }

    private ApiError error(HttpStatus status, String message, HttpServletRequest req, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message,
                req.getRequestURI(), MDC.get(CorrelationIdFilter.MDC_KEY), fieldErrors);
    }
}
