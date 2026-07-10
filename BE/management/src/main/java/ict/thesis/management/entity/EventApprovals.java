package ict.thesis.management.entity;

import java.time.Instant;

import ict.thesis.management.entity.enums.ApprovalDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "event_approvals")
public class EventApprovals {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Events event;

    @Column(name = "admin_id")
    private Long adminUser;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ApprovalDecision decision;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "decided_at")
    private Instant decidedAt;

}
