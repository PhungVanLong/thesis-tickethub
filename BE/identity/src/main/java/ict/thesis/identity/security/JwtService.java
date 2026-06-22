package ict.thesis.identity.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private final SecretKey secretKey;
    @Getter
    private final long expirationSeconds;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-seconds}") long expirationSeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(String userId, String role, String email, List<String> permissions) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
                   .subject(userId) // Chuẩn hóa Subject là User ID (Khóa ngoại logic hệ thống)
                   .issuedAt(Date.from(now))
                   .expiration(Date.from(expiresAt))
                   .claim("role", role) // Khớp cấu hình claims.get("role") ở API Gateway
                   .claim("email", email)
                   .claim("permissions", permissions)
                   .signWith(secretKey)
                   .compact();
    }

    // Đổi tên hàm để phản ánh đúng dữ liệu trích xuất (User ID dạng String)
    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    // Đổi tham số check từ username thành userId cho đồng bộ
    public boolean isTokenValid(String token, String userId) {
        Claims claims = parseClaims(token);
        return userId.equals(claims.getSubject()) && claims.getExpiration().after(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                   .verifyWith(secretKey)
                   .build()
                   .parseSignedClaims(token)
                   .getPayload(); // Cú pháp chuẩn của JJWT 0.12.x giúp đọc an toàn
    }
}