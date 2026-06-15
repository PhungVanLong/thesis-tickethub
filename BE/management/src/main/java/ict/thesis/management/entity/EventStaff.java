package ict.thesis.management.entity;

import java.time.Instant;

import jakarta.persistence.*;

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
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "event_staff")
public class EventStaff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Events event;

    @Column(name = "staff_id")
    private Long staff;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_event", nullable = false, length = 100)
    private RoleEventStaff roleInEvent;

    @Column(name = "assigned_at")
    private Instant assignedAt;

}
