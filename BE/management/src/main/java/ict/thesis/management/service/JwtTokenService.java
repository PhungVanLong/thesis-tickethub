package ict.thesis.management.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class JwtTokenService {
    private static final List<String> USER_ID_CLAIMS = List.of("userId", "id", "sub", "uid");
    private static final List<String> ROLE_CLAIMS = List.of("role", "roles", "authorities");

    private final ObjectMapper objectMapper;

    public JwtTokenService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Long extractUserId(String authorizationHeader) {
        JsonNode claims = extractClaims(authorizationHeader);
        for (String claim : USER_ID_CLAIMS) {
            Long userId = readLong(claims.get(claim));
            if (userId != null) {
                return userId;
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT token does not contain a user id");
    }

    public String extractRole(String authorizationHeader) {
        JsonNode claims = extractClaims(authorizationHeader);
        for (String claim : ROLE_CLAIMS) {
            String role = readRole(claims.get(claim));
            if (role != null) {
                return role;
            }
        }

        String nestedRole = readRole(claims.path("realm_access").path("roles"));
        if (nestedRole != null) {
            return nestedRole;
        }

        return null;
    }

    private JsonNode extractClaims(String authorizationHeader) {
        String token = normalizeBearerToken(authorizationHeader);
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token");
        }

        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readTree(new String(payload, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException | IOException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token", ex);
        }
    }

    private String normalizeBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization bearer token is required");
        }

        String token = authorizationHeader.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }

        if (token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization bearer token is required");
        }

        return token;
    }

    private Long readLong(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isNumber()) {
            return node.longValue();
        }
        if (node.isTextual()) {
            try {
                return Long.parseLong(node.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String readRole(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isTextual()) {
            String role = node.asText().trim();
            return role.isEmpty() ? null : role.toUpperCase(Locale.ROOT);
        }
        if (node.isArray() && !node.isEmpty()) {
            return readRole(node.get(0));
        }
        return null;
    }
}

