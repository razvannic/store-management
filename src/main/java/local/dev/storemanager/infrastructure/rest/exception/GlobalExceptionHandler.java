package local.dev.storemanager.infrastructure.rest.exception;

import local.dev.storemanager.application.exception.ProductNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProductNotFound(ProductNotFoundException ex) {
        return handleExceptionAndLog(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex) {
        return handleExceptionAndLog(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage()
                )).toList();

        return handleExceptionAndLog(errors, HttpStatus.BAD_REQUEST);
    }

    private static ResponseEntity<Map<String, Object>> handleExceptionAndLog(List<Map<String, String>> errors, HttpStatus httpStatus) {
        final var timestamp = Instant.now();
        log.error("Exceptions: {}. Status: {}. Timestamp: {}", errors, httpStatus, timestamp);
        return ResponseEntity.status(httpStatus)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", httpStatus.value(),
                        "error", httpStatus.getReasonPhrase(),
                        "messages", errors
                ));
    }

    private static ResponseEntity<Map<String, Object>> handleExceptionAndLog(String error, HttpStatus httpStatus) {
        final var timestamp = Instant.now();
        log.error("Exception: {}. Status: {} Timestamp: {}", error, httpStatus, timestamp);
        return ResponseEntity.status(httpStatus)
                .body(Map.of(
                        "timestamp", timestamp,
                        "status", httpStatus.value(),
                        "error", httpStatus.getReasonPhrase(),
                        "message", error
                ));
    }
}