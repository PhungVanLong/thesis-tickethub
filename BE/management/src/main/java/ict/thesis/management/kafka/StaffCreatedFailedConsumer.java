package ict.thesis.management.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class StaffCreatedFailedConsumer {
    private static final Logger logger = LoggerFactory.getLogger(StaffCreatedFailedConsumer.class);

    @KafkaListener(topics = "${kafka.topic.user-staff-create-failed}", groupId = "management-group")
    public void consume(String payload) {
        logger.error("Received staff creation failed callback event. Payload: {}", payload);
    }
}