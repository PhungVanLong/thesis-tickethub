package ict.thesis.management.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
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

}
