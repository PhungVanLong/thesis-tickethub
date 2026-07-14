package ict.thesis.management.dto.response;

import ict.thesis.management.entity.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventDetailResponse {
    private Long id;
    private Long organizationId;
    private String organizationName;
    private String organizationAbbreviation;
    private String organizationEmail;
    private String organizationHotline;
    private String organizationDescription;
    private String organizationAddress;
    private String organizationWebsite;
    private Long creatorId;
    private String creatorEmail;
    private String creatorName;
    private String title;
    private String description;
    private String venue;
    private String city;
    private String locationCoords;
    private Instant startTime;
    private Instant endTime;
    private String bannerUrl;
    private String category;
    private EventStatus status;
    private boolean isPublished;
    private Instant createdAt;
    private Instant updatedAt;
    private List<TicketTierResponse> ticketTiers;
    private List<SeatMapResponse> seatMaps;
}
