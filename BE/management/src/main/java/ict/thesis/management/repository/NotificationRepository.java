package ict.thesis.management.repository;

import ict.thesis.management.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrRecipientRoleOrderByCreatedAtDesc(Long userId, String recipientRole);
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
}
