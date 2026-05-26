package ict.thesis.booking.service;

import ict.thesis.booking.enties.Order;
import ict.thesis.booking.enties.OrderItem;
import ict.thesis.booking.enties.SeatRef;
import ict.thesis.booking.enties.TicketPromotion;
import ict.thesis.booking.enties.TicketTierRef;
import ict.thesis.booking.enties.UserRef;
import ict.thesis.booking.enties.enums.OrderStatus;
import ict.thesis.booking.enties.enums.SeatStatus;
import ict.thesis.booking.exception.BookingExceptions.BadRequestException;
import ict.thesis.booking.exception.BookingExceptions.ConflictException;
import ict.thesis.booking.exception.BookingExceptions.NotFoundException;
import ict.thesis.booking.repository.OrderItemRepository;
import ict.thesis.booking.repository.OrderRepository;
import ict.thesis.booking.repository.SeatRefRepository;
import ict.thesis.booking.repository.TicketTierRefRepository;
import ict.thesis.booking.repository.UserRefRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRefRepository userRefRepository;
    private final SeatRefRepository seatRefRepository;
    private final TicketTierRefRepository ticketTierRefRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public UserRef getCustomer(Long customerId) {
        if (customerId == null) {
            throw new BadRequestException("customerId là bắt buộc");
        }
        return userRefRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khách hàng id=" + customerId));
    }

    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng id=" + orderId));
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrder_Id(orderId);
    }

    public SeatRef getSeat(Long seatId) {
        if (seatId == null) {
            throw new BadRequestException("seatId là bắt buộc");
        }
        return seatRefRepository.findById(seatId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ghế id=" + seatId));
    }

    public TicketTierRef getTicketTier(Long ticketTierId) {
        if (ticketTierId == null) {
            throw new BadRequestException("ticketTierId là bắt buộc");
        }
        return ticketTierRefRepository.findById(ticketTierId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hạng vé id=" + ticketTierId));
    }

    public Order getExistingOrderByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        return orderRepository.findByIdempotencyKey(idempotencyKey.trim()).orElse(null);
    }

    @Transactional
    public Order createPendingOrder(UserRef customer, String idempotencyKey, OffsetDateTime now) {
        if (customer == null) {
            throw new BadRequestException("customer là bắt buộc");
        }
        OffsetDateTime createdAt = now == null ? OffsetDateTime.now(ZoneOffset.UTC) : now;
        return orderRepository.save(Order.builder()
                .customer(customer)
                .orderCode(generateOrderCode())
                .subtotal(BigDecimal.ZERO)
                .promotionDiscount(BigDecimal.ZERO)
                .voucherDiscount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .status(OrderStatus.PENDING)
                .idempotencyKey(normalize(idempotencyKey))
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build());
    }

    @Transactional
    public OrderItem createOrderItem(Order order,
                                     SeatRef seat,
                                     TicketTierRef ticketTier,
                                     TicketPromotion promotion,
                                     BigDecimal originalPrice,
                                     BigDecimal finalPrice) {
        if (order == null) {
            throw new BadRequestException("order là bắt buộc");
        }
        if (seat == null) {
            throw new BadRequestException("seat là bắt buộc");
        }
        if (ticketTier == null) {
            throw new BadRequestException("ticketTier là bắt buộc");
        }
        return orderItemRepository.save(OrderItem.builder()
                .order(order)
                .seat(seat)
                .ticketTier(ticketTier)
                .promotion(promotion)
                .originalPrice(originalPrice)
                .finalPrice(finalPrice)
                .build());
    }

    @Transactional
    public void reserveSeatAndDecreaseTier(SeatRef seat, TicketTierRef ticketTier, OffsetDateTime now) {
        if (seat == null || ticketTier == null) {
            throw new BadRequestException("seat/ticketTier là bắt buộc");
        }
        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new ConflictException("Ghế " + seat.getId() + " hiện không còn khả dụng");
        }
        if (ticketTier.getQuantityAvailable() == null || ticketTier.getQuantityAvailable() <= 0) {
            throw new ConflictException("Hạng vé " + ticketTier.getId() + " đã hết số lượng");
        }
        seat.setStatus(SeatStatus.BOOKED);
        seat.setSyncedAt(now == null ? OffsetDateTime.now(ZoneOffset.UTC) : now);
        seatRefRepository.save(seat);

        ticketTier.setQuantityAvailable(ticketTier.getQuantityAvailable() - 1);
        ticketTier.setSyncedAt(now == null ? OffsetDateTime.now(ZoneOffset.UTC) : now);
        ticketTierRefRepository.save(ticketTier);
    }

    @Transactional
    public void finalizeOrder(Order order,
                              BigDecimal subtotal,
                              BigDecimal promotionDiscount,
                              BigDecimal voucherDiscount,
                              BigDecimal totalAmount,
                              OffsetDateTime now,
                              OrderStatus status) {
        if (order == null) {
            throw new BadRequestException("order là bắt buộc");
        }
        OffsetDateTime updatedAt = now == null ? OffsetDateTime.now(ZoneOffset.UTC) : now;
        order.setSubtotal(subtotal);
        order.setPromotionDiscount(promotionDiscount);
        order.setVoucherDiscount(voucherDiscount);
        order.setTotalAmount(totalAmount);
        order.setStatus(status == null ? OrderStatus.PENDING : status);
        order.setUpdatedAt(updatedAt);
        orderRepository.save(order);
    }

    private String generateOrderCode() {
        String code;
        do {
            code = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (orderRepository.findByOrderCode(code).isPresent());
        return code;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}


