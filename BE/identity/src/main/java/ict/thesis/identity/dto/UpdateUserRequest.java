package ict.thesis.identity.dto;

public record UpdateUserRequest(
    String email,
    String fullName,
    String phone,
    String avatarUrl,
    String role,
    Boolean verified,
    Boolean active
) {
}

