package ict.thesis.booking.repository;

import ict.thesis.booking.enties.TicketTierRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketTierRefRepository extends JpaRepository<TicketTierRef, Long> {
}
