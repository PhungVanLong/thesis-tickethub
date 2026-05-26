package ict.thesis.identity.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ict.thesis.identity.dto.AuthResponse;
import ict.thesis.identity.dto.LoginRequest;
import ict.thesis.identity.dto.RegisterRequest;
import ict.thesis.identity.dto.UpdateUserRequest;
import ict.thesis.identity.dto.UserResponse;
import ict.thesis.identity.entity.User;
import ict.thesis.identity.entity.enums.UserRole;
import ict.thesis.identity.exception.ConflictException;
import ict.thesis.identity.exception.UnauthorizedException;
import ict.thesis.identity.repository.UserRepository;
import ict.thesis.identity.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RestTemplate restTemplate;

    @Value("${management.sync-url:http://localhost:8082/api/ref-users/sync}")
    private String managementSyncUrl;

    @Transactional // Đảm bảo tính nguyên tử khi thao tác xuống Database vật lý
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists");
        }

        User user = User.builder()
                        .email(request.email())
                        .passwordHash(passwordEncoder.encode(request.password())) // Băm Bcrypt
                        .fullName(request.fullName())
                        .phone(request.phone())
                        .role(UserRole.CUSTOMER) // Mặc định tài khoản đăng ký mới là Khách hàng [cite: 17]
                        .verified(false)
                        .active(true)
                        .build();

        // Lưu vào DB để sinh ID tự tăng tự động trước khi cấp Token
        User savedUser = userRepository.save(user);
        logger.info("Registered user successfully with email: {}", savedUser.getEmail());
        syncManagement(savedUser);

        // CHỈNH SỬA CỐT LÕI: Subject truyền vào Token chuyển thành ID dạng String để API Gateway đọc hiểu
        String token = jwtService.generateToken(savedUser.getId().toString(), savedUser.getRole().name().toLowerCase());
        return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds());
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.email() != null && !request.email().isBlank() && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new ConflictException("Email already exists");
            }
            user.setEmail(request.email());
        }

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName());
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            user.setPhone(request.phone());
        }
        if (request.avatarUrl() != null && !request.avatarUrl().isBlank()) {
            user.setAvatarUrl(request.avatarUrl());
        }
        if (request.role() != null && !request.role().isBlank()) {
            try {
                user.setRole(UserRole.valueOf(request.role().trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
            }
        }
        if (request.verified() != null) {
            user.setVerified(request.verified());
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }

        User saved = userRepository.save(user);
        syncManagement(saved);
        return toUserResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            if (!authentication.isAuthenticated()) {
                throw new UnauthorizedException("Invalid credentials");
            }

            User user = userRepository.findByEmail(request.email())
                                      .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

            // CHỈNH SỬA CỐT LÕI: Đồng bộ Subject là User ID tương đương với hàm Register
            String token = jwtService.generateToken(user.getId().toString(), user.getRole().name().toLowerCase());
            return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds());
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException("Invalid credentials");
        }
    }

    private void syncManagement(User user) {
        try {
            UserResponse payload = toUserResponse(user);
            restTemplate.postForEntity(managementSyncUrl, payload, Void.class);
        } catch (RestClientException ex) {
            logger.warn("Failed to sync user {} to management: {}", user.getId(), ex.getMessage());
        }
    }

    private UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole() == null ? null : user.getRole().name());
        response.setVerified(user.isVerified());
        response.setActive(user.isActive());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}