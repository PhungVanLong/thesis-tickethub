package ict.thesis.management.entity;

import java.math.BigDecimal;
import java.time.Instant;

import ict.thesis.management.entity.enums.TierType;
import jakarta.persistence.*;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Events getEvent() { return event; }
    public void setEvent(Events event) { this.event = event; }
    public SeatMap getSeatMap() { return seatMap; }
    public void setSeatMap(SeatMap seatMap) { this.seatMap = seatMap; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TierType getTierType() { return tierType; }
    public void setTierType(TierType tierType) { this.tierType = tierType; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getQuantityTotal() { return quantityTotal; }
    public void setQuantityTotal(Integer quantityTotal) { this.quantityTotal = quantityTotal; }
    public Integer getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(Integer quantityAvailable) { this.quantityAvailable = quantityAvailable; }
    public Integer getQuantitySold() { return quantitySold; }
    public void setQuantitySold(Integer quantitySold) { this.quantitySold = quantitySold; }
    public String getColorCode() { return colorCode; }
    public void setColorCode(String colorCode) { this.colorCode = colorCode; }
    public Instant getSaleStart() { return saleStart; }
    public void setSaleStart(Instant saleStart) { this.saleStart = saleStart; }
    public Instant getSaleEnd() { return saleEnd; }
    public void setSaleEnd(Instant saleEnd) { this.saleEnd = saleEnd; }
}
