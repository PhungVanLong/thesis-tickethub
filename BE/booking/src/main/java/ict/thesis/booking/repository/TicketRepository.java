package ict.thesis.booking.repository;

import ict.thesis.booking.enties.Ticket;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByOrderItem_Order_Id(Long orderId);

    boolean existsByTicketCode(String ticketCode);
}

