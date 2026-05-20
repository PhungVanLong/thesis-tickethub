package ict.thesis.booking.enties;

import java.time.OffsetDateTime;
import java.util.UUID;

import ict.thesis.booking.enties.enums.SeatStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "seat_ref")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatRef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "seat_map_id", columnDefinition = "uuid")
    private UUID seatMapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_tier_id")
    private TicketTierRef ticketTier;

    @Column(name = "seat_code_id", columnDefinition = "uuid")
    private UUID seatCodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SeatStatus status;

    @Column(name = "synced_at")
    private OffsetDateTime syncedAt;
}
