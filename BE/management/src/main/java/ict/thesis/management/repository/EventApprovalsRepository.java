package ict.thesis.management.repository;

import ict.thesis.management.entity.EventApprovals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventApprovalsRepository extends JpaRepository<EventApprovals, Long> {
}

