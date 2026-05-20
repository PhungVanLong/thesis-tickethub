package ict.thesis.booking.service;

import ict.thesis.booking.dto.BookingDtos.BookingItemRequest;
import ict.thesis.booking.dto.BookingDtos.BookingItemResponse;
import ict.thesis.booking.dto.BookingDtos.BookingResponse;
import ict.thesis.booking.dto.BookingDtos.CreateBookingRequest;
import ict.thesis.booking.dto.BookingDtos.PaymentResponse;
import ict.thesis.booking.dto.BookingDtos.TicketResponse;
import ict.thesis.booking.enties.Order;
import ict.thesis.booking.enties.OrderItem;
import ict.thesis.booking.enties.Payment;
import ict.thesis.booking.enties.Ticket;
import ict.thesis.booking.enties.TicketPromotion;
import ict.thesis.booking.enties.TicketTierRef;
import ict.thesis.booking.enties.UserRef;
import ict.thesis.booking.enties.VoucherRef;
import ict.thesis.booking.enties.enums.OrderStatus;
import ict.thesis.booking.exception.BookingExceptions.BadRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingService {

	private final OrderService orderService;
	private final PromotionService promotionService;
	private final VoucherService voucherService;
	private final PaymentService paymentService;
	private final TicketService ticketService;

	@Transactional
	public BookingResponse createBooking(CreateBookingRequest request) {
		validateRequest(request);

		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		String idempotencyKey = normalize(request.idempotencyKey());
		Order existing = orderService.getExistingOrderByIdempotencyKey(idempotencyKey);
		if (existing != null) {
			return getBooking(existing.getId());
		}

		UserRef customer = orderService.getCustomer(request.customerId());
		Order order = orderService.createPendingOrder(customer, idempotencyKey, now);

		List<OrderItem> orderItems = new ArrayList<>();
		BigDecimal subtotal = BigDecimal.ZERO;
		BigDecimal promotionDiscount = BigDecimal.ZERO;

		for (BookingItemRequest itemRequest : request.items()) {
			TicketTierRef ticketTier = orderService.getTicketTier(itemRequest.ticketTierId());
			ict.thesis.booking.enties.SeatRef seat = orderService.getSeat(itemRequest.seatId());
			orderService.reserveSeatAndDecreaseTier(seat, ticketTier, now);

			BigDecimal originalPrice = safeMoney(ticketTier.getPrice());
			TicketPromotion promotion = promotionService.getPromotion(itemRequest.promotionId());
			BigDecimal finalPrice = promotionService.calculateFinalPrice(promotion, ticketTier, originalPrice, now);

			OrderItem orderItem = orderService.createOrderItem(order, seat, ticketTier, promotion, originalPrice, finalPrice);
			orderItems.add(orderItem);

			if (promotion != null) {
				promotionService.recordUsage(promotion, orderItem, customer, originalPrice, finalPrice, now);
			}

			subtotal = subtotal.add(originalPrice);
			promotionDiscount = promotionDiscount.add(originalPrice.subtract(finalPrice));
		}

		VoucherRef voucher = voucherService.getVoucherByCode(request.voucherCode());
		BigDecimal voucherDiscount = voucherService.calculateDiscount(voucher, subtotal.subtract(promotionDiscount), now);
		BigDecimal totalAmount = subtotal.subtract(promotionDiscount).subtract(voucherDiscount);
		if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
			totalAmount = BigDecimal.ZERO;
		}

		Payment payment = paymentService.createPayment(
				order,
				totalAmount,
				request.gatewayName(),
				request.gatewayTxId(),
				request.currency(),
				now
		);

		if (voucher != null) {
			voucherService.recordUsage(voucher, order, customer, voucherDiscount, now);
		}

		List<Ticket> tickets = ticketService.issueTickets(orderItems, now);
		orderService.finalizeOrder(order, subtotal, promotionDiscount, voucherDiscount, totalAmount, now, OrderStatus.PAID);

		return mapToResponse(order, payment, orderItems, tickets);
	}

	@Transactional(readOnly = true)
	public BookingResponse getBooking(Long orderId) {
		Order order = orderService.getOrder(orderId);
		List<OrderItem> orderItems = orderService.getOrderItems(orderId);
		List<Payment> payments = paymentService.getPaymentsByOrderId(orderId);
		List<Ticket> tickets = ticketService.getTicketsByOrderId(orderId);

		Payment payment = payments.isEmpty() ? null : payments.get(0);
		return mapToResponse(order, payment, orderItems, tickets);
	}

	private void validateRequest(CreateBookingRequest request) {
		if (request == null) {
			throw new BadRequestException("Request không được rỗng");
		}
		if (request.customerId() == null) {
			throw new BadRequestException("customerId là bắt buộc");
		}
		if (request.items() == null || request.items().isEmpty()) {
			throw new BadRequestException("Danh sách vé không được rỗng");
		}
		for (BookingItemRequest item : request.items()) {
			if (item == null) {
				throw new BadRequestException("Item đặt vé không hợp lệ");
			}
			if (item.seatId() == null) {
				throw new BadRequestException("seatId là bắt buộc");
			}
			if (item.ticketTierId() == null) {
				throw new BadRequestException("ticketTierId là bắt buộc");
			}
		}
	}

	private BookingResponse mapToResponse(Order order, Payment payment, List<OrderItem> orderItems, List<Ticket> tickets) {
		List<BookingItemResponse> itemResponses = orderItems.stream()
				.sorted(Comparator.comparingLong(OrderItem::getId))
				.map(item -> new BookingItemResponse(
						item.getId(),
						item.getSeat().getId(),
						item.getTicketTier().getId(),
						item.getPromotion() == null ? null : item.getPromotion().getId(),
						safeMoney(item.getOriginalPrice()),
						safeMoney(item.getFinalPrice())
				))
				.toList();

		List<TicketResponse> ticketResponses = tickets.stream()
				.sorted(Comparator.comparingLong(Ticket::getId))
				.map(ticket -> new TicketResponse(
						ticket.getId(),
						ticket.getSeat().getId(),
						ticket.getTicketCode(),
						ticket.getQrCodeUrl(),
						ticket.getStatus(),
						ticket.getIssuedAt()
				))
				.toList();

		PaymentResponse paymentResponse = payment == null ? null : new PaymentResponse(
				payment.getId(),
				payment.getGatewayName(),
				payment.getGatewayTxId(),
				safeMoney(payment.getAmount()),
				payment.getCurrency(),
				payment.getStatus(),
				payment.getPaidAt()
		);

		return new BookingResponse(
				order.getId(),
				order.getOrderCode(),
				order.getCustomer() == null ? null : order.getCustomer().getId(),
				order.getCustomer() == null ? null : order.getCustomer().getFullName(),
				order.getStatus(),
				safeMoney(order.getSubtotal()),
				safeMoney(order.getPromotionDiscount()),
				safeMoney(order.getVoucherDiscount()),
				safeMoney(order.getTotalAmount()),
				order.getCreatedAt(),
				order.getUpdatedAt(),
				paymentResponse,
				itemResponses,
				ticketResponses
		);
	}

	private BigDecimal safeMoney(BigDecimal value) {
		return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}




