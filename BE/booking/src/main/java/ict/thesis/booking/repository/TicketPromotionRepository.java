package ict.thesis.booking.repository;

import ict.thesis.booking.enties.TicketPromotion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketPromotionRepository extends JpaRepository<TicketPromotion, Long> {
}

