package ict.thesis.management.service;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import ict.thesis.management.dto.IdentityUserResponse;
import ict.thesis.management.dto.request.OrganizerProfileRequest;
import ict.thesis.management.dto.request.OrganizerVerificationRequest;
import ict.thesis.management.dto.response.OrganizerProfileResponse;
import ict.thesis.management.entity.OrganizerProfile;
import ict.thesis.management.entity.RefUser;
import ict.thesis.management.entity.enums.OrganizerStatus;
import ict.thesis.management.entity.enums.UserRole;
import ict.thesis.management.repository.OrganizerProfileRepository;
import ict.thesis.management.repository.RefUserRepository;

@Service
public class OrganizerProfileService {
    private final OrganizerProfileRepository organizerProfileRepository;
    private final RefUserRepository refUserRepository;
    private final RefUserSyncService refUserSyncService;
    private final RestTemplate restTemplate;

    @Value("${identity.base-url:http://localhost:8081}")
    private String identityBase;

    public OrganizerProfileService(
        OrganizerProfileRepository organizerProfileRepository,
        RefUserRepository refUserRepository,
        RefUserSyncService refUserSyncService,
        RestTemplate restTemplate
    ) {
        this.organizerProfileRepository = organizerProfileRepository;
        this.refUserRepository = refUserRepository;
        this.refUserSyncService = refUserSyncService;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public OrganizerProfileResponse submitProfile(OrganizerProfileRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.getOrganizationName() == null || request.getOrganizationName().isBlank()) {
            throw new IllegalArgumentException("organizationName is required");
        }

        RefUser user = loadOrSyncRefUser(request.getUserId());

        OrganizerProfile profile = organizerProfileRepository.findByUserId(user.getId())
            .orElseGet(OrganizerProfile::new);
        profile.setId(user.getId());
        profile.setUser(user);
        profile.setOrganizationName(request.getOrganizationName().trim());
        profile.setAbbreviationName(request.getAbbreviationName());
        profile.setTaxCode(request.getTaxCode());
        profile.setRepresentativeName(request.getRepresentativeName());
        profile.setRepresentativePosition(request.getRepresentativePosition());
        profile.setHotline(request.getHotline());
        profile.setOfficialEmail(request.getOfficialEmail());
        profile.setProvinceCity(request.getProvinceCity());
        profile.setDistrict(request.getDistrict());
        profile.setWardCommune(request.getWardCommune());
        profile.setHeadquarterAddress(request.getHeadquarterAddress());
        profile.setWebsiteUrl(request.getWebsiteUrl());
        profile.setFanpageUrl(request.getFanpageUrl());
        profile.setDescription(request.getDescription());
        profile.setStatus(OrganizerStatus.PENDING);
        profile.setVerifiedByAdminId(null);
        profile.setVerifiedAt(null);
        profile.setVerificationReason(null);
        profile.setSyncedAt(Instant.now());

        OrganizerProfile saved = organizerProfileRepository.save(profile);
        return toResponse(saved, user);
    }

    @Transactional
    public OrganizerProfileResponse verifyProfile(Long userId, OrganizerVerificationRequest request) {
        if (request == null || request.adminUserId() == null) {
            throw new IllegalArgumentException("adminUserId is required");
        }
        if (request.decision() == null) {
            throw new IllegalArgumentException("decision is required");
        }
        if (request.decision() == OrganizerStatus.PENDING) {
            throw new IllegalArgumentException("decision must be APPROVED, REJECTED, or SUSPENDED");
        }

        OrganizerProfile profile = organizerProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organizer profile not found"));

        RefUser admin = refUserRepository.findById(request.adminUserId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));
        if (admin.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin user is required");
        }

        profile.setStatus(request.decision());
        profile.setVerifiedByAdminId(admin.getId());
        profile.setVerifiedAt(Instant.now());
        profile.setVerificationReason(request.reason());

        RefUser organizerUser = profile.getUser();
        if (request.decision() == OrganizerStatus.APPROVED && organizerUser != null && organizerUser.getRole() == UserRole.CUSTOMER) {
            IdentityUserResponse promoted = promoteOrganizer(organizerUser.getId());
            organizerUser = refUserSyncService.upsertFromIdentity(promoted);
        }

        OrganizerProfile saved = organizerProfileRepository.save(profile);
        return toResponse(saved, organizerUser);
    }

    private RefUser loadOrSyncRefUser(Long userId) {
        RefUser local = refUserRepository.findById(userId).orElse(null);
        if (local != null) {
            return local;
        }

        try {
            IdentityUserResponse response = restTemplate.getForObject(identityBase + "/api/users/{id}", IdentityUserResponse.class, userId);
            if (response == null) {
                throw new IllegalArgumentException("User not found in identity: " + userId);
            }
            if (!response.isActive() || !response.isVerified()) {
                throw new IllegalArgumentException("User is not active or not verified in identity");
            }
            return refUserSyncService.upsertFromIdentity(response);
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("User not found in identity: " + userId);
        }
    }

    private IdentityUserResponse promoteOrganizer(Long userId) {
        try {
            Map<String, Object> body = Map.of("role", "ORGANIZER");
            IdentityUserResponse response = restTemplate.exchange(
                identityBase + "/api/users/{id}",
                HttpMethod.PUT,
                new HttpEntity<>(body),
                IdentityUserResponse.class,
                userId
            ).getBody();
            if (response == null) {
                throw new IllegalArgumentException("Unable to promote organizer in identity");
            }
            return response;
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("Unable to promote organizer in identity");
        }
    }

    private OrganizerProfileResponse toResponse(OrganizerProfile profile, RefUser user) {
        return new OrganizerProfileResponse(
            profile.getId(),
            user == null ? null : user.getId(),
            profile.getOrganizationName(),
            profile.getStatus(),
            profile.getVerifiedByAdminId(),
            profile.getVerifiedAt(),
            profile.getVerificationReason(),
            user == null || user.getRole() == null ? null : user.getRole().name()
        );
    }
}

