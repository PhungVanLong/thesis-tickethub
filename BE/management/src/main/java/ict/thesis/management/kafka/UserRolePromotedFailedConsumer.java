package ict.thesis.management.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ict.thesis.management.entity.enums.OrganizationStatus;
import ict.thesis.management.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserRolePromotedFailedConsumer {
    private static final Logger logger = LoggerFactory.getLogger(UserRolePromotedFailedConsumer.class);
    private final OrganizationRepository organizationRepository;

    @KafkaListener(topics = "${kafka.topic.user-role-promote-failed}", groupId = "management-group")
    @Transactional
    public void consume(String payload) {
        logger.error("Received user role promotion failed callback event. Starting Saga compensating transaction. Payload: {}", payload);
        try {
            if (payload == null) {
                logger.warn("Null payload received in failure callback");
                return;
            }

            String orgIdStr = "";
            String errorMsg = "";
            if (payload.trim().startsWith("{")) {
                try {
                    com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
                    orgIdStr = node.has("organizationId") ? node.get("organizationId").asText() : "";
                    errorMsg = node.has("error") ? node.get("error").asText() : "Unknown error";
                } catch (Exception e) {
                    logger.error("Failed to parse JSON failure payload: {}", payload, e);
                }
            }

            if (orgIdStr.isEmpty()) {
                logger.warn("Invalid payload content received in failure callback: {}", payload);
                return;
            }

            final Long finalOrgId = Long.valueOf(orgIdStr);
            final String finalErrorMsg = errorMsg;
            organizationRepository.findById(finalOrgId).ifPresentOrElse(org -> {
                if (org.getStatus() == OrganizationStatus.ACTIVE) {
                    logger.info("Rolling back status of organization ID: {} from ACTIVE to PENDING due to promotion failure", finalOrgId);
                    org.setStatus(OrganizationStatus.PENDING);
                    org.setVerificationReason("[Lỗi phân quyền hệ thống]: " + finalErrorMsg);
                    organizationRepository.save(org);
                    logger.info("Successfully rolled back organization ID: {} to PENDING", finalOrgId);
                } else {
                    logger.warn("Organization ID: {} is in status {}, not ACTIVE. Skipping status rollback.", finalOrgId, org.getStatus());
                }
            }, () -> {
                logger.warn("Organization not found for orgId: {}", finalOrgId);
            });

        } catch (Exception e) {
            logger.error("Error processing user role promotion failure callback event: {}", payload, e);
        }
    }
}
