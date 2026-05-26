package ict.thesis.management.dto.request;

import ict.thesis.management.entity.enums.ApprovalDecision;

public record ApprovalRequest(
    Long adminUserId,
    ApprovalDecision decision,
    String reason
) {
}

