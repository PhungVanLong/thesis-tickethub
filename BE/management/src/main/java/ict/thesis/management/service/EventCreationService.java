package ict.thesis.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ict.thesis.management.dto.request.CreateEventRequest;
import ict.thesis.management.dto.request.SeatMapRequest;
import ict.thesis.management.dto.request.SeatRequest;
import ict.thesis.management.dto.request.TicketTierRequest;
import ict.thesis.management.dto.response.CreateEventResponse;
import ict.thesis.management.entity.Events;
import ict.thesis.management.entity.Organization;
import ict.thesis.management.entity.OrganizationMember;
import ict.thesis.management.entity.OutboxEvent;
import ict.thesis.management.entity.Seat;
import ict.thesis.management.entity.SeatMap;
import ict.thesis.management.entity.TicketTier;
import ict.thesis.management.entity.enums.EventStatus;
import ict.thesis.management.entity.enums.OrganizationRole;
import ict.thesis.management.entity.enums.OrganizationStatus;
import ict.thesis.management.entity.enums.OutboxStatus;
import ict.thesis.management.entity.enums.SeatStatus;
import ict.thesis.management.repository.EventsRepository;
import ict.thesis.management.repository.OrganizationMemberRepository;
import ict.thesis.management.repository.OutboxEventRepository;
import ict.thesis.management.repository.SeatMapRepository;
import ict.thesis.management.repository.SeatRepository;
import ict.thesis.management.repository.TicketTierRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventCreationService {

    private static final Logger log = LoggerFactory.getLogger(EventCreationService.class);

    private final EventsRepository eventsRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final SeatMapRepository seatMapRepository;
    private final TicketTierRepository ticketTierRepository;
    private final SeatRepository seatRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @Transactional
    public CreateEventResponse createEvent(Long userId, CreateEventRequest request) {
        // Auto find user organization
        OrganizationMember member = organizationMemberRepository.findByUserId(userId).stream().findFirst()
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "User is not a member of any organization"
            ));

        Organization organization = member.getOrganization();
        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, 
                "Organization is not active"
            );
        }

        // Only OWNER can create events
        if (member.getMemberRole() != OrganizationRole.OWNER) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the organization owner is allowed to create events"
            );
        }

        // Validate time
        Instant now = Instant.now();
        if (request.getStartTime() == null || request.getStartTime().isBefore(now)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Event start time must be in the future"
            );
        }
        if (request.getEndTime() == null || request.getEndTime().isBefore(request.getStartTime())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Event end time must be after start time"
            );
        }

        // Validate location
        if (request.getVenue() == null || request.getVenue().trim().isEmpty() ||
            request.getCity() == null || request.getCity().trim().isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Event venue and city are required"
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
        event.setCategory(request.getCategory());
        
        // Set initial status to PENDING
        event.setStatus(EventStatus.PENDING);
        event.setPublished(false);

        event.setCreatedAt(now);
        event.setUpdatedAt(now);

        Events savedEvent = eventsRepository.save(event);

        // Notify Admins
        try {
            notificationService.createNotification(
                null, 
                "ADMIN", 
                "Sự kiện mới đăng ký", 
                "Sự kiện '" + savedEvent.getTitle() + "' đã được đăng ký bởi ban tổ chức '" + organization.getName() + "' và đang chờ duyệt.", 
                savedEvent.getId()
            );
        } catch (Exception e) {
            log.error("Failed to create admin notification for event creation", e);
        }

        // Map to store saved TicketTier entities for Seat assignment
        Map<String, TicketTier> savedTiersMap = new HashMap<>();

        // 1. Save TicketTiers first
        if (request.getTicketTiers() != null) {
            for (TicketTierRequest ttReq : request.getTicketTiers()) {
                TicketTier ticketTier = new TicketTier();
                ticketTier.setEvent(savedEvent);
                ticketTier.setName(ttReq.getName());
                ticketTier.setTierType(ttReq.getTierType());
                ticketTier.setPrice(ttReq.getPrice());
                ticketTier.setQuantityTotal(ttReq.getQuantityTotal());
                ticketTier.setQuantityAvailable(ttReq.getQuantityTotal());
                ticketTier.setQuantitySold(0);
                ticketTier.setColorCode(ttReq.getColorCode());
                ticketTier.setSaleStart(ttReq.getSaleStart());
                ticketTier.setSaleEnd(ttReq.getSaleEnd());

                TicketTier savedTier = ticketTierRepository.save(ticketTier);
                savedTiersMap.put(savedTier.getName(), savedTier);
            }
        }

        // 2. Save SeatMaps and their Seats
        if (request.getSeatMaps() != null) {
            for (SeatMapRequest smReq : request.getSeatMaps()) {
                SeatMap seatMap = new SeatMap();
                seatMap.setEvent(savedEvent);
                seatMap.setName(smReq.getName());
                seatMap.setTotalRows(smReq.getTotalRows());
                seatMap.setTotalCols(smReq.getTotalCols());
                seatMap.setLayoutJson(smReq.getLayoutJson());
                seatMap.setCreatedAt(now);

                SeatMap savedSeatMap = seatMapRepository.save(seatMap);

                // Save seats for this SeatMap
                if (smReq.getSeats() != null) {
                    for (SeatRequest seatReq : smReq.getSeats()) {
                        Seat seat = new Seat();
                        seat.setSeatMap(savedSeatMap);
                        seat.setSeatCode(seatReq.getSeatCode());
                        seat.setRowLabel(seatReq.getRowLabel());
                        seat.setColNumber(seatReq.getColNumber());
                        seat.setStatus(SeatStatus.AVAILABLE);

                        // Find corresponding TicketTier by name
                        TicketTier ticketTier = savedTiersMap.get(seatReq.getTicketTierName());
                        if (ticketTier == null) {
                            throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Ticket tier " + seatReq.getTicketTierName() + " not found in the ticket tiers list"
                            );
                        }
                        seat.setTicketTier(ticketTier);
                        seatRepository.save(seat);

                        // Set SeatMap for TicketTier if not set
                        if (ticketTier.getSeatMap() == null) {
                            ticketTier.setSeatMap(savedSeatMap);
                            ticketTierRepository.save(ticketTier);
                        }
                    }
                }
            }
        }

        // Create Outbox Event to notify Admin
        try {
            Map<String, Object> notifyPayload = new HashMap<>();
            notifyPayload.put("eventId", savedEvent.getId());
            notifyPayload.put("title", savedEvent.getTitle());
            notifyPayload.put("organizationId", savedEvent.getOrganization().getId());
            notifyPayload.put("organizerId", userId);
            notifyPayload.put("status", savedEvent.getStatus().name());
            notifyPayload.put("createdAt", savedEvent.getCreatedAt().toString());

            String payloadJson = objectMapper.writeValueAsString(notifyPayload);

            OutboxEvent notifyEvent = new OutboxEvent();
            notifyEvent.setAggregateType("Event");
            notifyEvent.setAggregateId(savedEvent.getId());
            notifyEvent.setEventType("EVENT_PENDING");
            notifyEvent.setPayload(payloadJson);
            notifyEvent.setStatus(OutboxStatus.PENDING);
            notifyEvent.setRetryCount(0);
            notifyEvent.setCreatedAt(now);

            outboxEventRepository.save(notifyEvent);
        } catch (Exception e) {
            log.error("Failed to serialize notify admin payload for outbox", e);
        }

        CreateEventResponse response = new CreateEventResponse();
        response.setId(savedEvent.getId());
        response.setStatus(savedEvent.getStatus());
        response.setCreatedAt(savedEvent.getCreatedAt());
        response.setUpdatedAt(savedEvent.getUpdatedAt());
        return response;
    }
}
