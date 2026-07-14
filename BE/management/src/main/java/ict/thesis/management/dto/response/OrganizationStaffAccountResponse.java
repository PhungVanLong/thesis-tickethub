package ict.thesis.management.dto.response;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizationStaffAccountResponse {
    private String requestId;
    private String requestStatus;
    private Long organizationId;
    private String organizationName;
    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private String organizationRole;
    private Instant assignedAt;
    private Instant createdAt;
    private Instant updatedAt;
}