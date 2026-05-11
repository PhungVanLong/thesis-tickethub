package ict.thesis.management.entity;

import java.time.Instant;

import ict.thesis.management.entity.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="users_ref")
public class RefUser {
    @Id
    private Long id;
    private String email;
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private UserRole role;
    
    private Instant syncedAt;


}
