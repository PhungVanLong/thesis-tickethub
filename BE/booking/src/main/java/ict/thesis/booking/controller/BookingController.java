package ict.thesis.booking.controller;

import ict.thesis.booking.dto.BookingDtos.CreateBookingRequest;
import ict.thesis.booking.service.BookingService;
import ict.thesis.booking.service.BookingSseService;
import ict.thesis.booking.service.PaymentService;
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
    private final BookingSseService bookingSseService;
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Map<String, String>> submitBooking(@RequestBody CreateBookingRequest request) {
        String requestId = bookingService.submitBookingRequest(request);
        return ResponseEntity.ok(Map.of("requestId", requestId));
    }

    @GetMapping(path = "/stream/{requestId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBookingResult(@PathVariable String requestId) {
        return bookingSseService.subscribeToBookingResult(requestId);
    }

    @PostMapping("/{orderId}/mock-pay")
    public ResponseEntity<Map<String, String>> mockPay(@PathVariable Long orderId) {
        paymentService.completeMockPayment(orderId);
        return ResponseEntity.ok(Map.of("message", "Payment simulated successfully"));
    }

    @PostMapping("/{orderId}/mock-cancel")
    public ResponseEntity<Map<String, String>> mockCancel(@PathVariable Long orderId) {
        paymentService.cancelMockPayment(orderId);
        return ResponseEntity.ok(Map.of("message", "Cancellation simulated successfully"));
    }

    @GetMapping("/{orderId}/vnpay-url")
    public ResponseEntity<Map<String, String>> getVNPayUrl(@PathVariable Long orderId, jakarta.servlet.http.HttpServletRequest request) {
        String paymentUrl = paymentService.createVNPayPaymentUrl(orderId, request);
        return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
    }

    @GetMapping("/vnpay-return")
    public void vnpayReturn(@org.springframework.web.bind.annotation.RequestParam Map<String, String> allParams, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        boolean success = paymentService.processVNPayCallback(allParams);
        String txnRef = allParams.get("vnp_TxnRef");
        String orderIdStr = (txnRef != null && txnRef.contains("_")) ? txnRef.split("_")[0] : txnRef;
        String redirectUrl = "http://localhost:4200/checkout/" + orderIdStr;
        if (!success) {
            redirectUrl = "http://localhost:4200/checkout/" + orderIdStr + "?error=payment_failed";
        }
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/{orderId}/paypal-url")
    public ResponseEntity<Map<String, String>> getPayPalUrl(@PathVariable Long orderId) {
        String paymentUrl = paymentService.createPayPalPaymentUrl(orderId);
        return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl != null ? paymentUrl : ""));
    }

    @GetMapping("/paypal-return")
    public void paypalReturn(@org.springframework.web.bind.annotation.RequestParam("orderId") Long orderId,
                             @org.springframework.web.bind.annotation.RequestParam("token") String token,
                             jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        boolean success = paymentService.processPayPalCallback(orderId, token);
        String redirectUrl = "http://localhost:4200/checkout/" + orderId;
        if (!success) {
            redirectUrl = "http://localhost:4200/checkout/" + orderId + "?error=payment_failed";
        }
        response.sendRedirect(redirectUrl);
    }
}

