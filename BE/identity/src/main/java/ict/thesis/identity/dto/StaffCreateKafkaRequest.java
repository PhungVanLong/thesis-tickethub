package ict.thesis.identity.dto;

public record StaffCreateKafkaRequest(
        Long organizationId,
        Long requesterUserId,
        String email,
        String password,
        String fullName,
        String phone) {
}