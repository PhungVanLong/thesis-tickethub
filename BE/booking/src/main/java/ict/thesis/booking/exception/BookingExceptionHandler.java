package ict.thesis.booking.exception;

import ict.thesis.booking.dto.BookingDtos.ApiErrorResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BookingExceptionHandler {

    @ExceptionHandler(BookingExceptions.BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BookingExceptions.BadRequestException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(BookingExceptions.NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(BookingExceptions.NotFoundException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(BookingExceptions.ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(BookingExceptions.ConflictException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(BookingExceptions.BookingException.class)
    public ResponseEntity<ApiErrorResponse> handleBookingException(BookingExceptions.BookingException ex,
                                                                   HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống", request.getRequestURI(), ex.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status,
                                                   String message,
                                                   String path,
                                                   Object details) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                details
        ));
    }
}

