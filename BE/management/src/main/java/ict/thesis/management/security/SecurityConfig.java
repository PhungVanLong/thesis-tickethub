package ict.thesis.management.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final UserContextFilter userContextFilter;

    public SecurityConfig(UserContextFilter userContextFilter) {
        this.userContextFilter = userContextFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(userContextFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Allow public paths (Swagger, API Docs, Actuator, Error)
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**",
                                "/actuator/**", "/error")
                        .permitAll()

                        // Organizations
                        .requestMatchers(HttpMethod.POST, "/api/organizations").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/organizations/*/staff-accounts")
                        .hasAuthority("ORGANIZER")
                        .requestMatchers(HttpMethod.POST, "/api/organizations/*/verify").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/organizations").hasAuthority("ADMIN")

                        // Events
                        .requestMatchers(HttpMethod.GET, "/api/events/organizer/**").hasAuthority("ORGANIZER")
                        .requestMatchers(HttpMethod.POST, "/api/events/create").hasAnyAuthority("ORGANIZER", "STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/events/*/publish", "/api/events/*/cancel")
                        .hasAnyAuthority("ORGANIZER", "STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/events/*/approve").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()

                        .anyRequest().authenticated())
                .build();
    }
}
