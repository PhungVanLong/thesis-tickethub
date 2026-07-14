package ict.thesis.management.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import ict.thesis.management.dto.event.SeatStatusUpdateEvent;
import ict.thesis.management.entity.enums.SeatStatus;
import ict.thesis.management.repository.SeatRepository;
import ict.thesis.management.service.SeatStatusSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatStatusConsumer {

    private final SeatRepository seatRepository;
    private final SeatStatusSseService seatStatusSseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "${kafka.topic.seat-status-updates}", groupId = "management-group")
    @Transactional
    public void consume(String payload) {
        log.info("Received seat status update event payload: {}", payload);
        try {
            if (payload == null || payload.isBlank()) {
                log.warn("Empty payload received");
                return;
            }

            SeatStatusUpdateEvent event = objectMapper.readValue(payload, SeatStatusUpdateEvent.class);
            if (event.eventId() == null || event.seatIds() == null || event.seatIds().isEmpty()) {
                log.warn("Invalid event format received: {}", event);
                return;
            }

            // Convert String status to SeatStatus enum
            SeatStatus targetStatus = SeatStatus.valueOf(event.status());
            
            int updatedCount = seatRepository.updateStatusForIds(targetStatus, event.seatIds());
            log.info("Updated {} seats to status {} in DB", updatedCount, targetStatus);

            // Broadcast to all active SSE clients
            seatStatusSseService.broadcast(event.eventId(), event);

        } catch (IllegalArgumentException e) {
            log.error("Invalid SeatStatus in payload: {}", payload, e);
        } catch (Exception e) {
            log.error("Failed to process seat status update event", e);
        }
    }
}
