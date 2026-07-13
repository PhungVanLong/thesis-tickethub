package ict.thesis.booking.controller;

import ict.thesis.booking.enties.Checkin;
import ict.thesis.booking.enties.Ticket;
import ict.thesis.booking.enties.enums.CheckinMethod;
import ict.thesis.booking.enties.enums.TicketStatus;
import ict.thesis.booking.repository.CheckinRepository;
import ict.thesis.booking.repository.TicketRepository;
import ict.thesis.booking.service.BookingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final BookingService bookingService;
    private final TicketRepository ticketRepository;
    private final CheckinRepository checkinRepository;

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Map<String, Object>>> getCustomerTickets(@PathVariable Long customerId) {
        List<Map<String, Object>> tickets = bookingService.getCustomerTickets(customerId);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/code/{ticketCode}")
    public ResponseEntity<Map<String, Object>> getTicketDetailByCode(@PathVariable String ticketCode) {
        Map<String, Object> ticketDetail = bookingService.getTicketDetailByCode(ticketCode);
        return ResponseEntity.ok(ticketDetail);
    }

    @Data
    public static class CheckinRequest {
        private String ticketCode;
        private Long staffId;
        private Long eventId;
        private String deviceId;
        private String method; // QR_SCAN or MANUAL
    }

    @PostMapping("/checkin")
    public ResponseEntity<Map<String, Object>> performCheckin(
            @RequestBody CheckinRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {

        // 1. Find ticket
        Ticket ticket = ticketRepository.findByTicketCode(request.getTicketCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        // 2. Validate ticket status
        if (ticket.getStatus() == TicketStatus.USED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ticket has already been checked in");
        }
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket is cancelled");
        }

        // 3. Check for existing checkin record
        boolean alreadyCheckedIn = checkinRepository.findByTicketId(ticket.getId())
                .stream().anyMatch(c -> true);
        if (alreadyCheckedIn) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ticket has already been checked in");
        }

        // 4. Determine staffId
        Long staffId = request.getStaffId();
        if (staffId == null && userIdHeader != null) {
            try {
                staffId = Long.parseLong(userIdHeader);
            } catch (NumberFormatException ignored) {}
        }

        // 5. Record checkin
        CheckinMethod method = CheckinMethod.QR_SCAN;
        if ("MANUAL".equalsIgnoreCase(request.getMethod())) {
            method = CheckinMethod.MANUAL;
        }

        Checkin checkin = Checkin.builder()
                .ticket(ticket)
                .staff(staffId)
                .eventId(request.getEventId() != null ? request.getEventId()
                        : (ticket.getOrderItem() != null && ticket.getOrderItem().getOrder() != null
                                ? ticket.getOrderItem().getOrder().getEventId() : null))
                .method(method)
                .deviceId(request.getDeviceId())
                .checkedInAt(Instant.now())
                .build();
        checkinRepository.save(checkin);

        // 6. Update ticket status to USED
        ticket.setStatus(TicketStatus.USED);
        ticketRepository.save(ticket);

        // 7. Build response
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("ticketCode", ticket.getTicketCode());
        response.put("seatCode", ticket.getSeatCode());
        response.put("checkedInAt", checkin.getCheckedInAt().toString());
        response.put("method", method.name());

        // Include customer info if available
        if (ticket.getOrderItem() != null && ticket.getOrderItem().getOrder() != null) {
            response.put("customerEmail", ticket.getOrderItem().getOrder().getCustomerEmail());
            response.put("eventId", ticket.getOrderItem().getOrder().getEventId());
        }

        // Include tier info if available
        if (ticket.getOrderItem() != null && ticket.getOrderItem().getTicketTier() != null) {
            response.put("tierName", ticket.getOrderItem().getTicketTier().getName());
        }

        return ResponseEntity.ok(response);
    }
}

