package ict.thesis.management.dto.response;

import ict.thesis.management.entity.enums.TierType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketTierResponse {
    private Long id;
    private String name;
    private TierType tierType;
    private BigDecimal price;
    private Integer quantityTotal;
    private Integer quantityAvailable;
    private Integer quantitySold;
    private String colorCode;
    private Instant saleStart;
    private Instant saleEnd;
    private Long seatMapId;
}
