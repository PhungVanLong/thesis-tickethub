package ict.thesis.management.entity;

import ict.thesis.management.entity.enums.ApprovalDecision;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class EventApprovals {
    @Id
    private Long id;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Events event;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="admin_id")
    private RefUser refUser;

    @Enumerated(EnumType.STRING)
    private ApprovalDecision decision;

    private String reason;

    @Column(name = "decided_at")
    private Instant decidedAt;
}
