package ict.thesis.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.Instant;

@Getter
public class CreateEventRequest {
    @NotNull(message = "organizerId is required")
    private Long organizerId;
    @NotBlank(message = "title is required")
    private String title;
    private String description;
    private String venue;
    private String city;
    private String locationCoords;
    @NotNull(message = "startTime is required")
    private Instant startTime;
    @NotNull(message = "endTime is required")
    private Instant endTime;
    private String bannerUrl;

    public void setOrganizerId(Long organizerId) {
        this.organizerId = organizerId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setLocationCoords(String locationCoords) {
        this.locationCoords = locationCoords;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }
}
