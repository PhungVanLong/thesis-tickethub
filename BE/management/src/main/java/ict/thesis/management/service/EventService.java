package ict.thesis.management.service;

import ict.thesis.management.dto.request.CreateEventRequest;
import ict.thesis.management.dto.request.ApprovalRequest;
import ict.thesis.management.dto.response.CreateEventResponse;
import ict.thesis.management.dto.response.EventApprovalResponse;
import ict.thesis.management.entity.Events;
import ict.thesis.management.entity.EventApprovals;
import ict.thesis.management.entity.Organization;
import ict.thesis.management.entity.OrganizationMember;
import ict.thesis.management.entity.enums.EventStatus;
import ict.thesis.management.entity.enums.ApprovalDecision;
import ict.thesis.management.entity.enums.OrganizationRole;
import ict.thesis.management.entity.enums.OrganizationStatus;
import ict.thesis.management.repository.EventsRepository;
import ict.thesis.management.repository.OrganizationMemberRepository;
import ict.thesis.management.repository.EventApprovalsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class EventService {
    private final EventsRepository eventsRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final EventApprovalsRepository eventApprovalsRepository;

    public EventService(EventsRepository eventsRepository, 
                        OrganizationMemberRepository organizationMemberRepository,
                        EventApprovalsRepository eventApprovalsRepository) {
        this.eventsRepository = eventsRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.eventApprovalsRepository = eventApprovalsRepository;
    }

    @Transactional
    public CreateEventResponse createEvent(Long userId, CreateEventRequest request) {
        // Tìm thành viên tổ chức dựa trên organizationId và userId
        OrganizationMember member = organizationMemberRepository.findByOrganizationIdAndUserId(request.getOrganizationId(), userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "User is not a member of the specified organization"
            ));

        Organization organization = member.getOrganization();
        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, 
                "Organization is not active"
            );
        }

        Events event = new Events();
        event.setOrganization(organization);
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setVenue(request.getVenue());
        event.setCity(request.getCity());
        event.setLocationCoords(request.getLocationCoords());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setBannerUrl(request.getBannerUrl());
        event.setStatus(EventStatus.DRAFT);
        event.setPublished(false);

        Instant now = Instant.now();
        event.setCreatedAt(now);
        event.setUpdatedAt(now);

        Events saved = eventsRepository.save(event);
        CreateEventResponse response = new CreateEventResponse();
        response.setId(saved.getId());
        response.setStatus(saved.getStatus());
        response.setCreatedAt(saved.getCreatedAt());
        response.setUpdatedAt(saved.getUpdatedAt());
        return response;
    }

    @Transactional
    public EventApprovalResponse approveEvent(Long adminUserId, Long eventId, ApprovalRequest request) {
        if (request == null || adminUserId == null) {
            throw new IllegalArgumentException("request and adminUserId are required");
        }

        Events event = eventsRepository.findById(eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        EventApprovals approval = new EventApprovals();
        approval.setEvent(event);
        approval.setAdminUser(adminUserId);
        approval.setDecision(request.decision());
        approval.setReason(request.reason());
        
        Instant now = Instant.now();
        approval.setDecidedAt(now);

        if (request.decision() == ApprovalDecision.APPROVED) {
            event.setStatus(EventStatus.APPROVED);
        } else {
            event.setStatus(EventStatus.CANCELLED);
        }
        event.setUpdatedAt(now);

        eventsRepository.save(event);
        EventApprovals savedApproval = eventApprovalsRepository.save(approval);

        // Find owner of organization to map in response
        OrganizationMember ownerMember = organizationMemberRepository.findByOrganizationId(event.getOrganization().getId())
            .stream()
            .filter(m -> m.getMemberRole() == OrganizationRole.OWNER)
            .findFirst()
            .orElse(null);

        Long organizerId = (ownerMember != null) ? ownerMember.getUserId() : null;
        String organizerRole = (ownerMember != null) ? ownerMember.getMemberRole().name() : null;

        return new EventApprovalResponse(
            savedApproval.getId(),
            event.getId(),
            organizerId,
            organizerRole,
            adminUserId,
            savedApproval.getDecision(),
            event.getStatus(),
            savedApproval.getReason(),
            savedApproval.getDecidedAt()
        );
    }
}
