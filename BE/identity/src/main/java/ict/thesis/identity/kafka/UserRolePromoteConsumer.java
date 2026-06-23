package ict.thesis.identity.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import ict.thesis.identity.dto.UpdateUserRequest;
import ict.thesis.identity.service.AuthService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserRolePromoteConsumer {
    private static final Logger logger = LoggerFactory.getLogger(UserRolePromoteConsumer.class);
    private final AuthService authService;

    @KafkaListener(topics = "user-role-promote-topic", groupId = "identity-group")
    public void consume(String userIdStr) {
        logger.info("Received request to promote user role for userId: {}", userIdStr);
        try {
            Long userId = Long.valueOf(userIdStr);
            authService.updateUser(userId, new UpdateUserRequest(null, null, null, null, "ORGANIZER", null, null));
            logger.info("Successfully promoted user with ID: {} to ORGANIZER", userId);
        } catch (NumberFormatException e) {
            logger.error("Invalid userId received from Kafka message: {}", userIdStr, e);
        } catch (Exception e) {
            logger.error("Error updating user role for userId: {}", userIdStr, e);
        }
    }
}
