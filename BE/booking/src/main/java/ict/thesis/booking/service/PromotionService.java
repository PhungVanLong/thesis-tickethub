package ict.thesis.booking.service;

import ict.thesis.booking.enties.OrderItem;
import ict.thesis.booking.enties.PromotionUsage;
import ict.thesis.booking.enties.TicketPromotion;
import ict.thesis.booking.enties.TicketTierRef;
import ict.thesis.booking.enties.UserRef;
import ict.thesis.booking.enties.enums.PromoType;
import ict.thesis.booking.exception.BookingExceptions.BadRequestException;
import ict.thesis.booking.exception.BookingExceptions.ConflictException;
import ict.thesis.booking.repository.PromotionUsageRepository;
import ict.thesis.booking.repository.TicketPromotionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final TicketPromotionRepository ticketPromotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;

    public TicketPromotion getPromotion(Long promotionId) {
        if (promotionId == null) {
            return null;
        }
        return ticketPromotionRepository.findById(promotionId)
                .orElseThrow(() -> new ConflictException("Không tìm thấy khuyến mãi id=" + promotionId));
    }

    public BigDecimal calculateFinalPrice(TicketPromotion promotion,
                                          TicketTierRef ticketTier,
                                          BigDecimal originalPrice,
                                          OffsetDateTime now) {
        if (promotion == null) {
            return originalPrice;
        }
        validatePromotion(promotion, ticketTier, now);
        if (promotion.getPromoPrice() != null) {
            return capPrice(originalPrice, promotion.getPromoPrice());
        }
        if (promotion.getPromoType() == PromoType.PERCENTAGE) {
            BigDecimal discount = originalPrice.multiply(normalizeMoney(promotion.getDiscountValue()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return capPrice(originalPrice, originalPrice.subtract(discount));
        }
        if (promotion.getPromoType() == PromoType.FIXED_AMOUNT) {
            return capPrice(originalPrice, originalPrice.subtract(normalizeMoney(promotion.getDiscountValue())));
        }
        return originalPrice;
    }

    @Transactional
    public void recordUsage(TicketPromotion promotion,
                            OrderItem orderItem,
                            UserRef user,
                            BigDecimal originalPrice,
                            BigDecimal finalPrice,
                            OffsetDateTime now) {
        if (promotion == null || orderItem == null || user == null) {
            throw new BadRequestException("promotion/orderItem/user là bắt buộc");
        }
        OffsetDateTime usedAt = now == null ? OffsetDateTime.now() : now;
        BigDecimal savedAmount = originalPrice.subtract(finalPrice);
        promotion.setQuantitySold(increment(promotion.getQuantitySold()));
        ticketPromotionRepository.save(promotion);
        promotionUsageRepository.save(PromotionUsage.builder()
                .promotion(promotion)
                .orderItem(orderItem)
                .user(user)
                .originalPrice(originalPrice)
                .promoPrice(finalPrice)
                .savedAmount(savedAmount)
                .usedAt(usedAt)
                .build());
    }

    private void validatePromotion(TicketPromotion promotion, TicketTierRef ticketTier, OffsetDateTime now) {
        if (promotion == null) {
            return;
        }
        if (ticketTier == null) {
            throw new BadRequestException("ticketTier là bắt buộc");
        }
        if (promotion.getTicketTier() == null || !Objects.equals(promotion.getTicketTier().getId(), ticketTier.getId())) {
            throw new ConflictException("Khuyến mãi không áp dụng cho hạng vé này");
        }
        if (Boolean.FALSE.equals(promotion.getIsActive())) {
            throw new ConflictException("Khuyến mãi hiện đang tắt");
        }
        if (promotion.getStartsAt() != null && now != null && now.isBefore(promotion.getStartsAt())) {
            throw new ConflictException("Khuyến mãi chưa bắt đầu");
        }
        if (promotion.getEndsAt() != null && now != null && now.isAfter(promotion.getEndsAt())) {
            throw new ConflictException("Khuyến mãi đã hết hạn");
        }
        if (promotion.getQuantityLimit() != null) {
            int sold = promotion.getQuantitySold() == null ? 0 : promotion.getQuantitySold();
            if (sold >= promotion.getQuantityLimit()) {
                throw new ConflictException("Khuyến mãi đã hết số lượng");
            }
        }
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal capPrice(BigDecimal originalPrice, BigDecimal proposedPrice) {
        BigDecimal capped = proposedPrice.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : proposedPrice;
        if (capped.compareTo(originalPrice) > 0) {
            return originalPrice;
        }
        return capped.setScale(2, RoundingMode.HALF_UP);
    }

    private int increment(Integer value) {
        return value == null ? 1 : value + 1;
    }
}

