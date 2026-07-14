package ict.thesis.booking.service;

import ict.thesis.booking.enties.Order;
import ict.thesis.booking.enties.OrderItem;
import ict.thesis.booking.enties.enums.OrderStatus;
import ict.thesis.booking.repository.OrderItemRepository;
import ict.thesis.booking.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookingService bookingService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    private static final String ORDER_CANCELLED_TOPIC = "order-cancelled-topic";

    @Scheduled(cron = "0 * * * * *") // Runs every minute
    @Transactional
    public void cleanupExpiredOrders() {
        Instant cutoffTime = Instant.now().minus(10, ChronoUnit.MINUTES);
        log.info("Running expired orders cleanup task. Cutoff time: {}", cutoffTime);

        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, cutoffTime);
        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("Found {} expired pending orders to cancel.", expiredOrders.size());

        for (Order order : expiredOrders) {
            try {
                // Update order status to CANCELLED
                order.setStatus(OrderStatus.CANCELLED);
                order.setUpdatedAt(Instant.now());
                orderRepository.save(order);

                // Fetch order items to release seats and notify management service
                List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                
                // Release seats back to AVAILABLE
                List<Long> seatIds = items.stream()
                        .map(OrderItem::getSeat)
                        .filter(java.util.Objects::nonNull)
                        .toList();
                
                if (!seatIds.isEmpty()) {
                    bookingService.publishSeatStatus(order.getEventId(), seatIds, "AVAILABLE");
                }

                // Group by ticket tier to compile quantity returned
                Map<Long, Long> tierCounts = items.stream()
                        .filter(item -> item.getTicketTier() != null)
                        .collect(Collectors.groupingBy(item -> item.getTicketTier().getId(), Collectors.counting()));

                List<Map<String, Object>> itemsPayload = new ArrayList<>();
                for (Map.Entry<Long, Long> entry : tierCounts.entrySet()) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("ticketTierId", entry.getKey());
                    itemMap.put("quantity", entry.getValue().intValue());
                    itemsPayload.add(itemMap);
                }

                // Send cancelled event to Kafka
                Map<String, Object> eventPayload = new HashMap<>();
                eventPayload.put("orderId", order.getId());
                eventPayload.put("eventId", order.getEventId());
                eventPayload.put("items", itemsPayload);

                String jsonPayload = objectMapper.writeValueAsString(eventPayload);
                log.info("Publishing order-cancelled event: {}", jsonPayload);
                kafkaTemplate.send(ORDER_CANCELLED_TOPIC, order.getId().toString(), jsonPayload);

                log.info("Expired order {} has been successfully cancelled and seats released.", order.getId());
            } catch (Exception e) {
                log.error("Failed to cancel expired order ID: {}", order.getId(), e);
            }
        }
    }
}
