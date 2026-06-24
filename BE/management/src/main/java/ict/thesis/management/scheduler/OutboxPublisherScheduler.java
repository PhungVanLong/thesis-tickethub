package ict.thesis.management.scheduler;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ict.thesis.management.entity.OutboxEvent;
import ict.thesis.management.entity.enums.OutboxStatus;
import ict.thesis.management.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxPublisherScheduler {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherScheduler.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000) // Quét mỗi 5 giây
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatus(OutboxStatus.PENDING);
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} pending outbox events to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                String topic = getTopicForEvent(event.getEventType());
                if (topic == null) {
                    log.error("Unknown event type: {}, marking as FAILED. EventId: {}", event.getEventType(), event.getId());
                    event.setStatus(OutboxStatus.FAILED);
                    outboxEventRepository.save(event);
                    continue;
                }

                String payload = event.getPayload();
                log.info("Publishing outbox event to Kafka. Topic: {}, Payload: {}, EventId: {}", topic, payload, event.getId());

                // Gửi đồng bộ để đảm bảo Kafka đã ghi nhận thành công trước khi cập nhật DB
                kafkaTemplate.send(topic, payload).get();

                log.info("Successfully published outbox event to Kafka. EventId: {}", event.getId());
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);

            } catch (Exception e) {
                log.error("Error publishing outbox event to Kafka. EventId: {}, Current Retry: {}", event.getId(), event.getRetryCount(), e);
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 5) {
                    event.setStatus(OutboxStatus.FAILED);
                    log.error("Outbox event exceeded maximum retries. Marking as FAILED. EventId: {}", event.getId());
                }
                outboxEventRepository.save(event);
            }
        }
    }

    private String getTopicForEvent(String eventType) {
        if ("USER_ROLE_PROMOTE".equals(eventType)) {
            return "user-role-promote-topic";
        } else if ("USER_ROLE_DEMOTE".equals(eventType)) {
            return "user-role-demote-topic";
        }
        return null;
    }
}
