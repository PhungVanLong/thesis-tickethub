package ict.thesis.identity.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ict.thesis.identity.dto.StaffCreateKafkaRequest;
import ict.thesis.identity.dto.UserResponse;
import ict.thesis.identity.service.AuthService;
import lombok.RequiredArgsConstructor;

import org.springframework.kafka.core.KafkaTemplate;

@Component
@RequiredArgsConstructor
public class StaffCreateConsumer {
    private static final Logger logger = LoggerFactory.getLogger(StaffCreateConsumer.class);
    private final AuthService authService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user-staff-create-topic", groupId = "identity-group")
    public void consume(String payload) {
        logger.info("Received request to create staff account. Payload: {}", payload);
        String orgIdStr = "";
        String userIdStr = "";
        try {
            JsonNode node = objectMapper.readTree(payload);
            orgIdStr = node.has("organizationId") ? node.get("organizationId").asText() : "";
            String email = node.has("email") ? node.get("email").asText() : null;
            String password = node.has("password") ? node.get("password").asText() : null;
            String fullName = node.has("fullName") ? node.get("fullName").asText() : null;
            String phone = node.has("phone") && !node.get("phone").isNull() ? node.get("phone").asText() : null;

            if (orgIdStr.isBlank() || email == null || password == null || fullName == null) {
                throw new IllegalArgumentException("Invalid staff create payload");
            }

            StaffCreateKafkaRequest request = new StaffCreateKafkaRequest(
                    node.get("organizationId").asLong(),
                    node.has("requesterUserId") ? node.get("requesterUserId").asLong() : null,
                    email,
                    password,
                    fullName,
                    phone);

            UserResponse created = authService.createStaffAccount(
                    new ict.thesis.identity.dto.StaffRegisterRequest(request.email(), request.password(),
                            request.fullName(), request.phone()));
            userIdStr = created.getId() != null ? created.getId().toString() : "";

            String successPayload = String.format("{\"organizationId\":%s,\"userId\":%s}", orgIdStr, userIdStr);
            kafkaTemplate.send("user-staff-created-success-topic", successPayload);
            logger.info("Successfully created staff account for organization {} with user {}", orgIdStr, userIdStr);
        } catch (Exception e) {
            try {
                String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                String failedPayload = String.format("{\"organizationId\":%s,\"error\":\"%s\"}",
                        orgIdStr.isBlank() ? "null" : orgIdStr,
                        errorMsg.replace("\"", "\\\""));
                kafkaTemplate.send("user-staff-create-failed-topic", failedPayload);
            } catch (Exception ex) {
                logger.error("Failed to send staff create failure event to Kafka", ex);
            }
            logger.error("Error processing staff create payload: {}", payload, e);
        }
    }
}