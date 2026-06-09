package ict.thesis.management.dto.response;

import java.time.Instant;

import ict.thesis.management.entity.enums.OrganizerStatus;

public record OrganizerProfileResponse(
    Long profileId,
    Long userId,
    String organizationName,
    OrganizerStatus status,
    Long verifiedByAdminId,
    Instant verifiedAt,
    String verificationReason,
    String userRole
) {
}

