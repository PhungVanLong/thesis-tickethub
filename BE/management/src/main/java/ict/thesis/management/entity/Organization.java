package ict.thesis.management.entity;

import java.time.Instant;

import ict.thesis.management.entity.enums.OrganizationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "abbreviation_name", length = 50)
    private String abbreviationName;

    @Column(name = "tax_code", unique = true, length = 13) 
    private String taxCode;

    @Column(name = "representative_name")
    private String representativeName;

    @Column(name = "representative_position")
    private String representativePosition;

    @Column(name = "hotline", length = 15)
    private String hotline;

    @Column(name = "official_email")
    private String officialEmail;

    @Column(name = "province_city", length = 100)
    private String provinceCity;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "ward_commune", length = 100)
    private String wardCommune;

    @Column(name = "headquarter_address", columnDefinition = "TEXT")
    private String headquarterAddress;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "fanpage_url")
    private String fanpageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "synced_at")
    private Instant syncedAt;

    @Column(name = "verified_by_admin_id")
    private Long verifiedByAdminId;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verification_reason", columnDefinition = "TEXT")
    private String verificationReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrganizationStatus status = OrganizationStatus.PENDING_VERIFY;
}
