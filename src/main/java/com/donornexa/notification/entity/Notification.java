package com.donornexa.notification.entity;

import com.donornexa.messaging.entity.Message;
import com.donornexa.user.entity.User;


import com.donornexa.common.enums.NotificationStatus;
import com.donornexa.common.enums.NotificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a notification sent to a user.
 * Notifications are created by the system; they are never modified
 * except to update the read status.
 */
@Getter
@Setter
@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notifications_recipient_id",        columnList = "recipient_id"),
        @Index(name = "idx_notifications_notification_status", columnList = "notification_status"),
        @Index(name = "idx_notifications_notification_type",   columnList = "notification_type"),
        @Index(name = "idx_notifications_created_at",          columnList = "created_at")
    }
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Recipient is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_notifications_recipient"))
    private User recipient;

    @NotBlank(message = "Notification title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @NotBlank(message = "Notification message is required")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    @NotNull(message = "Notification type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 25)
    private NotificationType notificationType;

    @NotNull(message = "Notification status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", nullable = false, length = 10)
    private NotificationStatus notificationStatus;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.notificationStatus == null) {
            this.notificationStatus = NotificationStatus.UNREAD;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Notification{id=" + id + ", notificationType=" + notificationType
                + ", notificationStatus=" + notificationStatus + "}";
    }
}
