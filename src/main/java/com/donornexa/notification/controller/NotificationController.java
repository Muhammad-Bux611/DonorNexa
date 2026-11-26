package com.donornexa.notification.controller;

import com.donornexa.notification.entity.Notification;
import com.donornexa.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;

/**
 * Controller providing in-app notification center for all authenticated users
 * (DONOR, HOSPITAL, ADMIN).
 */
@Slf4j
@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * View in-app notifications inbox with pagination and optional filtering.
     */
    @GetMapping
    public String listNotifications(@RequestParam(value = "page", defaultValue = "0") int page,
                                    @RequestParam(value = "size", defaultValue = "10") int size,
                                    @RequestParam(value = "type", required = false) com.donornexa.common.enums.NotificationType type,
                                    @RequestParam(value = "status", required = false) com.donornexa.common.enums.NotificationStatus status,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    Model model) {

        int validPage = Math.max(0, page);
        int validSize = Math.max(1, Math.min(size, 50));
        Pageable pageable = PageRequest.of(validPage, validSize);

        String email = userDetails.getUsername();
        Page<Notification> notificationsPage = notificationService.getFilteredNotificationsForUser(email, type, status, pageable);
        long unreadCount = notificationService.getUnreadCount(email);

        model.addAttribute("notificationsPage", notificationsPage);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("types", com.donornexa.common.enums.NotificationType.values());
        model.addAttribute("statuses", com.donornexa.common.enums.NotificationStatus.values());

        return "notification/notifications";
    }

    /**
     * Mark a specific notification as READ.
     */
    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable("id") Long id,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        try {
            notificationService.markAsRead(id, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Notification marked as read.");
        } catch (AccessDeniedException ex) {
            log.warn("Access denied for user {} attempting to read notification {}", userDetails.getUsername(), id);
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied: You cannot view or modify this notification.");
        } catch (NoSuchElementException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Notification not found.");
        } catch (Exception ex) {
            log.error("Failed to mark notification {} as read: {}", id, ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update notification.");
        }

        return "redirect:/notifications";
    }

    /**
     * Mark all notifications as READ.
     */
    @PostMapping("/read-all")
    public String markAllAsRead(@AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            int marked = notificationService.markAllAsRead(userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    marked > 0 ? "Marked " + marked + " notification(s) as read." : "All notifications are already read.");
        } catch (Exception ex) {
            log.error("Failed to mark all notifications read for user {}: {}", userDetails.getUsername(), ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to mark notifications as read.");
        }

        return "redirect:/notifications";
    }
}
