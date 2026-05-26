package ict.thesis.management.controller;

import ict.thesis.management.dto.IdentityUserResponse;
import ict.thesis.management.entity.RefUser;
import ict.thesis.management.service.RefUserSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ref-users")
public class RefUserSyncController {
    private final RefUserSyncService refUserSyncService;

    public RefUserSyncController(RefUserSyncService refUserSyncService) {
        this.refUserSyncService = refUserSyncService;
    }

    @PostMapping("/sync")
    public ResponseEntity<RefUser> sync(@RequestBody IdentityUserResponse request) {
        return ResponseEntity.ok(refUserSyncService.upsertFromIdentity(request));
    }
}

