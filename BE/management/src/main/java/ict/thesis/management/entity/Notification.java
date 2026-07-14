package ict.thesis.management.entity;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "recipient_role", length = 50)
    private String recipientRole;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String message;

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "is_read")
    private boolean read = false;

    @Column(name = "created_at")
    private Instant createdAt;
}
