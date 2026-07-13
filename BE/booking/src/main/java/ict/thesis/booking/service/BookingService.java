package ict.thesis.booking.service;

import ict.thesis.booking.dto.BookingDtos.CreateBookingRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import ict.thesis.booking.enties.Order;
import ict.thesis.booking.enties.OrderItem;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ict.thesis.booking.repository.OrderRepository orderRepository;
    private final ict.thesis.booking.repository.OrderItemRepository orderItemRepository;
    private final RestTemplate restTemplate;
    
    @Value("${gateway.shared-secret}")
    private String gatewaySharedSecret;

    @Value("${management.service.url}")
    private String managementServiceUrl;
    
    // Store SseEmitter for each request ID
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    // Cache for completed results if the client hasn't subscribed yet
    private final Map<String, Long> completedBookings = new ConcurrentHashMap<>();
    private final Map<String, String> failedBookings = new ConcurrentHashMap<>();

    private static final String BOOKING_TOPIC = "booking.requests";
    private static final String SEAT_STATUS_TOPIC = "seat-status-updates";
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public record SeatStatusUpdateEvent(Long eventId, java.util.List<Long> seatIds, String status) {}

    /** Build HttpHeaders with gateway token and forward user context headers if present */
    private HttpHeaders buildInternalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Gateway-Token", gatewaySharedSecret);

        try {
            org.springframework.web.context.request.RequestAttributes attributes = 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                jakarta.servlet.http.HttpServletRequest request = 
                    ((org.springframework.web.context.request.ServletRequestAttributes) attributes).getRequest();
                
                String userId = request.getHeader("X-User-Id");
                String userRole = request.getHeader("X-User-Role");
                String userEmail = request.getHeader("X-User-Email");

                if (userId != null) headers.set("X-User-Id", userId);
                if (userRole != null) headers.set("X-User-Role", userRole);
                if (userEmail != null) headers.set("X-User-Email", userEmail);
            }
        } catch (Exception e) {
            log.warn("Could not extract user headers to forward", e);
        }
        return headers;
    }

    public void publishSeatStatus(Long eventId, java.util.List<Long> seatIds, String status) {
        if (eventId == null || seatIds == null || seatIds.isEmpty()) {
            return;
        }
        try {
            SeatStatusUpdateEvent event = new SeatStatusUpdateEvent(eventId, seatIds, status);
            String json = objectMapper.writeValueAsString(event);
            log.info("Publishing seat status update event: {}", json);
            kafkaTemplate.send(SEAT_STATUS_TOPIC, eventId.toString(), json);
        } catch (Exception e) {
            log.error("Failed to publish seat status update to Kafka", e);
        }
    }

    public void completeMockPayment(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(ict.thesis.booking.enties.enums.OrderStatus.PAID);
            order.setUpdatedAt(java.time.Instant.now());
            orderRepository.save(order);
            log.info("Mock payment completed for order ID: {}", orderId);

            java.util.List<Long> seatIds = orderItemRepository.findByOrderId(orderId).stream()
                .map(ict.thesis.booking.enties.OrderItem::getSeat)
                .filter(java.util.Objects::nonNull)
                .toList();

            publishSeatStatus(order.getEventId(), seatIds, "SOLD");
        });
    }

    public void cancelMockPayment(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(ict.thesis.booking.enties.enums.OrderStatus.CANCELLED);
            order.setUpdatedAt(java.time.Instant.now());
            orderRepository.save(order);
            log.info("Mock payment cancelled for order ID: {}", orderId);

            java.util.List<Long> seatIds = orderItemRepository.findByOrderId(orderId).stream()
                .map(ict.thesis.booking.enties.OrderItem::getSeat)
                .filter(java.util.Objects::nonNull)
                .toList();

            publishSeatStatus(order.getEventId(), seatIds, "AVAILABLE");
        });
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingMessage {
        private String requestId;
        private CreateBookingRequest payload;
    }

    public String submitBookingRequest(CreateBookingRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Submitting booking request {} to Kafka", requestId);
        
        BookingMessage message = new BookingMessage(requestId, request);
        try {
            String json = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(BOOKING_TOPIC, requestId, json);
        } catch (Exception e) {
            log.error("Failed to serialize BookingMessage for requestId: {}", requestId, e);
            throw new RuntimeException("Failed to serialize booking request", e);
        }
        return requestId;
    }

    public SseEmitter subscribeToBookingResult(String requestId) {
        SseEmitter emitter = new SseEmitter(60000L); // 60 seconds timeout

        // Check if there is already a cached success result
        if (completedBookings.containsKey(requestId)) {
            Long orderId = completedBookings.remove(requestId);
            try {
                emitter.send(SseEmitter.event().name("SUCCESS").data(orderId));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // Check if there is already a cached failed result
        if (failedBookings.containsKey(requestId)) {
            String reason = failedBookings.remove(requestId);
            try {
                emitter.send(SseEmitter.event().name("FAILED").data(reason));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        emitters.put(requestId, emitter);

        emitter.onCompletion(() -> emitters.remove(requestId));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(requestId);
        });
        emitter.onError(e -> {
            emitter.completeWithError(e);
            emitters.remove(requestId);
        });
        
        // Send a dummy event to establish connection immediately
        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void notifyBookingSuccess(String requestId, Long orderId) {
        SseEmitter emitter = emitters.get(requestId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("SUCCESS").data(orderId));
                emitter.complete();
            } catch (IOException e) {
                log.error("Failed to send SSE for requestId {}", requestId, e);
                emitter.completeWithError(e);
            } finally {
                emitters.remove(requestId);
            }
        } else {
            log.info("SseEmitter not found for requestId: {}, caching success result.", requestId);
            completedBookings.put(requestId, orderId);
        }
    }
    
    public void notifyBookingFailed(String requestId, String reason) {
        SseEmitter emitter = emitters.get(requestId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("FAILED").data(reason));
                emitter.complete();
            } catch (IOException e) {
                log.error("Failed to send FAILED SSE for requestId {}", requestId, e);
                emitter.completeWithError(e);
            } finally {
                emitters.remove(requestId);
            }
        } else {
            log.info("SseEmitter not found for requestId: {}, caching failed result.", requestId);
            failedBookings.put(requestId, reason);
        }
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Order not found"));

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("orderId", order.getId());
        response.put("orderCode", order.getOrderCode());
        response.put("subtotal", order.getSubtotal());
        response.put("totalAmount", order.getTotalAmount());
        response.put("status", order.getStatus().toString());

        // Use @LoadBalanced RestTemplate with Eureka service name
        HttpHeaders headers = buildInternalHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map<String, String> tierColorMap = new java.util.HashMap<>();
        String eventUrl = managementServiceUrl + "/api/events/" + order.getEventId();
        try {
            ResponseEntity<Map> eventResponse = restTemplate.exchange(eventUrl, HttpMethod.GET, entity, Map.class);
            Map<String, Object> event = eventResponse.getBody();
            if (event != null) {
                response.put("eventTitle", event.get("title"));
                response.put("eventDate", event.get("startTime"));
                response.put("eventVenue", event.get("venue"));
                response.put("bannerUrl", event.get("bannerUrl"));

                if (event.get("ticketTiers") != null) {
                    List<Map<String, Object>> tiers = (List<Map<String, Object>>) event.get("ticketTiers");
                    for (Map<String, Object> tier : tiers) {
                        String name = (String) tier.get("name");
                        String color = (String) tier.get("colorCode");
                        if (name != null && color != null) {
                            tierColorMap.put(name, color);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch event details from management service", e);
            response.put("eventTitle", "Event " + order.getEventId());
        }

        // Fetch seat codes mapping (with gateway token)
        Map<Long, String> seatCodeMap = new java.util.HashMap<>();
        try {
            String seatMapsUrl = managementServiceUrl + "/api/events/" + order.getEventId() + "/seat-maps";
            ResponseEntity<List> seatMapsResponse = restTemplate.exchange(seatMapsUrl, HttpMethod.GET, entity, List.class);
            List<Map<String, Object>> seatMaps = seatMapsResponse.getBody();
            if (seatMaps != null) {
                for (Map<String, Object> map : seatMaps) {
                    List<Map<String, Object>> seats = (List<Map<String, Object>>) map.get("seats");
                    if (seats != null) {
                        for (Map<String, Object> seat : seats) {
                            Number idNum = (Number) seat.get("id");
                            String seatCode = (String) seat.get("seatCode");
                            if (idNum != null && seatCode != null) {
                                seatCodeMap.put(idNum.longValue(), seatCode);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch seat map details from management service", e);
        }

        // Map items
        List<Map<String, Object>> itemsList = new java.util.ArrayList<>();
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            Map<String, Object> itemMap = new java.util.HashMap<>();
            String tierName = item.getTicketTier() != null ? item.getTicketTier().getName() : "Standard";
            itemMap.put("ticketTierName", tierName);
            itemMap.put("price", item.getFinalPrice());
            itemMap.put("quantity", 1);
            itemMap.put("ticketTierColor", tierColorMap.getOrDefault(tierName, "#2563eb"));
            
            String seatLabel = item.getSeatCode();
            if (seatLabel == null && item.getSeat() != null) {
                seatLabel = seatCodeMap.get(item.getSeat());
            }
            if (seatLabel == null) {
                seatLabel = "Seat " + item.getSeat();
            }
            itemMap.put("seatLabel", seatLabel);
            itemsList.add(itemMap);
        }
        response.put("items", itemsList);

        return response;
    }
}
