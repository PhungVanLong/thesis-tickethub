package ict.thesis.management.controller;

import ict.thesis.management.dto.request.CreateEventRequest;
import ict.thesis.management.dto.request.ApprovalRequest;
import ict.thesis.management.dto.response.CreateEventResponse;
import ict.thesis.management.dto.response.EventApprovalResponse;
import ict.thesis.management.security.UserContextHolder;
import ict.thesis.management.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ict.thesis.management.dto.response.EventResponse;
import ict.thesis.management.entity.enums.EventStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents(
        @RequestParam(required = false) EventStatus status
    ) {
        return ResponseEntity.ok(eventService.getAllEvents(status));
    }

    @PostMapping("/create")
    public ResponseEntity<CreateEventResponse> createEvent(
        @Valid @RequestBody CreateEventRequest request
    ) {
        Long userId = UserContextHolder.getContext().getUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(userId, request));
    }

    @PostMapping("/{eventId}/publish")
    public ResponseEntity<Map<String, String>> publishEvent(@PathVariable Long eventId) {
        Long userId = UserContextHolder.getContext().getUserId();
        eventService.publishEvent(userId, eventId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Event published successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{eventId}/cancel")
    public ResponseEntity<Map<String, String>> cancelEvent(@PathVariable Long eventId) {
        Long userId = UserContextHolder.getContext().getUserId();
        eventService.cancelEvent(userId, eventId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Event cancelled successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{eventId}/approve")
    public ResponseEntity<EventApprovalResponse> approveEvent(
        @PathVariable Long eventId,
        @Valid @RequestBody ApprovalRequest request
    ) {
        Long adminUserId = UserContextHolder.getContext().getUserId();
        return ResponseEntity.ok(eventService.approveEvent(adminUserId, eventId, request));
    }
}
