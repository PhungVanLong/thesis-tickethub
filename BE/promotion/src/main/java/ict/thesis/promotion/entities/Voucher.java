package ict.thesis.promotion.entities;

import java.math.BigDecimal;
import java.time.Instant;

import ict.thesis.promotion.entities.enums.VoucherApplyOn;
import ict.thesis.promotion.entities.enums.VoucherDiscountType;
import ict.thesis.promotion.entities.enums.VoucherType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "vouchers")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "organizer_id")
    private Long organizerId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "voucher_type", length = 50)
    private VoucherType voucherType;

    @Enumerated(EnumType.STRING)
    @Column(name = "apply_on", length = 50)
    private VoucherApplyOn applyOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 50)
    private VoucherDiscountType discountType;

    // changed precision from 15 to 38 to keep numeric scale consistent with ticket_promotions
    @Column(name = "discount_value", precision = 38, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_value", precision = 38, scale = 2)
    private BigDecimal minOrderValue;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Builder.Default
    @Column(name = "used_count")
    private Integer usedCount = 0;

    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    @Builder.Default
    @Column(name = "combinable")
    private Boolean combinable = false;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;
}
