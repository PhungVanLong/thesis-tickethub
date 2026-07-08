package ict.thesis.management.dto.response;

import ict.thesis.management.entity.enums.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {
    private Long id;
    private String seatCode;
    private String rowLabel;
    private Integer colNumber;
    private SeatStatus status;
    private Long ticketTierId;
    private String ticketTierName;
    private String colorCode;
}
