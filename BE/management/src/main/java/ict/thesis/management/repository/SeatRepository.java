package ict.thesis.management.repository;

import ict.thesis.management.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findBySeatMapId(Long seatMapId);
}
