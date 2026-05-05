package ict.thesis.identity.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(
        ApiException ex,
        HttpServletRequest request
    ) {
        logger.warn("API error: {}", ex.getMessage());
        return new ResponseEntity<>(
            new ApiErrorResponse(
                ex.getCode(),
                ex.getMessage(),
                Map.of(),
                Instant.now(),
                request.getRequestURI()
            ),
            ex.getStatus()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        return new ResponseEntity<>(
            new ApiErrorResponse(
                "VALIDATION_ERROR",
                "Validation failed",
                Map.of("fields", fieldErrors),
                Instant.now(),
                request.getRequestURI()
            ),
            HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
        Exception ex,
        HttpServletRequest request
    ) {
        logger.error("Unexpected error", ex);
        return new ResponseEntity<>(
            new ApiErrorResponse(
                "INTERNAL_ERROR",
                "Unexpected error",
                Map.of(),
                Instant.now(),
                request.getRequestURI()
            ),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
