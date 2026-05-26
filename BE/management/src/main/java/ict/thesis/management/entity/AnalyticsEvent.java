package ict.thesis.management.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "analytics_events")
public class AnalyticsEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", unique = true)
    private Events event;

    @Column(name = "total_tickets_sold")
    private Integer totalTicketsSold;

    @Column(name = "total_checkins")
    private Integer totalCheckins;

    @Column(name = "total_revenue")
    private BigDecimal totalRevenue;

    @Column(name = "tier_breakdown", columnDefinition = "jsonb")
    private String tierBreakdown;

    @Column(name = "snapshot_at")
    private Instant snapshotAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Events getEvent() { return event; }
    public void setEvent(Events event) { this.event = event; }
    public Integer getTotalTicketsSold() { return totalTicketsSold; }
    public void setTotalTicketsSold(Integer totalTicketsSold) { this.totalTicketsSold = totalTicketsSold; }
    public Integer getTotalCheckins() { return totalCheckins; }
    public void setTotalCheckins(Integer totalCheckins) { this.totalCheckins = totalCheckins; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public String getTierBreakdown() { return tierBreakdown; }
    public void setTierBreakdown(String tierBreakdown) { this.tierBreakdown = tierBreakdown; }
    public Instant getSnapshotAt() { return snapshotAt; }
    public void setSnapshotAt(Instant snapshotAt) { this.snapshotAt = snapshotAt; }
}
