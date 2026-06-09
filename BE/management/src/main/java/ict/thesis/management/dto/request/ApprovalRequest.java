package ict.thesis.management.dto.request;

import ict.thesis.management.entity.enums.ApprovalDecision;
import jakarta.validation.constraints.NotNull;

public record ApprovalRequest(
    @NotNull Long adminUserId,
    @NotNull ApprovalDecision decision,
    String reason
) {
}

