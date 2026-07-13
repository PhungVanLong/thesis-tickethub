package ict.thesis.booking.repository;

import ict.thesis.booking.enties.Checkin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CheckinRepository extends JpaRepository<Checkin, UUID> {
    java.util.List<Checkin> findByTicketId(Long ticketId);
}
