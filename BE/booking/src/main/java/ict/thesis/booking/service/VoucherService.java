package ict.thesis.booking.service;

import ict.thesis.booking.enties.Order;
import ict.thesis.booking.enties.UserRef;
import ict.thesis.booking.enties.VoucherRef;
import ict.thesis.booking.enties.VoucherUsageRef;
import ict.thesis.booking.enties.enums.DiscountType;
import ict.thesis.booking.exception.BookingExceptions.BadRequestException;
import ict.thesis.booking.exception.BookingExceptions.ConflictException;
import ict.thesis.booking.repository.VoucherRefRepository;
import ict.thesis.booking.repository.VoucherUsageRefRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRefRepository voucherRefRepository;
    private final VoucherUsageRefRepository voucherUsageRefRepository;

    public VoucherRef getVoucherByCode(String voucherCode) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return null;
        }
        return voucherRefRepository.findByCode(voucherCode.trim())
                .orElseThrow(() -> new ConflictException("Không tìm thấy voucher code=" + voucherCode));
    }

    public BigDecimal calculateDiscount(VoucherRef voucher, BigDecimal baseAmount, OffsetDateTime now) {
        if (voucher == null) {
            return BigDecimal.ZERO;
        }
        validateVoucher(voucher, now);
        if (baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount;
        if (voucher.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = baseAmount.multiply(normalizeMoney(voucher.getDiscountValue()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = normalizeMoney(voucher.getDiscountValue());
        }
        return discount.compareTo(baseAmount) > 0 ? baseAmount : discount.max(BigDecimal.ZERO);
    }

    @Transactional
    public void recordUsage(VoucherRef voucher,
                            Order order,
                            UserRef user,
                            BigDecimal discountApplied,
                            OffsetDateTime now) {
        if (voucher == null || order == null || user == null) {
            throw new BadRequestException("voucher/order/user là bắt buộc");
        }
        voucherUsageRefRepository.save(VoucherUsageRef.builder()
                .voucher(voucher)
                .order(order)
                .user(user)
                .discountApplied(discountApplied)
                .usedAt(now)
                .build());
    }

    private void validateVoucher(VoucherRef voucher, OffsetDateTime now) {
        if (voucher.getValidUntil() != null && now != null && now.isAfter(voucher.getValidUntil())) {
            throw new ConflictException("Voucher đã hết hạn");
        }
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }
}

