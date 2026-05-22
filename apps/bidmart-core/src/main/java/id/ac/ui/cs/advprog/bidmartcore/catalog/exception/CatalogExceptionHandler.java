package id.ac.ui.cs.advprog.bidmartcore.catalog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "id.ac.ui.cs.advprog.bidmartcore.catalog")
public class CatalogExceptionHandler {

    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_STATUS = "status";
    private static final String KEY_ERROR = "error";
    private static final String KEY_MESSAGE = "message";

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(KEY_TIMESTAMP, LocalDateTime.now());
        errorResponse.put(KEY_STATUS, HttpStatus.CONFLICT.value());
        errorResponse.put(KEY_ERROR, "Conflict");
        errorResponse.put(KEY_MESSAGE, ex.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(KEY_TIMESTAMP, LocalDateTime.now());
        errorResponse.put(KEY_STATUS, HttpStatus.BAD_REQUEST.value());
        errorResponse.put(KEY_ERROR, "Bad Request");
        errorResponse.put(KEY_MESSAGE, ex.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(KEY_TIMESTAMP, LocalDateTime.now());
        errorResponse.put(KEY_STATUS, HttpStatus.FORBIDDEN.value());
        errorResponse.put(KEY_ERROR, "Forbidden");
        errorResponse.put(KEY_MESSAGE, ex.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }
}