package ict.thesis.management.dto.request;

import java.math.BigDecimal;
import java.time.Instant;
import ict.thesis.management.entity.enums.TierType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
    @DecimalMin(value = "0.0", message = "Price must be greater than or equal to 0")
    private BigDecimal price;

    @NotNull(message = "Quantity total is required")
    @Min(value = 1, message = "Quantity total must be at least 1")
    private Integer quantityTotal;

    private String colorCode;
    private Instant saleStart;
    private Instant saleEnd;
}
