package ict.thesis.management.entity;

import java.time.Instant;

import jakarta.persistence.*;
import org.hibernate.annotations.ManyToAny;

import ict.thesis.management.entity.enums.EventStatus;

@Entity

public class Events {
    @Id
    private Long id;

    @ManyToOne( fetch= FetchType.LAZY)
    @JoinColumn(name = "organizer_id")
   private RefUser refUser;

    private String title    ;

    private String description; 

    private String venue;

    private String city;
    @Column(name = "location_coords")
    private String locationCoords;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name="end_time")
    private Instant endTime;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private EventStatus status;

    @Column(name = "is_published")
    private boolean isPublished;

    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;

   
    
}
