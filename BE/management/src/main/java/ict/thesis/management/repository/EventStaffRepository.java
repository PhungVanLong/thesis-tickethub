package ict.thesis.management.repository;

import java.util.List;
import ict.thesis.management.entity.EventStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventStaffRepository extends JpaRepository<EventStaff, Long> {
    List<EventStaff> findByEventId(Long eventId);
    boolean existsByEventIdAndStaff(Long eventId, Long staffId);
    void deleteByEventIdAndStaff(Long eventId, Long staffId);
}
