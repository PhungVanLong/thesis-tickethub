package ict.thesis.management.service;

import ict.thesis.management.dto.request.CreateEventRequest;
import ict.thesis.management.dto.response.CreateEventResponse;
import ict.thesis.management.entity.Events;
import ict.thesis.management.entity.OrganizerProfile;
import ict.thesis.management.entity.enums.EventStatus;
import ict.thesis.management.repository.EventsRepository;
import ict.thesis.management.repository.OrganizerProfileRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class EventService {
    private final EventsRepository eventsRepository;
    private final OrganizerProfileRepository organizerProfileRepository;

    public EventService(EventsRepository eventsRepository, OrganizerProfileRepository organizerProfileRepository) {
        this.eventsRepository = eventsRepository;
        this.organizerProfileRepository = organizerProfileRepository;
    }

    public CreateEventResponse createEvent(CreateEventRequest request) {
        OrganizerProfile organizerProfile = organizerProfileRepository.findByUserId(request.getOrganizerId())
            .orElseGet(() -> {
                OrganizerProfile profile = new OrganizerProfile();
                profile.setUserId(request.getOrganizerId());
                profile.setOrganizationName("Organizer " + request.getOrganizerId());
                profile.setSyncedAt(Instant.now());
                return organizerProfileRepository.save(profile);
            });

        Events event = new Events();
        event.setOrganizerProfile(organizerProfile);
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
        response.setCreatedAt(saved.getCreatedAt());
        response.setUpdatedAt(saved.getUpdatedAt());
        return response;
    }
}
