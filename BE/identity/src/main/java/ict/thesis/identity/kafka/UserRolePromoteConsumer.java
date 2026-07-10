package ict.thesis.identity.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import ict.thesis.identity.dto.UpdateUserRequest;
import ict.thesis.identity.service.AuthService;
import lombok.RequiredArgsConstructor;

import org.springframework.kafka.core.KafkaTemplate;

@Component
@RequiredArgsConstructor
public class UserRolePromoteConsumer {
    private static final Logger logger = LoggerFactory.getLogger(UserRolePromoteConsumer.class);
    private final AuthService authService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = "user-role-promote-topic", groupId = "identity-group")
    public void consume(String payload) {
        logger.info("Received request to promote user role. Payload: {}", payload);
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

            Long userId = Long.valueOf(userIdStr);
            authService.updateUser(userId, new UpdateUserRequest(null, null, null, null, "ORGANIZER", null, null));
            logger.info("Successfully promoted user with ID: {} to ORGANIZER", userId);

            // Gửi callback phản hồi thành công nếu có orgId
            if (!orgIdStr.isEmpty()) {
                logger.info("Sending callback success event back to Kafka for organization: {}, user: {}", orgIdStr, userIdStr);
                kafkaTemplate.send("user-role-promoted-success-topic", payload);
            }
        } catch (Exception e) {
            logger.error("Error processing user role promotion for payload: {}", payload, e);
            if (!orgIdStr.isEmpty()) {
                try {
                    String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    String failedPayload = String.format("{\"organizationId\":%s,\"userId\":%s,\"error\":\"%s\"}", 
                        orgIdStr, userIdStr.isEmpty() ? "null" : userIdStr, errorMsg.replace("\"", "\\\""));
                    logger.info("Sending callback failure event back to Kafka: {}", failedPayload);
                    kafkaTemplate.send("user-role-promote-failed-topic", failedPayload);
                } catch (Exception ex) {
                    logger.error("Failed to send rollback event to Kafka", ex);
                }
            }
        }
    }
}
