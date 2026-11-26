package com.donornexa.notification.repository;


import com.donornexa.notification.entity.Notification;
import com.donornexa.common.enums.NotificationStatus;
import com.donornexa.common.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Notification} entities.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find all notifications for a recipient, ordered newest first.
     * Suitable for displaying a notification inbox.
     */
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    /** Find top 5 recent notifications for recipient. */
    List<Notification> findTop5ByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    /** Find filtered notifications for a recipient with pagination. */
    @org.springframework.data.jpa.repository.Query("SELECT n FROM Notification n WHERE n.recipient.id = :recipientId " +
           "AND (:type IS NULL OR n.notificationType = :type) " +
           "AND (:status IS NULL OR n.notificationStatus = :status) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findFilteredByRecipient(
            @org.springframework.data.repository.query.Param("recipientId") Long recipientId,
            @org.springframework.data.repository.query.Param("type") NotificationType type,
            @org.springframework.data.repository.query.Param("status") NotificationStatus status,
            Pageable pageable);

    /** Find all notifications for a recipient with a specific read status. */
    List<Notification> findByRecipientIdAndNotificationStatus(Long recipientId,
                                                               NotificationStatus notificationStatus);

    /** Find notifications for a recipient with a specific type. */
    List<Notification> findByRecipientIdAndNotificationType(Long recipientId,
                                                             NotificationType notificationType);

    /** Count unread notifications for a recipient (useful for badge counters). */
    long countByRecipientIdAndNotificationStatus(Long recipientId,
                                                  NotificationStatus notificationStatus);

    /** Find notifications for a recipient with pagination, ordered newest first. */
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    /** Count system notifications by status */
    long countByNotificationStatus(NotificationStatus notificationStatus);

    /** Find all system notifications with a specific status */
    List<Notification> findByNotificationStatus(NotificationStatus notificationStatus);

    /** Count system notifications by type */
    long countByNotificationType(NotificationType notificationType);

    /** Administrative query with eager-loaded recipient user */
    @org.springframework.data.jpa.repository.Query("SELECT n FROM Notification n JOIN FETCH n.recipient r " +
           "WHERE (:type IS NULL OR n.notificationType = :type) " +
           "AND (:status IS NULL OR n.notificationStatus = :status) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findFilteredWithRecipient(
            @org.springframework.data.repository.query.Param("type") NotificationType type,
            @org.springframework.data.repository.query.Param("status") NotificationStatus status,
            Pageable pageable);
}
