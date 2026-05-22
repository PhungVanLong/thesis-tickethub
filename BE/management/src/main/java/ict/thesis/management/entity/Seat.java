package ict.thesis.management.entity;

import ict.thesis.management.entity.enums.SeatStatus;
import jakarta.persistence.*;
import java.io.Serializable;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SeatMap getSeatMap() { return seatMap; }
    public void setSeatMap(SeatMap seatMap) { this.seatMap = seatMap; }
    public TicketTier getTicketTier() { return ticketTier; }
    public void setTicketTier(TicketTier ticketTier) { this.ticketTier = ticketTier; }
    public String getSeatCode() { return seatCode; }
    public void setSeatCode(String seatCode) { this.seatCode = seatCode; }
    public String getRowLabel() { return rowLabel; }
    public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }
    public Integer getColNumber() { return colNumber; }
    public void setColNumber(Integer colNumber) { this.colNumber = colNumber; }
    public SeatStatus getStatus() { return status; }
    public void setStatus(SeatStatus status) { this.status = status; }
}
