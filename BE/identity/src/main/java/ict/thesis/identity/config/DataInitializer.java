package ict.thesis.identity.config;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import ict.thesis.identity.entity.User;
import ict.thesis.identity.entity.enums.UserRole;
import ict.thesis.identity.repository.UserRepository;

@Configuration
public class DataInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataInitializer.class);

    private static final String DEFAULT_ADMIN_EMAIL = "admin@local";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123456";

    @Bean
    public CommandLineRunner initAdminUser(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        org.springframework.jdbc.core.JdbcTemplate jdbcTemplate
    ) {
        return args -> {
            try {
                jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
                LOGGER.info("Dropped users_role_check constraint if it existed.");
            } catch (Exception e) {
                LOGGER.warn("Could not drop users_role_check constraint: {}", e.getMessage());
            }

            if (userRepository.existsByEmail(DEFAULT_ADMIN_EMAIL)) {
                return;
            }

            User admin = User.builder()
                .email(DEFAULT_ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
                .fullName("System Admin")
                .role(UserRole.ADMIN.name())
                .verified(true)
                .active(true)
                .createdAt(Instant.now())
                .build();

            userRepository.save(admin);
            LOGGER.info("Default admin created: {}", DEFAULT_ADMIN_EMAIL);
        };
    }
}
