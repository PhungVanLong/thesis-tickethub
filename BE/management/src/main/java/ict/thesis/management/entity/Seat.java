package ict.thesis.management.entity;

import ict.thesis.management.entity.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@Entity
@Table(
    name = "seats",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"seat_map_id", "seat_code"})
    }
)
public class Seat implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_map_id")
    private SeatMap seatMap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_tier_id")
    private TicketTier ticketTier;

    @Column(name = "seat_code", length = 50)
    private String seatCode;

    @Column(name = "row_label", length = 10)
    private String rowLabel;

    @Column(name = "col_number")
    private Integer colNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SeatStatus status;

}
