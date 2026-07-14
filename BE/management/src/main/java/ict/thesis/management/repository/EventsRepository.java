package ict.thesis.management.repository;

import java.util.List;
import ict.thesis.management.entity.Events;
import ict.thesis.management.entity.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

@Repository
public interface EventsRepository extends JpaRepository<Events, Long> {
    List<Events> findByStatus(EventStatus status);
    List<Events> findByOrganizationId(Long organizationId);

    @Query("SELECT e FROM Events e LEFT JOIN TicketTier tt ON tt.event = e " +
           "WHERE (:status IS NULL OR e.status = :status) " +
           "AND (cast(:category as string) IS NULL OR LOWER(e.category) = LOWER(cast(:category as string))) " +
           "AND (cast(:city as string) IS NULL OR LOWER(e.city) = LOWER(cast(:city as string))) " +
           "AND (cast(:startTime as timestamp) IS NULL OR e.startTime >= :startTime) " +
           "AND (cast(:endTime as timestamp) IS NULL OR e.startTime <= :endTime) " +
           "GROUP BY e.id " +
           "ORDER BY SUM(COALESCE(tt.quantitySold, 0)) DESC, e.startTime ASC")
    List<Events> findEventsTrending(
            @Param("status") EventStatus status,
            @Param("category") String category,
            @Param("city") String city,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query("SELECT e FROM Events e " +
           "WHERE (:status IS NULL OR e.status = :status) " +
           "AND (cast(:category as string) IS NULL OR LOWER(e.category) = LOWER(cast(:category as string))) " +
           "AND (cast(:city as string) IS NULL OR LOWER(e.city) = LOWER(cast(:city as string))) " +
           "AND (cast(:startTime as timestamp) IS NULL OR e.startTime >= :startTime) " +
           "AND (cast(:endTime as timestamp) IS NULL OR e.startTime <= :endTime) " +
           "ORDER BY e.startTime ASC")
    List<Events> findEventsChronological(
            @Param("status") EventStatus status,
            @Param("category") String category,
            @Param("city") String city,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query("SELECT e FROM Events e " +
           "WHERE e.status = :status AND e.id <> :currentId " +
           "ORDER BY CASE WHEN (e.category IS NOT NULL AND :category IS NOT NULL AND LOWER(e.category) = LOWER(:category)) THEN 0 ELSE 1 END, " +
           "CASE WHEN (e.city IS NOT NULL AND :city IS NOT NULL AND LOWER(e.city) = LOWER(:city)) THEN 0 ELSE 1 END, " +
           "e.startTime ASC")
    List<Events> findRelatedEvents(
            @Param("status") EventStatus status,
            @Param("category") String category,
            @Param("city") String city,
            @Param("currentId") Long currentId);
}
