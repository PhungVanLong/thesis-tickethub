package ict.thesis.booking.controller;

import ict.thesis.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final BookingService bookingService;

    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderDetails(@PathVariable Long orderId) {
        Map<String, Object> details = bookingService.getOrderDetail(orderId);
        return ResponseEntity.ok(details);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<org.springframework.data.domain.Page<Map<String, Object>>> getCustomerOrders(
            @PathVariable Long customerId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<Map<String, Object>> orders = bookingService.getCustomerOrders(customerId, pageable);
        return ResponseEntity.ok(orders);
    }

    @org.springframework.web.bind.annotation.PostMapping("/analytics/stats")
    public ResponseEntity<Map<String, Object>> getOrganizerDashboardStats(@org.springframework.web.bind.annotation.RequestBody java.util.List<Long> eventIds) {
        return ResponseEntity.ok(bookingService.getOrganizerDashboardStats(eventIds));
    }

    @org.springframework.web.bind.annotation.PostMapping("/analytics/recent")
    public ResponseEntity<java.util.List<Map<String, Object>>> getOrganizerDashboardRecentOrders(@org.springframework.web.bind.annotation.RequestBody java.util.List<Long> eventIds) {
        return ResponseEntity.ok(bookingService.getOrganizerDashboardRecentOrders(eventIds));
    }
}
