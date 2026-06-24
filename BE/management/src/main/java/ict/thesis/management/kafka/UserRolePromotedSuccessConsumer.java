package ict.thesis.management.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import ict.thesis.management.repository.OrganizationRepository;
import ict.thesis.management.service.EmailService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserRolePromotedSuccessConsumer {
    private static final Logger logger = LoggerFactory.getLogger(UserRolePromotedSuccessConsumer.class);

    private final OrganizationRepository organizationRepository;
    private final EmailService emailService;

    @KafkaListener(topics = "user-role-promoted-success-topic", groupId = "management-group")
    public void consume(String payload) {
        logger.info("Received user role promotion success callback event. Payload: {}", payload);
        try {
            if (payload == null) {
                logger.warn("Null payload received in success callback");
                return;
            }

            String orgIdStr = "";
            String userIdStr = "";
            if (payload.trim().startsWith("{")) {
                try {
                    com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
                    orgIdStr = node.has("organizationId") ? node.get("organizationId").asText() : "";
                    userIdStr = node.has("userId") ? node.get("userId").asText() : "";
                } catch (Exception e) {
                    logger.error("Failed to parse JSON callback payload: {}", payload, e);
                }
            } else {
                if (payload.contains(",")) {
                    String[] parts = payload.split(",");
                    orgIdStr = parts[0].trim();
                    userIdStr = parts[1].trim();
                }
            }

            if (orgIdStr.isEmpty() || userIdStr.isEmpty()) {
                logger.warn("Invalid payload content received in success callback: {}", payload);
                return;
            }

            Long orgId = Long.valueOf(orgIdStr);
            Long userId = Long.valueOf(userIdStr);

            logger.info("Querying organization information for orgId: {} to send approval email...", orgId);
            organizationRepository.findById(orgId).ifPresentOrElse(org -> {
                try {
                    emailService.sendVerificationEmail(org.getOfficialEmail(), org.getName(), true, "Đăng ký của bạn đã được phê duyệt thành công.");
                    logger.info("Triggered async approval email successfully to: {} for orgId: {}", org.getOfficialEmail(), orgId);
                } catch (Exception e) {
                    logger.error("Failed to send approval email for orgId: {}", orgId, e);
                }
            }, () -> {
                logger.warn("Organization not found for orgId: {}", orgId);
            });

        } catch (Exception e) {
            logger.error("Error processing user role promotion success callback event: {}", payload, e);
        }
    }
}
