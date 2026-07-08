package ict.thesis.management.service;

import ict.thesis.management.dto.request.CreateEventRequest;
import ict.thesis.management.dto.request.ApprovalRequest;
import ict.thesis.management.dto.request.SeatMapRequest;
import ict.thesis.management.dto.request.TicketTierRequest;
import ict.thesis.management.dto.request.SeatRequest;
import ict.thesis.management.dto.response.CreateEventResponse;
import ict.thesis.management.dto.response.EventApprovalResponse;
import ict.thesis.management.dto.response.EventResponse;
import ict.thesis.management.entity.Events;
import ict.thesis.management.entity.EventApprovals;
import ict.thesis.management.entity.Organization;
import ict.thesis.management.entity.OrganizationMember;
import ict.thesis.management.entity.SeatMap;
import ict.thesis.management.entity.TicketTier;
import ict.thesis.management.entity.Seat;
import ict.thesis.management.entity.OutboxEvent;
import ict.thesis.management.entity.enums.EventStatus;
import ict.thesis.management.entity.enums.ApprovalDecision;
import ict.thesis.management.entity.enums.OrganizationRole;
import ict.thesis.management.entity.enums.OrganizationStatus;
import ict.thesis.management.entity.enums.SeatStatus;
import ict.thesis.management.entity.enums.OutboxStatus;
import ict.thesis.management.repository.EventsRepository;
import ict.thesis.management.repository.OrganizationMemberRepository;
import ict.thesis.management.repository.EventApprovalsRepository;
import ict.thesis.management.repository.SeatMapRepository;
import ict.thesis.management.repository.TicketTierRepository;
import ict.thesis.management.repository.SeatRepository;
import ict.thesis.management.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventService {
    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventsRepository eventsRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final EventApprovalsRepository eventApprovalsRepository;
    private final SeatMapRepository seatMapRepository;
    private final TicketTierRepository ticketTierRepository;
    private final SeatRepository seatRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

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

        // Chỉ OWNER được phép tạo sự kiện
        if (member.getMemberRole() != OrganizationRole.OWNER) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the organization owner is allowed to create events"
            );
        }

        // Validation thời gian
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

        // Validation địa điểm
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
        // Khởi tạo trạng thái PENDING, loại bỏ DRAFT
        event.setStatus(EventStatus.PENDING);
        event.setPublished(false);

        event.setCreatedAt(now);
        event.setUpdatedAt(now);

        Events savedEvent = eventsRepository.save(event);

        // Map để lưu trữ TicketTier Entity đã lưu nhằm phục vụ việc gán cho Seat
        Map<String, TicketTier> savedTiersMap = new HashMap<>();

        // 1. Lưu TicketTiers trước
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

        // 2. Lưu SeatMaps và các Seats thuộc về SeatMap đó
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

                // Lưu các ghế ngồi của SeatMap này
                if (smReq.getSeats() != null) {
                    for (SeatRequest seatReq : smReq.getSeats()) {
                        Seat seat = new Seat();
                        seat.setSeatMap(savedSeatMap);
                        seat.setSeatCode(seatReq.getSeatCode());
                        seat.setRowLabel(seatReq.getRowLabel());
                        seat.setColNumber(seatReq.getColNumber());
                        seat.setStatus(SeatStatus.AVAILABLE);

                        // Tìm TicketTier tương ứng theo tên hạng vé
                        TicketTier ticketTier = savedTiersMap.get(seatReq.getTicketTierName());
                        if (ticketTier == null) {
                            throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Ticket tier " + seatReq.getTicketTierName() + " not found in the ticket tiers list"
                            );
                        }
                        seat.setTicketTier(ticketTier);
                        seatRepository.save(seat);

                        // Thiết lập SeatMap cho TicketTier nếu chưa thiết lập
                        if (ticketTier.getSeatMap() == null) {
                            ticketTier.setSeatMap(savedSeatMap);
                            ticketTierRepository.save(ticketTier);
                        }
                    }
                }
            }
        }

        // Tạo Outbox Event cho Admin nhận biết sự kiện cần duyệt
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

        // Tạo Outbox Event đồng bộ sang booking-service
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

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents(EventStatus status) {
        List<Events> list;
        if (status == null) {
            list = eventsRepository.findAll();
        } else {
            list = eventsRepository.findByStatus(status);
        }
        return list.stream()
                   .map(this::toEventResponse)
                   .toList();
    }

    private EventResponse toEventResponse(Events event) {
        return new EventResponse(
            event.getId(),
            event.getOrganization() != null ? event.getOrganization().getId() : null,
            event.getOrganization() != null ? event.getOrganization().getName() : null,
            event.getTitle(),
            event.getDescription(),
            event.getVenue(),
            event.getCity(),
            event.getLocationCoords(),
            event.getStartTime(),
            event.getEndTime(),
            event.getBannerUrl(),
            event.getStatus(),
            event.isPublished(),
            event.getCreatedAt(),
            event.getUpdatedAt()
        );
    }
}
