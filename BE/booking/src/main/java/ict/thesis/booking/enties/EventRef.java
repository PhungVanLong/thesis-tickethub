package ict.thesis.booking.enties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "events_ref")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRef {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "venue")
    private String venue;

    @Column(name = "city")
    private String city;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Column(name = "synced_at")
    private Instant syncedAt;
}
