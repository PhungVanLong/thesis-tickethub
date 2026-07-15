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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import ict.thesis.booking.enties.Order;
import ict.thesis.booking.enties.OrderItem;
import ict.thesis.booking.enties.Ticket;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ict.thesis.booking.repository.OrderRepository orderRepository;
    private final ict.thesis.booking.repository.OrderItemRepository orderItemRepository;
    private final RestTemplate restTemplate;
    private final ict.thesis.booking.repository.OutboxEventRepository outboxEventRepository;
    private final ict.thesis.booking.repository.TicketRepository ticketRepository;
    private final ict.thesis.booking.repository.CheckinRepository checkinRepository;
    private final ict.thesis.booking.repository.EventRefRepository eventRefRepository;
    private final ict.thesis.booking.repository.TicketTierRefRepository ticketTierRefRepository;

    @Value("${gateway.shared-secret}")
    private String gatewaySharedSecret;

    @Value("${management.service.url}")
    private String managementServiceUrl;



    @Value("${kafka.topic.booking-requests}")
    private String bookingTopic;

    @Value("${kafka.topic.seat-status-updates}")
    private String seatStatusTopic;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public record SeatStatusUpdateEvent(Long eventId, java.util.List<Long> seatIds, String status) {
    }

    /**
     * Build HttpHeaders with gateway token and forward user context headers if
     * present
     */
    private HttpHeaders buildInternalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Gateway-Token", gatewaySharedSecret);

        try {
            org.springframework.web.context.request.RequestAttributes attributes = org.springframework.web.context.request.RequestContextHolder
                    .getRequestAttributes();
            if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                jakarta.servlet.http.HttpServletRequest request = ((org.springframework.web.context.request.ServletRequestAttributes) attributes)
                        .getRequest();

                String userId = request.getHeader("X-User-Id");
                String userRole = request.getHeader("X-User-Role");
                String userEmail = request.getHeader("X-User-Email");

                if (userId != null)
                    headers.set("X-User-Id", userId);
                if (userRole != null)
                    headers.set("X-User-Role", userRole);
                if (userEmail != null)
                    headers.set("X-User-Email", userEmail);
            }
        } catch (Exception e) {
            log.warn("Could not extract user headers to forward", e);
        }
        return headers;
    }

    public ict.thesis.booking.enties.EventRef getOrSyncEvent(Long eventId) {
        if (eventId == null) return null;
        
        return eventRefRepository.findById(eventId).orElseGet(() -> {
            log.info("EventRef cache miss for eventId: {}. Fetching dynamically from management service.", eventId);
            try {
                String eventUrl = managementServiceUrl + "/api/events/" + eventId;
                HttpHeaders headers = buildInternalHeaders();
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                ResponseEntity<Map> response = restTemplate.exchange(eventUrl, HttpMethod.GET, entity, Map.class);
                Map<String, Object> body = response.getBody();
                if (body != null) {
                    String title = (String) body.get("title");
                    String startTimeStr = (String) body.get("startTime");
                    String endTimeStr = (String) body.get("endTime");
                    String venue = (String) body.get("venue");
                    String city = (String) body.get("city");
                    String bannerUrl = (String) body.get("bannerUrl");

                    java.time.Instant startTime = startTimeStr != null ? java.time.Instant.parse(startTimeStr) : null;
                    java.time.Instant endTime = endTimeStr != null ? java.time.Instant.parse(endTimeStr) : null;

                    ict.thesis.booking.enties.EventRef ref = ict.thesis.booking.enties.EventRef.builder()
                            .id(eventId)
                            .title(title)
                            .startTime(startTime)
                            .endTime(endTime)
                            .venue(venue)
                            .city(city)
                            .bannerUrl(bannerUrl)
                            .syncedAt(java.time.Instant.now())
                            .build();

                    eventRefRepository.save(ref);
                    
                    // Also sync ticket tiers if available to avoid future misses
                    if (body.get("ticketTiers") != null) {
                        List<Map<String, Object>> tiers = (List<Map<String, Object>>) body.get("ticketTiers");
                        for (Map<String, Object> tierMap : tiers) {
                            Long tierId = ((Number) tierMap.get("id")).longValue();
                            String tierName = (String) tierMap.get("name");
                            java.math.BigDecimal tierPrice = new java.math.BigDecimal(tierMap.get("price").toString());
                            Integer quantityAvailable = tierMap.get("quantityAvailable") != null 
                                    ? ((Number) tierMap.get("quantityAvailable")).intValue() : null;

                            if (!ticketTierRefRepository.existsById(tierId)) {
                                ict.thesis.booking.enties.TicketTierRef tierRef = ict.thesis.booking.enties.TicketTierRef.builder()
                                        .id(tierId)
                                        .eventId(eventId)
                                        .eventName(title)
                                        .name(tierName)
                                        .price(tierPrice)
                                        .quantityAvailable(quantityAvailable)
                                        .syncedAt(java.time.Instant.now())
                                        .build();
                                ticketTierRefRepository.save(tierRef);
                            }
                        }
                    }
                    
                    return ref;
                }
            } catch (Exception e) {
                log.error("Failed to dynamically fetch and sync eventId: {}", eventId, e);
            }
            return null;
        });
    }

    public void publishSeatStatus(Long eventId, java.util.List<Long> seatIds, String status) {
        if (eventId == null || seatIds == null || seatIds.isEmpty()) {
            return;
        }
        String json = "";
        try {
            SeatStatusUpdateEvent event = new SeatStatusUpdateEvent(eventId, seatIds, status);
            json = objectMapper.writeValueAsString(event);
            log.info("Publishing seat status update event: {}", json);
            kafkaTemplate.send(seatStatusTopic, eventId.toString(), json);

            // Save to Outbox DB as PROCESSED
            outboxEventRepository.save(ict.thesis.booking.enties.OutboxEvent.builder()
                    .aggregateType("SeatStatus")
                    .aggregateId(eventId.toString())
                    .eventType("SEAT_STATUS_UPDATED")
                    .payload(json)
                    .createdAt(java.time.Instant.now())
                    .status("PROCESSED")
                    .build());
        } catch (Exception e) {
            log.error("Failed to publish seat status update to Kafka", e);
            // Save to Outbox DB as FAILED
            outboxEventRepository.save(ict.thesis.booking.enties.OutboxEvent.builder()
                    .aggregateType("SeatStatus")
                    .aggregateId(eventId.toString())
                    .eventType("SEAT_STATUS_UPDATED")
                    .payload(json.isEmpty() ? "Error serializing payload" : json)
                    .createdAt(java.time.Instant.now())
                    .status("FAILED")
                    .build());
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BookingMessage {
        private String requestId;
        private ict.thesis.booking.dto.BookingDtos.CreateBookingRequest payload;
    }

    public String submitBookingRequest(ict.thesis.booking.dto.BookingDtos.CreateBookingRequest request) {
        String requestId = java.util.UUID.randomUUID().toString();
        log.info("Submitting booking request {} to Kafka", requestId);

        BookingMessage message = new BookingMessage(requestId, request);
        String json = "";
        try {
            json = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(bookingTopic, requestId, json);

            // Save to Outbox DB as PROCESSED
            outboxEventRepository.save(ict.thesis.booking.enties.OutboxEvent.builder()
                    .aggregateType("BookingRequest")
                    .aggregateId(requestId)
                    .eventType("BOOKING_REQUEST_CREATED")
                    .payload(json)
                    .createdAt(java.time.Instant.now())
                    .status("PROCESSED")
                    .build());
        } catch (Exception e) {
            log.error("Failed to serialize BookingMessage for requestId: {}", requestId, e);
            // Save to Outbox DB as FAILED
            outboxEventRepository.save(ict.thesis.booking.enties.OutboxEvent.builder()
                    .aggregateType("BookingRequest")
                    .aggregateId(requestId)
                    .eventType("BOOKING_REQUEST_CREATED")
                    .payload(json.isEmpty() ? "Error serializing payload" : json)
                    .createdAt(java.time.Instant.now())
                    .status("FAILED")
                    .build());
            throw new RuntimeException("Failed to serialize booking request", e);
        }
        return requestId;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Order not found"));

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("orderId", order.getId());
        response.put("orderCode", order.getOrderCode());
        response.put("eventId", order.getEventId());
        response.put("subtotal", order.getSubtotal());
        response.put("totalAmount", order.getTotalAmount());
        response.put("status", order.getStatus().toString());
        response.put("createdAt", order.getCreatedAt() != null ? order.getCreatedAt().toString() : null);

        Map<String, String> tierColorMap = new java.util.HashMap<>();
        try {
            ict.thesis.booking.enties.EventRef event = getOrSyncEvent(order.getEventId());
            if (event != null) {
                response.put("eventTitle", event.getTitle());
                response.put("eventDate", event.getStartTime() != null ? event.getStartTime().toString() : null);
                response.put("eventEndDate", event.getEndTime() != null ? event.getEndTime().toString() : null);
                response.put("eventVenue", event.getVenue());
                response.put("bannerUrl", event.getBannerUrl());
            }
        } catch (Exception e) {
            log.error("Failed to fetch event details locally/fallback", e);
            response.put("eventTitle", "Event " + order.getEventId());
        }

        // Fetch seat codes mapping (with gateway token)
        Map<Long, String> seatCodeMap = new java.util.HashMap<>();
        try {
            String seatMapsUrl = managementServiceUrl + "/api/events/" + order.getEventId() + "/seat-maps";
            HttpEntity<Void> entity = new HttpEntity<>(buildInternalHeaders());
            ResponseEntity<List> seatMapsResponse = restTemplate.exchange(seatMapsUrl, HttpMethod.GET, entity,
                    List.class);
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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<java.util.Map<String, Object>> getCustomerOrders(Long customerId, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Order> ordersPage = orderRepository.findByCustomerOrderByCreatedAtDesc(customerId, pageable);
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();

        java.util.Map<Long, java.util.Map<String, Object>> eventCache = new java.util.HashMap<>();
        HttpHeaders headers = buildInternalHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        for (Order order : ordersPage.getContent()) {
            java.util.Map<String, Object> orderMap = new java.util.HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("orderCode", order.getOrderCode());
            orderMap.put("totalAmount", order.getTotalAmount());
            orderMap.put("status", order.getStatus().toString());
            orderMap.put("createdAt", order.getCreatedAt());
            orderMap.put("eventId", order.getEventId());

            java.util.Map<String, Object> eventData = eventCache.get(order.getEventId());
            if (eventData == null) {
                try {
                    ict.thesis.booking.enties.EventRef event = getOrSyncEvent(order.getEventId());
                    if (event != null) {
                        eventData = new java.util.HashMap<>();
                        eventData.put("title", event.getTitle());
                        eventData.put("bannerUrl", event.getBannerUrl());
                        eventData.put("startTime", event.getStartTime() != null ? event.getStartTime().toString() : null);
                        eventCache.put(order.getEventId(), eventData);
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch event details locally/fallback for order {}", order.getId(), e);
                }
            }

            if (eventData != null) {
                orderMap.put("eventTitle", eventData.get("title"));
                orderMap.put("bannerUrl", eventData.get("bannerUrl"));
                orderMap.put("eventDate", eventData.get("startTime"));
            } else {
                orderMap.put("eventTitle", "Sự kiện " + order.getEventId());
                orderMap.put("bannerUrl", null);
                orderMap.put("eventDate", order.getCreatedAt());
            }
            result.add(orderMap);
        }
        return new org.springframework.data.domain.PageImpl<>(result, pageable, ordersPage.getTotalElements());
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<java.util.Map<String, Object>> getCustomerTickets(Long customerId, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Ticket> ticketsPage = ticketRepository.findByCustomerId(customerId, pageable);
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();

        java.util.Map<Long, java.util.Map<String, Object>> eventCache = new java.util.HashMap<>();
        HttpHeaders headers = buildInternalHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        for (Ticket ticket : ticketsPage.getContent()) {
            java.util.Map<String, Object> ticketMap = new java.util.HashMap<>();
            ticketMap.put("id", ticket.getId());
            ticketMap.put("ticketCode", ticket.getTicketCode());
            ticketMap.put("status", ticket.getStatus() != null ? ticket.getStatus().toString() : null);
            ticketMap.put("seatLabel",
                    ticket.getSeatCode() != null ? ticket.getSeatCode() : ("Seat " + ticket.getSeat()));
            String qrCodeUrl = ticket.getQrCodeUrl();
            if (qrCodeUrl == null || qrCodeUrl.trim().isEmpty()) {
                qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + ticket.getTicketCode();
            }
            ticketMap.put("qrCodeUrl", qrCodeUrl);

            OrderItem item = ticket.getOrderItem();
            if (item != null && item.getOrder() != null) {
                Long eventId = item.getOrder().getEventId();
                java.util.Map<String, Object> eventData = eventCache.get(eventId);
                if (eventData == null) {
                    try {
                        ict.thesis.booking.enties.EventRef event = getOrSyncEvent(eventId);
                        if (event != null) {
                            eventData = new java.util.HashMap<>();
                            eventData.put("eventTitle", event.getTitle());
                            eventData.put("eventDate", event.getStartTime() != null ? event.getStartTime().toString() : null);
                            eventData.put("venue", event.getVenue());
                            eventData.put("bannerUrl", event.getBannerUrl());
                            eventCache.put(eventId, eventData);
                        }
                    } catch (Exception e) {
                        log.error("Failed to fetch event details locally/fallback for ticket {}", ticket.getId(), e);
                    }
                }

                if (eventData != null) {
                    ticketMap.put("eventTitle", eventData.get("eventTitle"));
                    ticketMap.put("eventDate", eventData.get("eventDate"));
                    ticketMap.put("venue", eventData.get("venue"));
                    ticketMap.put("bannerUrl", eventData.get("bannerUrl"));
                } else {
                    ticketMap.put("eventTitle", "Sự kiện " + eventId);
                    ticketMap.put("eventDate", ticket.getExpiresAt());
                    ticketMap.put("venue", "Chưa xác định");
                    ticketMap.put("bannerUrl", null);
                }
            }
            result.add(ticketMap);
        }
        return new org.springframework.data.domain.PageImpl<>(result, pageable, ticketsPage.getTotalElements());
    }

    private String fetchCustomerEmailFromIdentityService(Long customerId) {
        if (customerId == null) {
            return null;
        }
        try {
            String url = "http://identity/api/users/" + customerId;
            HttpHeaders headers = buildInternalHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && body.get("email") != null) {
                return body.get("email").toString();
            }
        } catch (Exception e) {
            log.error("Failed to fetch customer email from Identity service for customerId: {}", customerId, e);
        }
        return null;
    }

    @org.springframework.transaction.annotation.Transactional
    public java.util.Map<String, Object> getTicketDetailByCode(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Ticket not found"));

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        
        // 1. Ticket basic info
        response.put("id", ticket.getId());
        response.put("ticketCode", ticket.getTicketCode());
        response.put("status", ticket.getStatus() != null ? ticket.getStatus().toString() : null);
        response.put("issuedAt", ticket.getIssuedAt());
        response.put("expiresAt", ticket.getExpiresAt());
        
        String qrCodeUrl = ticket.getQrCodeUrl();
        if (qrCodeUrl == null || qrCodeUrl.trim().isEmpty()) {
            qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + ticket.getTicketCode();
        }
        response.put("qrCodeUrl", qrCodeUrl);

        // 2. Seat info
        response.put("seatId", ticket.getSeat());
        response.put("seatLabel", ticket.getSeatCode() != null ? ticket.getSeatCode() : ("Seat " + ticket.getSeat()));

        // 3. Price & Tier details
        OrderItem item = ticket.getOrderItem();
        if (item != null) {
            response.put("originalPrice", item.getOriginalPrice());
            response.put("finalPrice", item.getFinalPrice());
            if (item.getTicketTier() != null) {
                response.put("ticketTierId", item.getTicketTier().getId());
                response.put("ticketTierName", item.getTicketTier().getName());
                response.put("ticketTierColor", "#2563eb");
            }
            
            // 4. Order info
            Order order = item.getOrder();
            if (order != null) {
                response.put("orderId", order.getId());
                response.put("orderCode", order.getOrderCode());
                response.put("customerId", order.getCustomer());
                
                String customerEmail = order.getCustomerEmail();
                if (customerEmail == null || customerEmail.trim().isEmpty()) {
                    customerEmail = fetchCustomerEmailFromIdentityService(order.getCustomer());
                    if (customerEmail != null) {
                        order.setCustomerEmail(customerEmail);
                        orderRepository.save(order);
                    }
                }
                response.put("customerEmail", customerEmail);
                response.put("orderStatus", order.getStatus() != null ? order.getStatus().toString() : null);
                response.put("orderCreatedAt", order.getCreatedAt());

                // 5. Event details (Fetch locally/fallback)
                Long eventId = order.getEventId();
                response.put("eventId", eventId);
                
                try {
                    ict.thesis.booking.enties.EventRef event = getOrSyncEvent(eventId);
                    if (event != null) {
                        response.put("eventTitle", event.getTitle());
                        response.put("eventDate", event.getStartTime() != null ? event.getStartTime().toString() : null);
                        response.put("venue", event.getVenue());
                        response.put("bannerUrl", event.getBannerUrl());
                        response.put("eventCity", event.getCity());
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch event details locally/fallback for ticket detail {}", ticket.getId(), e);
                    response.put("eventTitle", "Sự kiện " + eventId);
                    response.put("eventDate", ticket.getExpiresAt());
                    response.put("venue", "Chưa xác định");
                    response.put("bannerUrl", null);
                }
            }
        }

        // 6. Check-in history
        java.util.List<ict.thesis.booking.enties.Checkin> checkins = checkinRepository.findByTicketId(ticket.getId());
        java.util.List<java.util.Map<String, Object>> checkinList = new java.util.ArrayList<>();
        for (ict.thesis.booking.enties.Checkin checkin : checkins) {
            java.util.Map<String, Object> checkinMap = new java.util.HashMap<>();
            checkinMap.put("id", checkin.getId());
            checkinMap.put("staffId", checkin.getStaff());
            checkinMap.put("method", checkin.getMethod() != null ? checkin.getMethod().toString() : null);
            checkinMap.put("deviceId", checkin.getDeviceId());
            checkinMap.put("checkedInAt", checkin.getCheckedInAt());
            checkinList.add(checkinMap);
        }
        response.put("checkins", checkinList);

        return response;
    }

    public Map<String, Object> getOrganizerDashboardStats(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of(
                "totalRevenue", java.math.BigDecimal.ZERO,
                "totalTicketsSold", 0L,
                "totalCheckins", 0L
            );
        }

        java.math.BigDecimal totalRevenue = orderRepository.sumTotalAmountByEventIdsAndStatus(eventIds, ict.thesis.booking.enties.enums.OrderStatus.PAID);
        long totalTicketsSold = ticketRepository.countTicketsByEventIds(eventIds);
        long totalCheckins = ticketRepository.countTicketsByEventIdsAndStatus(eventIds, ict.thesis.booking.enties.enums.TicketStatus.USED);

        return Map.of(
            "totalRevenue", totalRevenue,
            "totalTicketsSold", totalTicketsSold,
            "totalCheckins", totalCheckins
        );
    }

    public List<Map<String, Object>> getOrganizerDashboardRecentOrders(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        org.springframework.data.domain.Pageable limitFive = org.springframework.data.domain.PageRequest.of(0, 5);
        List<Order> recentOrders = orderRepository.findRecentByEventIds(eventIds, limitFive);

        return recentOrders.stream().map(o -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("orderId", o.getId());
            map.put("orderCode", o.getOrderCode());
            map.put("customerEmail", o.getCustomerEmail());
            map.put("totalAmount", o.getTotalAmount());
            map.put("status", o.getStatus().name());
            map.put("createdAt", o.getCreatedAt().toString());
            return map;
        }).toList();
    }
}
