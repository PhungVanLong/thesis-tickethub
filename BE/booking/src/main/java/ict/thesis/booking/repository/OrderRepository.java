package ict.thesis.booking.repository;

import ict.thesis.booking.enties.Order;
import ict.thesis.booking.enties.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, Instant time);
    List<Order> findByCustomerOrderByCreatedAtDesc(Long customer);
}
