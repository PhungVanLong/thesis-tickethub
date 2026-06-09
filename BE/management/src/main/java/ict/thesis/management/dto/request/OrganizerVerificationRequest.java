package ict.thesis.management.dto.request;

import jakarta.validation.constraints.NotNull;

import ict.thesis.management.entity.enums.OrganizerStatus;

public record OrganizerVerificationRequest(
    @NotNull Long adminUserId,
    @NotNull OrganizerStatus decision,
    String reason
) {
}

