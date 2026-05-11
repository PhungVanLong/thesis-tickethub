package ict.thesis.management.entity;

import ict.thesis.management.entity.enums.SeatStatus;
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
@Table(name = "seats")
public class Seat {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_map_id")
    private SeatMap seatMap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_tier_id")
    private TicketTier ticketTier;

    @Column(name = "row_label")
    private String rowLabel;

    @Column(name = "col_number")
    private Integer colNumber;

    @Column(name = "seat_code")
    private String seatCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;
}

