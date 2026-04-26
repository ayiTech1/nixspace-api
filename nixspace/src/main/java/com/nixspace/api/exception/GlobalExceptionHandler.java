package com.nixspace.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NixSpaceExceptions.ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NixSpaceExceptions.ResourceNotFoundException ex) {
        return buildProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), "not-found");
    }

    @ExceptionHandler(NixSpaceExceptions.DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicate(NixSpaceExceptions.DuplicateResourceException ex) {
        return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), "conflict");
    }

    @ExceptionHandler(NixSpaceExceptions.AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(NixSpaceExceptions.AccessDeniedException ex) {
        return buildProblemDetail(HttpStatus.FORBIDDEN, ex.getMessage(), "forbidden");
    }

    @ExceptionHandler(NixSpaceExceptions.BadRequestException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(NixSpaceExceptions.BadRequestException ex) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), "bad-request");
    }

    @ExceptionHandler(NixSpaceExceptions.InvalidTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidToken(NixSpaceExceptions.InvalidTokenException ex) {
        return buildProblemDetail(HttpStatus.UNAUTHORIZED, ex.getMessage(), "unauthorized");
    }

    @ExceptionHandler(NixSpaceExceptions.BusinessRuleException.class)
    public ResponseEntity<ProblemDetail> handleBusinessRule(NixSpaceExceptions.BusinessRuleException ex) {
        return buildProblemDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), "business-rule-violation");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException ex) {
        return buildProblemDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password", "unauthorized");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ProblemDetail> handleDisabled(DisabledException ex) {
        return buildProblemDetail(HttpStatus.FORBIDDEN, "Account is disabled", "forbidden");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = (error instanceof FieldError fe) ? fe.getField() : error.getObjectName();
            fieldErrors.put(fieldName, error.getDefaultMessage());
        });

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setType(URI.create("urn:nixspace:validation-error"));
        pd.setProperty("errors", fieldErrors);
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        return buildProblemDetail(HttpStatus.PAYLOAD_TOO_LARGE, "File size exceeds the maximum allowed limit", "payload-too-large");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", "internal-error");
    }

    private ResponseEntity<ProblemDetail> buildProblemDetail(HttpStatus status, String detail, String errorCode) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create("urn:nixspace:" + errorCode));
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(status).body(pd);
    }
}
