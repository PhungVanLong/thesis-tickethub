package ict.thesis.management.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "seat_maps")
public class SeatMap {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Events event;

    private String name;

    @Column(name = "layout_json", columnDefinition = "jsonb")
    private String layoutJson;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "total_cols")
    private Integer totalCols;

    @Column(name = "created_at")
    private Instant createdAt;
}

