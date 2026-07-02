package ict.thesis.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatRequest {
    @NotBlank(message = "Seat code is required")
    private String seatCode;

    @NotBlank(message = "Row label is required")
    private String rowLabel;

    @NotNull(message = "Column number is required")
    private Integer colNumber;

    @NotBlank(message = "Ticket tier name is required")
    private String ticketTierName;
}
