package ict.thesis.management.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ict.thesis.management.dto.IdentityUserResponse;
import ict.thesis.management.entity.RefUser;
import ict.thesis.management.entity.enums.UserRole;
import ict.thesis.management.repository.RefUserRepository;

@Service
public class RefUserSyncService {
    private final RefUserRepository refUserRepository;

    public RefUserSyncService(RefUserRepository refUserRepository) {
        this.refUserRepository = refUserRepository;
    }

    @Transactional
    public RefUser upsertFromIdentity(IdentityUserResponse iu) {
        if (iu == null || iu.getId() == null) {
            throw new IllegalArgumentException("Identity user payload is required");
        }

        RefUser refUser = refUserRepository.findById(iu.getId()).orElseGet(RefUser::new);
        refUser.setId(iu.getId());
        refUser.setEmail(iu.getEmail());
        refUser.setFullName(iu.getFullName());
        refUser.setRole(mapRole(iu.getRole()));
        refUser.setSyncedAt(Instant.now());
        return refUserRepository.save(refUser);
    }

    private UserRole mapRole(String role) {
        if (role == null || role.isBlank()) {
            return UserRole.CUSTOMER;
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (Exception ex) {
            return UserRole.CUSTOMER;
        }
    }
}

