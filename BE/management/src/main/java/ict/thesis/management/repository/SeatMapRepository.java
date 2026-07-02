package ict.thesis.management.repository;

import ict.thesis.management.entity.SeatMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatMapRepository extends JpaRepository<SeatMap, Long> {
    List<SeatMap> findByEventId(Long eventId);
}
