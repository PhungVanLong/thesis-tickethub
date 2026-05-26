package ict.thesis.api_gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

class JwtGlobalAuthenticationFilterTest {

    private JwtGlobalAuthenticationFilter filter;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        filter = new JwtGlobalAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "secretString", "change-this-secret-key-please-change-very-long");
        ReflectionTestUtils.setField(filter, "publicPathsCsv", "/api/auth/login,/api/auth/register,/api/movies/**,/swagger-ui.html,/swagger-ui/**,/v3/api-docs/**,/api-docs/**");
        filter.init();
        secretKey = Keys.hmacShaKeyFor("change-this-secret-key-please-change-very-long".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void publicPath_shouldPassWithoutToken() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/auth/register").build());

        filter.filter(exchange, chain).block();

        ArgumentCaptor<org.springframework.web.server.ServerWebExchange> captor = ArgumentCaptor.forClass(org.springframework.web.server.ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertNull(captor.getValue().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void protectedPath_withValidToken_shouldAddHeaders() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        String token = Jwts.builder()
            .setSubject("42")
            .claim("role", "CUSTOMER")
            .setExpiration(new Date(System.currentTimeMillis() + 60_000))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/events/create")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build()
        );

        filter.filter(exchange, chain).block();

        ArgumentCaptor<org.springframework.web.server.ServerWebExchange> captor = ArgumentCaptor.forClass(org.springframework.web.server.ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertEquals("42", captor.getValue().getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("CUSTOMER", captor.getValue().getRequest().getHeaders().getFirst("X-User-Role"));
    }

    @Test
    void protectedPath_withExpiredToken_shouldReturn401() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        String token = Jwts.builder()
            .setSubject("42")
            .claim("role", "CUSTOMER")
            .setExpiration(new Date(System.currentTimeMillis() - 60_000))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/events/create")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build()
        );

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }
}

