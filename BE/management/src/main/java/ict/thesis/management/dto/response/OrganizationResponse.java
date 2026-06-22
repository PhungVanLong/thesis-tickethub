package ict.thesis.management.dto.response;

import java.time.Instant;
import ict.thesis.management.entity.enums.OrganizationStatus;

public record OrganizationResponse(
    Long id,
    String name,
    String abbreviationName,
    String taxCode,
    String representativeName,
    String representativePosition,
    String hotline,
    String officialEmail,
    String provinceCity,
    String district,
    String wardCommune,
    String headquarterAddress,
    String websiteUrl,
    String fanpageUrl,
    String description,
    OrganizationStatus status,
    Long verifiedByAdminId,
    Instant verifiedAt,
    String verificationReason,
    Instant syncedAt
) {
}
