package ict.thesis.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ict.thesis.management.dto.request.OrganizationStaffAccountRequest;
import ict.thesis.management.dto.response.OrganizationStaffAccountResponse;
import ict.thesis.management.entity.EventStaff;
import ict.thesis.management.entity.Organization;
import ict.thesis.management.entity.OrganizationMember;
import ict.thesis.management.entity.Events;
import ict.thesis.management.entity.OutboxEvent;
import ict.thesis.management.entity.enums.OrganizationRole;
import ict.thesis.management.entity.enums.OrganizationStatus;
import ict.thesis.management.entity.enums.RoleEventStaff;
import ict.thesis.management.repository.EventStaffRepository;
import ict.thesis.management.repository.EventsRepository;
import ict.thesis.management.repository.OrganizationMemberRepository;
import ict.thesis.management.repository.OrganizationRepository;
import ict.thesis.management.repository.OutboxEventRepository;
import ict.thesis.management.entity.enums.OutboxStatus;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganizationStaffService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final EventsRepository eventsRepository;
    private final EventStaffRepository eventStaffRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrganizationStaffAccountResponse createStaffAccount(Long organizationId, Long requesterUserId,
            OrganizationStaffAccountRequest request) {
        if (organizationId == null) {
            throw new IllegalArgumentException("organizationId is required");
        }
        if (requesterUserId == null) {
            throw new IllegalArgumentException("requesterUserId is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("password is required");
        }
        if (request.fullName() == null || request.fullName().isBlank()) {
            throw new IllegalArgumentException("fullName is required");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

        OrganizationMember requesterMember = organizationMemberRepository
                .findByOrganizationIdAndUserId(organizationId, requesterUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You are not a member of this organization"));

        if (requesterMember.getMemberRole() != OrganizationRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the organization owner can create staff accounts");
        }

        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Organization must be ACTIVE before creating staff accounts");
        }

        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("organizationId", organizationId);
            payloadMap.put("requesterUserId", requesterUserId);
            payloadMap.put("email", request.email());
            payloadMap.put("password", request.password());
            payloadMap.put("fullName", request.fullName());
            payloadMap.put("phone", request.phone());

            String payloadJson = objectMapper.writeValueAsString(payloadMap);

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateType("Organization");
            outboxEvent.setAggregateId(organizationId);
            outboxEvent.setEventType("USER_STAFF_CREATE");
            outboxEvent.setPayload(payloadJson);
            outboxEvent.setStatus(OutboxStatus.PENDING);
            outboxEvent.setRetryCount(0);
            outboxEvent.setCreatedAt(Instant.now());

            OutboxEvent savedOutbox = outboxEventRepository.save(outboxEvent);

            OrganizationStaffAccountResponse response = new OrganizationStaffAccountResponse();
            response.setRequestId(savedOutbox.getId().toString());
            response.setRequestStatus("QUEUED");
            response.setOrganizationId(organization.getId());
            response.setOrganizationName(organization.getName());
            response.setEmail(request.email());
            response.setFullName(request.fullName());
            response.setRole("STAFF");
            response.setOrganizationRole(OrganizationRole.STAFF.name());
            return response;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create outbox event: " + e.getMessage(), e);
        }
    }
}