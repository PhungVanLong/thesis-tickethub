package ict.thesis.management.controller;

import ict.thesis.management.dto.request.CreateEventRequest;
import ict.thesis.management.dto.response.CreateEventResponse;
import ict.thesis.management.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    } // contructor injection


    @PostMapping("/create")
    public ResponseEntity<CreateEventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        CreateEventResponse response = eventService.createEvent(request);
        return ResponseEntity.status(201).body(response);
    }


}
