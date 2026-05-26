package ict.thesis.management.service;

import ict.thesis.management.dto.request.CreateEventRequest;
import ict.thesis.management.dto.response.CreateEventResponse;
import ict.thesis.management.dto.IdentityUserResponse;
import ict.thesis.management.entity.Events;
import ict.thesis.management.entity.RefUser;
import ict.thesis.management.entity.enums.EventStatus;
import ict.thesis.management.entity.enums.UserRole;
import ict.thesis.management.repository.EventsRepository;
import ict.thesis.management.repository.RefUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
public class EventService {
    private final EventsRepository eventsRepository;
    private final RefUserRepository refUserRepository;
    private final RestTemplate restTemplate;
    private final String identityBase = "http://localhost:8081";

    public EventService(EventsRepository eventsRepository, RefUserRepository refUserRepository, RestTemplate restTemplate) {
        this.eventsRepository = eventsRepository;
        this.refUserRepository = refUserRepository;
        this.restTemplate = restTemplate;
    }
public CreateEventResponse createEvent( CreateEventRequest request) {
    // validate organizer exists locally or sync from identity
    RefUser organizer = null;
    if (request.getOrganizerId() == null) {
        throw new IllegalArgumentException("Organizer id required");
    }
    organizer = refUserRepository.findById(request.getOrganizerId()).orElse(null);
    if (organizer == null) {
        // try to fetch from identity
        String url = identityBase + "/api/auth/users/" + request.getOrganizerId();
        try {
            IdentityUserResponse iu = restTemplate.getForObject(url, IdentityUserResponse.class);
            if (iu == null) throw new IllegalArgumentException("Organizer not found in identity: " + request.getOrganizerId());
            if (!iu.isActive() || !iu.isVerified()) {
                throw new IllegalArgumentException("Organizer is not active or not verified in identity");
            }
            RefUser ru = new RefUser();
            ru.setId(iu.getId());
            ru.setEmail(iu.getEmail());
            ru.setFullName(iu.getFullName());
            try {
                ru.setRole(UserRole.valueOf(iu.getRole()));
            } catch (Exception ex) {
                ru.setRole(UserRole.CUSTOMER);
            }
            ru.setSyncedAt(Instant.now());
            organizer = refUserRepository.save(ru);
        } catch (HttpClientErrorException.NotFound nf) {
            throw new IllegalArgumentException("Organizer not found with id: " + request.getOrganizerId());
        }
    }
    //validation
    if(request.getTitle()==null || request.getTitle().isEmpty()){
        throw new IllegalArgumentException("Title required");
    }
    if(request.getStartTime() != null && request.getEndTime() != null && !request.getStartTime().isBefore(request.getEndTime())){
        throw new IllegalArgumentException("Start time must be before end time");

    }
    //map DTO -> entity
    Events event = new Events();
    // map DTO -> entity
    event.setRefUser(organizer);
    event.setTitle(request.getTitle());
    event.setDescription(request.getDescription());
    event.setVenue(request.getVenue());
    event.setCity(request.getCity());
    event.setLocationCoords(request.getLocationCoords());
    event.setStartTime(request.getStartTime());
    event.setEndTime(request.getEndTime());
    event.setBannerUrl(request.getBannerUrl());
    event.setStatus(EventStatus.DRAFT);
    event.setPublished(false);
    Instant now = Instant.now();
    event.setCreatedAt(now);
    event.setUpdatedAt(now);

    Events saved = eventsRepository.save(event);
    CreateEventResponse response = new CreateEventResponse();
    response.setId(saved.getId());
    response.setStatus(saved.getStatus());
    response.setCreatedAt(now);
    response.setUpdatedAt(now);
    return response;

}
}
