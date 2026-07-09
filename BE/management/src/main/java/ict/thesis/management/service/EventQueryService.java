package ict.thesis.management.service;

import ict.thesis.management.dto.response.EventDetailResponse;
import ict.thesis.management.dto.response.EventResponse;
import ict.thesis.management.dto.response.SeatMapResponse;
import ict.thesis.management.dto.response.SeatResponse;
import ict.thesis.management.dto.response.TicketTierResponse;
import ict.thesis.management.entity.Events;
import ict.thesis.management.entity.OrganizationMember;
import ict.thesis.management.entity.Seat;
import ict.thesis.management.entity.SeatMap;
import ict.thesis.management.entity.TicketTier;
import ict.thesis.management.entity.enums.EventStatus;
import ict.thesis.management.repository.EventsRepository;
import ict.thesis.management.repository.OrganizationMemberRepository;
import ict.thesis.management.repository.SeatMapRepository;
import ict.thesis.management.repository.SeatRepository;
import ict.thesis.management.repository.TicketTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventQueryService {

    private final EventsRepository eventsRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final TicketTierRepository ticketTierRepository;
    private final SeatMapRepository seatMapRepository;
    private final SeatRepository seatRepository;

    @Transactional(readOnly = true)
    public List<EventResponse> getOrganizerEvents(Long userId) {
        OrganizationMember member = organizationMemberRepository.findByUserId(userId).stream().findFirst()
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "User is not a member of any organization"
            ));

        Long orgId = member.getOrganization().getId();
        List<Events> list = eventsRepository.findByOrganizationId(orgId);
        return list.stream()
                   .map(this::toEventResponse)
                   .toList();
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

    @Transactional(readOnly = true)
    public EventDetailResponse getEventDetail(Long eventId) {
        Events event = eventsRepository.findById(eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        List<TicketTier> ticketTiers = ticketTierRepository.findByEventId(eventId);
        List<SeatMap> seatMaps = seatMapRepository.findByEventId(eventId);

        List<TicketTierResponse> tierResponses = ticketTiers.stream()
            .map(t -> new TicketTierResponse(
                t.getId(),
                t.getName(),
                t.getTierType(),
                t.getPrice(),
                t.getQuantityTotal(),
                t.getQuantityAvailable(),
                t.getQuantitySold(),
                t.getColorCode(),
                t.getSaleStart(),
                t.getSaleEnd(),
                t.getSeatMap() != null ? t.getSeatMap().getId() : null
            ))
            .toList();

        List<SeatMapResponse> seatMapResponses = seatMaps.stream()
            .map(sm -> {
                List<Seat> seats = seatRepository.findBySeatMapId(sm.getId());
                List<SeatResponse> seatResponses = seats.stream()
                    .map(s -> new SeatResponse(
                        s.getId(),
                        s.getSeatCode(),
                        s.getRowLabel(),
                        s.getColNumber(),
                        s.getStatus(),
                        s.getTicketTier() != null ? s.getTicketTier().getId() : null,
                        s.getTicketTier() != null ? s.getTicketTier().getName() : null,
                        s.getTicketTier() != null ? s.getTicketTier().getColorCode() : null
                    ))
                    .toList();

                return new SeatMapResponse(
                    sm.getId(),
                    eventId,
                    sm.getName(),
                    sm.getTotalRows(),
                    sm.getTotalCols(),
                    sm.getLayoutJson(),
                    sm.getCreatedAt(),
                    seatResponses
                );
            })
            .toList();

        return new EventDetailResponse(
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
            event.getUpdatedAt(),
            tierResponses,
            seatMapResponses
        );
    }

    public EventResponse toEventResponse(Events event) {
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
