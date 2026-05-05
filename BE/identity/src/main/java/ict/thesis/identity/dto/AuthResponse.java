package ict.thesis.identity.dto;

public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds
) {
}
