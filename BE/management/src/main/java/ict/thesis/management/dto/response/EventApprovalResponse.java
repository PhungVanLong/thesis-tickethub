package ict.thesis.management.dto.response;

import java.time.Instant;

import ict.thesis.management.entity.enums.ApprovalDecision;
import ict.thesis.management.entity.enums.EventStatus;

public record EventApprovalResponse(
    Long approvalId,
    Long eventId,
    Long organizerId,
    String organizerRole,
    Long adminUserId,
    ApprovalDecision decision,
    EventStatus eventStatus,
    String reason,
    Instant decidedAt
) {
}

