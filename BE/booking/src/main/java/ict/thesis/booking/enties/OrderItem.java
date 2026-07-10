package ict.thesis.booking.enties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "reservation_id")
    // private Reservation reservation;

    @Column(name = "seat_id")
    private Long seat;

    @Column(name = "seat_code")
    private String seatCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_tier_id")
    private TicketTierRef ticketTier;

    @Column(name = "original_price")
    private BigDecimal originalPrice;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "promotion_id")
    // private TicketPromotionRef promotion;

    @Column(name = "final_price")
    private BigDecimal finalPrice;
}
