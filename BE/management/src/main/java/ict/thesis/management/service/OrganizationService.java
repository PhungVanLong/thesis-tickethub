package ict.thesis.management.service;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ict.thesis.management.entity.OutboxEvent;
import ict.thesis.management.entity.enums.OutboxStatus;
import ict.thesis.management.repository.OutboxEventRepository;
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
    private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final EmailService emailService;

    public OrganizationService(OrganizationRepository organizationRepository, 
                               OrganizationMemberRepository organizationMemberRepository,
                               OutboxEventRepository outboxEventRepository,
                               EmailService emailService) {
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.emailService = emailService;
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

        if (org.getStatus() != OrganizationStatus.PENDING_VERIFY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Tổ chức này đã được xử lý duyệt trước đó (Trạng thái hiện tại: " + org.getStatus() + ")");
        }

        org.setStatus(request.decision());
        org.setVerifiedByAdminId(adminUserId);
        org.setVerifiedAt(Instant.now());
        org.setVerificationReason(request.reason());
        org.setSyncedAt(Instant.now());

        Organization saved = organizationRepository.save(org);

        if (request.decision() == OrganizationStatus.ACTIVE) {
            log.info("Organization status is ACTIVE. Looking for OWNER of organizationId: {}", organizationId);
            OrganizationMember ownerMember = organizationMemberRepository.findByOrganizationId(organizationId)
                .stream()
                .filter(m -> m.getMemberRole() == OrganizationRole.OWNER)
                .findFirst()
                .orElse(null);

            if (ownerMember != null) {
                log.info("Found OWNER with userId: {}. Creating OutboxEvent for role promotion...", ownerMember.getUserId());
                try {
                    OutboxEvent outboxEvent = new OutboxEvent();
                    outboxEvent.setAggregateType("Organization");
                    outboxEvent.setAggregateId(organizationId);
                    outboxEvent.setEventType("USER_ROLE_PROMOTE");
                    // Lưu payload dưới dạng JSON Object hợp lệ để tránh lỗi cú pháp cột jsonb của PostgreSQL
                    outboxEvent.setPayload("{\"organizationId\":" + organizationId + ",\"userId\":" + ownerMember.getUserId() + "}");
                    outboxEvent.setStatus(OutboxStatus.PENDING);
                    outboxEvent.setRetryCount(0);
                    outboxEvent.setCreatedAt(Instant.now());
                    
                    outboxEventRepository.save(outboxEvent);
                    log.info("Successfully saved OutboxEvent with payload: {}", outboxEvent.getPayload());
                } catch (Exception e) {
                    log.error("Failed to save OutboxEvent for userId: {}", ownerMember.getUserId(), e);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create outbox event: " + e.getMessage(), e);
                }
            } else {
                log.warn("No OWNER found for organizationId: {}", organizationId);
            }
        }

        // Gửi email thông báo từ chối trực tiếp và bất đồng bộ khi REJECTED
        if (request.decision() == OrganizationStatus.REJECTED) {
            try {
                emailService.sendVerificationEmail(saved.getOfficialEmail(), saved.getName(), false, request.reason());
            } catch (Exception e) {
                log.error("Failed to trigger rejection email sending for organization: {}", saved.getId(), e);
            }
        }

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
