package ict.thesis.management.dto.request;

import jakarta.validation.constraints.NotNull;
import ict.thesis.management.entity.enums.OrganizationStatus;

public record OrganizationVerificationRequest(
    @NotNull Long adminUserId,
    @NotNull OrganizationStatus decision,
    String reason
) {
}
