package ict.thesis.management.repository;

import java.util.List;
import ict.thesis.management.entity.Events;
import ict.thesis.management.entity.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventsRepository extends JpaRepository<Events, Long> {
    List<Events> findByStatus(EventStatus status);
    List<Events> findByOrganizationId(Long organizationId);
}
