package ict.thesis.management.dto.response;

import java.time.Instant;

import ict.thesis.management.entity.enums.EventStatus;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateEventResponse {
    private Long id;
    private EventStatus status;
    private Instant createdAt;
    private Instant updatedAt;

}

