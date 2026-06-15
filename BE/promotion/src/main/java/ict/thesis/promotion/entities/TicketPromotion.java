
package ict.thesis.promotion.entities;

import java.math.BigDecimal;
import java.time.Instant;

import ict.thesis.promotion.entities.enums.PromoType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "ticket_promotions")
public class TicketPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_tier_id")
    private TicketTierRef ticketTier;

    @Column(name = "organizer_id")
    private Long organizerId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "promo_type", length = 50)
    private PromoType promoType;

    // changed precision from 15 to 38 to support larger numeric values
    @Column(name = "discount_value", precision = 38, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "original_price", precision = 38, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "promo_price", precision = 38, scale = 2)
    private BigDecimal promoPrice;

    @Column(name = "quantity_limit")
    private Integer quantityLimit;

    @Builder.Default
    @Column(name = "quantity_sold")
    private Integer quantitySold = 0;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;
}
