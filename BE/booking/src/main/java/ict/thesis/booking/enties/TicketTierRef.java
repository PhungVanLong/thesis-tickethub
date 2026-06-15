package ict.thesis.booking.enties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ticket_tiers_ref")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketTierRef {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "version")
    private Long version;

    @Column(name = "name")
    private String name;

    @Column(name = "price" , nullable = false )
    private BigDecimal price;

    @Column(name = "sale_at")
    private Instant saleAt;

    @Column(name = "sale_end")
    private Instant saleEnd;

    @Column(name = "quantity_available")
    private Integer quantityAvailable;

    @Column(name = "synced_at")
    private Instant syncedAt;
}
