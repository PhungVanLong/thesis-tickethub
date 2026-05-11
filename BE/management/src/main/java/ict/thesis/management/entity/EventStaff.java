package ict.thesis.management.entity;

import java.time.Instant;

import ict.thesis.management.entity.enums.RoleEventStaff;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_staff")
public class EventStaff {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Events event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private RefUser staff;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_event", nullable = false)
    private RoleEventStaff roleInEvent;

    @Column(name = "assigned_at")
    private Instant assignedAt;
}

