package ict.thesis.management.service;

import ict.thesis.management.dto.response.EventResponse;
import ict.thesis.management.entity.Events;
import ict.thesis.management.entity.OrganizationMember;
import ict.thesis.management.entity.enums.EventStatus;
import ict.thesis.management.repository.EventsRepository;
import ict.thesis.management.repository.OrganizationMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventQueryService {

    private final EventsRepository eventsRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    @Transactional(readOnly = true)
    public List<EventResponse> getOrganizerEvents(Long userId) {
        OrganizationMember member = organizationMemberRepository.findByUserId(userId).stream().findFirst()
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "User is not a member of any organization"
            ));

        Long orgId = member.getOrganization().getId();
        List<Events> list = eventsRepository.findByOrganizationId(orgId);
        return list.stream()
                   .map(this::toEventResponse)
                   .toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents(EventStatus status) {
        List<Events> list;
        if (status == null) {
            list = eventsRepository.findAll();
        } else {
            list = eventsRepository.findByStatus(status);
        }
        return list.stream()
                   .map(this::toEventResponse)
                   .toList();
    }

    public EventResponse toEventResponse(Events event) {
        return new EventResponse(
            event.getId(),
            event.getOrganization() != null ? event.getOrganization().getId() : null,
            event.getOrganization() != null ? event.getOrganization().getName() : null,
            event.getTitle(),
            event.getDescription(),
            event.getVenue(),
            event.getCity(),
            event.getLocationCoords(),
            event.getStartTime(),
            event.getEndTime(),
            event.getBannerUrl(),
            event.getStatus(),
            event.isPublished(),
            event.getCreatedAt(),
            event.getUpdatedAt()
        );
    }
}
