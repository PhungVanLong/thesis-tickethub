package ict.thesis.management.dto.request;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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

    public Long getOrganizerId() {
        return organizerId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getVenue() {
        return venue;
    }

    public String getCity() {
        return city;
    }

    public String getLocationCoords() {
        return locationCoords;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

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
