package ict.thesis.management.service;

import ict.thesis.management.dto.response.EventDetailResponse;
import ict.thesis.management.dto.response.EventResponse;
import ict.thesis.management.dto.response.SeatMapResponse;
import ict.thesis.management.dto.response.SeatResponse;
import ict.thesis.management.dto.response.TicketTierResponse;
import ict.thesis.management.dto.response.IdentityUserResponse;
import ict.thesis.management.entity.Events;
import ict.thesis.management.entity.Organization;
import ict.thesis.management.entity.OrganizationMember;
import ict.thesis.management.entity.Seat;
import ict.thesis.management.entity.SeatMap;
import ict.thesis.management.entity.TicketTier;
import ict.thesis.management.entity.enums.EventStatus;
import ict.thesis.management.entity.enums.OrganizationRole;
import ict.thesis.management.repository.EventsRepository;
import ict.thesis.management.repository.OrganizationMemberRepository;
import ict.thesis.management.repository.SeatMapRepository;
import ict.thesis.management.repository.SeatRepository;
import ict.thesis.management.repository.TicketTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
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
    private final RestTemplate restTemplate;

    @Value("${identity.service.url}")
    private String identityServiceUrl;

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
    public List<EventResponse> getDiscoveryEvents(
            String category,
            String city,
            String timeRange,
            String sortBy,
            Integer limit) {
        
        EventStatus status = EventStatus.PUBLISHED; // Mặc định cho người dùng xem trên trang chủ
        
        java.time.Instant startTime = null;
        java.time.Instant endTime = null;
        
        java.time.Instant now = java.time.Instant.now();
        
        if ("WEEKEND".equalsIgnoreCase(timeRange)) {
            // Tính toán cuối tuần (Thứ Sáu, Thứ Bảy, Chủ Nhật) của tuần hiện tại
            java.time.ZonedDateTime nowZoned = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
            
            java.time.ZonedDateTime friday = nowZoned.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.FRIDAY))
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
            
            if (nowZoned.getDayOfWeek() == java.time.DayOfWeek.SATURDAY) {
                friday = friday.minusDays(1);
            } else if (nowZoned.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                friday = friday.minusDays(2);
            }
            
            java.time.ZonedDateTime sundayEnd = friday.plusDays(2)
                    .withHour(23).withMinute(59).withSecond(59).withNano(999999999);
            
            startTime = friday.toInstant();
            endTime = sundayEnd.toInstant();
        } else if ("MONTH".equalsIgnoreCase(timeRange)) {
            // Từ bây giờ đến cuối tháng này
            java.time.ZonedDateTime nowZoned = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
            java.time.ZonedDateTime endOfMonth = nowZoned.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth())
                    .withHour(23).withMinute(59).withSecond(59).withNano(999999999);
            
            startTime = now.isBefore(endOfMonth.toInstant()) ? now : endOfMonth.toInstant();
            endTime = endOfMonth.toInstant();
        }
        
        boolean sortByTrending = "TRENDING".equalsIgnoreCase(sortBy);
        
        List<Events> list;
        if (sortByTrending) {
            list = eventsRepository.findEventsTrending(status, category, city, startTime, endTime);
        } else {
            list = eventsRepository.findEventsChronological(status, category, city, startTime, endTime);
        }
        
        int limitVal = (limit != null && limit > 0) ? limit : 10;
        
        return list.stream()
                   .limit(limitVal)
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

        Organization org = event.getOrganization();
        String orgAbbrev = org != null ? org.getAbbreviationName() : null;
        String orgEmail = org != null ? org.getOfficialEmail() : null;
        String orgHotline = org != null ? org.getHotline() : null;

        Long creatorId = null;
        String creatorEmail = null;
        String creatorName = null;

        if (org != null) {
            OrganizationMember ownerMember = organizationMemberRepository.findByOrganizationId(org.getId())
                .stream()
                .filter(m -> m.getMemberRole() == OrganizationRole.OWNER)
                .findFirst()
                .orElse(null);

            if (ownerMember != null) {
                creatorId = ownerMember.getUserId();
                try {
                    IdentityUserResponse user = restTemplate.getForObject(
                        identityServiceUrl + "/api/users/" + creatorId, 
                        IdentityUserResponse.class
                    );
                    if (user != null) {
                        creatorEmail = user.getEmail();
                        creatorName = user.getFullName();
                    }
                } catch (Exception e) {
                    // Log and ignore to prevent failure of entire API if identity service is down
                }
            }
        }

        return new EventDetailResponse(
            event.getId(),
            org != null ? org.getId() : null,
            org != null ? org.getName() : null,
            orgAbbrev,
            orgEmail,
            orgHotline,
            org != null ? org.getDescription() : null,
            org != null ? org.getHeadquarterAddress() : null,
            org != null ? org.getWebsiteUrl() : null,
            creatorId,
            creatorEmail,
            creatorName,
            event.getTitle(),
            event.getDescription(),
            event.getVenue(),
            event.getCity(),
            event.getLocationCoords(),
            event.getStartTime(),
            event.getEndTime(),
            event.getBannerUrl(),
            event.getCategory(),
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
            event.getCategory(),
            event.getStatus(),
            event.isPublished(),
            event.getCreatedAt(),
            event.getUpdatedAt()
        );
    }
}
