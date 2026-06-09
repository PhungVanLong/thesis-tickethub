// package ict.thesis.management.controller;

// import ict.thesis.management.dto.request.CreateEventRequest;
// import ict.thesis.management.dto.request.ApprovalRequest;
// import ict.thesis.management.dto.response.CreateEventResponse;
// import ict.thesis.management.dto.response.EventApprovalResponse;
// import ict.thesis.management.service.EventService;
// import ict.thesis.management.service.EventApprovalService;
// import jakarta.validation.Valid;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// @RestController
// @RequestMapping("/api/events")
// public class EventController {

//     private final EventService eventService;
//     private final EventApprovalService eventApprovalService;

//     public EventController(EventService eventService, EventApprovalService eventApprovalService) {
//         this.eventService = eventService;
//         this.eventApprovalService = eventApprovalService;
//     } // contructor injection


//     @PostMapping("/create")
//     public ResponseEntity<CreateEventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
//         CreateEventResponse response = eventService.createEvent(request);
//         return ResponseEntity.status(201).body(response);
//     }

//     @PostMapping("/{eventId}/approve")
//     public ResponseEntity<EventApprovalResponse> approveEvent(
//         @PathVariable Long eventId,
//         @Valid @RequestBody ApprovalRequest request
//     ) {
//         return ResponseEntity.ok(eventApprovalService.approve(eventId, request));
//     }


// }
