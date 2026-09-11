package com.keystone.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised error handling — returns consistent JSON, never a stack trace.
 *
 * Shape: { timestamp, status, error, message, path, fieldErrors? }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // -------------------------------------------------------
    // 400 — Bean Validation failures (@Valid)
    // -------------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a));

        return build(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), fieldErrors);
    }

    // -------------------------------------------------------
    // 400 — ConstraintViolation (path/query params)
    // -------------------------------------------------------
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    // -------------------------------------------------------
    // 400 — Business rule violations (IllegalArgument/State)
    // -------------------------------------------------------
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> handleBusiness(
            RuntimeException ex,
            HttpServletRequest request) {

        HttpStatus status = ex instanceof IllegalStateException
                ? HttpStatus.CONFLICT          // 409 for lifecycle violations
                : HttpStatus.BAD_REQUEST;      // 400 for bad input

        return build(status, ex.getMessage(), request.getRequestURI(), null);
    }

    // -------------------------------------------------------
    // 401 — Authentication failures
    // -------------------------------------------------------
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuth(
            AuthenticationException ex,
            HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI(), null);
    }

    // -------------------------------------------------------
    // 403 — Access denied
    // -------------------------------------------------------
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI(), null);
    }

    // -------------------------------------------------------
    // 400 / 401 — RuntimeException catch-all
    // Not-found messages → 404, auth failure → 401, else → 500
    // -------------------------------------------------------
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(
            RuntimeException ex,
            HttpServletRequest request) {

        String msg = ex.getMessage() != null ? ex.getMessage() : "Unexpected error";
        String msgLower = msg.toLowerCase();

        HttpStatus status;
        if (msgLower.contains("not found")) {
            status = HttpStatus.NOT_FOUND;                        // 404
        } else if (msgLower.contains("invalid email or password")
                || msgLower.contains("invalid credentials")
                || msgLower.contains("unauthorized")) {
            status = HttpStatus.UNAUTHORIZED;                      // 401
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;             // 500
            log.error("Unhandled exception on [{}]: {}", request.getRequestURI(), msg, ex);
        }

        return build(status, msg, request.getRequestURI(), null);
    }

    // -------------------------------------------------------
    // Helper
    // -------------------------------------------------------
    private ResponseEntity<Map<String, Object>> build(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> fieldErrors) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            body.put("fieldErrors", fieldErrors);
        }
        return ResponseEntity.status(status).body(body);
    }
}
