package ict.thesis.management.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class UserContextFilter extends OncePerRequestFilter {

    @Value("${gateway.shared-secret}")
    private String gatewaySharedSecret;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> EXCLUDE_PATHS = List.of(
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/api-docs/**",
        "/actuator/**"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. Skip gateway verification for documentation and management endpoints
        if (shouldExclude(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Verify gateway shared token to prevent gateway bypass
        String gatewayToken = request.getHeader("X-Gateway-Token");
        if (gatewayToken == null || !gatewayToken.equals(gatewaySharedSecret)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Direct access is prohibited!");
            return;
        }

        // 3. Extract user context headers from Gateway
        String userIdStr = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");
        String email = request.getHeader("X-User-Email");

        try {
            if (userIdStr != null && !userIdStr.isBlank()) {
                Long userId = Long.valueOf(userIdStr);

                // Build local UserContext and set in ThreadLocal
                UserContext context = new UserContext();
                context.setUserId(userId);
                context.setRole(role);
                context.setEmail(email);
                UserContextHolder.setContext(context);

                // Parse roles and create SimpleGrantedAuthorities (no ROLE_ prefix)
                List<SimpleGrantedAuthority> authorities = Collections.emptyList();
                if (role != null && !role.isBlank()) {
                    authorities = Arrays.stream(role.split(","))
                            .map(String::trim)
                            .filter(r -> !r.isEmpty())
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                }

                // Set Spring Security Authentication
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        context, null, authorities
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            filterChain.doFilter(request, response);

        } finally {
            // Always clean up thread local contexts to prevent thread leaks in thread pool
            UserContextHolder.clearContext();
        }
    }

    private boolean shouldExclude(String path) {
        return EXCLUDE_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}
