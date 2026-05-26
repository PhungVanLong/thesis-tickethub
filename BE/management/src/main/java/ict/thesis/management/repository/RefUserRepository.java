package ict.thesis.management.repository;

import ict.thesis.management.entity.RefUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Ref;

@Repository
public interface RefUserRepository extends JpaRepository<RefUser, Long> {
}
