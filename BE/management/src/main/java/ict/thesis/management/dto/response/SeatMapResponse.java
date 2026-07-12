package ict.thesis.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatMapResponse {
    private Long id;
    private Long eventId;
    private String name;
    private Integer totalRows;
    private Integer totalCols;
    private String layoutJson;
    private Instant createdAt;
    private List<SeatResponse> seats;
}
