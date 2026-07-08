package ict.thesis.management.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ict.thesis.management.entity.Organization;
import ict.thesis.management.entity.enums.OrganizationStatus;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    boolean existsByTaxCode(String taxCode);
    List<Organization> findByStatus(OrganizationStatus status);
}
