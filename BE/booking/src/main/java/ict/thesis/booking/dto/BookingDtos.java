package ict.thesis.booking.dto;

import ict.thesis.booking.enties.enums.OrderStatus;
import ict.thesis.booking.enties.enums.PaymentStatus;
import ict.thesis.booking.enties.enums.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class BookingDtos {

    private BookingDtos() {
    }

    public record BookingItemRequest(Long seatId, Long ticketTierId, Long promotionId) {
    }

    public record CreateBookingRequest(
            Long customerId,
            String idempotencyKey,
            String voucherCode,
            String gatewayName,
            String gatewayTxId,
            String currency,
            List<BookingItemRequest> items
    ) {
    }

    public record BookingItemResponse(
            Long orderItemId,
            Long seatId,
            Long ticketTierId,
            Long promotionId,
            BigDecimal originalPrice,
            BigDecimal finalPrice
    ) {
    }

    public record PaymentResponse(
            Long paymentId,
            String gatewayName,
            String gatewayTxId,
            BigDecimal amount,
            String currency,
            PaymentStatus status,
            Instant paidAt
    ) {
    }

    public record TicketResponse(
            Long ticketId,
            Long seatId,
            String ticketCode,
            String qrCodeUrl,
            TicketStatus status,
            Instant issuedAt
    ) {
    }

    public record BookingResponse(
            Long orderId,
            String orderCode,
            Long customerId,
            String customerName,
            OrderStatus status,
            BigDecimal subtotal,
            BigDecimal promotionDiscount,
            BigDecimal voucherDiscount,
            BigDecimal totalAmount,
            Instant createdAt,
            Instant updatedAt,
            PaymentResponse payment,
            List<BookingItemResponse> items,
            List<TicketResponse> tickets
    ) {
    }

    public record ApiErrorResponse(
            Instant timestamp,
            int status,
            String error,
            String message,
            String path,
            Object details
    ) {
    }
}

