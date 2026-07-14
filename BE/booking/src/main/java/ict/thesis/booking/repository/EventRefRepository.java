package ict.thesis.booking.repository;

import ict.thesis.booking.enties.EventRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRefRepository extends JpaRepository<EventRef, Long> {
}
