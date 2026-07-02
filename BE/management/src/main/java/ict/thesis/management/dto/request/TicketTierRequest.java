package ict.thesis.management.dto.request;

import java.math.BigDecimal;
import java.time.Instant;
import ict.thesis.management.entity.enums.TierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketTierRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Tier type is required")
    private TierType tierType;

    @NotNull(message = "Price is required")
    private BigDecimal price;

    @NotNull(message = "Quantity total is required")
    private Integer quantityTotal;

    private String colorCode;
    private Instant saleStart;
    private Instant saleEnd;
}
