package ict.thesis.management.controller;

import ict.thesis.management.dto.request.OrganizationRequest;
import ict.thesis.management.dto.request.OrganizationVerificationRequest;
import ict.thesis.management.dto.response.OrganizationResponse;
import ict.thesis.management.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {
    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> submitOrganization(
        @RequestHeader("X-User-Id") Long userId,
        @Valid @RequestBody OrganizationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.submitOrganization(userId, request));
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<OrganizationResponse> verifyOrganization(
        @PathVariable Long id,
        @RequestHeader("X-User-Id") Long adminUserId,
        @Valid @RequestBody OrganizationVerificationRequest request
    ) {
        return ResponseEntity.ok(organizationService.verifyOrganization(id, adminUserId, request));
    }
}
