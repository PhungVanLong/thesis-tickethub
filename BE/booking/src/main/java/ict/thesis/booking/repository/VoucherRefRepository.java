package ict.thesis.booking.repository;

import ict.thesis.booking.enties.VoucherRef;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherRefRepository extends JpaRepository<VoucherRef, Long> {

    Optional<VoucherRef> findByCode(String code);
}

