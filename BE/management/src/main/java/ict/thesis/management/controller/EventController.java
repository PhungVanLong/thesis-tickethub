package ict.thesis.management.controller;

import ict.thesis.management.dto.request.CreateEventRequest;
import ict.thesis.management.dto.request.ApprovalRequest;
import ict.thesis.management.dto.response.CreateEventResponse;
import ict.thesis.management.dto.response.EventApprovalResponse;
import ict.thesis.management.security.UserContextHolder;
import ict.thesis.management.service.EventActionService;
import ict.thesis.management.service.EventCreationService;
import ict.thesis.management.service.EventQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ict.thesis.management.dto.response.EventResponse;
import ict.thesis.management.dto.response.EventDetailResponse;
import ict.thesis.management.entity.enums.EventStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventQueryService eventQueryService;
    private final EventCreationService eventCreationService;
    private final EventActionService eventActionService;

    public EventController(EventQueryService eventQueryService, 
                           EventCreationService eventCreationService, 
                           EventActionService eventActionService) {
        this.eventQueryService = eventQueryService;
        this.eventCreationService = eventCreationService;
        this.eventActionService = eventActionService;
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents(
        @RequestParam(required = false) EventStatus status
    ) {
        return ResponseEntity.ok(eventQueryService.getAllEvents(status));
    }

    @GetMapping("/discovery")
    public ResponseEntity<List<EventResponse>> getDiscoveryEvents(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String city,
        @RequestParam(required = false) String timeRange,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(eventQueryService.getDiscoveryEvents(category, city, timeRange, sortBy, limit));
    }

    @GetMapping("/organizer/my-events")
    public ResponseEntity<List<EventResponse>> getOrganizerEvents() {
        Long userId = UserContextHolder.getContext().getUserId();
        return ResponseEntity.ok(eventQueryService.getOrganizerEvents(userId));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailResponse> getEventDetail(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventQueryService.getEventDetail(eventId));
    }

    @GetMapping("/{eventId}/related")
    public ResponseEntity<List<EventResponse>> getRelatedEvents(
        @PathVariable Long eventId,
        @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(eventQueryService.getRelatedEvents(eventId, limit));
    }

    @PostMapping("/create")
    public ResponseEntity<CreateEventResponse> createEvent(
        @Valid @RequestBody CreateEventRequest request
    ) {
        Long userId = UserContextHolder.getContext().getUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(eventCreationService.createEvent(userId, request));
    }

    @PostMapping("/{eventId}/publish")
    public ResponseEntity<Map<String, String>> publishEvent(@PathVariable Long eventId) {
        Long userId = UserContextHolder.getContext().getUserId();
        eventActionService.publishEvent(userId, eventId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Event published successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{eventId}/cancel")
    public ResponseEntity<Map<String, String>> cancelEvent(@PathVariable Long eventId) {
        Long userId = UserContextHolder.getContext().getUserId();
        eventActionService.cancelEvent(userId, eventId);
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
        return ResponseEntity.ok(eventActionService.approveEvent(adminUserId, eventId, request));
    }
}
