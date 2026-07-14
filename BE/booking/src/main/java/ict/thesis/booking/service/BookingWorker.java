
package ict.thesis.booking.service;

import ict.thesis.booking.dto.BookingDtos.BookingItemRequest;
import ict.thesis.booking.dto.BookingDtos.CreateBookingRequest;
import ict.thesis.booking.enties.Order;
import ict.thesis.booking.enties.OrderItem;
import ict.thesis.booking.enties.enums.OrderStatus;
import ict.thesis.booking.repository.OrderItemRepository;
import ict.thesis.booking.repository.OrderRepository;
import ict.thesis.booking.repository.TicketTierRefRepository;
import ict.thesis.booking.service.BookingService.BookingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingWorker {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TicketTierRefRepository ticketTierRefRepository;
    private final BookingService bookingService;
    private final org.springframework.web.client.RestTemplate restTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @org.springframework.beans.factory.annotation.Value("${gateway.shared-secret}")
    private String gatewaySharedSecret;

    @org.springframework.beans.factory.annotation.Value("${management.service.url}")
    private String managementServiceUrl;

    private org.springframework.http.HttpHeaders buildInternalHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("X-Gateway-Token", gatewaySharedSecret);
        return headers;
    }

    @KafkaListener(topics = "booking.requests", groupId = "booking-group")
    @Transactional
    public void processBookingRequest(String payload) {
        log.info("Received booking request from Kafka payload: {}", payload);
        
        BookingMessage message = null;
        try {
            message = objectMapper.readValue(payload, BookingMessage.class);
            log.info("Processing booking request: {}", message.getRequestId());
            CreateBookingRequest request = message.getPayload();

            // Sync ticket tiers from management service dynamically (Pay-To-Win logic)
            try {
                String eventUrl = managementServiceUrl + "/api/events/" + request.eventId();
                org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(buildInternalHeaders());
                org.springframework.http.ResponseEntity<java.util.Map> eventResponse = 
                    restTemplate.exchange(eventUrl, org.springframework.http.HttpMethod.GET, entity, java.util.Map.class);
                java.util.Map<String, Object> event = eventResponse.getBody();
                if (event != null && event.get("ticketTiers") != null) {
                    java.util.List<java.util.Map<String, Object>> tiers = 
                        (java.util.List<java.util.Map<String, Object>>) event.get("ticketTiers");
                    for (java.util.Map<String, Object> tierMap : tiers) {
                        Long tierId = ((Number) tierMap.get("id")).longValue();
                        String tierName = (String) tierMap.get("name");
                        BigDecimal tierPrice = new BigDecimal(tierMap.get("price").toString());
                        
                        if (!ticketTierRefRepository.existsById(tierId)) {
                            ict.thesis.booking.enties.TicketTierRef ref = ict.thesis.booking.enties.TicketTierRef.builder()
                                .id(tierId)
                                .eventId(request.eventId())
                                .eventName((String) event.get("title"))
                                .name(tierName)
                                .price(tierPrice)
                                .syncedAt(java.time.Instant.now())
                                .build();
                            ticketTierRefRepository.save(ref);
                            log.info("Synced ticket tier dynamically: id={}, name={}, price={}", tierId, tierName, tierPrice);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to sync ticket tiers dynamically for eventId: {}", request.eventId(), e);
            }
            
            // Generate order code
            String orderCode = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            
            // Initialize Order with PENDING status
            Order order = new Order();
            order.setOrderCode(orderCode);
            order.setEventId(request.eventId());
            order.setCustomer(request.customerId());
            order.setCustomerEmail(request.customerEmail());
            order.setIdempotencyKey(request.idempotencyKey() != null ? request.idempotencyKey() : UUID.randomUUID().toString());
            order.setStatus(OrderStatus.PENDING);
            order.setCreatedAt(Instant.now());
            
            // Assume total amount is calculated later or fetched from management service.
            // For now, setting dummy values for test.
            order.setSubtotal(BigDecimal.ZERO);
            order.setTotalAmount(BigDecimal.ZERO);

            Order savedOrder = orderRepository.save(order);
            
            // Process order items (without locking seats yet)
            BigDecimal subtotal = BigDecimal.ZERO;
            
            for (BookingItemRequest itemReq : request.items()) {
                OrderItem item = new OrderItem();
                item.setOrder(savedOrder);
                item.setSeat(itemReq.seatId());
                item.setSeatCode(itemReq.seatLabel());
                
                if (itemReq.ticketTierId() != null) {
                    ticketTierRefRepository.findById(itemReq.ticketTierId()).ifPresent(tier -> {
                        item.setTicketTier(tier);
                        item.setOriginalPrice(tier.getPrice());
                        item.setFinalPrice(tier.getPrice());
                    });
                }
                
                if (item.getFinalPrice() == null) {
                    item.setOriginalPrice(BigDecimal.ZERO);
                    item.setFinalPrice(BigDecimal.ZERO);
                }
                
                subtotal = subtotal.add(item.getFinalPrice());
                orderItemRepository.save(item);
            }
            
            // Update total amount on order
            savedOrder.setSubtotal(subtotal);
            savedOrder.setTotalAmount(subtotal);
            orderRepository.save(savedOrder);
            
            log.info("Order created successfully: {}", savedOrder.getId());

            // Publish RESERVED status for the seats
            java.util.List<Long> seatIds = request.items().stream()
                .map(itemReq -> itemReq.seatId())
                .filter(java.util.Objects::nonNull)
                .toList();
            bookingService.publishSeatStatus(request.eventId(), seatIds, "RESERVED");
            
            // Notify Frontend via SSE AFTER transaction commit to avoid 404 race condition (Pay-To-Win logic)
            final String finalRequestId = message.getRequestId();
            final Long finalOrderId = savedOrder.getId();
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            bookingService.notifyBookingSuccess(finalRequestId, finalOrderId);
                        }
                    }
                );
            } else {
                bookingService.notifyBookingSuccess(finalRequestId, finalOrderId);
            }
            
        } catch (Exception e) {
            String rId = (message != null) ? message.getRequestId() : "UNKNOWN";
            log.error("Failed to process booking request {}", rId, e);
            if (message != null) {
                bookingService.notifyBookingFailed(message.getRequestId(), "Failed to create order: " + e.getMessage());
            }
        }
    }
}
