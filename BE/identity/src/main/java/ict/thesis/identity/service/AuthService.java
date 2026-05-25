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
import ict.thesis.identity.entity.User;
import ict.thesis.identity.entity.enums.UserRole;
import ict.thesis.identity.exception.ConflictException;
import ict.thesis.identity.exception.UnauthorizedException;
import ict.thesis.identity.repository.UserRepository;
import ict.thesis.identity.security.JwtService;
import lombok.RequiredArgsConstructor;

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

        // CHỈNH SỬA CỐT LÕI: Subject truyền vào Token chuyển thành ID dạng String để API Gateway đọc hiểu
        String token = jwtService.generateToken(savedUser.getId().toString(), savedUser.getRole().name().toLowerCase());
        return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds());
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
}