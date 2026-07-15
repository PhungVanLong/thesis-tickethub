package ict.thesis.booking.service;

import ict.thesis.booking.enties.Order;
import ict.thesis.booking.enties.enums.OrderStatus;
import ict.thesis.booking.repository.OrderItemRepository;
import ict.thesis.booking.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookingService bookingService;
    private final TicketService ticketService;
    private final ict.thesis.booking.config.VNPayConfig vnPayConfig;
    private final PayPalService payPalService;

    @Value("${management.service.url}")
    private String managementServiceUrl;

    public void completeMockPayment(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(OrderStatus.PAID);
            order.setUpdatedAt(Instant.now());
            orderRepository.save(order);
            log.info("Mock payment completed for order ID: {}", orderId);

            // Generate tickets and send Kafka notification
            ticketService.generateTicketsAndNotify(order);

            List<Long> seatIds = orderItemRepository.findByOrderId(orderId).stream()
                    .map(ict.thesis.booking.enties.OrderItem::getSeat)
                    .filter(Objects::nonNull)
                    .toList();

            bookingService.publishSeatStatus(order.getEventId(), seatIds, "SOLD");
        });
    }

    public void cancelMockPayment(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(Instant.now());
            orderRepository.save(order);
            log.info("Mock payment cancelled for order ID: {}", orderId);

            List<Long> seatIds = orderItemRepository.findByOrderId(orderId).stream()
                    .map(ict.thesis.booking.enties.OrderItem::getSeat)
                    .filter(Objects::nonNull)
                    .toList();

            bookingService.publishSeatStatus(order.getEventId(), seatIds, "AVAILABLE");
        });
    }

    public String createVNPayPaymentUrl(Long orderId, jakarta.servlet.http.HttpServletRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        String vnp_Version = vnPayConfig.getVnpVersion();
        String vnp_Command = vnPayConfig.getVnpCommand();
        String vnp_TxnRef = order.getId().toString() + "_" + UUID.randomUUID().toString().substring(0, 8);
        String vnp_IpAddr = vnPayConfig.getIpAddress(request);
        String vnp_TmnCode = vnPayConfig.getVnpCode();

        // Amount must be multiplied by 100 as per VNPay spec
        long amount = order.getTotalAmount().multiply(new BigDecimal("100")).longValue();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang: " + order.getOrderCode());
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getVnpReturnUrl());
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build Hash Data
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII.toString()));
                    // Build Query
                    query.append(java.net.URLEncoder.encode(fieldName, java.nio.charset.StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII.toString()));
                } catch (java.io.UnsupportedEncodingException e) {
                    log.error("Encoding failed", e);
                }
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = vnPayConfig.hashAllFields(vnp_Params);
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return vnPayConfig.getVnpPayUrl() + "?" + queryUrl;
    }

    public boolean processVNPayCallback(Map<String, String> params) {
        log.info("Processing VNPay Callback with params: {}", params);

        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        String vnp_TxnRef = params.get("vnp_TxnRef");

        if (vnp_TxnRef == null) {
            return false;
        }

        // vnp_TxnRef is formatted as {orderId}_{randomString}
        Long orderId = Long.parseLong(vnp_TxnRef.split("_")[0]);

        if ("00".equals(vnp_ResponseCode)) {
            completeMockPayment(orderId);
            log.info("VNPay Payment Successful for order ID: {}", orderId);
            return true;
        } else {
            cancelMockPayment(orderId);
            log.warn("VNPay Payment Failed for order ID: {}, response code: {}", orderId, vnp_ResponseCode);
            return false;
        }
    }

    public String createPayPalPaymentUrl(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return payPalService.createPayPalOrder(order);
    }

    public boolean processPayPalCallback(Long orderId, String token) {
        log.info("Processing PayPal Callback for order ID: {}, token: {}", orderId, token);
        boolean success = payPalService.capturePayPalOrder(token);
        if (success) {
            completeMockPayment(orderId);
            log.info("PayPal Payment Successful for order ID: {}", orderId);
            return true;
        } else {
            cancelMockPayment(orderId);
            log.warn("PayPal Payment Failed for order ID: {}", orderId);
            return false;
        }
    }
}
