package ict.thesis.management.service;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import ict.thesis.management.dto.IdentityUserResponse;
import ict.thesis.management.dto.request.ApprovalRequest;
import ict.thesis.management.dto.response.EventApprovalResponse;
import ict.thesis.management.entity.EventApprovals;
import ict.thesis.management.entity.Events;
import ict.thesis.management.entity.RefUser;
import ict.thesis.management.entity.enums.ApprovalDecision;
import ict.thesis.management.entity.enums.EventStatus;
import ict.thesis.management.entity.enums.UserRole;
import ict.thesis.management.repository.EventApprovalsRepository;
import ict.thesis.management.repository.EventsRepository;
import ict.thesis.management.repository.RefUserRepository;

@Service
public class EventApprovalService {
    private final EventsRepository eventsRepository;
    private final EventApprovalsRepository eventApprovalsRepository;
    private final RefUserRepository refUserRepository;
    private final RefUserSyncService refUserSyncService;
    private final RestTemplate restTemplate;

    @Value("${identity.base-url:http://localhost:8081}")
    private String identityBase;

    public EventApprovalService(
        EventsRepository eventsRepository,
        EventApprovalsRepository eventApprovalsRepository,
        RefUserRepository refUserRepository,
        RefUserSyncService refUserSyncService,
        RestTemplate restTemplate
    ) {
        this.eventsRepository = eventsRepository;
        this.eventApprovalsRepository = eventApprovalsRepository;
        this.refUserRepository = refUserRepository;
        this.refUserSyncService = refUserSyncService;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public EventApprovalResponse approve(Long eventId, ApprovalRequest request) {
        if (request == null || request.adminUserId() == null) {
            throw new IllegalArgumentException("adminUserId is required");
        }
        if (request.decision() == null) {
            throw new IllegalArgumentException("decision is required");
        }

        Events event = eventsRepository.findById(eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        RefUser admin = refUserRepository.findById(request.adminUserId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));
        if (admin.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin user is required");
        }

        RefUser organizer = event.getRefUser();
        if (organizer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event organizer is missing");
        }

        EventApprovals approval = new EventApprovals();
        approval.setEvent(event);
        approval.setAdminUser(admin);
        approval.setDecision(request.decision());
        approval.setReason(request.reason());
        approval.setDecidedAt(Instant.now());

        Instant now = Instant.now();
        if (request.decision() == ApprovalDecision.APPROVED) {
            event.setStatus(EventStatus.APPROVED);
            event.setPublished(false);
            if (organizer.getRole() == UserRole.CUSTOMER) {
                IdentityUserResponse promoted = promoteOrganizer(organizer.getId());
                organizer = refUserSyncService.upsertFromIdentity(promoted);
            }
        } else {
            event.setStatus(EventStatus.CANCELLED);
            event.setPublished(false);
        }
        event.setUpdatedAt(now);

        Events savedEvent = eventsRepository.save(event);
        EventApprovals savedApproval = eventApprovalsRepository.save(approval);

        return new EventApprovalResponse(
            savedApproval.getId(),
            savedEvent.getId(),
            organizer.getId(),
            organizer.getRole() == null ? null : organizer.getRole().name(),
            admin.getId(),
            savedApproval.getDecision(),
            savedEvent.getStatus(),
            savedApproval.getReason(),
            savedApproval.getDecidedAt()
        );
    }

    private IdentityUserResponse promoteOrganizer(Long userId) {
        try {
            Map<String, Object> body = Map.of("role", "ORGANIZER");
            IdentityUserResponse response = restTemplate.exchange(
                identityBase + "/api/auth/users/{id}",
                HttpMethod.PUT,
                new HttpEntity<>(body),
                IdentityUserResponse.class,
                userId
            ).getBody();
            if (response == null) {
                throw new IllegalArgumentException("Unable to promote organizer in identity");
            }
            return response;
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("Unable to promote organizer in identity");
        }
    }
}

