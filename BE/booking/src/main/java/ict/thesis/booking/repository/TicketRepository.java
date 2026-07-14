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

    @Query("SELECT t FROM Ticket t JOIN t.orderItem oi JOIN oi.order o WHERE o.id = :orderId")
    List<Ticket> findByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT COUNT(t) FROM Ticket t JOIN t.orderItem oi JOIN oi.order o WHERE o.eventId IN :eventIds")
    long countTicketsByEventIds(@Param("eventIds") List<Long> eventIds);

    @Query("SELECT COUNT(t) FROM Ticket t JOIN t.orderItem oi JOIN oi.order o WHERE o.eventId IN :eventIds AND t.status = :status")
    long countTicketsByEventIdsAndStatus(@Param("eventIds") List<Long> eventIds, @Param("status") ict.thesis.booking.enties.enums.TicketStatus status);
}
