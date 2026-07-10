package ict.thesis.management.entity;

import java.math.BigDecimal;
import java.time.Instant;

import ict.thesis.management.entity.enums.TierType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "ticket_tiers")
public class TicketTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Events event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_map_id")
    private SeatMap seatMap;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier_type", length = 50)
    private TierType tierType;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "quantity_total")
    private Integer quantityTotal;

    @Column(name = "quantity_available")
    private Integer quantityAvailable;

    @Column(name = "quantity_sold")
    private Integer quantitySold;

    @Column(name = "color_code", length = 10)
    private String colorCode;

    @Column(name = "sale_start")
    private Instant saleStart;

    @Column(name = "sale_end")
    private Instant saleEnd;

}
