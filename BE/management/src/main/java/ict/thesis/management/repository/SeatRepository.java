package ict.thesis.management.repository;

import ict.thesis.management.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findBySeatMapId(Long seatMapId);
    void deleteAllBySeatMapId(Long seatMapId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Seat s SET s.status = :status WHERE s.id IN :ids")
    int updateStatusForIds(ict.thesis.management.entity.enums.SeatStatus status, java.util.List<Long> ids);
}

