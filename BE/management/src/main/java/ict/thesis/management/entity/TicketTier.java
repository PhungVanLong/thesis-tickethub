package ict.thesis.management.entity;

import java.math.BigDecimal;
import java.time.Instant;

import ict.thesis.management.entity.enums.TierType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ticket_tiers")
public class TicketTier {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_map_id")
    private SeatMap seatMap;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier_type", nullable = false)
    private TierType tierType;

    private BigDecimal price;

    @Column(name = "quantity_total")
    private Integer quantityTotal;

    @Column(name = "quantity_available")
    private Integer quantityAvailable;

    @Column(name = "color_code")
    private String colorCode;

    @Column(name = "sale_start")
    private Instant saleStart;

    @Column(name = "sale_end")
    private Instant saleEnd;
}

