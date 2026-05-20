package ict.thesis.management.entity;

import java.time.Instant;

import ict.thesis.management.entity.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name="users_ref")
@Getter
@Setter
@NoArgsConstructor
@ToString
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
