package ict.thesis.management.service;

import ict.thesis.management.entity.Notification;
import ict.thesis.management.repository.NotificationRepository;
import ict.thesis.management.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getNotifications() {
        Long userId = UserContextHolder.getContext().getUserId();
        String role = UserContextHolder.getContext().getRole();

        if ("ADMIN".equalsIgnoreCase(role)) {
            return notificationRepository.findByUserIdOrRecipientRoleOrderByCreatedAtDesc(userId, "ADMIN");
        } else {
            return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
    }

    @Transactional
    public void markAsRead(Long id) {
        Long userId = UserContextHolder.getContext().getUserId();
        String role = UserContextHolder.getContext().getRole();

        notificationRepository.findById(id).ifPresent(notification -> {
            // Check authorization
            if (userId.equals(notification.getUserId()) || 
                ("ADMIN".equalsIgnoreCase(role) && "ADMIN".equalsIgnoreCase(notification.getRecipientRole()))) {
                notification.setRead(true);
                notificationRepository.save(notification);
            }
        });
    }

    @Transactional
    public void markAllAsRead() {
        List<Notification> notifications = getNotifications();
        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                notification.setRead(true);
                notificationRepository.save(notification);
            }
        }
    }

    @Transactional
    public void createNotification(Long userId, String recipientRole, String title, String message, Long eventId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setRecipientRole(recipientRole);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setEventId(eventId);
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
    }
}
