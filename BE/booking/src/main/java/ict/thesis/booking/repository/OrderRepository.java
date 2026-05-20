package ict.thesis.booking.repository;

import ict.thesis.booking.enties.Order;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderCode(String orderCode);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);
}

