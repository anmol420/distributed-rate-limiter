package codes.anmol.distributedratelimiter.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitExceedException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitException(RateLimitExceedException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Rate limit exceeded");
        body.put("key", ex.getKey());
        body.put("limit", ex.getLimit());
        body.put("windowSeconds", ex.getWindowSeconds());
        body.put("timestamp", Instant.now().toString());

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getWindowSeconds()))
                .body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for(FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Validation failed");
        body.put("field", fieldErrors);
        body.put("timestamp", Instant.now().toString());

        return ResponseEntity
                .badRequest()
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Internal server error");
        body.put("message", ex.getMessage());
        body.put("timestamp", Instant.now().toString());

        return ResponseEntity
                .internalServerError()
                .body(body);
    }
}
