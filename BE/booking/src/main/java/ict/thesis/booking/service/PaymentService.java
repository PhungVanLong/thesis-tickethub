package ict.thesis.booking.service;

import ict.thesis.booking.enties.Order;
import ict.thesis.booking.enties.Payment;
import ict.thesis.booking.enties.enums.PaymentStatus;
import ict.thesis.booking.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String DEFAULT_GATEWAY_NAME = "mock-gateway";
    private static final String DEFAULT_CURRENCY = "VND";

    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment createPayment(Order order,
                                 BigDecimal amount,
                                 String gatewayName,
                                 String gatewayTxId,
                                 String currency,
                                 OffsetDateTime now) {
        OffsetDateTime paidAt = now == null ? OffsetDateTime.now(ZoneOffset.UTC) : now;
        return paymentRepository.save(Payment.builder()
                .order(order)
                .gatewayName(normalizeOrDefault(gatewayName, DEFAULT_GATEWAY_NAME))
                .gatewayTxId(normalizeOrDefault(gatewayTxId, generateGatewayTxId()))
                .amount(amount)
                .currency(normalizeOrDefault(currency, DEFAULT_CURRENCY))
                .status(PaymentStatus.SUCCESS)
                .gatewayResponse("{\"message\":\"payment accepted\"}")
                .paidAt(paidAt)
                .build());
    }

    private String generateGatewayTxId() {
        return "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String normalizeOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}

