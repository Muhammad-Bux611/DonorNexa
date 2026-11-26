package com.donornexa.admin.service;

import com.donornexa.notification.entity.Notification;
import com.donornexa.common.enums.ConversationStatus;
import com.donornexa.common.enums.NotificationStatus;
import com.donornexa.common.enums.NotificationType;
import com.donornexa.messaging.repository.ConversationRepository;
import com.donornexa.messaging.repository.MessageRepository;
import com.donornexa.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service providing administrative monitoring of platform-wide system notification delivery
 * and coordination messaging volume without compromising personal message privacy.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCommunicationService {

    private final NotificationRepository notificationRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public Page<Notification> getNotificationsPage(NotificationType type, NotificationStatus status, Pageable pageable) {
        return notificationRepository.findFilteredWithRecipient(type, status, pageable);
    }

    @Transactional(readOnly = true)
    public long getTotalNotificationsCount() {
        return notificationRepository.count();
    }

    @Transactional(readOnly = true)
    public long getUnreadNotificationsCount() {
        return notificationRepository.countByNotificationStatus(NotificationStatus.UNREAD);
    }

    @Transactional(readOnly = true)
    public long getTotalConversationsCount() {
        return conversationRepository.count();
    }

    @Transactional(readOnly = true)
    public long getActiveConversationsCount() {
        return conversationRepository.countByStatus(ConversationStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public long getTotalMessagesCount() {
        return messageRepository.count();
    }

    @Transactional(readOnly = true)
    public long getUnreadMessagesCount() {
        return messageRepository.countByIsReadFalse();
    }

    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setNotificationStatus(NotificationStatus.READ);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public int markAllAsRead() {
        java.util.List<Notification> unread = notificationRepository.findByNotificationStatus(NotificationStatus.UNREAD);
        for (Notification n : unread) {
            n.setNotificationStatus(NotificationStatus.READ);
        }
        notificationRepository.saveAll(unread);
        return unread.size();
    }
}
