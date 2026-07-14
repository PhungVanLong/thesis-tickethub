package ict.thesis.management.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import ict.thesis.management.entity.TicketTier;
import ict.thesis.management.repository.TicketTierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCancelledKafkaListener {

    private final TicketTierRepository ticketTierRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "${kafka.topic.order-cancelled}", groupId = "management-group")
    @Transactional
    public void handleOrderCancelledEvent(String message) {
        log.info("Received order-cancelled event: {}", message);
        try {
            if (message == null || message.isBlank()) {
                return;
            }

            Map<String, Object> eventData = objectMapper.readValue(message, Map.class);
            Long orderId = ((Number) eventData.get("orderId")).longValue();
            List<Map<String, Object>> items = (List<Map<String, Object>>) eventData.get("items");

            if (items == null || items.isEmpty()) {
                log.warn("No items found in cancellation event for order ID: {}", orderId);
                return;
            }

            for (Map<String, Object> item : items) {
                if (item.get("ticketTierId") == null || item.get("quantity") == null) {
                    continue;
                }

                Long tierId = ((Number) item.get("ticketTierId")).longValue();
                int qty = ((Number) item.get("quantity")).intValue();

                ticketTierRepository.findById(tierId).ifPresentOrElse(tier -> {
                    int available = tier.getQuantityAvailable() != null ? tier.getQuantityAvailable() : 0;
                    int sold = tier.getQuantitySold() != null ? tier.getQuantitySold() : 0;

                    tier.setQuantityAvailable(available + qty);
                    tier.setQuantitySold(Math.max(0, sold - qty));
                    ticketTierRepository.save(tier);
                    log.info("Reverted TicketTier ID {}: quantityAvailable={}, quantitySold={}", tierId, tier.getQuantityAvailable(), tier.getQuantitySold());
                }, () -> log.error("TicketTier ID {} not found in management database for reversion", tierId));
            }

        } catch (Exception e) {
            log.error("Failed to process order-cancelled event", e);
        }
    }
}
