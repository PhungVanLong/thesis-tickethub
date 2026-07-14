package ict.thesis.booking.service;

import ict.thesis.booking.enties.Order;
import ict.thesis.booking.enties.OrderItem;
import ict.thesis.booking.enties.Ticket;
import ict.thesis.booking.enties.enums.TicketStatus;
import ict.thesis.booking.repository.OrderItemRepository;
import ict.thesis.booking.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final OrderItemRepository orderItemRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Value("${gateway.shared-secret}")
    private String gatewaySharedSecret;

    @Value("${management.service.url}")
    private String managementServiceUrl;

    private static final String ORDER_PAID_TOPIC = "order-paid-topic";

    @Transactional
    public void generateTicketsAndNotify(Order order) {
        log.info("Generating tickets for order ID: {}", order.getId());

        // Fetch order items
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        if (items.isEmpty()) {
            log.warn("No items found for order ID: {}, skipping ticket generation.", order.getId());
            return;
        }

        // Fetch event details to get event name, date, and set ticket expiration time (expiresAt)
        Instant expiresAt = null;
        String eventTitle = "Sự kiện " + order.getEventId();
        String eventVenue = "Chưa xác định";
        try {
            String eventUrl = managementServiceUrl + "/api/events/" + order.getEventId();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("X-Gateway-Token", gatewaySharedSecret);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.http.ResponseEntity<Map> response = restTemplate.exchange(
                    eventUrl, org.springframework.http.HttpMethod.GET, entity, Map.class
            );
            Map<String, Object> eventData = response.getBody();
            if (eventData != null) {
                eventTitle = (String) eventData.get("title");
                eventVenue = (String) eventData.get("venue");
                if (eventData.get("startTime") != null) {
                    expiresAt = Instant.parse(eventData.get("startTime").toString());
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch event details for ticket generation", e);
        }

        // If event date isn't fetched or is in the past, default expiresAt to 7 days from now
        if (expiresAt == null) {
            expiresAt = Instant.now().plus(java.time.Duration.ofDays(7));
        }

        List<Map<String, Object>> ticketsPayload = new ArrayList<>();

        for (OrderItem item : items) {
            String ticketCode = "TKT-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase();
            String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + ticketCode;

            Ticket ticket = Ticket.builder()
                    .orderItem(item)
                    .seat(item.getSeat())
                    .seatCode(item.getSeatCode())
                    .ticketCode(ticketCode)
                    .qrCodeUrl(qrCodeUrl)
                    .status(TicketStatus.VALID)
                    .issuedAt(Instant.now())
                    .expiresAt(expiresAt)
                    .build();

            ticketRepository.save(ticket);
            log.info("Ticket created successfully: ID={}, code={}", ticket.getId(), ticketCode);

            // Add ticket details for Kafka event
            Map<String, Object> tktMap = new HashMap<>();
            tktMap.put("ticketCode", ticketCode);
            tktMap.put("qrCodeUrl", qrCodeUrl);
            tktMap.put("seatId", item.getSeat());
            tktMap.put("seatCode", item.getSeatCode());
            tktMap.put("ticketTierId", item.getTicketTier() != null ? item.getTicketTier().getId() : null);
            tktMap.put("ticketTierName", item.getTicketTier() != null ? item.getTicketTier().getName() : "Vé");
            tktMap.put("price", item.getFinalPrice());
            ticketsPayload.add(tktMap);
        }

        // Build order paid Kafka event payload
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("orderId", order.getId());
        eventPayload.put("orderCode", order.getOrderCode());
        eventPayload.put("eventId", order.getEventId());
        eventPayload.put("eventTitle", eventTitle);
        eventPayload.put("eventVenue", eventVenue);
        eventPayload.put("eventDate", expiresAt.toString());
        eventPayload.put("customerEmail", order.getCustomerEmail());
        eventPayload.put("totalAmount", order.getTotalAmount());
        eventPayload.put("tickets", ticketsPayload);

        try {
            String jsonPayload = objectMapper.writeValueAsString(eventPayload);
            log.info("Publishing order-paid event: {}", jsonPayload);
            kafkaTemplate.send(ORDER_PAID_TOPIC, order.getId().toString(), jsonPayload);
        } catch (Exception e) {
            log.error("Failed to publish order-paid Kafka event for order ID: {}", order.getId(), e);
        }
    }
}
