package ict.thesis.management.entity;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "seat_maps")
public class SeatMap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Events event;

    @Column(length = 255)
    private String name;

    @Column(name = "layout_json", columnDefinition = "jsonb")
    private String layoutJson;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "total_cols")
    private Integer totalCols;

    @Column(name = "created_at")
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Events getEvent() { return event; }
    public void setEvent(Events event) { this.event = event; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLayoutJson() { return layoutJson; }
    public void setLayoutJson(String layoutJson) { this.layoutJson = layoutJson; }
    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }
    public Integer getTotalCols() { return totalCols; }
    public void setTotalCols(Integer totalCols) { this.totalCols = totalCols; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
