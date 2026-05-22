//package ict.thesis.identity.entity;
//
//import java.math.BigDecimal;
//import java.time.Instant;
//
//import ict.thesis.identity.entity.enums.DiscountType;
//import ict.thesis.identity.entity.enums.PromoApplyOn;
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.EnumType;
//import jakarta.persistence.Enumerated;
//import jakarta.persistence.FetchType;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.Index;
//import jakarta.persistence.JoinColumn;
//import jakarta.persistence.ManyToOne;
//import jakarta.persistence.Table;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//@Entity
//@Table(
//    name = "vouchers",
//    indexes = {
//        @Index(name = "idx_vouchers_code", columnList = "code", unique = true),
//        @Index(name = "idx_vouchers_organizer_id", columnList = "organizer_id")
//    }
//)
//@Getter
//@Setter
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class Voucher {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "organizer_id")
//    private User organizer;
//
//    @Column(nullable = false, unique = true)
//    private String code;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "discount_type", nullable = false)
//    private DiscountType discountType;
//
//    @Column(name = "discount_value", nullable = false, precision = 19, scale = 2)
//    private BigDecimal discountValue;
//
//    @Column(name = "min_order_value", precision = 19, scale = 2)
//    private BigDecimal minOrderValue;
//
//    @Column(name = "usage_limit")
//    private Integer usageLimit;
//
//    @Column(name = "used_count")
//    private Integer usedCount;
//
//    @Column(name = "valid_from", nullable = false)
//    private Instant validFrom;
//
//    @Column(name = "valid_until", nullable = false)
//    private Instant validUntil;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "apply_on", nullable = false)
//    private PromoApplyOn applyOn;
//
//    @Column(name = "is_active", nullable = false)
//    private boolean active;
//}
