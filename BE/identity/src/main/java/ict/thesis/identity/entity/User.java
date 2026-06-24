package ict.thesis.identity.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import ict.thesis.identity.entity.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true)
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(nullable = false)
    private String role; // Comma-separated roles, e.g. "CUSTOMER" or "CUSTOMER,ORGANIZER"

    public java.util.Set<UserRole> getRoles() {
        if (this.role == null || this.role.isBlank()) {
            return java.util.Set.of(UserRole.CUSTOMER);
        }
        return java.util.Arrays.stream(this.role.split(","))
                     .map(String::trim)
                     .map(UserRole::valueOf)
                     .collect(java.util.stream.Collectors.toSet());
    }

    public void setRoles(java.util.Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            this.role = UserRole.CUSTOMER.name();
        } else {
            this.role = roles.stream()
                             .map(Enum::name)
                             .collect(java.util.stream.Collectors.joining(","));
        }
    }

    public void addRole(UserRole roleToRequest) {
        java.util.Set<UserRole> currentRoles = new java.util.HashSet<>(getRoles());
        currentRoles.add(roleToRequest);
        setRoles(currentRoles);
    }

    public void removeRole(UserRole roleToRemove) {
        java.util.Set<UserRole> currentRoles = new java.util.HashSet<>(getRoles());
        currentRoles.remove(roleToRemove);
        setRoles(currentRoles);
    }

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private Instant resetTokenExpiry;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
