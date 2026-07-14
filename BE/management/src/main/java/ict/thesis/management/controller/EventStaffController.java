package ict.thesis.management.controller;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import ict.thesis.management.entity.EventStaff;
import ict.thesis.management.entity.Events;
import ict.thesis.management.entity.OrganizationMember;
import ict.thesis.management.entity.enums.RoleEventStaff;
import ict.thesis.management.repository.EventStaffRepository;
import ict.thesis.management.repository.EventsRepository;
import ict.thesis.management.repository.OrganizationMemberRepository;
import ict.thesis.management.dto.response.IdentityUserResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/events/{eventId}/staff")
@RequiredArgsConstructor
public class EventStaffController {

    private final EventStaffRepository eventStaffRepository;
    private final EventsRepository eventsRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final RestTemplate restTemplate;

    @Value("${identity.service.url}")
    private String identityServiceUrl;

    @Data
    public static class StaffAssignmentRequest {
        private String email;
        private String role;
    }

    @Data
    public static class StaffResponse {
        private Long id;
        private Long staffId;
        private String email;
        private String fullName;
        private String roleInEvent;
        private Instant assignedAt;
    }

    @GetMapping("/check")
    public ResponseEntity<Boolean> checkStaffAssignment(
            @PathVariable Long eventId,
            @RequestParam Long staffId) {
        boolean isAssigned = eventStaffRepository.existsByEventIdAndStaff(eventId, staffId);
        return ResponseEntity.ok(isAssigned);
    }

    @GetMapping
    public ResponseEntity<List<StaffResponse>> getEventStaff(@PathVariable Long eventId) {
        List<EventStaff> staffList = eventStaffRepository.findByEventId(eventId);
        List<StaffResponse> responseList = staffList.stream().map(s -> {
            StaffResponse r = new StaffResponse();
            r.setId(s.getId());
            r.setStaffId(s.getStaff());
            r.setRoleInEvent(s.getRoleInEvent().name());
            r.setAssignedAt(s.getAssignedAt());

            // Query user details from identity service
            try {
                IdentityUserResponse user = restTemplate.getForObject(
                        identityServiceUrl + "/api/users/" + s.getStaff(),
                        IdentityUserResponse.class);
                if (user != null) {
                    r.setEmail(user.getEmail());
                    r.setFullName(user.getFullName());
                }
            } catch (Exception e) {
                r.setEmail("unknown@tickethub.vn");
                r.setFullName("Unknown Staff");
            }
            return r;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    @PostMapping
    public ResponseEntity<StaffResponse> assignStaff(
            @PathVariable Long eventId,
            @RequestBody StaffAssignmentRequest request) {

        Events event = eventsRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        // 1. Look up user by email in identity service
        IdentityUserResponse user;
        try {
            user = restTemplate.getForObject(
                    identityServiceUrl + "/api/users/by-email?email=" + request.getEmail(),
                    IdentityUserResponse.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User with this email not found in system");
        }

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        if (user.getRole() == null || !user.getRole().toUpperCase().contains("STAFF")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a staff account");
        }

        OrganizationMember organizationMember = organizationMemberRepository.findByOrganizationIdAndUserId(
                event.getOrganization().getId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Staff does not belong to this organization"));

        // 2. Check if already assigned
        if (eventStaffRepository.existsByEventIdAndStaff(eventId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff is already assigned to this event");
        }

        // 3. Create assignment
        EventStaff staff = new EventStaff();
        staff.setEvent(event);
        staff.setStaff(user.getId());

        RoleEventStaff roleEnum;
        try {
            roleEnum = RoleEventStaff.valueOf(request.getRole().toUpperCase());
        } catch (Exception e) {
            roleEnum = RoleEventStaff.CHECKIN_STAFF;
        }
        staff.setRoleInEvent(roleEnum);
        staff.setAssignedAt(Instant.now());

        EventStaff saved = eventStaffRepository.save(staff);

        StaffResponse response = new StaffResponse();
        response.setId(saved.getId());
        response.setStaffId(saved.getStaff());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRoleInEvent(saved.getRoleInEvent().name());
        response.setAssignedAt(saved.getAssignedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{staffId}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> removeStaff(
            @PathVariable Long eventId,
            @PathVariable Long staffId) {
        eventStaffRepository.deleteByEventIdAndStaff(eventId, staffId);
        return ResponseEntity.noContent().build();
    }
}
