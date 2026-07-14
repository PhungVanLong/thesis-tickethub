package ict.thesis.management.kafka;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ict.thesis.management.entity.EventStaff;
import ict.thesis.management.entity.Events;
import ict.thesis.management.entity.OrganizationMember;
import ict.thesis.management.entity.enums.OrganizationRole;
import ict.thesis.management.entity.enums.RoleEventStaff;
import ict.thesis.management.repository.EventStaffRepository;
import ict.thesis.management.repository.EventsRepository;
import ict.thesis.management.repository.OrganizationMemberRepository;
import ict.thesis.management.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StaffCreatedSuccessConsumer {
    private static final Logger logger = LoggerFactory.getLogger(StaffCreatedSuccessConsumer.class);

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final EventsRepository eventsRepository;
    private final EventStaffRepository eventStaffRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user-staff-created-success-topic", groupId = "management-group")
    @Transactional
    public void consume(String payload) {
        logger.info("Received staff creation success callback event. Payload: {}", payload);
        try {
            JsonNode node = objectMapper.readTree(payload);
            Long organizationId = node.has("organizationId") ? node.get("organizationId").asLong() : null;
            Long userId = node.has("userId") ? node.get("userId").asLong() : null;

            if (organizationId == null || userId == null) {
                logger.warn("Invalid staff success payload: {}", payload);
                return;
            }

            organizationRepository.findById(organizationId).ifPresentOrElse(organization -> {
                OrganizationMember member = organizationMemberRepository
                        .findByOrganizationIdAndUserId(organizationId, userId)
                        .orElseGet(() -> {
                            OrganizationMember newMember = new OrganizationMember();
                            newMember.setOrganization(organization);
                            newMember.setUserId(userId);
                            newMember.setMemberRole(OrganizationRole.STAFF);
                            newMember.setAssignedAt(Instant.now());
                            return organizationMemberRepository.save(newMember);
                        });

                List<Events> organizationEvents = eventsRepository.findByOrganizationId(organizationId);
                Instant assignedAt = member.getAssignedAt() != null ? member.getAssignedAt() : Instant.now();
                List<EventStaff> eventStaffToSave = new ArrayList<>();

                for (Events event : organizationEvents) {
                    if (eventStaffRepository.existsByEventIdAndStaff(event.getId(), userId)) {
                        continue;
                    }

                    EventStaff eventStaff = new EventStaff();
                    eventStaff.setEvent(event);
                    eventStaff.setStaff(userId);
                    eventStaff.setRoleInEvent(RoleEventStaff.CHECKIN_STAFF);
                    eventStaff.setAssignedAt(assignedAt);
                    eventStaffToSave.add(eventStaff);
                }

                if (!eventStaffToSave.isEmpty()) {
                    eventStaffRepository.saveAll(eventStaffToSave);
                }

                logger.info("Successfully linked staff user {} to organization {} and {} event(s)", userId,
                        organizationId, organizationEvents.size());
            }, () -> logger.warn("Organization not found for staff success payload: {}", payload));
        } catch (Exception e) {
            logger.error("Error processing staff creation success callback event: {}", payload, e);
        }
    }
}