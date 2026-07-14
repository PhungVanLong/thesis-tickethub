package ict.thesis.management.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import ict.thesis.management.entity.TicketTier;
import ict.thesis.management.repository.TicketTierRepository;
import ict.thesis.management.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaidKafkaListener {

    private final TicketTierRepository ticketTierRepository;
    private final ict.thesis.management.repository.SeatRepository seatRepository;
    private final org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate;
    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "order-paid-topic", groupId = "management-group")
    @Transactional
    public void handleOrderPaidEvent(String message) {
        log.info("Received order-paid event: {}", message);
        try {
            if (message == null || message.isBlank()) {
                return;
            }

            Map<String, Object> eventData = objectMapper.readValue(message, Map.class);
            Long orderId = ((Number) eventData.get("orderId")).longValue();
            String orderCode = (String) eventData.get("orderCode");
            String eventTitle = (String) eventData.get("eventTitle");
            String eventVenue = (String) eventData.get("eventVenue");
            String eventDate = (String) eventData.get("eventDate");
            String customerEmail = (String) eventData.get("customerEmail");
            BigDecimal totalAmount = new BigDecimal(eventData.get("totalAmount").toString());

            List<Map<String, Object>> tickets = (List<Map<String, Object>>) eventData.get("tickets");
            if (tickets == null || tickets.isEmpty()) {
                log.warn("No tickets found in event for order ID: {}", orderId);
                return;
            }

            // Extract seat IDs from tickets
            List<Long> seatIds = tickets.stream()
                    .map(t -> t.get("seatId"))
                    .filter(java.util.Objects::nonNull)
                    .map(id -> ((Number) id).longValue())
                    .toList();

            if (!seatIds.isEmpty()) {
                // PESSIMISTIC LOCK: Claim seats
                log.info("Attempting to claim seats with pessimistic lock: {}", seatIds);
                List<ict.thesis.management.entity.Seat> seats = seatRepository.findAllByIdWithLock(seatIds);
                
                // Check if any seat is already SOLD
                boolean alreadySold = seats.stream().anyMatch(seat -> ict.thesis.management.entity.enums.SeatStatus.SOLD.equals(seat.getStatus()));
                if (alreadySold) {
                    log.warn("Seat conflict detected for order ID: {}. Some seats are already SOLD.", orderId);
                    
                    // Publish claim failure / refund event
                    Map<String, Object> refundEvent = new java.util.HashMap<>();
                    refundEvent.put("orderId", orderId);
                    refundEvent.put("reason", "Seat already sold (overbooking conflict)");
                    String refundMsg = objectMapper.writeValueAsString(refundEvent);
                    log.info("Publishing refund event to order-refund-topic: {}", refundMsg);
                    kafkaTemplate.send("order-refund-topic", orderId.toString(), refundMsg);
                    return; // Abort ticket processing and email sending
                }

                // Mark seats as SOLD in database
                for (ict.thesis.management.entity.Seat seat : seats) {
                    seat.setStatus(ict.thesis.management.entity.enums.SeatStatus.SOLD);
                    seatRepository.save(seat);
                }
            }

            // Group tickets by ticketTierId to count quantity per tier
            Map<Long, Long> tierQuantities = tickets.stream()
                    .filter(t -> t.get("ticketTierId") != null)
                    .collect(Collectors.groupingBy(
                            t -> ((Number) t.get("ticketTierId")).longValue(),
                            Collectors.counting()
                    ));

            // Update ticket tier quantities
            for (Map.Entry<Long, Long> entry : tierQuantities.entrySet()) {
                Long tierId = entry.getKey();
                int qty = entry.getValue().intValue();

                ticketTierRepository.findById(tierId).ifPresentOrElse(tier -> {
                    int available = tier.getQuantityAvailable() != null ? tier.getQuantityAvailable() : 0;
                    int sold = tier.getQuantitySold() != null ? tier.getQuantitySold() : 0;

                    tier.setQuantityAvailable(Math.max(0, available - qty));
                    tier.setQuantitySold(sold + qty);
                    ticketTierRepository.save(tier);
                    log.info("Updated TicketTier ID {}: quantityAvailable={}, quantitySold={}", tierId, tier.getQuantityAvailable(), tier.getQuantitySold());
                }, () -> log.error("TicketTier ID {} not found in management database", tierId));
            }

            // Send confirmation email
            emailService.sendTicketPurchaseSuccessEmail(
                    customerEmail,
                    eventTitle,
                    eventVenue,
                    eventDate,
                    orderCode,
                    totalAmount,
                    tickets
            );

        } catch (Exception e) {
            log.error("Failed to process order-paid event", e);
        }
    }
}
