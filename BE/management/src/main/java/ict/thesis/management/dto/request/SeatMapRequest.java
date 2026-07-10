package ict.thesis.management.dto.request;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatMapRequest {
    @NotBlank(message = "Seat map name is required")
    private String name;

    @NotNull(message = "Total rows is required")
    private Integer totalRows;

    @NotNull(message = "Total columns is required")
    private Integer totalCols;

    private String layoutJson;

    private List<SeatRequest> seats;
}
