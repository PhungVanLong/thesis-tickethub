package ict.thesis.booking.enties;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import ict.thesis.booking.enties.enums.PromoType;
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


@Entity
@Table(name = "ticket_promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_tier_id")
    private TicketTierRef ticketTier;

    @Column(name = "organizer_id", columnDefinition = "uuid")
    private UUID organizerId;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "promo_type")
    private PromoType promoType;

    @Column(name = "discount_value")
    private BigDecimal discountValue;

    @Column(name = "original_price")
    private BigDecimal originalPrice;

    @Column(name = "promo_price")
    private BigDecimal promoPrice;

    @Column(name = "quantity_limit")
    private Integer quantityLimit;

    @Column(name = "quantity_sold")
    private Integer quantitySold;

    @Column(name = "starts_at")
    private OffsetDateTime startsAt;

    @Column(name = "ends_at")
    private OffsetDateTime endsAt;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
