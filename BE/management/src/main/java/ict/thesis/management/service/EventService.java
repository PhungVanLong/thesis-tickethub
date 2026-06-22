package ict.thesis.management.service;

import ict.thesis.management.dto.request.CreateEventRequest;
import ict.thesis.management.dto.response.CreateEventResponse;
import ict.thesis.management.entity.Events;
import ict.thesis.management.entity.Organization;
import ict.thesis.management.entity.OrganizationMember;
import ict.thesis.management.entity.enums.EventStatus;
import ict.thesis.management.entity.enums.OrganizationStatus;
import ict.thesis.management.repository.EventsRepository;
import ict.thesis.management.repository.OrganizationMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class EventService {
    private final EventsRepository eventsRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    public EventService(EventsRepository eventsRepository, OrganizationMemberRepository organizationMemberRepository) {
        this.eventsRepository = eventsRepository;
        this.organizationMemberRepository = organizationMemberRepository;
    }

    public CreateEventResponse createEvent(Long userId, CreateEventRequest request) {
        // Tìm thành viên tổ chức dựa trên organizationId và userId
        OrganizationMember member = organizationMemberRepository.findByOrganizationIdAndUserId(request.getOrganizationId(), userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "User is not a member of the specified organization"
            ));

        Organization organization = member.getOrganization();
        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, 
                "Organization is not active"
            );
        }

        Events event = new Events();
        event.setOrganization(organization);
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
