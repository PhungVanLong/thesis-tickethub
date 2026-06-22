package ict.thesis.identity.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Transactional // Đảm bảo tính nguyên tử khi thao tác xuống Database vật lý
    public AuthResponse register(RegisterRequest request) {
        // 1. Kiểm tra xem email đã tồn tại trong hệ thống chưa
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists");
        }

        // 2. Tạo đối tượng User mới với các thông tin mặc định
        User user = User.builder()
                        .email(request.email())
                        .passwordHash(passwordEncoder.encode(request.password())) // Băm Bcrypt để bảo mật mật khẩu
                        .fullName(request.fullName())
                        .phone(request.phone())
                        .role(UserRole.CUSTOMER) // Mặc định tài khoản đăng ký mới là Khách hàng
                        .verified(false)
                        .active(true)
                        .build();

        // 3. Lưu vào DB để sinh ID tự tăng tự động
        User savedUser = userRepository.save(user);
        logger.info("Registered user successfully with email: {}", savedUser.getEmail());

        // CHỈNH SỬA CỐT LÕI: Thêm email vào Token
        String token = jwtService.generateToken(
            savedUser.getId().toString(), 
            savedUser.getRole().name().toLowerCase(),
            savedUser.getEmail()
        );
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
        return toUserResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            // 1. Sử dụng Spring Security để xác thực email và password
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            if (!authentication.isAuthenticated()) {
                throw new UnauthorizedException("Invalid credentials");
            }

            // 2. Lấy thông tin chi tiết của User từ Database
            User user = userRepository.findByEmail(request.email())
                                      .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

            // 3. Sinh token JWT trả về cho client (Chỉ chứa ID, Role, Email)
            // Token này sẽ được client đính vào Header để gọi các API khác thông qua Gateway
            String token = jwtService.generateToken(
                user.getId().toString(), 
                user.getRole().name().toLowerCase(),
                user.getEmail()
            );
            return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds());
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException("Invalid credentials");
        }
    }

    // Đã thực thi chức năng sinh OTP cho quên mật khẩu
    public void forgotPassword(ict.thesis.identity.dto.ForgotPasswordRequest request) {
        // 1. Kiểm tra email có tồn tại không
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email không tồn tại trong hệ thống"));

        // 2. Sinh mã OTP (6 chữ số) và lưu vào DB, có hạn trong 15 phút
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        user.setResetToken(otp);
        user.setResetTokenExpiry(java.time.Instant.now().plus(15, java.time.temporal.ChronoUnit.MINUTES));
        userRepository.save(user);

        // 3. Gửi email chứa mã (Hiện tại đang Mock log ra console)
        // TODO: Tích hợp JavaMailSender để gửi email thật
        logger.info(">>> MOCK EMAIL: Mã OTP khôi phục mật khẩu của [{}] là: [{}] <<<", request.email(), otp);
    }

    // TODO: Phát triển tính năng đặt lại mật khẩu
    public void resetPassword(ict.thesis.identity.dto.ResetPasswordRequest request) {
        // 1. Xác thực OTP/Token
        // 2. Cập nhật mật khẩu mới (băm bằng PasswordEncoder) vào DB
        // 3. Vô hiệu hóa OTP/Token cũ
        throw new UnsupportedOperationException("Tính năng Đặt lại mật khẩu đang được phát triển (TODO)");
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