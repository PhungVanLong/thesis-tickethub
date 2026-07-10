package ict.thesis.management.dto.request;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateEventRequest {
    @NotNull(message = "organizationId is required")
    private Long organizationId;

    @NotBlank(message = "title is required")
    private String title;

    private String description;

    @NotBlank(message = "venue is required")
    private String venue;

    @NotBlank(message = "city is required")
    private String city;

    private String locationCoords;

    @NotNull(message = "startTime is required")
    private Instant startTime;

    @NotNull(message = "endTime is required")
    private Instant endTime;

    private String bannerUrl;

    @Valid
    private List<TicketTierRequest> ticketTiers;

    @Valid
    private List<SeatMapRequest> seatMaps;
}
