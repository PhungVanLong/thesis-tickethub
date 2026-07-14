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

import ict.thesis.booking.service.CheckinSseService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final BookingService bookingService;
    private final TicketRepository ticketRepository;
    private final CheckinRepository checkinRepository;
    private final RestTemplate restTemplate;
    private final CheckinSseService checkinSseService;

    @org.springframework.beans.factory.annotation.Value("${gateway.shared-secret}")
    private String gatewaySharedSecret;

    @org.springframework.beans.factory.annotation.Value("${management.service.url}")
    private String managementServiceUrl;

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<org.springframework.data.domain.Page<Map<String, Object>>> getCustomerTickets(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<Map<String, Object>> tickets = bookingService.getCustomerTickets(customerId, pageable);
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

        Long eventId = checkin.getEventId();
        if (eventId != null) {
            checkinSseService.broadcast(eventId, response);
        }

        return ResponseEntity.ok(response);
    }

    @Data
    public static class StaffCheckinRequest {
        private String ticketCode;
        private String deviceId;
        private String method; // QR_SCAN or MANUAL
    }

    @PostMapping("/staff/checkin")
    public ResponseEntity<Map<String, Object>> performStaffCheckin(
            @RequestBody StaffCheckinRequest request,
            @RequestHeader(value = "X-User-Id", required = true) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = true) String userRoleHeader) {

        if (userRoleHeader == null || !userRoleHeader.toUpperCase().contains("STAFF")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Only staff members can perform this action.");
        }

        Long staffId;
        try {
            staffId = Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid User ID");
        }

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

        // 3. Get event ID
        Long eventId = null;
        if (ticket.getOrderItem() != null && ticket.getOrderItem().getOrder() != null) {
            eventId = ticket.getOrderItem().getOrder().getEventId();
        }
        if (eventId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket does not belong to any event");
        }

        // 4. Verify staff is assigned to the event of the ticket
        String checkUrl = managementServiceUrl + "/api/events/" + eventId + "/staff/check?staffId=" + staffId;
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("X-Gateway-Token", gatewaySharedSecret);
        org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

        try {
            ResponseEntity<Boolean> checkResponse = restTemplate.exchange(
                    checkUrl, org.springframework.http.HttpMethod.GET, entity, Boolean.class
            );
            if (checkResponse.getBody() == null || !checkResponse.getBody()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff is not assigned to this event");
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff is not assigned to this event");
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to verify staff assignment", e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to verify staff assignment", e);
        }

        // 5. Check for existing checkin record
        boolean alreadyCheckedIn = checkinRepository.findByTicketId(ticket.getId())
                .stream().anyMatch(c -> true);
        if (alreadyCheckedIn) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ticket has already been checked in");
        }

        // 6. Record checkin
        CheckinMethod method = CheckinMethod.QR_SCAN;
        if ("MANUAL".equalsIgnoreCase(request.getMethod())) {
            method = CheckinMethod.MANUAL;
        }

        Checkin checkin = Checkin.builder()
                .ticket(ticket)
                .staff(staffId)
                .eventId(eventId)
                .method(method)
                .deviceId(request.getDeviceId())
                .checkedInAt(Instant.now())
                .build();
        checkinRepository.save(checkin);

        // 7. Update ticket status to USED
        ticket.setStatus(TicketStatus.USED);
        ticketRepository.save(ticket);

        // 8. Build response
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("ticketCode", ticket.getTicketCode());
        response.put("seatCode", ticket.getSeatCode());
        response.put("checkedInAt", checkin.getCheckedInAt().toString());
        response.put("method", method.name());
        response.put("eventId", eventId);

        if (ticket.getOrderItem() != null && ticket.getOrderItem().getOrder() != null) {
            response.put("customerEmail", ticket.getOrderItem().getOrder().getCustomerEmail());
        }
        if (ticket.getOrderItem() != null && ticket.getOrderItem().getTicketTier() != null) {
            response.put("tierName", ticket.getOrderItem().getTicketTier().getName());
        }

        if (eventId != null) {
            checkinSseService.broadcast(eventId, response);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/staff/checkins")
    public ResponseEntity<org.springframework.data.domain.Page<Map<String, Object>>> getStaffCheckins(
            @RequestHeader(value = "X-User-Id", required = true) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = true) String userRoleHeader,
            @RequestParam(required = false) Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (userRoleHeader == null || !userRoleHeader.toUpperCase().contains("STAFF")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Only staff members can view history.");
        }

        Long staffId;
        try {
            staffId = Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid User ID");
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("checkedInAt").descending());
        org.springframework.data.domain.Page<Checkin> checkinPage;

        if (eventId != null) {
            checkinPage = checkinRepository.findByStaffAndEventId(staffId, eventId, pageable);
        } else {
            checkinPage = checkinRepository.findByStaff(staffId, pageable);
        }

        org.springframework.data.domain.Page<Map<String, Object>> responsePage = checkinPage.map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("checkinId", c.getId());
            map.put("checkedInAt", c.getCheckedInAt());
            map.put("method", c.getMethod() != null ? c.getMethod().name() : null);
            map.put("deviceId", c.getDeviceId());
            map.put("eventId", c.getEventId());

            Ticket t = c.getTicket();
            if (t != null) {
                map.put("ticketCode", t.getTicketCode());
                map.put("seatCode", t.getSeatCode());
                map.put("ticketStatus", t.getStatus() != null ? t.getStatus().name() : null);
                if (t.getOrderItem() != null) {
                    if (t.getOrderItem().getOrder() != null) {
                        map.put("customerEmail", t.getOrderItem().getOrder().getCustomerEmail());
                    }
                    if (t.getOrderItem().getTicketTier() != null) {
                        map.put("tierName", t.getOrderItem().getTicketTier().getName());
                    }
                }
            }
            return map;
        });

        return ResponseEntity.ok(responsePage);
    }

    @GetMapping(value = "/staff/checkins/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamCheckins(
            @RequestParam Long eventId) {
        return checkinSseService.subscribe(eventId);
    }
}

