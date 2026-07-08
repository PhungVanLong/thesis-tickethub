package ict.thesis.management.dto.response;

import java.time.Instant;
import ict.thesis.management.entity.enums.EventStatus;

public record EventResponse(
    Long id,
    Long organizationId,
    String organizationName,
    String title,
    String description,
    String venue,
    String city,
    String locationCoords,
    Instant startTime,
    Instant endTime,
    String bannerUrl,
    EventStatus status,
    boolean isPublished,
    Instant createdAt,
    Instant updatedAt
) {
}
