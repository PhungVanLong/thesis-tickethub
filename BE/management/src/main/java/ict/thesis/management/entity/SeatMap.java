package ict.thesis.management.entity;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
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

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "layout_json", columnDefinition = "jsonb")
    private String layoutJson;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "total_cols")
    private Integer totalCols;

    @Column(name = "created_at")
    private Instant createdAt;

}
