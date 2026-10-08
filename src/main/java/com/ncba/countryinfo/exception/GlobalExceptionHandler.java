package com.ncba.countryinfo.exception;

import com.ncba.countryinfo.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps every exception to a consistent ApiError with the right HTTP status.
 * Client errors (4xx) are logged at WARN, upstream/server errors (5xx) at ERROR.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        log.warn("validation_failed path={} fields={}", req.getRequestURI(), fieldErrors.keySet());
        return build(HttpStatus.BAD_REQUEST, "Request validation failed", req, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON", req, null);
    }

    @ExceptionHandler(CountryNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(CountryNotFoundException ex, HttpServletRequest req) {
        log.warn("not_found path={} reason=\"{}\"", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
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
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                message, req.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
