package ict.thesis.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ict.thesis.management.dto.request.ApprovalRequest;
import ict.thesis.management.dto.response.EventApprovalResponse;
import ict.thesis.management.entity.EventApprovals;
import ict.thesis.management.entity.Events;
import ict.thesis.management.entity.OrganizationMember;
import ict.thesis.management.entity.OutboxEvent;
import ict.thesis.management.entity.Seat;
import ict.thesis.management.entity.SeatMap;
import ict.thesis.management.entity.TicketTier;
import ict.thesis.management.entity.enums.ApprovalDecision;
import ict.thesis.management.entity.enums.EventStatus;
import ict.thesis.management.entity.enums.OrganizationRole;
import ict.thesis.management.entity.enums.OutboxStatus;
import ict.thesis.management.repository.EventApprovalsRepository;
import ict.thesis.management.repository.EventsRepository;
import ict.thesis.management.repository.OrganizationMemberRepository;
import ict.thesis.management.repository.OutboxEventRepository;
import ict.thesis.management.repository.SeatMapRepository;
import ict.thesis.management.repository.SeatRepository;
import ict.thesis.management.repository.TicketTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventActionService {

    private final EventsRepository eventsRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final EventApprovalsRepository eventApprovalsRepository;
    private final SeatMapRepository seatMapRepository;
    private final TicketTierRepository ticketTierRepository;
    private final SeatRepository seatRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void publishEvent(Long userId, Long eventId) {
        Events event = eventsRepository.findById(eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        OrganizationMember member = organizationMemberRepository.findByOrganizationIdAndUserId(event.getOrganization().getId(), userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN, 
                "User is not a member of the organization"
            ));

        if (member.getMemberRole() != OrganizationRole.OWNER) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, 
                "Only the organization owner can publish events"
            );
        }

        if (event.getStatus() != EventStatus.APPROVED) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "Event must be approved before publishing"
            );
        }

        Instant now = Instant.now();
        event.setStatus(EventStatus.PUBLISHED);
        event.setPublished(true);
        event.setUpdatedAt(now);
        eventsRepository.save(event);

        // Create Outbox Event to sync with booking-service
        try {
            List<TicketTier> ticketTiers = ticketTierRepository.findByEventId(eventId);
            List<SeatMap> seatMaps = seatMapRepository.findByEventId(eventId);

            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("eventId", event.getId());
            payloadMap.put("title", event.getTitle());
            payloadMap.put("description", event.getDescription());
            payloadMap.put("venue", event.getVenue());
            payloadMap.put("city", event.getCity());
            payloadMap.put("locationCoords", event.getLocationCoords());
            payloadMap.put("startTime", event.getStartTime().toString());
            payloadMap.put("endTime", event.getEndTime().toString());
            payloadMap.put("bannerUrl", event.getBannerUrl());
            payloadMap.put("organizationId", event.getOrganization().getId());

            List<Map<String, Object>> tiersList = new ArrayList<>();
            for (TicketTier tier : ticketTiers) {
                Map<String, Object> tierMap = new HashMap<>();
                tierMap.put("id", tier.getId());
                tierMap.put("name", tier.getName());
                tierMap.put("tierType", tier.getTierType().name());
                tierMap.put("price", tier.getPrice());
                tierMap.put("quantityTotal", tier.getQuantityTotal());
                tierMap.put("quantityAvailable", tier.getQuantityAvailable());
                tiersList.add(tierMap);
            }
            payloadMap.put("ticketTiers", tiersList);

            List<Map<String, Object>> seatMapsList = new ArrayList<>();
            for (SeatMap sm : seatMaps) {
                Map<String, Object> smMap = new HashMap<>();
                smMap.put("id", sm.getId());
                smMap.put("name", sm.getName());
                smMap.put("totalRows", sm.getTotalRows());
                smMap.put("totalCols", sm.getTotalCols());
                smMap.put("layoutJson", sm.getLayoutJson());

                List<Seat> seats = seatRepository.findBySeatMapId(sm.getId());
                List<Map<String, Object>> seatsList = new ArrayList<>();
                for (Seat seat : seats) {
                    Map<String, Object> seatMapItem = new HashMap<>();
                    seatMapItem.put("id", seat.getId());
                    seatMapItem.put("seatCode", seat.getSeatCode());
                    seatMapItem.put("rowLabel", seat.getRowLabel());
                    seatMapItem.put("colNumber", seat.getColNumber());
                    seatMapItem.put("status", seat.getStatus().name());
                    seatMapItem.put("ticketTierId", seat.getTicketTier() != null ? seat.getTicketTier().getId() : null);
                    seatsList.add(seatMapItem);
                }
                smMap.put("seats", seatsList);
                seatMapsList.add(smMap);
            }
            payloadMap.put("seatMaps", seatMapsList);

            String payloadJson = objectMapper.writeValueAsString(payloadMap);

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateType("Event");
            outboxEvent.setAggregateId(event.getId());
            outboxEvent.setEventType("EVENT_PUBLISHED");
            outboxEvent.setPayload(payloadJson);
            outboxEvent.setStatus(OutboxStatus.PENDING);
            outboxEvent.setRetryCount(0);
            outboxEvent.setCreatedAt(now);

            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to serialize event payload for outbox", 
                e
            );
        }
    }

    @Transactional
    public void cancelEvent(Long userId, Long eventId) {
        Events event = eventsRepository.findById(eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        OrganizationMember member = organizationMemberRepository.findByOrganizationIdAndUserId(event.getOrganization().getId(), userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN, 
                "User is not a member of the organization"
            ));

        if (member.getMemberRole() != OrganizationRole.OWNER) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, 
                "Only the organization owner can cancel events"
            );
        }

        Instant now = Instant.now();
        event.setStatus(EventStatus.CANCELLED);
        event.setUpdatedAt(now);
        eventsRepository.save(event);
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
