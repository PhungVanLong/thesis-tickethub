package ict.thesis.management.service;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ict.thesis.management.dto.request.OrganizerProfileRequest;
import ict.thesis.management.dto.request.OrganizerVerificationRequest;
import ict.thesis.management.dto.response.OrganizerProfileResponse;
import ict.thesis.management.entity.OrganizerProfile;
import ict.thesis.management.entity.enums.OrganizerStatus;
import ict.thesis.management.repository.OrganizerProfileRepository;

@Service
public class OrganizerProfileService {
    private final OrganizerProfileRepository organizerProfileRepository;

    public OrganizerProfileService(OrganizerProfileRepository organizerProfileRepository) {
        this.organizerProfileRepository = organizerProfileRepository;
    }
//1. SIGN IN PROFILE ORGANIZER
    @Transactional
    public OrganizerProfileResponse submitProfile(OrganizerProfileRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.getOrganizationName() == null || request.getOrganizationName().isBlank()) {
            throw new IllegalArgumentException("organizationName is required");
        }

        OrganizerProfile profile = organizerProfileRepository.findByUserId(request.getUserId())
            .orElseGet(OrganizerProfile::new);
        profile.setUserId(request.getUserId());
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
    
        // Mặc định đăng ký mới hoặc đăng ký lại sẽ chờ duyệt
        profile.setStatus(OrganizerStatus.PENDING);
        profile.setVerifiedByAdminId(null);
        profile.setVerifiedAt(null);
        profile.setVerificationReason(null);
        profile.setSyncedAt(Instant.now());

        OrganizerProfile saved = organizerProfileRepository.save(profile);
        return toResponse(saved);
    }
//Upd
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

        profile.setStatus(request.decision());
        profile.setVerifiedByAdminId(request.adminUserId());
        profile.setVerifiedAt(Instant.now());
        profile.setVerificationReason(request.reason());

        OrganizerProfile saved = organizerProfileRepository.save(profile);
        return toResponse(saved);
    }

    private OrganizerProfileResponse toResponse(OrganizerProfile profile) {
        return new OrganizerProfileResponse(
            profile.getUserId(),
            profile.getUserId(),
            profile.getOrganizationName(),
            profile.getStatus(),
            profile.getVerifiedByAdminId(),
            profile.getVerifiedAt(),
            profile.getVerificationReason(),
            null
        );
    }
}

