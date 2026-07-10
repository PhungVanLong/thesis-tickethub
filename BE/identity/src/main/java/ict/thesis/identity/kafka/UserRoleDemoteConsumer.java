package ict.thesis.identity.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ict.thesis.identity.entity.enums.UserRole;
import ict.thesis.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;

@Component
@RequiredArgsConstructor
public class UserRoleDemoteConsumer {
    private static final Logger logger = LoggerFactory.getLogger(UserRoleDemoteConsumer.class);
    private final UserRepository userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = "user-role-demote-topic", groupId = "identity-group")
    @Transactional
    public void consume(String payload) {
        logger.info("Received request to demote user role. Payload: {}", payload);
        String orgIdStr = "";
        String userIdStr = "";
        try {
            if (payload.trim().startsWith("{")) {
                try {
                    com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
                    orgIdStr = node.has("organizationId") ? node.get("organizationId").asText() : "";
                    userIdStr = node.has("userId") ? node.get("userId").asText() : "";
                } catch (Exception e) {
                    logger.error("Failed to parse JSON payload: {}", payload, e);
                }
            } else {
                if (payload.contains(",")) {
                    String[] parts = payload.split(",");
                    orgIdStr = parts[0].trim();
                    userIdStr = parts[1].trim();
                } else {
                    userIdStr = payload.trim();
                }
            }

            if (userIdStr.isEmpty()) {
                logger.warn("No userId found in demote request payload: {}", payload);
                return;
            }

            final String finalOrgIdStr = orgIdStr;
            final Long finalUserId = Long.valueOf(userIdStr);
            userRepository.findById(finalUserId).ifPresentOrElse(user -> {
                user.removeRole(UserRole.ORGANIZER);
                userRepository.save(user);
                logger.info("Successfully removed ORGANIZER role for user with ID: {}", finalUserId);

                // Gửi callback thành công về cho management
                if (!finalOrgIdStr.isEmpty()) {
                    logger.info("Sending callback demote success event to Kafka: {}", payload);
                    kafkaTemplate.send("user-role-demoted-success-topic", payload);
                }
            }, () -> {
                logger.error("User with ID: {} not found for role demotion", finalUserId);
            });

        } catch (Exception e) {
            logger.error("Error processing user role demotion for payload: {}", payload, e);
        }
    }
}
