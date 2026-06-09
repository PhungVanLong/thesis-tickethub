package ict.thesis.management.controller;

import ict.thesis.management.dto.request.OrganizerProfileRequest;
import ict.thesis.management.dto.request.OrganizerVerificationRequest;
import ict.thesis.management.dto.response.OrganizerProfileResponse;
import ict.thesis.management.service.OrganizerProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizers")
public class OrganizerProfileController {
    private final OrganizerProfileService organizerProfileService;

    public OrganizerProfileController(OrganizerProfileService organizerProfileService) {
        this.organizerProfileService = organizerProfileService;
    }

    @PostMapping("/profiles")
    public ResponseEntity<OrganizerProfileResponse> submitProfile(
        @Valid @RequestBody OrganizerProfileRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizerProfileService.submitProfile(request));
    }

    @PostMapping("/{userId}/verify")
    public ResponseEntity<OrganizerProfileResponse> verifyProfile(
        @PathVariable Long userId,
        @Valid @RequestBody OrganizerVerificationRequest request
    ) {
        return ResponseEntity.ok(organizerProfileService.verifyProfile(userId, request));
    }
}

