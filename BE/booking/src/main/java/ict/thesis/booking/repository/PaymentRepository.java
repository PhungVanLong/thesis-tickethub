package ict.thesis.booking.repository;

import ict.thesis.booking.enties.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrder_Id(Long orderId);

    Optional<Payment> findByGatewayTxId(String gatewayTxId);
}

