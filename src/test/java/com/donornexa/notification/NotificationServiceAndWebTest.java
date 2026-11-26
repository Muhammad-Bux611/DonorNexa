package com.donornexa.notification;

import com.donornexa.common.enums.Role;
import com.donornexa.notification.entity.Notification;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.common.enums.NotificationStatus;
import com.donornexa.common.enums.NotificationType;
import com.donornexa.notification.service.NotificationService;
import com.donornexa.notification.repository.NotificationRepository;
import com.donornexa.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 5 â€” Notification Service & Inbox Web Security Tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 5 â€” Notification Service & Web Security Tests")
class NotificationServiceAndWebTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationService notificationService;

    private static final String USER1_EMAIL = "notif.user1@example.com";
    private static final String USER2_EMAIL = "notif.user2@example.com";

    private User user1;
    private User user2;
    private Notification user1Notification;

    @BeforeEach
    void setUp() {
        user1 = userRepository.findByEmail(USER1_EMAIL).orElseGet(() -> {
            User u = new User();
            u.setEmail(USER1_EMAIL);
            u.setPasswordHash("$2a$10$e8Z4z61gCslV1bI3wO5B9.fKj2X1f7B9xO7iC9V8E3eXyR0B6xVae");
            u.setFullName("Notification Recipient One");
            u.setPhoneNumber("03331111222");
            u.setRole(Role.DONOR);
            u.setAccountStatus(AccountStatus.ACTIVE);
            u.setEnabled(true);
            return userRepository.save(u);
        });

        user2 = userRepository.findByEmail(USER2_EMAIL).orElseGet(() -> {
            User u = new User();
            u.setEmail(USER2_EMAIL);
            u.setPasswordHash("$2a$10$e8Z4z61gCslV1bI3wO5B9.fKj2X1f7B9xO7iC9V8E3eXyR0B6xVae");
            u.setFullName("Notification Recipient Two");
            u.setPhoneNumber("03332222333");
            u.setRole(Role.HOSPITAL);
            u.setAccountStatus(AccountStatus.ACTIVE);
            u.setEnabled(true);
            return userRepository.save(u);
        });

        user1Notification = notificationService.createNotification(
                user1,
                "Donor Verification Update",
                "Your volunteer profile has been reviewed and successfully approved.",
                NotificationType.SYSTEM_ALERT
        );
    }

    @Test
    @DisplayName("1. Anonymous user accessing /notifications is redirected to login")
    void anonymous_cannotAccessNotifications() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = USER1_EMAIL, roles = {"DONOR"})
    @DisplayName("2. Authenticated DONOR can view /notifications inbox")
    void donor_canAccessNotificationsView() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("notification/notifications"))
                .andExpect(content().string(containsString("Notification Center")))
                .andExpect(content().string(containsString("Donor Verification Update")));
    }

    @Test
    @WithMockUser(username = USER2_EMAIL, roles = {"HOSPITAL"})
    @DisplayName("3. Authenticated HOSPITAL can view /notifications inbox")
    void hospital_canAccessNotificationsView() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("notification/notifications"))
                .andExpect(content().string(containsString("Notification Center")));
    }

    @Test
    @WithMockUser(username = USER1_EMAIL, roles = {"DONOR"})
    @DisplayName("4. User can mark an unread notification as READ")
    void user_canMarkNotificationAsRead() throws Exception {
        mockMvc.perform(post("/notifications/" + user1Notification.getId() + "/read")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"))
                .andExpect(flash().attributeExists("successMessage"));

        Notification updated = notificationRepository.findById(user1Notification.getId()).orElseThrow();
        assertEquals(NotificationStatus.READ, updated.getNotificationStatus());
    }

    @Test
    @WithMockUser(username = USER2_EMAIL, roles = {"HOSPITAL"})
    @DisplayName("5. User cannot mark another user's notification as read (Cross-user isolation)")
    void user_cannotMarkAnotherUsersNotificationAsRead() throws Exception {
        mockMvc.perform(post("/notifications/" + user1Notification.getId() + "/read")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"))
                .andExpect(flash().attributeExists("errorMessage"));

        Notification unchanged = notificationRepository.findById(user1Notification.getId()).orElseThrow();
        assertEquals(NotificationStatus.UNREAD, unchanged.getNotificationStatus());
    }

    @Test
    @WithMockUser(username = USER1_EMAIL, roles = {"DONOR"})
    @DisplayName("6. User can mark all notifications as READ")
    void user_canMarkAllNotificationsAsRead() throws Exception {
        notificationService.createNotification(user1, "Alert 2", "Second alert", NotificationType.DONATION_REMINDER);

        mockMvc.perform(post("/notifications/read-all")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"))
                .andExpect(flash().attributeExists("successMessage"));

        long remainingUnread = notificationRepository.countByRecipientIdAndNotificationStatus(
                user1.getId(), NotificationStatus.UNREAD);
        assertEquals(0, remainingUnread);
    }

    @Test
    @WithMockUser(username = USER1_EMAIL, roles = {"DONOR"})
    @DisplayName("7. POST to /notifications/{id}/read without CSRF returns 403 Forbidden")
    void postReadWithoutCsrf_forbidden() throws Exception {
        mockMvc.perform(post("/notifications/" + user1Notification.getId() + "/read"))
                .andExpect(status().isForbidden());
    }
}
