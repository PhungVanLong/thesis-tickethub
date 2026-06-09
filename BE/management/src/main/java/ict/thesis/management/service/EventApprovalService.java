// package ict.thesis.management.service;

// import java.time.Instant;
// import java.util.Map;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.HttpEntity;
// import org.springframework.http.HttpMethod;
// import org.springframework.http.HttpStatus;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
// import org.springframework.web.client.RestClientException;
// import org.springframework.web.client.RestTemplate;
// import org.springframework.web.server.ResponseStatusException;

// import ict.thesis.management.dto.response.IdentityUserResponse;
// import ict.thesis.management.dto.request.ApprovalRequest;
// import ict.thesis.management.dto.response.EventApprovalResponse;
// import ict.thesis.management.entity.EventApprovals;
// import ict.thesis.management.entity.Events;
// import ict.thesis.management.entity.OrganizerProfile;
// // import ict.thesis.management.entity.RefUser;
// import ict.thesis.management.entity.enums.ApprovalDecision;
// import ict.thesis.management.entity.enums.EventStatus;
// // import ict.thesis.management.entity.enums.UserRole;
// import ict.thesis.management.repository.EventApprovalsRepository;
// import ict.thesis.management.repository.EventsRepository;
// // import ict.thesis.management.repository.RefUserRepository;

// @Service
// public class EventApprovalService {
//     private final EventsRepository eventsRepository;
//     private final EventApprovalsRepository eventApprovalsRepository;
//     // private final RefUserSyncService refUserSyncService;
//     private final RestTemplate restTemplate;

//     @Value("${identity.base-url:http://localhost:8081}")
//     private String identityBase;

//     public EventApprovalService(
//         EventsRepository eventsRepository,
//         EventApprovalsRepository eventApprovalsRepository,
//         RefUserRepository refUserRepository,
//         RefUserSyncService refUserSyncService,
//         RestTemplate restTemplate
//     ) {
//         this.eventsRepository = eventsRepository;
//         this.eventApprovalsRepository = eventApprovalsRepository;
//         this.refUserRepository = refUserRepository;
//         this.refUserSyncService = refUserSyncService;
//         this.restTemplate = restTemplate;
//     }

//     @Transactional
//     public EventApprovalResponse approve(Long eventId, ApprovalRequest request) {
//         if (request == null || request.adminUserId() == null) {
//             throw new IllegalArgumentException("adminUserId is required");
//         }
//         if (request.decision() == null) {
//             throw new IllegalArgumentException("decision is required");
//         }

//         Events event = eventsRepository.findById(eventId)
//             .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
//         RefUser admin = refUserRepository.findById(request.adminUserId())
//             .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));
//         if (admin.getRole() != UserRole.ADMIN) {
//             throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin user is required");
//         }

//         OrganizerProfile organizerProfile = event.getOrganizerProfile();
//         if (organizerProfile == null) {
//             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event organizer is missing");
//         }
//         IdentityUserResponse organizerUser = loadIdentityUser(organizerProfile.getUserId());

//         EventApprovals approval = new EventApprovals();
//         approval.setEvent(event);
//         approval.setAdminUser(admin);
//         approval.setDecision(request.decision());
//         approval.setReason(request.reason());
//         approval.setDecidedAt(Instant.now());

//         Instant now = Instant.now();
//         if (request.decision() == ApprovalDecision.APPROVED) {
//             event.setStatus(EventStatus.APPROVED);
//             event.setPublished(false);
//             if (organizerUser == null || organizerUser.getRole() == null || UserRole.CUSTOMER.name().equalsIgnoreCase(organizerUser.getRole())) {
//                 IdentityUserResponse promoted = promoteOrganizer(organizerProfile.getUserId());
//                 refUserSyncService.upsertFromIdentity(promoted);
//                 organizerUser = promoted;
//             }
//         } else {
//             event.setStatus(EventStatus.CANCELLED);
//             event.setPublished(false);
//         }
//         event.setUpdatedAt(now);

//         Events savedEvent = eventsRepository.save(event);
//         EventApprovals savedApproval = eventApprovalsRepository.save(approval);

//         return new EventApprovalResponse(
//             savedApproval.getId(),
//             savedEvent.getId(),
//             organizerUser == null ? organizerProfile.getUserId() : organizerUser.getId(),
//             organizerUser == null || organizerUser.getRole() == null ? null : organizerUser.getRole(),
//             admin.getId(),
//             savedApproval.getDecision(),
//             savedEvent.getStatus(),
//             savedApproval.getReason(),
//             savedApproval.getDecidedAt()
//         );
//     }

//     private IdentityUserResponse loadIdentityUser(Long userId) {
//         try {
//             IdentityUserResponse response = restTemplate.getForObject(identityBase + "/api/users/{id}", IdentityUserResponse.class, userId);
//             if (response == null) {
//                 throw new IllegalArgumentException("User not found in identity: " + userId);
//             }
//             return response;
//         } catch (RestClientException ex) {
//             throw new IllegalArgumentException("User not found in identity: " + userId);
//         }
//     }

//     private IdentityUserResponse promoteOrganizer(Long userId) {
//         try {
//             Map<String, Object> body = Map.of("role", "ORGANIZER");
//             IdentityUserResponse response = restTemplate.exchange(
//                 identityBase + "/api/users/{id}",
//                 HttpMethod.PUT,
//                 new HttpEntity<>(body),
//                 IdentityUserResponse.class,
//                 userId
//             ).getBody();
//             if (response == null) {
//                 throw new IllegalArgumentException("Unable to promote organizer in identity");
//             }
//             return response;
//         } catch (RestClientException ex) {
//             throw new IllegalArgumentException("Unable to promote organizer in identity");
//         }
//     }
// }

