package com.orbitallens.backend.api.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central exception translator for all REST controllers.
 * Ensures uniform JSON error responses across the entire API without exposing stack traces.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                details.add(error.getField() + ": " + error.getDefaultMessage())
        );
        ErrorResponse body = new ErrorResponse(
                "VALIDATION_ERROR",
                "Request validation failed",
                details,
                Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        ErrorResponse body = new ErrorResponse(
                "BAD_REQUEST",
                "Malformed JSON request body",
                Collections.singletonList(ex.getMessage()),
                Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        ErrorResponse body = new ErrorResponse(
                "BAD_REQUEST",
                ex.getMessage(),
                ex.getDetails(),
                Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ErrorResponse> handleDateTimeParse(DateTimeParseException ex) {
        ErrorResponse body = new ErrorResponse(
                "BAD_REQUEST",
                "Invalid ISO-8601 date-time format: " + ex.getParsedString(),
                Collections.singletonList(ex.getMessage()),
                Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(SatelliteNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleSatelliteNotSupported(SatelliteNotSupportedException ex) {
        ErrorResponse body = new ErrorResponse(
                "SATELLITE_NOT_SUPPORTED",
                ex.getMessage(),
                Collections.emptyList(),
                Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(TleExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTleExpired(TleExpiredException ex) {
        ErrorResponse body = new ErrorResponse(
                "TLE_EXPIRED",
                ex.getMessage(),
                Collections.emptyList(),
                Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(TleUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleTleUnavailable(TleUnavailableException ex) {
        ErrorResponse body = new ErrorResponse(
                "TLE_UNAVAILABLE",
                ex.getMessage(),
                Collections.emptyList(),
                Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse body = new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected internal error occurred",
                Collections.emptyList(),
                Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
