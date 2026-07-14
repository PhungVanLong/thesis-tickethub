package ict.thesis.booking.repository;

import ict.thesis.booking.enties.Checkin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CheckinRepository extends JpaRepository<Checkin, UUID> {
    java.util.List<Checkin> findByTicketId(Long ticketId);
    org.springframework.data.domain.Page<Checkin> findByStaff(Long staff, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Checkin> findByStaffAndEventId(Long staff, Long eventId, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Checkin> findByEventId(Long eventId, org.springframework.data.domain.Pageable pageable);
}
