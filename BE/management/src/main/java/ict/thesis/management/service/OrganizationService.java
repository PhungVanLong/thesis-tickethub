package ict.thesis.management.service;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ict.thesis.management.dto.request.OrganizationRequest;
import ict.thesis.management.dto.request.OrganizationVerificationRequest;
import ict.thesis.management.dto.response.OrganizationResponse;
import ict.thesis.management.entity.Organization;
import ict.thesis.management.entity.OrganizationMember;
import ict.thesis.management.entity.enums.OrganizationRole;
import ict.thesis.management.entity.enums.OrganizationStatus;
import ict.thesis.management.repository.OrganizationMemberRepository;
import ict.thesis.management.repository.OrganizationRepository;

@Service
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    public OrganizationService(OrganizationRepository organizationRepository, OrganizationMemberRepository organizationMemberRepository) {
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
    }

    @Transactional
    public OrganizationResponse submitOrganization(Long userId, OrganizationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }

        Organization org = new Organization();
        org.setName(request.getName().trim());
        org.setAbbreviationName(request.getAbbreviationName());
        org.setTaxCode(request.getTaxCode());
        org.setRepresentativeName(request.getRepresentativeName());
        org.setRepresentativePosition(request.getRepresentativePosition());
        org.setHotline(request.getHotline());
        org.setOfficialEmail(request.getOfficialEmail());
        org.setProvinceCity(request.getProvinceCity());
        org.setDistrict(request.getDistrict());
        org.setWardCommune(request.getWardCommune());
        org.setHeadquarterAddress(request.getHeadquarterAddress());
        org.setWebsiteUrl(request.getWebsiteUrl());
        org.setFanpageUrl(request.getFanpageUrl());
        org.setDescription(request.getDescription());
        org.setStatus(OrganizationStatus.PENDING_VERIFY);
        org.setSyncedAt(Instant.now());

        Organization savedOrg = organizationRepository.save(org);

        // Tạo OrganizationMember đầu tiên với vai trò OWNER
        OrganizationMember member = new OrganizationMember();
        member.setUserId(userId);
        member.setOrganization(savedOrg);
        member.setMemberRole(OrganizationRole.OWNER);
        member.setAssignedAt(Instant.now());
        organizationMemberRepository.save(member);

        return toResponse(savedOrg);
    }

    @Transactional
    public OrganizationResponse verifyOrganization(Long organizationId, Long adminUserId, OrganizationVerificationRequest request) {
        if (request == null || adminUserId == null) {
            throw new IllegalArgumentException("adminUserId is required");
        }
        if (request.decision() == null) {
            throw new IllegalArgumentException("decision is required");
        }

        Organization org = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

        org.setStatus(request.decision());
        org.setVerifiedByAdminId(adminUserId);
        org.setVerifiedAt(Instant.now());
        org.setVerificationReason(request.reason());
        org.setSyncedAt(Instant.now());

        Organization saved = organizationRepository.save(org);
        return toResponse(saved);
    }

    private OrganizationResponse toResponse(Organization org) {
        return new OrganizationResponse(
            org.getId(),
            org.getName(),
            org.getAbbreviationName(),
            org.getTaxCode(),
            org.getRepresentativeName(),
            org.getRepresentativePosition(),
            org.getHotline(),
            org.getOfficialEmail(),
            org.getProvinceCity(),
            org.getDistrict(),
            org.getWardCommune(),
            org.getHeadquarterAddress(),
            org.getWebsiteUrl(),
            org.getFanpageUrl(),
            org.getDescription(),
            org.getStatus(),
            org.getVerifiedByAdminId(),
            org.getVerifiedAt(),
            org.getVerificationReason(),
            org.getSyncedAt()
        );
    }
}
