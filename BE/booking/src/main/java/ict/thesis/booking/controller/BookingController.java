package ict.thesis.booking.controller;

import ict.thesis.booking.dto.BookingDtos.CreateBookingRequest;
import ict.thesis.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<Map<String, String>> submitBooking(@RequestBody CreateBookingRequest request) {
        String requestId = bookingService.submitBookingRequest(request);
        return ResponseEntity.ok(Map.of("requestId", requestId));
    }

    @GetMapping(path = "/stream/{requestId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBookingResult(@PathVariable String requestId) {
        return bookingService.subscribeToBookingResult(requestId);
    }

    @PostMapping("/{orderId}/mock-pay")
    public ResponseEntity<Map<String, String>> mockPay(@PathVariable Long orderId) {
        bookingService.completeMockPayment(orderId);
        return ResponseEntity.ok(Map.of("message", "Payment simulated successfully"));
    }

    @PostMapping("/{orderId}/mock-cancel")
    public ResponseEntity<Map<String, String>> mockCancel(@PathVariable Long orderId) {
        bookingService.cancelMockPayment(orderId);
        return ResponseEntity.ok(Map.of("message", "Cancellation simulated successfully"));
    }
}
