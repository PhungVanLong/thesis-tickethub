package ict.thesis.booking.controller;

import ict.thesis.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final BookingService bookingService;

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Map<String, Object>>> getCustomerTickets(@PathVariable Long customerId) {
        List<Map<String, Object>> tickets = bookingService.getCustomerTickets(customerId);
        return ResponseEntity.ok(tickets);
    }
}
