package com.donornexa.notification.service;

import com.donornexa.notification.entity.Notification;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.NotificationStatus;
import com.donornexa.common.enums.NotificationType;
import com.donornexa.notification.repository.NotificationRepository;
import com.donornexa.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Service managing user in-app notifications and read-state transitions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Retrieve paginated notifications for the specified user email, sorted newest first.
     */
    @Transactional(readOnly = true)
    public Page<Notification> getNotificationsForUser(String email, Pageable pageable) {
        User user = getUserByEmail(email);
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId(), pageable);
    }

    /**
     * Retrieve filtered paginated notifications for the specified user email.
     */
    @Transactional(readOnly = true)
    public Page<Notification> getFilteredNotificationsForUser(String email, NotificationType type, NotificationStatus status, Pageable pageable) {
        User user = getUserByEmail(email);
        return notificationRepository.findFilteredByRecipient(user.getId(), type, status, pageable);
    }

    /**
     * Count unread notifications for a user.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        User user = getUserByEmail(email);
        return notificationRepository.countByRecipientIdAndNotificationStatus(user.getId(), NotificationStatus.UNREAD);
    }

    /**
     * Mark a specific notification as read, ensuring ownership isolation.
     */
    @Transactional
    public void markAsRead(Long notificationId, String email) {
        User user = getUserByEmail(email);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NoSuchElementException("Notification not found with ID: " + notificationId));

        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to modify this notification.");
        }

        if (notification.getNotificationStatus() != NotificationStatus.READ) {
            notification.setNotificationStatus(NotificationStatus.READ);
            notificationRepository.save(notification);
            log.debug("Notification {} marked as READ for user {}", notificationId, email);
        }
    }

    /**
     * Mark all unread notifications for the user as read.
     */
    @Transactional
    public int markAllAsRead(String email) {
        User user = getUserByEmail(email);
        List<Notification> unreadList = notificationRepository.findByRecipientIdAndNotificationStatus(
                user.getId(), NotificationStatus.UNREAD);

        for (Notification notification : unreadList) {
            notification.setNotificationStatus(NotificationStatus.READ);
        }

        notificationRepository.saveAll(unreadList);
        log.info("Marked {} notifications as READ for user {}", unreadList.size(), email);
        return unreadList.size();
    }

    /**
     * Dispatches a new notification to the given recipient.
     */
    @Transactional
    public Notification createNotification(User recipient, String title, String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(type);
        notification.setNotificationStatus(NotificationStatus.UNREAD);
        notification.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new NoSuchElementException("User account not found: " + email));
    }
}
