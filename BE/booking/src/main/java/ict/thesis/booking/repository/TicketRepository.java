package ict.thesis.booking.repository;

import ict.thesis.booking.enties.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByTicketCode(String ticketCode);

    @Query("SELECT t FROM Ticket t JOIN t.orderItem oi JOIN oi.order o WHERE o.customer = :customerId ORDER BY t.issuedAt DESC")
    org.springframework.data.domain.Page<Ticket> findByCustomerId(@Param("customerId") Long customerId, org.springframework.data.domain.Pageable pageable);
}
