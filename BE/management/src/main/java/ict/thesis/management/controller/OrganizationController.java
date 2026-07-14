package ict.thesis.management.controller;

import ict.thesis.management.dto.request.OrganizationRequest;
import ict.thesis.management.dto.request.OrganizationStaffAccountRequest;
import ict.thesis.management.dto.request.OrganizationVerificationRequest;
import ict.thesis.management.dto.response.OrganizationResponse;
import ict.thesis.management.dto.response.OrganizationStaffAccountResponse;
import ict.thesis.management.service.OrganizationService;
import ict.thesis.management.service.OrganizationStaffService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ict.thesis.management.entity.enums.OrganizationStatus;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {
    private final OrganizationService organizationService;
    private final OrganizationStaffService organizationStaffService;

    public OrganizationController(OrganizationService organizationService,
            OrganizationStaffService organizationStaffService) {
        this.organizationService = organizationService;
        this.organizationStaffService = organizationStaffService;
    }

    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> getAllOrganizations(
            @RequestParam(required = false) OrganizationStatus status) {
        return ResponseEntity.ok(organizationService.getAllOrganizations(status));
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> submitOrganization(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.submitOrganization(userId, request));
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<OrganizationResponse> verifyOrganization(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long adminUserId,
            @Valid @RequestBody OrganizationVerificationRequest request) {
        return ResponseEntity.ok(organizationService.verifyOrganization(id, adminUserId, request));
    }

    @PostMapping("/{id}/staff-accounts")
    public ResponseEntity<OrganizationStaffAccountResponse> createStaffAccount(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody OrganizationStaffAccountRequest request) {
        return ResponseEntity.accepted()
                .body(organizationStaffService.createStaffAccount(id, userId, request));
    }

}
