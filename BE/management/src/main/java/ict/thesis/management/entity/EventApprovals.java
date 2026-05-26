package ict.thesis.management.entity;

import ict.thesis.management.entity.enums.ApprovalDecision;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "event_approvals")
public class EventApprovals {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Events event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private RefUser adminUser;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ApprovalDecision decision;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "decided_at")
    private Instant decidedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Events getEvent() { return event; }
    public void setEvent(Events event) { this.event = event; }
    public RefUser getAdminUser() { return adminUser; }
    public void setAdminUser(RefUser adminUser) { this.adminUser = adminUser; }
    public ApprovalDecision getDecision() { return decision; }
    public void setDecision(ApprovalDecision decision) { this.decision = decision; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
}
