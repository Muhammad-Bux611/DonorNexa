package com.donornexa.common.advice;

import com.donornexa.notification.entity.Notification;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.NotificationStatus;
import com.donornexa.notification.repository.NotificationRepository;
import com.donornexa.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Controller advice providing authenticated user details, roles,
 * unread notification counts, and recent notifications globally to all Thymeleaf views.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalDashboardControllerAdvice {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final com.donornexa.messaging.repository.MessageRepository messageRepository;

    @ModelAttribute("currentUser")
    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName().toLowerCase().trim()).orElse(null);
    }

    @ModelAttribute("unreadNotificationCount")
    public long getUnreadNotificationCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return 0;
        }
        Optional<User> user = userRepository.findByEmail(authentication.getName().toLowerCase().trim());
        return user.map(u -> notificationRepository.countByRecipientIdAndNotificationStatus(
                u.getId(), NotificationStatus.UNREAD)).orElse(0L);
    }

    @ModelAttribute("unreadMessageCount")
    public long getUnreadMessageCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return 0;
        }
        Optional<User> user = userRepository.findByEmail(authentication.getName().toLowerCase().trim());
        return user.map(u -> messageRepository.countByRecipientIdAndIsReadFalse(u.getId())).orElse(0L);
    }

    @ModelAttribute("recentNotifications")
    public List<Notification> getRecentNotifications(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Collections.emptyList();
        }
        Optional<User> user = userRepository.findByEmail(authentication.getName().toLowerCase().trim());
        return user.map(u -> notificationRepository.findTop5ByRecipientIdOrderByCreatedAtDesc(u.getId()))
                .orElse(Collections.emptyList());
    }
}
