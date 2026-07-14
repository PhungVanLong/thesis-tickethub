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
    org.springframework.data.domain.Page<Order> findByCustomerOrderByCreatedAtDesc(Long customer, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status AND o.eventId IN :eventIds")
    java.math.BigDecimal sumTotalAmountByEventIdsAndStatus(@org.springframework.data.repository.query.Param("eventIds") List<Long> eventIds, @org.springframework.data.repository.query.Param("status") OrderStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o WHERE o.eventId IN :eventIds ORDER BY o.createdAt DESC")
    List<Order> findRecentByEventIds(@org.springframework.data.repository.query.Param("eventIds") List<Long> eventIds, org.springframework.data.domain.Pageable pageable);
}
