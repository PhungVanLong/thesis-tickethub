package ict.thesis.api_gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

@Component
public class JwtGlobalAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String secretString;

    @Value("${gateway.security.public-paths:/api/auth/login,/api/auth/register,/api/movies/**,/swagger-ui.html,/swagger-ui/**,/v3/api-docs/**,/api-docs/**,/actuator/health,/actuator/info}")
    private String publicPathsCsv;

    private SecretKey secretKey;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Apply JWT check only for requests that actually require authentication.
        // Public endpoints pass through, internal endpoints must present a valid token.
        String path = request.getURI().getPath();
        if (isPublicPath(path) || HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }

        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return unauthorized(exchange.getResponse(), HttpStatus.UNAUTHORIZED);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange.getResponse(), HttpStatus.UNAUTHORIZED);
        }

        String jwtToken = authHeader.substring(7);
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(jwtToken)
                    .getBody();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId == null ? "" : userId)
                    .header("X-User-Role", role == null ? "" : role)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            return unauthorized(exchange.getResponse(), HttpStatus.UNAUTHORIZED);
        }
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, HttpStatus status) {
        response.setStatusCode(status);
        return response.setComplete();
    }

    private boolean isPublicPath(String path) {
        return Arrays.stream(publicPathsCsv.split(","))
            .map(String::trim)
            .filter(pattern -> !pattern.isBlank())
            .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    public int getOrder() {
        return -100; // run early
    }
}
