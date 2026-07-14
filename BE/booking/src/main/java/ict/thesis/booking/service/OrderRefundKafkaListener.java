package ict.thesis.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ict.thesis.booking.enties.Order;
import ict.thesis.booking.enties.Payment;
import ict.thesis.booking.enties.Ticket;
import ict.thesis.booking.enties.enums.OrderStatus;
import ict.thesis.booking.enties.enums.PaymentStatus;
import ict.thesis.booking.enties.enums.TicketStatus;
import ict.thesis.booking.repository.OrderItemRepository;
import ict.thesis.booking.repository.OrderRepository;
import ict.thesis.booking.repository.PaymentRepository;
import ict.thesis.booking.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderRefundKafkaListener {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookingService bookingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "${kafka.topic.order-refund}", groupId = "booking-group")
    @Transactional
    public void handleOrderRefundEvent(String message) {
        log.info("Received order-refund event: {}", message);
        try {
            if (message == null || message.isBlank()) {
                return;
            }

            Map<String, Object> eventData = objectMapper.readValue(message, Map.class);
            Long orderId = ((Number) eventData.get("orderId")).longValue();
            String reason = (String) eventData.get("reason");

            // Update order status to REFUNDED
            orderRepository.findById(orderId).ifPresent(order -> {
                order.setStatus(OrderStatus.REFUNDED);
                order.setUpdatedAt(Instant.now());
                orderRepository.save(order);
                log.info("Updated order ID {} status to REFUNDED due to conflict.", orderId);

                // Update payment status to REFUNDED
                paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.REFUNDED);
                    payment.setRefundTxId("REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                    payment.setRefundedAt(Instant.now());
                    payment.setRefundReason(reason != null ? reason : "Overbooking seat conflict");
                    paymentRepository.save(payment);
                    log.info("Updated payment ID {} status to REFUNDED.", payment.getId());
                });

                // Update generated tickets to CANCELLED
                List<Ticket> tickets = ticketRepository.findByOrderId(orderId);
                for (Ticket ticket : tickets) {
                    ticket.setStatus(TicketStatus.CANCELLED);
                    ticketRepository.save(ticket);
                    log.info("Cancelled ticket ID: {}, code: {}", ticket.getId(), ticket.getTicketCode());
                }

                // Release seats back to AVAILABLE
                List<Long> seatIds = orderItemRepository.findByOrderId(orderId).stream()
                        .map(item -> item.getSeat())
                        .filter(java.util.Objects::nonNull)
                        .toList();

                if (!seatIds.isEmpty()) {
                    bookingService.publishSeatStatus(order.getEventId(), seatIds, "AVAILABLE");
                    log.info("Released seats {} to AVAILABLE.", seatIds);
                }
            });

        } catch (Exception e) {
            log.error("Failed to process order-refund event", e);
        }
    }
}
