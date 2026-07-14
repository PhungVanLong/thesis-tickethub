package ict.thesis.management.dto.event;

import java.util.List;

public record SeatStatusUpdateEvent(
    Long eventId,
    List<Long> seatIds,
    String status
) {}
