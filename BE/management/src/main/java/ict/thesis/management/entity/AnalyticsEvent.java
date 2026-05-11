package ict.thesis.management.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "analytics_events")
public class AnalyticsEvent {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
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
}

